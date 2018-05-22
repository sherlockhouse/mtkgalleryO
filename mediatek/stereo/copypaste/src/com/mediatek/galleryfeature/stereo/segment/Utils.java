package com.mediatek.galleryfeature.stereo.segment;

import android.os.StatFs;

import com.mediatek.util.Log;

/**
 * Utils class.
 */
public class Utils {
    private static final String TAG = Log.Tag("Cp/Utils");

    private static final int MIN_STORAGE_SPACE = 15 * 1024 * 1024;

    /**
     * Judge if storage is safe for saving (13M picture).
     * @param dirPath the directory path
     * @return ture if storage is sufficient
     */
    public static boolean isStorageSafeForSaving(String dirPath) {
        try {
            StatFs stat = new StatFs(dirPath);
            long spaceLeft = (long) (stat.getAvailableBlocks())
                    * stat.getBlockSize();
            Log.v(TAG, "storage available in this volume is: " + spaceLeft);
            if (spaceLeft < MIN_STORAGE_SPACE) {
                return false;
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            Log.d(TAG, "may sdcard unmounted (or switched) for this moment");
            return false;
        }
        return true;
    }
}
