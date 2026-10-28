package com.aell.zonefocus.zone.model;

import com.aell.zonefocus.zone.ZoneBox;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Раскладка мира: список участков и активный участок. В 0.1.0 раскладка на мир одна; формат файла уже хранит
 * список ({@link #toWorldJson}), чтобы потом добавить переключение без миграции.
 */
public final class Layout
{
    public static final int FORMAT_VERSION = 2;

    public final String id;
    public String name = "";
    public final List<Section> sections = new ArrayList<>();
    public int activeSection;
    /** Счётчик изменений в памяти — для кэшей экранов; сигнатуры по нему не кэшируются (§2.3). */
    public int version;

    public Layout(String id)
    {
        this.id = id;
    }

    public static Layout create()
    {
        return new Layout(UUID.randomUUID().toString());
    }

    public boolean hasSections()
    {
        return ! this.sections.isEmpty();
    }

    public @Nullable Section active()
    {
        if (this.sections.isEmpty())
        {
            return null;
        }

        this.activeSection = Math.floorMod(this.activeSection, this.sections.size());
        return this.sections.get(this.activeSection);
    }

    public int indexOf(Section section)
    {
        for (int i = 0; i < this.sections.size(); ++i)
        {
            if (this.sections.get(i) == section)
            {
                return i;
            }
        }

        return -1;
    }

    public @Nullable Section byId(String id)
    {
        for (Section section : this.sections)
        {
            if (section.id.equals(id))
            {
                return section;
            }
        }

        return null;
    }

    public void touch()
    {
        ++this.version;
    }

    public void clear()
    {
        this.sections.clear();
        this.activeSection = 0;
        this.touch();
    }

    // ===== JSON =====

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", this.id);
        obj.addProperty("name", this.name);
        obj.addProperty("active_section", this.activeSection);
        List<JsonObject> list = new ArrayList<>();
        this.sections.forEach(s -> list.add(s.toJson()));
        obj.add("sections", Json.array(list));
        return obj;
    }

    public static Layout fromJson(JsonObject obj)
    {
        String id = Json.str(obj, "id", "");
        Layout layout = new Layout(id.isEmpty() ? UUID.randomUUID().toString() : id);
        layout.name = Json.str(obj, "name", "");
        layout.activeSection = Math.max(0, Json.integer(obj, "active_section", 0));

        for (JsonObject item : Json.objects(obj, "sections"))
        {
            Section section = Section.fromJson(item);

            if (section != null)
            {
                layout.sections.add(section);
            }
        }

        return layout;
    }

    /** Запись мира: {@code {version: 2, layouts: [...], active_layout: 0}}. */
    public JsonObject toWorldJson()
    {
        JsonObject obj = new JsonObject();
        obj.addProperty("version", FORMAT_VERSION);
        obj.add("layouts", Json.array(List.of(this.toJson())));
        obj.addProperty("active_layout", 0);
        return obj;
    }

    /**
     * Запись мира любого формата → раскладка. Без поля {@code version} — старая запись 0.0.x
     * ({@code kind, placement, nx, ny, nz, active, hidden, show_hidden, manual_*}), она становится одним участком.
     */
    public static Layout fromWorldJson(JsonObject obj)
    {
        if (! obj.has("version"))
        {
            return fromLegacy(obj);
        }

        List<JsonObject> layouts = Json.objects(obj, "layouts");

        if (layouts.isEmpty())
        {
            return create();
        }

        int index = Math.max(0, Math.min(layouts.size() - 1, Json.integer(obj, "active_layout", 0)));
        return fromJson(layouts.get(index));
    }

    public static boolean isLegacy(JsonObject obj)
    {
        return ! obj.has("version");
    }

    static Layout fromLegacy(JsonObject obj)
    {
        Layout layout = create();
        String kind = Json.str(obj, "kind", "NONE");
        String placement = Json.str(obj, "placement", "");
        Rule rule;
        List<PlacementRef> refs = new ArrayList<>();

        switch (kind)
        {
            case "GRID" ->
            {
                rule = new Rule.Grid(Json.integer(obj, "nx", 1), Json.integer(obj, "ny", 1), Json.integer(obj, "nz", 1));
                refs.add(PlacementRef.byName(placement));
            }
            case "SUB_REGIONS" ->
            {
                rule = Rule.SubRegions.INSTANCE;
                refs.add(PlacementRef.byName(placement));
            }
            case "MANUAL" ->
            {
                BlockPos min = Json.pos(obj, "manual_min");
                BlockPos max = Json.pos(obj, "manual_max");

                if (min == null || max == null)
                {
                    return layout;
                }

                // Пустое имя — старая getPlacement() брала единственное включённое размещение; ссылка без ключей
                // при сопоставлении делает то же (PlacementMatcher)
                PlacementRef base = PlacementRef.byName(placement);
                rule = new Rule.Manual(List.of(new ZoneBox(min, max)), base, Json.pos(obj, "manual_origin"));

                if (! placement.isEmpty())
                {
                    refs.add(base);
                }
            }
            default ->
            {
                return layout;
            }
        }

        Section section = Section.create(rule);
        section.placements.addAll(refs);
        section.activeZone = Math.max(0, Json.integer(obj, "active", 0));
        section.showHidden = Json.bool(obj, "show_hidden", false);
        section.hiddenZones.addAll(Json.ints(Json.str(obj, "hidden", "")));
        layout.sections.add(section);
        layout.activeSection = 0;

        return layout;
    }
}
