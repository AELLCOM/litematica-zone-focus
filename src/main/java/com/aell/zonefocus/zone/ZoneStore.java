package com.aell.zonefocus.zone;

import com.aell.zonefocus.zone.model.Layout;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.util.data.json.JsonUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

/**
 * Раскладки по мирам в {@code config/zonefocus_zones.json}: {@code worlds → scopeKey → запись мира}. Старая
 * запись (без {@code version}) читается как один участок; перед первой перезаписью такого файла делается копия
 * {@code zonefocus_zones.json.bak-0.0.9} — откат на 0.0.9 участков не видит.
 */
@Environment(EnvType.CLIENT)
public final class ZoneStore
{
    private static final String FILE_NAME = "zonefocus_zones.json";
    private static final String BACKUP_NAME = FILE_NAME + ".bak-0.0.9";

    private ZoneStore() {}

    public static String scopeKey(Minecraft mc)
    {
        if (mc.level == null)
        {
            return "";
        }

        String server = mc.getCurrentServer() != null ? mc.getCurrentServer().ip
                : (mc.getSingleplayerServer() != null
                        ? "sp:" + mc.getSingleplayerServer().getWorldData().getLevelName()
                        : "local");

        return server + "|" + mc.level.dimension().identifier();
    }

    public static Layout load(String scopeKey)
    {
        if (scopeKey.isEmpty())
        {
            return Layout.create();
        }

        JsonObject root = readRoot();

        if (root != null && JsonUtils.hasObject(root, "worlds"))
        {
            JsonObject worlds = root.getAsJsonObject("worlds");

            if (JsonUtils.hasObject(worlds, scopeKey))
            {
                return Layout.fromWorldJson(worlds.getAsJsonObject(scopeKey));
            }
        }

        return Layout.create();
    }

    public static void save(Layout layout, String scopeKey)
    {
        if (scopeKey.isEmpty())
        {
            return;
        }

        JsonObject root = readRoot();

        if (root == null)
        {
            root = new JsonObject();
        }

        JsonObject worlds = JsonUtils.hasObject(root, "worlds")
                ? root.getAsJsonObject("worlds")
                : new JsonObject();

        backupLegacyOnce(worlds);

        if (! layout.hasSections())
        {
            worlds.remove(scopeKey);
        }
        else
        {
            worlds.add(scopeKey, layout.toWorldJson());
        }

        root.add("worlds", worlds);

        File dir = FabricLoader.getInstance().getConfigDir().toFile();

        if ((dir.exists() && dir.isDirectory()) || dir.mkdirs())
        {
            JsonUtils.writeJsonToFile(root, new File(dir, FILE_NAME).toPath());
        }
    }

    /** В файле ещё есть записи старого формата — один раз сохранить копию, пока не перезаписали. */
    private static void backupLegacyOnce(JsonObject worlds)
    {
        boolean legacy = false;

        for (var entry : worlds.entrySet())
        {
            if (entry.getValue().isJsonObject() && Layout.isLegacy(entry.getValue().getAsJsonObject()))
            {
                legacy = true;
                break;
            }
        }

        if (! legacy)
        {
            return;
        }

        File dir = FabricLoader.getInstance().getConfigDir().toFile();
        File source = new File(dir, FILE_NAME);
        File backup = new File(dir, BACKUP_NAME);

        if (source.isFile() && ! backup.exists())
        {
            try
            {
                Files.copy(source.toPath(), backup.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
            }
            catch (IOException ignored)
            {
                // Копия не вышла — всё равно пишем: данные в новом формате важнее
            }
        }
    }

    private static JsonObject readRoot()
    {
        File file = new File(FabricLoader.getInstance().getConfigDir().toFile(), FILE_NAME);

        if (file.exists() && file.isFile() && file.canRead())
        {
            JsonElement element = JsonUtils.parseJsonFile(file.toPath());

            if (element != null && element.isJsonObject())
            {
                return element.getAsJsonObject();
            }
        }

        return null;
    }
}
