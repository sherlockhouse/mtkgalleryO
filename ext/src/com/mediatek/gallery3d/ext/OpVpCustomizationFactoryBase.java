package com.mediatek.gallery3d.ext;

import android.content.Context;

public class OpVpCustomizationFactoryBase {
    public IMovieExtension makeMovieExtension(Context context) {
        return new DefaultMovieExtension(context);
    }
}
