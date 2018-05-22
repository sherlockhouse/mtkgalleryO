package com.mediatek.galleryportable;

import android.content.Context;

import com.mediatek.common.util.OperatorCustomizationFactoryLoader;
import com.mediatek.common.util.OperatorCustomizationFactoryLoader.OperatorFactoryInfo;

import java.util.ArrayList;
import java.util.List;

public class OpFactoryUtils {
    private static final String TAG = "Gallery2/OpFactoryUtils";

    private static boolean sIsOpFactoryLoaderExist = false;
    private static boolean sHasChecked = false;
    private static final List<OperatorFactoryInfo> sOpGalleryFactoryInfoList
            = new ArrayList<OperatorFactoryInfo>();
    private static final List<OperatorFactoryInfo> sOpVpFactoryInfoList
            = new ArrayList<OperatorFactoryInfo>();

    static {
        if (isOpFactoryLoaderExist()) {
            // add gallery operator factory info
            sOpGalleryFactoryInfoList.add(
                    new OperatorFactoryInfo(
                            // apk name
                            "OP01Gallery.apk",
                            // factory class name
                            "com.mediatek.gallery.op01.Op01GalleryCustomizationFactory",
                            // apk's package name
                            "com.mediatek.gallery.op01",
                            // operator id, OP01 has only one customization, only need to set it
                            "OP01"
                    ));

            // add video player operator factory info
            sOpVpFactoryInfoList.add(
                    new OperatorFactoryInfo(
                            "OP01Gallery.apk",
                            "com.mediatek.gallery.op01.Op01VideoCustomizationFactory",
                            "com.mediatek.gallery.op01",
                            "OP01"
                 ));

            // add video player operator factory info
            sOpVpFactoryInfoList.add(
                    new OperatorFactoryInfo(
                            "OP02Gallery.apk",
                            "com.mediatek.gallery.op02.Op02VideoCustomizationFactory",
                            "com.mediatek.gallery.op02",
                            "OP02"
                 ));
        }
    }

    // Gallery factory interface
    public static Object getOpGalleryFactory(
            Context context) {
        Object opGalleryFactory = null;
        if (isOpFactoryLoaderExist()) {
            opGalleryFactory = OperatorCustomizationFactoryLoader.loadFactory(
                            context, sOpGalleryFactoryInfoList);
        }
        Log.d(TAG, "<getOpGalleryFactory> factory = " + opGalleryFactory);
        return opGalleryFactory;
    }

    // Video Player factory interface
    public static Object getOpVpFactory(Context context) {
        Object opVpFactory = null;
        if (isOpFactoryLoaderExist()) {
            opVpFactory = OperatorCustomizationFactoryLoader.loadFactory(
                            context, sOpVpFactoryInfoList);
        }
        Log.d(TAG, "<getOpVpFactory> factory = " + opVpFactory);
        return opVpFactory;
    }

    private static boolean isOpFactoryLoaderExist() {
        if (!sHasChecked) {
            try {
                Class<?> clazz =
                        OpFactoryUtils.class.getClassLoader().loadClass(
                                "com.mediatek.common.util.OperatorCustomizationFactoryLoader");
                sIsOpFactoryLoaderExist = (clazz != null);
                sHasChecked = true;
            } catch (ClassNotFoundException e) {
                sIsOpFactoryLoaderExist = false;
                sHasChecked = true;
            }
        }
        return sIsOpFactoryLoaderExist;
    }
}

