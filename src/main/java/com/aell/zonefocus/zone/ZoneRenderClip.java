package com.aell.zonefocus.zone;

import com.aell.zonefocus.ZoneFocusModule;
import com.aell.zonefocus.config.ZoneFocusConfigs;
import com.aell.zonefocus.zone.model.Section;
import com.aell.zonefocus.zone.scope.PlacementMatcher.Resolved;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.SchematicWorldRefresher;
import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.util.IntBoundingBox;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

/**
 * Обрезка голограммы по участкам. Litematica собирает чанк схемы по частям размещений и на каждую часть
 * (бокс подрегиона ∩ чанк) зовёт {@code LayerRange.getClampedRenderBoundingBox} — миксин отдаёт сюда и
 * исходный бокс части, и обрезанный слоями результат. По исходному боксу часть опознаётся геометрически
 * (какой подрегион какого размещения её содержит), и к ней применяется правило её размещения из плана:
 * активный участок — его активная зона, другие участки — ничего или целиком, схемы вне раскладки — целиком
 * или ничего, скрытые участки — ничего.
 *
 * <p>План — неизменяемый снимок, строится в тике клиента и подменяется volatile-ссылкой; {@code clip} зовётся
 * из рабочих потоков сборки чанков и в живую карту Litematica не ходит. Часть, которой в плане нет (схему
 * сдвинули между постановкой задачи и сборкой), не показывается и записывается в «промахи» — тик ставит такие
 * чанки на пересборку один раз на версию плана.
 */
@Environment(EnvType.CLIENT)
public final class ZoneRenderClip
{
    private static final int TRANSITION_TIMEOUT = 40;

    /** Правило видимости размещения: всё, ничего или зоны (в координатах мира). */
    private record Rule(boolean all, List<ZoneBox> zones, boolean active)
    {
        static final Rule ALL = new Rule(true, List.of(), false);
        static final Rule NONE = new Rule(false, List.of(), false);

        boolean nothing()
        {
            return ! this.all && this.zones.isEmpty();
        }

        String descriptor()
        {
            if (this.all)
            {
                return "all";
            }

            StringBuilder builder = new StringBuilder(this.active ? "active" : "zone");

            for (ZoneBox zone : this.zones)
            {
                builder.append(',').append(zone.signature());
            }

            return builder.toString();
        }
    }

    private record Entry(List<ZoneBox> parts, Rule rule)
    {
        boolean owns(IntBoundingBox box)
        {
            for (ZoneBox part : this.parts)
            {
                if (part.minX() <= box.minX() && box.maxX() <= part.maxX()
                        && part.minY() <= box.minY() && box.maxY() <= part.maxY()
                        && part.minZ() <= box.minZ() && box.maxZ() <= part.maxZ())
                {
                    return true;
                }
            }

            return false;
        }

        boolean owns(int x, int y, int z)
        {
            for (ZoneBox part : this.parts)
            {
                if (x >= part.minX() && x <= part.maxX() && y >= part.minY() && y <= part.maxY()
                        && z >= part.minZ() && z <= part.maxZ())
                {
                    return true;
                }
            }

            return false;
        }
    }

    private static final class ClipPlan
    {
        final List<Entry> entries;
        /** Что именно видно у каждого размещения — без координат схем: по нему решается, нужна ли пересборка. */
        final String rules;
        final @Nullable String activeSectionId;
        final @Nullable ZoneBox activeZone;
        final int version;

        ClipPlan(List<Entry> entries, String rules, @Nullable String activeSectionId, @Nullable ZoneBox activeZone, int version)
        {
            this.entries = List.copyOf(entries);
            this.rules = rules;
            this.activeSectionId = activeSectionId;
            this.activeZone = activeZone;
            this.version = version;
        }
    }

    @Nullable
    private static volatile ClipPlan plan;

    /** Уходящая зона активного участка на переходе внахлёст — видна у размещений активного участка. */
    @Nullable
    private static volatile ZoneBox fadingBox;

    private static final Set<Long> pending = ConcurrentHashMap.newKeySet();
    private static final Set<Long> misses = ConcurrentHashMap.newKeySet();

    private static int transitionTicks;
    private static String lastSignature = "-";
    private static int planCounter;
    private static int flushedMissesVersion = -1;

    private ZoneRenderClip() {}

    public static void update()
    {
        ZoneManager manager = ZoneManager.getInstance();
        boolean enabled = ZoneFocusModule.isEnabled()
                && ZoneFocusConfigs.CLIP_SCHEMATIC_RENDER.getBooleanValue()
                && ZoneFocusConfigs.COUNT_BY_ZONE.getBooleanValue()
                && manager.hasSections();

        String signature = enabled
                ? manager.layoutSignature() + '#' + ZoneFocusConfigs.HIDE_OTHER_SECTIONS.getBooleanValue()
                        + ZoneFocusConfigs.HIDE_OUTSIDE_SCHEMATICS.getBooleanValue()
                : "-";

        if (! signature.equals(lastSignature))
        {
            lastSignature = signature;
            ClipPlan previous = plan;
            ClipPlan next = enabled ? build(manager) : null;

            if (next == null)
            {
                endTransition();
                plan = null;

                if (previous != null)
                {
                    SchematicWorldRefresher.INSTANCE.updateAll();
                }
            }
            else if (previous == null || ! previous.rules.equals(next.rules))
            {
                // Видимость изменилась. Плавный переход — только между соседними зонами одного участка:
                // старая зона остаётся видна, пока собираются чанки новой. Далёкие зоны объемлющим боксом
                // накрыли бы всё между ними — там честнее короткая пустота.
                boolean sameSection = previous != null && previous.activeSectionId != null
                        && previous.activeSectionId.equals(next.activeSectionId);
                boolean onlyZoneMoved = sameSection && previous.activeZone != null && next.activeZone != null
                        && ! previous.activeZone.equals(next.activeZone)
                        && previous.rules.replace(previous.activeZone.signature(), "?")
                                .equals(next.rules.replace(next.activeZone.signature(), "?"));

                if (onlyZoneMoved && adjacent(previous.activeZone, next.activeZone))
                {
                    beginTransition(previous.activeZone, next.activeZone);
                    plan = next;
                }
                else
                {
                    endTransition();
                    plan = next;
                    SchematicWorldRefresher.INSTANCE.updateAll();
                }
            }
            else
            {
                // Правила те же, схемы лишь сдвинулись — Litematica сама пересобирает затронутые чанки
                plan = next;
            }
        }

        if (fadingBox != null && (pending.isEmpty() || --transitionTicks <= 0))
        {
            endTransition();
        }

        flushMisses();
    }

    private static ClipPlan build(ZoneManager manager)
    {
        Section active = manager.activeSection();
        boolean hideOther = ZoneFocusConfigs.HIDE_OTHER_SECTIONS.getBooleanValue();
        boolean hideOutside = ZoneFocusConfigs.HIDE_OUTSIDE_SCHEMATICS.getBooleanValue();
        ZoneBox activeZone = active != null && ! active.hidden ? manager.activeZone(active) : null;
        Rule activeRule = activeZone != null ? new Rule(false, List.of(activeZone), true) : Rule.NONE;
        Resolved activeResolved = active != null ? manager.resolve(active) : null;

        List<Entry> entries = new ArrayList<>();
        StringBuilder rules = new StringBuilder();

        for (SchematicPlacement placement : ZoneManager.enabledPlacements())
        {
            if (! placement.matchesRequirement(SubRegionPlacement.RequiredEnabled.RENDERING_ENABLED))
            {
                continue;
            }

            List<ZoneBox> parts = new ArrayList<>();

            for (Box box : placement.getSubRegionBoxes(SubRegionPlacement.RequiredEnabled.PLACEMENT_ENABLED).values())
            {
                parts.add(ZoneBox.of(box));
            }

            Section section = manager.sectionOf(placement);
            Rule rule;

            if (section == null)
            {
                // Вне раскладки; у активного участка без схемы — его зона (старое поведение ручной зоны)
                rule = active != null && active.hasNoPlacements() && activeResolved != null
                        && activeResolved.scope().contains(placement)
                        ? activeRule
                        : (hideOutside ? Rule.NONE : Rule.ALL);
            }
            else if (section.hidden)
            {
                rule = Rule.NONE;
            }
            else if (section == active)
            {
                rule = activeRule;
            }
            else
            {
                rule = hideOther ? Rule.NONE : Rule.ALL;
            }

            entries.add(new Entry(parts, rule));
            rules.append(placement.getHashId()).append('=').append(rule.descriptor()).append(';');
        }

        return new ClipPlan(entries, rules.toString(), active != null ? active.id : null, activeZone, ++planCounter);
    }

    /**
     * Соседние зоны — те, что касаются или перекрываются хотя бы по всем трём осям с
     * зазором не больше одного блока. Только для них объемлющий бокс двух зон не сильно
     * больше их самих.
     */
    private static boolean adjacent(ZoneBox a, ZoneBox b)
    {
        return a.minX() <= b.maxX() + 1 && b.minX() <= a.maxX() + 1
                && a.minY() <= b.maxY() + 1 && b.minY() <= a.maxY() + 1
                && a.minZ() <= b.maxZ() + 1 && b.minZ() <= a.maxZ() + 1;
    }

    private static void beginTransition(ZoneBox previous, ZoneBox next)
    {
        endTransition();

        fadingBox = previous;
        transitionTicks = TRANSITION_TIMEOUT;
        pending.clear();

        forEachChunk(next, (chunkX, chunkZ) -> {
            if (schedule(chunkX, chunkZ))
            {
                pending.add(ChunkPos.asLong(chunkX, chunkZ));
            }
        });

        if (pending.isEmpty())
        {
            endTransition();
        }
    }

    private static void endTransition()
    {
        ZoneBox fading = fadingBox;

        if (fading == null)
        {
            return;
        }

        fadingBox = null;
        pending.clear();

        forEachChunk(fading, ZoneRenderClip::schedule);
    }

    /** Чанки, чьи части план не опознал, — на пересборку из тика, один раз на версию плана. */
    private static void flushMisses()
    {
        ClipPlan current = plan;

        if (misses.isEmpty() || current == null)
        {
            misses.clear();
            return;
        }

        if (flushedMissesVersion == current.version)
        {
            misses.clear();
            return;
        }

        flushedMissesVersion = current.version;

        for (long key : misses)
        {
            schedule(ChunkPos.getX(key), ChunkPos.getZ(key));
        }

        misses.clear();
    }

    public static void onChunkRebuilt(int chunkX, int chunkZ)
    {
        if (! pending.isEmpty())
        {
            pending.remove(ChunkPos.asLong(chunkX, chunkZ));
        }
    }

    private static boolean schedule(int chunkX, int chunkZ)
    {
        WorldSchematic world = SchematicWorldHandler.getSchematicWorld();
        Minecraft mc = Minecraft.getInstance();

        if (world == null || mc.level == null || ! world.getChunkSource().hasChunk(chunkX, chunkZ)
                || ! WorldUtils.isClientChunkLoaded(mc.level, chunkX, chunkZ))
        {
            return false;
        }

        world.scheduleChunkRenders(chunkX, chunkZ);

        return true;
    }

    private static void forEachChunk(ZoneBox box, ChunkAction action)
    {
        int minChunkX = box.minX() >> 4;
        int maxChunkX = box.maxX() >> 4;
        int minChunkZ = box.minZ() >> 4;
        int maxChunkZ = box.maxZ() >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; ++chunkX)
        {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; ++chunkZ)
            {
                action.accept(chunkX, chunkZ);
            }
        }
    }

    @FunctionalInterface
    private interface ChunkAction
    {
        void accept(int chunkX, int chunkZ);
    }

    /**
     * Из рабочего потока сборки чанка: {@code original} — бокс части (подрегион ∩ чанк), по нему ищется её
     * размещение; {@code clamped} — он же после обрезки слоями, его и режем. {@code null} — часть не рисуется.
     */
    @Nullable
    public static IntBoundingBox clip(IntBoundingBox original, IntBoundingBox clamped)
    {
        ClipPlan current = plan;

        if (current == null)
        {
            return clamped;
        }

        ZoneBox fading = fadingBox;
        boolean owned = false;
        IntBoundingBox result = null;

        for (Entry entry : current.entries)
        {
            if (! entry.owns(original))
            {
                continue;
            }

            owned = true;

            if (entry.rule.all)
            {
                return clamped;
            }

            for (ZoneBox zone : entry.rule.zones)
            {
                result = join(result, intersect(clamped, zone));
            }

            if (entry.rule.active && fading != null)
            {
                result = join(result, intersect(clamped, fading));
            }
        }

        if (! owned)
        {
            misses.add(ChunkPos.asLong(original.minX() >> 4, original.minZ() >> 4));
        }

        return result;
    }

    @Nullable
    private static IntBoundingBox join(@Nullable IntBoundingBox a, @Nullable IntBoundingBox b)
    {
        if (a == null)
        {
            return b;
        }

        if (b == null)
        {
            return a;
        }

        return IntBoundingBox.createProper(
                Math.min(a.minX(), b.minX()), Math.min(a.minY(), b.minY()), Math.min(a.minZ(), b.minZ()),
                Math.max(a.maxX(), b.maxX()), Math.max(a.maxY(), b.maxY()), Math.max(a.maxZ(), b.maxZ()));
    }

    @Nullable
    private static IntBoundingBox intersect(IntBoundingBox box, ZoneBox zone)
    {
        int minX = Math.max(box.minX(), zone.minX());
        int minY = Math.max(box.minY(), zone.minY());
        int minZ = Math.max(box.minZ(), zone.minZ());
        int maxX = Math.min(box.maxX(), zone.maxX());
        int maxY = Math.min(box.maxY(), zone.maxY());
        int maxZ = Math.min(box.maxZ(), zone.maxZ());

        if (minX > maxX || minY > maxY || minZ > maxZ)
        {
            return null;
        }

        return IntBoundingBox.createProper(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static boolean contains(BlockPos pos)
    {
        return contains(pos.getX(), pos.getY(), pos.getZ());
    }

    /** Точка видима: сущности, блок-сущности, луч. Без плана — да; вне всех размещений плана — нет. */
    public static boolean contains(int x, int y, int z)
    {
        ClipPlan current = plan;

        if (current == null)
        {
            return true;
        }

        ZoneBox fading = fadingBox;

        for (Entry entry : current.entries)
        {
            if (! entry.owns(x, y, z))
            {
                continue;
            }

            if (entry.rule.all)
            {
                return true;
            }

            for (ZoneBox zone : entry.rule.zones)
            {
                if (inside(zone, x, y, z))
                {
                    return true;
                }
            }

            if (entry.rule.active && fading != null && inside(fading, x, y, z))
            {
                return true;
            }
        }

        return false;
    }

    private static boolean inside(ZoneBox zone, int x, int y, int z)
    {
        return x >= zone.minX() && x <= zone.maxX()
                && y >= zone.minY() && y <= zone.maxY()
                && z >= zone.minZ() && z <= zone.maxZ();
    }

    public static boolean isClipping()
    {
        return plan != null;
    }
}
