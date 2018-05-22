package com.mediatek.photopicker.utils;

import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * SystemPropertyUtils.
 */
public class SystemPropertyUtils {
    private static final String TAG = "PhotoPicker/SystemPropertyUtils";
    private static boolean sIsSystemPropertiesExist = false;
    private static Class<?> sSystemPropertiesClass;
    private static Method sGetIntMethod;
    private static Method sGetMethod;

    static {
        try {
            Class<?> sSystemPropertiesClass = SystemPropertyUtils.class.getClassLoader().
                    loadClass("android.os.SystemProperties");
            sGetIntMethod = sSystemPropertiesClass.getDeclaredMethod("getInt",
                    String.class, int.class);
            sGetIntMethod.setAccessible(true);
            sGetMethod = sSystemPropertiesClass.getDeclaredMethod("get", String.class);
            sGetMethod.setAccessible(true);
            sIsSystemPropertiesExist = (sSystemPropertiesClass != null);
        } catch (ClassNotFoundException e1) {
            sIsSystemPropertiesExist = false;
            Log.e(TAG, "ClassNotFoundException", e1);
        } catch (NoSuchMethodException e2) {
            sIsSystemPropertiesExist = false;
            Log.e(TAG, "NoSuchMethodException", e2);
        }
    }

    /**
     * Get int properties.
     * @param key key
     * @param defaultValue default value
     * @return value
     */
    public static int getInt(String key, int defaultValue) {
        if (sIsSystemPropertiesExist && sGetIntMethod != null) {
            try {
                return (int) sGetIntMethod.invoke(null, key, defaultValue);
            } catch (IllegalAccessException e1) {
                return defaultValue;
            } catch (InvocationTargetException e2) {
                return defaultValue;
            }
        } else {
            return defaultValue;
        }
    }

    /**
     * Get properties.
     * @param key key
     * @return value
     */
    public static String get(String key) {
        if (sIsSystemPropertiesExist && sGetMethod != null) {
            try {
                return (String) sGetMethod.invoke(null, key);
            } catch (IllegalAccessException e1) {
                return "";
            } catch (InvocationTargetException e2) {
                return "";
            }
        } else {
            return "";
        }
    }
}
