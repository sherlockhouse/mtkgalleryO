package com.mediatek.refocus;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.mediatek.util.Log;

import java.util.ArrayList;

/**
 * Check and request permission.
 */
public class PermissionUtil {
    private static final String TAG = Log.Tag("Rf/PermissionUtil");

    /**
     * Check WRITE_EXTERNAL_STORAGE/READ_EXTERNAL_STORAGE permissions for PhotoPicker.
     * @param activity GalleryActivity
     * @return If all permissions are granted, return true.
     *         If one of them is denied, request permissions and return false.
     */
    public static boolean checkAndRequestForPhotoPicker(Activity activity) {
        // get permissions needed in current scenario
        ArrayList<String> permissionsNeeded = new ArrayList<String>();
        permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        // check status of permissions, get which permissions need to request
        ArrayList<String> permissionsNeedRequest = new ArrayList<String>();
        for (String permission : permissionsNeeded) {
            if (ContextCompat.checkSelfPermission(activity, permission)
                    == PackageManager.PERMISSION_GRANTED) {
                continue;
            }
            permissionsNeedRequest.add(permission);
        }

        // request permissions
        if (permissionsNeedRequest.size() == 0) {
            Log.d(TAG, "<checkAndRequestForGallery> all permissions are granted");
            return true;
        } else {
            Log.d(TAG, "<checkAndRequestForGallery> not all permissions are granted, reuqest");
            String[] permissions = new String[permissionsNeedRequest.size()];
            permissions = permissionsNeedRequest.toArray(permissions);
            ActivityCompat.requestPermissions(activity, permissions, 0);
            return false;
        }
    }

    /**
     * Check if all permissions in String[] are granted.
     * @param permissions A group of permissions
     * @param grantResults The granted status of permissions
     * @return If all permissions are granted, return true, or else return false.
     */
    public static boolean isAllPermissionsGranted(String[] permissions, int[] grantResults) {
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Show toast after permission is denied.
     * @param context current application environment
     */
    public static void showDeniedPrompt(Context context) {
        Toast.makeText(context, getDeniedPermissionString(), Toast.LENGTH_SHORT).show();
    }

    private static String getDeniedPermissionString() {
        return "Permissions denied.\nCan change in Settings-Apps-Permission.";
    }
}
