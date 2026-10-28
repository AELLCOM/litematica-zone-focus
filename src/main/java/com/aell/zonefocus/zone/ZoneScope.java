package com.aell.zonefocus.zone;

import com.aell.layerlist.api.CountScope;
import com.aell.zonefocus.ZoneFocusModule;
import com.aell.zonefocus.config.ZoneFocusConfigs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.malilib.util.position.LayerRange;
import java.util.List;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public class ZoneScope implements CountScope
{
    private static final ZoneScope INSTANCE = new ZoneScope();

    public static ZoneScope getInstance()
    {
        return INSTANCE;
    }

    @Override
    public boolean isActive()
    {
        return ZoneFocusModule.isEnabled()
                && ZoneFocusConfigs.COUNT_BY_ZONE.getBooleanValue()
                && Minecraft.getInstance().level != null
                // Есть участки — скоуп активен; у пустого или скрытого активного участка getBoxes пуст, и
                // Material HUD показывает «пусто», а не весь мир
                && ZoneManager.getInstance().hasSections();
    }

    @Override
    public List<Box> getBoxes()
    {
        return ZoneManager.getInstance().getCountBoxes();
    }

    @Override
    public String getSignature()
    {
        return ZoneManager.getInstance().getSignature();
    }

    /** Ширина подписи в HUD: Material HUD её не ограничивает, поэтому обрезаем сами. */
    private static final int LABEL_MAX_WIDTH = 140;

    @Override
    public String getLabel()
    {
        ZoneManager manager = ZoneManager.getInstance();
        String label = hudLabel(manager);

        if (this.applyLayerRange())
        {
            String layer = layerLabel();

            if (! layer.isEmpty())
            {
                label = label + " · " + layer;
            }
        }

        return truncate(label);
    }

    /** Короткая форма для HUD: при одном участке — как раньше, при нескольких — «У2 · З7/24», «У2 · пуст». */
    private static String hudLabel(ZoneManager manager)
    {
        var section = manager.activeSection();

        if (section == null)
        {
            return "";
        }

        boolean several = manager.sections().size() > 1;
        int number = manager.getActiveSectionIndex() + 1;

        if (section.hidden)
        {
            return several ? fi.dy.masa.malilib.util.StringUtils.translate("zonefocus.hud.section_hidden", number)
                    : com.aell.zonefocus.shaded.ui.Strings.plain("zonefocus.hud.hidden");
        }

        if (! manager.hasZones() || ! manager.isNonEmpty(section))
        {
            return several ? fi.dy.masa.malilib.util.StringUtils.translate("zonefocus.hud.section_empty", number)
                    : com.aell.zonefocus.shaded.ui.Strings.plain("zonefocus.hud.empty");
        }

        String label = manager.getLabel();
        return several ? fi.dy.masa.malilib.util.StringUtils.translate("zonefocus.hud.section_prefix", number, shorten(label)) : label;
    }

    /** «Зона 7/24» → «З7/24», «Подрегион 7/24» → «П7/24»: первая буква слова плюс номера. */
    private static String shorten(String label)
    {
        int space = label.indexOf(' ');

        if (space <= 0 || space == label.length() - 1)
        {
            return label;
        }

        return label.substring(0, 1) + label.substring(space + 1);
    }

    private static String truncate(String label)
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc == null || mc.font == null || mc.font.width(label) <= LABEL_MAX_WIDTH)
        {
            return label;
        }

        return mc.font.plainSubstrByWidth(label, LABEL_MAX_WIDTH - mc.font.width("…")) + "…";
    }

    private static String layerLabel()
    {
        LayerRange range = DataManager.getRenderLayerRange();
        String axis = range.getAxis().getName().toUpperCase(Locale.ROOT);

        return switch (range.getLayerMode())
        {
            case SINGLE_LAYER -> axis + " = " + range.getLayerSingle();
            case LAYER_RANGE -> axis + " " + range.getLayerRangeMin() + " .. " + range.getLayerRangeMax();
            case ALL_ABOVE -> axis + " >= " + range.getLayerAbove();
            case ALL_BELOW -> axis + " <= " + range.getLayerBelow();
            default -> "";
        };
    }

    @Override
    public List<String> getFooterLines()
    {
        return ZoneManager.getInstance().getFooterLines();
    }

    @Override
    public boolean applyLayerRange()
    {
        return ! ZoneFocusConfigs.IGNORE_LAYERS_IN_ZONE.getBooleanValue();
    }

    @Override
    public boolean searchWholeScope()
    {
        return ZoneFocusConfigs.SEARCH_WHOLE_ZONE.getBooleanValue();
    }
}
