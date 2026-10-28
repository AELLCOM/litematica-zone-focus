package com.aell.zonefocus.mixin;

import com.aell.zonefocus.zone.ZoneRenderClip;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.LayerRange;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LayerRange.class)
public abstract class MixinLayerRange
{
    @Inject(method = "getClampedRenderBoundingBox", at = @At("RETURN"), cancellable = true)
    private void zonefocus_clipToZone(IntBoundingBox box, CallbackInfoReturnable<IntBoundingBox> cir)
    {
        IntBoundingBox clamped = cir.getReturnValue();

        // box — исходная часть размещения (по ней опознаётся схема), clamped — она же после обрезки слоями
        if (clamped != null && (Object) this == DataManager.getRenderLayerRange())
        {
            cir.setReturnValue(ZoneRenderClip.clip(box, clamped));
        }
    }
}
