package com.aell.zonefocus.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface IMixinAbstractContainerScreen
{
    @Accessor("leftPos")
    int zonefocus_getLeftPos();

    @Accessor("topPos")
    int zonefocus_getTopPos();

    @Accessor("imageWidth")
    int zonefocus_getImageWidth();

    @Accessor("imageHeight")
    int zonefocus_getImageHeight();
}
