
package com.mediatek.galleryfeature.stereothumbnail;

import android.content.Context;
import android.content.res.Resources;

import com.mediatek.gallerybasic.base.ExtItem;
import com.mediatek.gallerybasic.base.Layer;
import com.mediatek.gallerybasic.base.MediaData;
import com.mediatek.gallerybasic.base.MediaMember;
import com.mediatek.gallerybasic.gl.GLIdleExecuter;
import com.mediatek.gallerybasic.util.Log;

public class StereoThumbMember extends MediaMember {

    private static final String TAG = "MtkGallery2/StereoThumbMember";
    private static final int TYPE_STEREO_THUMB = 2;
    public static int sType;
    private static final int PRIORITY = 10;
    private Layer mLayer;

    public StereoThumbMember(Context context) {
        super(context);
    }

    /**
     * Constructor.
     *
     * @param context the context is used for create layer.
     * @param exe     the exe is used for create layer.
     */
    public StereoThumbMember(Context context, GLIdleExecuter exe, Resources res) {
        super(context, exe, res);
    }

    @Override
    public boolean isMatching(MediaData md) {
        boolean isMatchStereoThumbRule = false;
        if (StereoField.sSupportStereo && md != null) {
            if (md.extFileds != null) {
                Object field = md.extFileds.getImageField(StereoField.TYPE_REFOCUS);
                if (field != null) {
                    isMatchStereoThumbRule = (int) field == TYPE_STEREO_THUMB;
                }
            }
        }
        Log.d(TAG, "<isMatching> return " + isMatchStereoThumbRule);
        return isMatchStereoThumbRule;
    }

    @Override
    public ExtItem getItem(MediaData md) {
        Log.d(TAG, "<getItem>");
        return new StereoThumbItem(md);
    }

    @Override
    public Layer getLayer() {
        if (mLayer == null) {
            mLayer = new StereoThumbLayer(mContext, mResources);
        }
        Log.d(TAG, "<getLayer> return StereoThumbLayer = " + mLayer);
        return mLayer;
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    protected void onTypeObtained(int type) {
        sType = type;
    }
}