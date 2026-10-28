package com.aell.zonefocus.compat;

import com.aell.zonefocus.gui.ZoneFocusScreens;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ModMenuImpl implements ModMenuApi
{
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory()
    {
        return screen ->
        {
            com.aell.zonefocus.shaded.ui.Nav.clear();

            return ZoneFocusScreens.settings(screen, null);
        };
    }
}
