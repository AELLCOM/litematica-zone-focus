package com.aell.zonefocus.zone;

import java.util.LinkedHashSet;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Снимок активного участка в старом виде (0.0.x): одно правило, одно имя схемы, одна сетка. Нужен старым
 * экранам, мосту и стенду, которые читают {@link ZoneManager#getState()}. Только чтение: записи в поля
 * никуда не попадают — сама раскладка живёт в {@link com.aell.zonefocus.zone.model.Layout}.
 */
@Environment(EnvType.CLIENT)
public class ZoneState
{
    public enum Kind
    {
        NONE,
        GRID,
        SUB_REGIONS,
        MANUAL
    }

    public Kind kind = Kind.NONE;
    public String placementName = "";
    public int nx = 1;
    public int ny = 1;
    public int nz = 1;
    public int activeIndex;

    public final Set<Integer> hidden = new LinkedHashSet<>();

    public boolean showHidden;

    @Nullable public BlockPos manualMin;
    @Nullable public BlockPos manualMax;
    @Nullable public BlockPos manualOrigin;
}
