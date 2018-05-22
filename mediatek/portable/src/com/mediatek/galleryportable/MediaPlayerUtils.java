package com.mediatek.galleryportable;

import android.media.MediaPlayer;

import java.lang.reflect.Method;

public class MediaPlayerUtils {

    private static final String TAG = "VP_MediaPlayerUtils";

    private static boolean sHasChecked = false;
    private static boolean sHasSetParameterFunction = false;

    private static boolean hasSetGetParameterFunction() {
        if (!sHasChecked) {
            try {
                Method method1 = MediaPlayer.class.getDeclaredMethod(
                        "setParameter", int.class, int.class);
                Method method2 = MediaPlayer.class.getDeclaredMethod(
                        "setParameter", int.class, String.class);
                Method method3 = MediaPlayer.class.getDeclaredMethod(
                        "getStringParameter", int.class);
                sHasSetParameterFunction = (method1 != null) && (method2 != null)
                        && (method3 != null);
            } catch (NoSuchMethodException e) {
                Log.e(TAG, "MediaPlayer#setParameter() or getStringParameter() is not found");
                sHasSetParameterFunction = false;
            }
            sHasChecked = true;
            Log.d(TAG, "hasSetGetParameterFunction = " + sHasSetParameterFunction);
        }
        return sHasSetParameterFunction;
    }

    public static void setParameter(MediaPlayer mp, int key, int value) {
        if (hasSetGetParameterFunction() && mp != null) {
            mp.setParameter(key, value);
        }
    }

    public static void setParameter(MediaPlayer mp, int key, String value) {
        if (hasSetGetParameterFunction() && mp != null) {
            mp.setParameter(key, value);
        }
    }

    public static String getStringParameter(MediaPlayer mp, int key) {
        if (hasSetGetParameterFunction() && mp != null) {
            return mp.getStringParameter(key);
        }
        return null;
    }
}
