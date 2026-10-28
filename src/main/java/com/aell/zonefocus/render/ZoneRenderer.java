package com.aell.zonefocus.render;

import com.aell.zonefocus.config.ZoneFocusConfigs;
import com.aell.zonefocus.tool.ZoneTool;
import com.aell.zonefocus.zone.ZoneBox;
import com.aell.zonefocus.zone.ZoneManager;
import com.aell.zonefocus.zone.ZoneScanner;
import com.aell.zonefocus.zone.model.Section;
import fi.dy.masa.malilib.util.StringUtils;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ZoneRenderer
{
    private static final int COLOR_ACTIVE      = 0xFF50D0FF;
    private static final int COLOR_ACTIVE_FILL = 0x1050D0FF;
    private static final int COLOR_OTHER       = 0x60FFFFFF;
    private static final int COLOR_DONE        = 0x8070E070;
    private static final int COLOR_PREVIEW     = 0xFFFFD050;
    private static final int COLOR_FACE        = 0xFFFF8040;

    public static int activeColor()
    {
        return COLOR_ACTIVE;
    }

    public static int otherColor()
    {
        return 0xFF000000 | (COLOR_OTHER & 0x00FFFFFF);
    }

    public static int doneColor()
    {
        return 0xFF000000 | (COLOR_DONE & 0x00FFFFFF);
    }

    private static final int COLOR_SECTION = 0x50C090FF;

    public static int otherSectionColor()
    {
        return 0xFF000000 | (COLOR_SECTION & 0x00FFFFFF);
    }

    private static int previewIndex = -1;

    private static boolean gridVisibleFromGui;

    public static void setPreviewIndex(int index)
    {
        previewIndex = index;
    }

    public static void setGridVisibleFromGui(boolean visible)
    {
        gridVisibleFromGui = visible;
    }

    private static boolean shouldShowOtherZones()
    {
        return gridVisibleFromGui || ZoneFocusConfigs.SHOW_ALL_ZONES.getBooleanValue();
    }

    public static void render()
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null)
        {
            return;
        }

        renderTool(mc);

        ZoneManager manager = ZoneManager.getInstance();
        Section section = manager.activeSection();

        if (section == null || section.hidden || ! manager.hasZones())
        {
            return;
        }

        boolean seeThrough = ZoneFocusConfigs.ZONE_SEE_THROUGH.getBooleanValue();
        boolean labels = ZoneFocusConfigs.SHOW_ZONE_LABELS.getBooleanValue();
        double rangeSq = Math.pow(ZoneFocusConfigs.ZONE_DRAW_RANGE.getIntegerValue(), 2);
        Vec3 playerPos = mc.player.position();

        List<ZoneBox> boxes = manager.zoneBoxes();
        int active = manager.getActiveIndex();

        if (boxes.isEmpty())
        {
            return;
        }

        if (ZoneFocusConfigs.SHOW_OTHER_SECTIONS.getBooleanValue())
        {
            renderOtherSections(manager, section, playerPos, rangeSq, seeThrough);
        }

        if (shouldShowOtherZones())
        {
            for (int i = 0; i < boxes.size(); ++i)
            {
                if (i == active || manager.isHidden(i))
                {
                    continue;
                }

                ZoneBox box = boxes.get(i);

                if (playerPos.distanceToSqr(box.center()) > rangeSq)
                {
                    continue;
                }

                ZoneScanner.Stats stats = ZoneScanner.getInstance().getStats(i);
                // Зона без блоков схемы «готовой» не считается
                boolean done = stats != null && stats.hasData() && stats.missing() == 0L;
                int color = i == previewIndex ? COLOR_PREVIEW : done ? COLOR_DONE : COLOR_OTHER;

                outline(box, GizmoStyle.stroke(color, i == previewIndex ? 2.5f : 1.0f), seeThrough, 0.0);

                if (labels)
                {
                    String text = stats != null
                            ? StringUtils.translate("zonefocus.world.zone_with_percent", i + 1, stats.percent())
                            : StringUtils.translate("zonefocus.world.zone", i + 1);
                    text(text, box.center(), color, seeThrough);
                }
            }
        }

        if (ZoneFocusConfigs.SHOW_ACTIVE_ZONE.getBooleanValue())
        {
            ZoneBox box = boxes.get(Math.floorMod(active, boxes.size()));
            outline(box, GizmoStyle.strokeAndFill(COLOR_ACTIVE, 3.0f, COLOR_ACTIVE_FILL), seeThrough, 0.01);

            if (labels)
            {
                String label = manager.getLabel();

                if (manager.sections().size() > 1)
                {
                    label = StringUtils.translate("zonefocus.world.section_prefix", manager.getActiveSectionIndex() + 1, label);
                }

                text(label, box.center().add(0.0, box.sizeY() / 2.0 + 0.6, 0.0), COLOR_ACTIVE, seeThrough);
            }
        }
    }

    /** Тонкие рамки зон остальных участков (не скрытых) — чтобы видеть, где что лежит. */
    private static void renderOtherSections(ZoneManager manager, Section active, Vec3 playerPos, double rangeSq, boolean seeThrough)
    {
        for (int s = 0; s < manager.sections().size(); ++s)
        {
            Section section = manager.sections().get(s);

            if (section == active || section.hidden)
            {
                continue;
            }

            List<ZoneBox> boxes = manager.zoneBoxes(section);

            for (int i = 0; i < boxes.size(); ++i)
            {
                ZoneBox box = boxes.get(i);

                if (section.hiddenZones.contains(i) || playerPos.distanceToSqr(box.center()) > rangeSq)
                {
                    continue;
                }

                outline(box, GizmoStyle.stroke(COLOR_SECTION, 1.0f), seeThrough, 0.0);
            }
        }
    }

    private static void renderTool(Minecraft mc)
    {
        ZoneTool tool = ZoneTool.getInstance();

        if (! tool.isActive())
        {
            return;
        }

        boolean seeThrough = ZoneFocusConfigs.ZONE_SEE_THROUGH.getBooleanValue();
        ZoneBox preview = tool.getPreviewBox();

        if (preview != null)
        {
            outline(preview, GizmoStyle.strokeAndFill(COLOR_PREVIEW, 3.0f, 0x18FFD050), seeThrough, 0.01);
            text(StringUtils.translate("zonefocus.world.size",
                            preview.sizeX(), preview.sizeY(), preview.sizeZ()),
                    preview.center().add(0.0, preview.sizeY() / 2.0 + 0.6, 0.0), COLOR_PREVIEW, seeThrough);
        }

        Direction face = tool.getHoveredFace();
        ZoneBox current = tool.getEditedBox();

        if (face != null && current != null)
        {
            AABB aabb = current.toAabb();
            AABB slab = switch (face)
            {
                case WEST -> new AABB(aabb.minX, aabb.minY, aabb.minZ, aabb.minX + 0.06, aabb.maxY, aabb.maxZ);
                case EAST -> new AABB(aabb.maxX - 0.06, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ);
                case DOWN -> new AABB(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.minY + 0.06, aabb.maxZ);
                case UP -> new AABB(aabb.minX, aabb.maxY - 0.06, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ);
                case NORTH -> new AABB(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.minZ + 0.06);
                case SOUTH -> new AABB(aabb.minX, aabb.minY, aabb.maxZ - 0.06, aabb.maxX, aabb.maxY, aabb.maxZ);
            };

            GizmoProperties gizmo = Gizmos.cuboid(slab, GizmoStyle.strokeAndFill(COLOR_FACE, 2.0f, 0x40FF8040));

            if (seeThrough)
            {
                gizmo.setAlwaysOnTop();
            }
        }
    }

    private static void outline(ZoneBox box, GizmoStyle style, boolean seeThrough, double grow)
    {
        GizmoProperties gizmo = Gizmos.cuboid(box.toAabb(grow), style);

        if (seeThrough)
        {
            gizmo.setAlwaysOnTop();
        }
    }

    private static void text(@Nullable String text, Vec3 pos, int color, boolean seeThrough)
    {
        if (text == null || text.isEmpty())
        {
            return;
        }

        GizmoProperties gizmo = Gizmos.billboardText(text, pos, TextGizmo.Style.forColorAndCentered(color));

        if (seeThrough)
        {
            gizmo.setAlwaysOnTop();
        }
    }
}
