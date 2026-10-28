package com.aell.zonefocus.compat;

import com.aell.layerlist.api.LayerListApi;
import com.aell.zonefocus.zone.ZoneScope;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class MaterialHudBridge
{
    private MaterialHudBridge() {}

    public static void install()
    {
        // Кнопки «Зоны» в шапке настроек соседа больше нет: всё, что было на своём экране зон,
        // стоит на его вкладке «Zone Focus» — вторая дверь в те же настройки только путала
        LayerListApi.setCountScope(ZoneScope.getInstance());
    }

    /** Экран настроек соседа, открытый на вкладке «Zone Focus». */
    public static @Nullable Screen settingsScreen(@Nullable Screen parent, @Nullable String backLabelKey)
    {
        return LayerListApi.createSettings(parent, backLabelKey, LayerListApi.TAB_ZONE_FOCUS);
    }

    public static void markDirty()
    {
        LayerListApi.markDirty();
    }

    public static boolean isCountingZone()
    {
        return LayerListApi.isScopeActive() && LayerListApi.hasData()
                && LayerListApi.getBlocksTotal() > 0L;
    }

    public static long blocksMissing()
    {
        return LayerListApi.getBlocksMissing();
    }

    /** Проверка Material HUD; ответ он сам пишет в чат. */
    public static String toggleVerification()
    {
        return LayerListApi.toggleVerification().getString();
    }

    public static boolean isVerifying()
    {
        return LayerListApi.isVerificationActive();
    }

    public static String verificationSummary()
    {
        return LayerListApi.getVerificationSummary();
    }

    // ===== 3D-превью для «Управления зонами» (API 5). Ручка — объект Material HUD, наружу как Object =====

    /** Новое превью или {@code null}, если Material HUD старее. */
    public static @Nullable Object createPreview()
    {
        try
        {
            return LayerListApi.createZonePreview();
        }
        catch (LinkageError e)
        {
            return null;
        }
    }

    public static void previewFile(Object handle, @Nullable java.nio.file.Path file)
    {
        ((com.aell.layerlist.api.ZonePreview) handle).setFile(file);
    }

    public static void previewZones(Object handle, int @Nullable [][] boxes, int active, int @Nullable [] tones, boolean draft)
    {
        ((com.aell.layerlist.api.ZonePreview) handle).setZones(boxes, active, tones, draft);
    }

    public static void previewProgress(Object handle, int @Nullable [][] boxes, int active, int @Nullable [] percents)
    {
        ((com.aell.layerlist.api.ZonePreview) handle).setZones(boxes, active, percents);
    }

    /** Готовность по блокам в 3D (непостроенное — «призраком»); {@code null} — без неё. */
    public static void previewBuild(Object handle, @Nullable fi.dy.masa.litematica.schematic.placement.SchematicPlacement placement)
    {
        ((com.aell.layerlist.api.ZonePreview) handle).setBuildPlacement(placement);
    }

    public static int previewBuiltPercent(Object handle)
    {
        return ((com.aell.layerlist.api.ZonePreview) handle).builtPercent();
    }

    /** Качество 3D-превью Material HUD: истина — «Качество», ложь — «Оптимизация». */
    public static boolean previewQuality()
    {
        return LayerListApi.isPreviewQuality();
    }

    public static void setPreviewQuality(boolean quality)
    {
        LayerListApi.setPreviewQuality(quality);
    }

    public static void previewRender(Object handle, fi.dy.masa.malilib.render.GuiContext ctx, int x, int y, int w, int h,
            int mouseX, int mouseY)
    {
        ((com.aell.layerlist.api.ZonePreview) handle).render(ctx, x, y, w, h, mouseX, mouseY);
    }

    public static boolean previewClicked(Object handle, double x, double y, int button)
    {
        return ((com.aell.layerlist.api.ZonePreview) handle).mouseClicked(x, y, button);
    }

    public static boolean previewDragged(Object handle, double x, double y, int button, double dx, double dy)
    {
        return ((com.aell.layerlist.api.ZonePreview) handle).mouseDragged(x, y, button, dx, dy);
    }

    public static boolean previewReleased(Object handle, double x, double y, int button)
    {
        return ((com.aell.layerlist.api.ZonePreview) handle).mouseReleased(x, y, button);
    }

    public static boolean previewScrolled(Object handle, double x, double y, double amount)
    {
        return ((com.aell.layerlist.api.ZonePreview) handle).mouseScrolled(x, y, amount);
    }

    /** Зона, по которой щёлкнули без протяжки, или −1 (читается один раз). */
    public static int previewClickedZone(Object handle)
    {
        return ((com.aell.layerlist.api.ZonePreview) handle).clicked();
    }

    public static int previewHovered(Object handle)
    {
        return ((com.aell.layerlist.api.ZonePreview) handle).hovered();
    }

    public static String previewStatus(Object handle)
    {
        return ((com.aell.layerlist.api.ZonePreview) handle).status();
    }

    public static void previewClose(Object handle)
    {
        ((com.aell.layerlist.api.ZonePreview) handle).close();
    }

    /** Вид камеры 3D — имя предустановки Material HUD: {@code ISOMETRIC}, {@code TOP}, {@code FRONT}. */
    public static void previewView(Object handle, String view)
    {
        ((com.aell.layerlist.api.ZonePreview) handle).setView(com.aell.layerlist.api.ZonePreview.View.valueOf(view));
    }

    public static String previewViewName(Object handle)
    {
        return ((com.aell.layerlist.api.ZonePreview) handle).view().name();
    }

    /** Превью — карта сверху для 2D: снимок схемы строго сверху в повороте размещения, мышью не крутится. */
    public static void previewMap(Object handle, fi.dy.masa.litematica.schematic.placement.SchematicPlacement placement)
    {
        ((com.aell.layerlist.api.ZonePreview) handle).setMap(placement);
    }

    /** Срез карты: блоки выше слоя {@code topY} схемы не рисуются; меньше нуля — без среза. */
    public static void previewMapCut(Object handle, int topY)
    {
        ((com.aell.layerlist.api.ZonePreview) handle).setMapCut(topY);
    }

    // ===== Превью участка (API 6): только при MaterialHudCompat.hasSectionPreview() =====

    /** Участок из размещений: снимок с мира схем по их общему габариту; тот же состав — без пересборки. */
    public static void previewPlacements(Object handle, java.util.List<fi.dy.masa.litematica.schematic.placement.SchematicPlacement> placements)
    {
        ((com.aell.layerlist.api.ZonePreview) handle).setPlacements(placements);
    }

    /** Начало координат зон в режиме участка (минимальный угол габарита в мире); {@code null} — модели нет. */
    public static net.minecraft.core.@Nullable BlockPos previewWorldMin(Object handle)
    {
        return ((com.aell.layerlist.api.ZonePreview) handle).worldMin();
    }

    /** Точка в блоках от начала координат зон → {x, y, глубина} экрана; {@code null} — модели нет. */
    public static float @Nullable [] previewProject(Object handle, float x, float y, float z)
    {
        return ((com.aell.layerlist.api.ZonePreview) handle).project(x, y, z);
    }

    /** Габарит модели в блоках {sizeX, sizeY, sizeZ}; {@code null} — модели нет. */
    public static int @Nullable [] previewSize(Object handle)
    {
        return ((com.aell.layerlist.api.ZonePreview) handle).size();
    }

    public static boolean previewHasModel(Object handle)
    {
        return ((com.aell.layerlist.api.ZonePreview) handle).hasModel();
    }

    /** Срез по высоте в любом виде: блоки выше {@code topY} (от низа модели) не рисуются; меньше нуля — без среза. */
    public static void previewLayerCut(Object handle, int topY)
    {
        ((com.aell.layerlist.api.ZonePreview) handle).setLayerCut(topY);
    }

    /** Карта без мыши в осях мира: {@code TOP}, {@code FRONT} или {@code SIDE}. */
    public static void previewMapView(Object handle, String view)
    {
        ((com.aell.layerlist.api.ZonePreview) handle).setMapView(com.aell.layerlist.api.ZonePreview.View.valueOf(view));
    }

    /** Блок под правым щелчком без протяжки (в блоках от начала координат зон) или {@code null}; читается один раз. */
    public static int @Nullable [] previewRightClicked(Object handle)
    {
        return ((com.aell.layerlist.api.ZonePreview) handle).rightClicked();
    }

    /** Снятие снимка участка, 0…1. */
    public static float previewSnapshotProgress(Object handle)
    {
        return ((com.aell.layerlist.api.ZonePreview) handle).snapshotProgress();
    }

    public static int toggleZoneSearch()
    {
        if (! LayerListApi.isScopeActive() || LayerListApi.getEntries().isEmpty())
        {
            return -1;
        }

        return LayerListApi.toggleStorageSearch(LayerListApi.getEntries());
    }
}
