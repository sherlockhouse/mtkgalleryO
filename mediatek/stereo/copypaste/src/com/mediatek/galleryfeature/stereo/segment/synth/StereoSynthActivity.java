/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2015. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.galleryfeature.stereo.segment.synth;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore.Images.ImageColumns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Toast;

import com.mediatek.galleryfeature.stereo.segment.ImageSaver;
import com.mediatek.galleryfeature.stereo.segment.ImageShow;
import com.mediatek.galleryfeature.stereo.segment.MainImageMaster;
import com.mediatek.galleryfeature.stereo.segment.SegmentUtils;
import com.mediatek.galleryfeature.stereo.segment.StereoSegmentWrapper;
import com.mediatek.galleryfeature.stereo.segment.copypaste.R;
import com.mediatek.util.Log;
import com.mediatek.util.StereoImage;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Activity for image synthesis (so-called "paste") in copy & paste feature.
 */
public class StereoSynthActivity extends Activity {
    private static final String TAG = Log.Tag("Cp/StereoSynthActivity");

    // TODO erase ACTION_STEREO_SYNTH, since it is for private usage
    private static final String ACTION_STEREO_SYNTH = "com.mediatek.segment.action.SYNTH";
    private static final int REQUEST_CODE_REFINE = 10;
    private static final int ACTIONBAR_BACKGROUND_COLOR = 0x99333333;
    private static final int SAVE_QUALITY = 100;

    private MainImageMaster mMasterImage;
    private StereoSegmentWrapper mMaskSimulator;
    private LoadBitmapTask mLoadBitmapTask;
    private WeakReference<ProgressDialog> mSavingProgressDialog;
    private ActionBar mActionBar;
    private Menu mMenu;
    private String mAbsoluteFilePath;
    private Uri mSelectedImageUri;
    private File mSaveDirectory;

    private View mSaveButton;
    private ImageShow mImageShowSynth;
    private MenuItem mMenuLayerUp;
    private MenuItem mMenuLayerDown;
    private MenuItem mMenuRefineOriginal;

    private boolean mLoadingVisible = true;
    private boolean mIsDepthImage; // useless
    // exit if file to edit has been deleted
    private boolean mNeedCheckInvalid = true;
    private AtomicBoolean mSavingSynth = new AtomicBoolean(false);
    /**
     * Async task to load main bitmap for ImageShow.
     */
    private class LoadBitmapTask extends AsyncTask<Uri, Boolean, Boolean> {
        @Override
        protected Boolean doInBackground(Uri... params) {
            if (!mMasterImage.loadBitmap(params[0])) {
                return false;
            }
            return true;
        }

        @Override
        protected void onProgressUpdate(Boolean... values) {
            super.onProgressUpdate(values);
            if (isCancelled()) {
                return;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (isCancelled()) {
                return;
            }

            if (!result) {
                cannotLoadImage();
                return;
            }

            mLoadBitmapTask = null;

            Intent intent = getIntent();
            if (intent.getAction().equals(ACTION_STEREO_SYNTH)) {
                String copySource = intent.getStringExtra("COPY_SRC_URI");
                if (copySource != null) {
                    LoadCopySourceTask copySourceLoad = new LoadCopySourceTask();
                    Log.d(TAG, "<LoadBitmapTask.onPostExecute> copySource = " + copySource);
                    Uri copySourceUri = Uri.parse(copySource);
                    copySourceLoad.execute(copySourceUri);
                }
            }
            super.onPostExecute(result);
        }
    }

    /**
     * Async task to load copy source (foreground object) bitmap.
     */
    private class LoadCopySourceTask extends AsyncTask<Uri, Boolean, Boolean> {
        private Bitmap mBitmapCopySource;

        public LoadCopySourceTask() {
            mBitmapCopySource = SegmentUtils.getCommunicationBitmap();
        }

        @Override
        protected Boolean doInBackground(Uri... params) {
            Log.d(TAG, "<LoadCopySourceTask.doInBackground> copy " + params[0]);

            if (mBitmapCopySource != null) {
                loadSegmentor();
                return true;
            }

            MainImageMaster foregroundImageMaster = new MainImageMaster(StereoSynthActivity.this);
            boolean result = foregroundImageMaster.loadBitmap(params[0]);
            if (result) {
                mBitmapCopySource = foregroundImageMaster.getBitmap();
                loadSegmentor();
            }
            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (mLoadingVisible) {
                stopLoadingIndicator();
            }

            if (!result) { // segment invalid
                return;
            }
            ((ImageShowSynth) (mImageShowSynth)).setCopySource(mBitmapCopySource);
            Log.d(TAG, "<LoadCopySourceTask.onPostExecute> mBitmapCopySource = "
                    + mBitmapCopySource);
            mMenuLayerDown.setVisible(mIsDepthImage ? true : false);
            mMenuLayerUp.setVisible(mIsDepthImage ? true : false);
            mMenuRefineOriginal.setVisible(mIsDepthImage ? true : false);

            updateMenuIcon(mMenuLayerDown, R.drawable.m_move_object_down);

            ((ImageShowSynth) (mImageShowSynth)).setMode(ImageShowSynth.MODE_SRC_FOREMOST);

            mImageShowSynth.setSegmenter(mMaskSimulator);
            mImageShowSynth.setVisibility(View.VISIBLE);
            mSaveButton.setVisibility(View.VISIBLE);
            mSaveButton.setEnabled(true);
            onImageChange();
            mLoadBitmapTask = null;

            super.onPostExecute(result);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        StereoImage.createDisplayRect(this);
        // exit if file to edit has been deleted @{
        // it mainly aims to handle language switch or LCA case here
        boolean needExit = finishIfInvalidUri();
        mNeedCheckInvalid = false;
        if (needExit) {
            return;
        }

        // avoid flash when launching
        getWindow().setBackgroundDrawable(new ColorDrawable(0));
        mSavingSynth.set(false);
        loadXML();

        mMasterImage = new MainImageMaster(this);
        mImageShowSynth.setImageMaster(mMasterImage);

        loadMainBitmapAsync();
    }

    @Override
    protected void onDestroy() {
        hideSavingProgress();

        if (mLoadBitmapTask != null) {
            mLoadBitmapTask.cancel(false);
        }

        if (mMaskSimulator != null) {
            mMaskSimulator.release();
            mMaskSimulator = null;
        }

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.m_stereo_copypaste_menu, menu);
        mMenu = menu;
        setupMenu();
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mImageShowSynth != null) {
            mImageShowSynth.recycle();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        finishIfInvalidUri();
        mNeedCheckInvalid = true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            onBackPressed();
            return true;
        default:
            return false;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // TODO do we really need to response onConfigurationChange?
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "<onActivityResult>,requestCode:" + requestCode);
        // TODO loadSegmentor is designed to invocate in background thread
        loadSegmentor();
        if (mMaskSimulator != null) {
            mImageShowSynth.setSegmenter(mMaskSimulator);
            onImageChange();
        }
    }

    @Override
    public void finish() {
        Log.d(TAG, "<finish>");
        // release copy source bitmap reference
        SegmentUtils.setCommunicationBitmap(null);
        super.finish();
    }

    private void onImageChange() {
        if (mImageShowSynth != null) {
            mImageShowSynth.invalidate();
        }
    }

    private void loadSegmentor() {
        if (mIsDepthImage && mMaskSimulator == null) {
            mMaskSimulator = new StereoSegmentWrapper(this,
                    mMasterImage.getOriginalBounds().width(),
                    mMasterImage.getOriginalBounds().height(), mMasterImage.getBitmap().getWidth(),
                    mMasterImage.getBitmap().getHeight());
            boolean isSegmentValid = mMaskSimulator.initSegment(mMasterImage.getUri(),
                    mAbsoluteFilePath, mMasterImage.getBitmap());
            if (!isSegmentValid) {
                mMaskSimulator = null;
            }
        }
    }

    private boolean finishIfInvalidUri() {
        if (!mNeedCheckInvalid) {
            return false;
        }
        Uri uri = null;
        Intent intent = getIntent();
        if (intent != null) {
            uri = intent.getData();
        }
        Log.d(TAG, "<finishIfInvalidUri> uri:" + uri);
        if (uri != null) {
            if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
                return false;
            }
            // fix cursor leak
            final String[] projection = new String[] { ImageColumns.DATA,
                    SegmentUtils.CAMERA_REFOCUS };
            android.database.Cursor cursor = null;
            cursor = getContentResolver().query(uri, projection, null, null, null);
            if (cursor == null) {
                Log.d(TAG, "<finishIfInvalidUri> cannot get cursor for:" + uri);
                return true;
            }
            try {
                if (cursor.moveToNext()) {
                    mAbsoluteFilePath = cursor.getString(0);
                    Log.d(TAG, "<finishIfInvalidUri> mAbsoluteFilePath = " + mAbsoluteFilePath);
                    mIsDepthImage = (cursor.getInt(1) == 1);
                    Log.d(TAG, "<finishIfInvalidUri> mIsDepthImage = " + mIsDepthImage);
                } else {
                    Log.d(TAG, "<finishIfInvalidUri> cannot find data for: " + uri);
                    return true;
                }
            } finally {
                cursor.close();
            }
        }
        return false;
    }

    private void showSavingProgress(String albumName) {
        ProgressDialog progress;
        if (mSavingProgressDialog != null) {
            progress = mSavingProgressDialog.get();
            if (progress != null) {
                progress.show();
                return;
            }
        }
        // TODO: Allow cancellation of the saving process
        String progressText;
        if (albumName == null) {
            progressText = getString(R.string.saving_image);
        } else {
            progressText = getString(R.string.filtershow_saving_image, albumName);
        }
        progress = ProgressDialog.show(this, "", progressText, true, false);
        mSavingProgressDialog = new WeakReference<ProgressDialog>(progress);
    }

    private void hideSavingProgress() {
        if (mSavingProgressDialog != null) {
            ProgressDialog progress = mSavingProgressDialog.get();
            if (progress != null) {
                progress.dismiss();
            }
        }
    }

    private void startLoadingIndicator() {
        final View loading = findViewById(R.id.loading);
        mLoadingVisible = true;
        loading.setVisibility(View.VISIBLE);
    }

    private void stopLoadingIndicator() {
        final View loading = findViewById(R.id.loading);
        loading.setVisibility(View.GONE);
        mLoadingVisible = false;
    }

    private void startLoadBitmap(Uri uri) {
        mImageShowSynth.setVisibility(View.INVISIBLE);
        startLoadingIndicator();
        mLoadBitmapTask = new LoadBitmapTask();
        mLoadBitmapTask.execute(uri);
    }

    private void cannotLoadImage() {
        Toast.makeText(this, R.string.cannot_load_image, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void loadMainBitmapAsync() {
        Intent intent = getIntent();
        mSelectedImageUri = intent.getData();
        mSaveDirectory = ImageSaver.getSaveDirectory(this, mSelectedImageUri);
        if (mSelectedImageUri != null) {
            startLoadBitmap(mSelectedImageUri);
        }
    }

    private void setActionBarDisplayOptions(ActionBar actionBar, boolean displayHomeAsUp,
            boolean showTitle) {
        if (actionBar == null) {
            return;
        }
        int options = 0;
        if (displayHomeAsUp) {
            options |= ActionBar.DISPLAY_HOME_AS_UP;
        }
        if (showTitle) {
            options |= ActionBar.DISPLAY_SHOW_TITLE;
        }

        actionBar.setDisplayOptions(options, ActionBar.DISPLAY_HOME_AS_UP
                | ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setHomeButtonEnabled(displayHomeAsUp);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setTitle(R.string.m_save);
    }

    private void loadXML() {
        setContentView(R.layout.m_stereo_synth_activity);

        mActionBar = getActionBar();
        mActionBar.setBackgroundDrawable(new ColorDrawable(ACTIONBAR_BACKGROUND_COLOR));
        mActionBar.setSubtitle(null);
        mActionBar.setCustomView(R.layout.m_stereo_actionbar);
        mSaveButton = mActionBar.getCustomView();
        View clickableRegion = mSaveButton.findViewById(R.id.stereo_segment_save);
        clickableRegion.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSavingSynth.compareAndSet(false, true)) {
                    saveSynthedImage();
                }
            }
        });

        setActionBarDisplayOptions(mActionBar, true, true);

        mImageShowSynth = (ImageShow) findViewById(R.id.imageShowSynth);
    }

    private void setupMenu() {
        if (mMenu == null || mMasterImage == null) {
            return;
        }

        mMenuLayerUp = mMenu.findItem(R.id.refine_layerup);
        mMenuLayerDown = mMenu.findItem(R.id.refine_layerdown);
        mMenuRefineOriginal = mMenu.findItem(R.id.refine_refine_original);

        mMenuLayerUp.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (mImageShowSynth != null) {
                    updateMenuIcon(mMenuLayerDown, R.drawable.m_move_object_down);
                    ((ImageShowSynth) (mImageShowSynth)).setMode(ImageShowSynth.MODE_SRC_FOREMOST);
                    mImageShowSynth.invalidate();
                }
                return false;
            }
        });

        mMenuLayerDown.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (mImageShowSynth != null) {
                    updateMenuIcon(mMenuLayerUp, R.drawable.m_move_object_up);
                    ((ImageShowSynth) (mImageShowSynth)).setMode(ImageShowSynth.MODE_OBJ_FOREMOST);
                    mImageShowSynth.invalidate();
                }
                return false;
            }
        });

        mMenuRefineOriginal.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent("com.mediatek.segment.action.REFINE");
                intent.setDataAndType(getIntent().getData(), getIntent().getType()).setFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);

                if (mMaskSimulator != null) {
                    mMaskSimulator.release();
                    mMaskSimulator = null;
                }

                startActivityForResult(intent, REQUEST_CODE_REFINE);
                return false;
            }
        });
    }

    private void errorHandle() {
        Toast.makeText(this, getString(R.string.m_general_err_tip),
                Toast.LENGTH_LONG).show();
        finish();
    }

    private void saveSynthedImage() {
        new Thread(new Runnable() {
            public void run() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        showSavingProgress(/* "..." */null);
                    }
                });

                Bitmap toSaveBitmap = ((ImageShowSynth) (mImageShowSynth)).getSynthBitmap();

                File target = ImageSaver.getNewFile(mSaveDirectory);
                ImageSaver.saveBitmapToFile(target, toSaveBitmap, Bitmap.CompressFormat.JPEG);
                long time = System.currentTimeMillis();
                final Uri dstUri = ImageSaver.linkNewFileToUri(StereoSynthActivity.this,
                        mSelectedImageUri, target, toSaveBitmap, time);
                toSaveBitmap.recycle();
                runOnUiThread(new Runnable() {
                    public void run() {
                        mSavingSynth.set(false);
                        hideSavingProgress();
                        Log.d(TAG, "<saveSynthedImage> uri:" + dstUri);

                        setResult(RESULT_OK, new Intent().setData(dstUri));
                        StereoSynthActivity.this.finish();
                    }
                });
            }
        }).start();
    }

    private void updateMenuIcon(MenuItem menuItem, int iconId) {
        mMenuLayerUp.setIcon(R.drawable.m_move_object_up_inactive);
        mMenuLayerDown.setIcon(R.drawable.m_move_object_down_inactive);
        menuItem.setIcon(iconId);
    }
}
