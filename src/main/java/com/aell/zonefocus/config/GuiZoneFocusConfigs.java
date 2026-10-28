package com.aell.zonefocus.config;

import com.aell.zonefocus.shaded.ui.Nav;
import com.aell.zonefocus.shaded.ui.SettingsScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class GuiZoneFocusConfigs extends SettingsScreen
{
    public static final String ZONES_BACK_LABEL = "zonefocus.gui.parent.zones";

    private GuiZoneFocusConfigs()
    {
        super(ZoneFocusSettings.getInstance());
    }

    public static GuiZoneFocusConfigs create(@Nullable Screen parent, @Nullable String backLabelKey)
    {
        GuiZoneFocusConfigs screen = new GuiZoneFocusConfigs();
        screen.setParent(parent);
        screen.backLabel(backLabelKey);
        screen.highlightBack(ZONES_BACK_LABEL.equals(backLabelKey));

        return screen;
    }

    public static void open(@Nullable Screen parent, @Nullable String backLabelKey)
    {
        Nav.open(create(parent, backLabelKey), parent);
    }
}
