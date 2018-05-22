package com.mediatek.gallery3d.util;

import android.content.Context;

import com.mediatek.gallery3d.ext.IGalleryPickerExt;
import com.mediatek.gallery3d.ext.IImageOptionsExt;
import com.mediatek.gallery3d.ext.OperatorPlugin;

public class GalleryPluginUtils {
    private static final String TAG = "Gallery2/GalleryPluginUtils";
    private static IImageOptionsExt sImageOptions;
    private volatile static IGalleryPickerExt sGalleryPicker;
    private volatile static Context sContext = null;

    public static void initialize(Context context) {
        sContext = context;
    }

    public static IImageOptionsExt getImageOptionsPlugin() {
        if (sImageOptions == null) {
            synchronized (GalleryPluginUtils.class) {
                if (sImageOptions == null) {
                    sImageOptions = OperatorPlugin.getOpGalleryFactory(sContext)
                            .makeImageOptionsExt(sContext);
                    Log.d(TAG, "<getImageOptionsPlugin> sImageOptions = " + sImageOptions);
                }
            }
        }
        return sImageOptions;
    }

    /**
     * Get gallery picker plugin.
     * @return IGalleryPickerExt Gallery picker
     */
    public static IGalleryPickerExt getGalleryPickerPlugin() {
        if (sGalleryPicker == null) {
            synchronized (GalleryPluginUtils.class) {
                if (sGalleryPicker == null) {
                    sGalleryPicker = OperatorPlugin.getOpGalleryFactory(sContext)
                            .makeGalleryPickerExt(sContext);
                    Log.d(TAG, "<getGalleryPickerPlugin> sGalleryPicker = " + sGalleryPicker);
                }
            }
        }
        return sGalleryPicker;
    }
}
