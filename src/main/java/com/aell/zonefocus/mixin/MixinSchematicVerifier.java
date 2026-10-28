package com.aell.zonefocus.mixin;

import com.aell.zonefocus.zone.ZoneVerifierClip;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import java.util.List;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SchematicVerifier.class)
public abstract class MixinSchematicVerifier
{
    @Inject(method = "getSelectedMismatchPositionsForRender", at = @At("RETURN"),
            cancellable = true, require = 0)
    private void zonefocus_clipMismatchesToZone(
            CallbackInfoReturnable<List<SchematicVerifier.MismatchRenderPos>> cir)
    {
        List<SchematicVerifier.MismatchRenderPos> list = cir.getReturnValue();

        if (list != null)
        {
            cir.setReturnValue(ZoneVerifierClip.clipMismatches(list));
        }
    }

    @Inject(method = "getSelectedMismatchBlockPositionsForRender", at = @At("RETURN"),
            cancellable = true, require = 0)
    private void zonefocus_clipMismatchPositionsToZone(CallbackInfoReturnable<List<BlockPos>> cir)
    {
        List<BlockPos> list = cir.getReturnValue();

        if (list != null)
        {
            cir.setReturnValue(ZoneVerifierClip.clipPositions(list));
        }
    }
}
