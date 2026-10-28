package com.aell.zonefocus.gui;

import com.aell.zonefocus.config.ZoneFocusConfigs;
import com.aell.zonefocus.gui.manage.ZoneEditor;
import com.aell.zonefocus.shaded.ui.Nav;
import com.aell.zonefocus.shaded.ui.Paint;
import com.aell.zonefocus.shaded.ui.Strings;
import com.aell.zonefocus.shaded.ui.Style;
import com.aell.zonefocus.shaded.ui.Ui;
import com.aell.zonefocus.shaded.ui.Vanilla;
import com.aell.zonefocus.shaded.ui.kit.Button;
import com.aell.zonefocus.shaded.ui.kit.Kit;
import com.aell.zonefocus.shaded.ui.kit.ScrollList;
import com.aell.zonefocus.shaded.ui.kit.TextFocus;
import com.aell.zonefocus.shaded.ui.kit.TextInput;
import com.aell.zonefocus.shaded.ui.kit.Tooltip;
import com.aell.zonefocus.shaded.ui.kit.Widget;
import com.aell.zonefocus.zone.FileZones;
import com.aell.zonefocus.zone.ZoneBox;
import com.aell.zonefocus.zone.ZoneManager;
import com.aell.zonefocus.zone.model.Bounds;
import com.aell.zonefocus.zone.model.Layout;
import com.aell.zonefocus.zone.model.PlacementRef;
import com.aell.zonefocus.zone.model.Rule;
import com.aell.zonefocus.zone.model.Section;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.StringUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Util;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Окно «Новый участок» / «Разбить заново»: выбор размещений флажками, способ разбиения, «Каждая отдельно /
 * Общий габарит», имя участка. Слева список включённых размещений, справа способ и сетка, внизу «Разбить» и
 * «Отмена». Экран на ките aell-ui, пиксельный, цвета — {@link Ui}.
 */
@Environment(EnvType.CLIENT)
public final class GuiSectionSplit extends GuiBase
{
    private static final int CTRL_H = 18;
    private static final int LABEL_H = 11;
    private static final int AXIS_STEP = Ui.ROW_H + 1;
    private static final int KEY_UP = 265;
    private static final int KEY_DOWN = 264;
    private static final int KEY_ESC = 256;
    private static final int LEFT_MIN = 200;
    private static final int LEFT_MAX = 300;
    private static final int ROW_H = 30;
    private static final int BOX = 12;

    private enum Mode { GRID, SUB_REGIONS, SELECTION }

    /** Строка списка: включённое размещение, его габарит и участок, в котором оно уже состоит. */
    private record Row(SchematicPlacement placement, @Nullable ZoneBox box, @Nullable Section owner, int ownerNumber) {}

    private final @Nullable Section section;
    private final List<Widget> widgets = new ArrayList<>();
    private final ScrollList<Row> list = new ScrollList<>(this::renderRow);
    private final Set<UUID> checked = new LinkedHashSet<>();
    private final Button[] modes = new Button[2];
    private final Button[] bounds = new Button[2];
    private final TextInput[] fields = new TextInput[3];
    private final Button[] minus = new Button[3];
    private final Button[] plus = new Button[3];
    private Button subRegions;
    private Button fromSelection;
    private Button split;
    private Button cancel;
    private TextInput nameField;
    private boolean nameTouched;
    private boolean settingName;
    private String typedName = "";
    private boolean combined;
    private boolean initialized;
    private List<Row> rows = List.of();

    private int px;
    private int py;
    private int pw;
    private int ph;
    private int leftW;
    private int top;
    private int rightX;
    private int rightW;
    private int rowsY;
    private int chipsY;
    private int boundsY;
    private int nameY;
    private int notesY;
    private int bottom;

    private GuiSectionSplit(@Nullable Screen parent, @Nullable Section section)
    {
        this.setParent(parent);
        this.section = section;
        this.title = Strings.plain(section == null ? "zonefocus.gui.split.title_new" : "zonefocus.gui.split.title_redo");
    }

    /** Открыть поверх {@code parent}: {@code section == null} — новый участок, иначе «Разбить заново» этого. */
    public static void open(Screen parent, @Nullable Section section)
    {
        Nav.open(new GuiSectionSplit(parent, section), parent);
    }

    private static String t(String key)
    {
        return Strings.plain("zonefocus.gui.split." + key);
    }

    private static String m(String key)
    {
        return Strings.plain("zonefocus.gui.manager." + key);
    }

    // ===== Построение =====

    @Override
    public void initGui()
    {
        super.initGui();
        this.build();
        this.layout();
    }

    private void build()
    {
        this.widgets.clear();
        this.loadRows();

        this.modes[0] = new Button(m("mode_count"), Button.Style.SECONDARY, () -> this.setMode(false));
        this.modes[1] = new Button(m("mode_size"), Button.Style.SECONDARY, () -> this.setMode(true));
        this.bounds[0] = new Button(t("bounds_each"), Button.Style.SECONDARY, () -> this.combined = false)
                .tooltip(t("bounds_each_tip"));
        this.bounds[1] = new Button(t("bounds_combined"), Button.Style.SECONDARY, () -> this.combined = true)
                .tooltip(t("bounds_combined_tip"));

        for (int axis = 0; axis < 3; ++axis)
        {
            final int a = axis;
            this.minus[a] = new Button("−", Button.Style.SECONDARY, () -> this.nudge(a, -1)).centered(true);
            this.plus[a] = new Button("+", Button.Style.SECONDARY, () -> this.nudge(a, 1)).centered(true);
            this.fields[a] = new TextInput().maxLength(3).centered(true).filter(c -> c >= '0' && c <= '9')
                    .onChange(text -> this.typed(a, text))
                    .onSubmit(text -> this.fields[a].setFocused(false))
                    .tooltip(m("field_tip"));
            this.widgets.add(this.minus[a]);
            this.widgets.add(this.fields[a]);
            this.widgets.add(this.plus[a]);
        }

        this.subRegions = new Button(m("sub_regions"), Button.Style.SECONDARY, () -> this.apply(Mode.SUB_REGIONS))
                .centered(true).tooltip(m("sub_regions_tip"));
        this.fromSelection = new Button(t("from_selection"), Button.Style.SECONDARY, () -> this.apply(Mode.SELECTION))
                .centered(true).tooltip(t("from_selection_tip"));
        this.nameField = new TextInput().maxLength(48).hint(t("name_hint")).onChange(text -> {
            if (! this.settingName)
            {
                this.nameTouched = true;
                this.typedName = text;
            }
        }).onSubmit(text -> this.nameField.setFocused(false));
        this.split = new Button(t("split"), Button.Style.PRIMARY, () -> this.apply(Mode.GRID)).centered(true);
        this.cancel = new Button(t("cancel"), Button.Style.SECONDARY, this::back).centered(true);

        this.list.showSelection(false).rowHeight(row -> ROW_H).gap(Ui.ROW_GAP);
        this.list.setEmptyText(t("list_empty"));
        this.list.listener(new ScrollList.Listener<>()
        {
            @Override
            public void activate(Row item, int index)
            {
                GuiSectionSplit.this.toggle(item);
            }

            @Override
            public boolean click(Row item, int index, int rowX, int rowY, int rowW, int rowH, MouseButtonEvent click)
            {
                if (Kit.left(click))
                {
                    GuiSectionSplit.this.toggle(item);
                    return true;
                }

                return false;
            }
        });
        this.list.setItems(this.rows);

        this.widgets.add(this.modes[0]);
        this.widgets.add(this.modes[1]);
        this.widgets.add(this.bounds[0]);
        this.widgets.add(this.bounds[1]);
        this.widgets.add(this.subRegions);
        this.widgets.add(this.fromSelection);
        this.widgets.add(this.nameField);
        this.widgets.add(this.split);
        this.widgets.add(this.cancel);
        this.widgets.add(this.list);
        this.syncFields(true);
        this.settingName = true;
        this.nameField.setText(this.nameTouched ? this.typedName : this.defaultName());
        this.settingName = false;
    }

    /** Список включённых размещений; при «Разбить заново» состав участка отмечен заранее. */
    private void loadRows()
    {
        ZoneManager manager = ZoneManager.getInstance();
        List<Row> result = new ArrayList<>();
        List<Section> sections = manager.sections();

        for (SchematicPlacement placement : ZoneManager.enabledPlacements())
        {
            Section owner = manager.sectionOf(placement);
            int number = owner == null ? 0 : sections.indexOf(owner) + 1;
            result.add(new Row(placement, ZoneManager.enclosingBox(placement), owner, number));

            if (! this.initialized && this.section != null && owner == this.section)
            {
                this.checked.add(placement.getHashId());
            }
        }

        this.rows = result;

        if (! this.initialized && this.section != null)
        {
            this.combined = this.section.bounds == Bounds.COMBINED;
            this.nameTouched = true;
            this.typedName = this.section.name;
        }

        this.initialized = true;
    }

    private String defaultName()
    {
        List<Row> picked = this.picked();
        return picked.isEmpty() ? "" : picked.getFirst().placement().getName();
    }

    private List<Row> picked()
    {
        List<Row> result = new ArrayList<>();

        for (Row row : this.rows)
        {
            if (this.checked.contains(row.placement().getHashId()))
            {
                result.add(row);
            }
        }

        return result;
    }

    private void toggle(Row row)
    {
        UUID id = row.placement().getHashId();

        if (! this.checked.remove(id))
        {
            this.checked.add(id);
        }

        if (! this.nameTouched)
        {
            this.settingName = true;
            this.nameField.setText(this.defaultName());
            this.settingName = false;
        }
    }

    // ===== Раскладка =====

    private void layout()
    {
        this.px = Ui.OUTER;
        this.py = Ui.OUTER;
        this.pw = this.width - Ui.OUTER * 2;
        this.ph = this.height - Ui.OUTER * 2;
        this.top = this.py + Ui.HEADER_H + Ui.PAD_GROUP;
        this.bottom = this.py + this.ph - Ui.PAD_GROUP - CTRL_H - Ui.PAD_GROUP;
        int x = this.px + Ui.PAD_SECTION;
        this.leftW = Math.max(LEFT_MIN, Math.min(LEFT_MAX, this.pw * 42 / 100));
        this.rightX = x + this.leftW + Ui.PAD_SECTION;
        this.rightW = this.px + this.pw - Ui.PAD_SECTION - this.rightX;

        // Слева: список и строки-предупреждения под ним
        int listY = this.top + LABEL_H + Ui.PAD_MICRO;
        this.notesY = this.bottom - 3 * 11;
        this.list.setBounds(x, listY, this.leftW, Math.max(40, this.notesY - Ui.PAD_MICRO - listY));

        // Справа сверху вниз
        int y = this.top + LABEL_H;
        int segW = (this.rightW - Ui.PAD_IN) / 2;
        this.modes[0].setBounds(this.rightX, y, segW, CTRL_H);
        this.modes[1].setBounds(this.rightX + segW + Ui.PAD_IN, y, this.rightW - segW - Ui.PAD_IN, CTRL_H);
        y += CTRL_H + Ui.PAD_GROUP;
        this.rowsY = y + LABEL_H;

        for (int axis = 0; axis < 3; ++axis)
        {
            int ry = this.rowsY + axis * AXIS_STEP;
            int right = this.rightX + this.rightW;
            this.plus[axis].setBounds(right - CTRL_H, ry + 1, CTRL_H, CTRL_H);
            this.fields[axis].setBounds(right - CTRL_H - Ui.PAD_MICRO - 40, ry + 1, 40, CTRL_H);
            this.minus[axis].setBounds(right - CTRL_H * 2 - Ui.PAD_MICRO * 2 - 40, ry + 1, CTRL_H, CTRL_H);
        }

        this.chipsY = this.rowsY + 3 * AXIS_STEP + Ui.PAD_IN;
        this.boundsY = this.chipsY + 14 + Ui.PAD_GROUP;
        this.bounds[0].setBounds(this.rightX, this.boundsY + LABEL_H, segW, CTRL_H);
        this.bounds[1].setBounds(this.rightX + segW + Ui.PAD_IN, this.boundsY + LABEL_H, this.rightW - segW - Ui.PAD_IN, CTRL_H);
        int ay = this.boundsY + LABEL_H + CTRL_H + Ui.PAD_GROUP;
        this.subRegions.setBounds(this.rightX, ay, segW, Ui.ROW_H);
        this.fromSelection.setBounds(this.rightX + segW + Ui.PAD_IN, ay, this.rightW - segW - Ui.PAD_IN, Ui.ROW_H);
        this.nameY = ay + Ui.ROW_H + Ui.PAD_GROUP;
        this.nameField.setBounds(this.rightX, this.nameY + LABEL_H, this.rightW, CTRL_H);

        int cancelW = Math.max(70, this.cancel.preferredWidth(this.font));
        int splitW = Math.max(90, this.split.preferredWidth(this.font));
        int fy = this.py + this.ph - Ui.PAD_GROUP - CTRL_H;
        this.cancel.setBounds(this.px + this.pw - Ui.PAD_SECTION - cancelW, fy, cancelW, CTRL_H);
        this.split.setBounds(this.cancel.x() - Ui.PAD_IN - splitW, fy, splitW, CTRL_H);
    }

    // ===== Черновик =====

    private boolean bySize()
    {
        return ZoneFocusConfigs.SPLIT_BY_SIZE;
    }

    private int max()
    {
        return this.bySize() ? ZoneEditor.MAX_SIZE : ZoneEditor.MAX_COUNT;
    }

    private int value(int axis)
    {
        return this.bySize()
                ? switch (axis) { case 0 -> ZoneFocusConfigs.SPLIT_SIZE_X; case 1 -> ZoneFocusConfigs.SPLIT_SIZE_Y; default -> ZoneFocusConfigs.SPLIT_SIZE_Z; }
                : switch (axis) { case 0 -> ZoneFocusConfigs.SPLIT_X; case 1 -> ZoneFocusConfigs.SPLIT_Y; default -> ZoneFocusConfigs.SPLIT_Z; };
    }

    private void setValue(int axis, int value)
    {
        int v = Math.max(1, Math.min(this.max(), value));

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
    }

    private void setMode(boolean bySize)
    {
        ZoneFocusConfigs.SPLIT_BY_SIZE = bySize;
        this.syncFields(true);
    }

    private void nudge(int axis, int delta)
    {
        this.setValue(axis, this.value(axis) + delta);
        this.syncFields(true);
    }

    private void typed(int axis, String text)
    {
        if (! text.isEmpty())
        {
            try
            {
                this.setValue(axis, Integer.parseInt(text));
            }
            catch (NumberFormatException ignored)
            {
            }
        }
    }

    private void syncFields(boolean force)
    {
        for (int axis = 0; axis < 3; ++axis)
        {
            TextInput f = this.fields[axis];

            if (f != null && (force || ! f.focused()))
            {
                String v = String.valueOf(this.value(axis));

                if (! v.equals(f.text()))
                {
                    f.setText(v);
                }
            }
        }
    }

    /** Габарит разбиения: общий (объединение) или первое отмеченное размещение. */
    private @Nullable ZoneBox sizingBox(List<Row> picked, boolean useCombined)
    {
        ZoneBox result = null;

        for (Row row : picked)
        {
            if (row.box() == null)
            {
                continue;
            }

            if (! useCombined)
            {
                return row.box();
            }

            result = result == null ? row.box() : result.union(row.box());
        }

        return result;
    }

    /** Число зон по осям: из полей (режим «число») или по размеру зоны. */
    private int[] counts(@Nullable ZoneBox box)
    {
        int[] n = new int[3];
        int[] size = box == null ? new int[] {1, 1, 1} : new int[] {box.sizeX(), box.sizeY(), box.sizeZ()};

        for (int axis = 0; axis < 3; ++axis)
        {
            n[axis] = this.bySize() ? ZoneManager.ceilDiv(size[axis], Math.max(1, this.value(axis))) : Math.max(1, this.value(axis));
        }

        return n;
    }

    // ===== Применение =====

    private void apply(Mode mode)
    {
        List<Row> picked = this.picked();

        if (picked.isEmpty())
        {
            return;
        }

        ZoneBox selection = null;

        if (mode == Mode.SELECTION)
        {
            selection = GuiZoneMain.selectionBox();

            if (selection == null)
            {
                return;
            }
        }

        ZoneManager manager = ZoneManager.getInstance();
        Layout layout = manager.getLayout();
        boolean useCombined = this.combined && picked.size() > 1;
        int[] n = this.counts(this.sizingBox(picked, useCombined));
        String name = this.nameField.text().trim();

        if (name.isEmpty())
        {
            name = this.defaultName();
        }

        // Доноры — другие участки, у которых забираем отмеченные размещения (до любых правок состава)
        Map<Section, List<Row>> donors = new LinkedHashMap<>();

        for (Row row : picked)
        {
            if (row.owner() != null && row.owner() != this.section)
            {
                donors.computeIfAbsent(row.owner(), k -> new ArrayList<>()).add(row);
            }
        }

        List<PlacementRef> refs = new ArrayList<>();

        for (Row row : picked)
        {
            SchematicPlacement p = row.placement();
            PlacementRef ref = PlacementRef.byName(p.getName());
            ref.id = p.getHashId();
            ref.file = FileZones.key(p);
            ref.box = row.box();
            refs.add(ref);
        }

        // Состав донора правим до записи в целевой участок: ссылки целевого общих полей с донором не имеют
        for (Map.Entry<Section, List<Row>> donor : donors.entrySet())
        {
            donor.getKey().placements.removeIf(ref -> this.refTaken(ref, donor.getValue()));
        }

        Section target = this.section;

        if (target == null)
        {
            target = Section.create(new Rule.Grid(n[0], n[1], n[2]));
            layout.sections.add(target);
        }

        target.name = name;
        target.bounds = useCombined ? Bounds.COMBINED : Bounds.PER_PLACEMENT;
        target.placements.clear();
        target.placements.addAll(refs);
        layout.touch();

        for (Section d : donors.keySet())
        {
            if (d.placements.isEmpty())
            {
                int index = layout.indexOf(d);

                if (index >= 0)
                {
                    manager.removeSection(index);
                }
            }
        }

        switch (mode)
        {
            case GRID -> manager.splitGrid(target, n[0], n[1], n[2]);
            case SUB_REGIONS -> manager.splitBySubRegions(target);
            case SELECTION -> manager.setManualBoxes(target, List.of(selection));
        }

        new ZoneFocusConfigs().save();
        this.back();
    }

    /** Ссылка донора указывает на одно из забираемых размещений: по id, иначе по имени и файлу. */
    private boolean refTaken(PlacementRef ref, List<Row> taken)
    {
        for (Row row : taken)
        {
            SchematicPlacement p = row.placement();

            if (ref.id != null)
            {
                if (ref.id.equals(p.getHashId()))
                {
                    return true;
                }
            }
            else if (ref.name.equals(p.getName()) && (ref.file == null || ref.file.equals(FileZones.key(p))))
            {
                return true;
            }
        }

        return false;
    }

    /** Назад к родителю: он при открытии перечитывает состояние заново. */
    private void back()
    {
        if (Nav.peek() != null)
        {
            Nav.back();
        }
        else
        {
            this.closeGui(true);
        }
    }

    // ===== Отрисовка =====

    @Override
    protected void drawScreenBackground(GuiContext ctx, int mouseX, int mouseY)
    {
        Paint.rect(ctx, 0, 0, this.width, this.height, Ui.C_DIM);

        if (Style.vanilla())
        {
            // Ванильное окно: подложка попапа и разделитель под шапкой
            Vanilla.popup(ctx, this.px, this.py, this.pw, this.ph);
            Vanilla.headerSeparator(ctx, this.px + 1, this.py + Ui.HEADER_H, this.pw - 2);
            return;
        }

        Paint.card(ctx, this.px, this.py, this.pw, this.ph, Ui.R_CTRL, Ui.C_PANEL, Ui.C_PANEL_EDGE);
        Paint.rect(ctx, this.px + 1, this.py + 1, this.pw - 2, Ui.HEADER_H - 1, Ui.C_CHROME);
        Paint.rect(ctx, this.px, this.py + Ui.HEADER_H, this.pw, 1, Ui.C_DIVIDER);
    }

    @Override
    protected void drawTitle(GuiContext ctx, int mouseX, int mouseY, float partialTicks)
    {
        int x = this.px + Ui.PAD_SECTION;
        Paint.midText(ctx, this.font, this.title, x, this.py, Ui.HEADER_H, Ui.C_TEXT, false);
        x += this.font.width(this.title) + Ui.PAD_REL;

        if (this.section != null && ! this.section.name.isEmpty())
        {
            Paint.midText(ctx, this.font, Paint.fit(this.font, "· " + this.section.name, this.px + this.pw - x - Ui.PAD_SECTION),
                    x, this.py, Ui.HEADER_H, Ui.C_TEXT_3, false);
        }
    }

    @Override
    protected void drawContents(GuiContext ctx, int mouseX, int mouseY, float partialTicks)
    {
        Tooltip.beginFrame();
        this.syncFields(false);
        List<Row> picked = this.picked();
        boolean multi = picked.size() >= 2;
        boolean useCombined = this.combined && multi;
        int x = this.px + Ui.PAD_SECTION;

        this.modes[0].selected(! this.bySize());
        this.modes[1].selected(this.bySize());
        this.bounds[0].selected(! useCombined);
        this.bounds[1].selected(useCombined);
        this.bounds[0].setVisible(multi);
        this.bounds[1].setVisible(multi);
        this.split.setEnabled(! picked.isEmpty());
        this.subRegions.setEnabled(! picked.isEmpty());
        this.fromSelection.setEnabled(! picked.isEmpty() && GuiZoneMain.selectionBox() != null);

        section(ctx, StringUtils.translate("zonefocus.gui.split.section_schematics", picked.size()), x, this.top, this.leftW);
        section(ctx, m("section_mode"), this.rightX, this.top, this.rightW);
        section(ctx, m("section_grid"), this.rightX, this.rowsY - LABEL_H, this.rightW);

        String[] axes = {"X", "Y", "Z"};

        for (int axis = 0; axis < 3; ++axis)
        {
            int ry = this.rowsY + axis * AXIS_STEP;
            Paint.midText(ctx, this.font, axes[axis], this.rightX, ry, Ui.ROW_H, Ui.C_ACCENT, false);
            String hint = StringUtils.translate(this.bySize() ? "zonefocus.gui.axis_size_hint" : "zonefocus.gui.axis_count_hint",
                    axes[axis]);
            Paint.midText(ctx, this.font, Paint.fit(this.font, hint, this.minus[axis].x() - this.rightX - 12 - Ui.PAD_IN),
                    this.rightX + 12, ry, Ui.ROW_H, Ui.C_TEXT_2, false);
        }

        // Итог чипами
        ZoneBox box = this.sizingBox(picked, useCombined);
        int[] n = this.counts(box);
        int total = n[0] * n[1] * n[2] * (useCombined ? 1 : Math.max(1, picked.size()));
        int cx = chip(ctx, StringUtils.translate("zonefocus.gui.manager.chip_zones", total), this.rightX, this.chipsY, Ui.C_ON);

        if (box != null)
        {
            String size = ZoneManager.ceilDiv(box.sizeX(), n[0]) + "×" + ZoneManager.ceilDiv(box.sizeY(), n[1]) + "×"
                    + ZoneManager.ceilDiv(box.sizeZ(), n[2]);
            chip(ctx, "~" + size, cx + Ui.PAD_IN, this.chipsY, Ui.C_ACCENT);
        }

        if (multi)
        {
            section(ctx, t("section_bounds"), this.rightX, this.boundsY, this.rightW);
        }

        section(ctx, t("section_name"), this.rightX, this.nameY, this.rightW);
        this.drawNotes(ctx, x, picked);

        for (Widget w : this.widgets)
        {
            w.render(ctx, mouseX, mouseY);
        }

        int room = this.split.x() - Ui.PAD_GROUP - x;

        if (room > 80)
        {
            Paint.midText(ctx, this.font, Paint.fit(this.font, t("hint"), room), x, this.split.y(), CTRL_H, Ui.C_TEXT_3, false);
        }

        Tooltip.render(ctx, this.font, mouseX, mouseY, this.width, this.height);
    }

    /** Жёлтые строки под списком: перенос из других участков и наложение габаритов. */
    private void drawNotes(GuiContext ctx, int x, List<Row> picked)
    {
        List<String> lines = new ArrayList<>();

        for (Row row : picked)
        {
            if (row.owner() != null && row.owner() != this.section)
            {
                lines.add(StringUtils.translate("zonefocus.gui.split.move_note", row.ownerNumber()));
            }
        }

        boolean overlap = false;

        for (int i = 0; i < picked.size() && ! overlap; ++i)
        {
            for (int j = i + 1; j < picked.size(); ++j)
            {
                ZoneBox a = picked.get(i).box();
                ZoneBox b = picked.get(j).box();

                if (a != null && b != null && a.intersect(b) != null)
                {
                    overlap = true;
                    break;
                }
            }
        }

        if (overlap)
        {
            lines.add(t("overlap_note"));
        }

        int y = this.notesY;

        for (int i = 0; i < Math.min(3, lines.size()); ++i)
        {
            Paint.midText(ctx, this.font, Paint.fit(this.font, lines.get(i), this.leftW), x, y, 11, Ui.C_GOLD, false);
            y += 11;
        }
    }

    private void renderRow(GuiContext ctx, Row row, int index, int x, int y, int w, int h, boolean selected,
            boolean hovered, int mouseX, int mouseY)
    {
        boolean on = this.checked.contains(row.placement().getHashId());
        int bx = x + Ui.PAD_REL;
        int by = y + (h - BOX) / 2;

        if (Style.vanilla())
        {
            // Ванильная галочка вместо карточки с пиксельной отметкой
            Vanilla.checkbox(ctx, bx, by, BOX, BOX, on, hovered);
        }
        else
        {
            Paint.card(ctx, bx, by, BOX, BOX, Ui.R_CTRL, on ? Ui.alpha(Ui.C_ACCENT, 0.25F) : Ui.C_TRACK,
                    on ? Ui.C_ACCENT : Ui.C_DIVIDER);

            if (on)
            {
                // Галочка пикселями
                Paint.rect(ctx, bx + 3, by + 6, 1, 2, Ui.C_ACCENT);
                Paint.rect(ctx, bx + 4, by + 7, 1, 2, Ui.C_ACCENT);
                Paint.rect(ctx, bx + 5, by + 6, 1, 2, Ui.C_ACCENT);
                Paint.rect(ctx, bx + 6, by + 5, 1, 2, Ui.C_ACCENT);
                Paint.rect(ctx, bx + 7, by + 4, 1, 2, Ui.C_ACCENT);
                Paint.rect(ctx, bx + 8, by + 3, 1, 2, Ui.C_ACCENT);
            }
        }

        int tx = bx + BOX + Ui.PAD_REL;
        int right = x + w - Ui.PAD_REL;
        ZoneBox b = row.box();
        String size = b == null ? "" : b.sizeX() + "×" + b.sizeY() + "×" + b.sizeZ();
        int lineH = h / 2;
        Paint.midTextRight(ctx, this.font, size, right, y + 2, lineH, Ui.C_ACCENT, false);

        int nameRoom = right - tx - this.font.width(size) - Ui.PAD_REL;
        Paint.midText(ctx, this.font, Paint.fit(this.font, row.placement().getName(), Math.max(10, nameRoom)), tx, y + 2, lineH,
                on ? Ui.C_TEXT : Ui.C_TEXT_2, false);

        String file = row.placement().getSchematicFile() == null ? ""
                : row.placement().getSchematicFile().getFileName().toString();
        int fileRoom = right - tx;

        if (row.owner() != null && row.owner() != this.section)
        {
            String tail = StringUtils.translate("zonefocus.gui.split.in_section", row.ownerNumber());
            Paint.midTextRight(ctx, this.font, tail, right, y + lineH - 1, lineH, Ui.C_GOLD, false);
            fileRoom -= this.font.width(tail) + Ui.PAD_REL;
        }

        Paint.midText(ctx, this.font, Paint.fit(this.font, file, Math.max(10, fileRoom)), tx, y + lineH - 1, lineH,
                Ui.C_TEXT_3, false);
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

        if (! Style.vanilla())
        {
            // В ванильном режиме метка без плитки, только цветной текст
            Paint.card(ctx, x, y, w, 14, Ui.R_CTRL, Ui.alpha(color, 0.14F), Ui.alpha(color, 0.5F));
        }

        Paint.midText(ctx, this.font, text, x + Ui.PAD_REL, y, 14, color, false);
        return x + w;
    }

    // ===== Ввод =====

    @Override
    public boolean onMouseClicked(MouseButtonEvent click, boolean doubleClick)
    {
        for (TextInput f : this.fields)
        {
            if (f.focused() && ! f.contains(click.x(), click.y()))
            {
                f.setFocused(false);
                this.syncFields(true);
            }
        }

        if (this.nameField.focused() && ! this.nameField.contains(click.x(), click.y()))
        {
            this.nameField.setFocused(false);
        }

        for (Widget w : this.widgets)
        {
            if (w.visible() && w.mouseClicked(click, doubleClick))
            {
                return true;
            }
        }

        return super.onMouseClicked(click, doubleClick);
    }

    @Override
    public boolean onMouseReleased(MouseButtonEvent click)
    {
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
        for (int axis = 0; axis < 3; ++axis)
        {
            int ry = this.rowsY + axis * AXIS_STEP;

            if (vertical != 0.0D && mouseX >= this.rightX && mouseX < this.rightX + this.rightW && mouseY >= ry
                    && mouseY < ry + Ui.ROW_H)
            {
                this.nudge(axis, (vertical > 0 ? 1 : -1) * (isShiftDown() ? 10 : 1));
                return true;
            }
        }

        if (this.list.contains(mouseX, mouseY) && this.list.mouseScrolled(mouseX, mouseY, vertical))
        {
            return true;
        }

        return super.onMouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean onKeyTyped(KeyEvent key)
    {
        for (int axis = 0; axis < 3; ++axis)
        {
            if (this.fields[axis].focused())
            {
                if (key.key() == KEY_UP || key.key() == KEY_DOWN)
                {
                    this.nudge(axis, (key.key() == KEY_UP ? 1 : -1) * (isShiftDown() ? 10 : 1));
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

        if (this.nameField.focused())
        {
            if (key.key() == KEY_ESC)
            {
                this.nameField.setFocused(false);
                return true;
            }

            return this.nameField.keyPressed(key) || true;
        }

        if (key.key() == KEY_ESC)
        {
            this.back();
            return true;
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
        for (TextInput f : this.fields)
        {
            if (f.focused())
            {
                return f.charTyped(event);
            }
        }

        if (this.nameField.focused())
        {
            return this.nameField.charTyped(event);
        }

        return super.onCharTyped(event);
    }

    @Override
    public boolean preeditUpdated(net.minecraft.client.input.@Nullable PreeditEvent event)
    {
        return TextFocus.preedit(event) || super.preeditUpdated(event);
    }

    @Override
    public void removed()
    {
        for (TextInput f : this.fields)
        {
            f.setFocused(false);
        }

        this.nameField.setFocused(false);
        new ZoneFocusConfigs().save();
        super.removed();
    }
}
