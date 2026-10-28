package com.aell.zonefocus.tool;

import com.aell.zonefocus.compat.AxiomCompat;
import com.aell.zonefocus.ZoneFocusModule;
import com.aell.zonefocus.config.ZoneFocusConfigs;
import com.aell.zonefocus.mixin.IMixinAbstractContainerScreen;
import com.aell.zonefocus.zone.ZoneManager;
import com.aell.zonefocus.shaded.ui.SlotAnchor;
import com.aell.zonefocus.shaded.ui.Style;
import com.aell.zonefocus.shaded.ui.Vanilla;
import com.aell.zonefocus.ui.Draw;
import com.aell.zonefocus.ui.Theme;
import fi.dy.masa.malilib.render.GuiContext;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public final class ToolButton extends AbstractWidget
{
    private static final int SIZE = 18;
    private static final int GAP = 4;

    private static final int COLOR_SLOT_DISABLED  = 0xA0505058;

    private final AbstractContainerScreen<?> screen;

    private boolean hadPlacement = true;

    private ToolButton(AbstractContainerScreen<?> screen)
    {
        super(0, 0, SIZE, SIZE, label());
        this.screen = screen;
        this.refreshTooltip(hasPlacement());
        this.reposition();
    }

    public static void register()
    {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (! isInventory(screen) || ! shouldShow())
            {
                return;
            }

            Screens.getWidgets(screen).add(new ToolButton((AbstractContainerScreen<?>) screen));
        });
    }

    /**
     * Место пересчитывается каждый кадр, а не один раз при открытии экрана: панель эффектов
     * появляется и исчезает по ходу дела, книга рецептов двигает окно, а масштаб интерфейса
     * человек меняет прямо в настройках.
     */
    private void reposition()
    {
        IMixinAbstractContainerScreen access = (IMixinAbstractContainerScreen) this.screen;
        int guiX = access.zonefocus_getLeftPos();
        int guiY = access.zonefocus_getTopPos();
        int guiW = access.zonefocus_getImageWidth();
        int guiH = access.zonefocus_getImageHeight();
        int screenW = this.screen.width;
        int screenH = this.screen.height;

        // Над окном креатива идёт ряд вкладок — туда нельзя
        boolean allowAbove = ! (this.screen instanceof CreativeModeInventoryScreen)
                && ! AxiomCompat.isCreativeInventoryScreen(this.screen);

        int[] pos = SlotAnchor.place(screenW, screenH, guiX, guiY, guiW, guiH, SIZE, GAP,
                SlotAnchor.effectsPanel(screenW, guiX, guiY, guiW, this.effectCount()),
                SlotAnchor.leftOccupied(screenW, guiX, guiW), allowAbove, this.neighbours());

        this.setX(pos[0]);
        this.setY(pos[1]);
    }

    private int effectCount()
    {
        Minecraft mc = Minecraft.getInstance();

        return this.screen instanceof InventoryScreen inventory && inventory.showsActiveEffects()
                && mc.player != null ? mc.player.getActiveEffects().size() : 0;
    }

    /** Чужие виджеты на экране: свои кнопки добавляют и другие моды, наезжать на них незачем. */
    private List<SlotAnchor.Rect> neighbours()
    {
        List<SlotAnchor.Rect> rects = new ArrayList<>();

        for (AbstractWidget widget : Screens.getWidgets(this.screen))
        {
            if (widget == this || ! widget.visible)
            {
                continue;
            }

            rects.add(new SlotAnchor.Rect(widget.getX(), widget.getY(),
                    widget.getWidth(), widget.getHeight()));
        }

        return rects;
    }

    private static boolean isInventory(Screen screen)
    {
        return screen instanceof InventoryScreen
                || screen instanceof CreativeModeInventoryScreen
                || AxiomCompat.isCreativeInventoryScreen(screen);
    }

    private static boolean shouldShow()
    {
        return ZoneFocusModule.isEnabled()
                && ZoneFocusConfigs.TOOL_ENABLED.getBooleanValue()
                && ZoneFocusConfigs.TOOL_INVENTORY_BUTTON.getBooleanValue();
    }

    private static boolean hasPlacement()
    {
        return ! ZoneManager.enabledPlacements().isEmpty();
    }

    private static Component label()
    {
        return Component.translatable(ZoneTool.getInstance().isActive()
                ? "zonefocus.gui.tool_off"
                : "zonefocus.gui.tool_on");
    }

    private void refreshTooltip(boolean placement)
    {
        this.hadPlacement = placement;
        this.setTooltip(Tooltip.create(Component.translatable(placement
                ? "zonefocus.gui.tool_button.tooltip"
                : "zonefocus.gui.tool_button.no_placement")));
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick)
    {
        if (! hasPlacement())
        {
            return;
        }

        ZoneTool tool = ZoneTool.getInstance();

        tool.toggle();
        this.setMessage(label());

        if (tool.isActive())
        {
            Minecraft.getInstance().gui.setScreen(null);
        }
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick)
    {
        this.reposition();

        int x = this.getX();
        int y = this.getY();
        boolean active = ZoneTool.getInstance().isActive();
        boolean placement = hasPlacement();

        if (placement != this.hadPlacement)
        {
            this.refreshTooltip(placement);
        }

        GuiContext ctx = GuiContext.fromGuiGraphics(gfx);

        boolean vanilla = Style.vanilla();

        if (vanilla)
        {
            // Ванильный стиль: обычная кнопка Minecraft, включённый инструмент — подсвечен
            Vanilla.button(ctx, x, y, SIZE, SIZE, placement, active || this.isHovered());
        }
        else
        {
            Draw.card(ctx, x, y, SIZE, SIZE, 4,
                    active ? Theme.GREEN_FILL : Theme.PANEL_DEEP,
                    active ? Theme.GREEN_EDGE : Theme.EDGE);
        }

        int iconX = x + (SIZE - ToolIcon.SIZE) / 2;
        int iconY = y + (SIZE - ToolIcon.SIZE) / 2;

        if (active)
        {
            ToolIcon.drawSelect(gfx, iconX, iconY);
        }
        else
        {
            ToolIcon.drawLock(gfx, iconX, iconY);
        }

        if (vanilla)
        {
            return;
        }

        if (! placement)
        {
            Draw.round(ctx, x + 1, y + 1, SIZE - 2, SIZE - 2, 3, COLOR_SLOT_DISABLED);
        }
        else if (this.isHovered())
        {
            Draw.round(ctx, x + 1, y + 1, SIZE - 2, SIZE - 2, 3, Theme.ROW_HOVER);
            Draw.roundEdge(ctx, x, y, SIZE, SIZE, 4, Theme.GREEN_EDGE);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output)
    {
        output.add(NarratedElementType.TITLE, this.getMessage());
    }
}
