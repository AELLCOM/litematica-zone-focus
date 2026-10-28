package com.aell.zonefocus.gui.manage;

import com.aell.zonefocus.shaded.ui.Paint;
import com.aell.zonefocus.shaded.ui.Style;
import com.aell.zonefocus.shaded.ui.Vanilla;
import com.aell.zonefocus.zone.ZoneScanner;
import fi.dy.masa.malilib.render.GuiContext;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;

/**
 * Карта зон сверху — общая для обоих вариантов экрана, цвета — палитрой варианта. Номер на каждой
 * клетке; активная зона — синим (цвет активной зоны в мире), наведённая — светлой рамкой. Пока черновик
 * не применён, клетки серые и некликабельные, сверху подсказка «нажмите «Разбить»».
 *
 * <p>Под клетками может лежать снимок схемы сверху (Material HUD, {@code backdrop}): тогда подрегионы не
 * заливаются — только контур, а номера идут с тенью, чтобы читались на блоках.
 */
@Environment(EnvType.CLIENT)
public final class ZoneMap
{
    /** Цвета варианта: фон, контур схемы, линии сетки, активная зона, прогресс. */
    public record Palette(int panel, int edge, int region, int regionEdge, int cut, int number, int numberMuted,
            int active, int activeFill, int hover, int done, int partial, int missing, int banner) {}

    private ZoneMap() {}

    private static void rect(GuiContext ctx, int x, int y, int w, int h, int color)
    {
        if (w > 0 && h > 0 && (color >>> 24) != 0)
        {
            Paint.fill(ctx, x, y, x + w, y + h, color);
        }
    }

    private static void outline(GuiContext ctx, int x, int y, int w, int h, int color)
    {
        rect(ctx, x, y, w, 1, color);
        rect(ctx, x, y + h - 1, w, 1, color);
        rect(ctx, x, y + 1, 1, h - 2, color);
        rect(ctx, x + w - 1, y + 1, 1, h - 2, color);
    }

    private static int alpha(int color, int alpha)
    {
        return (alpha << 24) | (color & 0xFFFFFF);
    }

    private static final int INSET = 10;

    /** Прямоугольник габарита схемы на карте {x, y, w, h} — туда ложится снимок сверху. */
    public static int[] layout(ZoneEditor editor, int x, int y, int w, int h)
    {
        editor.cells(x + INSET, y + INSET, w - INSET * 2, h - INSET * 2);
        return editor.mapRect().clone();
    }

    /** Нарисовать карту; вернуть номер зоны под мышью или -1. */
    public static int render(GuiContext ctx, Font font, ZoneEditor editor, Palette p, int x, int y, int w, int h,
            int mouseX, int mouseY, boolean backdrop)
    {
        List<ZoneEditor.Cell> cells = editor.cells(x + INSET, y + INSET, w - INSET * 2, h - INSET * 2);
        boolean live = editor.live();
        int[] map = editor.mapRect();
        int hovered = -1;

        for (int[] r : editor.regions())
        {
            if (backdrop)
            {
                outline(ctx, r[0], r[1], r[2], r[3], p.regionEdge());
            }
            else
            {
                rect(ctx, r[0], r[1], r[2], r[3], p.region());
            }
        }

        for (ZoneEditor.Cell c : cells)
        {
            boolean over = live && mouseX >= c.x() && mouseX < c.x() + c.w() && mouseY >= c.y() && mouseY < c.y() + c.h();
            boolean active = live && c.index() == editor.active();
            ZoneScanner.Stats stats = live && ZoneEditor.showProgress ? editor.stats(c.index()) : null;

            if (c.missing())
            {
                // Зона схемы, которой сейчас нет: серая, полупрозрачная, без прогресса и выбора
                rect(ctx, c.x(), c.y(), c.w(), c.h(), 0x60808088);
                outline(ctx, c.x(), c.y(), c.w(), c.h(), alpha(p.numberMuted(), 0x80));
                String gone = String.valueOf(c.index() + 1);
                int gw = font.width(gone);

                if (c.w() >= gw + 4 && c.h() >= 10)
                {
                    Paint.text(ctx, font, gone, c.x() + (c.w() - gw) / 2 + 1, c.y() + (c.h() - 8) / 2 + 1,
                            alpha(p.numberMuted(), 0xA0), backdrop);
                }

                continue;
            }

            // Ванильный вид: клетка без снимка схемы под ней — плитка-кнопка Minecraft, активная подсвечена
            boolean tile = Style.vanilla() && ! backdrop && live;

            if (tile)
            {
                Vanilla.button(ctx, c.x(), c.y(), c.w(), c.h(), true, active || over);
            }

            if (stats != null && stats.total() > 0)
            {
                int tone = stats.missing() == 0 ? p.done() : stats.missing() < stats.total() ? p.partial() : p.missing();
                rect(ctx, c.x(), c.y(), c.w(), c.h(), alpha(tone, 0x40));
            }

            if (active && ! tile)
            {
                rect(ctx, c.x(), c.y(), c.w(), c.h(), p.activeFill());
            }

            if (over)
            {
                hovered = c.index();
            }

            if (! tile)
            {
                outline(ctx, c.x(), c.y(), c.w(), c.h(), active ? p.active() : over ? p.hover() : p.cut());
            }

            String label = String.valueOf(c.index() + 1);
            int tw = font.width(label);
            int color = ! live ? p.numberMuted() : tile ? (active ? Style.V_HOVER : Style.V_WHITE) : active ? p.active() : p.number();

            if (c.w() >= tw + 4 && c.h() >= 10)
            {
                Paint.text(ctx, font, label, c.x() + (c.w() - tw) / 2 + 1, c.y() + (c.h() - 8) / 2 + 1, color, backdrop);
            }
            else if (c.w() >= tw * 0.6F + 2 && c.h() >= 7)
            {
                // Узкая клетка — номер мельче, но всё равно на каждой
                ctx.pose().pushMatrix();
                ctx.pose().translate(c.x() + (c.w() - tw * 0.6F) / 2.0F + 0.5F, c.y() + (c.h() - 8 * 0.6F) / 2.0F + 0.5F);
                ctx.pose().scale(0.6F, 0.6F);
                Paint.text(ctx, font, label, 0, 0, color, backdrop);
                ctx.pose().popMatrix();
            }
        }

        if (map[2] > 0)
        {
            outline(ctx, map[0], map[1], map[2], map[3], p.regionEdge());
        }

        // Сверху — север вверху; спереди и сбоку — подпись осей карты
        int nx = x + w - 14;
        int ny = y + 5;

        if (ZoneEditor.mapView == 0)
        {
            Paint.text(ctx, font, "N", nx + 4, ny + 1, p.numberMuted(), false);
            rect(ctx, nx, ny + 1, 1, 7, p.numberMuted());
            rect(ctx, nx - 1, ny + 2, 1, 2, p.numberMuted());
            rect(ctx, nx + 1, ny + 2, 1, 2, p.numberMuted());
        }
        else
        {
            String axes = (ZoneEditor.mapView == 1 ? "X→" : "←Z") + " Y↑";
            Paint.text(ctx, font, axes, x + w - 6 - font.width(axes), ny + 1, p.numberMuted(), false);
        }

        return hovered;
    }

    /** Номер зоны под точкой в последней раскладке карты или -1. */
    public static int hit(ZoneEditor editor, int x, int y, int w, int h, double mouseX, double mouseY)
    {
        if (! editor.live())
        {
            return -1;
        }

        for (ZoneEditor.Cell c : editor.cells(x + INSET, y + INSET, w - INSET * 2, h - INSET * 2))
        {
            if (! c.missing() && mouseX >= c.x() && mouseX < c.x() + c.w() && mouseY >= c.y() && mouseY < c.y() + c.h())
            {
                return c.index();
            }
        }

        return -1;
    }
}
