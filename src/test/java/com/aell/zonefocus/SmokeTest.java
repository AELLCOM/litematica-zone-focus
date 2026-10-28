package com.aell.zonefocus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aell.zonefocus.zone.ZoneBox;
import org.junit.jupiter.api.Test;

class SmokeTest
{
    @Test
    void zoneBoxSize()
    {
        assertEquals(2, new ZoneBox(0, 0, 0, 1, 1, 1).sizeX());
    }
}
