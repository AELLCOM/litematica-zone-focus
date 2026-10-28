package com.aell.zonefocus.config;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigResettable;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ZoneFocusDefaults
{
    public static final int SPLIT_X = 2;
    public static final int SPLIT_Y = 2;
    public static final int SPLIT_Z = 2;
    public static final boolean SPLIT_BY_SIZE = false;
    public static final int SPLIT_SIZE_X = 16;
    public static final int SPLIT_SIZE_Y = 8;
    public static final int SPLIT_SIZE_Z = 16;

    private ZoneFocusDefaults() {}

    public static void resetEverything()
    {
        for (IConfigBase config : ZoneFocusConfigs.OPTIONS)
        {
            if (config instanceof IConfigResettable resettable)
            {
                resettable.resetToDefault();
            }
        }

        for (ConfigHotkey hotkey : ZoneFocusConfigs.HOTKEYS)
        {
            hotkey.resetToDefault();
        }

        ZoneFocusConfigs.SPLIT_X = SPLIT_X;
        ZoneFocusConfigs.SPLIT_Y = SPLIT_Y;
        ZoneFocusConfigs.SPLIT_Z = SPLIT_Z;
        ZoneFocusConfigs.SPLIT_BY_SIZE = SPLIT_BY_SIZE;
        ZoneFocusConfigs.SPLIT_SIZE_X = SPLIT_SIZE_X;
        ZoneFocusConfigs.SPLIT_SIZE_Y = SPLIT_SIZE_Y;
        ZoneFocusConfigs.SPLIT_SIZE_Z = SPLIT_SIZE_Z;

        new ZoneFocusConfigs().save();
    }
}
