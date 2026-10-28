package com.aell.zonefocus.tool;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;

@Environment(EnvType.CLIENT)
public final class ToolIcon
{
    public static final int SIZE = 14;

    public static final int MOUSE_WIDTH  = 7;
    public static final int MOUSE_HEIGHT = 10;

    private static final int COLOR_SELECT = 0xFF50D0FF;
    private static final int COLOR_LOCK   = 0xFF3C3C44;
    private static final int COLOR_HOLE   = 0xFF8B8B8B;

    private static final int COLOR_MOUSE  = 0xFFFFFFFF;
    private static final int COLOR_WHEEL  = 0xFFFF5555;

    private ToolIcon() {}

    public static void drawSelect(GuiGraphicsExtractor gfx, int x, int y)
    {
        int arm = 4;

        rect(gfx, x, y, arm, 1, COLOR_SELECT);
        rect(gfx, x, y, 1, arm, COLOR_SELECT);
        rect(gfx, x + SIZE - arm, y, arm, 1, COLOR_SELECT);
        rect(gfx, x + SIZE - 1, y, 1, arm, COLOR_SELECT);
        rect(gfx, x, y + SIZE - 1, arm, 1, COLOR_SELECT);
        rect(gfx, x, y + SIZE - arm, 1, arm, COLOR_SELECT);
        rect(gfx, x + SIZE - arm, y + SIZE - 1, arm, 1, COLOR_SELECT);
        rect(gfx, x + SIZE - 1, y + SIZE - arm, 1, arm, COLOR_SELECT);

        rect(gfx, x + SIZE / 2 - 1, y + SIZE / 2 - 1, 2, 2, COLOR_SELECT);
    }

    public static void drawLock(GuiGraphicsExtractor gfx, int x, int y)
    {
        int bodyX = x + 1;
        int bodyY = y + 6;

        rect(gfx, bodyX + 2, y, 8, 1, COLOR_LOCK);
        rect(gfx, bodyX + 2, y, 1, 6, COLOR_LOCK);
        rect(gfx, bodyX + 9, y, 1, 6, COLOR_LOCK);

        rect(gfx, bodyX, bodyY, 12, 8, COLOR_LOCK);

        rect(gfx, bodyX + 5, bodyY + 2, 2, 4, COLOR_HOLE);
    }

    public static void drawMouseWheel(GuiGraphicsExtractor gfx, int x, int y)
    {
        int w = MOUSE_WIDTH;
        int h = MOUSE_HEIGHT;

        rect(gfx, x + 1, y, w - 2, 1, COLOR_MOUSE);
        rect(gfx, x + 1, y + h - 1, w - 2, 1, COLOR_MOUSE);
        rect(gfx, x, y + 1, 1, h - 2, COLOR_MOUSE);
        rect(gfx, x + w - 1, y + 1, 1, h - 2, COLOR_MOUSE);

        rect(gfx, x + w / 2, y + 2, 1, 3, COLOR_WHEEL);
    }

    private static void rect(GuiGraphicsExtractor gfx, int x, int y, int width, int height, int color)
    {
        gfx.fill(x, y, x + width, y + height, color);
    }
}
