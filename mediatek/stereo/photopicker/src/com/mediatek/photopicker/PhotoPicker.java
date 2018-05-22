package com.mediatek.photopicker;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.mediatek.photopicker.hook.IControlHook;
import com.mediatek.photopicker.hook.IModelHook;
import com.mediatek.photopicker.hook.IViewHook;
import com.mediatek.photopicker.permission.PermissionUtil;
import com.mediatek.photopicker.utils.Log;
import com.mediatek.photopicker.utils.Utils;

import java.io.File;

/**
 * PhotoPicker activity.
 */
public class PhotoPicker extends Activity {
    private static final String TAG = "PhotoPicker/PhotoPicker";

    public static final int REQUEST_CODE = 1603;
    protected static Config sConfig = null;
    protected Config mConfig = new Config();
    private static final int NUM_COLUMNS_PORTRAIT = 3;
    private static final int NUM_COLUMNS_LANDSCAPE = 4;

    private Uri mUri = Uri.parse("content://media/external/");
    private PhotoPickerView mPhotoPickerView = null;
    private ContentObserver mDataObserver;

    private View mMainView = null;
    private boolean mGranted;

    /**
     * Configuration for PhotoPicker.
     */
    public static class Config {
        public static final int INCLUDE_TYPE_IMAGE = 1;
        public static final int INCLUDE_TYPE_VIDEO = 1 << 1;
        public static final int INCLUDE_TYPE_ALL = INCLUDE_TYPE_IMAGE | INCLUDE_TYPE_VIDEO;
        public static final int DATA_MODE_MEDIA_STORAGE = 0;
        public static final int DATA_MODE_CUSTOMIZATION = 1;

        public Drawable actionBarIcon;
        public String title;
        public String subTitle;
        public int includeTypes;
        public int dataMode;

        // controlHook is called in corresponding life cycle callback of PhotoPicker
        public IControlHook controlHook;
        // viewHook is called in PhotoPickerView & SlotImageview
        public IViewHook viewHook;
        // modelHook is called in PickerAdapter
        public IModelHook modelHook;
    }

    /**
     * Launch PhotoPicker activity by third-party application.
     * @param activity
     *          Primitive activity which can launch {@link PhotoPicker}.<br>
     * @param config
     *          Configuration file that PhotoPicker.Config.<br>
     */
    public static void launch(Activity activity, Config config) {
        Log.d(TAG, "<launch>");
        sConfig = config;
        Class cls = PhotoPicker.class;
        if (activity instanceof PhotoPicker) {
            cls = activity.getClass();
        }
        Intent intent = new Intent(activity, cls);
        activity.startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (mGranted) {
            adjustOrientation();
        }
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            Log.d(TAG, "<onOptionsItemSelected> " + "Home key down && do finish");
            finish();
            return false;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    /**
     * Notify thread to reload data.
     */
    public void reload() {
        mPhotoPickerView.reload();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "<onCreate> " + Utils.BEGIN);
        long timeStart = System.currentTimeMillis();

        if (Utils.ISTRACE) {
            File ppIssue = new File(Utils.PPISSUE);
            if (!ppIssue.exists()) {
                ppIssue.mkdirs();
                Log.d(TAG, "<onCreate> " + "Create traceview file succeed.");
            }
            android.os.Debug.startMethodTracing(ppIssue + "/photopicker.trace");
        }

        super.onCreate(savedInstanceState);
        mMainView = Utils.getMyLayout(this, "photopicker_main");
        setContentView(mMainView);

        initConfig();
        mConfig.controlHook.onCreate(savedInstanceState);
        initActionBar();

        mGranted = PermissionUtil.checkAndRequestForPhotoPicker(this);
        Log.d(TAG, "<onCreate> " + "mGranted: " + mGranted);
        if (mGranted) {
            initInGrant();
        }

        mDataObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                if (mGranted) {
                    Log.d(TAG, "<onChange> " + "Reload data from observer");
                    reload();
                }
            }
        };
        getContentResolver().registerContentObserver(mUri, true, mDataObserver);
        Log.d(TAG, "<onCreate> " + "<debugtime> onCreate time: " +
                   (System.currentTimeMillis() - timeStart) + "ms");
        Log.d(TAG, "<onCreate> " + Utils.END);
    }

    private void initInGrant() {
        mPhotoPickerView = (PhotoPickerView) mMainView.findViewWithTag("photopickerview");
        adjustOrientation();
        mPhotoPickerView.initPhotoPickerView(mConfig);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        mConfig.controlHook.onActivityResult(requestCode, resultCode, intent);
        if (Utils.ISTRACE) {
            if (null != intent) {
                Log.d(TAG, "<onActivityResult> " + "uri: " + intent.getData());
            } else {
                Log.d(TAG, "<onActivityResult> " + "uri: " + null);
            }
        }

        if (PhotoPicker.REQUEST_CODE == requestCode) {
            if (RESULT_CANCELED == resultCode) {
                return;
            } else if (RESULT_OK == resultCode) {
                setResult(RESULT_OK, intent);
            }
        } else {
            Log.d(TAG, "<onActivityResult> " + "Other REQUEST_CODE, do nothing.");
        }
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mGranted = PermissionUtil.isAllPermissionsGranted(permissions, grantResults);
        Log.d(TAG, "<onRequestPermissionsResult> " + "mGranted: " + mGranted);
        if (mGranted) {
            initInGrant();
        } else {
            Log.d(TAG, "<onRequestPermissionsResult> permission denied partly, do request");
            for (int i = 0; i < permissions.length; i++) {
                if (Manifest.permission.READ_EXTERNAL_STORAGE.equals(permissions[i]) &&
                    grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    PermissionUtil.showDeniedPrompt(this);
                    break;
                }
                if (Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permissions[i]) &&
                    grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    PermissionUtil.showDeniedPrompt(this);
                    break;
                }
            }
            finish();
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "<onResume> " + Utils.BEGIN + this);
        super.onResume();
        if (mGranted) {
            mPhotoPickerView.onResume();
        }
        mConfig.controlHook.onResume();
        Log.d(TAG, "<onResume> " + Utils.END);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "<onPause> " + Utils.BEGIN + this);
        super.onPause();
        if (mGranted) {
            mPhotoPickerView.onPause();
        }
        mConfig.controlHook.onPause();
        Log.d(TAG, "<onPause> " + Utils.END);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "<onDestroy> " + Utils.BEGIN + this);
        super.onDestroy();
        if (mGranted) {
            mPhotoPickerView.onDestroy();
        }
        mConfig.controlHook.onDestroy();
        getContentResolver().unregisterContentObserver(mDataObserver);

        mConfig = null;
        sConfig = null;
        if (Utils.ISTRACE) {
            android.os.Debug.stopMethodTracing();
        }
        Log.d(TAG, "<onDestroy> " + Utils.END);
    }

    protected Config getDefaultConfig() {
        Config config = new Config();
        config.actionBarIcon = null;
        config.title = Utils.getMyString(this, "photopicker_title");
        config.subTitle = "";
        config.includeTypes = 1; //TODO remove?
        config.dataMode = 1; //TODO remove?
        return config;
    }

    private void initConfig() {
        mConfig = new Config();
        Config config = getDefaultConfig();
        if (null != sConfig) {
            mConfig.title = sConfig.title != null ? sConfig.title : config.title;
            mConfig.subTitle = sConfig.subTitle != null ? sConfig.subTitle : config.subTitle;
            mConfig.actionBarIcon = sConfig.actionBarIcon != null ?
                                    sConfig.actionBarIcon : null;
            mConfig.includeTypes = sConfig.includeTypes != 0 ?
                                   sConfig.includeTypes : config.includeTypes;
            mConfig.dataMode = sConfig.dataMode != 0 ? sConfig.dataMode : config.dataMode;
            mConfig.controlHook = sConfig.controlHook != null ?
                                  sConfig.controlHook : Utils.getControlHook();
            mConfig.viewHook = sConfig.viewHook != null ?
                               sConfig.viewHook : Utils.getViewHook(this);
            mConfig.modelHook = sConfig.modelHook;
        } else {
            mConfig = config;
            mConfig.controlHook = Utils.getControlHook();
            mConfig.viewHook = Utils.getViewHook(this);
        }
        Log.d(TAG, "<initConfig> " + "debugConfig ...");
        Utils.debugConfig(mConfig);
    }

    private void adjustOrientation() {
        int currentOrientation = getResources().getConfiguration().orientation;
        int restoreFirPos = mPhotoPickerView.getFirstVisiblePosition();
        switch (currentOrientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                mPhotoPickerView.setNumColumns(NUM_COLUMNS_LANDSCAPE);
                break;
            case Configuration.ORIENTATION_PORTRAIT:
            default:
                mPhotoPickerView.setNumColumns(NUM_COLUMNS_PORTRAIT);
                break;
        }
        mPhotoPickerView.setSelection(restoreFirPos);
    }

    private void initActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayShowHomeEnabled(false);

        if (null != mConfig.actionBarIcon &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Log.d(TAG, "<initActionBar> " + "Set actionbar icon: " + mConfig.actionBarIcon);
            actionBar.setHomeAsUpIndicator(mConfig.actionBarIcon);
        }
        actionBar.setTitle(mConfig.title);
        actionBar.setSubtitle(mConfig.subTitle);
    }
}
