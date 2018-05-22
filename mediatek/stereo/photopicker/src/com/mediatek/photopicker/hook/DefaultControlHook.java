package com.mediatek.photopicker.hook;

import android.content.Intent;
import android.os.Bundle;

/**
 * Null implementation for IControlHook.
 */
public class DefaultControlHook implements IControlHook {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // null implementation for enlarge the scope of life-cycle of Android on onCreate.
    }

    @Override
    public void onResume() {
        // null implementation for enlarge the scope of life-cycle of Android on onResume.
    }

    @Override
    public void onPause() {
        // null implementation for enlarge the scope of life-cycle of Android on onPause.
    }

    @Override
    public void onDestroy() {
        // null implementation for enlarge the scope of life-cycle of Android on onDestroy.
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // null implementation for enlarge the scope of life-cycle of Android on onActivityResult.
    }
}
