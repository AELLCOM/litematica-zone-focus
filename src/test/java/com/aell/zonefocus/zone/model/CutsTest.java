package com.aell.zonefocus.zone.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.aell.zonefocus.zone.ZoneBox;
import com.aell.zonefocus.zone.scope.ZoneLayouter;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

/** Правило «разрезы»: нормализация, ровная сетка как разрезы, отрезки по оси, JSON. */
class CutsTest
{
    @Test
    void normalizesSortsAndDropsDuplicatesAndNonPositive()
    {
        Rule.Cuts cuts = new Rule.Cuts(new int[] { 20, 5, 20, 0, -3 }, null, new int[0]);
        assertArrayEquals(new int[] { 5, 20 }, cuts.xs());
        assertArrayEquals(new int[0], cuts.ys());
        assertEquals(3, cuts.nx());
        assertEquals(1, cuts.ny());
        assertEquals(1, cuts.nz());
    }

    @Test
    void evenMatchesGridSlices()
    {
        // 10 блоков на 3 доли: 4 + 3 + 3, как у ZoneManager.slices
        assertArrayEquals(new int[] { 4, 7 }, Rule.Cuts.even(10, 3));
        assertArrayEquals(new int[0], Rule.Cuts.even(10, 1));
        // Долей больше, чем блоков — по блоку
        assertArrayEquals(new int[] { 1, 2 }, Rule.Cuts.even(3, 8));
    }

    @Test
    void ofGridAndClamp()
    {
        ZoneBox box = new ZoneBox(0, 0, 0, 9, 3, 19);
        Rule.Cuts cuts = Rule.Cuts.of(new Rule.Grid(2, 1, 4), box);
        assertArrayEquals(new int[] { 5 }, cuts.xs());
        assertArrayEquals(new int[] { 5, 10, 15 }, cuts.zs());
        Rule.Cuts wide = new Rule.Cuts(new int[] { 3, 10, 12 }, new int[] { 4 }, new int[] { 19 });
        Rule.Cuts clamped = wide.clamp(box);
        assertArrayEquals(new int[] { 3 }, clamped.xs());
        assertArrayEquals(new int[0], clamped.ys());
        assertArrayEquals(new int[] { 19 }, clamped.zs());
    }

    @Test
    void segmentsSplitAxisAtCuts()
    {
        int[][] seg = ZoneLayouter.segments(100, 10, new int[] { 3, 7 });
        assertEquals(3, seg.length);
        assertArrayEquals(new int[] { 100, 102 }, seg[0]);
        assertArrayEquals(new int[] { 103, 106 }, seg[1]);
        assertArrayEquals(new int[] { 107, 109 }, seg[2]);
        // Разрезы на краях и за габаритом пропускаются
        assertEquals(1, ZoneLayouter.segments(0, 5, new int[] { 0, 5, 9 }).length);
    }

    @Test
    void jsonRoundTrip()
    {
        Rule.Cuts cuts = new Rule.Cuts(new int[] { 10 }, new int[0], new int[] { 20, 30 });
        Rule back = Rule.fromJson(JsonParser.parseString(cuts.toJson().toString()).getAsJsonObject());
        assertInstanceOf(Rule.Cuts.class, back);
        assertEquals(cuts, back);
    }
}
