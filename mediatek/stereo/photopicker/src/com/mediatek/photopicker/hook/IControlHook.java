package com.mediatek.photopicker.hook;

import android.content.Intent;
import android.os.Bundle;

/**
 * IControlHook interface for enlarge the scope of life-cycle of Android.
 */
public interface IControlHook {
    /**
     * For enlarge the scope of life-cycle of Android on onCreate.
     * @param savedInstanceState null
     */
    public void onCreate(Bundle savedInstanceState);

    /**
     * For enlarge the scope of life-cycle of Android on onResume.
     */
    public void onResume();

    /**
     * For enlarge the scope of life-cycle of Android on onPause.
     */
    public void onPause();

    /**
     * For enlarge the scope of life-cycle of Android on onDestroy.
     */
    public void onDestroy();

    /**
     * For enlarge the scope of life-cycle of Android on onActivityResult.
     * @param requestCode null
     * @param resultCode null
     * @param intent null
     */
    public void onActivityResult(int requestCode, int resultCode, Intent intent);
}
