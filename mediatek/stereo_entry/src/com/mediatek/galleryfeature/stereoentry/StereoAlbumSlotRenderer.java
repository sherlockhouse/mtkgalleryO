package com.mediatek.galleryfeature.stereoentry;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.mediatek.gallerybasic.base.IAlbumSlotRenderer;
import com.mediatek.gallerybasic.base.MediaData;
import com.mediatek.gallerybasic.gl.MGLCanvas;
import com.mediatek.gallerybasic.gl.MResourceTexture;

/**
 * Draw Stereo Icon in album page.
 */
public class StereoAlbumSlotRenderer implements IAlbumSlotRenderer {
    private final static int sStereoIconId = R.drawable.m_stereo_icon;
    private static MResourceTexture sStereoOverlay;
    private Context mContext;
    private Resources mResource;

    public StereoAlbumSlotRenderer(Context context, Resources resources) {
        mContext = context;
        mResource = resources;
        sStereoOverlay = new MResourceTexture(mResource, sStereoIconId);
    }

    public boolean renderContent(MGLCanvas canvas, int width, int height, MediaData data) {
        return false;
    }

    public boolean renderCover(MGLCanvas canvas, int width, int height, MediaData data) {
        if (data.extFileds != null) {
            Object field = data.extFileds.getImageField(StereoBottomControl
                    .TYPE_REFOCUS);
            if (null != field && (1 == (int) field)) {
                int side = Math.min(width, height) / 5;
                sStereoOverlay.draw(canvas, side / 4, height - side * 5 / 4, side, side);
                return true;
            }
        }
        return false;
    }
}
