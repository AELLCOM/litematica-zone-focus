package com.aell.zonefocus.gui;

import com.aell.zonefocus.compat.MaterialHudBridge;
import com.aell.zonefocus.compat.MaterialHudCompat;
import com.aell.zonefocus.config.GuiZoneFocusConfigs;
import com.aell.zonefocus.config.ZoneFocusConfigs;
import com.aell.zonefocus.shaded.ui.Paint;
import com.aell.zonefocus.shaded.ui.Strings;
import com.aell.zonefocus.shaded.ui.Style;
import com.aell.zonefocus.shaded.ui.Vanilla;
import com.aell.zonefocus.tool.ZoneTool;
import com.aell.zonefocus.ui.Draw;
import com.aell.zonefocus.ui.Theme;
import com.aell.zonefocus.ui.ZfButton;
import com.aell.zonefocus.ui.ZfScreen;
import com.aell.zonefocus.verify.ZoneVerifier;
import com.aell.zonefocus.zone.ZoneBox;
import com.aell.zonefocus.zone.ZoneManager;
import com.aell.zonefocus.zone.ZoneActions;
import com.aell.zonefocus.zone.ZoneState;
import com.aell.zonefocus.zone.model.Section;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.selection.SelectionMode;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.StringUtils;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Util;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class GuiZoneMain extends ZfScreen
{
    private static final int MIN_PANEL_W = 220;
    private static final int MAX_PANEL_W = 300;
    private static final int ROW_EXTRA = 46;
    private static final int FOOTER_EXTRA = 48;
    private static final int CHIP_H = 12;
    private static final int CHIP_STEP = 14;

    private static final String[] ACTION_KEYS = {
            "zonefocus.gui.split",
            "zonefocus.gui.zone_list",
            "zonefocus.gui.sub_regions",
            "zonefocus.gui.from_litematica",
            "zonefocus.gui.tool_on",
            "zonefocus.gui.tool_off",
            "zonefocus.gui.search_zone",
            "zonefocus.gui.verify_zone",
            "zonefocus.gui.verify_off",
            "zonefocus.gui.clear_zone",
            "zonefocus.gui.clear_zone_confirm",
            "zonefocus.gui.clear_all",
            "zonefocus.gui.clear_all_confirm" };

    private static final long CONFIRM_MS = 3000L;

    private @Nullable ZfButton clearZoneButton;
    private @Nullable ZfButton clearAllButton;
    private long clearZoneUntil;
    private long clearAllUntil;

    public GuiZoneMain(@Nullable Screen parent)
    {
        super(parent);
        this.title = Strings.plain("zonefocus.gui.main_title");
        this.crumb = "Litematica Zone Focus";
        this.edgePinned = true;
        this.switcher = new com.aell.zonefocus.shaded.ui.ModSwitcher("zonefocus");
    }

    @Override
    protected int maxWidth()
    {
        int need = this.getStringWidth(this.title);

        for (String key : ACTION_KEYS)
        {
            need = Math.max(need, this.getStringWidth(Strings.plain(key)) + ROW_EXTRA);
        }

        need = Math.max(need, this.getStringWidth(Strings.plain("zonefocus.gui.settings"))
                + this.getStringWidth(Strings.plain("zonefocus.gui.back")) + FOOTER_EXTRA);

        int width = need + this.pad() * 2;

        return Math.max(MIN_PANEL_W, Math.min(MAX_PANEL_W, width));
    }

    @Override
    public void initGui()
    {
        super.initGui();

        ZoneManager manager = ZoneManager.getInstance();
        ZoneVerifier verifier = ZoneVerifier.getInstance();
        ZoneTool tool = ZoneTool.getInstance();

        int top = this.contentY() + this.chipBlockHeight();
        int bottom = this.footerY() - 10;
        int count = 8;
        int step = Math.max(15, Math.min(26, (bottom - top) / count));
        int height = Math.max(12, Math.min(step - 3, bottom - (top + (count - 1) * step)));
        int y = top;

        y = this.action(y, step, height, "zonefocus.gui.manager.open", Theme.BLUE, () -> {
            Section activeSection = manager.activeSection();

            if (activeSection != null)
            {
                GuiBase.openGui(new com.aell.zonefocus.gui.manage.GuiZoneManager(this, activeSection));
                return;
            }

            SchematicPlacement placement = com.aell.zonefocus.zone.ZoneActions.managerPlacement();

            if (placement == null)
            {
                this.setMessage(Strings.plain("zonefocus.message.no_placement"), Theme.RED);
                this.reinit();
                return;
            }

            GuiBase.openGui(new com.aell.zonefocus.gui.manage.GuiZoneManager(this, placement));
        });

        y = this.action(y, step, height, "zonefocus.gui.sub_regions", Theme.MUTED, () -> {
            SchematicPlacement placement = pickPlacement();

            if (placement == null)
            {
                this.setMessage(Strings.plain("zonefocus.message.no_placement"), Theme.RED);
                this.reinit();
                return;
            }

            manager.splitBySubRegions(placement);
            this.setMessage(StringUtils.translate("zonefocus.message.zones_ready", manager.getZoneCount()));
            this.reinit();
        });

        y = this.action(y, step, height, "zonefocus.gui.from_litematica", Theme.MUTED, () -> {
            ZoneBox box = selectionBox();

            if (box == null)
            {
                this.setMessage(Strings.plain("zonefocus.message.no_selection"), Theme.RED);
                this.reinit();
                return;
            }

            ZoneActions.zoneFromSelection();
            this.setMessage(Strings.plain("zonefocus.message.zone_from_selection"));
            this.reinit();
        });

        String toolKey = tool.isActive() ? "zonefocus.gui.tool_off" : "zonefocus.gui.tool_on";
        ZfButton markButton = this.actionButton(y, height, toolKey, Theme.GREEN, () -> {
            if (! ZoneFocusConfigs.TOOL_ENABLED.getBooleanValue())
            {
                this.setMessage(Strings.plain("zonefocus.message.tool_disabled"), Theme.RED);
                this.reinit();
                return;
            }

            ZoneTool.getInstance().toggle();

            if (ZoneTool.getInstance().isActive())
            {
                this.closeGui(false);
            }
            else
            {
                this.reinit();
            }
        });
        markButton.keyHint(ZoneFocusConfigs.TOGGLE_TOOL.getKeybind().getKeysDisplayString());
        y += step;

        y = this.action(y, step, height, "zonefocus.gui.search_zone", Theme.CYAN, () -> {
            if (! MaterialHudCompat.isIntegrated())
            {
                this.setMessage(Strings.plain("zonefocus.message.requires_material_hud"), Theme.RED);
                this.reinit();
                return;
            }

            int searching = MaterialHudBridge.toggleZoneSearch();

            if (searching < 0)
            {
                this.setMessage(Strings.plain("zonefocus.message.nothing_to_search"), Theme.RED);
            }
            else
            {
                this.setMessage(searching > 0
                        ? StringUtils.translate("zonefocus.message.search_on", searching)
                        : Strings.plain("zonefocus.message.search_off"));
            }

            this.reinit();
        });

        String verifyKey = verifier.isHighlighting() ? "zonefocus.gui.verify_off" : "zonefocus.gui.verify_zone";
        y = this.action(y, step, height, verifyKey, Theme.YELLOW, () -> {
            this.setMessage(verifier.toggle());
            this.reinit();
        });

        this.clearZoneButton = this.actionButton(y, height, "zonefocus.gui.clear_zone", Theme.RED, () -> {
            if (Util.getMillis() > this.clearZoneUntil)
            {
                // Первый щелчок — только подтверждение, второй за 3 с — действие
                this.clearZoneUntil = Util.getMillis() + CONFIRM_MS;
                this.clearAllUntil = 0L;
                return;
            }

            this.clearZoneUntil = 0L;

            if (manager.hasSections())
            {
                manager.removeSection(manager.getActiveSectionIndex());
            }

            this.setMessage(Strings.plain("zonefocus.message.zone_cleared"));
            this.reinit();
        });
        y += step;

        this.clearAllButton = this.actionButton(y, height, "zonefocus.gui.clear_all", Theme.RED, () -> {
            if (Util.getMillis() > this.clearAllUntil)
            {
                this.clearAllUntil = Util.getMillis() + CONFIRM_MS;
                this.clearZoneUntil = 0L;
                return;
            }

            this.clearAllUntil = 0L;
            manager.clear();
            this.setMessage(Strings.plain("zonefocus.message.zone_cleared"));
            this.reinit();
        });

        String settings = Strings.plain("zonefocus.gui.settings");
        int settingsWidth = Math.min(this.contentWidth() - 70, this.getStringWidth(settings) + 24);

        this.button(this.contentX(), this.footerY(), settingsWidth, 18, settings,
                () -> GuiZoneFocusConfigs.open(this, GuiZoneFocusConfigs.ZONES_BACK_LABEL))
                .style(ZfButton.Style.PRIMARY)
                .accent(Theme.GREEN);

        String back = Strings.plain("zonefocus.gui.back");
        int backWidth = Math.max(56, this.getStringWidth(back) + 20);

        this.button(this.contentX() + this.contentWidth() - backWidth, this.footerY(), backWidth, 18, back,
                () -> this.closeGui(true))
                .style(ZfButton.Style.GHOST);
    }

    private int action(int y, int step, int height, String labelKey, int dot, Runnable action)
    {
        this.actionButton(y, height, labelKey, dot, action);

        return y + step;
    }

    private ZfButton actionButton(int y, int height, String labelKey, int dot, Runnable action)
    {
        ZfButton button = this.button(this.contentX(), y, this.contentWidth(), height,
                Strings.plain(labelKey), action);

        return button.style(ZfButton.Style.ROW).dot(dot);
    }

    private void reinit()
    {
        this.clearElements();
        this.initGui();
    }

    @Nullable
    public static SchematicPlacement pickPlacement()
    {
        SchematicPlacement bound = ZoneManager.getInstance().getPlacement();

        if (bound != null)
        {
            return bound;
        }

        List<SchematicPlacement> enabled = ZoneManager.enabledPlacements();

        if (enabled.isEmpty())
        {
            return null;
        }

        SchematicPlacement selected = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

        return selected != null && selected.isEnabled() ? selected : enabled.getFirst();
    }

    @Nullable
    public static ZoneBox selectionBox()
    {
        var manager = DataManager.getSelectionManager();
        AreaSelection selection = manager.getSelectionMode() == SelectionMode.SIMPLE
                ? manager.getSimpleSelection()
                : manager.getCurrentSelection();

        if (selection == null)
        {
            return null;
        }

        ZoneBox result = null;

        for (Box box : selection.getAllSubRegionBoxes())
        {
            if (box.getPos1() == null || box.getPos2() == null)
            {
                continue;
            }

            ZoneBox current = ZoneBox.of(box);
            result = result == null ? current : result.union(current);
        }

        return result;
    }

    @Override
    protected void drawContents(GuiContext ctx, int mouseX, int mouseY, float partialTicks)
    {
        ZoneManager manager = ZoneManager.getInstance();
        ZoneVerifier verifier = ZoneVerifier.getInstance();

        long now = Util.getMillis();

        if (this.clearZoneButton != null)
        {
            this.clearZoneButton.setDisplayString(Strings.plain(now <= this.clearZoneUntil
                    ? "zonefocus.gui.clear_zone_confirm" : "zonefocus.gui.clear_zone"));
        }

        if (this.clearAllButton != null)
        {
            this.clearAllButton.setDisplayString(Strings.plain(now <= this.clearAllUntil
                    ? "zonefocus.gui.clear_all_confirm" : "zonefocus.gui.clear_all"));
        }

        int x = this.contentX();
        int y = this.contentY();
        int limit = this.contentX() + this.contentWidth();
        Chip hovered = null;
        int hoverX = 0;
        int hoverY = 0;

        for (Chip chip : this.chips(manager, verifier))
        {
            String text = Draw.fit(this.font, chip.text(), this.contentWidth() - 12);
            int width = this.getStringWidth(text) + 12;

            if (x > this.contentX() && x + width > limit)
            {
                x = this.contentX();
                y += CHIP_STEP;
            }

            if (Style.vanilla())
            {
                // Ванильный вид: информационная метка без плитки, только цветной текст
                Paint.text(ctx, this.font, text, x + 6, y + 2, chip.text2(), false);
            }
            else
            {
                Draw.chip(ctx, this.font, text, x, y, chip.text2(), chip.fill(), chip.edge());
            }

            if (! chip.tip().isEmpty() && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + CHIP_H)
            {
                hovered = chip;
                hoverX = mouseX;
                hoverY = mouseY;
            }

            x += width + Theme.GAP;
        }

        this.drawFooter(ctx);

        if (hovered != null)
        {
            this.drawChipTip(ctx, hovered.tip(), hoverX, hoverY);
        }
    }

    /** Подсказка чипа: строки с именами, в стиле подсказок экрана. */
    private void drawChipTip(GuiContext ctx, List<String> lines, int mouseX, int mouseY)
    {
        int textWidth = 0;

        for (String line : lines)
        {
            textWidth = Math.max(textWidth, this.getStringWidth(line));
        }

        int width = textWidth + 12;
        int height = lines.size() * 10 + 6;
        int tx = Math.max(4, Math.min(mouseX + 10, this.width - width - 4));
        int ty = Math.max(4, Math.min(mouseY + 10, this.height - height - 4));
        if (Style.vanilla())
        {
            // Ванильная рамка подсказки Minecraft
            Vanilla.tooltip(ctx, tx, ty, width, height);
        }
        else
        {
            Draw.card(ctx, tx, ty, width, height, Theme.RADIUS, Theme.PANEL_DEEP, Theme.EDGE);
        }

        for (int i = 0; i < lines.size(); ++i)
        {
            Paint.text(ctx, this.font, lines.get(i), tx + 6, ty + 4 + i * 10, i == 0 ? Theme.TEXT : Theme.MUTED, false);
        }
    }

    private record Chip(String text, int text2, int fill, int edge, List<String> tip)
    {
        Chip(String text, int text2, int fill, int edge)
        {
            this(text, text2, fill, edge, List.of());
        }
    }

    private List<Chip> chips(ZoneManager manager, ZoneVerifier verifier)
    {
        List<Chip> chips = new java.util.ArrayList<>();
        boolean hasZones = manager.hasZones();
        List<Section> sections = manager.sections();
        boolean multi = sections.size() > 1;
        List<SchematicPlacement> enabled = ZoneManager.enabledPlacements();

        if (multi)
        {
            List<String> names = new java.util.ArrayList<>();
            names.add(Strings.plain("zonefocus.gui.chip_sections_tip"));

            for (int i = 0; i < sections.size(); ++i)
            {
                names.add((i == manager.getActiveSectionIndex() ? "> " : "  ") + (i + 1) + ". "
                        + com.aell.zonefocus.gui.manage.ZoneEditor.sectionTitle(sections.get(i)));
            }

            chips.add(new Chip(StringUtils.translate("zonefocus.gui.chip_sections", sections.size(),
                    manager.getActiveSectionIndex() + 1), Theme.GREEN, Theme.GREEN_FILL, Theme.GREEN_EDGE, names));

            ZoneBox zone = manager.getActiveZone();

            if (hasZones && zone != null && manager.getKind() != ZoneState.Kind.MANUAL)
            {
                chips.add(new Chip(StringUtils.translate("zonefocus.gui.chip_zone", manager.getActiveIndex() + 1,
                        manager.getZoneCount(), zone.sizeX() + "×" + zone.sizeY() + "×" + zone.sizeZ()),
                        Theme.GREEN, Theme.GREEN_FILL, Theme.GREEN_EDGE));
            }
            else
            {
                chips.add(new Chip(describeState(manager), hasZones ? Theme.GREEN : Theme.MUTED,
                        hasZones ? Theme.GREEN_FILL : Theme.ROW, hasZones ? Theme.GREEN_EDGE : Theme.EDGE_SOFT));
            }
        }
        else
        {
            chips.add(new Chip(describeState(manager),
                    hasZones ? Theme.GREEN : Theme.MUTED,
                    hasZones ? Theme.GREEN_FILL : Theme.ROW,
                    hasZones ? Theme.GREEN_EDGE : Theme.EDGE_SOFT));
        }

        SchematicPlacement placement = pickPlacement();

        if (multi || enabled.size() > 1)
        {
            List<String> names = new java.util.ArrayList<>();
            names.add(Strings.plain("zonefocus.gui.chip_schematics_tip"));
            enabled.forEach(p -> names.add("  " + p.getName()));
            chips.add(new Chip(StringUtils.translate("zonefocus.gui.chip_schematics", enabled.size()),
                    Theme.BLUE, Theme.BLUE_FILL, Theme.BLUE_EDGE, names));
        }
        else if (placement != null)
        {
            chips.add(new Chip(StringUtils.translate("zonefocus.gui.placement", placement.getName()),
                    Theme.BLUE, Theme.BLUE_FILL, Theme.BLUE_EDGE));
        }

        if (verifier.isHighlighting())
        {
            chips.add(new Chip(StringUtils.translate("zonefocus.gui.verify_state", verifier.getSummary()),
                    Theme.YELLOW, Theme.alpha(Theme.YELLOW, 0.16F), Theme.alpha(Theme.YELLOW, 0.5F)));
        }

        return chips;
    }

    private int chipBlockHeight()
    {
        int x = this.contentX();
        int limit = this.contentX() + this.contentWidth();
        int rows = 1;

        for (Chip chip : this.chips(ZoneManager.getInstance(), ZoneVerifier.getInstance()))
        {
            int width = this.getStringWidth(Draw.fit(this.font, chip.text(), this.contentWidth() - 12)) + 12;

            if (x > this.contentX() && x + width > limit)
            {
                x = this.contentX();
                ++rows;
            }

            x += width + Theme.GAP;
        }

        return rows * CHIP_STEP + CHIP_H / 2;
    }

    private static String describeState(ZoneManager manager)
    {
        ZoneState state = manager.getState();
        ZoneBox zone = manager.getActiveZone();

        if (! manager.hasZones() || zone == null)
        {
            return Strings.plain("zonefocus.gui.state_none");
        }

        String size = zone.sizeX() + "×" + zone.sizeY() + "×" + zone.sizeZ();
        String text = describeKind(manager, state, size);

        return manager.sections().size() > 1
                ? StringUtils.translate("zonefocus.world.section_prefix", manager.getActiveSectionIndex() + 1, text)
                : text;
    }

    private static String describeKind(ZoneManager manager, ZoneState state, String size)
    {
        return switch (state.kind)
        {
            case GRID -> StringUtils.translate("zonefocus.gui.state_grid",
                    state.nx + "×" + state.ny + "×" + state.nz, manager.getZoneCount(),
                    manager.getActiveIndex() + 1, size);
            case SUB_REGIONS -> StringUtils.translate("zonefocus.gui.state_sub_regions",
                    manager.getZoneCount(), manager.getActiveIndex() + 1, size);
            case MANUAL -> StringUtils.translate("zonefocus.gui.state_manual", size);
            case NONE -> Strings.plain("zonefocus.gui.state_none");
        };
    }
}
