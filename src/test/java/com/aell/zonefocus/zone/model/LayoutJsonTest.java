package com.aell.zonefocus.zone.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aell.zonefocus.zone.ZoneBox;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class LayoutJsonTest
{
    private static JsonObject parse(String json)
    {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    // ===== Старый формат (0.0.x) =====

    @Test
    void legacyGrid()
    {
        Layout layout = Layout.fromWorldJson(parse("{\"kind\":\"GRID\",\"placement\":\"Портал\",\"nx\":3,\"ny\":1,"
                + "\"nz\":2,\"active\":4,\"show_hidden\":false,\"hidden\":\"1,5\"}"));

        assertEquals(1, layout.sections.size());
        Section s = layout.sections.get(0);
        assertEquals(new Rule.Grid(3, 1, 2), s.rule);
        assertEquals(1, s.placements.size());
        assertEquals("Портал", s.placements.get(0).name);
        assertNull(s.placements.get(0).id);
        assertEquals(4, s.activeZone);
        assertEquals(Set.of(1, 5), s.hiddenZones);
        assertFalse(s.showHidden);
        assertEquals(Bounds.PER_PLACEMENT, s.bounds);
    }

    @Test
    void legacySubRegions()
    {
        Layout layout = Layout.fromWorldJson(parse("{\"kind\":\"SUB_REGIONS\",\"placement\":\"castle\",\"active\":0}"));

        assertEquals(1, layout.sections.size());
        Section s = layout.sections.get(0);
        assertSame(Rule.SubRegions.INSTANCE, s.rule);
        assertEquals(1, s.placements.size());
        assertEquals("castle", s.placements.get(0).name);
    }

    @Test
    void legacyManualWithName()
    {
        Layout layout = Layout.fromWorldJson(parse("{\"kind\":\"MANUAL\",\"placement\":\"castle\",\"active\":0,"
                + "\"manual_min\":\"1,2,3\",\"manual_max\":\"10,12,13\",\"manual_origin\":\"0,64,0\"}"));

        assertEquals(1, layout.sections.size());
        Section s = layout.sections.get(0);
        Rule.Manual manual = (Rule.Manual) s.rule;
        assertEquals(List.of(new ZoneBox(1, 2, 3, 10, 12, 13)), manual.boxes());
        assertNotNull(manual.base());
        assertEquals("castle", manual.base().name);
        assertEquals(new BlockPos(0, 64, 0), manual.origin());
        assertEquals(1, s.placements.size());
        assertSame(manual.base(), s.placements.get(0));
    }

    @Test
    void legacyManualWithoutName()
    {
        Layout layout = Layout.fromWorldJson(parse("{\"kind\":\"MANUAL\",\"placement\":\"\",\"active\":0,"
                + "\"manual_min\":\"1,2,3\",\"manual_max\":\"10,12,13\"}"));

        Section s = layout.sections.get(0);
        Rule.Manual manual = (Rule.Manual) s.rule;
        assertNotNull(manual.base());
        assertTrue(manual.base().isBlank());
        assertTrue(s.placements.isEmpty());
    }

    @Test
    void legacyManualWithoutMax()
    {
        Layout layout = Layout.fromWorldJson(parse("{\"kind\":\"MANUAL\",\"placement\":\"castle\","
                + "\"manual_min\":\"1,2,3\"}"));

        assertFalse(layout.hasSections());
    }

    @Test
    void legacyNone()
    {
        Layout layout = Layout.fromWorldJson(parse("{\"kind\":\"NONE\",\"placement\":\"x\"}"));

        assertFalse(layout.hasSections());
    }

    // ===== Формат v2 =====

    @Test
    void roundTripV2()
    {
        UUID refId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        Layout layout = new Layout("layout-1");
        layout.name = "Раскладка";
        layout.activeSection = 1;

        Section grid = new Section("sec-grid", new Rule.Grid(2, 3, 4));
        grid.name = "Замок";
        grid.hidden = true;
        grid.showHidden = true;
        grid.hiddenZones.add(2);
        grid.activeZone = 3;
        grid.bounds = Bounds.COMBINED;
        grid.combinedBox = new ZoneBox(-5, 0, -6, 20, 30, 40);
        grid.combinedOrigin = new BlockPos(7, 64, -8);
        grid.placements.add(new PlacementRef(refId, "Башня", "tower.litematic", new ZoneBox(0, 0, 0, 9, 9, 9)));
        grid.placements.add(PlacementRef.byName("Стена"));
        layout.sections.add(grid);

        PlacementRef base = new PlacementRef(null, "Мост", "bridge.litematic", new ZoneBox(1, 1, 1, 2, 2, 2));
        Section manual = new Section("sec-manual", new Rule.Manual(
                List.of(new ZoneBox(0, 0, 0, 5, 5, 5), new ZoneBox(10, 10, 10, 15, 15, 15)), base,
                new BlockPos(3, 70, 3)));
        manual.placements.add(base);
        layout.sections.add(manual);

        JsonObject json = layout.toWorldJson();
        assertEquals(2, json.get("version").getAsInt());
        assertFalse(Layout.isLegacy(json));

        Layout back = Layout.fromWorldJson(JsonParser.parseString(json.toString()).getAsJsonObject());

        assertEquals("layout-1", back.id);
        assertEquals("Раскладка", back.name);
        assertEquals(1, back.activeSection);
        assertEquals(2, back.sections.size());

        Section g = back.sections.get(0);
        assertEquals("sec-grid", g.id);
        assertEquals("Замок", g.name);
        assertTrue(g.hidden);
        assertTrue(g.showHidden);
        assertEquals(Set.of(2), g.hiddenZones);
        assertEquals(3, g.activeZone);
        assertEquals(Bounds.COMBINED, g.bounds);
        assertEquals(new ZoneBox(-5, 0, -6, 20, 30, 40), g.combinedBox);
        assertEquals(new BlockPos(7, 64, -8), g.combinedOrigin);
        assertEquals(new Rule.Grid(2, 3, 4), g.rule);
        assertEquals(2, g.placements.size());
        assertEquals(refId, g.placements.get(0).id);
        assertEquals("Башня", g.placements.get(0).name);
        assertEquals("tower.litematic", g.placements.get(0).file);
        assertEquals(new ZoneBox(0, 0, 0, 9, 9, 9), g.placements.get(0).box);
        assertEquals("Стена", g.placements.get(1).name);
        assertNull(g.placements.get(1).id);
        assertNull(g.placements.get(1).file);
        assertNull(g.placements.get(1).box);

        Section m = back.sections.get(1);
        assertEquals("sec-manual", m.id);
        assertFalse(m.hidden);
        assertFalse(m.showHidden);
        assertTrue(m.hiddenZones.isEmpty());
        assertEquals(Bounds.PER_PLACEMENT, m.bounds);
        assertNull(m.combinedBox);
        assertNull(m.combinedOrigin);
        Rule.Manual rm = (Rule.Manual) m.rule;
        assertEquals(List.of(new ZoneBox(0, 0, 0, 5, 5, 5), new ZoneBox(10, 10, 10, 15, 15, 15)), rm.boxes());
        assertNotNull(rm.base());
        assertEquals("Мост", rm.base().name);
        assertEquals("bridge.litematic", rm.base().file);
        assertEquals(new ZoneBox(1, 1, 1, 2, 2, 2), rm.base().box);
        assertEquals(new BlockPos(3, 70, 3), rm.origin());
        assertEquals(1, m.placements.size());
        assertEquals("Мост", m.placements.get(0).name);
    }

    // ===== Битые значения =====

    @Test
    void sectionWithoutRuleKindIsSkipped()
    {
        JsonObject broken = parse("{\"id\":\"a\",\"rule\":{\"nx\":2}}");
        assertNull(Section.fromJson(broken));

        JsonObject good = new Section("ok", new Rule.Grid(1, 1, 1)).toJson();
        JsonArray sections = new JsonArray();
        sections.add(broken);
        sections.add(good);
        JsonObject layoutJson = new JsonObject();
        layoutJson.add("sections", sections);

        Layout layout = Layout.fromJson(layoutJson);
        assertEquals(1, layout.sections.size());
        assertEquals("ok", layout.sections.get(0).id);
    }

    @Test
    void activeSectionOutOfRangeWraps()
    {
        Layout layout = new Layout("l");
        layout.sections.add(new Section("a", new Rule.Grid(1, 1, 1)));
        layout.sections.add(new Section("b", new Rule.Grid(1, 1, 1)));
        layout.activeSection = 5;

        JsonObject json = layout.toJson();
        Layout back = Layout.fromJson(json);

        Section active = back.active();
        assertNotNull(active);
        assertEquals("b", active.id);
        assertEquals(1, back.activeSection);

        assertNull(new Layout("empty").active());
    }

    // ===== Section.clampZones =====

    @Test
    void clampZonesActiveWraps()
    {
        Section s = new Section("s", new Rule.Grid(1, 1, 1));
        s.activeZone = 7;
        s.clampZones(5);
        assertEquals(2, s.activeZone);
    }

    @Test
    void clampZonesDropsOutOfRangeHidden()
    {
        Section s = new Section("s", new Rule.Grid(1, 1, 1));
        s.hiddenZones.addAll(List.of(1, 9));
        s.clampZones(5);
        assertEquals(Set.of(1), s.hiddenZones);
    }

    @Test
    void clampZonesZeroCount()
    {
        Section s = new Section("s", new Rule.Grid(1, 1, 1));
        s.activeZone = 3;
        s.hiddenZones.add(1);
        s.clampZones(0);
        assertEquals(0, s.activeZone);
        assertTrue(s.hiddenZones.isEmpty());
    }

    // ===== PlacementRef =====

    @Test
    void placementRefBadUuid()
    {
        PlacementRef ref = PlacementRef.fromJson(parse("{\"id\":\"not-a-uuid\",\"name\":\"Портал\","
                + "\"file\":\"p.litematic\",\"box\":\"0,0,0/4,4,4\"}"));

        assertNull(ref.id);
        assertEquals("Портал", ref.name);
        assertEquals("p.litematic", ref.file);
        assertEquals(new ZoneBox(0, 0, 0, 4, 4, 4), ref.box);
    }
}
