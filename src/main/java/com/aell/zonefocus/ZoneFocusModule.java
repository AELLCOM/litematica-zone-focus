package com.aell.zonefocus;

import com.aell.zonefocus.config.ZoneFocusConfigs;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Общий выключатель мода. Раньше его не было: мод стоит — значит работает. Тумблер понадобился,
 * когда настройки зон переехали во вкладку Litematica Material HUD: у встроенного модуля тумблер
 * есть, и человек ждёт его на том же месте, пока движок ведёт этот мод. Имя опции — {@code
 * zoneFocus}, дословно как у соседа, поэтому вкладка правит её через общий мост.
 *
 * <p>Выключенный мод спит целиком: подсчёт по зоне, обрезка схемы, рамки зон, проверка зоны,
 * инструмент разметки и клавиши действий. Экран настроек продолжает открываться — иначе включить
 * обратно было бы нечем.
 */
@Environment(EnvType.CLIENT)
public final class ZoneFocusModule
{
    private ZoneFocusModule() {}

    public static boolean isEnabled()
    {
        return ZoneFocusConfigs.ENABLED.getBooleanValue();
    }
}
