package com.aell.zonefocus.config;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import fi.dy.masa.malilib.util.data.json.JsonUtils;
import java.io.File;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

@Environment(EnvType.CLIENT)
public class ZoneFocusConfigs implements IConfigHandler
{
    private static final String CONFIG_FILE_NAME = "zonefocus.json";

    /** Конфиг Material HUD: оттуда при первом запуске переезжают настройки встроенного модуля зон. */
    private static final String DONOR_FILE_NAME = "layerlist.json";

    /** 5 — участки (0.1.0): новые опции с умолчаниями, ничего не сбрасывается. */
    private static final int SETTINGS_REVISION = 5;

    /** Общий выключатель мода; имя дословно как у встроенного модуля Material HUD. */
    public static final ConfigBoolean ENABLED            = new ConfigBoolean("zoneFocus", true);

    public static final ConfigBoolean COUNT_BY_ZONE      = new ConfigBoolean("countByZone", true);
    /** Оформление окон (22.10.2026): 0 — Aell UI (по умолчанию; рядом с Material HUD — его оформление), 1 — Vanilla, 2 — Vanilla+. */
    public static final ConfigInteger UI_STYLE           = new ConfigInteger("uiStyle", 0, 0, 2);
    public static final ConfigBoolean SEARCH_WHOLE_ZONE  = new ConfigBoolean("searchWholeZone", false);
    public static final ConfigBoolean IGNORE_LAYERS_IN_ZONE = new ConfigBoolean("ignoreLayersInZone", false);
    public static final ConfigBoolean CLIP_SCHEMATIC_RENDER = new ConfigBoolean("clipSchematicRender", true);

    public static final ConfigBoolean AUTO_ZONE_BY_POSITION = new ConfigBoolean("autoZoneByPosition", false);
    public static final ConfigBoolean AUTO_NEXT_WHEN_DONE   = new ConfigBoolean("autoNextWhenDone", false);

    /** Участки (0.1.0): схемы других участков и схемы вне раскладки — скрывать или показывать целиком. */
    public static final ConfigBoolean HIDE_OTHER_SECTIONS     = new ConfigBoolean("hideOtherSections", true);
    public static final ConfigBoolean HIDE_OUTSIDE_SCHEMATICS = new ConfigBoolean("hideOutsideSchematics", false);
    public static final ConfigBoolean CYCLE_ACROSS_SECTIONS   = new ConfigBoolean("cycleAcrossSections", false);
    public static final ConfigBoolean SHOW_OTHER_SECTIONS     = new ConfigBoolean("showOtherSections", false);

    public static final ConfigBoolean SHOW_ACTIVE_ZONE = new ConfigBoolean("showActiveZone", true);
    public static final ConfigBoolean SHOW_ALL_ZONES   = new ConfigBoolean("showAllZones", false);
    public static final ConfigBoolean SHOW_ZONE_LABELS = new ConfigBoolean("showZoneLabels", true);
    public static final ConfigBoolean ZONE_SEE_THROUGH = new ConfigBoolean("zoneSeeThrough", true);
    public static final ConfigInteger ZONE_DRAW_RANGE  = new ConfigInteger("zoneDrawRange", 96, 16, 512);

    public static final ConfigBoolean TOOL_ENABLED     = new ConfigBoolean("toolEnabled", true);
    public static final ConfigBoolean TOOL_INVENTORY_BUTTON = new ConfigBoolean("toolInventoryButton", true);
    public static final ConfigBoolean TOOL_KEY_HINTS   = new ConfigBoolean("toolKeyHints", true);
    public static final ConfigBoolean TOOL_CLAMP_TO_SCHEMATIC = new ConfigBoolean("toolClampToSchematic", true);
    public static final ConfigInteger TOOL_REACH           = new ConfigInteger("toolReach", 128, 8, 512);

    /** Писать в чат (выкл. по умолчанию с 0.0.9): иначе отклик на действия — строкой над хотбаром. */
    public static final ConfigBoolean CHAT_MESSAGES        = new ConfigBoolean("chatMessages", false);

    public static final ConfigInteger SCAN_BLOCKS_PER_TICK = new ConfigInteger("scanBlocksPerTick", 250000, 10000, 4000000);

    public static final ConfigBoolean VERIFY_ENABLED     = new ConfigBoolean("verifyZone", true);
    public static final ConfigBoolean VERIFY_MISSING     = new ConfigBoolean("verifyMissing", true);
    public static final ConfigBoolean VERIFY_WRONG_BLOCK = new ConfigBoolean("verifyWrongBlocks", true);
    public static final ConfigBoolean VERIFY_WRONG_STATE = new ConfigBoolean("verifyWrongStates", true);
    public static final ConfigBoolean VERIFY_EXTRA       = new ConfigBoolean("verifyExtraBlocks", true);
    public static final ConfigBoolean VERIFY_SEE_THROUGH = new ConfigBoolean("verifySeeThrough", true);
    public static final ConfigBoolean VERIFY_FILL        = new ConfigBoolean("verifyFillBoxes", false);
    public static final ConfigInteger VERIFY_DRAW_RANGE  = new ConfigInteger("verifyDrawRange", 64, 8, 256);
    public static final ConfigInteger VERIFY_MAX_MARKERS = new ConfigInteger("verifyMaxMarkers", 2000, 50, 20000);

    private static final KeybindSettings EMPTY_OK =
            KeybindSettings.create(KeybindSettings.Context.INGAME, fi.dy.masa.malilib.hotkeys.KeyAction.PRESS,
                    false, true, false, true);

    public static final ConfigHotkey OPEN_SETTINGS = new ConfigHotkey("openSettings", "LEFT_CONTROL,J", EMPTY_OK);
    public static final ConfigHotkey OPEN_ZONE_LIST = new ConfigHotkey("openZoneList", "", EMPTY_OK);
    public static final ConfigHotkey NEXT_ZONE   = new ConfigHotkey("nextZone", "", EMPTY_OK);
    public static final ConfigHotkey PREV_ZONE   = new ConfigHotkey("prevZone", "", EMPTY_OK);
    public static final ConfigHotkey NEXT_SECTION = new ConfigHotkey("nextSection", "", EMPTY_OK);
    public static final ConfigHotkey PREV_SECTION = new ConfigHotkey("prevSection", "", EMPTY_OK);

    public static final ConfigHotkey TOGGLE_TOOL = new ConfigHotkey("toggleTool", "", EMPTY_OK);

    public static final ConfigHotkey VERIFY_ZONE = new ConfigHotkey("verifyZoneKey", "LEFT_CONTROL,P", EMPTY_OK);

    public static final List<IConfigBase> OPTIONS = ImmutableList.of(
            ENABLED, UI_STYLE, COUNT_BY_ZONE, CLIP_SCHEMATIC_RENDER, SEARCH_WHOLE_ZONE, IGNORE_LAYERS_IN_ZONE,
            AUTO_ZONE_BY_POSITION, AUTO_NEXT_WHEN_DONE,
            HIDE_OTHER_SECTIONS, HIDE_OUTSIDE_SCHEMATICS, CYCLE_ACROSS_SECTIONS, SHOW_OTHER_SECTIONS,
            SHOW_ACTIVE_ZONE, SHOW_ALL_ZONES, SHOW_ZONE_LABELS, ZONE_SEE_THROUGH, ZONE_DRAW_RANGE,
            TOOL_ENABLED, TOOL_INVENTORY_BUTTON, TOOL_KEY_HINTS, TOOL_CLAMP_TO_SCHEMATIC, TOOL_REACH,
            SCAN_BLOCKS_PER_TICK, CHAT_MESSAGES,
            VERIFY_ENABLED, VERIFY_MISSING, VERIFY_WRONG_BLOCK, VERIFY_WRONG_STATE, VERIFY_EXTRA,
            VERIFY_SEE_THROUGH, VERIFY_FILL, VERIFY_DRAW_RANGE, VERIFY_MAX_MARKERS);

    public static final List<ConfigHotkey> HOTKEYS = ImmutableList.of(
            OPEN_SETTINGS, OPEN_ZONE_LIST,
            NEXT_ZONE, PREV_ZONE, NEXT_SECTION, PREV_SECTION, TOGGLE_TOOL, VERIFY_ZONE);

    public static int SPLIT_X = ZoneFocusDefaults.SPLIT_X;
    public static int SPLIT_Y = ZoneFocusDefaults.SPLIT_Y;
    public static int SPLIT_Z = ZoneFocusDefaults.SPLIT_Z;
    public static boolean SPLIT_BY_SIZE = ZoneFocusDefaults.SPLIT_BY_SIZE;
    public static int SPLIT_SIZE_X = ZoneFocusDefaults.SPLIT_SIZE_X;
    public static int SPLIT_SIZE_Y = ZoneFocusDefaults.SPLIT_SIZE_Y;
    public static int SPLIT_SIZE_Z = ZoneFocusDefaults.SPLIT_SIZE_Z;

    @Override
    public void load()
    {
        File configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), CONFIG_FILE_NAME);

        if (! configFile.exists() || ! configFile.isFile() || ! configFile.canRead())
        {
            adoptMaterialHudSettings();
            return;
        }

        JsonElement element = JsonUtils.parseJsonFile(configFile.toPath());

        if (element != null && element.isJsonObject())
        {
            JsonObject root = element.getAsJsonObject();
            ConfigUtils.readConfigBase(root, "Options", OPTIONS);
            migrateUiStyle(root);
            ConfigUtils.readConfigBase(root, "Hotkeys", HOTKEYS);

            SPLIT_X = JsonUtils.getIntegerOrDefault(root, "split_x", ZoneFocusDefaults.SPLIT_X);
            SPLIT_Y = JsonUtils.getIntegerOrDefault(root, "split_y", ZoneFocusDefaults.SPLIT_Y);
            SPLIT_Z = JsonUtils.getIntegerOrDefault(root, "split_z", ZoneFocusDefaults.SPLIT_Z);
            SPLIT_BY_SIZE = JsonUtils.getBooleanOrDefault(root, "split_by_size", ZoneFocusDefaults.SPLIT_BY_SIZE);
            SPLIT_SIZE_X = JsonUtils.getIntegerOrDefault(root, "split_size_x", ZoneFocusDefaults.SPLIT_SIZE_X);
            SPLIT_SIZE_Y = JsonUtils.getIntegerOrDefault(root, "split_size_y", ZoneFocusDefaults.SPLIT_SIZE_Y);
            SPLIT_SIZE_Z = JsonUtils.getIntegerOrDefault(root, "split_size_z", ZoneFocusDefaults.SPLIT_SIZE_Z);

            int revision = JsonUtils.getIntegerOrDefault(root, "settings_revision", 1);

            if (revision < 2)
            {
                SEARCH_WHOLE_ZONE.resetToDefault();
                TOGGLE_TOOL.resetToDefault();
                VERIFY_ZONE.resetToDefault();
            }

            if (revision < 3)
            {
                OPEN_ZONE_LIST.resetToDefault();
            }

            if (revision < 4)
            {
                resetAutoSwitch();
            }

            if (revision < SETTINGS_REVISION)
            {
                this.save();
            }
        }
    }

    /**
     * Первый запуск: настройки зон могли жить у встроенного модуля Litematica Material HUD —
     * под теми же именами, в отдельной секции {@code "ZoneFocus"} его файла. Молча забираем
     * их оттуда, иначе у человека, который выстроил зоны и подкрутил проверку под свой сервер,
     * всё разом откатится на умолчания. Читаем, но не пишем: Material HUD живёт своей жизнью.
     * Клавиши не переезжают — в семье модов привязки никогда не общие.
     */
    private static void adoptMaterialHudSettings()
    {
        File donor = new File(FabricLoader.getInstance().getConfigDir().toFile(), DONOR_FILE_NAME);

        if (! donor.exists() || ! donor.isFile() || ! donor.canRead())
        {
            return;
        }

        JsonElement element = JsonUtils.parseJsonFile(donor.toPath());

        if (element == null || ! element.isJsonObject())
        {
            return;
        }

        JsonObject root = element.getAsJsonObject();

        if (! root.has("ZoneFocus"))
        {
            return;
        }

        ConfigUtils.readConfigBase(root, "ZoneFocus", OPTIONS);
        resetAutoSwitch();

        SPLIT_X = JsonUtils.getIntegerOrDefault(root, "zf_split_x", ZoneFocusDefaults.SPLIT_X);
        SPLIT_Y = JsonUtils.getIntegerOrDefault(root, "zf_split_y", ZoneFocusDefaults.SPLIT_Y);
        SPLIT_Z = JsonUtils.getIntegerOrDefault(root, "zf_split_z", ZoneFocusDefaults.SPLIT_Z);
        SPLIT_BY_SIZE = JsonUtils.getBooleanOrDefault(root, "zf_split_by_size", ZoneFocusDefaults.SPLIT_BY_SIZE);
        SPLIT_SIZE_X = JsonUtils.getIntegerOrDefault(root, "zf_split_size_x", ZoneFocusDefaults.SPLIT_SIZE_X);
        SPLIT_SIZE_Y = JsonUtils.getIntegerOrDefault(root, "zf_split_size_y", ZoneFocusDefaults.SPLIT_SIZE_Y);
        SPLIT_SIZE_Z = JsonUtils.getIntegerOrDefault(root, "zf_split_size_z", ZoneFocusDefaults.SPLIT_SIZE_Z);
    }

    /**
     * Автопереключение зон — по умолчанию выключено (10.10.2026). Раньше «К следующей после достройки» была
     * включена по умолчанию, и malilib записывал это значение в файл: умолчание в коде одно не помогает —
     * при обновлении (и при переезде настроек встроенного модуля Material HUD) обе опции выключаются один раз.
     */
    private static void resetAutoSwitch()
    {
        AUTO_ZONE_BY_POSITION.resetToDefault();
        AUTO_NEXT_WHEN_DONE.resetToDefault();
    }

    @Override
    public void save()
    {
        File dir = FabricLoader.getInstance().getConfigDir().toFile();

        if ((dir.exists() && dir.isDirectory()) || dir.mkdirs())
        {
            JsonObject root = new JsonObject();
            ConfigUtils.writeConfigBase(root, "Options", OPTIONS);
            ConfigUtils.writeConfigBase(root, "Hotkeys", HOTKEYS);

            root.addProperty("settings_revision", SETTINGS_REVISION);
            root.addProperty("split_x", SPLIT_X);
            root.addProperty("split_y", SPLIT_Y);
            root.addProperty("split_z", SPLIT_Z);
            root.addProperty("split_by_size", SPLIT_BY_SIZE);
            root.addProperty("split_size_x", SPLIT_SIZE_X);
            root.addProperty("split_size_y", SPLIT_SIZE_Y);
            root.addProperty("split_size_z", SPLIT_SIZE_Z);

            JsonUtils.writeJsonToFile(root, new File(dir, CONFIG_FILE_NAME).toPath());
        }
    }

    /** До 22.10.2026 стиль был галочкой {@code uiVanilla}: включённая становится «Vanilla». */
    private static void migrateUiStyle(JsonObject root)
    {
        JsonObject options = root.has("Options") && root.get("Options").isJsonObject() ? root.getAsJsonObject("Options") : null;

        if (options != null && ! options.has("uiStyle") && options.has("uiVanilla")
                && options.get("uiVanilla").isJsonPrimitive() && options.get("uiVanilla").getAsBoolean())
        {
            UI_STYLE.setIntegerValue(1);
        }
    }
}
