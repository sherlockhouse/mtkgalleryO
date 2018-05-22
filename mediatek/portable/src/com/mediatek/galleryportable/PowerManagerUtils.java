package com.mediatek.galleryportable;

import android.os.PowerManager;

import java.lang.reflect.Method;

public class PowerManagerUtils {

    private static boolean sHasSetBacklightOffForWfdFunction = false;
    private static boolean sHasChecked = false;

    public static void setBacklightOffForWfd(PowerManager pm, boolean off) {
        if (hasSetBacklightOffForWfdFunction()) {
            pm.setBacklightOffForWfd(off);
        }
        return;
    }

    private static boolean hasSetBacklightOffForWfdFunction() {
        if (!sHasChecked) {
            try {
                Method method =
                        PowerManager.class.getDeclaredMethod("setBacklightOffForWfd",
                                boolean.class);
                sHasSetBacklightOffForWfdFunction = (method != null);
                sHasChecked = true;
            } catch (NoSuchMethodException e) {
                sHasSetBacklightOffForWfdFunction = false;
                sHasChecked = true;
            }
        }
        return sHasSetBacklightOffForWfdFunction;
    }
}
