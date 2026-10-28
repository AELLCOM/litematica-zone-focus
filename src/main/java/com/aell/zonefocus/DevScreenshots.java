package com.aell.zonefocus;

import com.aell.zonefocus.config.GuiZoneFocusConfigs;
import com.aell.zonefocus.gui.GuiZoneMain;
import com.aell.zonefocus.zone.ZoneManager;
import java.io.File;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import fi.dy.masa.malilib.gui.GuiBase;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;

@Environment(EnvType.CLIENT)
public final class DevScreenshots
{
    private static final int START_TICKS = 60;
    private static final int STEP_TICKS = 10;

    private static int ticks;
    private static int step;

    private DevScreenshots() {}

    public static void register()
    {
        if (! enabled())
        {
            return;
        }

        ClientTickEvents.END_CLIENT_TICK.register(DevScreenshots::tick);
    }

    private static boolean enabled()
    {
        if ("1".equals(System.getenv("ZF_SHOTS")))
        {
            return true;
        }

        return new File(FabricLoader.getInstance().getGameDir().toFile(), "zf_shots").exists();
    }

    private static void tick(Minecraft mc)
    {
        ++ticks;

        // ZF_UI_VANILLA=0|1|2 — оформление на весь прогон снимков (Aell UI / Vanilla / Vanilla+)
        String vanilla = System.getenv("ZF_UI_VANILLA");
        int style = "2".equals(vanilla) ? 2 : "1".equals(vanilla) ? 1 : 0;

        if (vanilla != null && com.aell.zonefocus.config.ZoneFocusConfigs.UI_STYLE.getIntegerValue() != style)
        {
            com.aell.zonefocus.config.ZoneFocusConfigs.UI_STYLE.setIntegerValue(style);
        }

        if (mc.level == null || mc.player == null || ticks < START_TICKS)
        {
            return;
        }

        if ((ticks - START_TICKS) % STEP_TICKS != 0)
        {
            return;
        }

        run(mc, step);
        ++step;
    }

    private static void run(Minecraft mc, int index)
    {
        switch (index)
        {
            case 0 ->
            {
                BlockPos base = mc.player.blockPosition();
                ZoneManager.getInstance().setManualBox(base.offset(-24, -8, -24), base.offset(24, 8, 24));
                mc.setScreen(new GuiZoneMain(null));
            }
            case 1 -> shot(mc, "1-zones");
            case 2 -> GuiZoneFocusConfigs.open(new GuiZoneMain(null),
                    GuiZoneFocusConfigs.ZONES_BACK_LABEL);
            case 3 -> shot(mc, "2-ctrl-j");
            case 4 ->
            {
                var placement = com.aell.zonefocus.zone.ZoneActions.managerPlacement();

                if (placement != null)
                {
                    mc.setScreen(new com.aell.zonefocus.gui.manage.GuiZoneManager(null, placement));
                }
            }
            case 5 -> shot(mc, "3-list");
            default ->
            {
                int tab = (index - 6) / 2;

                if (tab == tabCount())
                {
                    if ((index - 6) % 2 == 0)
                    {
                        Screen current = fi.dy.masa.malilib.util.GuiUtils.getCurrentScreen();

                        if (current instanceof GuiBase gui)
                        {
                            for (int i = 0; i < 6; ++i)
                            {
                                gui.onMouseScrolled(200.0D, 150.0D, 0.0D, -1.0D);
                            }
                        }
                    }
                    else
                    {
                        shot(mc, "5-hotkeys-scrolled");
                    }

                    return;
                }

                if (tab > tabCount())
                {
                    mc.stop();

                    return;
                }

                if ((index - 6) % 2 == 0)
                {
                    mc.setScreen(GuiZoneFocusConfigs.create(null, null).showTab(tab));
                }
                else
                {
                    shot(mc, "4-tab-" + tab);
                }
            }
        }
    }

    private static int tabCount()
    {
        return GuiZoneFocusConfigs.create(null, null).tabCount();
    }

    private static void shot(Minecraft mc, String name)
    {
        Screenshot.grab(mc.gameDirectory, mc.getMainRenderTarget(), msg -> { });
        System.out.println("[zonefocus] screenshot: " + name);
    }
}
