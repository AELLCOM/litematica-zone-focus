package com.aell.zonefocus.compat;

import com.aell.zonefocus.ZoneChat;
import com.aell.zonefocus.config.GuiZoneFocusConfigs;
import com.aell.zonefocus.config.ZoneFocusConfigs;
import com.aell.zonefocus.zone.FileZones;
import com.aell.zonefocus.zone.ZoneActions;
import com.aell.zonefocus.zone.ZoneBox;
import com.aell.zonefocus.zone.ZoneManager;
import com.aell.zonefocus.zone.ZoneScanner;
import com.aell.zonefocus.zone.ZoneState;
import com.aell.zonefocus.zone.model.Rule;
import com.aell.zonefocus.zone.model.Section;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.StringUtils;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Мост для встроенного модуля Zone Focus в Litematica Material HUD: у него те же опции зон
 * под теми же именами, и настройки у нас общие. Имя класса и подписи методов — контракт
 * между модами: сосед находит их рефлексией (см. {@code com.aell.ui.SharedOptions}), поэтому
 * переименовывать их нельзя, иначе у людей с обоими модами настройки разойдутся на два набора.
 *
 * <p>Отдаём только {@link ZoneFocusConfigs#OPTIONS} — опции движка зон. Клавиши сосед не
 * увидит и не перепишет: в семье модов привязки никогда не общие.
 *
 * <p>Вторая половина моста — действия с зонами и вход в свой экран настроек. С 0.1.0 зоны живут в участках:
 * общие методы работают с <b>активным участком</b>, файловые — с <b>участком файла</b> (одиночным участком,
 * в котором стоит размещение этого файла). Новое — только новыми методами; номер версии — {@link #bridgeApi()}.
 */
@Environment(EnvType.CLIENT)
public final class ZoneFocusBridge
{
    private ZoneFocusBridge() {}

    /** Версия моста: 2 — участки (0.1.0). */
    public static int bridgeApi()
    {
        return 2;
    }

    public static @Nullable IConfigBase option(String name)
    {
        for (IConfigBase config : ZoneFocusConfigs.OPTIONS)
        {
            if (config.getName().equals(name))
            {
                return config;
            }
        }

        return null;
    }

    public static boolean hasPlacement()
    {
        return ZoneActions.hasPlacement();
    }

    public static boolean hasSelection()
    {
        return ZoneActions.hasSelection();
    }

    /** Есть не скрытый участок с зонами. */
    public static boolean hasZones()
    {
        return ZoneManager.getInstance().layoutHasZones();
    }

    /** «Управление зонами» (с 0.0.8): разбиение, карта, список зон. */
    public static void openManager()
    {
        ZoneActions.openManager();
    }

    public static void openSplit()
    {
        ZoneActions.openSplit();
    }

    public static void openList()
    {
        ZoneActions.openList();
    }

    public static void splitBySubRegions()
    {
        ZoneActions.splitBySubRegions();
    }

    public static void zoneFromSelection()
    {
        ZoneActions.zoneFromSelection();
    }

    public static void clearZones()
    {
        ZoneActions.clearZones();
    }

    public static boolean startMarking()
    {
        return ZoneActions.startMarking();
    }

    // ===== Сводка зон для карточки «Управление зонами» во вкладке Material HUD (с 0.0.8) =====

    /** Имя схемы активного участка; групповой — «имя участка (+N)»; {@code null} — схемы нет. */
    public static @Nullable String zonePlacementName()
    {
        ZoneManager manager = ZoneManager.getInstance();
        Section section = manager.activeSection();

        if (section == null)
        {
            SchematicPlacement placement = ZoneActions.managerPlacement();
            return placement != null ? placement.getName() : null;
        }

        if (section.isGroup())
        {
            return sectionName(manager, section) + " (+" + (section.placements.size() - 1) + ")";
        }

        SchematicPlacement placement = manager.getPlacement();
        return placement != null ? placement.getName() : sectionName(manager, section);
    }

    private static String sectionName(ZoneManager manager, Section section)
    {
        if (! section.name.isEmpty())
        {
            return section.name;
        }

        var first = section.firstPlacement();
        return first != null && ! first.name.isEmpty() ? first.name
                : StringUtils.translate("zonefocus.gui.section_default_name", manager.getLayout().indexOf(section) + 1);
    }

    /** {число зон, активная (с 0, нормализована), nx, ny, nz} — сетка нулями, если зоны не сеткой; {@code null} — зон нет. */
    public static int @Nullable [] zoneSummary()
    {
        ZoneManager manager = ZoneManager.getInstance();

        if (! manager.hasZones())
        {
            return null;
        }

        ZoneState state = manager.getState();
        boolean grid = manager.getKind() == ZoneState.Kind.GRID;
        return new int[] { manager.getZoneCount(), manager.getActiveIndex(),
                grid ? state.nx : 0, grid ? state.ny : 0, grid ? state.nz : 0 };
    }

    /** Зоны активного участка в мире {minX, minY, minZ, maxX, maxY, maxZ} по номерам; {@code null} — зон нет. */
    public static int @Nullable [][] zoneBoxes()
    {
        ZoneManager manager = ZoneManager.getInstance();
        return manager.hasZones() ? toArray(manager.zoneBoxes()) : null;
    }

    private static int[][] toArray(List<ZoneBox> boxes)
    {
        int[][] out = new int[boxes.size()][];

        for (int i = 0; i < out.length; ++i)
        {
            ZoneBox b = boxes.get(i);
            out[i] = new int[] { b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ() };
        }

        return out;
    }

    /** Готовность каждой зоны активного участка в процентах (−1 — не посчитана); скана не было — запускаем. */
    public static int @Nullable [] zoneProgress()
    {
        ZoneManager manager = ZoneManager.getInstance();
        Section section = manager.activeSection();

        if (section == null || ! manager.hasZones())
        {
            return null;
        }

        return progressOf(manager, section);
    }

    private static int[] progressOf(ZoneManager manager, Section section)
    {
        ZoneScanner scanner = ZoneScanner.getInstance();

        if (! scanner.hasStats(section) && ! scanner.isScanning(section))
        {
            scanner.requestScan(section);
        }

        int[] out = new int[manager.zoneBoxes(section).size()];

        for (int i = 0; i < out.length; ++i)
        {
            ZoneScanner.Stats st = scanner.getStats(section, i);
            out[i] = st == null ? -1 : st.percent();
        }

        return out;
    }

    /** Сделать зону активного участка активной (щелчок по карточке). */
    public static void setActiveZone(int index)
    {
        ZoneManager.getInstance().setActiveIndex(index);
    }

    // ===== Участки (с 0.1.0, bridgeApi 2) =====

    public static int sectionCount()
    {
        return ZoneManager.getInstance().sections().size();
    }

    public static int activeSection()
    {
        return ZoneManager.getInstance().getActiveSectionIndex();
    }

    public static String[] sectionNames()
    {
        ZoneManager manager = ZoneManager.getInstance();
        List<Section> sections = manager.sections();
        String[] out = new String[sections.size()];

        for (int i = 0; i < out.length; ++i)
        {
            out[i] = sectionName(manager, sections.get(i));
        }

        return out;
    }

    /** {число зон, активная зона, скрыт ? 1 : 0, число схем, правило: 1 сетка / 2 подрегионы / 3 вручную}; {@code null} — нет участка. */
    public static int @Nullable [] sectionSummary(int index)
    {
        ZoneManager manager = ZoneManager.getInstance();
        Section section = sectionAt(manager, index);

        if (section == null)
        {
            return null;
        }

        int zones = manager.zoneBoxes(section).size();
        int rule = switch (section.rule) { case Rule.Grid ignored -> 1; case Rule.Cuts ignored -> 1; case Rule.SubRegions ignored -> 2; case Rule.Manual ignored -> 3; };
        return new int[] { zones, zones > 0 ? Math.floorMod(section.activeZone, zones) : 0, section.hidden ? 1 : 0,
                section.placements.size(), rule };
    }

    public static int @Nullable [][] sectionZoneBoxes(int index)
    {
        ZoneManager manager = ZoneManager.getInstance();
        Section section = sectionAt(manager, index);
        return section != null ? toArray(manager.zoneBoxes(section)) : null;
    }

    public static int @Nullable [] sectionZoneProgress(int index)
    {
        ZoneManager manager = ZoneManager.getInstance();
        Section section = sectionAt(manager, index);
        return section != null && ! manager.zoneBoxes(section).isEmpty() ? progressOf(manager, section) : null;
    }

    public static void setActiveSection(int index)
    {
        ZoneManager.getInstance().setActiveSection(index);
    }

    public static void setActiveZone(int sectionIndex, int zoneIndex)
    {
        ZoneManager manager = ZoneManager.getInstance();
        Section section = sectionAt(manager, sectionIndex);

        if (section != null)
        {
            manager.setActiveZone(section, zoneIndex);
        }
    }

    private static @Nullable Section sectionAt(ZoneManager manager, int index)
    {
        List<Section> sections = manager.sections();
        return index >= 0 && index < sections.size() ? sections.get(index) : null;
    }

    // ===== Зоны схемы-файла: библиотека Material HUD (с 0.0.8) =====

    /** Размещение этого файла стоит в групповом участке — библиотека им не управляет. */
    public static boolean fileInGroup(String path)
    {
        ZoneManager manager = ZoneManager.getInstance();

        for (SchematicPlacement placement : ZoneManager.enabledPlacements())
        {
            if (path.equals(FileZones.key(placement)))
            {
                Section section = manager.sectionOf(placement);

                if (section != null && section.isGroup())
                {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Сетка файла {nx, ny, nz} в осях схемы: у одиночного участка размещения этого файла с правилом «сетка» — его
     * сетка; участок есть, но правило другое — {@code null}; участка нет — запомненная для файла; в группе — {@code null}.
     */
    public static int @Nullable [] fileZoneGrid(String path)
    {
        if (fileInGroup(path))
        {
            return null;
        }

        Section section = FileZones.singleSectionOf(path);
        ZoneManager manager = ZoneManager.getInstance();

        if (section != null)
        {
            SchematicPlacement zoned = manager.resolve(section).first();

            if (! (section.rule instanceof Rule.Grid grid) || zoned == null)
            {
                return null;
            }

            return FileZones.swapsXZ(zoned) ? new int[] { grid.nz(), grid.ny(), grid.nx() }
                    : new int[] { grid.nx(), grid.ny(), grid.nz() };
        }

        return FileZones.grid(path);
    }

    /**
     * Зоны файла в блоках схемы от минимального угла всех её регионов ({minX, minY, minZ, maxX, maxY, maxZ}
     * включительно), по порядку номеров: зоны участка размещения этого файла или запомненная сетка по габариту
     * {@code sizeX×sizeY×sizeZ}; {@code null} — зон нет или размещение в группе.
     */
    public static int @Nullable [][] fileZoneBoxes(String path, int sizeX, int sizeY, int sizeZ)
    {
        if (fileInGroup(path))
        {
            return null;
        }

        Section section = FileZones.singleSectionOf(path);
        ZoneManager manager = ZoneManager.getInstance();
        SchematicPlacement zoned = section != null ? manager.resolve(section).first() : null;

        if (section != null && zoned != null)
        {
            return FileZones.toLocal(zoned, manager.zoneBoxes(section));
        }

        int[] g = FileZones.grid(path);
        return g != null ? FileZones.localGrid(sizeX, sizeY, sizeZ, g[0], g[1], g[2]) : null;
    }

    /** Активная зона участка файла (с 0); иначе −1. */
    public static int fileActiveZone(String path)
    {
        Section section = fileInGroup(path) ? null : FileZones.singleSectionOf(path);
        ZoneManager manager = ZoneManager.getInstance();

        if (section == null)
        {
            return -1;
        }

        int zones = manager.zoneBoxes(section).size();
        return zones > 0 ? Math.floorMod(section.activeZone, zones) : -1;
    }

    /** Готовность зон участка файла (−1 — ещё не посчитана); скана не было — запускаем; иначе {@code null}. */
    public static int @Nullable [] fileZoneProgress(String path)
    {
        Section section = fileInGroup(path) ? null : FileZones.singleSectionOf(path);
        ZoneManager manager = ZoneManager.getInstance();

        if (section == null || manager.zoneBoxes(section).isEmpty())
        {
            return null;
        }

        return progressOf(manager, section);
    }

    /** Сделать зону участка файла активной (щелчок по превью) — и сам участок становится активным. */
    public static void setFileActiveZone(String path, int index)
    {
        Section section = fileInGroup(path) ? null : FileZones.singleSectionOf(path);

        if (section != null)
        {
            ZoneManager.getInstance().setActiveZone(section, index);
        }
    }

    /** Разбить файл сеткой (в осях схемы); размещён — сетка сразу встаёт и на его участок (или новый). */
    public static void setFileZoneGrid(String path, int nx, int ny, int nz)
    {
        if (fileInGroup(path))
        {
            Section group = groupOf(path);
            ZoneChat.notice(Component.literal("§e" + StringUtils.translate("zonefocus.message.in_group",
                    group != null ? ZoneManager.getInstance().getLayout().indexOf(group) + 1 : 0)));
            return;
        }

        FileZones.set(path, nx, ny, nz);
        SchematicPlacement placement = FileZones.placementOf(path);

        if (placement != null)
        {
            int[] g = FileZones.gridFor(placement);

            if (g != null)
            {
                ZoneManager.getInstance().splitGrid(placement, g[0], g[1], g[2]);
            }
        }
    }

    private static @Nullable Section groupOf(String path)
    {
        ZoneManager manager = ZoneManager.getInstance();

        for (SchematicPlacement placement : ZoneManager.enabledPlacements())
        {
            if (path.equals(FileZones.key(placement)))
            {
                Section section = manager.sectionOf(placement);

                if (section != null && section.isGroup())
                {
                    return section;
                }
            }
        }

        return null;
    }

    /** «Отменить разбиение»: у файла — и у его участка (только его, не всей раскладки). */
    public static void clearFileZones(String path)
    {
        FileZones.clear(path);

        if (fileInGroup(path))
        {
            return;
        }

        Section section = FileZones.singleSectionOf(path);

        if (section != null)
        {
            ZoneManager manager = ZoneManager.getInstance();
            manager.removeSection(manager.getLayout().indexOf(section));
        }
    }

    public static boolean filePlaced(String path)
    {
        return FileZones.placementOf(path) != null;
    }

    /** «Управление зонами» для размещения этого файла (его участок, групповой тоже); не размещён — {@code false}. */
    public static boolean openManagerForFile(String path)
    {
        SchematicPlacement placement = FileZones.placementOf(path);

        if (placement == null)
        {
            return false;
        }

        net.minecraft.client.gui.screens.Screen current = GuiUtils.getCurrentScreen();
        com.aell.zonefocus.shaded.ui.Nav.open(new com.aell.zonefocus.gui.manage.GuiZoneManager(current, placement), current);
        return true;
    }

    /** Свой экран настроек: там остались проверка зоны и клавиши — их у соседа нет. */
    public static void openOwnSettings()
    {
        GuiZoneFocusConfigs.open(GuiUtils.getCurrentScreen(), null);
    }

    /** Только запись своего файла: ничего у соседа не зовём, чтобы сохранение не зацикливалось. */
    public static void saveOwn()
    {
        new ZoneFocusConfigs().save();
    }
}
