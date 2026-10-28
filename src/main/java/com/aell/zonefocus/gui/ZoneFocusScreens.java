package com.aell.zonefocus.gui;

import com.aell.zonefocus.compat.MaterialHudBridge;
import com.aell.zonefocus.compat.MaterialHudCompat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

/**
 * Куда ведут «Настройки Zone Focus» — единственное место, где это решается.
 *
 * <p>Стоит Litematica Material HUD — настройки зон живут его вкладкой «Zone Focus», и все входы
 * (Mod Menu, кнопки на экранах Litematica, клавиша) открывают её: свой экран зон в этом случае
 * дублировал бы вкладку и расходился бы с ней. Material HUD нет — открывается свой экран зон,
 * как и раньше. Сосед старой сборки, ещё не знающий этого вызова, роняет {@link LinkageError} —
 * тогда тоже свой экран.
 */
@Environment(EnvType.CLIENT)
public final class ZoneFocusScreens
{
    private ZoneFocusScreens() {}

    public static boolean inMaterialHud()
    {
        return MaterialHudCompat.isIntegrated();
    }

    public static Screen settings(@Nullable Screen parent, @Nullable String backLabelKey)
    {
        if (MaterialHudCompat.isIntegrated())
        {
            try
            {
                Screen screen = MaterialHudBridge.settingsScreen(parent, backLabelKey);

                if (screen != null)
                {
                    return screen;
                }
            }
            catch (LinkageError e)
            {
                // Material HUD старее вкладки — открываем свой экран
            }
        }

        return new GuiZoneMain(parent);
    }

    /** Родитель для списка зон, открытого клавишей: свой экран зон, пока он есть. */
    public static @Nullable Screen zonesParent()
    {
        return MaterialHudCompat.isIntegrated() ? null : new GuiZoneMain(null);
    }
}
