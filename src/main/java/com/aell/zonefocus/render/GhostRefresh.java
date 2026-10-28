package com.aell.zonefocus.render;

import fi.dy.masa.litematica.util.SchematicWorldRefresher;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;

@Environment(EnvType.CLIENT)
public final class GhostRefresh
{
    private GhostRefresh() {}

    public static void onBlockChanged(BlockPos pos)
    {
        WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();

        if (schematicWorld != null && ! schematicWorld.getBlockState(pos).isAir())
        {
            SchematicWorldRefresher.INSTANCE.markSchematicChunkForRenderUpdate(pos);
        }
    }
}
