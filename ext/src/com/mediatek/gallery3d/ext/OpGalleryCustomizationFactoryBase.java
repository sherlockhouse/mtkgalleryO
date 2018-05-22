package com.mediatek.gallery3d.ext;

import android.content.Context;

public class OpGalleryCustomizationFactoryBase {
    public IGalleryPickerExt makeGalleryPickerExt(Context context) {
        return new DefaultGalleryPickerExt(context);
    }

    public IImageOptionsExt makeImageOptionsExt(Context context) {
        return new DefaultImageOptionsExt(context);
    }
}
