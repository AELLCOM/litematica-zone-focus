package com.aell.zonefocus;

import com.aell.zonefocus.config.ZoneFocusConfigs;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Отклик мода на действия игрока (инструмент разметки, проверка зоны): в чат, если включено «Сообщения в чат»,
 * иначе строкой над хотбаром — с выключенным чатом (по умолчанию с 0.0.9) нажатие не остаётся без ответа.
 */
@Environment(EnvType.CLIENT)
public final class ZoneChat
{
    private ZoneChat() {}

    public static void notice(Component body)
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null)
        {
            return;
        }

        if (ZoneFocusConfigs.CHAT_MESSAGES.getBooleanValue())
        {
            mc.player.displayClientMessage(body, false);
        }
        else
        {
            mc.gui.setOverlayMessage(body, false);
        }
    }
}
