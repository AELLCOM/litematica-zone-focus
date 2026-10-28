package com.aell.zonefocus.mixin;

import com.aell.zonefocus.render.GhostRefresh;
import com.aell.zonefocus.verify.ZoneVerifier;
import com.aell.zonefocus.zone.ZoneScanner;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class MixinClientLevel
{
    @Inject(method = "sendBlockUpdated", at = @At("HEAD"), require = 0)
    private void zonefocus_onBlockChanged(BlockPos pos, BlockState previous, BlockState current,
                                          int flags, CallbackInfo ci)
    {
        if (previous == current || ! com.aell.zonefocus.ZoneFocusModule.isEnabled())
        {
            return;
        }

        ZoneScanner.getInstance().onBlockChanged(pos, previous, current);
        ZoneVerifier.getInstance().onBlockChanged(pos, current);
        GhostRefresh.onBlockChanged(pos);
    }
}
