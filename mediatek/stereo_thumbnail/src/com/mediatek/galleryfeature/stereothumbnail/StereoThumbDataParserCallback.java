package com.mediatek.galleryfeature.stereothumbnail;

import android.text.TextUtils;

import com.mediatek.gallerybasic.base.IDataParserCallback;
import com.mediatek.gallerybasic.base.MediaData;
import com.mediatek.gallerybasic.util.Log;

import java.io.File;

public class StereoThumbDataParserCallback implements IDataParserCallback {

    private static final String TAG = "MtkGallery2/StereoThumbDataParserCallback";

    @Override
    public void onPostParse(MediaData md) {
        Log.d(TAG, "<onPostParse> md = " + md);
        int camera_refocus = 0;
        if (StereoField.sSupportStereo && md != null) {
            if (md.extFileds != null) {
                Object field = md.extFileds.getImageField(StereoField.TYPE_REFOCUS);
                if (field != null) {
                    camera_refocus = (int) field;
                }
            }
            // if stereo image generated, try to delete stereo thumbnail
            if (camera_refocus == 1 && md.filePath != null) {
                String[] segments = md.filePath.split("/");
                segments[segments.length - 1]
                        = "." + segments[segments.length - 1] + ".stereothumb";
                String thumbnailPath = TextUtils.join("/", segments);
                File thumbnailFile = new File(thumbnailPath);
                if (thumbnailFile.exists() && thumbnailFile.delete()) {
                    Log.d(TAG, "deleted successfully: " + thumbnailFile);
                }
            }
        }
    }
}
