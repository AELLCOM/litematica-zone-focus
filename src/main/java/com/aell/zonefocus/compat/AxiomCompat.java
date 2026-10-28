package com.aell.zonefocus.compat;

import java.lang.reflect.Method;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;

@Environment(EnvType.CLIENT)
public final class AxiomCompat
{
    private static final boolean LOADED = FabricLoader.getInstance().isModLoaded("axiom");

    private static final String MANAGER = "com.moulberry.axiom.buildertools.BuilderToolManager";

    private static final String COLOUR_SCREEN   = "com.moulberry.axiom.screen.CreativeColourScreen";
    private static final String GRADIENT_SCREEN = "com.moulberry.axiom.screen.CreativeGradientScreen";

    private static Method isToolSlotActive;
    private static boolean broken;

    private AxiomCompat() {}

    public static boolean isLoaded()
    {
        return LOADED;
    }

    public static boolean isCreativeInventoryScreen(Screen screen)
    {
        if (! LOADED)
        {
            return false;
        }

        String name = screen.getClass().getName();

        return COLOUR_SCREEN.equals(name) || GRADIENT_SCREEN.equals(name);
    }

    public static boolean isAxiomToolActive()
    {
        Method method = isToolSlotActive();

        if (method == null)
        {
            return false;
        }

        try
        {
            return (Boolean) method.invoke(null);
        }
        catch (Throwable e)
        {
            broken = true;
            isToolSlotActive = null;
            return false;
        }
    }

    private static Method isToolSlotActive()
    {
        if (isToolSlotActive == null)
        {
            if (! LOADED || broken)
            {
                return null;
            }

            try
            {
                isToolSlotActive = Class.forName(MANAGER).getMethod("isToolSlotActive");
            }
            catch (Throwable e)
            {
                broken = true;
            }
        }

        return isToolSlotActive;
    }
}
