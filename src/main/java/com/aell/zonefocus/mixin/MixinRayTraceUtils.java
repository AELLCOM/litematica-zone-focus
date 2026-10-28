package com.aell.zonefocus.mixin;

import com.aell.zonefocus.zone.ZoneRenderClip;
import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.malilib.util.LayerRange;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RayTraceUtils.class)
public abstract class MixinRayTraceUtils
{
    @Redirect(
            method = "traceFirstStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lfi/dy/masa/malilib/util/LayerRange;isPositionWithinRange(III)Z"),
            require = 0)
    private static boolean zonefocus_firstStepInZone(LayerRange range, int x, int y, int z)
    {
        return range.isPositionWithinRange(x, y, z) && ZoneRenderClip.contains(x, y, z);
    }

    @Redirect(
            method = "traceLoopSteps",
            at = @At(
                    value = "INVOKE",
                    target = "Lfi/dy/masa/malilib/util/LayerRange;isPositionWithinRange(III)Z"),
            require = 0)
    private static boolean zonefocus_loopStepInZone(LayerRange range, int x, int y, int z)
    {
        return range.isPositionWithinRange(x, y, z) && ZoneRenderClip.contains(x, y, z);
    }
}
