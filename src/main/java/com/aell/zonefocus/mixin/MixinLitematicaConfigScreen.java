package com.aell.zonefocus.mixin;

import com.aell.zonefocus.gui.ZoneFocusScreens;
import com.aell.zonefocus.shaded.ui.LitematicaSlots;
import com.aell.zonefocus.shaded.ui.Nav;
import com.aell.zonefocus.shaded.ui.Strings;
import fi.dy.masa.litematica.gui.GuiConfigs;
import fi.dy.masa.malilib.gui.GuiBase;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(GuiConfigs.class)
public abstract class MixinLitematicaConfigScreen
{
    @Inject(method = "initGui", at = @At("TAIL"), require = 0)
    private void zonefocus$addModButton(CallbackInfo ci)
    {
        // Массив — локальный, не поле: одноимённые private static поля из миксинов всех
        // аддонов Mixin вливает в один GuiConfigs, поле становится общим, и побеждает
        // массив последнего миксина — у старых сборок соседей он короче, и кнопка мода,
        // которого там нет, вставала в чужой слот (под кнопку соседа).
        String[] slotOrder = { "layerlist", "zonefocus", "ghostalpha", "easyplacefixplus" };

        GuiBase screen = (GuiBase) (Object) this;

        screen.addButton(LitematicaSlots.addonTabButton(screen,
                Strings.plain("zonefocus.gui.litematica_button"),
                com.aell.zonefocus.config.ZoneFocusSettings.ICON,
                LitematicaSlots.hasAllTab(this), slotOrder, "zonefocus"),
                (b, mouseButton) -> Nav.open(
                        ZoneFocusScreens.settings(screen, "layerlist.gui.parent.litematica"),
                        screen));
    }
}
