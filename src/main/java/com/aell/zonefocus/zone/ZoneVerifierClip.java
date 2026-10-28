package com.aell.zonefocus.zone;

import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;

@Environment(EnvType.CLIENT)
public final class ZoneVerifierClip
{
    private ZoneVerifierClip() {}

    public static List<SchematicVerifier.MismatchRenderPos> clipMismatches(
            List<SchematicVerifier.MismatchRenderPos> list)
    {
        if (! ZoneRenderClip.isClipping() || list.isEmpty())
        {
            return list;
        }

        List<SchematicVerifier.MismatchRenderPos> result = new ArrayList<>(list.size());

        for (SchematicVerifier.MismatchRenderPos entry : list)
        {
            if (ZoneRenderClip.contains(entry.pos))
            {
                result.add(entry);
            }
        }

        return result;
    }

    public static List<BlockPos> clipPositions(List<BlockPos> list)
    {
        if (! ZoneRenderClip.isClipping() || list.isEmpty())
        {
            return list;
        }

        List<BlockPos> result = new ArrayList<>(list.size());

        for (BlockPos pos : list)
        {
            if (ZoneRenderClip.contains(pos))
            {
                result.add(pos);
            }
        }

        return result;
    }
}
