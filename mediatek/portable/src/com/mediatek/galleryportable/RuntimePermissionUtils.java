package com.mediatek.galleryportable;

import android.content.Context;

public class RuntimePermissionUtils {

    private static String sDeniedPermissionString = "";
    private static boolean sHasChecked = false;

    public static String getDeniedPermissionString(Context context) {
        if (!sHasChecked) {
            try {
                sDeniedPermissionString =
                        context.getResources().getString(
                                com.mediatek.internal.R.string.denied_required_permission);
            } catch (Exception e) {
                sDeniedPermissionString =
                        "Permissions denied. You can change them in Settings -> Apps.";
            }
            sHasChecked = true;
        }
        return sDeniedPermissionString;
    }
}
