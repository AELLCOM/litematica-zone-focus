package com.aell.zonefocus.zone.model;

import com.aell.zonefocus.zone.ZoneBox;
import com.google.gson.JsonObject;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * Ссылка участка на размещение Litematica. Основной ключ — {@code id} ({@code SchematicPlacement.getHashId()},
 * Litematica сама пишет его в json размещений), имя и файл — запасные; {@code box} — габарит размещения на момент
 * последнего удачного сопоставления, по нему зоны отсутствующей схемы остаются в нумерации.
 */
public final class PlacementRef
{
    public @Nullable UUID id;
    public String name;
    public @Nullable String file;
    public @Nullable ZoneBox box;

    public PlacementRef(@Nullable UUID id, String name, @Nullable String file, @Nullable ZoneBox box)
    {
        this.id = id;
        this.name = name == null ? "" : name;
        this.file = file;
        this.box = box;
    }

    public static PlacementRef byName(String name)
    {
        return new PlacementRef(null, name, null, null);
    }

    public PlacementRef copy()
    {
        return new PlacementRef(this.id, this.name, this.file, this.box);
    }

    /** Ссылка без единого ключа — ручная зона, созданная без схемы. */
    public boolean isBlank()
    {
        return this.id == null && this.name.isEmpty() && this.file == null;
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        if (this.id != null)
        {
            obj.addProperty("id", this.id.toString());
        }

        obj.addProperty("name", this.name);

        if (this.file != null)
        {
            obj.addProperty("file", this.file);
        }

        Json.putBox(obj, "box", this.box);

        return obj;
    }

    public static PlacementRef fromJson(JsonObject obj)
    {
        UUID id = null;
        String raw = Json.str(obj, "id", "");

        if (! raw.isEmpty())
        {
            try
            {
                id = UUID.fromString(raw);
            }
            catch (IllegalArgumentException ignored) {}
        }

        String file = Json.str(obj, "file", "");

        return new PlacementRef(id, Json.str(obj, "name", ""), file.isEmpty() ? null : file, Json.box(obj, "box"));
    }

    @Override
    public String toString()
    {
        return this.name + (this.id != null ? "#" + this.id.toString().substring(0, 8) : "");
    }
}
