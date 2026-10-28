package com.aell.zonefocus.zone.scope;

import com.aell.zonefocus.zone.ZoneBox;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.selection.Box;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Объём зоны — единственное место, где решается, что в ней считать, проверять и рисовать: пересечения коробки
 * зоны с подрегионами размещений участка (S(section)). Отсюда берут счёт для Material HUD, сканер прогресса и
 * проверка — раньше каждый считал по-своему.
 */
@Environment(EnvType.CLIENT)
public final class ZoneVolume
{
    private ZoneVolume() {}

    public static List<ZoneBox> of(PlacementMatcher.Resolved resolved, ZoneBox zone)
    {
        List<ZoneBox> out = new ArrayList<>();

        for (SchematicPlacement placement : resolved.scope())
        {
            // Подрегионы одной схемы могут накладываться — Material HUD считает по коробке на каждую пару, поэтому
            // из каждого следующего подрегиона вычитаем уже отданные коробки этого же размещения
            List<ZoneBox> own = new ArrayList<>();

            for (Box sub : placement.getSubRegionBoxes(SubRegionPlacement.RequiredEnabled.PLACEMENT_ENABLED).values())
            {
                ZoneBox intersection = ZoneBox.of(sub).intersect(zone);

                if (intersection != null)
                {
                    List<ZoneBox> pieces = subtractAll(intersection, own);
                    own.addAll(pieces);
                }
            }

            out.addAll(own);
        }

        return out;
    }

    /** {@code box} минус все {@code taken}: список непересекающихся коробок. */
    static List<ZoneBox> subtractAll(ZoneBox box, List<ZoneBox> taken)
    {
        List<ZoneBox> pieces = new ArrayList<>();
        pieces.add(box);

        for (ZoneBox cut : taken)
        {
            List<ZoneBox> next = new ArrayList<>();

            for (ZoneBox piece : pieces)
            {
                subtract(piece, cut, next);
            }

            pieces = next;
        }

        return pieces;
    }

    /** {@code box} минус {@code cut} → до шести коробок по осям (пустых не бывает). */
    static void subtract(ZoneBox box, ZoneBox cut, List<ZoneBox> out)
    {
        ZoneBox overlap = box.intersect(cut);

        if (overlap == null)
        {
            out.add(box);
            return;
        }

        if (box.minX() < overlap.minX())
        {
            out.add(new ZoneBox(box.minX(), box.minY(), box.minZ(), overlap.minX() - 1, box.maxY(), box.maxZ()));
        }

        if (overlap.maxX() < box.maxX())
        {
            out.add(new ZoneBox(overlap.maxX() + 1, box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()));
        }

        if (box.minY() < overlap.minY())
        {
            out.add(new ZoneBox(overlap.minX(), box.minY(), box.minZ(), overlap.maxX(), overlap.minY() - 1, box.maxZ()));
        }

        if (overlap.maxY() < box.maxY())
        {
            out.add(new ZoneBox(overlap.minX(), overlap.maxY() + 1, box.minZ(), overlap.maxX(), box.maxY(), box.maxZ()));
        }

        if (box.minZ() < overlap.minZ())
        {
            out.add(new ZoneBox(overlap.minX(), overlap.minY(), box.minZ(), overlap.maxX(), overlap.maxY(), overlap.minZ() - 1));
        }

        if (overlap.maxZ() < box.maxZ())
        {
            out.add(new ZoneBox(overlap.minX(), overlap.minY(), overlap.maxZ() + 1, overlap.maxX(), overlap.maxY(), box.maxZ()));
        }
    }
}
