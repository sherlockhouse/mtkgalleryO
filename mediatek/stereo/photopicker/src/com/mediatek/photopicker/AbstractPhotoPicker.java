package com.mediatek.photopicker;

import android.app.Activity;

/**
 * Abstract photoPicker.
 */
public abstract class AbstractPhotoPicker {
    /**
     * Launch PhotoPicker.
     * @param activity
     *          The activity who want to use PhotoPicker tool<br>
     * @param config
     *          The configuration that contain default value and three hook<br>
     */
    public void launch(Activity activity, PhotoPicker.Config config) {
        PhotoPicker.launch(activity, config);
    }

    /**
     * As inlet for third-part applications that need help from PhotoPicker.
     */
    public abstract void launchPhotoPicker();
}
