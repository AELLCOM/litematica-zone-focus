package com.aell.zonefocus;

import com.aell.zonefocus.compat.MaterialHudBridge;
import com.aell.zonefocus.compat.MaterialHudCompat;
import com.aell.zonefocus.config.GuiZoneFocusConfigs;
import com.aell.zonefocus.shaded.ui.Nav;
import com.aell.zonefocus.config.ZoneFocusConfigs;
import com.aell.zonefocus.gui.ZoneFocusScreens;
import com.aell.zonefocus.render.ZoneRenderer;
import com.aell.zonefocus.render.ZoneVerifyRenderer;
import com.aell.zonefocus.tool.ZoneTool;
import com.aell.zonefocus.tool.ToolButton;
import com.aell.zonefocus.tool.ToolKeyHints;
import com.aell.zonefocus.verify.ZoneVerifier;
import com.aell.zonefocus.zone.ZoneManager;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;

@Environment(EnvType.CLIENT)
public class ZoneFocusClient implements ClientModInitializer, IKeybindProvider, IHotkeyCallback
{
    @Override
    public void onInitializeClient()
    {
        ZoneFocusConfigs config = new ZoneFocusConfigs();
        config.load();
        ConfigManager.getInstance().registerConfigHandler("zonefocus", config);
        // Ванильный стиль: своя настройка или тема Material HUD, если он рядом — Zone Focus в одной теме с ним
        com.aell.zonefocus.shaded.ui.Style.levelWhen(ZoneFocusConfigs.UI_STYLE::getIntegerValue, true);

        InputEventHandler.getKeybindManager().registerKeybindProvider(this);

        for (ConfigHotkey hotkey : ZoneFocusConfigs.HOTKEYS)
        {
            hotkey.getKeybind().setCallback(this);
        }

        InputEventHandler.getInputManager().registerMouseInputHandler(ZoneTool.getInstance());

        com.aell.zonefocus.shaded.ui.ModSwitcher.register("zonefocus", "Litematica Zone Focus",
                () -> GuiZoneFocusConfigs.create(null, null));
        MaterialHudCompat.detect();

        DevScreenshots.register();

        if (MaterialHudCompat.isIntegrated())
        {
            try
            {
                MaterialHudBridge.install();
            }
            catch (LinkageError e)
            {
                MaterialHudCompat.markBroken();
            }
        }

        ToolButton.register();
        ToolKeyHints.register();

        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> {
            if (ZoneFocusModule.isEnabled())
            {
                ZoneTool.getInstance().onRenderTick(Minecraft.getInstance());
                ZoneRenderer.render();
                ZoneVerifyRenderer.render();
            }
        });

        ClientChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            if (ZoneFocusModule.isEnabled())
            {
                ZoneVerifier.getInstance().onChunkLoaded(chunk.getPos().x, chunk.getPos().z);
            }
        });

        // Зоны тикают и при выключенном моде: этот тик отпускает обрезку схемы и следит за сменой мира
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ZoneManager.getInstance().onClientTick(client);

            if (ZoneFocusModule.isEnabled())
            {
                ZoneVerifier.getInstance().onClientTick(client);
            }
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) ->
                ZoneTool.getInstance().shouldSuppressBlockInteraction()
                        ? InteractionResult.FAIL
                        : InteractionResult.PASS);

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) ->
                ZoneTool.getInstance().shouldSuppressBlockInteraction()
                        ? InteractionResult.FAIL
                        : InteractionResult.PASS);

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
                ZoneTool.getInstance().shouldSuppressBlockInteraction()
                        ? InteractionResult.FAIL
                        : InteractionResult.PASS);
    }

    private static void printMessage(String message)
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player != null && ! message.isEmpty())
        {
            ZoneChat.notice(Component.literal("§b" + message));
        }
    }

    @Override
    public void addKeysToMap(IKeybindManager manager)
    {
        for (ConfigHotkey hotkey : ZoneFocusConfigs.HOTKEYS)
        {
            manager.addKeybindToMap(hotkey.getKeybind());
        }
    }

    @Override
    public void addHotkeys(IKeybindManager manager)
    {
        manager.addHotkeysForCategory("zonefocus", "Litematica Zone Focus", ZoneFocusConfigs.HOTKEYS);
    }

    @Override
    public boolean onKeyAction(KeyAction action, IKeybind key)
    {
        if (key == ZoneFocusConfigs.OPEN_SETTINGS.getKeybind())
        {
            Nav.clear();
            GuiBase.openGui(ZoneFocusScreens.settings(null, null));
            return true;
        }

        // Выключенный мод молчит на клавиши действий; настройки открываются всегда, иначе
        // включить его обратно было бы нечем
        if (! ZoneFocusModule.isEnabled())
        {
            return false;
        }

        if (key == ZoneFocusConfigs.OPEN_ZONE_LIST.getKeybind())
        {
            var placement = com.aell.zonefocus.zone.ZoneActions.managerPlacement();

            if (placement != null)
            {
                Nav.clear();
                GuiBase.openGui(new com.aell.zonefocus.gui.manage.GuiZoneManager(ZoneFocusScreens.zonesParent(), placement));
            }

            return true;
        }

        if (key == ZoneFocusConfigs.NEXT_ZONE.getKeybind())
        {
            ZoneManager.getInstance().cycleZone(1);
            return true;
        }

        if (key == ZoneFocusConfigs.PREV_ZONE.getKeybind())
        {
            ZoneManager.getInstance().cycleZone(-1);
            return true;
        }

        if (key == ZoneFocusConfigs.NEXT_SECTION.getKeybind())
        {
            ZoneManager.getInstance().cycleSection(1);
            return true;
        }

        if (key == ZoneFocusConfigs.PREV_SECTION.getKeybind())
        {
            ZoneManager.getInstance().cycleSection(-1);
            return true;
        }

        if (key == ZoneFocusConfigs.TOGGLE_TOOL.getKeybind())
        {
            ZoneTool.getInstance().toggle();
            return true;
        }

        if (key == ZoneFocusConfigs.VERIFY_ZONE.getKeybind())
        {
            // С Material HUD проверка одна — его Ctrl+B (по зоне, если включён счёт по зоне)
            if (MaterialHudCompat.ownsVerification())
            {
                try
                {
                    MaterialHudBridge.toggleVerification();
                    return true;
                }
                catch (LinkageError e)
                {
                    MaterialHudCompat.markBroken();
                }
            }

            printMessage(ZoneVerifier.getInstance().toggle());
            return true;
        }

        return false;
    }
}
