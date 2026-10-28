package com.aell.zonefocus.zone;

import org.jetbrains.annotations.Nullable;

import com.aell.zonefocus.gui.GuiZoneMain;
import com.aell.zonefocus.gui.manage.GuiZoneManager;
import com.aell.zonefocus.shaded.ui.Nav;
import com.aell.zonefocus.tool.ZoneTool;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.util.GuiUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;

/**
 * Действия с зонами одним набором коротких вызовов. Нужны вкладке «Zone Focus» в Litematica
 * Material HUD: движок ведёт этот мод, а кнопки стоят у соседа, и жать он должен наши объекты —
 * свои у него в это время спят. Сосед зовёт их через {@code compat.ZoneFocusBridge} по имени,
 * поэтому имена методов — часть контракта между модами.
 *
 * <p>Экраны открываются поверх текущего (то есть поверх экрана соседа), «Назад» возвращает туда же.
 */
@Environment(EnvType.CLIENT)
public final class ZoneActions
{
    private ZoneActions() {}

    public static boolean hasPlacement()
    {
        return GuiZoneMain.pickPlacement() != null;
    }

    public static boolean hasSelection()
    {
        return GuiZoneMain.selectionBox() != null;
    }

    public static boolean hasZones()
    {
        return ZoneManager.getInstance().hasZones();
    }

    /** Площадка для «Управления зонами»: схема уже разбитых зон, иначе выбранная/единственная. */
    public static @Nullable SchematicPlacement managerPlacement()
    {
        if (ZoneManager.getInstance().hasZones() && ZoneManager.getInstance().getPlacement() != null)
        {
            return ZoneManager.getInstance().getPlacement();
        }

        return GuiZoneMain.pickPlacement();
    }

    /** «Управление зонами» поверх текущего экрана; без включённой схемы — ничего. */
    public static void openManager()
    {
        Screen current = GuiUtils.getCurrentScreen();
        var section = ZoneManager.getInstance().activeSection();

        // Есть участки — открываем активный (групповой и без схемы тоже); иначе — по размещению, как раньше
        if (section != null)
        {
            Nav.open(new GuiZoneManager(current, section), current);
            return;
        }

        SchematicPlacement placement = managerPlacement();

        if (placement == null)
        {
            return;
        }

        Nav.open(new GuiZoneManager(current, placement), current);
    }

    /** Старое имя контракта (разбиение) — теперь то же «Управление зонами». */
    public static void openSplit()
    {
        openManager();
    }

    /** Старое имя контракта (список зон) — теперь то же «Управление зонами». */
    public static void openList()
    {
        openManager();
    }

    public static void splitBySubRegions()
    {
        SchematicPlacement placement = GuiZoneMain.pickPlacement();

        if (placement != null)
        {
            ZoneManager.getInstance().splitBySubRegions(placement);
        }
    }

    public static void zoneFromSelection()
    {
        ZoneBox box = GuiZoneMain.selectionBox();

        if (box != null)
        {
            ZoneManager.getInstance().zoneFromSelection(GuiZoneMain.pickPlacement(), box);
        }
    }

    /** «Сбросить зону» у соседа стоит под одним участком — убираем активный участок, не всю раскладку. */
    public static void clearZones()
    {
        ZoneManager manager = ZoneManager.getInstance();

        if (manager.sections().size() <= 1)
        {
            manager.clear();
        }
        else
        {
            manager.removeSection(manager.getActiveSectionIndex());
        }
    }

    /** Включает инструмент разметки; false — инструмент не поднялся, экран закрывать не за чем. */
    public static boolean startMarking()
    {
        ZoneTool.getInstance().setActive(true);

        return ZoneTool.getInstance().isActive();
    }
}
