package com.aell.zonefocus.compat;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class MaterialHudCompat
{
    public static final int REQUIRED_API_VERSION = 1;

    /** С этой версии API проверку блоков ведёт Material HUD (Ctrl+B), своя у нас молчит. */
    public static final int VERIFICATION_API_VERSION = 5;

    private static boolean integrated;
    private static int apiVersion = -1;

    private MaterialHudCompat() {}

    public static void detect()
    {
        int detectedVersion;

        try
        {
            Class<?> api = Class.forName("com.aell.layerlist.api.LayerListApi");

            try
            {
                detectedVersion = api.getField("API_VERSION").getInt(null);
            }
            catch (NoSuchFieldException e)
            {
                detectedVersion = 0;
            }
        }
        catch (ClassNotFoundException | IllegalAccessException | LinkageError e)
        {
            detectedVersion = -1;
        }

        apiVersion = detectedVersion;
        integrated = detectedVersion >= REQUIRED_API_VERSION;
    }

    public static boolean isIntegrated()
    {
        return integrated;
    }

    /** Проверку блоков ведёт Material HUD: вкладки «Проверка» у нас нет, Ctrl+P — его Ctrl+B. */
    public static boolean ownsVerification()
    {
        return integrated && apiVersion >= VERIFICATION_API_VERSION;
    }

    /** 3D-превью схемы с зонами из библиотеки Material HUD (API 5). */
    public static boolean hasPreview()
    {
        return integrated && apiVersion >= VERIFICATION_API_VERSION;
    }

    /** С этой версии превью берёт участок из нескольких размещений, режет по высоте, смотрит сбоку и рисует мир Voxy. */
    public static final int SECTION_PREVIEW_API_VERSION = 6;

    /** Превью участка (API 6): любой участок в 3D, карты сверху/спереди/сбоку, срез, проекция точек для разрезов. */
    public static boolean hasSectionPreview()
    {
        return integrated && apiVersion >= SECTION_PREVIEW_API_VERSION;
    }

    public static void markBroken()
    {
        integrated = false;
    }
}
