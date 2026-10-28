package com.aell.zonefocus.zone;

import com.aell.zonefocus.config.ZoneFocusConfigs;
import com.aell.zonefocus.zone.model.Section;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Прогресс по зонам: статистика хранится по участку ({@code section.id}) и номеру зоны, объём каждой зоны —
 * {@link ZoneManager#volume} (то же, что считает Material HUD). Статистика участка живёт, пока не изменились его
 * зоны ({@link ZoneManager#sectionLayoutSignature}); пересборка участка сбрасывает её явно.
 */
@Environment(EnvType.CLIENT)
public final class ZoneScanner
{
    public record Stats(long total, long missing, boolean partial)
    {
        public int percent()
        {
            return this.total > 0 ? (int) ((this.total - this.missing) * 100L / this.total) : 100;
        }

        /** В зоне нет блоков схемы — «готово» о ней сказать нельзя. */
        public boolean hasData()
        {
            return this.total > 0;
        }
    }

    private static final class SectionStats
    {
        final String signature;
        final Stats[] stats;
        /** Объём каждой зоны на момент скана — по нему onBlockChanged узнаёт, чьи блоки изменились. */
        final List<List<ZoneBox>> volumes;

        SectionStats(String signature, int zones)
        {
            this.signature = signature;
            this.stats = new Stats[zones];
            this.volumes = new ArrayList<>(zones);

            for (int i = 0; i < zones; ++i)
            {
                this.volumes.add(List.of());
            }
        }
    }

    private static final ZoneScanner INSTANCE = new ZoneScanner();

    private final Map<String, SectionStats> bySection = new HashMap<>();
    /** Актуальные сигнатуры участков со статистикой — обновляются раз в тик. */
    private final Map<String, String> currentSignatures = new HashMap<>();

    private final Deque<String> queue = new ArrayDeque<>();
    private @Nullable String scanningId;
    private @Nullable SectionStats scanningStats;
    private List<ZoneBox> zones = List.of();
    private List<ZoneBox> currentBoxes = List.of();
    private int zoneIndex;
    private int boxIndex;
    private long positionIndex;
    private long zoneTotal;
    private long zoneMissing;
    private boolean zonePartial;

    public static ZoneScanner getInstance()
    {
        return INSTANCE;
    }

    public void invalidate()
    {
        this.bySection.clear();
        this.currentSignatures.clear();
        this.queue.clear();
        this.stopScanning();
    }

    /** Участок пересобран — его статистика недействительна. */
    public void invalidateSection(String sectionId)
    {
        this.bySection.remove(sectionId);
        this.currentSignatures.remove(sectionId);
        this.queue.remove(sectionId);

        if (sectionId.equals(this.scanningId))
        {
            this.stopScanning();
        }
    }

    private void stopScanning()
    {
        this.scanningId = null;
        this.scanningStats = null;
        this.zones = List.of();
        this.currentBoxes = List.of();
    }

    /** Посчитать активный участок. */
    public void requestScan()
    {
        Section section = ZoneManager.getInstance().activeSection();

        if (section != null)
        {
            this.requestScan(section);
        }
    }

    public void requestScan(Section section)
    {
        if (! section.id.equals(this.scanningId) && ! this.queue.contains(section.id))
        {
            this.queue.add(section.id);
        }
    }

    /** Посчитать все участки по порядку. */
    public void requestScanAll()
    {
        for (Section section : ZoneManager.getInstance().sections())
        {
            this.requestScan(section);
        }
    }

    public boolean isScanning()
    {
        return this.scanningId != null || ! this.queue.isEmpty();
    }

    public boolean isScanning(Section section)
    {
        return section.id.equals(this.scanningId) || this.queue.contains(section.id);
    }

    public int getScannedZones()
    {
        return this.scanningStats != null ? Math.min(this.zoneIndex, this.scanningStats.stats.length) : 0;
    }

    public int getTotalZones()
    {
        return this.scanningStats != null ? this.scanningStats.stats.length : 0;
    }

    // ===== Статистика =====

    private @Nullable SectionStats valid(Section section)
    {
        SectionStats stats = this.bySection.get(section.id);

        if (stats == null)
        {
            return null;
        }

        String current = this.currentSignatures.get(section.id);

        if (current == null)
        {
            current = ZoneManager.getInstance().sectionLayoutSignature(section);
            this.currentSignatures.put(section.id, current);
        }

        return stats.signature.equals(current) ? stats : null;
    }

    @Nullable
    public Stats getStats(Section section, int zoneIndex)
    {
        SectionStats stats = this.valid(section);

        if (stats == null || zoneIndex < 0 || zoneIndex >= stats.stats.length)
        {
            return null;
        }

        return stats.stats[zoneIndex];
    }

    public boolean hasStats(Section section)
    {
        SectionStats stats = this.valid(section);
        return stats != null && stats.stats.length > 0;
    }

    /** Фасад: активный участок. */
    @Nullable
    public Stats getStats(int zoneIndex)
    {
        Section section = ZoneManager.getInstance().activeSection();
        return section != null ? this.getStats(section, zoneIndex) : null;
    }

    public boolean hasStats()
    {
        Section section = ZoneManager.getInstance().activeSection();
        return section != null && this.hasStats(section);
    }

    // ===== Тик =====

    public void onClientTick(Minecraft mc)
    {
        ZoneManager manager = ZoneManager.getInstance();

        // Сигнатуры участков со статистикой — раз в тик, а не на каждый getStats из отрисовки
        this.currentSignatures.clear();

        for (String id : this.bySection.keySet())
        {
            Section section = manager.sectionById(id);

            if (section != null)
            {
                this.currentSignatures.put(id, manager.sectionLayoutSignature(section));
            }
        }

        if (this.scanningId == null && ! this.queue.isEmpty())
        {
            this.startNext(manager);
        }

        if (this.scanningId == null || this.scanningStats == null || mc.level == null)
        {
            return;
        }

        Section section = manager.sectionById(this.scanningId);
        WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();

        if (section == null || schematicWorld == null)
        {
            this.stopScanning();
            return;
        }

        boolean ignoreState = Configs.Generic.MATERIAL_LIST_IGNORE_STATE.getBooleanValue();
        long budget = ZoneFocusConfigs.SCAN_BLOCKS_PER_TICK.getIntegerValue();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int lastChunkX = Integer.MIN_VALUE;
        int lastChunkZ = Integer.MIN_VALUE;
        boolean chunkLoaded = false;

        while (budget > 0 && this.zoneIndex < this.zones.size())
        {
            if (this.currentBoxes.isEmpty() && this.boxIndex == 0 && this.positionIndex == 0)
            {
                this.currentBoxes = manager.volume(section, this.zoneIndex);
                this.scanningStats.volumes.set(this.zoneIndex, this.currentBoxes);
            }

            if (this.boxIndex >= this.currentBoxes.size())
            {
                this.scanningStats.stats[this.zoneIndex] = new Stats(this.zoneTotal, this.zoneMissing, this.zonePartial);
                ++this.zoneIndex;
                this.currentBoxes = List.of();
                this.boxIndex = 0;
                this.positionIndex = 0;
                this.zoneTotal = 0;
                this.zoneMissing = 0;
                this.zonePartial = false;
                continue;
            }

            ZoneBox box = this.currentBoxes.get(this.boxIndex);
            long volume = box.volume();
            int sizeX = box.sizeX();
            int sizeY = box.sizeY();

            while (budget > 0 && this.positionIndex < volume)
            {
                long index = this.positionIndex++;
                --budget;

                int dx = (int) (index % sizeX);
                int dy = (int) ((index / sizeX) % sizeY);
                int dz = (int) (index / ((long) sizeX * sizeY));
                pos.set(box.minX() + dx, box.minY() + dy, box.minZ() + dz);

                int chunkX = pos.getX() >> 4;
                int chunkZ = pos.getZ() >> 4;

                if (chunkX != lastChunkX || chunkZ != lastChunkZ)
                {
                    lastChunkX = chunkX;
                    lastChunkZ = chunkZ;
                    chunkLoaded = WorldUtils.isClientChunkLoaded(mc.level, chunkX, chunkZ);
                }

                BlockState stateSchematic = schematicWorld.getBlockState(pos);

                if (stateSchematic.isAir())
                {
                    continue;
                }

                ++this.zoneTotal;

                if (! chunkLoaded)
                {
                    this.zonePartial = true;
                    ++this.zoneMissing;
                    continue;
                }

                if (isMissing(stateSchematic, mc.level.getBlockState(pos), ignoreState))
                {
                    ++this.zoneMissing;
                }
            }

            if (this.positionIndex >= volume)
            {
                ++this.boxIndex;
                this.positionIndex = 0;
            }
        }

        if (this.zoneIndex >= this.zones.size())
        {
            this.stopScanning();
        }
    }

    private void startNext(ZoneManager manager)
    {
        while (! this.queue.isEmpty())
        {
            String id = this.queue.poll();
            Section section = manager.sectionById(id);

            if (section == null)
            {
                continue;
            }

            List<ZoneBox> boxes = manager.zoneBoxes(section);

            if (boxes.isEmpty())
            {
                this.bySection.remove(id);
                continue;
            }

            String signature = manager.sectionLayoutSignature(section);
            this.scanningStats = new SectionStats(signature, boxes.size());
            this.bySection.put(id, this.scanningStats);
            this.currentSignatures.put(id, signature);
            this.scanningId = id;
            this.zones = boxes;
            this.zoneIndex = 0;
            this.currentBoxes = List.of();
            this.boxIndex = 0;
            this.positionIndex = 0;
            this.zoneTotal = 0;
            this.zoneMissing = 0;
            this.zonePartial = false;
            return;
        }
    }

    public void onBlockChanged(BlockPos pos, BlockState previous, BlockState current)
    {
        if (this.bySection.isEmpty())
        {
            return;
        }

        WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();

        if (schematicWorld == null)
        {
            return;
        }

        BlockState stateSchematic = schematicWorld.getBlockState(pos);

        if (stateSchematic.isAir())
        {
            return;
        }

        boolean ignoreState = Configs.Generic.MATERIAL_LIST_IGNORE_STATE.getBooleanValue();
        boolean wasMissing = isMissing(stateSchematic, previous, ignoreState);
        boolean nowMissing = isMissing(stateSchematic, current, ignoreState);

        if (wasMissing == nowMissing)
        {
            return;
        }

        for (SectionStats section : this.bySection.values())
        {
            for (int i = 0; i < section.stats.length; ++i)
            {
                Stats stats = section.stats[i];

                if (stats == null || ! containsAny(section.volumes.get(i), pos))
                {
                    continue;
                }

                long missing = Math.max(0L, Math.min(stats.total(), stats.missing() + (nowMissing ? 1L : -1L)));
                section.stats[i] = new Stats(stats.total(), missing, stats.partial());
            }
        }
    }

    private static boolean containsAny(List<ZoneBox> boxes, BlockPos pos)
    {
        for (ZoneBox box : boxes)
        {
            if (box.contains(pos))
            {
                return true;
            }
        }

        return false;
    }

    private static boolean isMissing(BlockState stateSchematic, BlockState stateClient, boolean ignoreState)
    {
        return stateClient.isAir()
                || (stateClient != stateSchematic
                        && ! (ignoreState && stateClient.getBlock() == stateSchematic.getBlock()));
    }
}
