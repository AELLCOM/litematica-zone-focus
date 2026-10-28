package com.aell.zonefocus.zone.model;

import com.aell.zonefocus.zone.ZoneBox;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * Участок: группа схем плюс правило разбиения. Номера зон постоянны внутри участка; {@code id} — ключ
 * прогресса, сигнатур и скрытия, живёт дольше любого пересчёта.
 */
public final class Section
{
    public final String id;
    public String name = "";
    public final List<PlacementRef> placements = new ArrayList<>();
    public Rule rule;
    public Bounds bounds = Bounds.PER_PLACEMENT;
    /**
     * Общий габарит при разбиении в координатах мира и origin первого размещения на тот момент (только
     * {@link Bounds#COMBINED}): при чтении габарит сдвигается вместе с первым размещением.
     */
    public @Nullable ZoneBox combinedBox;
    public @Nullable net.minecraft.core.BlockPos combinedOrigin;
    public int activeZone;
    public final Set<Integer> hiddenZones = new LinkedHashSet<>();
    public boolean showHidden;
    public boolean hidden;

    public Section(String id, Rule rule)
    {
        this.id = id;
        this.rule = rule;
    }

    public static Section create(Rule rule)
    {
        return new Section(UUID.randomUUID().toString(), rule);
    }

    public boolean isSingle()
    {
        return this.placements.size() == 1;
    }

    public boolean isGroup()
    {
        return this.placements.size() > 1;
    }

    public boolean hasNoPlacements()
    {
        return this.placements.isEmpty();
    }

    public @Nullable PlacementRef firstPlacement()
    {
        return this.placements.isEmpty() ? null : this.placements.getFirst();
    }

    /** Новое правило: активная зона и скрытия сбрасываются, как и раньше при любом разбиении. */
    public void resetZones()
    {
        this.activeZone = 0;
        this.hiddenZones.clear();
    }

    /** Активная зона и скрытия — в пределах {@code count} зон. */
    public void clampZones(int count)
    {
        if (count <= 0)
        {
            this.activeZone = 0;
            this.hiddenZones.clear();
            return;
        }

        this.activeZone = Math.floorMod(this.activeZone, count);
        this.hiddenZones.removeIf(i -> i < 0 || i >= count);
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", this.id);
        obj.addProperty("name", this.name);
        obj.addProperty("hidden", this.hidden);
        obj.addProperty("active_zone", this.activeZone);
        obj.addProperty("show_hidden", this.showHidden);

        if (! this.hiddenZones.isEmpty())
        {
            obj.addProperty("hidden_zones", Json.ints(this.hiddenZones));
        }

        obj.addProperty("bounds", this.bounds.name());
        Json.putBox(obj, "combined_box", this.combinedBox);
        Json.putPos(obj, "combined_origin", this.combinedOrigin);
        List<JsonObject> refs = new ArrayList<>();
        this.placements.forEach(p -> refs.add(p.toJson()));
        obj.add("placements", Json.array(refs));
        obj.add("rule", this.rule.toJson());

        return obj;
    }

    public static @Nullable Section fromJson(JsonObject obj)
    {
        Rule rule = Rule.fromJson(Json.object(obj, "rule"));

        if (rule == null)
        {
            return null;
        }

        String id = Json.str(obj, "id", "");
        Section section = new Section(id.isEmpty() ? UUID.randomUUID().toString() : id, rule);
        section.name = Json.str(obj, "name", "");
        section.hidden = Json.bool(obj, "hidden", false);
        section.activeZone = Math.max(0, Json.integer(obj, "active_zone", 0));
        section.showHidden = Json.bool(obj, "show_hidden", false);
        section.hiddenZones.addAll(Json.ints(Json.str(obj, "hidden_zones", "")));
        section.bounds = Bounds.of(Json.str(obj, "bounds", "PER_PLACEMENT"));
        section.combinedBox = Json.box(obj, "combined_box");
        section.combinedOrigin = Json.pos(obj, "combined_origin");

        for (JsonObject ref : Json.objects(obj, "placements"))
        {
            section.placements.add(PlacementRef.fromJson(ref));
        }

        return section;
    }
}
