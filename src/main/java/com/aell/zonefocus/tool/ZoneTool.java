package com.aell.zonefocus.tool;

import com.aell.zonefocus.compat.AxiomCompat;
import com.aell.zonefocus.ZoneChat;
import com.aell.zonefocus.ZoneFocusModule;
import com.aell.zonefocus.config.ZoneFocusConfigs;
import com.aell.zonefocus.shaded.ui.Strings;
import com.aell.zonefocus.zone.ZoneBox;
import com.aell.zonefocus.zone.ZoneManager;
import com.aell.zonefocus.zone.ZoneState;
import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.malilib.hotkeys.IMouseInputHandler;
import fi.dy.masa.malilib.util.GuiUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ZoneTool implements IMouseInputHandler
{
    private static final ZoneTool INSTANCE = new ZoneTool();

    private boolean active;
    private boolean attackHeldLast;

    @Nullable private BlockPos anchor;
    @Nullable private BlockPos dragEnd;
    @Nullable private Direction hoveredFace;

    public static ZoneTool getInstance()
    {
        return INSTANCE;
    }

    public boolean isActive()
    {
        return this.active && ZoneFocusConfigs.TOOL_ENABLED.getBooleanValue()
                && ZoneFocusModule.isEnabled();
    }

    public void setActive(boolean value)
    {
        if (value == this.active)
        {
            return;
        }

        if (value && AxiomCompat.isAxiomToolActive())
        {
            Minecraft mc = Minecraft.getInstance();

            if (mc.player != null)
            {
                ZoneChat.notice(Component.literal("§7" + Strings.plain("zonefocus.message.axiom_tool_active")));
            }

            return;
        }

        this.active = value;
        this.anchor = null;
        this.dragEnd = null;
        this.hoveredFace = null;

        Minecraft mc = Minecraft.getInstance();

        boolean hintOnScreen = value && ZoneFocusConfigs.TOOL_KEY_HINTS.getBooleanValue();

        if (mc.player != null && ! hintOnScreen)
        {
            ZoneChat.notice(Component.literal(value
                    ? "§b" + Strings.plain("zonefocus.message.tool_on")
                    : "§7" + Strings.plain("zonefocus.message.tool_off")));
        }
    }

    public void toggle()
    {
        this.setActive(! this.active);
    }

    @Nullable
    public ZoneBox getPreviewBox()
    {
        if (this.anchor != null && this.dragEnd != null)
        {
            return new ZoneBox(this.anchor, this.dragEnd);
        }

        return null;
    }

    @Nullable
    public ZoneBox getEditedBox()
    {
        ZoneManager manager = ZoneManager.getInstance();

        return manager.getKind() == ZoneState.Kind.MANUAL ? manager.getActiveZone() : null;
    }

    @Nullable
    public Direction getHoveredFace()
    {
        return this.hoveredFace;
    }

    public void onRenderTick(Minecraft mc)
    {
        if (! this.isActive() || mc.player == null || mc.level == null || GuiUtils.getCurrentScreen() != null)
        {
            this.attackHeldLast = false;
            return;
        }

        boolean attackHeld = mc.options.keyAttack.isDown();
        BlockPos target = this.traceBlock(mc);

        if (attackHeld && ! this.attackHeldLast)
        {
            if (target != null)
            {
                this.anchor = target;
                this.dragEnd = target;
            }
        }
        else if (attackHeld && this.anchor != null)
        {
            BlockPos end = target != null ? target : this.traceOnAnchorPlane(mc);

            if (end != null)
            {
                this.dragEnd = end;
            }
        }
        else if (! attackHeld && this.attackHeldLast && this.anchor != null && this.dragEnd != null)
        {
            // Клик без протяжки (коробка 1×1×1) — не зона, а промах мимо
            if (! this.anchor.equals(this.dragEnd))
            {
                ZoneManager.getInstance().addToolBox(this.anchor, this.dragEnd);
            }

            this.anchor = null;
            this.dragEnd = null;
        }

        this.attackHeldLast = attackHeld;
        this.hoveredFace = this.anchor == null ? this.traceFace(mc) : null;
    }

    @Nullable
    private BlockPos traceBlock(Minecraft mc)
    {
        double reach = ZoneFocusConfigs.TOOL_REACH.getIntegerValue();
        Vec3 eyes = mc.player.getEyePosition(1.0f);

        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        BlockHitResult schematic = RayTraceUtils.traceToSchematicWorld(mc.player, reach, false, false);

        if (schematic != null && schematic.getType() == HitResult.Type.BLOCK)
        {
            best = schematic.getBlockPos();
            bestDist = eyes.distanceToSqr(schematic.getLocation());
        }

        HitResult world = mc.player.pick(reach, 0.0f, false);

        if (world instanceof BlockHitResult blockHit && world.getType() == HitResult.Type.BLOCK)
        {
            double dist = eyes.distanceToSqr(world.getLocation());

            if (dist < bestDist)
            {
                best = blockHit.getBlockPos();
            }
        }

        return best;
    }

    @Nullable
    private BlockPos traceOnAnchorPlane(Minecraft mc)
    {
        if (this.anchor == null)
        {
            return null;
        }

        Vec3 eyes = mc.player.getEyePosition(1.0f);
        Vec3 look = mc.player.getViewVector(1.0f);
        double planeY = this.anchor.getY() + 0.5;

        if (Math.abs(look.y) < 1.0e-4)
        {
            return null;
        }

        double t = (planeY - eyes.y) / look.y;
        double reach = ZoneFocusConfigs.TOOL_REACH.getIntegerValue();

        if (t <= 0.0 || t > reach)
        {
            return null;
        }

        Vec3 hit = eyes.add(look.scale(t));

        return BlockPos.containing(hit.x, planeY, hit.z);
    }

    @Nullable
    private Direction traceFace(Minecraft mc)
    {
        ZoneBox box = this.getEditedBox();

        if (box == null)
        {
            return null;
        }

        Vec3 eyes = mc.player.getEyePosition(1.0f);
        double reach = ZoneFocusConfigs.TOOL_REACH.getIntegerValue();
        Vec3 end = eyes.add(mc.player.getViewVector(1.0f).scale(reach));
        AABB aabb = box.toAabb();

        if (aabb.contains(eyes))
        {
            Vec3 look = mc.player.getViewVector(1.0f);
            return Direction.getApproximateNearest(look.x, look.y, look.z);
        }

        return aabb.clip(eyes, end)
                .map(hit -> faceOf(aabb, hit))
                .orElse(null);
    }

    @Nullable
    private static Direction faceOf(AABB aabb, Vec3 hit)
    {
        double epsilon = 1.0e-3;

        if (Math.abs(hit.x - aabb.minX) < epsilon) { return Direction.WEST; }
        if (Math.abs(hit.x - aabb.maxX) < epsilon) { return Direction.EAST; }
        if (Math.abs(hit.y - aabb.minY) < epsilon) { return Direction.DOWN; }
        if (Math.abs(hit.y - aabb.maxY) < epsilon) { return Direction.UP; }
        if (Math.abs(hit.z - aabb.minZ) < epsilon) { return Direction.NORTH; }
        if (Math.abs(hit.z - aabb.maxZ) < epsilon) { return Direction.SOUTH; }

        return null;
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double amount)
    {
        Minecraft mc = Minecraft.getInstance();

        if (! this.isActive() || GuiUtils.getCurrentScreen() != null || amount == 0.0)
        {
            return false;
        }

        ZoneBox box = this.getEditedBox();
        Direction face = this.hoveredFace;

        if (box == null || face == null)
        {
            return false;
        }

        ZoneBox moved = box.moveFace(face, amount > 0 ? 1 : -1);
        ZoneManager.getInstance().setActiveManualBox(moved);

        return true;
    }

    public boolean shouldSuppressBlockInteraction()
    {
        return this.isActive() && GuiUtils.getCurrentScreen() == null;
    }
}
