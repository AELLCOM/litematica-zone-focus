package com.aell.zonefocus.zone.model;

import com.aell.zonefocus.zone.ZoneBox;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/** Чтение json модели без malilib: отсутствующее или битое поле — значение по умолчанию, не исключение. */
final class Json
{
    private Json() {}

    static String str(JsonObject obj, String key, String fallback)
    {
        JsonElement e = obj.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : fallback;
    }

    static int integer(JsonObject obj, String key, int fallback)
    {
        JsonElement e = obj.get(key);

        if (e != null && e.isJsonPrimitive() && ((JsonPrimitive) e).isNumber())
        {
            try
            {
                return e.getAsInt();
            }
            catch (NumberFormatException ignored) {}
        }

        return fallback;
    }

    static boolean bool(JsonObject obj, String key, boolean fallback)
    {
        JsonElement e = obj.get(key);
        return e != null && e.isJsonPrimitive() && ((JsonPrimitive) e).isBoolean() ? e.getAsBoolean() : fallback;
    }

    static @Nullable JsonObject object(JsonObject obj, String key)
    {
        JsonElement e = obj.get(key);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
    }

    static List<JsonObject> objects(JsonObject obj, String key)
    {
        List<JsonObject> out = new ArrayList<>();
        JsonElement e = obj.get(key);

        if (e != null && e.isJsonArray())
        {
            for (JsonElement item : e.getAsJsonArray())
            {
                if (item.isJsonObject())
                {
                    out.add(item.getAsJsonObject());
                }
            }
        }

        return out;
    }

    static JsonArray array(List<JsonObject> items)
    {
        JsonArray out = new JsonArray();
        items.forEach(out::add);
        return out;
    }

    /** "x,y,z" ↔ BlockPos. */
    static @Nullable BlockPos pos(JsonObject obj, String key)
    {
        return parsePos(str(obj, key, ""));
    }

    static @Nullable BlockPos parsePos(String value)
    {
        String[] parts = value.split(",");

        if (parts.length != 3)
        {
            return null;
        }

        try
        {
            return new BlockPos(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim()));
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    static void putPos(JsonObject obj, String key, @Nullable BlockPos pos)
    {
        if (pos != null)
        {
            obj.addProperty(key, pos.getX() + "," + pos.getY() + "," + pos.getZ());
        }
    }

    /** "x1,y1,z1/x2,y2,z2" ↔ ZoneBox (тот же вид, что {@link ZoneBox#signature}). */
    static @Nullable ZoneBox box(JsonObject obj, String key)
    {
        return parseBox(str(obj, key, ""));
    }

    static @Nullable ZoneBox parseBox(String value)
    {
        String[] halves = value.split("/");

        if (halves.length != 2)
        {
            return null;
        }

        BlockPos a = parsePos(halves[0]);
        BlockPos b = parsePos(halves[1]);
        return a != null && b != null ? new ZoneBox(a, b) : null;
    }

    static void putBox(JsonObject obj, String key, @Nullable ZoneBox box)
    {
        if (box != null)
        {
            obj.addProperty(key, box.signature());
        }
    }

    /** "1,4,7" ↔ список неотрицательных чисел. */
    static List<Integer> ints(String value)
    {
        List<Integer> out = new ArrayList<>();

        for (String part : value.split(","))
        {
            String trimmed = part.trim();

            if (trimmed.isEmpty())
            {
                continue;
            }

            try
            {
                int n = Integer.parseInt(trimmed);

                if (n >= 0)
                {
                    out.add(n);
                }
            }
            catch (NumberFormatException ignored) {}
        }

        return out;
    }

    static String ints(Iterable<Integer> values)
    {
        StringBuilder builder = new StringBuilder();

        for (int v : values)
        {
            if (builder.length() > 0) { builder.append(','); }

            builder.append(v);
        }

        return builder.toString();
    }
}
