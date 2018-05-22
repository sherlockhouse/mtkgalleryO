package com.mediatek.galleryfeature.stereoentry;

import android.os.Environment;
import com.mediatek.gallerybasic.base.ExtFields;
import com.mediatek.gallerybasic.base.IFieldDefinition;
import com.mediatek.galleryportable.SystemPropertyUtils;

import java.io.File;


public class StereoField implements IFieldDefinition {
    public final static String TYPE_REFOCUS = "camera_refocus";
    public static final String IMAGE_STEREO_SUPPORT_PROPERTY = "ro.mtk_cam_img_refocus_support";
    public static boolean sSupportStereo = false;

    static {
        sSupportStereo = SystemPropertyUtils.get(IMAGE_STEREO_SUPPORT_PROPERTY).equals("1");
        if (!sSupportStereo) {
            File file = new File(Environment.getExternalStorageDirectory(),
                    "SUPPORT_STEREO");
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
