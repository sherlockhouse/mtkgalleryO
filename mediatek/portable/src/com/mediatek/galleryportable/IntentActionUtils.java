package com.mediatek.galleryportable;

import android.content.Intent;

import java.lang.reflect.Field;

public class IntentActionUtils {
    private static boolean sHasHdmiPlugAction = false;
    private static boolean sHasMediaUnsharedAction = false;
    private static boolean sHasChecked = false;

    public static String getHdmiPlugAction() {
        checkWetherSupport();
        if (sHasHdmiPlugAction) {
            return Intent.ACTION_HDMI_PLUG;
        } else {
            return "android.intent.action.HDMI_PLUG";
        }
    }

    public static String getMediaUnsharedAction() {
        checkWetherSupport();
        if (sHasMediaUnsharedAction) {
            return Intent.ACTION_MEDIA_UNSHARED;
        } else {
            return "android.intent.action.MEDIA_UNSHARED";
        }
    }

    private static void checkWetherSupport() {
        if (!sHasChecked) {
            try {
                Field field = Intent.class.getDeclaredField("ACTION_HDMI_PLUG");
                sHasHdmiPlugAction = (field != null);
            } catch (NoSuchFieldException e) {
                sHasHdmiPlugAction = false;
            }
            try {
                Field field = Intent.class.getDeclaredField("ACTION_MEDIA_UNSHARED");
                sHasMediaUnsharedAction = (field != null);
            } catch (NoSuchFieldException e) {
                sHasMediaUnsharedAction = false;
            }
            sHasChecked = true;
        }
    }

}
