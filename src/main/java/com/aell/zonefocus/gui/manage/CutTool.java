package com.aell.zonefocus.gui.manage;

import com.aell.zonefocus.compat.MaterialHudBridge;
import com.aell.zonefocus.shaded.ui.Paint;
import com.aell.zonefocus.shaded.ui.Ui;
import com.aell.zonefocus.zone.ZoneBox;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.StringUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Разрезы участка мышью — на 2D-карте (линии) и на 3D-превью (плоскости с ручкой). Плоскость двигается с шагом в
 * один блок: на карте — по пикселю под курсором, в 3D — по проекции движения мыши на экранную ось разреза.
 * Отпустили — разрез встаёт в участок ({@link ZoneEditor#moveCut}), число зон то же.
 *
 * <p>Резать можно только применённую сетку или разрезы одним габаритом ({@link ZoneEditor#cuttable()}).
 */
@Environment(EnvType.CLIENT)
final class CutTool
{
    private static final int HIT_2D = 3;
    private static final int HIT_3D = 6;
    private static final int HANDLE = 5;
    /** Блок оси на экране короче этой доли самой длинной оси — ось смотрит почти на зрителя, мышью её не двигать. */
    private static final float AXIS_SHARE = 0.25F;
    private static final int LINE = 0xA0E0B341;
    private static final int LINE_HOT = 0xFFFFD166;
    private static final String[] AXES = { "X", "Y", "Z" };

    private int hoverAxis = -1;
    private int hoverIndex = -1;
    private int dragAxis = -1;
    private int dragIndex = -1;
    private int dragPos;
    private int dragStart;
    private boolean drag3d;
    private double pressX;
    private double pressY;
    private float dirX;
    private float dirY;
    /** Ручки 3D последнего кадра: ось, номер, экран, направление оси на экране (пикс. на блок). */
    private float[][] handles = new float[0][];

    boolean dragging()
    {
        return this.dragAxis >= 0;
    }

    /** Начало кадра: наведение считается заново (не тянем — подсветки нет, пока кадр её не найдёт). */
    void frame()
    {
        if (! this.dragging())
        {
            this.hoverAxis = -1;
            this.hoverIndex = -1;
        }
    }

    boolean hovered()
    {
        return this.hoverAxis >= 0;
    }

    int hoverAxis()
    {
        return this.hoverAxis;
    }

    int hoverIndex()
    {
        return this.hoverIndex;
    }

    /** Подсказка под курсором: что тащим и куда. */
    @Nullable String tip(ZoneEditor editor)
    {
        ZoneBox box = editor.cutBox();
        int[][] cuts = editor.cuts();

        if (box == null || cuts == null)
        {
            return null;
        }

        if (this.dragging())
        {
            return StringUtils.translate("zonefocus.gui.cut.dragging", AXES[this.dragAxis],
                    ZoneEditor.min(box, this.dragAxis) + this.dragPos);
        }

        if (this.hovered() && this.hoverIndex < cuts[this.hoverAxis].length)
        {
            return StringUtils.translate("zonefocus.gui.cut.hover", AXES[this.hoverAxis],
                    ZoneEditor.min(box, this.hoverAxis) + cuts[this.hoverAxis][this.hoverIndex]);
        }

        return null;
    }

    private int position(int axis, int index, int[][] cuts)
    {
        return this.dragAxis == axis && this.dragIndex == index ? this.dragPos : cuts[axis][index];
    }

    // ===== 2D =====

    /** Линии разрезов поверх карты; наведённая или тянущаяся — ярче. */
    void render2d(GuiContext ctx, ZoneEditor editor, int mouseX, int mouseY)
    {
        if (! this.dragging() || this.drag3d)
        {
            this.hoverAxis = -1;
            this.hoverIndex = -1;
        }

        ZoneBox box = editor.cutBox();
        int[][] cuts = editor.cuts();
        int[] map = editor.mapRect();

        if (box == null || cuts == null || map[2] <= 0)
        {
            return;
        }

        boolean inside = mouseX >= map[0] - HIT_2D && mouseX < map[0] + map[2] + HIT_2D
                && mouseY >= map[1] - HIT_2D && mouseY < map[1] + map[3] + HIT_2D;
        double best = HIT_2D + 0.5;

        for (int axis : new int[] { ZoneEditor.hAxis(), ZoneEditor.vAxis() })
        {
            boolean vertical = axis == ZoneEditor.hAxis();

            for (int i = 0; i < cuts[axis].length; ++i)
            {
                double s = editor.cutScreen(box, axis, this.position(axis, i, cuts));

                if (Double.isNaN(s))
                {
                    continue;
                }

                int at = (int) Math.round(s);

                if (! this.dragging() && inside)
                {
                    double d = Math.abs((vertical ? mouseX : mouseY) - s);

                    if (d < best)
                    {
                        best = d;
                        this.hoverAxis = axis;
                        this.hoverIndex = i;
                    }
                }

                boolean hot = (this.dragAxis == axis && this.dragIndex == i) || (! this.dragging() && this.hoverAxis == axis && this.hoverIndex == i);
                int color = hot ? LINE_HOT : LINE;

                if (vertical)
                {
                    Paint.rect(ctx, at - (hot ? 1 : 0), map[1], hot ? 2 : 1, map[3], color);
                }
                else
                {
                    Paint.rect(ctx, map[0], at - (hot ? 1 : 0), map[2], hot ? 2 : 1, color);
                }
            }
        }
    }

    /** ЛКМ на карте: взяли линию под курсором. */
    boolean press2d()
    {
        if (this.hoverAxis < 0)
        {
            return false;
        }

        this.dragAxis = this.hoverAxis;
        this.dragIndex = this.hoverIndex;
        this.drag3d = false;
        return true;
    }

    // ===== 3D =====

    /**
     * Плоскости разрезов поверх 3D: контур по габариту участка и ручка в середине. Координаты превью — блоки от
     * {@code origin} (начало координат модели).
     */
    void render3d(GuiContext ctx, ZoneEditor editor, Object preview, BlockPos origin, int mouseX, int mouseY,
            int x, int y, int w, int h)
    {
        if (! this.dragging() || ! this.drag3d)
        {
            this.hoverAxis = -1;
            this.hoverIndex = -1;
        }

        ZoneBox box = editor.cutBox();
        int[][] cuts = editor.cuts();

        if (box == null || cuts == null || ! MaterialHudBridge.previewHasModel(preview))
        {
            this.handles = new float[0][];
            return;
        }

        float[] lo = { box.minX() - origin.getX(), box.minY() - origin.getY(), box.minZ() - origin.getZ() };
        float[] hi = { box.maxX() + 1 - origin.getX(), box.maxY() + 1 - origin.getY(), box.maxZ() + 1 - origin.getZ() };

        // Длина блока каждой оси на экране: у большой модели блок меньше пикселя, поэтому «ось смотрит на зрителя»
        // — это ось заметно короче остальных, а не короче пикселя
        float[] mid0 = { (lo[0] + hi[0]) * 0.5F, (lo[1] + hi[1]) * 0.5F, (lo[2] + hi[2]) * 0.5F };
        float[] base = MaterialHudBridge.previewProject(preview, mid0[0], mid0[1], mid0[2]);
        float[] axisLen = new float[3];
        float longest = 0.0F;

        for (int a = 0; a < 3 && base != null; ++a)
        {
            float[] p = mid0.clone();
            p[a] += 1.0F;
            float[] s = MaterialHudBridge.previewProject(preview, p[0], p[1], p[2]);
            axisLen[a] = s == null ? 0.0F : (float) Math.hypot(s[0] - base[0], s[1] - base[1]);
            longest = Math.max(longest, axisLen[a]);
        }
        int count = cuts[0].length + cuts[1].length + cuts[2].length;
        float[][] made = new float[count][];
        int n = 0;
        double best = HIT_3D + 0.5;
        boolean inside = mouseX >= x && mouseY >= y && mouseX < x + w && mouseY < y + h;
        ctx.enableScissor(x, y, x + w, y + h);

        for (int axis = 0; axis < 3; ++axis)
        {
            int b = (axis + 1) % 3;
            int c = (axis + 2) % 3;

            for (int i = 0; i < cuts[axis].length; ++i)
            {
                float at = lo[axis] + this.position(axis, i, cuts);
                float[][] corners = new float[4][];
                boolean ok = true;

                for (int k = 0; k < 4; ++k)
                {
                    float[] p = new float[3];
                    p[axis] = at;
                    p[b] = (k == 1 || k == 2) ? hi[b] : lo[b];
                    p[c] = k >= 2 ? hi[c] : lo[c];
                    corners[k] = MaterialHudBridge.previewProject(preview, p[0], p[1], p[2]);
                    ok &= corners[k] != null;
                }

                float[] mid = new float[3];
                mid[axis] = at;
                mid[b] = (lo[b] + hi[b]) * 0.5F;
                mid[c] = (lo[c] + hi[c]) * 0.5F;
                float[] center = MaterialHudBridge.previewProject(preview, mid[0], mid[1], mid[2]);
                mid[axis] = at + 1.0F;
                float[] step = MaterialHudBridge.previewProject(preview, mid[0], mid[1], mid[2]);

                if (! ok || center == null || step == null)
                {
                    continue;
                }

                float dx = step[0] - center[0];
                float dy = step[1] - center[1];
                boolean movable = longest > 0.0F && axisLen[axis] >= longest * AXIS_SHARE && dx * dx + dy * dy > 1.0E-4F;
                made[n++] = new float[] { axis, i, center[0], center[1], dx, dy, movable ? 1 : 0 };

                if (! this.dragging() && inside && movable)
                {
                    double d = Math.hypot(mouseX - center[0], mouseY - center[1]);

                    if (d < best)
                    {
                        best = d;
                        this.hoverAxis = axis;
                        this.hoverIndex = i;
                    }
                }

                boolean hot = (this.dragAxis == axis && this.dragIndex == i) || (! this.dragging() && this.hoverAxis == axis && this.hoverIndex == i);
                int color = hot ? LINE_HOT : LINE;

                for (int k = 0; k < 4; ++k)
                {
                    float[] p0 = corners[k];
                    float[] p1 = corners[(k + 1) % 4];
                    line(ctx, p0[0], p0[1], p1[0], p1[1], hot ? 2.0F : 1.0F, color);
                }

                if (movable)
                {
                    int hx = Math.round(center[0]) - HANDLE / 2;
                    int hy = Math.round(center[1]) - HANDLE / 2;
                    Paint.rect(ctx, hx - 1, hy - 1, HANDLE + 2, HANDLE + 2, 0xE0101014);
                    Paint.rect(ctx, hx, hy, HANDLE, HANDLE, color);
                }
            }
        }

        ctx.disableScissor();
        this.handles = java.util.Arrays.copyOf(made, n);
    }

    /** ЛКМ по ручке в 3D: взяли плоскость. */
    boolean press3d(ZoneEditor editor, double mouseX, double mouseY)
    {
        int[][] cuts = editor.cuts();

        if (this.hoverAxis < 0 || cuts == null)
        {
            return false;
        }

        for (float[] h : this.handles)
        {
            if ((int) h[0] == this.hoverAxis && (int) h[1] == this.hoverIndex && h[6] > 0.0F)
            {
                this.dragAxis = this.hoverAxis;
                this.dragIndex = this.hoverIndex;
                this.drag3d = true;
                this.pressX = mouseX;
                this.pressY = mouseY;
                this.dirX = h[4];
                this.dirY = h[5];
                this.dragStart = cuts[this.dragAxis][this.dragIndex];
                this.dragPos = this.dragStart;
                return true;
            }
        }

        return false;
    }

    /** Мышь тянет плоскость: новое положение — ближайший блок, между соседними разрезами. */
    void drag(ZoneEditor editor, double mouseX, double mouseY)
    {
        ZoneBox box = editor.cutBox();
        int[][] cuts = editor.cuts();

        if (! this.dragging() || box == null || cuts == null || this.dragIndex >= cuts[this.dragAxis].length)
        {
            return;
        }

        int next;

        if (this.drag3d)
        {
            double len2 = this.dirX * this.dirX + this.dirY * this.dirY;
            double blocks = ((mouseX - this.pressX) * this.dirX + (mouseY - this.pressY) * this.dirY) / len2;
            next = this.dragStart + (int) Math.round(blocks);
        }
        else
        {
            boolean vertical = this.dragAxis == ZoneEditor.hAxis();
            next = editor.screenToCut(box, this.dragAxis, vertical ? mouseX : mouseY);
        }

        int[] row = cuts[this.dragAxis];
        int lower = this.dragIndex > 0 ? row[this.dragIndex - 1] + 1 : 1;
        int upper = this.dragIndex + 1 < row.length ? row[this.dragIndex + 1] - 1 : ZoneEditor.size(box, this.dragAxis) - 1;
        this.dragPos = Math.max(lower, Math.min(upper, next));
    }

    /** Отпустили: разрез встаёт в участок; истина — что-то изменилось. */
    boolean release(ZoneEditor editor)
    {
        if (! this.dragging())
        {
            return false;
        }

        int[][] cuts = editor.cuts();
        boolean changed = cuts != null && this.dragIndex < cuts[this.dragAxis].length
                && cuts[this.dragAxis][this.dragIndex] != this.dragPos;

        if (changed)
        {
            editor.moveCut(this.dragAxis, this.dragIndex, this.dragPos);
        }

        this.dragAxis = -1;
        this.dragIndex = -1;
        return changed;
    }

    void cancel()
    {
        this.dragAxis = -1;
        this.dragIndex = -1;
        this.hoverAxis = -1;
        this.hoverIndex = -1;
    }

    static String axisName(int axis)
    {
        return AXES[axis];
    }

    /** Отрезок толщиной {@code width} пикселей GUI: единичный квадрат, повёрнутый и растянутый позой. */
    private static void line(GuiContext ctx, float x1, float y1, float x2, float y2, float width, int color)
    {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);

        if (length < 0.05F)
        {
            return;
        }

        ctx.pose().pushMatrix();
        ctx.pose().translate(x1, y1);
        ctx.pose().rotate((float) Math.atan2(dy, dx));
        ctx.pose().translate(0.0F, -width * 0.5F);
        ctx.pose().scale(length, width);
        Paint.fill(ctx, 0, 0, 1, 1, color);
        ctx.pose().popMatrix();
    }

    static int edgeColor()
    {
        return Ui.alpha(LINE_HOT, 0.6F);
    }
}
