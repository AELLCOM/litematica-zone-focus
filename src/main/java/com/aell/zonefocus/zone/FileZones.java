package com.aell.zonefocus.zone;

import com.aell.zonefocus.zone.model.Section;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.util.data.json.JsonUtils;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import org.jetbrains.annotations.Nullable;

/**
 * Разбиение схемы-файла на зоны (с 0.0.8): сетка nx×ny×nz в осях самой схемы, без размещения — так его
 * делают в библиотеке Material HUD, прямо в превью. Размещение этой схемы получает сетку само, если у Zone
 * Focus нет участка с зонами; разбиение одиночного участка сеткой запоминается и для его файла — библиотека
 * показывает те же зоны. Отмена разбиения убирает и то и другое.
 *
 * <p>Зоны размещения живут в координатах мира, превью — в координатах схемы от минимального угла всех
 * регионов: {@link #toLocal} переводит обратно с учётом поворота и отражения размещения, поэтому номера
 * на превью совпадают с номерами в мире. Хранится в {@code config/zonefocus_file_zones.json} по
 * абсолютному пути файла.
 */
@Environment(EnvType.CLIENT)
public final class FileZones
{
    private static final String FILE_NAME = "zonefocus_file_zones.json";
    private static final Map<String, int[]> GRIDS = new LinkedHashMap<>();
    /** Размещения, которым сетка уже раздана в этой игре: отменили зоны в мире — заново не навязываем. */
    private static final Set<SchematicPlacement> APPLIED = Collections.newSetFromMap(new WeakHashMap<>());
    private static boolean loaded;

    private FileZones() {}

    public static String key(Path file)
    {
        return file.toAbsolutePath().normalize().toString();
    }

    public static @Nullable String key(@Nullable SchematicPlacement placement)
    {
        Path file = placement != null ? placement.getSchematicFile() : null;
        return file != null ? key(file) : null;
    }

    /** Сетка файла {nx, ny, nz} в осях схемы или {@code null}. */
    public static int @Nullable [] grid(String path)
    {
        load();
        int[] g = GRIDS.get(path);
        return g != null ? g.clone() : null;
    }

    public static void set(String path, int nx, int ny, int nz)
    {
        load();
        GRIDS.put(path, new int[] { Math.max(1, nx), Math.max(1, ny), Math.max(1, nz) });
        save();
    }

    public static void clear(String path)
    {
        load();

        if (GRIDS.remove(path) != null)
        {
            save();
        }
    }

    /** Повёрнутое на 90° размещение меняет местами оси X и Z схемы и мира. */
    public static boolean swapsXZ(SchematicPlacement placement)
    {
        Rotation r = placement.getRotation();
        return r == Rotation.CLOCKWISE_90 || r == Rotation.COUNTERCLOCKWISE_90;
    }

    /** Размещение разбили сеткой (в осях мира) — запомнить её для файла в осях схемы. */
    public static void remember(SchematicPlacement placement, int nx, int ny, int nz)
    {
        String path = key(placement);

        if (path != null)
        {
            boolean swap = swapsXZ(placement);
            set(path, swap ? nz : nx, ny, swap ? nx : nz);
            APPLIED.add(placement);
        }
    }

    public static void forget(@Nullable SchematicPlacement placement)
    {
        String path = key(placement);

        if (path != null)
        {
            clear(path);
        }
    }

    /** Сетка файла в осях мира этого размещения или {@code null}. */
    public static int @Nullable [] gridFor(SchematicPlacement placement)
    {
        String path = key(placement);
        int[] g = path != null ? grid(path) : null;

        if (g == null)
        {
            return null;
        }

        return swapsXZ(placement) ? new int[] { g[2], g[1], g[0] } : g;
    }

    /**
     * Тик: ни у одного участка нет зон с объёмом (то же условие, что раньше «зон нет»), а у файла включённого
     * размещения есть сетка — раздать её размещению новым участком.
     */
    public static void autoApply(ZoneManager manager)
    {
        if (manager.hasNonEmptySection())
        {
            return;
        }

        for (SchematicPlacement placement : ZoneManager.enabledPlacements())
        {
            if (APPLIED.contains(placement))
            {
                continue;
            }

            int[] g = gridFor(placement);

            if (g != null)
            {
                APPLIED.add(placement);
                manager.splitGrid(placement, g[0], g[1], g[2]);
                return;
            }
        }
    }

    /** Одиночный участок с размещением этого файла: сначала активный, затем с наименьшим номером; иначе {@code null}. */
    public static @Nullable Section singleSectionOf(String path)
    {
        ZoneManager manager = ZoneManager.getInstance();
        Section active = manager.activeSection();

        if (active != null && active.isSingle() && path.equals(singleFile(manager, active)))
        {
            return active;
        }

        for (Section section : manager.sections())
        {
            if (section.isSingle() && path.equals(singleFile(manager, section)))
            {
                return section;
            }
        }

        return null;
    }

    private static @Nullable String singleFile(ZoneManager manager, Section section)
    {
        SchematicPlacement placement = manager.resolve(section).first();
        return placement != null ? key(placement) : null;
    }

    /** Включённое размещение этого файла: то, у которого участок, иначе первое; {@code null} — не размещён. */
    public static @Nullable SchematicPlacement placementOf(String path)
    {
        Section section = singleSectionOf(path);
        SchematicPlacement zoned = section != null ? ZoneManager.getInstance().resolve(section).first() : null;

        if (zoned != null)
        {
            return zoned;
        }

        for (SchematicPlacement placement : ZoneManager.enabledPlacements())
        {
            if (path.equals(key(placement)))
            {
                return placement;
            }
        }

        return null;
    }

    /** Размещение этого файла, стоящее в одиночном участке с зонами, или {@code null}. */
    public static @Nullable SchematicPlacement zonedPlacement(String path)
    {
        Section section = singleSectionOf(path);
        ZoneManager manager = ZoneManager.getInstance();

        return section != null && ! manager.zoneBoxes(section).isEmpty() ? manager.resolve(section).first() : null;
    }

    /**
     * Сетка по габариту схемы {@code sizeX×sizeY×sizeZ} в её осях, от угла 0: тот же порядок, что у зон в
     * мире ({@link ZoneManager#slices}, Y внешний, потом Z, потом X).
     */
    public static int[][] localGrid(int sizeX, int sizeY, int sizeZ, int nx, int ny, int nz)
    {
        int[][] xs = ZoneManager.slices(0, Math.max(0, sizeX - 1), nx);
        int[][] ys = ZoneManager.slices(0, Math.max(0, sizeY - 1), ny);
        int[][] zs = ZoneManager.slices(0, Math.max(0, sizeZ - 1), nz);
        List<int[]> out = new ArrayList<>(xs.length * ys.length * zs.length);

        for (int[] y : ys)
        {
            for (int[] z : zs)
            {
                for (int[] x : xs)
                {
                    out.add(new int[] { x[0], y[0], z[0], x[1], y[1], z[1] });
                }
            }
        }

        return out.toArray(new int[0][]);
    }

    /** Минимальный угол всех регионов схемы в её координатах — от него считает снимок превью. */
    public static BlockPos enclosingMin(LitematicaSchematic schematic)
    {
        Map<String, BlockPos> positions = schematic.getAreaPositions();
        Map<String, BlockPos> sizes = schematic.getAreaSizes();
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;

        for (Map.Entry<String, BlockPos> area : positions.entrySet())
        {
            BlockPos size = sizes.get(area.getKey());

            if (size == null)
            {
                continue;
            }

            BlockPos pos = area.getValue();
            minX = Math.min(minX, pos.getX() + (size.getX() < 0 ? size.getX() + 1 : 0));
            minY = Math.min(minY, pos.getY() + (size.getY() < 0 ? size.getY() + 1 : 0));
            minZ = Math.min(minZ, pos.getZ() + (size.getZ() < 0 ? size.getZ() + 1 : 0));
        }

        return minX == Integer.MAX_VALUE ? BlockPos.ZERO : new BlockPos(minX, minY, minZ);
    }

    /** Коробки мира → коробки схемы от её минимального угла (обратный поворот и отражение размещения). */
    public static int[][] toLocal(SchematicPlacement placement, List<ZoneBox> boxes)
    {
        LitematicaSchematic schematic = placement.getSchematic();
        BlockPos min = schematic != null ? enclosingMin(schematic) : BlockPos.ZERO;
        BlockPos origin = placement.getOrigin();
        int[][] out = new int[boxes.size()][];

        for (int i = 0; i < boxes.size(); ++i)
        {
            ZoneBox b = boxes.get(i);
            BlockPos a = back(placement, origin, b.minX(), b.minY(), b.minZ()).subtract(min);
            BlockPos c = back(placement, origin, b.maxX(), b.maxY(), b.maxZ()).subtract(min);
            out[i] = new int[] { Math.min(a.getX(), c.getX()), Math.min(a.getY(), c.getY()), Math.min(a.getZ(), c.getZ()),
                    Math.max(a.getX(), c.getX()), Math.max(a.getY(), c.getY()), Math.max(a.getZ(), c.getZ()) };
        }

        return out;
    }

    /**
     * Точка мира → точка схемы. Litematica ставит блок схемы так: сначала отражение, потом поворот
     * ({@link PositionUtils#getTransformedBlockPos}); обратно — поворот назад, потом то же отражение
     * (оно само себе обратное).
     */
    static BlockPos back(SchematicPlacement placement, BlockPos origin, int x, int y, int z)
    {
        BlockPos relative = new BlockPos(x - origin.getX(), y - origin.getY(), z - origin.getZ());
        BlockPos unrotated = PositionUtils.getTransformedBlockPos(relative, net.minecraft.world.level.block.Mirror.NONE,
                PositionUtils.getReverseRotation(placement.getRotation()));
        return PositionUtils.getTransformedBlockPos(unrotated, placement.getMirror(), Rotation.NONE);
    }

    private static void load()
    {
        if (loaded)
        {
            return;
        }

        loaded = true;
        File file = new File(FabricLoader.getInstance().getConfigDir().toFile(), FILE_NAME);

        if (! file.isFile())
        {
            return;
        }

        try
        {
            JsonElement root = JsonUtils.parseJsonFile(file.toPath());

            if (root != null && root.isJsonObject())
            {
                for (Map.Entry<String, JsonElement> e : root.getAsJsonObject().entrySet())
                {
                    if (e.getValue().isJsonObject())
                    {
                        JsonObject g = e.getValue().getAsJsonObject();
                        GRIDS.put(e.getKey(), new int[] { Math.max(1, JsonUtils.getIntegerOrDefault(g, "nx", 1)),
                                Math.max(1, JsonUtils.getIntegerOrDefault(g, "ny", 1)),
                                Math.max(1, JsonUtils.getIntegerOrDefault(g, "nz", 1)) });
                    }
                }
            }
        }
        catch (RuntimeException e)
        {
            // Битый файл — начинаем с пустого
        }
    }

    private static void save()
    {
        JsonObject root = new JsonObject();

        for (Map.Entry<String, int[]> e : GRIDS.entrySet())
        {
            JsonObject g = new JsonObject();
            g.addProperty("nx", e.getValue()[0]);
            g.addProperty("ny", e.getValue()[1]);
            g.addProperty("nz", e.getValue()[2]);
            root.add(e.getKey(), g);
        }

        JsonUtils.writeJsonToFile(root, new File(FabricLoader.getInstance().getConfigDir().toFile(), FILE_NAME).toPath());
    }
}
