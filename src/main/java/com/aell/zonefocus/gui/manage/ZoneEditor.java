package com.aell.zonefocus.gui.manage;

import com.aell.zonefocus.config.ZoneFocusConfigs;
import com.aell.zonefocus.zone.ZoneBox;
import com.aell.zonefocus.zone.ZoneManager;
import com.aell.zonefocus.zone.ZoneScanner;
import com.aell.zonefocus.zone.model.Bounds;
import com.aell.zonefocus.zone.model.PlacementRef;
import com.aell.zonefocus.zone.model.Rule;
import com.aell.zonefocus.zone.model.Section;
import com.aell.zonefocus.zone.scope.PlacementMatcher;
import com.aell.zonefocus.zone.scope.ZoneLayouter;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.selection.Box;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

/**
 * Логика экрана «Управление зонами» без отрисовки — общая для обоих вариантов экрана. Черновик сетки
 * (число или размер зон по осям) живёт в {@link ZoneFocusConfigs}, применённые зоны — в
 * {@link ZoneManager}. Карта — вид сверху (X вправо, Z вниз, север сверху), по одному срезу Y: номера
 * клеток совпадают с порядком зон в {@code ZoneManager} (Y внешний, потом Z, потом X).
 */
@Environment(EnvType.CLIENT)
public final class ZoneEditor
{
    public static final int MAX_COUNT = 64;
    public static final int MAX_SIZE = 512;

    /** Клетка карты: номер зоны (с нуля) и место на экране. */
    public record Cell(int index, int x, int y, int w, int h, boolean missing) {}

    /** Строка списка зон; {@code missing} — зона схемы, которой сейчас нет среди размещений. */
    public record Row(int index, ZoneBox box, @Nullable ZoneScanner.Stats stats, int distance, boolean missing) {}

    /** «Показывать прогресс» — по умолчанию выключено, на время игры. */
    public static boolean showProgress;

    /** Карта как 3D-превью схемы (только с Material HUD 1.0.5+); иначе — вид сверху. */
    public static boolean view3d;

    /** Каркасы зон на 3D-превью; выкл — видна только постройка. */
    public static boolean showFrames = true;

    /** Плоскость 2D-карты: 0 — сверху (X вправо, Z вниз), 1 — спереди (X вправо, Y вверх), 2 — сбоку (Z влево, Y вверх). */
    public static int mapView;

    /** Ось вправо на карте. */
    public static int hAxis()
    {
        return mapView == 2 ? 2 : 0;
    }

    /** Ось вниз на карте (сверху — Z, спереди и сбоку — Y, отражённая: верх наверху). */
    public static int vAxis()
    {
        return mapView == 0 ? 2 : 1;
    }

    /** Ось глубины — по ней карта режется на срезы. */
    public static int depthAxis()
    {
        return mapView == 0 ? 1 : mapView == 1 ? 2 : 0;
    }

    /** Сбоку (взгляд на запад) север справа: Z растёт влево. */
    private static boolean flipH()
    {
        return mapView == 2;
    }

    /** Спереди и сбоку Y растёт вверх, а экранная ось — вниз. */
    private static boolean flipV()
    {
        return mapView != 0;
    }

    public static int size(ZoneBox b, int axis)
    {
        return axis == 0 ? b.sizeX() : axis == 1 ? b.sizeY() : b.sizeZ();
    }

    public static int min(ZoneBox b, int axis)
    {
        return axis == 0 ? b.minX() : axis == 1 ? b.minY() : b.minZ();
    }

    public static int max(ZoneBox b, int axis)
    {
        return axis == 0 ? b.maxX() : axis == 1 ? b.maxY() : b.maxZ();
    }

    /** Сортировка списка: по номеру, по прогрессу, по остатку, по расстоянию. */
    public static int sortMode;
    public static final String[] SORT_KEYS = {"zonefocus.gui.sort_number", "zonefocus.gui.sort_progress",
            "zonefocus.gui.sort_left", "zonefocus.gui.sort_distance"};

    /** Участок, на который создан редактор; {@code null} — черновик для размещения, участок появится при apply. */
    private @Nullable Section section;
    private final @Nullable SchematicPlacement placement;
    private int slice;
    private int[] mapRect = {0, 0, 0, 0};

    /** Редактор участка. */
    public ZoneEditor(Section section)
    {
        this.section = section;
        this.placement = null;
        this.init();
    }

    /** Старый вход (заморожен для стенда): участок размещения либо черновик для него. */
    public ZoneEditor(SchematicPlacement placement)
    {
        this.placement = placement;
        this.section = ZoneManager.getInstance().sectionOf(placement);
        this.init();
    }

    private void init()
    {
        // Участок уже разбит сеткой — черновик начинается с неё, карта сразу живая
        this.adoptRule();

        if (this.applied() && this.active() >= 0)
        {
            this.slice = this.sliceOf(this.active());
        }
    }

    /** Черновик (числа слева) — по правилу участка: после разреза мышью числа сходятся с картой. */
    public void adoptRule()
    {
        Section s = this.section();
        int[] counts = s != null && this.hasZones() ? ruleCounts(s.rule) : null;

        // Числа слева расходятся с правилом (разрезали мышью или не с этого экрана) — берём числа правила
        if (counts != null && (counts[0] != this.nx() || counts[1] != this.ny() || counts[2] != this.nz()))
        {
            ZoneFocusConfigs.SPLIT_BY_SIZE = false;
            ZoneFocusConfigs.SPLIT_X = Math.min(MAX_COUNT, counts[0]);
            ZoneFocusConfigs.SPLIT_Y = Math.min(MAX_COUNT, counts[1]);
            ZoneFocusConfigs.SPLIT_Z = Math.min(MAX_COUNT, counts[2]);
        }

        this.draftTouched = false;
    }

    /** Число зон по осям у сетки и разрезов; у подрегионов и ручных — {@code null}. */
    private static int @Nullable [] ruleCounts(Rule rule)
    {
        return switch (rule)
        {
            case Rule.Grid g -> new int[] { g.nx(), g.ny(), g.nz() };
            case Rule.Cuts c -> new int[] { c.nx(), c.ny(), c.nz() };
            default -> null;
        };
    }

    /** Участок редактора: заданный, а для черновика размещения — найденный по нему (после apply). */
    public @Nullable Section section()
    {
        ZoneManager manager = ZoneManager.getInstance();

        if (this.section != null && manager.sections().contains(this.section))
        {
            return this.section;
        }

        if (this.placement != null)
        {
            this.section = manager.sectionOf(this.placement);
            return this.section;
        }

        return null;
    }

    private @Nullable PlacementMatcher.Resolved resolved()
    {
        Section s = this.section();
        return s == null ? null : ZoneManager.getInstance().resolve(s);
    }

    /** Первое найденное размещение участка (или размещение из старого конструктора); может быть {@code null}. */
    public @Nullable SchematicPlacement placement()
    {
        PlacementMatcher.Resolved r = this.resolved();
        SchematicPlacement first = r == null ? null : r.first();
        return first != null ? first : this.placement;
    }

    /** Имя участка для заголовка: своё, иначе имя первой схемы, иначе «Участок N». */
    public String title()
    {
        Section s = this.section();
        return s == null && this.placement != null ? this.placement.getName() : sectionTitle(s);
    }

    public static String sectionTitle(@Nullable Section s)
    {
        if (s == null)
        {
            return "";
        }

        if (! s.name.isEmpty())
        {
            return s.name;
        }

        PlacementRef first = s.firstPlacement();

        if (first != null && ! first.name.isEmpty())
        {
            return first.name;
        }

        return fi.dy.masa.malilib.util.StringUtils.translate("zonefocus.gui.sections.default_name",
                ZoneManager.getInstance().getLayout().indexOf(s) + 1);
    }

    /** Габарит участка: общий для COMBINED, иначе объединение габаритов ссылок; без участка — габарит размещения. */
    public @Nullable ZoneBox bounds()
    {
        Section s = this.section();

        if (s == null)
        {
            return this.placement == null ? null : ZoneManager.enclosingBox(this.placement);
        }

        PlacementMatcher.Resolved r = ZoneManager.getInstance().resolve(s);
        return s.bounds == Bounds.COMBINED ? ZoneLayouter.combinedBox(s, r) : ZoneLayouter.unionOf(s, r);
    }

    // ===== Черновик =====

    public boolean bySize()
    {
        return ZoneFocusConfigs.SPLIT_BY_SIZE;
    }

    public void setBySize(boolean value)
    {
        ZoneFocusConfigs.SPLIT_BY_SIZE = value;
        this.draftTouched = true;
    }

    /** Числа слева трогали после открытия (или последнего «Разбить») — «разрезы» уступают место черновику. */
    private boolean draftTouched;

    /** Каждый кадр: у «разрезов» числа слева идут за правилом, пока их не трогали; истина — числа сменились. */
    public boolean followRule()
    {
        Section s = this.section();

        if (s == null || ! (s.rule instanceof Rule.Cuts) || this.draftTouched)
        {
            return false;
        }

        int before = this.nx() * 10000 + this.ny() * 100 + this.nz();
        this.adoptRule();
        return before != this.nx() * 10000 + this.ny() * 100 + this.nz();
    }

    public int max()
    {
        return this.bySize() ? MAX_SIZE : MAX_COUNT;
    }

    public int value(int axis)
    {
        return this.bySize()
                ? switch (axis) { case 0 -> ZoneFocusConfigs.SPLIT_SIZE_X; case 1 -> ZoneFocusConfigs.SPLIT_SIZE_Y; default -> ZoneFocusConfigs.SPLIT_SIZE_Z; }
                : switch (axis) { case 0 -> ZoneFocusConfigs.SPLIT_X; case 1 -> ZoneFocusConfigs.SPLIT_Y; default -> ZoneFocusConfigs.SPLIT_Z; };
    }

    public void setValue(int axis, int value)
    {
        int v = Math.max(1, Math.min(this.max(), value));
        this.draftTouched |= v != this.value(axis);

        if (this.bySize())
        {
            switch (axis)
            {
                case 0 -> ZoneFocusConfigs.SPLIT_SIZE_X = v;
                case 1 -> ZoneFocusConfigs.SPLIT_SIZE_Y = v;
                default -> ZoneFocusConfigs.SPLIT_SIZE_Z = v;
            }
        }
        else
        {
            switch (axis)
            {
                case 0 -> ZoneFocusConfigs.SPLIT_X = v;
                case 1 -> ZoneFocusConfigs.SPLIT_Y = v;
                default -> ZoneFocusConfigs.SPLIT_Z = v;
            }
        }

        this.slice = Math.min(this.slice, this.ny() - 1);
    }

    public void nudge(int axis, int delta)
    {
        this.setValue(axis, this.value(axis) + delta);
    }

    /** Число зон по оси в черновике (в режиме «размер» — сколько выйдет). */
    public int count(int axis)
    {
        ZoneBox b = this.bounds();

        if (! this.bySize() || b == null)
        {
            return this.value(axis);
        }

        int size = axis == 0 ? b.sizeX() : axis == 1 ? b.sizeY() : b.sizeZ();
        return ZoneManager.ceilDiv(size, this.value(axis));
    }

    public int nx() { return this.count(0); }
    public int ny() { return this.count(1); }
    public int nz() { return this.count(2); }

    public int total()
    {
        return Math.max(1, this.nx() * this.ny() * this.nz());
    }

    /** Размер одной зоны черновика «примерно X×Y×Z». */
    public int[] zoneSize()
    {
        ZoneBox b = this.bounds();

        if (b == null)
        {
            return new int[]{0, 0, 0};
        }

        return new int[]{
                (int) Math.ceil(b.sizeX() / (double) this.nx()),
                (int) Math.ceil(b.sizeY() / (double) this.ny()),
                (int) Math.ceil(b.sizeZ() / (double) this.nz())};
    }

    public long zoneVolume()
    {
        ZoneBox b = this.bounds();
        return b == null ? 0L : b.volume() / this.total();
    }

    // ===== Применённые зоны =====

    private void activate()
    {
        Section s = this.section();

        if (s != null)
        {
            int index = ZoneManager.getInstance().getLayout().indexOf(s);

            if (index >= 0 && index != ZoneManager.getInstance().getActiveSectionIndex())
            {
                ZoneManager.getInstance().setActiveSection(index);
            }
        }
    }

    /** Зоны участка в нумерации менеджера. */
    public List<ZoneBox> zoneBoxes()
    {
        Section s = this.section();
        return s == null ? List.of() : ZoneManager.getInstance().zoneBoxes(s);
    }

    /** У участка есть зоны (любого вида). */
    public boolean hasZones()
    {
        return ! this.zoneBoxes().isEmpty();
    }

    /** Черновик совпадает с применённой сеткой — карта живая: клик выбирает зону. */
    public boolean applied()
    {
        Section s = this.section();
        int[] counts = s != null ? ruleCounts(s.rule) : null;

        if (counts == null || ! this.hasZones())
        {
            return false;
        }

        // Разрезы, поставленные мышью (или не с этого экрана), — применённые, пока числа слева не трогали
        if (s.rule instanceof Rule.Cuts && ! this.draftTouched)
        {
            return true;
        }

        return counts[0] == this.nx() && counts[1] == this.ny() && counts[2] == this.nz();
    }

    /**
     * Разрезы можно двигать мышью: применённые сетка или разрезы одним габаритом (одна схема, общий габарит или уже
     * разрезы). У сетки по каждой схеме группы плоскости свои у каждой схемы — одной линией их не взять.
     */
    public boolean cuttable()
    {
        Section s = this.section();

        if (s == null || ! this.applied())
        {
            return false;
        }

        return s.rule instanceof Rule.Cuts || s.bounds == Bounds.COMBINED || s.placements.size() <= 1;
    }

    /** Разрезы участка {xs, ys, zs} в блоках от угла {@link #cutBox()}; не режется — {@code null}. */
    public int @Nullable [][] cuts()
    {
        Section s = this.section();
        return s != null && this.cuttable() ? ZoneManager.getInstance().cuts(s) : null;
    }

    /** Габарит, от которого отсчитаны разрезы. */
    public @Nullable ZoneBox cutBox()
    {
        Section s = this.section();
        return s != null ? ZoneManager.getInstance().sectionBox(s) : null;
    }

    /** Подвинуть плоскость; число зон то же — активная зона остаётся. */
    public void moveCut(int axis, int index, int position)
    {
        Section s = this.section();

        if (s != null && this.cuttable())
        {
            ZoneManager.getInstance().moveCut(s, axis, index, position);
        }
    }

    /** Добавить разрез по оси в смещении {@code position} от угла габарита; черновик — по новому правилу. */
    public void addCut(int axis, int position)
    {
        Section s = this.section();
        int[][] all = this.cuts();
        ZoneBox box = this.cutBox();

        if (s == null || all == null || box == null || position <= 0 || position >= size(box, axis))
        {
            return;
        }

        int[] next = java.util.Arrays.copyOf(all[axis], all[axis].length + 1);
        next[next.length - 1] = position;
        all[axis] = next;
        ZoneManager.getInstance().setCuts(s, all[0], all[1], all[2]);
        this.adoptRule();
        this.slice = Math.min(this.slice, this.slices() - 1);
    }

    /** Убрать разрез; черновик — по новому правилу. */
    public void removeCut(int axis, int index)
    {
        Section s = this.section();
        int[][] all = this.cuts();

        if (s == null || all == null || index < 0 || index >= all[axis].length)
        {
            return;
        }

        int[] next = new int[all[axis].length - 1];

        for (int i = 0, j = 0; i < all[axis].length; ++i)
        {
            if (i != index)
            {
                next[j++] = all[axis][i];
            }
        }

        all[axis] = next;
        ZoneManager.getInstance().setCuts(s, all[0], all[1], all[2]);
        this.adoptRule();
        this.slice = Math.min(this.slice, this.slices() - 1);
    }

    public boolean subRegions()
    {
        Section s = this.section();
        return s != null && this.hasZones() && s.rule instanceof Rule.SubRegions;
    }

    public boolean manual()
    {
        Section s = this.section();
        return s != null && this.hasZones() && s.rule instanceof Rule.Manual;
    }

    /** Карта показывает настоящие зоны участка (сетка совпала с черновиком, подрегионы или ручные), а не черновик. */
    public boolean live()
    {
        return this.applied() || this.subRegions() || this.manual();
    }

    public void apply()
    {
        Section s = this.section();
        ZoneManager manager = ZoneManager.getInstance();

        if (s != null)
        {
            if (this.bySize())
            {
                manager.splitGrid(s, this.nx(), this.ny(), this.nz());
            }
            else
            {
                manager.splitGrid(s, ZoneFocusConfigs.SPLIT_X, ZoneFocusConfigs.SPLIT_Y, ZoneFocusConfigs.SPLIT_Z);
            }
        }
        else if (this.placement != null)
        {
            if (this.bySize())
            {
                manager.splitBySize(this.placement,
                        ZoneFocusConfigs.SPLIT_SIZE_X, ZoneFocusConfigs.SPLIT_SIZE_Y, ZoneFocusConfigs.SPLIT_SIZE_Z);
            }
            else
            {
                manager.splitGrid(this.placement,
                        ZoneFocusConfigs.SPLIT_X, ZoneFocusConfigs.SPLIT_Y, ZoneFocusConfigs.SPLIT_Z);
            }
        }

        new ZoneFocusConfigs().save();
        this.draftTouched = false;

        if (this.active() >= 0)
        {
            this.slice = this.sliceOf(this.active());
        }
    }

    public void splitBySubRegions()
    {
        Section s = this.section();

        if (s != null)
        {
            ZoneManager.getInstance().splitBySubRegions(s);
        }
        else if (this.placement != null)
        {
            ZoneManager.getInstance().splitBySubRegions(this.placement);
        }
    }

    /** «Отменить разбиение участка»: участок удаляется, его схемы снова вне раскладки. */
    public void undo()
    {
        Section s = this.section();

        if (s != null)
        {
            ZoneManager.getInstance().removeSection(ZoneManager.getInstance().getLayout().indexOf(s));
        }
    }

    /** «Очистить всё»: вся раскладка. */
    public void clearAll()
    {
        ZoneManager.getInstance().clear();
    }

    /** Коробки Manual-участка в текущих координатах; у других правил — пусто. */
    public List<ZoneBox> manualBoxes()
    {
        Section s = this.section();
        return s == null ? List.of() : ZoneManager.getInstance().manualBoxes(s);
    }

    /** Удалить зону Manual-участка; у других правил — ничего. */
    public void removeZone(int index)
    {
        Section s = this.section();

        if (s != null && s.rule instanceof Rule.Manual)
        {
            ZoneManager.getInstance().removeManualBox(s, index);
        }
    }

    public int active()
    {
        Section s = this.section();

        if (s == null || ! this.hasZones())
        {
            return -1;
        }

        int count = this.zoneBoxes().size();
        return Math.floorMod(s.activeZone, count);
    }

    public void select(int index)
    {
        Section s = this.section();

        if (s == null || ! this.hasZones())
        {
            return;
        }

        ZoneManager.getInstance().setActiveZone(s, index);

        if (this.applied())
        {
            this.slice = this.sliceOf(index);
        }
    }

    /** Срезы карты по оси глубины её плоскости (сверху — по Y, спереди — по Z, сбоку — по X). */
    public int slices()
    {
        return this.subRegions() || this.manual() ? 1 : this.count(depthAxis());
    }

    public int slice()
    {
        return this.slice;
    }

    public void setSlice(int value)
    {
        this.slice = Math.max(0, Math.min(this.slices() - 1, value));
    }

    /** Срез зоны по оси глубины карты: номер её доли по этой оси (порядок зон Y→Z→X, блоками по схемам). */
    public int sliceOf(int index)
    {
        int nx = Math.max(1, this.nx());
        int ny = Math.max(1, this.ny());
        int nz = Math.max(1, this.nz());
        int local = index % (nx * ny * nz);
        int part = switch (depthAxis())
        {
            case 0 -> local % nx;
            case 2 -> (local / nx) % nz;
            default -> local / (nx * nz);
        };

        return Math.max(0, Math.min(this.slices() - 1, part));
    }

    /** Черновик сетки по числам слева — те же коробки и номера, что даст «Разбить» (порядок Y→Z→X). */
    public List<ZoneBox> draftBoxes()
    {
        List<ZoneBox> out = new ArrayList<>();
        ZoneBox b = this.bounds();

        if (b == null)
        {
            return out;
        }

        for (int[] yr : ZoneManager.slices(b.minY(), b.maxY(), this.ny()))
        {
            for (int[] zr : ZoneManager.slices(b.minZ(), b.maxZ(), this.nz()))
            {
                for (int[] xr : ZoneManager.slices(b.minX(), b.maxX(), this.nx()))
                {
                    out.add(new ZoneBox(xr[0], yr[0], zr[0], xr[1], yr[1], zr[1]));
                }
            }
        }

        return out;
    }

    // ===== Карта =====

    private @Nullable ZoneBox mapBox;
    private double mapScale;

    /** Где на экране лежит сама схема в последней раскладке {@link #cells}: x, y, w, h. */
    public int[] mapRect()
    {
        return this.mapRect;
    }

    /** Пикселей на блок в последней раскладке карты; 0 — карты нет. */
    public double mapScale()
    {
        return this.mapScale;
    }

    /**
     * Клетки карты в области {@code x, y, w, h} в плоскости {@link #mapView}. Сетка — на выбранном срезе по оси
     * глубины; при зонах по подрегионам и ручных — все зоны.
     */
    public List<Cell> cells(int x, int y, int w, int h)
    {
        List<Cell> out = new ArrayList<>();
        ZoneBox b = this.bounds();
        this.mapBox = b;

        if (b == null || w < 8 || h < 8)
        {
            this.mapScale = 0.0;
            return out;
        }

        int sizeH = size(b, hAxis());
        int sizeV = size(b, vAxis());
        double scale = Math.min(w / (double) sizeH, h / (double) sizeV);
        int drawnW = Math.max(2, (int) Math.round(sizeH * scale));
        int drawnH = Math.max(2, (int) Math.round(sizeV * scale));
        int ox = x + (w - drawnW) / 2;
        int oy = y + (h - drawnH) / 2;
        this.mapRect = new int[]{ox, oy, drawnW, drawnH};
        this.mapScale = scale;

        if (this.subRegions() || this.manual())
        {
            List<ZoneBox> boxes = this.zoneBoxes();

            for (int i = 0; i < boxes.size(); ++i)
            {
                out.add(this.rect(i, boxes.get(i), false));
            }

            return out;
        }

        // Применённая сетка — настоящие зоны участка (в том числе по нескольким схемам), иначе черновик
        boolean applied = this.applied();
        List<ZoneBox> boxes = applied ? this.zoneBoxes() : this.draftBoxes();

        for (int i = 0; i < boxes.size(); ++i)
        {
            if (this.sliceOf(i) == this.slice)
            {
                out.add(this.rect(i, boxes.get(i), applied && this.isMissing(i)));
            }
        }

        return out;
    }

    private Cell rect(int index, ZoneBox z, boolean missing)
    {
        int[] r = this.rectOf(z);
        return new Cell(index, r[0], r[1], r[2], r[3], missing);
    }

    /** Коробка мира → прямоугольник карты {x, y, w, h} в последней раскладке (снимок под клетками, подрегионы). */
    public int[] rectOf(ZoneBox z)
    {
        ZoneBox b = this.mapBox;

        if (b == null || this.mapScale <= 0.0)
        {
            return new int[] { 0, 0, 0, 0 };
        }

        int[] h = range(z, b, hAxis(), flipH(), this.mapScale, this.mapRect[0]);
        int[] v = range(z, b, vAxis(), flipV(), this.mapScale, this.mapRect[1]);
        return new int[] { h[0], v[0], Math.max(1, h[1] - h[0]), Math.max(1, v[1] - v[0]) };
    }

    private static int[] range(ZoneBox z, ZoneBox b, int axis, boolean flip, double scale, int origin)
    {
        int lo = min(z, axis) - min(b, axis);
        int hi = max(z, axis) + 1 - min(b, axis);

        if (flip)
        {
            int size = size(b, axis);
            int t = size - hi;
            hi = size - lo;
            lo = t;
        }

        return new int[] { origin + (int) Math.round(lo * scale), origin + (int) Math.round(hi * scale) };
    }

    /**
     * Плоскость разреза (смещение {@code offset} от угла габарита {@code box} по оси) → экранная координата линии на
     * карте: X для оси вправо, Y для оси вниз; ось не в плоскости карты — {@code NaN}.
     */
    public double cutScreen(ZoneBox box, int axis, int offset)
    {
        ZoneBox b = this.mapBox;

        if (b == null || this.mapScale <= 0.0)
        {
            return Double.NaN;
        }

        int world = min(box, axis) + offset;

        if (axis == hAxis())
        {
            double c = world - min(b, axis);
            return this.mapRect[0] + (flipH() ? size(b, axis) - c : c) * this.mapScale;
        }

        if (axis == vAxis())
        {
            double c = world - min(b, axis);
            return this.mapRect[1] + (flipV() ? size(b, axis) - c : c) * this.mapScale;
        }

        return Double.NaN;
    }

    /** Экранная координата → ближайшая плоскость между блоками, смещение от угла {@code box} по оси. */
    public int screenToCut(ZoneBox box, int axis, double screen)
    {
        ZoneBox b = this.mapBox;

        if (b == null || this.mapScale <= 0.0)
        {
            return 0;
        }

        boolean h = axis == hAxis();
        double c = (screen - (h ? this.mapRect[0] : this.mapRect[1])) / this.mapScale;

        if (h ? flipH() : flipV())
        {
            c = size(b, axis) - c;
        }

        return (int) Math.round(c) + min(b, axis) - min(box, axis);
    }

    /** Блок мира под точкой карты по оси (вправо или вниз). */
    public int screenToBlock(int axis, double screen)
    {
        ZoneBox b = this.mapBox;

        if (b == null || this.mapScale <= 0.0)
        {
            return 0;
        }

        boolean h = axis == hAxis();
        int c = (int) Math.floor((screen - (h ? this.mapRect[0] : this.mapRect[1])) / this.mapScale);

        if (h ? flipH() : flipV())
        {
            c = size(b, axis) - 1 - c;
        }

        return min(b, axis) + c;
    }

    /** Подрегионы схемы в масштабе последней раскладки — контур схемы на карте. */
    public List<int[]> regions()
    {
        List<int[]> out = new ArrayList<>();
        ZoneBox b = this.bounds();
        int[] r = this.mapRect;

        if (b == null || r[2] <= 0)
        {
            return out;
        }

        List<SchematicPlacement> placements = new ArrayList<>();
        PlacementMatcher.Resolved res = this.resolved();

        if (res != null)
        {
            placements.addAll(res.found());
        }
        else if (this.placement != null)
        {
            placements.add(this.placement);
        }

        for (SchematicPlacement p : placements)
        {
            for (Box sub : p.getSubRegionBoxes(SubRegionPlacement.RequiredEnabled.PLACEMENT_ENABLED).values())
            {
                out.add(this.rectOf(ZoneBox.of(sub)));
            }
        }

        return out;
    }

    // ===== Список и прогресс =====

    /**
     * Зона принадлежит ссылке участка без найденного размещения (Grid по отдельным схемам: зоны идут по ссылкам
     * в порядке {@code placements}, ссылки без габарита зон не дают).
     */
    private boolean isMissing(int index)
    {
        Section s = this.section();
        PlacementMatcher.Resolved res = this.resolved();

        if (s == null || res == null || ! (s.rule instanceof Rule.Grid) || s.bounds == Bounds.COMBINED)
        {
            return false;
        }

        int per = Math.max(1, this.nx() * this.ny() * this.nz());
        int owner = index / per;
        int seen = 0;

        for (PlacementRef ref : s.placements)
        {
            SchematicPlacement live = res.of(ref);
            boolean hasBox = live != null ? ZoneManager.enclosingBox(live) != null || ref.box != null : ref.box != null;

            if (! hasBox)
            {
                continue;
            }

            if (seen++ == owner)
            {
                return live == null;
            }
        }

        return false;
    }

    public List<Row> rows()
    {
        List<Row> out = new ArrayList<>();
        Section s = this.section();

        if (s == null || ! this.hasZones())
        {
            return out;
        }

        List<ZoneBox> boxes = this.zoneBoxes();
        var player = Minecraft.getInstance().player;

        for (int index = 0; index < boxes.size(); ++index)
        {
            if (! s.showHidden && s.hiddenZones.contains(index))
            {
                continue;
            }

            ZoneBox box = boxes.get(index);
            int distance = player == null ? -1 : (int) Math.sqrt(player.position().distanceToSqr(box.center()));
            out.add(new Row(index, box, this.stats(index), distance, this.isMissing(index)));
        }

        switch (sortMode)
        {
            case 1 -> out.sort(java.util.Comparator.comparingLong(r -> r.stats() == null || r.stats().total() == 0L
                    ? Long.MAX_VALUE : r.stats().percent()));
            case 2 -> out.sort(java.util.Comparator.comparingLong(r -> r.stats() == null || r.stats().total() == 0L
                    ? Long.MAX_VALUE : -r.stats().missing()));
            case 3 -> out.sort(java.util.Comparator.comparingInt(Row::distance));
            default -> { }
        }

        return out;
    }

    public boolean isHidden(int index)
    {
        Section s = this.section();
        return s != null && s.hiddenZones.contains(index);
    }

    public void toggleHidden(int index)
    {
        this.activate();
        ZoneManager.getInstance().toggleHidden(index);
    }

    public int hiddenCount()
    {
        Section s = this.section();
        return s == null || ! this.hasZones() ? 0 : s.hiddenZones.size();
    }

    public void showAllZones()
    {
        this.activate();
        ZoneManager.getInstance().showAllZones();
    }

    public @Nullable ZoneScanner.Stats stats(int index)
    {
        Section s = this.section();
        ZoneScanner scanner = ZoneScanner.getInstance();
        return s != null && scanner.hasStats(s) ? scanner.getStats(s, index) : null;
    }

    public boolean scanning()
    {
        Section s = this.section();
        return s != null && ZoneScanner.getInstance().isScanning(s);
    }

    public void scan()
    {
        Section s = this.section();

        if (s != null)
        {
            ZoneScanner.getInstance().requestScan(s);
        }
    }
}
