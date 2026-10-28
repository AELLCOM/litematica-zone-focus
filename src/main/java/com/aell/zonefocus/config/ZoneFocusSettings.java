package com.aell.zonefocus.config;

import com.aell.zonefocus.shaded.ui.Model;
import com.aell.zonefocus.shaded.ui.Num;
import com.aell.zonefocus.shaded.ui.SharedOptions;
import com.aell.zonefocus.shaded.ui.Strings;
import com.aell.zonefocus.shaded.ui.SettingsModel;
import com.aell.zonefocus.shaded.ui.Ui;
import com.aell.zonefocus.shaded.ui.Values;
import com.aell.zonefocus.compat.MaterialHudCompat;
import com.aell.zonefocus.render.ZoneRenderer;
import com.aell.zonefocus.ui.Theme;
import com.aell.zonefocus.verify.ZoneVerifier;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class ZoneFocusSettings implements SettingsModel
{
    public static final String MOD_ID = "zonefocus";
    public static final String MOD_NAME = "Litematica Zone Focus";
    public static final String MALI_MOD_NAME = "Litematica Zone Focus";

    public static final net.minecraft.resources.Identifier ICON =
            net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/mod_icon.png");

    private static final String NEIGHBOUR_MOD_ID = "layerlist";

    /**
     * Встроенный модуль Zone Focus внутри Litematica Material HUD: те же опции под теми же
     * именами. Пока стоит этот мод, движок ведём мы, но настраивать человек может из обоих
     * экранов, поэтому значения общие, а его файл на диске держим в согласии со своим.
     */
    private static final SharedOptions HUD_SHARED =
            SharedOptions.of("com.aell.layerlist.compat.ZoneFocusBridge");

    private static final ZoneFocusSettings INSTANCE = new ZoneFocusSettings();

    private @Nullable List<Model.Tab> tabs;

    private ZoneFocusSettings() {}

    public static ZoneFocusSettings getInstance()
    {
        return INSTANCE;
    }

    @Override
    public String modName()
    {
        return MOD_NAME;
    }

    @Override
    public String modId()
    {
        return MOD_ID;
    }

    @Override
    public String maliModName()
    {
        return MALI_MOD_NAME;
    }

    @Override
    public net.minecraft.resources.Identifier icon()
    {
        return ICON;
    }

    @Override
    public List<Model.Tab> tabs()
    {
        if (this.tabs == null)
        {
            this.tabs = buildTabs();
        }

        return this.tabs;
    }

    @Override
    public void save()
    {
        new ZoneFocusConfigs().save();

        // Значения ведём мы, но у Material HUD те же опции: пусть его файл не отстаёт, иначе
        // после снятия этого мода там всплывут настройки, которых человек уже не помнит
        HUD_SHARED.push(ZoneFocusConfigs.OPTIONS);
        HUD_SHARED.saveSibling();
    }

    @Override
    public void resetAll()
    {
        ZoneFocusDefaults.resetEverything();
    }

    @Override
    public @Nullable Model.Status status()
    {
        if (! ZoneFocusConfigs.ENABLED.getBooleanValue())
        {
            return new Model.Status(text("status.disabled"), Ui.C_WARN,
                    "zonefocus.gui.tab_counting", "zoneFocus");
        }

        if (! hasEnabledPlacement())
        {
            return new Model.Status(Strings.t("status.no_schematic"), Ui.C_WARN, null, null);
        }

        if (! MaterialHudCompat.ownsVerification()
                && ZoneFocusConfigs.VERIFY_ENABLED.getBooleanValue() && noVerifyCategory())
        {
            return new Model.Status(text("warn.no_verify_category"), Ui.C_WARN,
                    "zonefocus.gui.tab_verification", "verifyMissing");
        }

        if (neighbourPresent() && ! MaterialHudCompat.isIntegrated())
        {
            return new Model.Status(text("status.api_mismatch"), Ui.C_FOREIGN,
                    "zonefocus.gui.tab_counting", "countByZone");
        }

        return null;
    }

    /** Акцент раздела стиля — тёплый золотой, как в Material HUD (22.10.2026). */
    private static final int UI_STYLE_ACCENT = 0xFFE0A030;

    private static List<Model.Tab> buildTabs()
    {
        List<Model.Tab> tabs = new ArrayList<>();

        tabs.add(new Model.Tab("zonefocus.gui.tab_counting").add(
                new Model.Section("zonefocus.section.module")
                        .note("zonefocus.section.module.note").add(
                        Model.Option.toggle("zoneFocus", Values.of(ZoneFocusConfigs.ENABLED)),
                        Model.Option.toggle("chatMessages", Values.of(ZoneFocusConfigs.CHAT_MESSAGES))),
                // Стиль окон: отдельный золотой акцент, как в Material HUD
                new Model.Section("zonefocus.section.ui_style")
                        .note("zonefocus.section.ui_style.note")
                        .accent(UI_STYLE_ACCENT)
                        .add(Model.Option.choice("uiStyle", Values.choice(ZoneFocusConfigs.UI_STYLE,
                                com.aell.zonefocus.shaded.ui.Style.LEVEL_KEYS))),
                new Model.Section("zonefocus.section.what_to_count").add(
                        Model.Option.toggle("countByZone", Values.of(ZoneFocusConfigs.COUNT_BY_ZONE))
                                .gate(ZoneFocusSettings::neighbourGate),
                        Model.Option.toggle("ignoreLayersInZone",
                                Values.of(ZoneFocusConfigs.IGNORE_LAYERS_IN_ZONE)),
                        Model.Option.toggle("searchWholeZone", Values.of(ZoneFocusConfigs.SEARCH_WHOLE_ZONE))
                                .gate(ZoneFocusSettings::neighbourGate)),
                new Model.Section("zonefocus.section.performance").add(
                        Model.Option.integer("scanBlocksPerTick",
                                        Values.of(ZoneFocusConfigs.SCAN_BLOCKS_PER_TICK), Num.Scale.LOG)
                                .foreignTip(ZoneFocusSettings::budgetTip))));

        tabs.add(new Model.Tab("zonefocus.gui.tab_zones").add(
                new Model.Section("zonefocus.section.auto_switch").add(
                        Model.Option.toggle("autoZoneByPosition",
                                Values.of(ZoneFocusConfigs.AUTO_ZONE_BY_POSITION)),
                        Model.Option.toggle("autoNextWhenDone",
                                Values.of(ZoneFocusConfigs.AUTO_NEXT_WHEN_DONE))),
                new Model.Section("zonefocus.section.sections").add(
                        Model.Option.toggle("hideOtherSections",
                                Values.of(ZoneFocusConfigs.HIDE_OTHER_SECTIONS)),
                        Model.Option.toggle("hideOutsideSchematics",
                                Values.of(ZoneFocusConfigs.HIDE_OUTSIDE_SCHEMATICS)),
                        Model.Option.toggle("cycleAcrossSections",
                                Values.of(ZoneFocusConfigs.CYCLE_ACROSS_SECTIONS)))));

        tabs.add(new Model.Tab("zonefocus.gui.tab_rendering").add(
                new Model.Section("zonefocus.section.schematic").add(
                        Model.Option.toggle("clipSchematicRender",
                                Values.of(ZoneFocusConfigs.CLIP_SCHEMATIC_RENDER))),
                new Model.Section("zonefocus.section.zone_boxes").add(
                        Model.Option.toggle("showActiveZone", Values.of(ZoneFocusConfigs.SHOW_ACTIVE_ZONE))
                                .swatch(ZoneRenderer::activeColor),
                        Model.Option.toggle("showAllZones", Values.of(ZoneFocusConfigs.SHOW_ALL_ZONES))
                                .swatch(ZoneRenderer::doneColor),
                        Model.Option.toggle("showOtherSections", Values.of(ZoneFocusConfigs.SHOW_OTHER_SECTIONS))
                                .swatch(ZoneRenderer::otherSectionColor),
                        Model.Option.toggle("showZoneLabels", Values.of(ZoneFocusConfigs.SHOW_ZONE_LABELS))
                                .child().gate(ZoneFocusSettings::boxesGate),
                        Model.Option.toggle("zoneSeeThrough", Values.of(ZoneFocusConfigs.ZONE_SEE_THROUGH))
                                .child().gate(ZoneFocusSettings::boxesGate),
                        Model.Option.integer("zoneDrawRange", Values.of(ZoneFocusConfigs.ZONE_DRAW_RANGE),
                                        Num.Scale.LINEAR)
                                .child().gate(ZoneFocusSettings::boxesGate)
                                .preview(new RangePreview(ZoneFocusConfigs.ZONE_DRAW_RANGE.getMaxIntegerValue())))));

        tabs.add(new Model.Tab("zonefocus.gui.tab_marking").add(
                new Model.Section("zonefocus.section.tool").add(
                        Model.Option.toggle("toolEnabled", Values.of(ZoneFocusConfigs.TOOL_ENABLED)),
                        Model.Option.integer("toolReach", Values.of(ZoneFocusConfigs.TOOL_REACH),
                                        Num.Scale.LINEAR)
                                .child().gate(ZoneFocusSettings::toolGate),
                        Model.Option.toggle("toolClampToSchematic",
                                        Values.of(ZoneFocusConfigs.TOOL_CLAMP_TO_SCHEMATIC))
                                .child().gate(ZoneFocusSettings::toolGate),
                        // Кнопка и подсказки инструмента — под тем же заголовком «Инструмент»
                        Model.Option.toggle("toolInventoryButton",
                                        Values.of(ZoneFocusConfigs.TOOL_INVENTORY_BUTTON))
                                .child().gate(ZoneFocusSettings::toolGate),
                        Model.Option.toggle("toolKeyHints", Values.of(ZoneFocusConfigs.TOOL_KEY_HINTS))
                                .child().gate(ZoneFocusSettings::toolGate))));

        // С Material HUD проверку ведёт он (Ctrl+B, своя вкладка «Проверка») — второй не показываем
        if (! MaterialHudCompat.ownsVerification())
        {
        tabs.add(new Model.Tab("zonefocus.gui.tab_verification").add(
                new Model.Section("zonefocus.section.verify_mode").add(
                        Model.Option.toggle("verifyZone", Values.of(ZoneFocusConfigs.VERIFY_ENABLED))),
                new Model.Section("zonefocus.section.verify_what")
                        .warning(() -> ZoneFocusConfigs.VERIFY_ENABLED.getBooleanValue() && noVerifyCategory()
                                ? text("warn.no_verify_category") : null)
                        .add(
                        Model.Option.toggle("verifyMissing", Values.of(ZoneFocusConfigs.VERIFY_MISSING))
                                .child().gate(ZoneFocusSettings::verifyGate)
                                .swatch(ZoneVerifier.Category.MISSING::color),
                        Model.Option.toggle("verifyWrongBlocks", Values.of(ZoneFocusConfigs.VERIFY_WRONG_BLOCK))
                                .child().gate(ZoneFocusSettings::verifyGate)
                                .swatch(ZoneVerifier.Category.WRONG_BLOCK::color),
                        Model.Option.toggle("verifyWrongStates", Values.of(ZoneFocusConfigs.VERIFY_WRONG_STATE))
                                .child().gate(ZoneFocusSettings::verifyGate)
                                .swatch(ZoneVerifier.Category.WRONG_STATE::color),
                        Model.Option.toggle("verifyExtraBlocks", Values.of(ZoneFocusConfigs.VERIFY_EXTRA))
                                .child().gate(ZoneFocusSettings::verifyGate)
                                .swatch(ZoneVerifier.Category.EXTRA::color)),
                new Model.Section("zonefocus.section.verify_look").add(
                        Model.Option.toggle("verifySeeThrough", Values.of(ZoneFocusConfigs.VERIFY_SEE_THROUGH))
                                .child().gate(ZoneFocusSettings::verifyGate),
                        Model.Option.toggle("verifyFillBoxes", Values.of(ZoneFocusConfigs.VERIFY_FILL))
                                .child().gate(ZoneFocusSettings::verifyGate),
                        Model.Option.integer("verifyDrawRange", Values.of(ZoneFocusConfigs.VERIFY_DRAW_RANGE),
                                        Num.Scale.LINEAR)
                                .child().gate(ZoneFocusSettings::verifyGate)
                                .preview(new RangePreview(ZoneFocusConfigs.VERIFY_DRAW_RANGE.getMaxIntegerValue())),
                        Model.Option.integer("verifyMaxMarkers", Values.of(ZoneFocusConfigs.VERIFY_MAX_MARKERS),
                                        Num.Scale.LOG)
                                .child().gate(ZoneFocusSettings::verifyGate))));
        }

        tabs.add(new Model.Tab("zonefocus.gui.tab_hotkeys").add(
                new Model.Section("zonefocus.section.screens").note("zonefocus.section.screens.note").add(
                        key("openSettings", ZoneFocusConfigs.OPEN_SETTINGS)
                                .swatch(() -> Theme.BLUE),
                        key("openZoneList", ZoneFocusConfigs.OPEN_ZONE_LIST)
                                .swatch(() -> Theme.BLUE)),
                new Model.Section("zonefocus.section.actions").add(
                        key("toggleTool", ZoneFocusConfigs.TOGGLE_TOOL)
                                .swatch(() -> Theme.GREEN)
                                .gate(ZoneFocusSettings::toolGate),
                        key("nextZone", ZoneFocusConfigs.NEXT_ZONE)
                                .swatch(ZoneRenderer::activeColor),
                        key("prevZone", ZoneFocusConfigs.PREV_ZONE)
                                .swatch(ZoneRenderer::activeColor),
                        key("nextSection", ZoneFocusConfigs.NEXT_SECTION)
                                .swatch(ZoneRenderer::otherSectionColor),
                        key("prevSection", ZoneFocusConfigs.PREV_SECTION)
                                .swatch(ZoneRenderer::otherSectionColor),
                        MaterialHudCompat.ownsVerification()
                                ? key("verifyZoneKeyHud", ZoneFocusConfigs.VERIFY_ZONE)
                                        .swatch(() -> Theme.YELLOW)
                                : key("verifyZoneKey", ZoneFocusConfigs.VERIFY_ZONE)
                                        .swatch(() -> Theme.YELLOW)
                                        .gate(ZoneFocusSettings::verifyGate))));

        return tabs;
    }

    private static Model.Option key(String name, fi.dy.masa.malilib.config.options.ConfigHotkey config)
    {
        return Model.Option.keybind(name, Values.of(config), Values.keybind(config));
    }

    private static @Nullable Model.Reason neighbourGate()
    {
        return MaterialHudCompat.isIntegrated() ? null
                : new Model.Reason(text("gate.needs_material_hud"), true);
    }

    private static @Nullable Model.Reason verifyGate()
    {
        return ZoneFocusConfigs.VERIFY_ENABLED.getBooleanValue() ? null
                : new Model.Reason(text("gate.verify_off"), false);
    }

    private static @Nullable Model.Reason toolGate()
    {
        return ZoneFocusConfigs.TOOL_ENABLED.getBooleanValue() ? null
                : new Model.Reason(text("gate.tool_off"), false);
    }

    private static @Nullable Model.Reason boxesGate()
    {
        return ZoneFocusConfigs.SHOW_ACTIVE_ZONE.getBooleanValue()
                || ZoneFocusConfigs.SHOW_ALL_ZONES.getBooleanValue()
                ? null : new Model.Reason(text("gate.no_boxes"), false);
    }

    private static List<String> budgetTip()
    {
        return neighbourPresent() ? List.of(Strings.t("tip.budgets_add_up")) : List.of();
    }

    private static boolean neighbourPresent()
    {
        return FabricLoader.getInstance().isModLoaded(NEIGHBOUR_MOD_ID);
    }

    private static boolean noVerifyCategory()
    {
        return ! ZoneFocusConfigs.VERIFY_MISSING.getBooleanValue()
                && ! ZoneFocusConfigs.VERIFY_WRONG_BLOCK.getBooleanValue()
                && ! ZoneFocusConfigs.VERIFY_WRONG_STATE.getBooleanValue()
                && ! ZoneFocusConfigs.VERIFY_EXTRA.getBooleanValue();
    }

    private static boolean hasEnabledPlacement()
    {
        try
        {
            for (SchematicPlacement placement :
                    DataManager.getSchematicPlacementManager().getAllSchematicsPlacements())
            {
                if (placement.isEnabled())
                {
                    return true;
                }
            }

            return false;
        }
        catch (RuntimeException | LinkageError e)
        {
            return true;
        }
    }

    private static String text(String suffix)
    {
        return Strings.plain(MOD_ID + ".ui." + suffix);
    }

    private record RangePreview(int maxRange) implements Model.Preview
    {
        private static final int WIDTH = 24;
        private static final int HEIGHT = 4;

        @Override
        public int width()
        {
            return WIDTH;
        }

        @Override
        public void draw(fi.dy.masa.malilib.render.GuiContext ctx, int x, int rowTop, Model.Option option)
        {
            if (option.number == null)
            {
                return;
            }

            int y = rowTop + (Ui.ROW_H - HEIGHT) / 2;
            int fill = Math.round(WIDTH * option.number.get() / (float) this.maxRange);

            com.aell.zonefocus.shaded.ui.Paint.rect(ctx, x, y, WIDTH, HEIGHT, Ui.C_TRACK);
            com.aell.zonefocus.shaded.ui.Paint.rect(ctx, x, y, Math.max(1, fill), HEIGHT, Ui.alpha(Ui.C_ACCENT, Ui.A_PREVIEW));
        }
    }
}
