package com.aell.zonefocus.mixin;

import com.aell.zonefocus.gui.ZoneFocusScreens;
import com.aell.zonefocus.shaded.ui.LitematicaSlots;
import com.aell.zonefocus.shaded.ui.Nav;
import com.aell.zonefocus.shaded.ui.Strings;
import fi.dy.masa.litematica.gui.GuiRenderLayer;
import fi.dy.masa.malilib.gui.GuiBase;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(GuiRenderLayer.class)
public abstract class MixinLitematicaRenderLayerScreen
{
    private static final String[] SLOT_ORDER = { "layerlist", "zonefocus", "ghostalpha", "easyplacefixplus" };

    @Inject(method = "initGui", at = @At("TAIL"), require = 0)
    private void zonefocus$addModButton(CallbackInfo ci)
    {
        GuiBase screen = (GuiBase) (Object) this;

        screen.addButton(LitematicaSlots.addonTabButton(screen,
                Strings.plain("zonefocus.gui.litematica_button"),
                com.aell.zonefocus.config.ZoneFocusSettings.ICON,
                LitematicaSlots.hasAllTab(screen), SLOT_ORDER, "zonefocus"),
                (b, mouseButton) -> Nav.open(
                        ZoneFocusScreens.settings(screen, "layerlist.gui.parent.litematica"),
                        screen));
    }
}
