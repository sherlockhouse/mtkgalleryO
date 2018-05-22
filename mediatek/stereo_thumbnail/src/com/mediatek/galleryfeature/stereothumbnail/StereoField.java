package com.mediatek.galleryfeature.stereothumbnail;

import android.os.Environment;
import com.mediatek.gallerybasic.base.ExtFields;
import com.mediatek.gallerybasic.base.IFieldDefinition;
import com.mediatek.galleryportable.SystemPropertyUtils;

import java.io.File;


public class StereoField implements IFieldDefinition {
    public final static String TYPE_REFOCUS = "camera_refocus";
    public static final String THUMBNAIL_STEREO_SUPPORT = "ro.mtk_cam_stereo_camera_support";
    public static boolean sSupportStereo = false;

    static {
        sSupportStereo = SystemPropertyUtils.get(THUMBNAIL_STEREO_SUPPORT).equals("1");
        if (!sSupportStereo) {
            File file = new File(Environment.getExternalStorageDirectory(),
                    "SUPPORT_STEREO_THUMBNAIL");
            sSupportStereo = file.exists();
        }
    }

    public StereoField() {

    }

    @Override
    public void onFieldDefine() {
        if (sSupportStereo) {
            ExtFields.addImageFiled(TYPE_REFOCUS);
        }
    }
}
