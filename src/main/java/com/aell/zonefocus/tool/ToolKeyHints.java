package com.aell.zonefocus.tool;

import com.aell.zonefocus.config.ZoneFocusConfigs;
import com.aell.zonefocus.shaded.ui.Paint;
import com.aell.zonefocus.shaded.ui.Strings;
import com.aell.zonefocus.shaded.ui.Style;
import com.aell.zonefocus.shaded.ui.Vanilla;
import com.aell.zonefocus.ui.Draw;
import com.aell.zonefocus.ui.Theme;
import fi.dy.masa.malilib.event.RenderEventHandler;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.GuiUtils;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.profiling.ProfilerFiller;

@Environment(EnvType.CLIENT)
public final class ToolKeyHints implements IRenderer
{
    private static final ToolKeyHints INSTANCE = new ToolKeyHints();

    private static final int PADDING     = 3;
    private static final int GLYPH_GAP   = 3;
    private static final int BOTTOM_GAP  = 34;
    private static final int MARGIN      = 4;

    private static final int COLOR_TEXT  = Theme.TEXT;

    private ToolKeyHints() {}

    public static void register()
    {
        RenderEventHandler.getInstance().registerInGameGuiRenderer(INSTANCE);
    }

    @Override
    public void onExtractGuiOverlayPost(GuiContext ctx, float partialTicks, ProfilerFiller profiler)
    {
        Minecraft mc = Minecraft.getInstance();

        if (! ZoneTool.getInstance().isActive() || GuiUtils.getCurrentScreen() != null
                || ! ZoneFocusConfigs.TOOL_KEY_HINTS.getBooleanValue()
                || mc.player == null || mc.gui.hud.isHidden())
        {
            return;
        }

        draw(ctx.getGuiGraphics(), ctx.fontRenderer(), lines());
    }

    private record Hint(String before, boolean mouse, String after) {}

    private static List<Hint> lines()
    {
        List<Hint> hints = new ArrayList<>(4);
        boolean dragging = ZoneTool.getInstance().getPreviewBox() != null;

        if (dragging)
        {
            hints.add(new Hint(Strings.plain("zonefocus.hint.dragging"), false, ""));
            return hints;
        }

        hints.add(new Hint(Strings.plain("zonefocus.hint.drag"), false, ""));
        hints.add(new Hint(Strings.plain("zonefocus.hint.save"), false, ""));
        hints.add(new Hint(Strings.plain("zonefocus.hint.wheel_before"), true,
                Strings.plain("zonefocus.hint.wheel_after")));

        return hints;
    }

    private static void draw(GuiGraphicsExtractor gfx, Font font, List<Hint> hints)
    {
        int screenWidth = GuiUtils.getScaledWindowWidth();
        int screenHeight = GuiUtils.getScaledWindowHeight();

        int lineHeight = Math.max(font.lineHeight, ToolIcon.MOUSE_HEIGHT) + 2;
        int maxWidth = 0;

        for (Hint hint : hints)
        {
            maxWidth = Math.max(maxWidth, width(font, hint));
        }

        int panelHeight = hints.size() * lineHeight + PADDING * 2 + 2;
        int panelWidth = maxWidth + PADDING * 2 + 4;
        int panelX = Math.max(0, screenWidth - panelWidth - MARGIN);
        int panelY = screenHeight - BOTTOM_GAP - panelHeight;

        GuiContext ctx = GuiContext.fromGuiGraphics(gfx);
        if (Style.vanilla())
        {
            // Ванильный стиль: рамка подсказки Minecraft
            Vanilla.tooltip(ctx, panelX, panelY, panelWidth, panelHeight);
        }
        else
        {
            Draw.card(ctx, panelX, panelY, panelWidth, panelHeight, 6, Theme.PANEL, Theme.EDGE);
        }

        for (int i = 0; i < hints.size(); i++)
        {
            Hint hint = hints.get(i);

            int x = panelX + panelWidth - PADDING - 2 - width(font, hint);
            int y = panelY + PADDING + i * lineHeight;
            int textY = y + (lineHeight - 2 - font.lineHeight) / 2;

            Paint.text(ctx, font, hint.before(), x, textY, COLOR_TEXT, true);
            x += font.width(hint.before());

            if (hint.mouse())
            {
                x += GLYPH_GAP;
                ToolIcon.drawMouseWheel(gfx, x, y);
                x += ToolIcon.MOUSE_WIDTH + GLYPH_GAP;

                Paint.text(ctx, font, hint.after(), x, textY, COLOR_TEXT, true);
            }
        }
    }

    private static int width(Font font, Hint hint)
    {
        int width = font.width(hint.before());

        if (hint.mouse())
        {
            width += GLYPH_GAP * 2 + ToolIcon.MOUSE_WIDTH + font.width(hint.after());
        }

        return width;
    }
}
