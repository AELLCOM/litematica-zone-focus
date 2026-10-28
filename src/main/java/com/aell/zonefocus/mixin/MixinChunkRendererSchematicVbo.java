package com.aell.zonefocus.mixin;

import com.aell.zonefocus.zone.ZoneRenderClip;
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkRendererSchematicVbo.class)
public abstract class MixinChunkRendererSchematicVbo
{
    @Shadow
    protected abstract ChunkPos getChunkPos();

    @Inject(method = "rebuildChunk", at = @At("TAIL"), require = 0)
    private void zonefocus_onChunkRebuilt(CallbackInfo ci)
    {
        ChunkPos pos = this.getChunkPos();

        if (pos != null)
        {
            ZoneRenderClip.onChunkRebuilt(pos.x, pos.z);
        }
    }
}
