package com.mediatek.galleryfeature.stereoentry;

import android.content.ContentValues;
import android.net.Uri;
import com.mediatek.gallerybasic.base.IFilterShowImageSaver;

import java.io.File;

/**
 * Stereo image should modify to sample image after edit operation.
 */

public class StereoFilterShowImageSaver implements IFilterShowImageSaver {
    /// Fo Stereo image, camera_refocus == 1.
    private static final String CAMERA_REFOCUS = "camera_refocus";
    @Override
    public void updateExifData(Uri uri) {

    }

    @Override
    public void updateMediaDatabase(File file, ContentValues values) {
        if (StereoField.sSupportStereo) {
            //Clear isRefocus column
            values.put(CAMERA_REFOCUS, 0);
        }
    }
}
