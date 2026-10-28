package com.aell.zonefocus.mixin;

import com.aell.zonefocus.zone.ZoneRenderClip;
import fi.dy.masa.litematica.render.schematic.WorldRendererSchematic;
import fi.dy.masa.malilib.util.LayerRange;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldRendererSchematic.class)
public abstract class MixinWorldRendererSchematic
{
    @Redirect(
            method = "prepareBlockEntities",
            at = @At(
                    value = "INVOKE",
                    target = "Lfi/dy/masa/malilib/util/LayerRange;isPositionWithinRange(III)Z"),
            require = 0)
    private boolean zonefocus_blockEntityInZone(LayerRange range, int x, int y, int z)
    {
        return range.isPositionWithinRange(x, y, z) && ZoneRenderClip.contains(x, y, z);
    }

    @Redirect(
            method = "prepareEntities",
            at = @At(
                    value = "INVOKE",
                    target = "Lfi/dy/masa/malilib/util/LayerRange;isPositionWithinRange(III)Z"),
            require = 0)
    private boolean zonefocus_entityInZone(LayerRange range, int x, int y, int z)
    {
        return range.isPositionWithinRange(x, y, z) && ZoneRenderClip.contains(x, y, z);
    }
}
