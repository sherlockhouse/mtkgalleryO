package com.mediatek.gallery3d.ext;

import android.content.Context;

import com.mediatek.gallery3d.util.Log;
import com.mediatek.galleryportable.OpFactoryUtils;

import java.util.ArrayList;
import java.util.List;

public class OperatorPlugin {
    private static final String TAG = "Gallery2/OperatorPlugin";

    private volatile static OpGalleryCustomizationFactoryBase sOpGalleryFactory = null;
    private volatile static OpVpCustomizationFactoryBase sOpVpFactory = null;

    // Gallery factory interface
    public static OpGalleryCustomizationFactoryBase getOpGalleryFactory(
            Context context) {
        if (sOpGalleryFactory == null) {
            synchronized (OpFactoryUtils.class) {
                if (sOpGalleryFactory == null) {
                    Object obj = OpFactoryUtils.getOpGalleryFactory(context);
                    if (obj == null) {
                        obj = new OpGalleryCustomizationFactoryBase();
                    }
                    sOpGalleryFactory = (OpGalleryCustomizationFactoryBase) obj;
                    Log.d(TAG, "<getOpGalleryFactory> factory = " + sOpGalleryFactory);
                }
            }
        }
        return sOpGalleryFactory;
    }

    // Video Player factory interface
    public static OpVpCustomizationFactoryBase getOpVpFactory(Context context) {
        if (sOpVpFactory == null) {
            synchronized (OperatorPlugin.class) {
                if (sOpVpFactory == null) {
                    Object obj = OpFactoryUtils.getOpVpFactory(context);
                    if (obj == null) {
                        obj = new OpVpCustomizationFactoryBase();
                    }
                    sOpVpFactory = (OpVpCustomizationFactoryBase) obj;
                    Log.d(TAG, "<getOpVpFactory> factory = " + sOpVpFactory);
                }
            }
        }
        return sOpVpFactory;
    }
}

