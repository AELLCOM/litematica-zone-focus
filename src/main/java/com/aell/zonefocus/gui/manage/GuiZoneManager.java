package com.aell.zonefocus.gui.manage;

import com.aell.zonefocus.shaded.ui.Paint;
import com.aell.zonefocus.shaded.ui.Strings;
import com.aell.zonefocus.shaded.ui.Style;
import com.aell.zonefocus.shaded.ui.Ui;
import com.aell.zonefocus.shaded.ui.Vanilla;
import com.aell.zonefocus.shaded.ui.kit.Button;
import com.aell.zonefocus.shaded.ui.kit.ContextMenu;
import com.aell.zonefocus.shaded.ui.kit.Dropdown;
import com.aell.zonefocus.shaded.ui.kit.Kit;
import com.aell.zonefocus.shaded.ui.kit.Layers;
import com.aell.zonefocus.shaded.ui.kit.ScrollList;
import com.aell.zonefocus.shaded.ui.kit.Slider;
import com.aell.zonefocus.shaded.ui.kit.TextFocus;
import com.aell.zonefocus.shaded.ui.kit.TextInput;
import com.aell.zonefocus.shaded.ui.kit.Toggle;
import com.aell.zonefocus.shaded.ui.kit.Tooltip;
import com.aell.zonefocus.shaded.ui.kit.Widget;
import com.aell.zonefocus.compat.MaterialHudBridge;
import com.aell.zonefocus.compat.MaterialHudCompat;
import com.aell.zonefocus.render.ZoneRenderer;
import com.aell.zonefocus.gui.GuiSectionSplit;
import com.aell.zonefocus.zone.FileZones;
import com.aell.zonefocus.zone.ZoneBox;
import com.aell.zonefocus.zone.ZoneManager;
import com.aell.zonefocus.zone.ZoneScanner;
import com.aell.zonefocus.zone.model.Rule;
import com.aell.zonefocus.zone.model.Section;
import com.aell.zonefocus.zone.scope.PlacementMatcher;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.StringUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Util;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * «Управление зонами», вариант А — на ките aell-ui (те же кнопки, поля и списки, что в Material HUD).
 * Слева способ и сетка по осям с полями ввода, справа карта (номер на каждой клетке, клик — активная
 * зона) и список зон с прогрессом. Логика — {@link ZoneEditor}, карта — {@link ZoneMap}.
 *
 * <p>С Material HUD 1.0.5+ в шапке переключатель «2D | 3D»: в 3D карта — превью схемы из его библиотеки
 * (изометрия, вращение и масштаб мышью, кнопки «Изометрия», «Сверху», «Спереди», как в библиотеке) с каркасами
 * и номерами зон; щелчок по зоне делает её активной, «Прогресс» красит каркасы. В 2D под клетками карты — снимок
 * схемы строго сверху (без вращения) в повороте размещения, с несколькими срезами Y — срезанный по текущему.
 * Ручки превью — объекты Material HUD, все вызовы — через {@link MaterialHudBridge}.
 */
@Environment(EnvType.CLIENT)
public final class GuiZoneManager extends GuiBase
{
    private static final Identifier ICON = Identifier.fromNamespaceAndPath("zonefocus", "icon.png");
    private static final int LEFT_MIN = 220;
    private static final int LEFT_MAX = 300;
    private static final int CTRL_H = 18;
    private static final int KEY_UP = 265;
    private static final int KEY_DOWN = 264;
    private static final int KEY_ESC = 256;
    private static final long CONFIRM_MS = 3000L;

    static final ZoneMap.Palette PALETTE = new ZoneMap.Palette(
            0xF0101014, Ui.C_DIVIDER, 0x307AA8F5, 0x807AA8F5, 0x60E0B341, Ui.C_TEXT_2, Ui.C_TEXT_3,
            0xFF50D0FF, 0x3050D0FF, Ui.C_TEXT, Ui.C_ON, Ui.C_GOLD, Ui.C_DANGER, Ui.C_TEXT_2);

    private ZoneEditor editor;
    private final Layers layers = new Layers();
    private final List<Widget> widgets = new ArrayList<>();
    private final Button[] modes = new Button[2];
    private final TextInput[] fields = new TextInput[3];
    private final Button[] minus = new Button[3];
    private final Button[] plus = new Button[3];
    private final List<Button> sliceButtons = new ArrayList<>();
    private Button split;
    private Button subRegions;
    private Button undo;
    private Button scan;
    private Button done;
    private Button showHidden;
    private Dropdown<Integer> sort;
    private Toggle progress;
    private @Nullable Button view2d;
    private @Nullable Button view3d;
    /** В 3D: показать или убрать каркасы зон — посмотреть на саму постройку. */
    private @Nullable Button frames;
    /** В 3D, рядом с 2D / 3D: качество модели превью (настройка Material HUD). */
    private @Nullable Button qualityHi;
    private @Nullable Button qualityLo;
    /** В 3D: предустановки камеры, как у превью библиотеки Material HUD; «Сбоку» — с Material HUD API 6. */
    private final Button[] views = new Button[4];
    private static final String[] VIEW_NAMES = {"ISOMETRIC", "TOP", "FRONT", "SIDE"};
    /** В 2D: плоскость карты — сверху, спереди, сбоку ({@link ZoneEditor#mapView}). */
    private final Button[] mapViews = new Button[3];
    private static final String[] MAP_VIEW_NAMES = {"TOP", "FRONT", "SIDE"};
    /** В 3D: срез по высоте — блоки выше не рисуются (как ползунок «Срез» библиотеки Material HUD). */
    private @Nullable Slider layerCut;
    private int layerCutMax = -1;
    private final CutTool cutTool = new CutTool();
    /** Где нажали кнопку мыши в 3D — отличить щелчок от протяжки. */
    private double pressX;
    private double pressY;
    /** Превью Material HUD для 3D; {@code null} — ещё не нужно или Material HUD старее. */
    private @Nullable Object preview;
    /** Превью Material HUD для 2D — снимок схемы сверху под клетками карты; {@code null} — ещё не нужно. */
    private @Nullable Object topView;
    private boolean topViewBroken;
    private boolean dragging3d;
    private int previewPoll;
    private final ScrollList<ZoneEditor.Row> list = new ScrollList<>(this::renderRow);
    /** Секция «Участки»: список при нескольких участках, при одном — строка-заголовок. */
    private final ScrollList<Section> sectionList = new ScrollList<>(this::renderSectionRow);
    private Button addSection;
    private Button clearAll;
    private TextInput renameField;
    private @Nullable Section renaming;
    private @Nullable Section seenActive;
    private int lastSectionCount = -1;
    private int sectionsH;
    private int leftTop;
    /** Подтверждение вторым щелчком: что взведено → до какого времени. */
    private final Map<String, Long> armed = new HashMap<>();
    private String statusText = "";
    private long statusUntil;

    private int px;
    private int py;
    private int pw;
    private int ph;
    private int leftW;
    private int rowsY;
    private int mapX;
    private int mapY;
    private int mapW;
    private int mapH;
    private int listY;
    private long undoArmedUntil;
    private int lastSlices = -1;

    public GuiZoneManager(@Nullable Screen parent, SchematicPlacement placement)
    {
        this.setParent(parent);
        this.editor = new ZoneEditor(placement);
        this.seenActive = ZoneManager.getInstance().activeSection();
        this.title = Strings.plain("zonefocus.gui.manager.title");
    }

    public GuiZoneManager(@Nullable Screen parent, Section section)
    {
        this.setParent(parent);
        this.editor = new ZoneEditor(section);
        this.seenActive = ZoneManager.getInstance().activeSection();
        this.title = Strings.plain("zonefocus.gui.manager.title");
    }

    /** Пересоздать редактор на другой участок. */
    private void reload(Section section)
    {
        this.editor = new ZoneEditor(section);
        this.seenActive = section;
        this.lastSlices = -1;
        this.syncFields(true);
        this.layout();
    }

    @Override
    public void initGui()
    {
        super.initGui();
        ZoneRenderer.setGridVisibleFromGui(true);
        this.layers.setScreen(this.width, this.height);
        this.build();
        this.layout();
    }

    private static String t(String key)
    {
        return Strings.plain("zonefocus.gui.manager." + key);
    }

    private static String sec(String key)
    {
        return Strings.plain("zonefocus.gui.sections." + key);
    }

    private static String sec(String key, Object... args)
    {
        return StringUtils.translate("zonefocus.gui.sections." + key, args);
    }

    private void build()
    {
        this.widgets.clear();

        this.modes[0] = new Button(t("mode_count"), Button.Style.SECONDARY, () -> this.setMode(false));
        this.modes[1] = new Button(t("mode_size"), Button.Style.SECONDARY, () -> this.setMode(true));

        for (int axis = 0; axis < 3; ++axis)
        {
            final int a = axis;
            this.minus[a] = new Button("−", Button.Style.SECONDARY, () -> this.nudge(a, -1)).centered(true);
            this.plus[a] = new Button("+", Button.Style.SECONDARY, () -> this.nudge(a, 1)).centered(true);
            this.fields[a] = new TextInput().maxLength(3).centered(true).filter(c -> c >= '0' && c <= '9')
                    .onChange(text -> this.typed(a, text))
                    .onSubmit(text -> this.fields[a].setFocused(false))
                    .tooltip(t("field_tip"));
            this.widgets.add(this.minus[a]);
            this.widgets.add(this.fields[a]);
            this.widgets.add(this.plus[a]);
        }

        this.split = new Button("", Button.Style.PRIMARY, this::apply).centered(true);
        this.subRegions = new Button(t("sub_regions"), Button.Style.SECONDARY, () -> {
            this.editor.splitBySubRegions();
            this.refresh();
        }).centered(true).tooltip(t("sub_regions_tip"));
        this.undo = new Button(t("undo"), Button.Style.DANGER, this::undo).centered(true).tooltip(t("undo_tip"));
        this.scan = new Button(t("scan"), Button.Style.PRIMARY, () -> this.editor.scan()).accent(Ui.C_GOLD).centered(true);
        this.done = new Button(t("done"), Button.Style.SECONDARY, () -> this.closeGui(true)).centered(true);
        this.progress = new Toggle(t("progress"), () -> ZoneEditor.showProgress, v -> ZoneEditor.showProgress = v)
                .tooltip(t("progress_tip"));

        this.list.showSelection(true).rowHeight(row -> 18).gap(Ui.ROW_GAP);
        this.list.listener(new ScrollList.Listener<>()
        {
            @Override
            public void select(ZoneEditor.@Nullable Row item, int index)
            {
                if (item != null && item.index() != GuiZoneManager.this.editor.active())
                {
                    GuiZoneManager.this.editor.select(item.index());
                }
            }

            @Override
            public void context(ZoneEditor.Row item, int index, int mouseX, int mouseY)
            {
                GuiZoneManager.this.rowMenu(item, mouseX, mouseY);
            }
        });
        this.sort = new Dropdown<>(this.layers, List.of(0, 1, 2, 3), ZoneEditor.sortMode,
                mode -> Strings.plain(ZoneEditor.SORT_KEYS[mode]), mode -> ZoneEditor.sortMode = mode)
                .tooltip(t("sort_tip"));
        this.showHidden = new Button("", Button.Style.GHOST, () -> {
            this.editor.showAllZones();
            this.refresh();
        }).centered(true);
        this.list.setEmptyText(t("list_empty"));

        this.addSection = new Button(sec("add"), Button.Style.SECONDARY,
                () -> GuiSectionSplit.open(this, null)).centered(true).tooltip(sec("add_tip"));
        this.clearAll = new Button(t("clear_all"), Button.Style.GHOST, () -> this.confirm("clearall", this::clearEverything))
                .centered(true).tooltip(t("clear_all_tip"));
        this.renameField = new TextInput().maxLength(40).tooltip(sec("rename_tip"))
                .onSubmit(text -> this.commitRename()).onFocus(() -> { });
        this.renameField.setVisible(false);
        this.sectionList.showSelection(true).rowHeight(row -> SECTION_ROW_H).gap(Ui.ROW_GAP);
        this.sectionList.listener(new ScrollList.Listener<>()
        {
            @Override
            public void select(@Nullable Section item, int index)
            {
                if (item != null)
                {
                    ZoneManager.getInstance().setActiveSection(index);
                    GuiZoneManager.this.reload(item);
                }
            }

            @Override
            public void context(Section item, int index, int mouseX, int mouseY)
            {
                GuiZoneManager.this.sectionMenu(item, mouseX, mouseY);
            }
        });

        if (MaterialHudCompat.hasPreview())
        {
            this.view2d = new Button("2D", Button.Style.SECONDARY, () -> this.setView3d(false)).centered(true)
                    .tooltip(t("view2d_tip"));
            this.view3d = new Button("3D", Button.Style.SECONDARY, () -> this.setView3d(true)).centered(true)
                    .tooltip(t("view3d_tip"));
            this.frames = new Button(t("frames"), Button.Style.SECONDARY, () -> {
                ZoneEditor.showFrames = ! ZoneEditor.showFrames;
                this.previewPoll = 0;
            }).centered(true).tooltip(t("frames_tip"));
            this.qualityHi = new Button(t("quality"), Button.Style.SECONDARY, () -> this.setQuality(true)).centered(true)
                    .tooltip(t("quality_tip"));
            this.qualityLo = new Button(t("optimized"), Button.Style.SECONDARY, () -> this.setQuality(false)).centered(true)
                    .tooltip(t("quality_tip"));
            String[] labels = {t("view_iso"), t("view_top"), t("view_front"), t("view_side")};
            int viewCount = MaterialHudCompat.hasSectionPreview() ? 4 : 3;

            for (int i = 0; i < viewCount; ++i)
            {
                final String name = VIEW_NAMES[i];
                this.views[i] = new Button(labels[i], Button.Style.SECONDARY, () -> this.setCameraView(name)).centered(true);
                this.widgets.add(this.views[i]);
            }

            if (MaterialHudCompat.hasSectionPreview())
            {
                this.layerCut = new Slider(0, 0, 0, this::onLayerCut).tooltip(t("layer_cut_tip"));
                this.widgets.add(this.layerCut);
            }

            this.widgets.add(this.view2d);
            this.widgets.add(this.view3d);
            this.widgets.add(this.frames);
            this.widgets.add(this.qualityHi);
            this.widgets.add(this.qualityLo);
        }

        String[] mapLabels = {t("view_top"), t("view_front"), t("view_side")};

        for (int i = 0; i < this.mapViews.length; ++i)
        {
            final int view = i;
            this.mapViews[i] = new Button(mapLabels[i], Button.Style.SECONDARY, () -> this.setMapView(view)).centered(true)
                    .tooltip(t("map_view_tip"));
            this.widgets.add(this.mapViews[i]);
        }

        this.widgets.add(this.modes[0]);
        this.widgets.add(this.modes[1]);
        this.widgets.add(this.split);
        this.widgets.add(this.subRegions);
        this.widgets.add(this.undo);
        this.widgets.add(this.scan);
        this.widgets.add(this.done);
        this.widgets.add(this.progress);
        this.widgets.add(this.sort);
        this.widgets.add(this.showHidden);
        this.widgets.add(this.list);
        this.widgets.add(this.sectionList);
        this.widgets.add(this.addSection);
        this.widgets.add(this.clearAll);
        this.widgets.add(this.renameField);
        this.syncFields(true);
    }

    private static final int SECTION_ROW_H = 16;
    private static final int SECTION_ROWS_MAX = 3;
    private static final int LABEL_H = 11;
    private static final int AXIS_STEP = Ui.ROW_H + 1;

    private int top;
    private int chipsY;
    private int listLabelY;

    private void layout()
    {
        this.px = Ui.OUTER;
        this.py = Ui.OUTER;
        this.pw = this.width - Ui.OUTER * 2;
        this.ph = this.height - Ui.OUTER * 2;

        this.top = this.py + Ui.HEADER_H + Ui.PAD_GROUP;
        int x = this.px + Ui.PAD_SECTION;
        this.leftW = Math.max(LEFT_MIN, Math.min(LEFT_MAX, this.pw * 38 / 100));
        int bottom = this.py + this.ph - Ui.PAD_GROUP - CTRL_H - Ui.PAD_GROUP;

        // Шапка: тумблер прогресса справа
        int toggleW = this.progress.preferredWidth(this.font);
        this.progress.setBounds(this.px + this.pw - Ui.PAD_SECTION - toggleW, this.py + (Ui.HEADER_H - CTRL_H) / 2,
                toggleW, CTRL_H);

        if (this.view2d != null && this.view3d != null)
        {
            int seg = 26;
            int segX = this.progress.x() - Ui.PAD_GROUP - seg * 2 - Ui.PAD_MICRO;
            this.view2d.setBounds(segX, this.progress.y(), seg, CTRL_H);
            this.view3d.setBounds(segX + seg + Ui.PAD_MICRO, this.progress.y(), seg, CTRL_H);

            if (this.qualityHi != null && this.qualityLo != null)
            {
                int loW = this.qualityLo.preferredWidth(this.font);
                int hiW = this.qualityHi.preferredWidth(this.font);
                int qx = segX - Ui.PAD_REL - loW;
                this.qualityLo.setBounds(qx, this.progress.y(), loW, CTRL_H);
                this.qualityHi.setBounds(qx - Ui.PAD_MICRO - hiW, this.progress.y(), hiW, CTRL_H);
            }
        }

        // Слева сверху вниз: участки, способ, сетка по осям, итог чипами, действия
        int n = ZoneManager.getInstance().sections().size();
        this.lastSectionCount = n;
        int addW = this.addSection.preferredWidth(this.font);

        if (n > 1)
        {
            int rows = Math.min(n, SECTION_ROWS_MAX);
            int listH = rows * (SECTION_ROW_H + Ui.ROW_GAP) - Ui.ROW_GAP;
            this.addSection.setBounds(x + this.leftW - addW, this.top - 3, addW, 14);
            this.sectionList.setBounds(x, this.top + LABEL_H + Ui.PAD_MICRO, this.leftW, listH);
            this.sectionList.setVisible(true);
            this.sectionsH = LABEL_H + Ui.PAD_MICRO + listH + Ui.PAD_GROUP;
        }
        else
        {
            this.addSection.setBounds(x + this.leftW - addW, this.top + (CTRL_H - 14) / 2, addW, 14);
            this.sectionList.setVisible(false);
            this.sectionsH = CTRL_H + Ui.PAD_GROUP;
        }

        this.renameField.setBounds(x, this.top, this.leftW, CTRL_H);
        this.leftTop = this.top + this.sectionsH;
        int y = this.leftTop + LABEL_H;
        int segW = (this.leftW - Ui.PAD_IN) / 2;
        this.modes[0].setBounds(x, y, segW, CTRL_H);
        this.modes[1].setBounds(x + segW + Ui.PAD_IN, y, this.leftW - segW - Ui.PAD_IN, CTRL_H);
        y += CTRL_H + Ui.PAD_GROUP;
        this.rowsY = y + LABEL_H;

        for (int axis = 0; axis < 3; ++axis)
        {
            int ry = this.rowsY + axis * AXIS_STEP;
            int right = x + this.leftW;
            this.plus[axis].setBounds(right - CTRL_H, ry + 1, CTRL_H, CTRL_H);
            this.fields[axis].setBounds(right - CTRL_H - Ui.PAD_MICRO - 40, ry + 1, 40, CTRL_H);
            this.minus[axis].setBounds(right - CTRL_H * 2 - Ui.PAD_MICRO * 2 - 40, ry + 1, CTRL_H, CTRL_H);
        }

        this.chipsY = this.rowsY + 3 * AXIS_STEP + Ui.PAD_IN;
        int actionsY = this.chipsY + 14 + Ui.PAD_GROUP;
        this.split.setBounds(x, actionsY, this.leftW, Ui.ROW_H);
        int step = Ui.ROW_H + Ui.PAD_IN;

        if (actionsY + step * 3 <= bottom)
        {
            // Места хватает — каждое действие во всю ширину, подписи целиком
            this.subRegions.setBounds(x, actionsY + step, this.leftW, Ui.ROW_H);
            this.undo.setBounds(x, actionsY + step * 2, this.leftW, Ui.ROW_H);
        }
        else
        {
            int half = (this.leftW - Ui.PAD_IN) / 2;
            this.subRegions.setBounds(x, actionsY + step, half, Ui.ROW_H);
            this.undo.setBounds(x + half + Ui.PAD_IN, actionsY + step, this.leftW - half - Ui.PAD_IN, Ui.ROW_H);
        }

        // Справа: карта (срезы Y — в строке её подписи), список
        this.mapX = x + this.leftW + Ui.PAD_SECTION;
        this.mapW = this.px + this.pw - Ui.PAD_SECTION - this.mapX;
        this.mapY = this.top + LABEL_H + Ui.PAD_MICRO;
        int body = bottom - this.mapY;
        // В 3D схема высокая — карте больше места, списку меньше
        this.mapH = Math.max(50, body * (this.in3d() ? 62 : 52) / 100);
        this.listLabelY = this.mapY + this.mapH + Ui.PAD_GROUP;

        if (this.frames != null)
        {
            int fw = this.frames.preferredWidth(this.font);
            this.frames.setBounds(this.mapX + this.mapW - fw - Ui.PAD_IN, this.mapY + Ui.PAD_IN, fw, 14);
        }

        // Виды камеры (3D) и плоскости карты (2D) — над превью, справа в строке его подписи (как полоска над превью
        // библиотеки): поверх модели полупрозрачный фон кнопок не читался бы
        int vr = this.mapX + this.mapW;

        for (int i = this.views.length - 1; i >= 0; --i)
        {
            Button b = this.views[i];

            if (b != null)
            {
                int bw = b.preferredWidth(this.font);
                vr -= bw;
                b.setBounds(vr, this.top - 4, bw, 14);
                vr -= Ui.PAD_MICRO;
            }
        }

        int mr = this.mapX + this.mapW;

        for (int i = this.mapViews.length - 1; i >= 0; --i)
        {
            Button b = this.mapViews[i];
            int bw = b.preferredWidth(this.font);
            mr -= bw;
            b.setBounds(mr, this.top - 4, bw, 14);
            mr -= Ui.PAD_MICRO;
        }

        this.sliceRight = mr - Ui.PAD_REL;

        // Срез по высоте в 3D — полоской внизу превью, над моделью
        if (this.layerCut != null)
        {
            int sw = Math.min(180, Math.max(60, this.mapW / 3));
            this.layerCut.setBounds(this.mapX + this.mapW - Ui.PAD_IN - sw, this.mapY + this.mapH - Ui.PAD_IN - 12, sw, 12);
        }
        this.listY = this.listLabelY + LABEL_H + Ui.PAD_MICRO;
        this.list.setBounds(this.mapX, this.listY, this.mapW, Math.max(20, bottom - this.listY));
        int scanW = this.scan.preferredWidth(this.font);
        this.scan.setBounds(this.mapX + this.mapW - scanW, this.listLabelY - 3, scanW, 14);
        int sortW = Math.min(150, this.sort.preferredWidth(this.font));
        this.sort.setBounds(this.scan.x() - Ui.PAD_IN - sortW, this.listLabelY - 3, sortW, 14);

        this.layoutSlices(this.top - 4);

        int doneW = Math.max(70, this.done.preferredWidth(this.font));
        this.done.setBounds(this.px + this.pw - Ui.PAD_SECTION - doneW, this.py + this.ph - Ui.PAD_GROUP - CTRL_H, doneW, CTRL_H);
        int clearW = this.clearAll.preferredWidth(this.font);
        this.clearAll.setBounds(x, this.done.y(), clearW, CTRL_H);
        this.refresh();
    }

    private void layoutSlices(int y)
    {
        this.widgets.removeAll(this.sliceButtons);
        this.sliceButtons.clear();
        int slices = this.editor.slices();
        this.lastSlices = slices;

        // В 3D видны все срезы сразу — кнопки срезов не нужны
        if (slices <= 1 || this.in3d())
        {
            return;
        }

        // Справа налево от кнопок плоскости карты: «Срез Y [1][2]…»
        int right = this.sliceRight;
        List<Button> made = new ArrayList<>();

        for (int i = slices - 1; i >= 0; --i)
        {
            final int s = i;
            Button b = new Button(String.valueOf(i + 1), Button.Style.SECONDARY, () -> this.editor.setSlice(s)).centered(true);
            int w = Math.max(18, b.preferredWidth(this.font));

            if (right - w < this.mapX + this.font.width(t("section_map")) + this.font.width(this.sliceLabel()) + 20)
            {
                break;
            }

            right -= w;
            b.setBounds(right, y, w, 14);
            right -= Ui.PAD_MICRO;
            made.add(0, b);
        }

        this.sliceLabelX = right - Ui.PAD_IN - this.font.width(this.sliceLabel());
        this.sliceButtons.addAll(made);
        this.widgets.addAll(made);
    }

    private int sliceLabelX;
    private int sliceRight;

    /** «Срез Y» / «Срез Z» / «Срез X» — по оси глубины плоскости карты. */
    private String sliceLabel()
    {
        return StringUtils.translate("zonefocus.gui.manager.slice_axis", CutTool.axisName(ZoneEditor.depthAxis()));
    }

    private void setMapView(int view)
    {
        if (ZoneEditor.mapView == view)
        {
            return;
        }

        ZoneEditor.mapView = view;
        this.editor.setSlice(0);
        this.cutTool.cancel();
        this.lastSlices = -1;
        this.layout();
    }

    private void onLayerCut(int value)
    {
        this.previewPoll = 0;
    }

    /** Верх видимой модели для среза 3D: блоки от низа; без среза — −1. */
    private int layerCutTop()
    {
        Slider s = this.layerCut;
        return s == null || this.layerCutMax < 0 || s.value() >= this.layerCutMax ? -1 : s.value();
    }

    // ===== Действия =====

    private boolean in3d()
    {
        return ZoneEditor.view3d && this.view3d != null && this.can3d();
    }

    /**
     * 3D-превью: с Material HUD API 6 — для любого участка с найденными схемами (модель снимается с мира схем);
     * со старым — только для участка из одной схемы (модель — из файла).
     */
    private boolean can3d()
    {
        if (MaterialHudCompat.hasSectionPreview())
        {
            return ! this.previewPlacements().isEmpty();
        }

        Section s = this.editor.section();

        if (s == null)
        {
            return this.editor.placement() != null;
        }

        return ZoneManager.getInstance().resolve(s).found().size() == 1;
    }

    /** Размещения модели превью: найденные схемы участка; черновик без участка — его размещение. */
    private List<SchematicPlacement> previewPlacements()
    {
        Section s = this.editor.section();

        if (s != null)
        {
            List<SchematicPlacement> found = ZoneManager.getInstance().resolve(s).found();

            if (! found.isEmpty())
            {
                return found;
            }
        }

        SchematicPlacement p = this.editor.placement();
        return p != null ? List.of(p) : List.of();
    }

    // ===== Участки =====

    /** Подтверждение вторым щелчком в течение {@link #CONFIRM_MS}; пока не подтверждено — подсказка в подвале. */
    private void confirm(String id, Runnable action)
    {
        long now = Util.getMillis();
        Long until = this.armed.get(id);

        if (until != null && now <= until)
        {
            this.armed.remove(id);
            this.statusText = "";
            action.run();
            return;
        }

        this.armed.put(id, now + CONFIRM_MS);
        this.statusText = sec("confirm", t("clearall".equals(id) ? "menu_clear_all" : "menu_delete_section"));
        this.statusUntil = now + CONFIRM_MS;
    }

    private boolean isArmed(String id)
    {
        Long until = this.armed.get(id);
        return until != null && Util.getMillis() <= until;
    }

    private void clearEverything()
    {
        this.editor.clearAll();
        this.afterSectionsChanged();
    }

    /** После удаления участков: редактор остаётся на живом участке, а если их не осталось — закрываем или черновик. */
    private void afterSectionsChanged()
    {
        ZoneManager manager = ZoneManager.getInstance();

        if (this.editor.section() == null)
        {
            Section active = manager.activeSection();

            if (active != null)
            {
                this.reload(active);
                return;
            }

            if (this.editor.placement() == null)
            {
                this.closeGui(true);
                return;
            }
        }

        this.seenActive = manager.activeSection();
        this.layout();
    }

    private void deleteSection(Section section)
    {
        ZoneManager manager = ZoneManager.getInstance();
        manager.removeSection(manager.getLayout().indexOf(section));
        this.afterSectionsChanged();
    }

    private void startRename(Section section)
    {
        this.renaming = section;
        this.renameField.setText(section.name.isEmpty() ? ZoneEditor.sectionTitle(section) : section.name);
        this.renameField.setVisible(true);
        this.renameField.setFocused(true);
        this.renameField.selectAll();
    }

    private void commitRename()
    {
        Section section = this.renaming;
        this.renaming = null;
        this.renameField.setFocused(false);
        this.renameField.setVisible(false);

        if (section != null)
        {
            String name = this.renameField.text().strip();

            if (! name.equals(section.name))
            {
                ZoneManager.getInstance().renameSection(section, name);
            }
        }
    }

    private void cancelRename()
    {
        this.renaming = null;
        this.renameField.setFocused(false);
        this.renameField.setVisible(false);
    }

    private void sectionMenu(Section section, int mouseX, int mouseY)
    {
        ZoneManager manager = ZoneManager.getInstance();
        ContextMenu.open(this.layers, mouseX, mouseY, 0, List.of(
                ContextMenu.Item.of(t("menu_rename"), () -> this.startRename(section)),
                ContextMenu.Item.of(section.hidden ? t("menu_section_show") : t("menu_section_hide"), () -> {
                    manager.setSectionHidden(section, ! section.hidden);
                    this.refresh();
                }),
                ContextMenu.Item.of(t("menu_resplit"), () -> GuiSectionSplit.open(this, section)),
                ContextMenu.Item.separator(),
                ContextMenu.Item.danger(t("menu_delete_section"),
                        () -> this.confirm("delete:" + section.id, () -> this.deleteSection(section))),
                ContextMenu.Item.danger(t("menu_clear_all"), () -> this.confirm("clearall", this::clearEverything))));
    }

    private static String schemCount(int n)
    {
        int mod100 = n % 100;
        int mod10 = n % 10;
        String form = n == 1 ? "one" : mod100 >= 11 && mod100 <= 14 ? "many" : mod10 == 1 ? "one"
                : mod10 >= 2 && mod10 <= 4 ? "few" : "many";
        return sec("schem_" + form, n);
    }

    private static String ruleText(Section s)
    {
        return switch (s.rule)
        {
            case Rule.Grid g -> sec("rule_grid", g.nx() + "×" + g.ny() + "×" + g.nz());
            case Rule.Cuts c -> sec("rule_cuts", c.nx() + "×" + c.ny() + "×" + c.nz());
            case Rule.SubRegions ignored -> sec("rule_sub");
            case Rule.Manual m -> sec("rule_manual", m.boxes().size());
        };
    }

    private void renderSectionRow(GuiContext ctx, Section s, int index, int x, int y, int w, int h, boolean selected,
            boolean hovered, int mouseX, int mouseY)
    {
        ZoneManager manager = ZoneManager.getInstance();
        PlacementMatcher.Resolved resolved = manager.resolve(s);
        boolean gone = resolved.isEmpty() && ! (s.rule instanceof Rule.Manual);
        int color = gone || s.hidden ? Ui.C_TEXT_3 : selected ? Ui.C_TEXT : Ui.C_TEXT_2;
        int right = x + w - Ui.PAD_REL;

        // Прогресс — справа, если участок уже сканировали
        ZoneScanner scanner = ZoneScanner.getInstance();

        if (scanner.hasStats(s))
        {
            long total = 0L;
            long missing = 0L;
            int zones = manager.zoneBoxes(s).size();

            for (int i = 0; i < zones; ++i)
            {
                ZoneScanner.Stats st = scanner.getStats(s, i);

                if (st != null)
                {
                    total += st.total();
                    missing += st.missing();
                }
            }

            if (total > 0L)
            {
                String pct = (int) ((total - missing) * 100L / total) + "%";
                Paint.midTextRight(ctx, this.font, pct, right, y, h, missing == 0L ? Ui.C_ON : Ui.C_GOLD, false);
                right -= this.font.width(pct) + Ui.PAD_GROUP;
            }
        }

        StringBuilder text = new StringBuilder("[").append(index + 1).append("] ").append(ZoneEditor.sectionTitle(s));
        text.append(" · ").append(schemCount(s.placements.size())).append(" · ").append(ruleText(s));

        if (resolved.isPartial())
        {
            text.append(" · ").append(sec("partial", s.placements.size() - resolved.found().size()));
        }

        if (s.hidden)
        {
            text.append(" · ").append(sec("hidden"));
        }

        Paint.midText(ctx, this.font, Paint.fit(this.font, text.toString(), Math.max(0, right - x - Ui.PAD_REL)),
                x + Ui.PAD_REL, y, h, color, false);
    }

    private boolean quality()
    {
        try
        {
            return MaterialHudBridge.previewQuality();
        }
        catch (LinkageError e)
        {
            return false;
        }
    }

    private void setQuality(boolean value)
    {
        try
        {
            MaterialHudBridge.setPreviewQuality(value);
        }
        catch (LinkageError ignored)
        {
        }

        this.refresh();
    }

    private void setView3d(boolean value)
    {
        ZoneEditor.view3d = value;
        this.previewPoll = 0;
        // Видеопамять: модель держит только видимое превью, второе соберётся заново, когда понадобится
        Object hidden = value ? this.topView : this.preview;

        if (hidden != null)
        {
            try
            {
                MaterialHudBridge.previewClose(hidden);
            }
            catch (LinkageError | RuntimeException ignored)
            {
            }
        }

        this.layout();
    }

    /** Вид камеры 3D-превью ({@link #VIEW_NAMES}); {@code null} — превью нет или Material HUD старее. */
    private @Nullable String cameraView()
    {
        if (this.preview == null)
        {
            return VIEW_NAMES[0];
        }

        try
        {
            return MaterialHudBridge.previewViewName(this.preview);
        }
        catch (LinkageError | RuntimeException e)
        {
            return null;
        }
    }

    private void setCameraView(String name)
    {
        if (this.preview == null)
        {
            return;
        }

        try
        {
            MaterialHudBridge.previewView(this.preview, name);
        }
        catch (LinkageError | RuntimeException ignored)
        {
        }

        this.refresh();
    }

    private void setMode(boolean bySize)
    {
        this.editor.setBySize(bySize);
        this.syncFields(true);
        this.refresh();
    }

    private void nudge(int axis, int delta)
    {
        this.editor.nudge(axis, delta);
        this.syncFields(true);
        this.refresh();
    }

    private void typed(int axis, String text)
    {
        if (! text.isEmpty())
        {
            try
            {
                this.editor.setValue(axis, Integer.parseInt(text));
            }
            catch (NumberFormatException ignored)
            {
            }
        }

        this.refresh();
    }

    private void apply()
    {
        this.syncFields(true);
        this.editor.apply();
        this.refresh();
    }

    private void undo()
    {
        long now = Util.getMillis();

        if (now > this.undoArmedUntil)
        {
            this.undoArmedUntil = now + CONFIRM_MS;
            return;
        }

        this.undoArmedUntil = 0L;
        this.editor.undo();
        this.afterSectionsChanged();
        this.refresh();
    }

    /** Поля не в фокусе показывают значение черновика (после «−/+», колеса, смены режима). */
    private void syncFields(boolean force)
    {
        for (int axis = 0; axis < 3; ++axis)
        {
            TextInput f = this.fields[axis];

            if (f != null && (force || ! f.focused()))
            {
                String v = String.valueOf(this.editor.value(axis));

                if (! v.equals(f.text()))
                {
                    f.setText(v);
                }
            }
        }
    }

    private void refresh()
    {
        if (this.editor.followRule())
        {
            this.syncFields(true);
        }

        boolean bySize = this.editor.bySize();
        this.modes[0].selected(! bySize);
        this.modes[1].selected(bySize);

        if (this.view2d != null && this.view3d != null)
        {
            this.view2d.selected(! this.in3d());
            this.view3d.selected(this.in3d());
        }

        if (this.frames != null)
        {
            this.frames.setVisible(this.in3d());
            this.frames.selected(ZoneEditor.showFrames);
        }

        String camera = this.cameraView();

        for (int i = 0; i < this.views.length; ++i)
        {
            if (this.views[i] != null)
            {
                this.views[i].setVisible(this.in3d() && camera != null);
                this.views[i].selected(VIEW_NAMES[i].equals(camera));
            }
        }

        for (int i = 0; i < this.mapViews.length; ++i)
        {
            this.mapViews[i].setVisible(! this.in3d());
            this.mapViews[i].selected(ZoneEditor.mapView == i);
        }

        if (this.layerCut != null)
        {
            this.layerCut.setVisible(this.in3d() && this.layerCutMax > 0);
        }

        if (this.qualityHi != null && this.qualityLo != null)
        {
            boolean quality = this.quality();
            this.qualityHi.setVisible(this.in3d());
            this.qualityLo.setVisible(this.in3d());
            this.qualityHi.selected(quality);
            this.qualityLo.selected(! quality);
        }
        boolean applied = this.editor.applied();
        this.split.label(applied ? t("split_done") : StringUtils.translate("zonefocus.gui.manager.split", this.editor.total()));
        this.split.setEnabled(! applied && this.editor.bounds() != null);
        this.undo.setEnabled(this.editor.section() != null);
        this.clearAll.label(this.isArmed("clearall") ? t("clear_all_confirm") : t("clear_all"));
        this.clearAll.setVisible(! ZoneManager.getInstance().sections().isEmpty());
        List<Section> all = ZoneManager.getInstance().sections();
        this.sectionList.setItems(new ArrayList<>(all));
        this.sectionList.setSelectedIndex(all.indexOf(this.editor.section()));
        this.undo.label(Util.getMillis() <= this.undoArmedUntil ? t("undo_confirm") : t("undo"));
        this.scan.setVisible(ZoneEditor.showProgress && this.editor.hasZones());
        this.scan.setEnabled(! this.editor.scanning());
        int sortRight = this.scan.visible() ? this.scan.x() - Ui.PAD_IN : this.mapX + this.mapW;
        this.sort.setBounds(sortRight - this.sort.w(), this.sort.y(), this.sort.w(), 14);
        this.sort.setVisible(this.editor.hasZones());
        int hidden = this.editor.hiddenCount();
        this.showHidden.setVisible(hidden > 0);
        this.showHidden.label(StringUtils.translate("zonefocus.gui.show_all_zones", hidden));
        int hw = this.showHidden.preferredWidth(this.font);
        this.showHidden.setBounds(this.sort.x() - Ui.PAD_IN - hw, this.sort.y(), hw, 14);

        for (int i = 0; i < this.sliceButtons.size(); ++i)
        {
            this.sliceButtons.get(i).selected(i == this.editor.slice());
        }

        List<ZoneEditor.Row> rows = this.editor.rows();
        this.list.setItems(rows);

        for (int i = 0; i < rows.size(); ++i)
        {
            if (rows.get(i).index() == this.editor.active())
            {
                this.list.setSelectedIndex(i);
                break;
            }
        }
    }

    // ===== Отрисовка =====

    @Override
    protected void drawScreenBackground(GuiContext ctx, int mouseX, int mouseY)
    {
        Paint.rect(ctx, 0, 0, this.width, this.height, Ui.C_DIM);
        Paint.card(ctx, this.px, this.py, this.pw, this.ph, Ui.R_CTRL, Ui.C_PANEL, Ui.C_PANEL_EDGE);

        if (Style.vanilla())
        {
            // Ванильный стиль: шапку не заливаем, под ней разделитель Minecraft
            Vanilla.headerSeparator(ctx, this.px, this.py + Ui.HEADER_H, this.pw);
        }
        else
        {
            Paint.rect(ctx, this.px + 1, this.py + 1, this.pw - 2, Ui.HEADER_H - 1, Ui.C_CHROME);
            Paint.rect(ctx, this.px, this.py + Ui.HEADER_H, this.pw, 1, Ui.C_DIVIDER);
        }
    }

    @Override
    protected void drawTitle(GuiContext ctx, int mouseX, int mouseY, float partialTicks)
    {
        int x = this.px + Ui.PAD_SECTION;
        Paint.texture(ctx, ICON, x, this.py + (Ui.HEADER_H - 16) / 2, 16);
        x += 16 + Ui.PAD_REL;
        String title = t("title");
        Paint.midText(ctx, this.font, title, x, this.py, Ui.HEADER_H, Ui.C_TEXT, false);
        x += this.font.width(title) + Ui.PAD_REL;
        ZoneBox b = this.editor.bounds();
        String sub = "· " + this.editor.title()
                + (b == null ? "" : "  " + b.sizeX() + "×" + b.sizeY() + "×" + b.sizeZ());
        int right = this.qualityHi != null && this.qualityHi.visible() ? this.qualityHi.x()
                : this.view2d != null ? this.view2d.x() : this.progress.x();
        int room = right - Ui.PAD_GROUP - x;
        Paint.midText(ctx, this.font, Paint.fit(this.font, sub, Math.max(0, room)), x, this.py, Ui.HEADER_H, Ui.C_TEXT_3, false);
    }

    @Override
    protected void drawContents(GuiContext ctx, int mouseX, int mouseY, float partialTicks)
    {
        Tooltip.beginFrame();
        this.syncFields(false);

        // Активный участок сменился снаружи (окно «Новый участок», удаление) — редактор следом
        ZoneManager manager = ZoneManager.getInstance();
        Section active = manager.activeSection();

        if (active != this.seenActive)
        {
            this.seenActive = active;

            if (active != null && active != this.editor.section())
            {
                this.reload(active);
            }
        }

        if (this.editor.section() == null && this.editor.placement() == null)
        {
            if (active != null)
            {
                this.reload(active);
            }
            else
            {
                this.closeGui(true);
                return;
            }
        }

        if (this.editor.slices() != this.lastSlices || manager.sections().size() != this.lastSectionCount)
        {
            this.layout();
        }

        this.refresh();

        int x = this.px + Ui.PAD_SECTION;
        int sections = manager.sections().size();

        if (sections > 1)
        {
            section(ctx, StringUtils.translate("zonefocus.gui.sections.label", sections), x, this.top,
                    this.addSection.x() - Ui.PAD_REL - x);
        }
        else
        {
            String head = StringUtils.translate("zonefocus.gui.sections.header", this.editor.title());
            Paint.midText(ctx, this.font, Paint.fit(this.font, head, Math.max(0, this.addSection.x() - Ui.PAD_REL - x)),
                    x, this.top, CTRL_H, Ui.C_TEXT_2, false);
        }

        section(ctx, t("section_mode"), x, this.leftTop, this.leftW);
        section(ctx, t("section_grid"), x, this.rowsY - LABEL_H, this.leftW);

        String[] axes = {"X", "Y", "Z"};

        for (int axis = 0; axis < 3; ++axis)
        {
            int ry = this.rowsY + axis * AXIS_STEP;
            boolean over = Paint.hit(mouseX, mouseY, x, ry, this.leftW, Ui.ROW_H);

            if (over)
            {
                Paint.round(ctx, x - Ui.PAD_IN, ry, this.leftW + Ui.PAD_IN * 2, Ui.ROW_H, Ui.R_ROW, Ui.C_ROW_HOVER);
            }

            Paint.midText(ctx, this.font, axes[axis], x, ry, Ui.ROW_H, Ui.C_ACCENT, false);
            String hint = StringUtils.translate(this.editor.bySize() ? "zonefocus.gui.axis_size_hint"
                    : "zonefocus.gui.axis_count_hint", axes[axis]);
            Paint.midText(ctx, this.font, hint, x + 12, ry, Ui.ROW_H, Ui.C_TEXT_2, false);
        }

        // Итог черновика — чипами
        int[] size = this.editor.zoneSize();
        int cx = chip(ctx, StringUtils.translate("zonefocus.gui.manager.chip_zones", this.editor.total()), x, this.chipsY, Ui.C_ON);
        cx = chip(ctx, size[0] + "×" + size[1] + "×" + size[2], cx + Ui.PAD_IN, this.chipsY, Ui.C_ACCENT);

        String volume = StringUtils.translate("zonefocus.gui.manager.chip_volume", group(this.editor.zoneVolume()));

        if (cx + Ui.PAD_IN + this.font.width(volume) + Ui.PAD_REL * 2 <= x + this.leftW)
        {
            chip(ctx, volume, cx + Ui.PAD_IN, this.chipsY, Ui.C_TEXT_2);
        }

        // Справа: карта
        int labelRight = ! this.sliceButtons.isEmpty() ? this.sliceLabelX - Ui.PAD_REL - this.mapX
                : this.views[0] != null && this.views[0].visible() ? this.views[0].x() - Ui.PAD_REL - this.mapX
                : this.mapViews[0].visible() ? this.mapViews[0].x() - Ui.PAD_REL - this.mapX : this.mapW;
        int built = this.in3d() && this.preview != null && ZoneEditor.showProgress
                ? MaterialHudBridge.previewBuiltPercent(this.preview) : -1;
        section(ctx, built >= 0 ? StringUtils.translate("zonefocus.gui.manager.section_3d_built", built)
                : t(this.in3d() ? "section_3d" : ZoneEditor.mapView == 1 ? "section_map_front" : ZoneEditor.mapView == 2 ? "section_map_side" : "section_map"), this.mapX, this.top, labelRight);

        if (! this.sliceButtons.isEmpty())
        {
            Paint.text(ctx, this.font, this.sliceLabel(), this.sliceLabelX, this.top, Ui.C_TEXT_3, false);
        }

        Paint.card(ctx, this.mapX, this.mapY, this.mapW, this.mapH, Ui.R_CTRL, PALETTE.panel(), PALETTE.edge());
        int hovered;
        this.cutTool.frame();

        if (this.in3d())
        {
            hovered = this.render3d(ctx, mouseX, mouseY);
        }
        else
        {
            hovered = ZoneMap.render(ctx, this.font, this.editor, PALETTE, this.mapX, this.mapY, this.mapW, this.mapH,
                    mouseX, mouseY, this.renderTopView(ctx));

            if (this.editor.cuttable())
            {
                this.cutTool.render2d(ctx, this.editor, mouseX, mouseY);
            }
        }

        if (this.cutTool.hovered() || this.cutTool.dragging())
        {
            // Над линией разреза — не зона, а разрез: подсказка про него
            hovered = -1;
            String tip = this.cutTool.tip(this.editor);

            if (tip != null)
            {
                Tooltip.offer(this.cutTool, tip);
            }
        }

        if (this.layerCut != null && this.layerCut.visible())
        {
            // Подпись среза слева от ползунка, на тёмной подложке поверх модели
            String label = this.layerCutTop() < 0 ? t("layer_cut_off")
                    : StringUtils.translate("zonefocus.gui.manager.layer_cut", this.layerCut.value() + 1, this.layerCutMax + 1);
            int lw = this.font.width(label);
            int lx = this.layerCut.x() - Ui.PAD_IN - lw;
            Paint.round(ctx, lx - Ui.PAD_REL, this.layerCut.y() - 2, this.layerCut.x() + this.layerCut.w() - lx + Ui.PAD_REL * 2,
                    16, Ui.R_CTRL, 0xC0101014);
            Paint.midText(ctx, this.font, label, lx, this.layerCut.y(), 12, Ui.C_TEXT_2, false);
        }

        if (ZoneEditor.view3d && this.view3d != null && ! this.can3d())
        {
            String note = Paint.fit(this.font, t(MaterialHudCompat.hasSectionPreview() ? "no3d_missing" : "no3d_group"), this.mapW - Ui.PAD_SECTION * 2);
            int nw = this.font.width(note) + Ui.PAD_REL * 2;
            Paint.card(ctx, this.mapX + Ui.PAD_IN, this.mapY + Ui.PAD_IN, nw, 14, Ui.R_CTRL, 0xE0101014,
                    Ui.alpha(Ui.C_GOLD, 0.5F));
            Paint.midText(ctx, this.font, note, this.mapX + Ui.PAD_IN + Ui.PAD_REL, this.mapY + Ui.PAD_IN, 14, Ui.C_GOLD, false);
        }

        if (! this.editor.live())
        {
            String draft = Paint.fit(this.font, t("draft"), this.mapW - Ui.PAD_SECTION * 2);
            int tw = this.font.width(draft) + Ui.PAD_REL * 2;
            int bx = this.mapX + (this.mapW - tw) / 2;
            // Над ползунком среза, если он есть, — не поверх него
            int by = this.mapY + this.mapH - 18 - (this.layerCut != null && this.layerCut.visible() ? 18 : 0);
            Paint.card(ctx, bx, by, tw, 14, Ui.R_CTRL, 0xE0101014, Ui.alpha(Ui.C_ACCENT, 0.5F));
            Paint.midText(ctx, this.font, draft, bx + Ui.PAD_REL, by, 14, Ui.C_ACCENT, false);
        }

        if (hovered >= 0)
        {
            Tooltip.offer(this.editor, StringUtils.translate("zonefocus.gui.manager.cell_tip", hovered + 1));
        }

        // Наведённая клетка или строка подсвечивается и в мире
        int rowHover = this.list.contains(mouseX, mouseY) ? this.list.indexAt(mouseX, mouseY) : -1;
        int preview = hovered >= 0 ? hovered
                : rowHover >= 0 && rowHover < this.list.items().size() ? this.list.items().get(rowHover).index() : -1;
        ZoneRenderer.setPreviewIndex(preview);

        int headLeft = this.showHidden.visible() ? this.showHidden.x() : this.sort.visible() ? this.sort.x()
                : this.scan.visible() ? this.scan.x() : this.mapX + this.mapW;
        section(ctx, StringUtils.translate("zonefocus.gui.manager.section_zones", this.list.items().size()),
                this.mapX, this.listLabelY, headLeft - Ui.PAD_REL - this.mapX);

        for (Widget w : this.widgets)
        {
            w.render(ctx, mouseX, mouseY);
        }

        // Подвал: подтверждение или подсказка, если влезает
        int hintX = this.clearAll.visible() ? this.clearAll.x() + this.clearAll.w() + Ui.PAD_GROUP : x;
        int room = this.done.x() - Ui.PAD_GROUP - hintX;
        boolean status = Util.getMillis() <= this.statusUntil && ! this.statusText.isEmpty();

        if (room > 80)
        {
            String text = status ? this.statusText : t(this.in3d() ? "hint3d" : "hint");
            Paint.midText(ctx, this.font, Paint.fit(this.font, text, room), hintX, this.done.y(),
                    CTRL_H, status ? Ui.C_GOLD : Ui.C_TEXT_3, false);
        }

        this.layers.render(ctx, mouseX, mouseY);
        Tooltip.render(ctx, this.font, mouseX, mouseY, this.width, this.height);
    }

    /** 3D-превью в рамке карты; зона под курсором или −1. */
    private int render3d(GuiContext ctx, int mouseX, int mouseY)
    {
        if (this.preview == null)
        {
            this.preview = MaterialHudBridge.createPreview();

            if (this.preview == null)
            {
                ZoneEditor.view3d = false;
                return -1;
            }
        }

        List<SchematicPlacement> placements = this.previewPlacements();

        if (placements.isEmpty())
        {
            return -1;
        }

        SchematicPlacement placement = placements.getFirst();
        boolean section = MaterialHudCompat.hasSectionPreview();

        try
        {
            if (section)
            {
                // Участок целиком — снимок с мира схем: все его схемы, с поворотами; если стоит Voxy — мир под ними
                MaterialHudBridge.previewPlacements(this.preview, placements);
                this.syncLayerCut(this.preview);
                MaterialHudBridge.previewLayerCut(this.preview, this.layerCutTop());
            }
            else
            {
                MaterialHudBridge.previewFile(this.preview, placement.getSchematicFile());
            }

            // «Показывать прогресс» — и по блокам: построенное как есть, остальное «призраком»
            MaterialHudBridge.previewBuild(this.preview, ZoneEditor.showProgress ? placement : null);

            if (--this.previewPoll <= 0)
            {
                this.previewPoll = 5;
                this.feedZones();
            }

            int x = this.mapX + 1;
            int y = this.mapY + 1;
            int w = this.mapW - 2;
            int h = this.mapH - 2;
            MaterialHudBridge.previewRender(this.preview, ctx, x, y, w, h, mouseX, mouseY);
            net.minecraft.core.BlockPos origin = section ? MaterialHudBridge.previewWorldMin(this.preview) : null;

            if (origin != null && this.editor.cuttable())
            {
                this.cutTool.render3d(ctx, this.editor, this.preview, origin, mouseX, mouseY, x, y, w, h);
            }

            return MaterialHudBridge.previewHovered(this.preview);
        }
        catch (LinkageError | RuntimeException e)
        {
            // Material HUD сломан или старее — назад к карте сверху
            this.preview = null;
            ZoneEditor.view3d = false;
            return -1;
        }
    }

    /** Ползунок среза — по высоте модели (появилась или сменилась — диапазон заново, без среза). */
    private void syncLayerCut(Object handle)
    {
        int[] size = MaterialHudBridge.previewSize(handle);
        int max = size != null ? size[1] - 1 : -1;

        if (this.layerCut != null && max != this.layerCutMax)
        {
            this.layerCutMax = max;
            this.layerCut.setRange(0, Math.max(0, max), Math.max(0, max));
            this.refresh();
        }
    }

    /**
     * 2D: снимок схемы в прямоугольник её габарита на карте — клетки зон ложатся на постройку. С Material HUD API 6 —
     * участок целиком и плоскость карты (сверху, спереди, сбоку); со старым — одна схема и только сверху.
     * Ложь — снимка нет, карта как раньше.
     */
    private boolean renderTopView(GuiContext ctx)
    {
        if (MaterialHudCompat.hasSectionPreview())
        {
            return this.renderSectionMap(ctx);
        }

        SchematicPlacement placement = this.editor.placement();

        if (ZoneEditor.mapView != 0 || this.topViewBroken || ! MaterialHudCompat.hasPreview() || placement == null || ! this.can3d())
        {
            return false;
        }

        int[] rect = ZoneMap.layout(this.editor, this.mapX, this.mapY, this.mapW, this.mapH);

        if (rect[2] < 8 || rect[3] < 8)
        {
            return false;
        }

        if (this.topView == null)
        {
            this.topView = MaterialHudBridge.createPreview();

            if (this.topView == null)
            {
                this.topViewBroken = true;
                return false;
            }
        }

        try
        {
            MaterialHudBridge.previewFile(this.topView, placement.getSchematicFile());
            MaterialHudBridge.previewMap(this.topView, placement);
            MaterialHudBridge.previewMapCut(this.topView, this.sliceTop());
            MaterialHudBridge.previewRender(this.topView, ctx, rect[0], rect[1], rect[2], rect[3], -1, -1);
            return true;
        }
        catch (LinkageError | RuntimeException e)
        {
            // Material HUD без карты сверху — карта как раньше, без снимка
            Object broken = this.topView;
            this.topView = null;
            this.topViewBroken = true;

            try
            {
                MaterialHudBridge.previewClose(broken);
            }
            catch (LinkageError | RuntimeException ignored)
            {
            }

            return false;
        }
    }

    /** Карта по участку (API 6): снимок с мира схем в плоскости карты; сверху — срезан по текущему срезу Y. */
    private boolean renderSectionMap(GuiContext ctx)
    {
        List<SchematicPlacement> placements = this.previewPlacements();

        if (this.topViewBroken || placements.isEmpty())
        {
            return false;
        }

        ZoneMap.layout(this.editor, this.mapX, this.mapY, this.mapW, this.mapH);

        if (this.topView == null)
        {
            this.topView = MaterialHudBridge.createPreview();

            if (this.topView == null)
            {
                this.topViewBroken = true;
                return false;
            }
        }

        try
        {
            MaterialHudBridge.previewPlacements(this.topView, placements);
            MaterialHudBridge.previewMapView(this.topView, MAP_VIEW_NAMES[ZoneEditor.mapView]);
            net.minecraft.core.BlockPos origin = MaterialHudBridge.previewWorldMin(this.topView);
            int[] size = MaterialHudBridge.previewSize(this.topView);

            if (origin == null || size == null)
            {
                return false;
            }

            int top = ZoneEditor.mapView == 0 ? this.sliceTopWorld() : Integer.MIN_VALUE;
            MaterialHudBridge.previewLayerCut(this.topView, top == Integer.MIN_VALUE ? -1 : top - origin.getY());
            ZoneBox model = new ZoneBox(origin.getX(), origin.getY(), origin.getZ(),
                    origin.getX() + size[0] - 1, origin.getY() + size[1] - 1, origin.getZ() + size[2] - 1);
            int[] rect = this.editor.rectOf(model);

            if (rect[2] < 8 || rect[3] < 8)
            {
                return false;
            }

            MaterialHudBridge.previewRender(this.topView, ctx, rect[0], rect[1], rect[2], rect[3], -1, -1);
            return true;
        }
        catch (LinkageError | RuntimeException e)
        {
            Object broken = this.topView;
            this.topView = null;
            this.topViewBroken = true;

            try
            {
                MaterialHudBridge.previewClose(broken);
            }
            catch (LinkageError | RuntimeException ignored)
            {
            }

            return false;
        }
    }

    /** Верх текущего среза Y карты сверху в мире; срез один — {@link Integer#MIN_VALUE}. */
    private int sliceTopWorld()
    {
        if (this.editor.slices() <= 1)
        {
            return Integer.MIN_VALUE;
        }

        List<ZoneBox> boxes = this.editor.applied() ? this.editor.zoneBoxes() : this.editor.draftBoxes();
        int top = Integer.MIN_VALUE;

        for (int i = 0; i < boxes.size(); ++i)
        {
            if (this.editor.sliceOf(i) == this.editor.slice())
            {
                top = Math.max(top, boxes.get(i).maxY());
            }
        }

        return top;
    }

    /**
     * Верх текущего среза Y в координатах схемы: снимок срезает блоки выше, и видно, что внутри среза.
     * Срез один (или зоны — подрегионы) — без среза, −1.
     */
    private int sliceTop()
    {
        ZoneBox b = this.editor.bounds();
        SchematicPlacement placement = this.editor.placement();

        if (b == null || placement == null || this.editor.slices() <= 1)
        {
            return -1;
        }

        int[][] ys = ZoneManager.slices(b.minY(), b.maxY(), this.editor.ny());
        int s = Math.max(0, Math.min(this.editor.slice(), ys.length - 1));
        int[][] local = FileZones.toLocal(placement,
                List.of(new ZoneBox(b.minX(), ys[s][0], b.minZ(), b.maxX(), ys[s][1], b.maxZ())));

        return local[0][4];
    }

    /**
     * Зоны в превью: применённые — из Zone Focus, иначе черновик по числам слева; в координатах схемы
     * (обратный поворот размещения), номера — как в мире. «Прогресс» красит каркасы по скану.
     */
    private void feedZones()
    {
        if (this.preview == null)
        {
            return;
        }

        // Каркасы убраны — на превью только постройка
        if (! ZoneEditor.showFrames)
        {
            MaterialHudBridge.previewZones(this.preview, null, -1, null, false);
            return;
        }

        SchematicPlacement placement = this.editor.placement();

        if (placement == null)
        {
            return;
        }

        boolean draft = ! this.editor.live();
        List<ZoneBox> world = draft ? this.editor.draftBoxes() : this.editor.zoneBoxes();
        int[][] local;

        if (MaterialHudCompat.hasSectionPreview())
        {
            // Модель — с мира схем: зоны в блоках мира от её угла
            net.minecraft.core.BlockPos origin = MaterialHudBridge.previewWorldMin(this.preview);

            if (origin == null)
            {
                return;
            }

            local = new int[world.size()][];

            for (int i = 0; i < local.length; ++i)
            {
                ZoneBox b = world.get(i);
                local[i] = new int[] { b.minX() - origin.getX(), b.minY() - origin.getY(), b.minZ() - origin.getZ(),
                        b.maxX() - origin.getX(), b.maxY() - origin.getY(), b.maxZ() - origin.getZ() };
            }
        }
        else
        {
            local = FileZones.toLocal(placement, world);
        }

        if (! draft && ZoneEditor.showProgress)
        {
            // Прогресс включён, а скана не было — запускаем сам, кнопку жать не обязательно
            if (this.editor.stats(0) == null && ! this.editor.scanning())
            {
                this.editor.scan();
            }

            // Прогресс: каркас по готовности и процент у номера (−1 — зона ещё не посчитана)
            int[] percents = new int[world.size()];

            for (int i = 0; i < percents.length; ++i)
            {
                ZoneScanner.Stats s = this.editor.stats(i);
                percents[i] = s == null ? -1 : s.percent();
            }

            MaterialHudBridge.previewProgress(this.preview, local, this.editor.active(), percents);
            return;
        }

        MaterialHudBridge.previewZones(this.preview, local, draft ? -1 : this.editor.active(), null, draft);
    }

    private boolean inMap(double x, double y)
    {
        return x >= this.mapX && y >= this.mapY && x < this.mapX + this.mapW && y < this.mapY + this.mapH;
    }

    // ===== Разрезы мышью =====

    /** ПКМ по 2D-карте: блок под курсором по двум осям плоскости карты. */
    private void cutMenu2d(double mouseX, double mouseY)
    {
        int[] map = this.editor.mapRect();

        if (map[2] <= 0 || mouseX < map[0] || mouseY < map[1] || mouseX >= map[0] + map[2] || mouseY >= map[1] + map[3])
        {
            return;
        }

        int[] block = new int[3];
        boolean[] axes = new boolean[3];
        block[ZoneEditor.hAxis()] = this.editor.screenToBlock(ZoneEditor.hAxis(), mouseX);
        block[ZoneEditor.vAxis()] = this.editor.screenToBlock(ZoneEditor.vAxis(), mouseY);
        axes[ZoneEditor.hAxis()] = true;
        axes[ZoneEditor.vAxis()] = true;
        this.cutMenu(mouseX, mouseY, block, axes);
    }

    /**
     * Меню «Разрезать здесь» по блоку мира {@code block} для отмеченных осей: новый разрез ложится на ближнюю к
     * началу оси грань блока — блок открывает новую зону. Наведённый разрез можно убрать.
     */
    private void cutMenu(double mouseX, double mouseY, int[] block, boolean[] axes)
    {
        List<ContextMenu.Item> items = new ArrayList<>();
        ZoneBox box = this.editor.cutBox();
        int[][] cuts = this.editor.cuts();

        if (box == null || cuts == null)
        {
            String why = this.editor.section() == null || ! this.editor.applied() ? t("cut_need_split") : t("cut_need_single");
            items.add(ContextMenu.Item.disabled(t("cut_here"), why));
        }
        else
        {
            for (int axis = 0; axis < 3; ++axis)
            {
                if (! axes[axis])
                {
                    continue;
                }

                final int a = axis;
                final int offset = block[axis] - ZoneEditor.min(box, axis);
                String label = StringUtils.translate("zonefocus.gui.cut.add", CutTool.axisName(axis), block[axis]);
                boolean exists = java.util.Arrays.stream(cuts[axis]).anyMatch(c -> c == offset);

                if (offset <= 0 || offset >= ZoneEditor.size(box, axis) || exists)
                {
                    items.add(ContextMenu.Item.disabled(label, t("cut_edge")));
                }
                else
                {
                    items.add(ContextMenu.Item.of(label, () -> {
                        this.editor.addCut(a, offset);
                        this.syncFields(true);
                        this.previewPoll = 0;
                        this.layout();
                    }));
                }
            }

            if (this.cutTool.hovered())
            {
                final int a = this.cutTool.hoverAxis();
                final int index = this.cutTool.hoverIndex();

                if (index < cuts[a].length)
                {
                    items.add(ContextMenu.Item.separator());
                    items.add(ContextMenu.Item.danger(StringUtils.translate("zonefocus.gui.cut.remove", CutTool.axisName(a),
                            ZoneEditor.min(box, a) + cuts[a][index]), () -> {
                        this.editor.removeCut(a, index);
                        this.cutTool.cancel();
                        this.syncFields(true);
                        this.previewPoll = 0;
                        this.layout();
                    }));
                }
            }
        }

        ContextMenu.open(this.layers, (int) mouseX, (int) mouseY, 120, items);
    }

    private void section(GuiContext ctx, String label, int x, int y, int w)
    {
        Paint.text(ctx, this.font, label, x, y, Ui.C_TEXT_3, false);
        int lx = x + this.font.width(label) + Ui.PAD_REL;
        Paint.rect(ctx, lx, y + 4, Math.max(0, x + w - lx), 1, Ui.C_DIVIDER);
    }

    private int chip(GuiContext ctx, String text, int x, int y, int color)
    {
        int w = this.font.width(text) + Ui.PAD_REL * 2;
        Paint.card(ctx, x, y, w, 14, Ui.R_CTRL, Ui.alpha(color, 0.14F), Ui.alpha(color, 0.5F));
        Paint.midText(ctx, this.font, text, x + Ui.PAD_REL, y, 14, color, false);
        return x + w;
    }

    private static String group(long value)
    {
        return value >= 10_000 ? (value / 1000) + "K" : String.valueOf(value);
    }

    private void renderRow(GuiContext ctx, ZoneEditor.Row row, int index, int x, int y, int w, int h, boolean selected,
            boolean hovered, int mouseX, int mouseY)
    {
        boolean active = row.index() == this.editor.active();
        String num = String.valueOf(row.index() + 1);
        int badgeW = Math.max(18, this.font.width(num) + 8);
        Paint.card(ctx, x + Ui.PAD_IN, y + 2, badgeW, h - 4, Ui.R_CTRL,
                active ? Ui.alpha(0xFF50D0FF, 0.25F) : Ui.C_ROW_HOVER, active ? 0xFF50D0FF : 0);
        Paint.midText(ctx, this.font, num, x + Ui.PAD_IN + (badgeW - this.font.width(num)) / 2, y, h,
                active ? 0xFF50D0FF : Ui.C_TEXT_2, false);

        ZoneBox b = row.box();
        int tx = x + Ui.PAD_IN + badgeW + Ui.PAD_REL;
        boolean hiddenRow = this.editor.isHidden(row.index()) || row.missing();
        String size = b.sizeX() + "×" + b.sizeY() + "×" + b.sizeZ() + (hiddenRow ? "  " + t("hidden") : "");
        Paint.midText(ctx, this.font, size, tx, y, h, hiddenRow ? Ui.C_TEXT_3 : active ? Ui.C_TEXT : Ui.C_TEXT_2, false);

        int right = x + w - Ui.PAD_REL;

        if (row.distance() >= 0)
        {
            String d = row.distance() + Strings.plain("zonefocus.gui.meters");
            Paint.midTextRight(ctx, this.font, d, right, y, h, Ui.C_TEXT_3, false);
            right -= this.font.width(d) + Ui.PAD_GROUP;
        }

        if (ZoneEditor.showProgress)
        {
            ZoneScanner.Stats s = row.stats();
            String pct = s == null ? Strings.plain("zonefocus.gui.zone_unknown") : s.percent() + "%";
            int tone = s == null ? Ui.C_TEXT_3 : s.missing() == 0 ? Ui.C_ON : s.missing() < s.total() ? Ui.C_GOLD : Ui.C_DANGER;
            Paint.midTextRight(ctx, this.font, pct, right, y, h, tone, false);
            right -= this.font.width(pct) + Ui.PAD_REL;
            int barW = Math.min(80, Math.max(30, right - tx - 90));
            int barX = right - barW;
            Paint.round(ctx, barX, y + h / 2 - 2, barW, 4, 2, Ui.C_TRACK);

            if (s != null && s.total() > 0)
            {
                int fill = (int) (barW * (s.total() - s.missing()) / (double) s.total());
                Paint.round(ctx, barX, y + h / 2 - 2, Math.max(2, fill), 4, 2, tone);
            }
        }
    }

    private void rowMenu(ZoneEditor.Row row, int mouseX, int mouseY)
    {
        boolean hidden = this.editor.isHidden(row.index());
        List<ContextMenu.Item> items = new ArrayList<>(List.of(
                ContextMenu.Item.of(t("menu_build"), () -> {
                    this.editor.select(row.index());
                    this.refresh();
                }),
                ContextMenu.Item.of(hidden ? t("menu_show") : t("menu_hide"), () -> {
                    this.editor.toggleHidden(row.index());
                    this.refresh();
                })));

        if (this.editor.manual())
        {
            items.add(ContextMenu.Item.danger(t("menu_delete_zone"), () -> {
                this.editor.removeZone(row.index());
                this.refresh();
            }));
        }

        ContextMenu.open(this.layers, mouseX, mouseY, 0, items);
    }

    // ===== Ввод =====

    @Override
    public boolean onMouseClicked(MouseButtonEvent click, boolean doubleClick)
    {
        if (this.layers.mouseClicked(click, doubleClick))
        {
            return true;
        }

        if (this.renaming != null && ! this.renameField.contains(click.x(), click.y()))
        {
            this.commitRename();
        }

        for (TextInput f : this.fields)
        {
            if (f.focused() && ! f.contains(click.x(), click.y()))
            {
                f.setFocused(false);
                this.syncFields(true);
            }
        }

        for (Widget w : this.widgets)
        {
            if (w.visible() && w.mouseClicked(click, doubleClick))
            {
                return true;
            }
        }

        // Разрез под курсором — тянем его, а не крутим модель и не выбираем зону
        if (Kit.left(click) && this.inMap(click.x(), click.y())
                && (this.in3d() ? this.cutTool.press3d(this.editor, click.x(), click.y()) : this.cutTool.press2d()))
        {
            return true;
        }

        if (this.in3d() && this.preview != null && this.inMap(click.x(), click.y()))
        {
            this.pressX = click.x();
            this.pressY = click.y();

            if (MaterialHudBridge.previewClicked(this.preview, click.x(), click.y(), click.button()))
            {
                this.dragging3d = true;
            }

            return true;
        }

        if (Kit.right(click) && ! this.in3d() && this.inMap(click.x(), click.y()))
        {
            this.cutMenu2d(click.x(), click.y());
            return true;
        }

        if (Kit.left(click) && ! this.in3d())
        {
            int zone = ZoneMap.hit(this.editor, this.mapX, this.mapY, this.mapW, this.mapH, click.x(), click.y());

            if (zone >= 0)
            {
                Vanilla.click();
                this.editor.select(zone);
                this.refresh();
                return true;
            }
        }

        return super.onMouseClicked(click, doubleClick);
    }

    @Override
    public boolean onMouseReleased(MouseButtonEvent click)
    {
        if (this.cutTool.dragging())
        {
            if (this.cutTool.release(this.editor))
            {
                this.previewPoll = 0;
                this.refresh();
            }

            return true;
        }

        if (this.dragging3d && this.preview != null)
        {
            this.dragging3d = false;
            MaterialHudBridge.previewReleased(this.preview, click.x(), click.y(), click.button());
            int zone = MaterialHudBridge.previewClickedZone(this.preview);

            // Щелчок без протяжки по каркасу — активная зона, как клик по клетке карты
            if (zone >= 0 && this.editor.live())
            {
                Vanilla.click();
                this.editor.select(zone);
                this.previewPoll = 0;
                this.refresh();
            }

            // Правый щелчок без протяжки — меню разреза по блоку под курсором (над ручкой — и «убрать разрез»)
            boolean rightClick = Kit.right(click) && Math.abs(click.x() - this.pressX) + Math.abs(click.y() - this.pressY) <= 3.0;

            if (MaterialHudCompat.hasSectionPreview() && rightClick)
            {
                int[] block = MaterialHudBridge.previewRightClicked(this.preview);
                net.minecraft.core.BlockPos origin = MaterialHudBridge.previewWorldMin(this.preview);

                if (block != null && origin != null)
                {
                    this.cutMenu(click.x(), click.y(), new int[] { origin.getX() + block[0], origin.getY() + block[1],
                            origin.getZ() + block[2] }, new boolean[] { true, true, true });
                }
                else if (this.cutTool.hovered())
                {
                    this.cutMenu(click.x(), click.y(), new int[3], new boolean[3]);
                }
            }

            return true;
        }

        for (Widget w : this.widgets)
        {
            if (w.visible() && w.mouseReleased(click))
            {
                return true;
            }
        }

        return super.onMouseReleased(click);
    }

    @Override
    public boolean onMouseDragged(MouseButtonEvent click, double dx, double dy)
    {
        if (this.cutTool.dragging())
        {
            this.cutTool.drag(this.editor, click.x(), click.y());
            return true;
        }

        if (this.dragging3d && this.preview != null)
        {
            return MaterialHudBridge.previewDragged(this.preview, click.x(), click.y(), click.button(), dx, dy);
        }

        for (Widget w : this.widgets)
        {
            if (w.visible() && w.mouseDragged(click, dx, dy))
            {
                return true;
            }
        }

        return super.onMouseDragged(click, dx, dy);
    }

    @Override
    public boolean onMouseScrolled(double mouseX, double mouseY, double horizontal, double vertical)
    {
        int x = this.px + Ui.PAD_SECTION;

        for (int axis = 0; axis < 3; ++axis)
        {
            int ry = this.rowsY + axis * AXIS_STEP;

            if (vertical != 0.0D && mouseX >= x && mouseX < x + this.leftW && mouseY >= ry && mouseY < ry + Ui.ROW_H)
            {
                this.nudge(axis, (vertical > 0 ? 1 : -1) * (isShiftDown() ? 10 : 1));
                return true;
            }
        }

        if (this.list.contains(mouseX, mouseY) && this.list.mouseScrolled(mouseX, mouseY, vertical))
        {
            return true;
        }

        if (this.in3d() && this.preview != null && this.inMap(mouseX, mouseY) && vertical != 0.0D)
        {
            return MaterialHudBridge.previewScrolled(this.preview, mouseX, mouseY, vertical);
        }

        return super.onMouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean onKeyTyped(KeyEvent key)
    {
        if (this.renaming != null)
        {
            if (key.key() == KEY_ESC)
            {
                this.cancelRename();
                return true;
            }

            return this.renameField.keyPressed(key) || true;
        }

        for (int axis = 0; axis < 3; ++axis)
        {
            if (this.fields[axis].focused())
            {
                if (key.key() == KEY_UP || key.key() == KEY_DOWN)
                {
                    this.editor.nudge(axis, (key.key() == KEY_UP ? 1 : -1) * (isShiftDown() ? 10 : 1));
                    this.syncFields(true);
                    return true;
                }

                if (key.key() == KEY_ESC)
                {
                    this.fields[axis].setFocused(false);
                    this.syncFields(true);
                    return true;
                }

                return this.fields[axis].keyPressed(key) || true;
            }
        }

        for (Widget w : this.widgets)
        {
            if (w.visible() && w.keyPressed(key))
            {
                return true;
            }
        }

        return super.onKeyTyped(key);
    }

    @Override
    public boolean onCharTyped(CharacterEvent event)
    {
        if (this.renaming != null)
        {
            return this.renameField.charTyped(event);
        }

        for (TextInput f : this.fields)
        {
            if (f.focused())
            {
                return f.charTyped(event);
            }
        }

        return super.onCharTyped(event);
    }

    @Override
    public void removed()
    {
        if (this.renaming != null)
        {
            this.commitRename();
        }

        for (TextInput f : this.fields)
        {
            f.setFocused(false);
        }

        new com.aell.zonefocus.config.ZoneFocusConfigs().save();

        if (this.preview != null)
        {
            MaterialHudBridge.previewClose(this.preview);
        }

        if (this.topView != null)
        {
            MaterialHudBridge.previewClose(this.topView);
        }

        ZoneRenderer.setPreviewIndex(-1);
        ZoneRenderer.setGridVisibleFromGui(false);
        super.removed();
    }
}
