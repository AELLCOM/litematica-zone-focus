package com.aell.zonefocus.zone;

import com.aell.zonefocus.ZoneChat;
import com.aell.zonefocus.ZoneFocusModule;
import com.aell.zonefocus.compat.MaterialHudBridge;
import com.aell.zonefocus.compat.MaterialHudCompat;
import com.aell.zonefocus.config.ZoneFocusConfigs;
import com.aell.zonefocus.shaded.ui.Strings;
import com.aell.zonefocus.zone.model.Bounds;
import com.aell.zonefocus.zone.model.Layout;
import com.aell.zonefocus.zone.model.PlacementRef;
import com.aell.zonefocus.zone.model.Rule;
import com.aell.zonefocus.zone.model.Section;
import com.aell.zonefocus.zone.scope.PlacementMatcher;
import com.aell.zonefocus.zone.scope.PlacementMatcher.Resolved;
import com.aell.zonefocus.zone.scope.ZoneLayouter;
import com.aell.zonefocus.zone.scope.ZoneVolume;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.malilib.util.StringUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Раскладка текущего мира: участки (группа схем + правило разбиения) и их зоны. Старый API «одна зона на
 * мир» сохранён как фасад над активным участком — им пользуются экраны, мост к Material HUD и стенд.
 */
@Environment(EnvType.CLIENT)
public final class ZoneManager
{
    private static final ZoneManager INSTANCE = new ZoneManager();

    private static final int AUTO_NEXT_COOLDOWN = 40;

    private Layout layout = Layout.create();
    private int autoNextCooldown;
    private String lastScopeKey = "";

    /** Сопоставление участков с размещениями — на тик, пересчитывается при любой правке раскладки. */
    private Map<Section, Resolved> resolvedCache = Map.of();
    private int resolvedVersion = -1;
    private long resolvedTick = -1;
    private long tick;

    public static ZoneManager getInstance()
    {
        return INSTANCE;
    }

    // ===== Раскладка и участки =====

    public Layout getLayout()
    {
        return this.layout;
    }

    public List<Section> sections()
    {
        return this.layout.sections;
    }

    @Nullable
    public Section activeSection()
    {
        return this.layout.active();
    }

    public int getActiveSectionIndex()
    {
        return this.layout.hasSections() ? Math.floorMod(this.layout.activeSection, this.layout.sections.size()) : -1;
    }

    public boolean hasSections()
    {
        return this.layout.hasSections();
    }

    public Resolved resolve(Section section)
    {
        if (this.resolvedVersion != this.layout.version || this.resolvedTick != this.tick
                || ! this.resolvedCache.containsKey(section))
        {
            this.resolvedCache = PlacementMatcher.resolveAll(this.layout, enabledPlacements());
            this.resolvedVersion = this.layout.version;
            this.resolvedTick = this.tick;
        }

        Resolved resolved = this.resolvedCache.get(section);
        return resolved != null ? resolved : new Resolved(section, List.of(), Map.of(), List.of());
    }

    private void invalidateResolved()
    {
        this.resolvedVersion = -1;
    }

    public List<ZoneBox> zoneBoxes(Section section)
    {
        List<ZoneBox> boxes = ZoneLayouter.zones(this.resolve(section));
        section.clampZones(boxes.size());
        return boxes;
    }

    @Nullable
    public ZoneBox activeZone(Section section)
    {
        List<ZoneBox> boxes = this.zoneBoxes(section);
        return boxes.isEmpty() ? null : boxes.get(Math.floorMod(section.activeZone, boxes.size()));
    }

    /** Объём зоны участка — что считать, проверять и рисовать. */
    public List<ZoneBox> volume(Section section, int zoneIndex)
    {
        List<ZoneBox> boxes = this.zoneBoxes(section);

        if (boxes.isEmpty())
        {
            return List.of();
        }

        return ZoneVolume.of(this.resolve(section), boxes.get(Math.floorMod(zoneIndex, boxes.size())));
    }

    /** У участка есть хоть одна зона с непустым объёмом. */
    public boolean isNonEmpty(Section section)
    {
        List<ZoneBox> boxes = this.zoneBoxes(section);
        Resolved resolved = this.resolve(section);

        for (ZoneBox zone : boxes)
        {
            if (! ZoneVolume.of(resolved, zone).isEmpty())
            {
                return true;
            }
        }

        return false;
    }

    /** Есть не скрытый участок с непустым объёмом (тик, мост). */
    public boolean layoutHasZones()
    {
        for (Section section : this.layout.sections)
        {
            if (! section.hidden && this.isNonEmpty(section))
            {
                return true;
            }
        }

        return false;
    }

    /** Есть участок с непустым объёмом, скрытые тоже — старое условие {@code hasZones()} для autoApply. */
    public boolean hasNonEmptySection()
    {
        for (Section section : this.layout.sections)
        {
            if (this.isNonEmpty(section))
            {
                return true;
            }
        }

        return false;
    }

    public void setActiveSection(int index)
    {
        if (this.layout.hasSections())
        {
            this.layout.activeSection = Math.floorMod(index, this.layout.sections.size());
            this.afterChange(false);
        }
    }

    public void cycleSection(int direction)
    {
        int count = this.layout.sections.size();

        if (count == 0)
        {
            return;
        }

        int index = this.getActiveSectionIndex();

        for (int step = 0; step < count; ++step)
        {
            index = Math.floorMod(index + direction, count);

            if (! this.layout.sections.get(index).hidden)
            {
                this.setActiveSection(index);
                return;
            }
        }
    }

    public void renameSection(Section section, String name)
    {
        section.name = name == null ? "" : name.trim();
        this.afterChange(false);
    }

    public void setSectionHidden(Section section, boolean hidden)
    {
        section.hidden = hidden;

        if (hidden && section == this.activeSection())
        {
            this.moveActivityOff(section);
        }

        this.afterChange(false);
    }

    /** Активный участок пуст или скрыт — активность на ближайший непустой не скрытый, по кругу. */
    private boolean moveActivityOff(Section from)
    {
        int count = this.layout.sections.size();
        int start = this.layout.indexOf(from);

        for (int step = 1; step < count; ++step)
        {
            int index = Math.floorMod(start + step, count);
            Section candidate = this.layout.sections.get(index);

            if (! candidate.hidden && this.isNonEmpty(candidate))
            {
                this.layout.activeSection = index;
                return true;
            }
        }

        return false;
    }

    public void removeSection(int index)
    {
        if (index < 0 || index >= this.layout.sections.size())
        {
            return;
        }

        Section section = this.layout.sections.get(index);
        this.forgetFileGrid(section);
        this.layout.sections.remove(index);

        if (this.layout.activeSection > index)
        {
            --this.layout.activeSection;
        }

        this.afterChange(true);
    }

    /** Участок размещения или {@code null}. */
    @Nullable
    public Section sectionOf(SchematicPlacement placement)
    {
        if (! this.layout.hasSections())
        {
            return null;
        }

        this.resolve(this.layout.sections.getFirst());
        return PlacementMatcher.sectionOf(this.resolvedCache, placement);
    }

    private Section newSectionFor(@Nullable SchematicPlacement placement, Rule rule)
    {
        Section section = Section.create(rule);

        if (placement != null)
        {
            PlacementRef ref = PlacementRef.byName(placement.getName());
            ref.id = placement.getHashId();
            ref.file = FileZones.key(placement);
            ref.box = enclosingBox(placement);
            section.placements.add(ref);
            section.name = placement.getName();
        }

        this.layout.sections.add(section);
        this.layout.touch();
        this.invalidateResolved();
        return section;
    }

    private void activate(Section section)
    {
        int index = this.layout.indexOf(section);

        if (index >= 0)
        {
            this.layout.activeSection = index;
        }
    }

    // ===== Разбиение участков =====

    public void splitGrid(Section section, int nx, int ny, int nz)
    {
        section.rule = new Rule.Grid(nx, ny, nz);

        if (section.bounds == Bounds.COMBINED)
        {
            this.layout.touch();
            this.invalidateResolved();
            Resolved resolved = this.resolve(section);
            section.combinedBox = ZoneLayouter.unionOf(section, resolved);
            SchematicPlacement first = resolved.first();
            section.combinedOrigin = first != null ? first.getOrigin() : null;
        }
        else
        {
            section.combinedBox = null;
            section.combinedOrigin = null;
        }

        this.rebuilt(section);
        this.rememberFileGrid(section);
    }

    /** Габарит участка целиком (общий снимок или живое объединение схем); {@code null} — схем не найдено. */
    @Nullable
    public ZoneBox sectionBox(Section section)
    {
        return ZoneLayouter.sectionBox(section, this.resolve(section));
    }

    /**
     * Разрезы участка в блоках по осям {xs, ys, zs} от минимального угла {@link #sectionBox}: у сетки — её равные
     * доли, у «разрезов» — как есть; подрегионы и ручные коробки — {@code null}. Для линий на карте и превью.
     */
    public int @Nullable [][] cuts(Section section)
    {
        ZoneBox bounds = this.sectionBox(section);

        if (bounds == null)
        {
            return null;
        }

        Rule.Cuts cuts = switch (section.rule)
        {
            case Rule.Grid grid -> Rule.Cuts.of(grid, bounds);
            case Rule.Cuts c -> c.clamp(bounds);
            default -> null;
        };

        return cuts != null ? new int[][] { cuts.xs(), cuts.ys(), cuts.zs() } : null;
    }

    /**
     * Поставить разрезы (мышью на карте или превью). Число зон то же — активная зона и скрытия остаются; иначе
     * сбрасываются, как при любом разбиении. У группы габарит становится общим (снимок), сетка файла забывается:
     * разрезы в блоках ей не выразить.
     */
    public void setCuts(Section section, int[] xs, int[] ys, int[] zs)
    {
        this.forgetFileGrid(section);
        ZoneBox bounds = this.sectionBox(section);

        if (bounds == null)
        {
            return;
        }

        if (section.isGroup() && section.bounds != Bounds.COMBINED)
        {
            section.bounds = Bounds.COMBINED;
            Resolved resolved = this.resolve(section);
            section.combinedBox = ZoneLayouter.unionOf(section, resolved);
            SchematicPlacement first = resolved.first();
            section.combinedOrigin = first != null ? first.getOrigin() : null;
        }

        int before = this.zoneBoxes(section).size();
        Rule.Cuts next = new Rule.Cuts(xs, ys, zs).clamp(bounds);

        if (next.equals(section.rule))
        {
            return;
        }

        section.rule = next;
        this.layout.touch();
        this.invalidateResolved();
        int after = this.zoneBoxes(section).size();

        if (after != before)
        {
            this.rebuilt(section);
            return;
        }

        ZoneScanner.getInstance().invalidateSection(section.id);
        this.afterChange(true);
    }

    /** Подвинуть одну плоскость разреза на {@code position} (блоков от края габарита по оси {@code axis}). */
    public void moveCut(Section section, int axis, int index, int position)
    {
        int[][] all = this.cuts(section);
        ZoneBox bounds = this.sectionBox(section);

        if (all == null || bounds == null || index < 0 || index >= all[axis].length)
        {
            return;
        }

        int size = axis == 0 ? bounds.sizeX() : axis == 1 ? bounds.sizeY() : bounds.sizeZ();
        int lower = index > 0 ? all[axis][index - 1] + 1 : 1;
        int upper = index + 1 < all[axis].length ? all[axis][index + 1] - 1 : size - 1;
        int[] moved = all[axis].clone();
        moved[index] = Math.max(lower, Math.min(upper, position));
        all[axis] = moved;
        this.setCuts(section, all[0], all[1], all[2]);
    }

    public void splitBySubRegions(Section section)
    {
        this.forgetFileGrid(section);
        section.rule = Rule.SubRegions.INSTANCE;
        section.combinedBox = null;
        section.combinedOrigin = null;
        this.rebuilt(section);
    }

    public void setManualBoxes(Section section, List<ZoneBox> boxes)
    {
        this.forgetFileGrid(section);
        Resolved resolved = this.resolve(section);
        SchematicPlacement base = resolved.first();
        PlacementRef baseRef = section.firstPlacement();
        section.rule = new Rule.Manual(boxes, baseRef, base != null ? base.getOrigin() : null);
        section.combinedBox = null;
        section.combinedOrigin = null;
        this.rebuilt(section);
    }

    /** Коробки участка в текущих координатах мира (сдвиг базы «запечён»). */
    public List<ZoneBox> manualBoxes(Section section)
    {
        return section.rule instanceof Rule.Manual ? this.zoneBoxes(section) : List.of();
    }

    public void addManualBox(Section section, ZoneBox box)
    {
        List<ZoneBox> boxes = new ArrayList<>(this.manualBoxes(section));
        boxes.add(box);
        this.setManualBoxes(section, boxes);
        section.activeZone = boxes.size() - 1;
        this.afterChange(false);
    }

    public void removeManualBox(Section section, int index)
    {
        List<ZoneBox> boxes = new ArrayList<>(this.manualBoxes(section));

        if (index < 0 || index >= boxes.size())
        {
            return;
        }

        boxes.remove(index);

        if (boxes.isEmpty())
        {
            this.removeSection(this.layout.indexOf(section));
            return;
        }

        this.setManualBoxes(section, boxes);
    }

    /** Правило или состав участка изменились: активная зона и скрытия сброшены, участок активен, статистика сброшена. */
    private void rebuilt(Section section)
    {
        section.resetZones();
        this.activate(section);
        ZoneScanner.getInstance().invalidateSection(section.id);
        this.afterChange(true);
        // Зона под игроком — только внутри пересобранного участка: он только что стал активным, и уводить
        // активность в соседний участок из-за того, что игрок стоит в его зоне, нельзя
        this.selectZoneAtPlayer(section);
    }

    /** Первая не скрытая зона участка, содержащая игрока, становится активной; участок при этом не меняется. */
    private void selectZoneAtPlayer(Section section)
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null)
        {
            return;
        }

        BlockPos pos = mc.player.blockPosition();
        List<ZoneBox> boxes = this.zoneBoxes(section);

        for (int i = 0; i < boxes.size(); ++i)
        {
            if (! section.hiddenZones.contains(i) && boxes.get(i).contains(pos))
            {
                if (section.activeZone != i)
                {
                    section.activeZone = i;
                    this.afterChange(false);
                }

                return;
            }
        }
    }

    private void rememberFileGrid(Section section)
    {
        if (section.isSingle() && section.rule instanceof Rule.Grid grid)
        {
            SchematicPlacement placement = this.resolve(section).first();

            if (placement != null)
            {
                FileZones.remember(placement, grid.nx(), grid.ny(), grid.nz());
            }
        }
    }

    private void forgetFileGrid(Section section)
    {
        if (section.isSingle() && section.rule instanceof Rule.Grid)
        {
            FileZones.forget(this.resolve(section).first());
        }
    }

    // ===== Фасад: активный участок как одна раскладка =====

    /** Снимок активного участка в старом виде; только чтение. */
    public ZoneState getState()
    {
        ZoneState state = new ZoneState();
        Section section = this.activeSection();

        if (section == null)
        {
            return state;
        }

        state.kind = section.rule.kind();
        state.activeIndex = this.getActiveIndex();
        state.hidden.addAll(section.hiddenZones);
        state.showHidden = section.showHidden;
        Resolved resolved = this.resolve(section);
        SchematicPlacement first = resolved.first();
        PlacementRef ref = section.firstPlacement();
        state.placementName = first != null ? first.getName() : ref != null ? ref.name : "";

        switch (section.rule)
        {
            case Rule.Grid grid ->
            {
                state.nx = grid.nx();
                state.ny = grid.ny();
                state.nz = grid.nz();
            }
            case Rule.Cuts cuts ->
            {
                state.nx = cuts.nx();
                state.ny = cuts.ny();
                state.nz = cuts.nz();
            }
            case Rule.Manual manual ->
            {
                ZoneBox box = this.activeZone(section);

                if (box != null)
                {
                    state.manualMin = box.min();
                    state.manualMax = box.max();
                }

                state.manualOrigin = first != null ? first.getOrigin() : manual.origin();
            }
            default -> { }
        }

        return state;
    }

    public boolean hasZones()
    {
        Section section = this.activeSection();
        return section != null && ! this.zoneBoxes(section).isEmpty();
    }

    public ZoneState.Kind getKind()
    {
        Section section = this.activeSection();
        return section != null ? section.rule.kind() : ZoneState.Kind.NONE;
    }

    public int getActiveIndex()
    {
        Section section = this.activeSection();

        if (section == null)
        {
            return 0;
        }

        int count = this.zoneBoxes(section).size();
        return count > 0 ? Math.floorMod(section.activeZone, count) : 0;
    }

    public int getZoneCount()
    {
        return this.zoneBoxes().size();
    }

    public boolean isHidden(int index)
    {
        Section section = this.activeSection();
        return section != null && section.hiddenZones.contains(index);
    }

    public int getHiddenCount()
    {
        Section section = this.activeSection();

        if (section == null)
        {
            return 0;
        }

        int count = this.getZoneCount();
        int hidden = 0;

        for (int index : section.hiddenZones)
        {
            if (index < count)
            {
                ++hidden;
            }
        }

        return hidden;
    }

    public void toggleHidden(int index)
    {
        Section section = this.activeSection();

        if (section == null || index < 0 || index >= this.getZoneCount())
        {
            return;
        }

        if (! section.hiddenZones.remove(index))
        {
            section.hiddenZones.add(index);

            if (index == this.getActiveIndex())
            {
                this.cycleZone(1);
            }
        }

        this.afterChange(false);
    }

    public void showAllZones()
    {
        Section section = this.activeSection();

        if (section != null)
        {
            section.hiddenZones.clear();
            this.afterChange(false);
        }
    }

    public boolean isShowHidden()
    {
        Section section = this.activeSection();
        return section != null && section.showHidden;
    }

    public void toggleShowHidden()
    {
        Section section = this.activeSection();

        if (section != null)
        {
            section.showHidden = ! section.showHidden;
            this.afterChange(false);
        }
    }

    public List<Integer> listedIndices()
    {
        Section section = this.activeSection();
        int count = this.getZoneCount();
        List<Integer> result = new ArrayList<>(count);

        for (int i = 0; i < count; ++i)
        {
            if (section.showHidden || ! section.hiddenZones.contains(i))
            {
                result.add(i);
            }
        }

        return result;
    }

    /** Размещение одиночного активного участка; групповой или без схемы — {@code null}. */
    @Nullable
    public SchematicPlacement getPlacement()
    {
        Section section = this.activeSection();

        if (section == null)
        {
            return null;
        }

        Resolved resolved = this.resolve(section);

        if (section.isSingle())
        {
            return resolved.first();
        }

        // Ручная зона без схемы со старых версий: ссылка без ключей и единственное включённое размещение
        if (section.hasNoPlacements() && section.rule instanceof Rule.Manual manual && manual.base() != null
                && manual.base().isBlank())
        {
            return PlacementMatcher.find(manual.base(), enabledPlacements());
        }

        return null;
    }

    public static List<SchematicPlacement> enabledPlacements()
    {
        List<SchematicPlacement> result = new ArrayList<>();

        for (SchematicPlacement placement : DataManager.getSchematicPlacementManager().getAllSchematicsPlacements())
        {
            if (placement.isEnabled())
            {
                result.add(placement);
            }
        }

        return result;
    }

    public List<ZoneBox> zoneBoxes()
    {
        Section section = this.activeSection();
        return section != null ? this.zoneBoxes(section) : List.of();
    }

    @Nullable
    public ZoneBox getActiveZone()
    {
        Section section = this.activeSection();
        return section != null ? this.activeZone(section) : null;
    }

    public List<Box> getCountBoxes()
    {
        Section section = this.activeSection();

        if (section == null || section.hidden)
        {
            return List.of();
        }

        List<Box> result = new ArrayList<>();

        for (ZoneBox box : this.volume(section, section.activeZone))
        {
            result.add(box.toLitematicaBox("zonefocus"));
        }

        return result;
    }

    @Nullable
    public static ZoneBox enclosingBox(SchematicPlacement placement)
    {
        ZoneBox result = null;

        for (Box box : placement.getSubRegionBoxes(SubRegionPlacement.RequiredEnabled.PLACEMENT_ENABLED).values())
        {
            ZoneBox current = ZoneBox.of(box);
            result = result == null ? current : result.union(current);
        }

        return result;
    }

    /** Раскладка отрезка на n долей; используется и диаграммой экрана деления. */
    public static int[][] slices(int min, int max, int n)
    {
        int length = max - min + 1;
        n = Math.max(1, Math.min(n, length));
        int base = length / n;
        int remainder = length % n;
        int[][] result = new int[n][2];
        int start = min;

        for (int i = 0; i < n; ++i)
        {
            int size = base + (i < remainder ? 1 : 0);
            result[i][0] = start;
            result[i][1] = start + size - 1;
            start += size;
        }

        return result;
    }

    /** Участок размещения для старых вызовов: групповой — не трогаем ({@code null}), иначе его или новый. */
    @Nullable
    private Section sectionForFacade(SchematicPlacement placement, Rule rule)
    {
        Section section = this.sectionOf(placement);

        if (section == null)
        {
            return this.newSectionFor(placement, rule);
        }

        if (section.isGroup())
        {
            this.noticeInGroup(section);
            return null;
        }

        return section;
    }

    private void noticeInGroup(Section section)
    {
        ZoneChat.notice(Component.literal("§e"
                + StringUtils.translate("zonefocus.message.in_group", this.layout.indexOf(section) + 1)));
    }

    public void splitGrid(SchematicPlacement placement, int nx, int ny, int nz)
    {
        Section section = this.sectionForFacade(placement, new Rule.Grid(nx, ny, nz));

        if (section != null)
        {
            this.splitGrid(section, nx, ny, nz);
        }
    }

    public void splitBySize(SchematicPlacement placement, int sizeX, int sizeY, int sizeZ)
    {
        ZoneBox bounds = enclosingBox(placement);

        if (bounds == null)
        {
            return;
        }

        this.splitGrid(placement,
                ceilDiv(bounds.sizeX(), Math.max(1, sizeX)),
                ceilDiv(bounds.sizeY(), Math.max(1, sizeY)),
                ceilDiv(bounds.sizeZ(), Math.max(1, sizeZ)));
    }

    public static int ceilDiv(int value, int divisor)
    {
        return Math.max(1, (value + divisor - 1) / divisor);
    }

    public void splitBySubRegions(SchematicPlacement placement)
    {
        Section section = this.sectionForFacade(placement, Rule.SubRegions.INSTANCE);

        if (section != null)
        {
            this.splitBySubRegions(section);
        }
    }

    /** Одна ручная коробка: заменить коробки активного Manual-участка либо создать новый участок. */
    public void setManualBox(BlockPos corner1, BlockPos corner2)
    {
        ZoneBox box = new ZoneBox(corner1, corner2);
        SchematicPlacement placement = this.nearestPlacement(box);

        if (ZoneFocusConfigs.TOOL_CLAMP_TO_SCHEMATIC.getBooleanValue() && placement != null)
        {
            ZoneBox bounds = enclosingBox(placement);

            if (bounds != null)
            {
                ZoneBox clamped = box.intersect(bounds);

                if (clamped != null)
                {
                    box = clamped;
                }
            }
        }

        Section section = this.activeSection();

        if (section == null || ! (section.rule instanceof Rule.Manual))
        {
            section = this.newSectionFor(placement, new Rule.Manual(List.of(box), null, null));
        }

        this.setManualBoxes(section, List.of(box));
    }

    /** Разметка мышью: коробка добавляется в активный Manual-участок новой зоной, иначе — новый Manual-участок. */
    public void addToolBox(BlockPos corner1, BlockPos corner2)
    {
        ZoneBox box = this.clampToSchematic(new ZoneBox(corner1, corner2));
        Section section = this.activeSection();

        if (section != null && section.rule instanceof Rule.Manual && ! section.hidden)
        {
            this.addManualBox(section, box);
            return;
        }

        this.setManualBox(corner1, corner2);
    }

    /** Колесо над гранью: заменить активную коробку активного Manual-участка. */
    public void setActiveManualBox(ZoneBox box)
    {
        Section section = this.activeSection();

        if (section == null || ! (section.rule instanceof Rule.Manual))
        {
            return;
        }

        List<ZoneBox> boxes = new ArrayList<>(this.manualBoxes(section));

        if (boxes.isEmpty())
        {
            return;
        }

        int index = Math.floorMod(section.activeZone, boxes.size());
        boxes.set(index, this.clampToSchematic(box));
        this.setManualBoxes(section, boxes);
        section.activeZone = index;
        this.afterChange(false);
    }

    private ZoneBox clampToSchematic(ZoneBox box)
    {
        SchematicPlacement placement = this.nearestPlacement(box);

        if (ZoneFocusConfigs.TOOL_CLAMP_TO_SCHEMATIC.getBooleanValue() && placement != null)
        {
            ZoneBox bounds = enclosingBox(placement);
            ZoneBox clamped = bounds != null ? box.intersect(bounds) : null;

            if (clamped != null)
            {
                return clamped;
            }
        }

        return box;
    }

    /** Зона из выделения Litematica — Manual-участок этого размещения или новый. */
    public void zoneFromSelection(@Nullable SchematicPlacement placement, ZoneBox box)
    {
        Section section = placement != null ? this.sectionOf(placement) : null;

        if (section != null && section.isGroup())
        {
            this.noticeInGroup(section);
            return;
        }

        if (section == null || ! (section.rule instanceof Rule.Manual))
        {
            section = this.newSectionFor(placement, new Rule.Manual(List.of(box), null, null));
        }

        this.setManualBoxes(section, List.of(box));
    }

    @Nullable
    private SchematicPlacement nearestPlacement(ZoneBox box)
    {
        List<SchematicPlacement> enabled = enabledPlacements();

        for (SchematicPlacement placement : enabled)
        {
            ZoneBox bounds = enclosingBox(placement);

            if (bounds != null && bounds.intersect(box) != null)
            {
                return placement;
            }
        }

        return enabled.size() == 1 ? enabled.getFirst() : null;
    }

    public void setActiveIndex(int index)
    {
        Section section = this.activeSection();
        int count = this.getZoneCount();

        if (section != null && count > 0)
        {
            section.activeZone = Math.floorMod(index, count);
            this.afterChange(false);
        }
    }

    public void setActiveZone(Section section, int index)
    {
        int count = this.zoneBoxes(section).size();

        if (count > 0)
        {
            section.activeZone = Math.floorMod(index, count);
            this.activate(section);
            this.afterChange(false);
        }
    }

    public void cycleZone(int direction)
    {
        Section section = this.activeSection();
        int count = this.getZoneCount();

        if (section == null || count == 0)
        {
            return;
        }

        int index = this.getActiveIndex();
        boolean across = ZoneFocusConfigs.CYCLE_ACROSS_SECTIONS.getBooleanValue() && this.layout.sections.size() > 1;

        for (int step = 0; step < count; ++step)
        {
            index = Math.floorMod(index + direction, count);

            // С последней зоны по настройке уходим в следующий участок, а не по кругу
            if (across && ((direction > 0 && index <= this.getActiveIndex()) || (direction < 0 && index >= this.getActiveIndex())))
            {
                this.cycleSectionToEdge(direction);
                return;
            }

            if (! section.hiddenZones.contains(index))
            {
                this.setActiveIndex(index);
                return;
            }
        }
    }

    /** Следующий (или предыдущий) не скрытый участок, и в нём первая (или последняя) не скрытая зона. */
    private void cycleSectionToEdge(int direction)
    {
        int count = this.layout.sections.size();
        int start = this.getActiveSectionIndex();

        for (int step = 1; step <= count; ++step)
        {
            Section section = this.layout.sections.get(Math.floorMod(start + direction * step, count));

            if (section.hidden)
            {
                continue;
            }

            int zones = this.zoneBoxes(section).size();

            for (int i = 0; i < zones; ++i)
            {
                int zone = direction > 0 ? i : zones - 1 - i;

                if (! section.hiddenZones.contains(zone))
                {
                    section.activeZone = zone;
                    this.activate(section);
                    this.afterChange(false);
                    return;
                }
            }
        }
    }

    /** «Отменить разбиение» — вся раскладка. */
    public void clear()
    {
        for (Section section : this.layout.sections)
        {
            this.forgetFileGrid(section);
        }

        this.layout.clear();
        this.afterChange(true);
    }

    private void afterChange(boolean resetStats)
    {
        this.layout.touch();
        this.invalidateResolved();

        if (resetStats)
        {
            ZoneScanner.getInstance().invalidate();
        }

        this.autoNextCooldown = AUTO_NEXT_COOLDOWN;
        if (MaterialHudCompat.isIntegrated())
        {
            MaterialHudBridge.markDirty();
        }
        ZoneStore.save(this.layout, this.lastScopeKey);
    }

    public void onClientTick(Minecraft mc)
    {
        if (mc.level == null || mc.player == null)
        {
            return;
        }

        ++this.tick;
        String scopeKey = ZoneStore.scopeKey(mc);

        if (! scopeKey.equals(this.lastScopeKey))
        {
            this.lastScopeKey = scopeKey;
            this.layout = ZoneStore.load(scopeKey);
            this.invalidateResolved();
            ZoneScanner.getInstance().invalidate();

            if (MaterialHudCompat.isIntegrated())
            {
                MaterialHudBridge.markDirty();
            }
        }

        ZoneRenderClip.update();

        if (ZoneFocusModule.isEnabled())
        {
            FileZones.autoApply(this);
        }

        if (! ZoneFocusModule.isEnabled() || ! this.layout.hasSections())
        {
            return;
        }

        // Активный участок пуст или скрыт — уходим на ближайший живой
        Section active = this.activeSection();

        if (active != null && (active.hidden || ! this.isNonEmpty(active)) && this.moveActivityOff(active))
        {
            this.afterChange(false);
        }

        if (this.autoNextCooldown > 0)
        {
            --this.autoNextCooldown;
        }

        if (ZoneFocusConfigs.AUTO_ZONE_BY_POSITION.getBooleanValue())
        {
            this.selectZoneAtPlayer();
        }
        else if (ZoneFocusConfigs.AUTO_NEXT_WHEN_DONE.getBooleanValue() && this.autoNextCooldown == 0
                && MaterialHudCompat.isIntegrated() && MaterialHudBridge.isCountingZone()
                && MaterialHudBridge.blocksMissing() == 0L && this.getZoneCount() > 1)
        {
            this.cycleZone(1);
        }

        ZoneScanner.getInstance().onClientTick(mc);
    }

    /**
     * Зона под игроком: если активная уже содержит его — ничего (без дребезга между наложенными зонами);
     * иначе сначала зоны активного участка, затем остальные не скрытые участки по порядку.
     */
    public void selectZoneAtPlayer()
    {
        Minecraft mc = Minecraft.getInstance();
        Section active = this.activeSection();

        if (mc.player == null || active == null)
        {
            return;
        }

        BlockPos pos = mc.player.blockPosition();
        ZoneBox current = this.activeZone(active);

        if (current != null && current.contains(pos))
        {
            return;
        }

        int count = this.layout.sections.size();
        int start = this.getActiveSectionIndex();

        for (int step = 0; step < count; ++step)
        {
            Section section = this.layout.sections.get(Math.floorMod(start + step, count));

            if (section.hidden)
            {
                continue;
            }

            List<ZoneBox> boxes = this.zoneBoxes(section);

            for (int i = 0; i < boxes.size(); ++i)
            {
                if (! section.hiddenZones.contains(i) && boxes.get(i).contains(pos))
                {
                    section.activeZone = i;
                    this.activate(section);
                    this.afterChange(false);
                    return;
                }
            }
        }
    }

    public String getLabel()
    {
        Section section = this.activeSection();
        int count = this.getZoneCount();

        if (section == null)
        {
            return "";
        }

        return switch (section.rule)
        {
            case Rule.Grid ignored -> StringUtils.translate("zonefocus.hud.zone_of", this.getActiveIndex() + 1, count);
            case Rule.Cuts ignored -> StringUtils.translate("zonefocus.hud.zone_of", this.getActiveIndex() + 1, count);
            case Rule.SubRegions ignored -> StringUtils.translate("zonefocus.hud.subregion_of", this.getActiveIndex() + 1, count);
            case Rule.Manual ignored -> count > 1
                    ? StringUtils.translate("zonefocus.hud.zone_of", this.getActiveIndex() + 1, count)
                    : Strings.plain("zonefocus.hud.zone");
        };
    }

    public List<String> getFooterLines()
    {
        ZoneBox zone = this.getActiveZone();

        if (zone == null)
        {
            return List.of();
        }

        return List.of(StringUtils.translate("zonefocus.hud.zone_size",
                zone.sizeX(), zone.sizeY(), zone.sizeZ()));
    }

    /** Сигнатура объёма активной зоны: всё, от чего зависит, что считает Material HUD и проверка. */
    public String getSignature()
    {
        Section section = this.activeSection();

        if (section == null)
        {
            return "-";
        }

        return this.sectionSignature(section);
    }

    public String sectionSignature(Section section)
    {
        ZoneBox zone = this.activeZone(section);
        Resolved resolved = this.resolve(section);
        StringBuilder builder = new StringBuilder(section.id).append(':').append(section.hidden ? 'h' : '-')
                .append(':').append(section.rule.toJson()).append(':').append(section.bounds).append(':');

        for (SchematicPlacement placement : resolved.scope())
        {
            builder.append(placement.getHashId()).append('@').append(placement.getOrigin())
                    .append(placement.getRotation()).append(placement.getMirror()).append(';');
        }

        builder.append(':').append(section.activeZone).append(':').append(zone == null ? "-" : zone.signature());
        builder.append(':').append(zone != null && ZoneVolume.of(resolved, zone).isEmpty() ? "empty" : "ok");

        return builder.toString();
    }

    /** Сигнатура всей раскладки — для обрезки и сканера. */
    public String layoutSignature()
    {
        StringBuilder builder = new StringBuilder().append(this.getActiveSectionIndex()).append(':');

        for (Section section : this.layout.sections)
        {
            builder.append(this.sectionSignature(section)).append('|');
        }

        builder.append("::");

        for (SchematicPlacement placement : enabledPlacements())
        {
            builder.append(placement.getHashId()).append('@').append(placement.getOrigin())
                    .append(placement.getRotation()).append(placement.getMirror())
                    .append(placement.matchesRequirement(SubRegionPlacement.RequiredEnabled.RENDERING_ENABLED) ? 'r' : '-')
                    .append(';');
        }

        return builder.toString();
    }

    /** Сигнатура зон участка без состояния — ключ статистики сканера. */
    public String sectionLayoutSignature(Section section)
    {
        StringBuilder builder = new StringBuilder(section.id).append(':').append(section.rule.toJson());

        for (ZoneBox box : this.zoneBoxes(section))
        {
            builder.append('|').append(box.signature());
        }

        return builder.toString();
    }

    /** Участок по id — для статистики и экранов. */
    @Nullable
    public Section sectionById(String id)
    {
        return this.layout.byId(id);
    }
}
