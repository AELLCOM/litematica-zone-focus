package com.aell.zonefocus.zone.scope;

import com.aell.zonefocus.zone.ZoneBox;
import com.aell.zonefocus.zone.ZoneManager;
import com.aell.zonefocus.zone.model.Bounds;
import com.aell.zonefocus.zone.model.PlacementRef;
import com.aell.zonefocus.zone.model.Rule;
import com.aell.zonefocus.zone.model.Section;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.selection.Box;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Зоны участка по его правилу. Порядок зон постоянный: для сетки — размещение за размещением, внутри Y→Z→X
 * (так же нумерует превью библиотеки Material HUD); для подрегионов — по размещениям, внутри по имени.
 */
@Environment(EnvType.CLIENT)
public final class ZoneLayouter
{
    private ZoneLayouter() {}

    public static List<ZoneBox> zones(PlacementMatcher.Resolved resolved)
    {
        Section section = resolved.section();

        return switch (section.rule)
        {
            case Rule.Grid grid -> grid(section, resolved, grid);
            case Rule.Cuts cuts -> cuts(section, resolved, cuts);
            case Rule.SubRegions ignored -> subRegions(resolved);
            case Rule.Manual manual -> manual(resolved, manual);
        };
    }

    /** Габарит участка целиком: снимок общего габарита или живое объединение всех его схем. */
    public static @Nullable ZoneBox sectionBox(Section section, PlacementMatcher.Resolved resolved)
    {
        return section.bounds == Bounds.COMBINED ? combinedBox(section, resolved) : unionOf(section, resolved);
    }

    /** Разрезы в блоках: отрезки между соседними плоскостями по каждой оси, порядок зон Y→Z→X, как у сетки. */
    private static List<ZoneBox> cuts(Section section, PlacementMatcher.Resolved resolved, Rule.Cuts cuts)
    {
        List<ZoneBox> out = new ArrayList<>();
        ZoneBox bounds = sectionBox(section, resolved);

        if (bounds == null)
        {
            return out;
        }

        int[][] xs = segments(bounds.minX(), bounds.sizeX(), cuts.xs());
        int[][] ys = segments(bounds.minY(), bounds.sizeY(), cuts.ys());
        int[][] zs = segments(bounds.minZ(), bounds.sizeZ(), cuts.zs());

        for (int[] y : ys)
        {
            for (int[] z : zs)
            {
                for (int[] x : xs)
                {
                    out.add(new ZoneBox(x[0], y[0], z[0], x[1], y[1], z[1]));
                }
            }
        }

        return out;
    }

    /** Отрезки [начало, конец] по оси от {@code min} длиной {@code size}, разрезанные в смещениях {@code cuts}. */
    public static int[][] segments(int min, int size, int[] cuts)
    {
        List<int[]> out = new ArrayList<>();
        int start = 0;

        for (int c : cuts)
        {
            if (c <= start || c >= size)
            {
                continue;
            }

            out.add(new int[] { min + start, min + c - 1 });
            start = c;
        }

        out.add(new int[] { min + start, min + size - 1 });
        return out.toArray(new int[0][]);
    }

    /** Габарит ссылки: живой, если размещение найдено, иначе снимок — зоны отсутствующей схемы остаются в нумерации. */
    private static @Nullable ZoneBox boxOf(PlacementRef ref, PlacementMatcher.Resolved resolved)
    {
        SchematicPlacement placement = resolved.of(ref);
        ZoneBox live = placement != null ? ZoneManager.enclosingBox(placement) : null;
        return live != null ? live : ref.box;
    }

    private static List<ZoneBox> grid(Section section, PlacementMatcher.Resolved resolved, Rule.Grid grid)
    {
        List<ZoneBox> out = new ArrayList<>();

        if (section.bounds == Bounds.COMBINED)
        {
            ZoneBox bounds = combinedBox(section, resolved);

            if (bounds != null)
            {
                gridInto(out, bounds, grid);
            }

            return out;
        }

        for (PlacementRef ref : section.placements)
        {
            ZoneBox bounds = boxOf(ref, resolved);

            if (bounds != null)
            {
                gridInto(out, bounds, grid);
            }
        }

        return out;
    }

    /** Общий габарит: снимок при разбиении, сдвинутый вместе с первым размещением; снимка нет — объединение живых. */
    public static @Nullable ZoneBox combinedBox(Section section, PlacementMatcher.Resolved resolved)
    {
        if (section.combinedBox != null)
        {
            SchematicPlacement first = resolved.first();

            if (first != null && section.combinedOrigin != null)
            {
                BlockPos delta = first.getOrigin().subtract(section.combinedOrigin);
                return delta.equals(BlockPos.ZERO) ? section.combinedBox : section.combinedBox.offset(delta);
            }

            return section.combinedBox;
        }

        return unionOf(section, resolved);
    }

    public static @Nullable ZoneBox unionOf(Section section, PlacementMatcher.Resolved resolved)
    {
        ZoneBox union = null;

        for (PlacementRef ref : section.placements)
        {
            ZoneBox box = boxOf(ref, resolved);

            if (box != null)
            {
                union = union == null ? box : union.union(box);
            }
        }

        return union;
    }

    private static void gridInto(List<ZoneBox> out, ZoneBox bounds, Rule.Grid grid)
    {
        int[][] xs = ZoneManager.slices(bounds.minX(), bounds.maxX(), grid.nx());
        int[][] ys = ZoneManager.slices(bounds.minY(), bounds.maxY(), grid.ny());
        int[][] zs = ZoneManager.slices(bounds.minZ(), bounds.maxZ(), grid.nz());

        for (int[] y : ys)
        {
            for (int[] z : zs)
            {
                for (int[] x : xs)
                {
                    out.add(new ZoneBox(x[0], y[0], z[0], x[1], y[1], z[1]));
                }
            }
        }
    }

    private static List<ZoneBox> subRegions(PlacementMatcher.Resolved resolved)
    {
        List<ZoneBox> out = new ArrayList<>();

        for (SchematicPlacement placement : resolved.found())
        {
            Map<String, Box> boxes = placement.getSubRegionBoxes(SubRegionPlacement.RequiredEnabled.PLACEMENT_ENABLED);
            List<String> names = new ArrayList<>(boxes.keySet());
            names.sort(Comparator.naturalOrder());

            for (String name : names)
            {
                out.add(ZoneBox.of(boxes.get(name)));
            }
        }

        return out;
    }

    private static List<ZoneBox> manual(PlacementMatcher.Resolved resolved, Rule.Manual manual)
    {
        BlockPos delta = manualDelta(resolved, manual);

        if (delta.equals(BlockPos.ZERO))
        {
            return manual.boxes();
        }

        List<ZoneBox> out = new ArrayList<>(manual.boxes().size());

        for (ZoneBox box : manual.boxes())
        {
            out.add(box.offset(delta));
        }

        return out;
    }

    /** Сдвиг ручных коробок: насколько базовое размещение уехало с момента записи; базы нет — нуль. */
    public static BlockPos manualDelta(PlacementMatcher.Resolved resolved, Rule.Manual manual)
    {
        if (manual.base() == null || manual.origin() == null)
        {
            return BlockPos.ZERO;
        }

        SchematicPlacement base = resolved.of(manual.base());

        if (base == null)
        {
            // База не в списке ссылок участка (мигрированная ручная зона без имени) — ищем среди всех найденных
            base = resolved.first();
        }

        return base != null ? base.getOrigin().subtract(manual.origin()) : BlockPos.ZERO;
    }
}
