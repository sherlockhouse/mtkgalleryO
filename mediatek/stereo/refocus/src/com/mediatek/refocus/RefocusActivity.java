/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediatek.refocus;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.refocus.ReFocusView.RefocusListener;
import com.mediatek.stereoapplication.imagerefocus.RefocusImage;
import com.mediatek.util.Log;

import java.io.File;
import java.lang.ref.WeakReference;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * RefocusActivity for stereo refocus feature.
 *
 */
public class RefocusActivity extends Activity implements
        RefocusListener, SeekBar.OnSeekBarChangeListener {
    private static final String TAG = Log.Tag("Rf/RefocusActivity");
    private static final int FIRST_DURATION_TIME = 500;
    private static final String MIME_TYPE = "image/*";
    private static final String REFOCUS_MIME_TYPE = "mimeType";
    private static final String ENABLE_DEBUG_STRING = "debug.gallery.enable";
    private static final String ENABLE_DUMP_BITMAP_STRING = "dump.gallery.enable";
    private static final String ANIMATION_LEN_SETTING_STRING = "animation.duration";
    private static final String TIME_STAMP_NAME = "'IMG'_yyyyMMdd_HHmmss";
    private static final boolean ENABLE_DEBUG = SystemPropertyUtils.get(
            ENABLE_DEBUG_STRING).equals("1");
    private static final boolean ENABLE_DUMP_BITMAP = SystemPropertyUtils.get(
            ENABLE_DUMP_BITMAP_STRING).equals("1");
    private static final int ANIMATION_LEN_FOR_DEBUG = SystemPropertyUtils.getInt(
            ANIMATION_LEN_SETTING_STRING, 0);
    private static final int MSG_INIT_FINISH = 1;
    private static final int MSG_GENERATE_IMAGE = 2;
    private static final int MSG_GENERATE_DONE = 3;
    private static final int MSG_REFOCUS_ERROR = 4;
    private static final int MSG_HIDE_DOF_VIEW = 5;

    private static final int REQUEST_SET_AS = 1;
    private static final int PROGRESS_PER_DOF = 30;
    private static final float DEFAULT_X_COORD_FACTOR = 0.5f;
    private static final float DEFAULT_Y_COORD_FACTOR = 0.5f;
    private static final int HALF_TRANSPARENT_COLOR = 0x99333333;
    private static final int DOF_VIEW_DELAY_TIME = 1000;
    private static final String[] DOFDATA = {"11", "10",
            "9.0", "8.0", "7.2", "6.3", "5.6", "4.5", "3.6", "2.8",
            "2.2", "1.8", "1.4", "1.2", "1.0", "0.8"};
    private static final int[] DEPTHDATA = {4, 6, 8, 10, 12, 14, 16, 18,
        20, 22, 24, 26, 28, 30, 32, 34};
    private static final int DEFAULT_NUM = 12;
    private static int mCurrentNum = DEFAULT_NUM;

    private WeakReference<DialogFragment> mSavingProgressDialog;
    private WeakReference<DialogFragment> mLoadingProgressDialog;

    private float mImageWidth;
    private float mImageHeight;
    private int mImageOrientation;
    private String mSourceFilePath;
    private Bitmap mOriginalBitmap;
    private Uri mSourceUri;
    private ReFocusView mRefocusView;
    private ImageRefocus mRefocusImage;
    private View mSaveButton;
    private Handler mHandler;
    private LoadBitmapTask mLoadBitmapTask;
    private GeneRefocusImageTask mGeneRefocusImageTask;
    private SeekBar mRefocusSeekBar;
    private TextView mDofView;

    private Uri mInsertUri;
    private String mFilePath;
    private int[] mTouchBitmapCoord = new int[2];
    private int mShowImageTotalDurationTime;
    private int mShowImageFirstDurationTime = FIRST_DURATION_TIME;

    private boolean mIsSetDepthOnly = false;
    private boolean mIsSharingImage = false;
    private boolean mIsSetPictureAs = false;
    private boolean mIsCancelThread = false;
    /*
    Reload this config from refocus.cfg in assets:
    true  -> generate refocus image and show it at first launch
    false -> show original image
    */
    private boolean mIsShowRefocusImage = false;
    private boolean mIsFirstLaunch = true;
    // if has not done refocusing once, then return ori bitmap when saving
    private boolean mHasGeneratedEffect = false;
    private AtomicBoolean mIsDoingRefocus = new AtomicBoolean(false);
    private boolean mGranted;

    @Override
    public void onCreate(Bundle bundle) {
        TraceHelper.beginSection(">>>>Refocus-onCreate");
        long begin = System.currentTimeMillis();
        Log.d(TAG, "<onCreate> begin");
        super.onCreate(bundle);

        mGranted = PermissionUtil.checkAndRequestForPhotoPicker(this);
        Log.d(TAG, "<onCreate> " + "mGranted: " + mGranted);
        initializeViews();
        mHandler = new Handler() {
            public void handleMessage(Message message) {
                switch (message.what) {
                case MSG_INIT_FINISH:
                    hideLoadingProgress();
                    setSaveState(true);
                    return;
                case MSG_GENERATE_IMAGE:
                    if (mGeneRefocusImageTask != null) {
                        mGeneRefocusImageTask.notifyDirty();
                    }
                    return;
                case MSG_GENERATE_DONE:
                    setSaveState(true);
                    return;
                case MSG_REFOCUS_ERROR:
                    errorHandleWhenRefocus();
                    return;
                case MSG_HIDE_DOF_VIEW:
                    mDofView.setVisibility(View.GONE);
                    break;
                default:
                    throw new AssertionError();
                }
            }
        };
        if (mGranted) {
            initializeData();
        }
        mGeneRefocusImageTask = new GeneRefocusImageTask();
        mGeneRefocusImageTask.start();
        Log.d(TAG, "<onCreate><PERF> end, costs " + (System.currentTimeMillis() - begin));
        TraceHelper.endSection();
    }

    @Override
    public void onProgressChanged(SeekBar refocusSeekBar, int progress,
            boolean fromuser) {
        mCurrentNum = progress / PROGRESS_PER_DOF;
        mDofView.setText("F" + DOFDATA[mCurrentNum]);
    }

    @Override
    public void onStartTrackingTouch(SeekBar refocusSeekBar) {
        mHandler.removeMessages(MSG_HIDE_DOF_VIEW);
        mDofView.setText("F" + DOFDATA[mCurrentNum]);
        mDofView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onStopTrackingTouch(SeekBar refocusSeekBar) {
        mHandler.sendEmptyMessageDelayed(MSG_HIDE_DOF_VIEW, DOF_VIEW_DELAY_TIME);
        if (!mIsDoingRefocus.compareAndSet(false, true)) {
            Log.d(TAG, "<onStopTrackingTouch> please wait!");
            showWaitToast();
            return;
        }
        setSaveState(false);
        mIsSetDepthOnly = true;
        mIsCancelThread = false;
        mCurrentNum = refocusSeekBar.getProgress() / PROGRESS_PER_DOF;
        Log.d(TAG, "<onStopTrackingTouch> Seekbar reset mCurrentNum = " + mCurrentNum);
        mHandler.sendEmptyMessage(MSG_GENERATE_IMAGE);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "<onDestroy>");
        if (mGeneRefocusImageTask != null) {
            mGeneRefocusImageTask.terminate();
            mGeneRefocusImageTask = null;
        }
        if (mLoadBitmapTask != null) {
            mLoadBitmapTask.cancel(false);
            mLoadBitmapTask = null;
        }
        if (mRefocusImage != null) {
            mRefocusImage.release();
        }
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }


    @Override
    protected void onPause() {
        Log.d(TAG, "<OnPause>");
        super.onPause();
        hideLoadingProgress();
        hideSavingProgress();
        mIsShowRefocusImage = false;
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "<onResume>");
        super.onResume();
        if (mRefocusView != null) {
            mRefocusView.moveBackCheck();
            Log.d(TAG, "<onResume> moveBackCheck");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mGranted = PermissionUtil.isAllPermissionsGranted(permissions, grantResults);
        Log.d(TAG, "<onRequestPermissionsResult> " + "mGranted: " + mGranted);
        if (mGranted) {
            initializeData();
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
    public void setRefocusImage(float x, float y) {
        if (!mIsDoingRefocus.compareAndSet(false, true)) {
            Log.d(TAG, "<setRefocusImage> please wait!");
            showWaitToast();
            return;
        }
        setSaveState(false);
        mTouchBitmapCoord[0] = (int) (x * mImageWidth);
        mTouchBitmapCoord[1] = (int) (y * mImageHeight);
        mIsSetDepthOnly = false;
        Log.d(TAG, "<setRefocusImage> x = " + x + ", y = " + y + "mTouchBitmapCoord[0] = "
                + mTouchBitmapCoord[0] + " mTouchBitmapCoord[1] = " + mTouchBitmapCoord[1]);
        if (mGeneRefocusImageTask != null) {
            mGeneRefocusImageTask.notifyDirty();
        }
    }

    // Shows status bar in portrait view, hide in landscape view
    private void toggleStatusBarByOrientation() {
        Window win = getWindow();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            win.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            win.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    private void initRefocusSeekBar() {
        Log.d(TAG, "<initRefocusSeekBar>");
        ImageView small = (ImageView) this.findViewById(R.id.small_aperture);
        small.setVisibility(View.VISIBLE);

        ImageView big = (ImageView) this.findViewById(R.id.big_aperture);
        big.setVisibility(View.VISIBLE);

        mDofView = (TextView) this.findViewById(R.id.dof_view);

        mRefocusSeekBar = (SeekBar) this.findViewById(R.id.refocusSeekBar);
        mRefocusSeekBar.setProgress(mCurrentNum * PROGRESS_PER_DOF);
        mRefocusSeekBar.setVisibility(View.VISIBLE);
        mRefocusSeekBar.setOnSeekBarChangeListener(this);
    }

    private void setSaveState(boolean enable) {
        if (mSaveButton != null) {
            mSaveButton.setEnabled(enable);
        }
    }

    private void startLoadBitmap(String filePath) {
        Log.d(TAG, "<startLoadBitmap> filePath:" + filePath);
        if (filePath != null) {
            setSaveState(false);
            showLoadingProgress();
            mLoadBitmapTask = new LoadBitmapTask();
            mLoadBitmapTask.execute(filePath);
        } else {
            showImageLoadFailToast();
            finish();
        }
    }

    private void requestFirstRefocus() {
        setRefocusImage(mTouchBitmapCoord[0] / mImageWidth, mTouchBitmapCoord[1] / mImageHeight);
    }

    private void calcDefaultFocalPoint() {
        Rect faceRect = mRefocusImage.getDefaultFaceRect((int) mImageWidth, (int) mImageHeight);
        if (faceRect != null) {
            int faceCenterX = (faceRect.left + faceRect.right) / 2;
            int faceCenterY = (faceRect.top + faceRect.bottom) / 2;
            Log.d(TAG, "<calcDefaultFocalPoint> use face, faceRect " + faceRect
                    + ", faceCenterX " + faceCenterX + ", faceCenterY " + faceCenterY);
            mTouchBitmapCoord[0] = faceCenterX;
            mTouchBitmapCoord[1] = faceCenterY;
            return;
        }

        int[] coord = mRefocusImage.getDefaultFocusCoord((int) mImageWidth, (int) mImageHeight);
        if (coord[0] > 0 && coord[0] < mImageWidth && coord[1] > 0 && coord[1] < mImageHeight) {
            Log.d(TAG, "<calcDefaultFocalPoint> use focal point, xCoord " + coord[0]
                    + ", yCoord " + coord[1]);
            mTouchBitmapCoord[0] = coord[0];
            mTouchBitmapCoord[1] = coord[1];
        } else {
            Log.d(TAG, "<calcDefaultFocalPoint> use image center");
        }
    }

    private void showImageLoadFailToast() {
        CharSequence text = getString(R.string.cannot_load_image);
        Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        toast.show();
    }

    private void showWaitToast() {
        CharSequence text = getString(R.string.m_please_wait);
        Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        toast.show();
    }

    /**
     * Load bitmap task.
     */
    private class LoadBitmapTask extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... params) {
            long beginTime = System.currentTimeMillis();
            String filePath = params[0];
            TraceHelper.beginSection(">>>>Refocus-initRefocusImages");
            if (!initRefocusImages()) {
                return null;
            }
            TraceHelper.endSection();
            Log.d(TAG, "<LoadBitmapTask><PERF> initRefocusImages = " +
                    (System.currentTimeMillis() - beginTime));

            beginTime = System.currentTimeMillis();
            TraceHelper.beginSection(">>>>Refocus-LoadBitmapTask");
            Bitmap bitmap = RefocusHelper.decodeBitmap(filePath,
                mRefocusImage.getViewWidth(), mRefocusImage.getViewHeigth());
            TraceHelper.endSection();

            long decodeTime = System.currentTimeMillis() - beginTime;
            Log.d(TAG, "<LoadBitmapTask><PERF> decode time = " + decodeTime);

            beginTime = System.currentTimeMillis();
            TraceHelper.beginSection(">>>>Refocus-calcDefaultFocalPoint");
            calcDefaultFocalPoint();
            TraceHelper.endSection();
            Log.d(TAG, "<LoadBitmapTask><PERF> calcDefaultFocalPoint time = "
                    + (System.currentTimeMillis() - beginTime));
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            mOriginalBitmap = result;
            if (mOriginalBitmap != null && mImageWidth != 0 && mImageHeight != 0) {
                long beginTime1 = System.currentTimeMillis();

                TraceHelper.beginSection(">>>>Refocus-setImageActor");
                mRefocusView.setImageActor(mOriginalBitmap, mOriginalBitmap.getWidth(),
                        mOriginalBitmap.getHeight());
                TraceHelper.endSection();

                Bitmap bmp = mOriginalBitmap.copy(mOriginalBitmap.getConfig(), false);

                TraceHelper.beginSection(">>>>Refocus-setImageActorNew");
                mRefocusView.setImageActorNew(bmp);
                TraceHelper.endSection();

                long needtime = System.currentTimeMillis() - beginTime1;
                Log.d(TAG, "<LoadBitmapTask><PERF> setImageActor time = " + needtime);
                long beginTime2 = System.currentTimeMillis();
                needtime = System.currentTimeMillis() - beginTime2;
                Log.d(TAG, "<LoadBitmapTask><PERF> setTransitionTime time = " + needtime);
                long spendTime = System.currentTimeMillis() - beginTime1;
                Log.d(TAG, "<LoadBitmapTask><PERF> onPostExecute costs time = " + spendTime);

                initRefocusSeekBar();
                if (mIsShowRefocusImage) {
                    requestFirstRefocus();
                } else {
                    mHandler.sendEmptyMessage(MSG_INIT_FINISH);
                }
            } else {
                Log.d(TAG, "<LoadBitmapTask> could not load image for Refocus!!");
                if (mOriginalBitmap != null) {
                    mOriginalBitmap.recycle();
                    mOriginalBitmap = null;
                }
                showImageLoadFailToast();
                setResult(RESULT_CANCELED, new Intent());
                finish();
            }
        }
    }

    protected void saveRefocusBitmap() {
        if (!mIsDoingRefocus.compareAndSet(false, true)) {
            Log.d(TAG, "<saveRefocusBitmap> please wait!");
            showWaitToast();
            return;
        }
        setSaveState(false);
        mRefocusView.setTransitionTime(1, 0);
        showSavingProgress(null);
        startSaveBitmap(mSourceUri);
    }

    private void startSaveBitmap(Uri sourceUri) {
        SaveBitmapTask saveTask = new SaveBitmapTask(sourceUri);
        saveTask.execute();
    }

    /**
     * Save bitmap task.
     */
    private class SaveBitmapTask extends AsyncTask<Bitmap, Void, Boolean> {
        Uri mSourceUri;

        public SaveBitmapTask(Uri sourceUri) {
            mSourceUri = sourceUri;
        }

        @Override
        protected Boolean doInBackground(Bitmap... params) {
            if (mSourceUri == null) {
                return false;
            }
            if (!mHasGeneratedEffect && !mIsShowRefocusImage) {
                Log.d(TAG, "<SaveBitmapTask> has not generated refocusing, return ori bitmap");
                mInsertUri = mSourceUri;
                return true;
            }
            Log.d(TAG, "<SaveBitmapTask> start");
            TraceHelper.beginSection(">>>>Refocus-SaveBitmapTask");
            String filename;
            boolean result = false;
            boolean replaceBlurImage = true;
            if (replaceBlurImage) {
                filename = mSourceFilePath;
                long begin = System.currentTimeMillis();
                TraceHelper.beginSection(">>>>Refocus-saveRefocusImage");
                result = mRefocusImage.saveRefocusImage(filename, mTouchBitmapCoord,
                        mImageWidth, mImageHeight, replaceBlurImage);
                TraceHelper.endSection();
                Log.d(TAG, "<SaveBitmapTask><PERF> saveRefocusImage costs "
                        + (System.currentTimeMillis() - begin));
                if (!result) {
                    Log.d(TAG, "<SaveBitmapTask> saveRefocusImage error!! replaceBlurImage true");
                    mInsertUri = null;
                    TraceHelper.endSection();
                    return false;
                }
                begin = System.currentTimeMillis();
                TraceHelper.beginSection(">>>>Refocus-updateContent");
                mInsertUri = RefocusHelper.updateContent(RefocusActivity.this, mSourceUri,
                        new File(mSourceFilePath), DOFDATA[mCurrentNum]);
                TraceHelper.endSection();
                Log.d(TAG, "<SaveBitmapTask><PERF> updateContent costs "
                        + (System.currentTimeMillis() - begin));
            } else {
                filename = new SimpleDateFormat(TIME_STAMP_NAME).format(
                        new Date(System.currentTimeMillis()));
                File file = RefocusHelper.getNewFile(RefocusActivity.this, mSourceUri, filename);
                if (file == null) {
                    Log.d(TAG, "<SaveBitmapTask> getNewFile error!! replaceBlurImage false");
                    mInsertUri = null;
                    return false;
                }
                long begin = System.currentTimeMillis();
                TraceHelper.beginSection(">>>>Refocus-saveRefocusImage");
                result = mRefocusImage.saveRefocusImage(file.getAbsolutePath(), mTouchBitmapCoord,
                        mImageWidth, mImageHeight, replaceBlurImage);
                TraceHelper.endSection();
                Log.d(TAG, "<SaveBitmapTask><PERF> saveRefocusImage costs "
                        + (System.currentTimeMillis() - begin));
                if (!result) {
                    Log.d(TAG, "<SaveBitmapTask> saveRefocusImage error!! replaceBlurImage false");
                    mInsertUri = null;
                    return false;
                }
                begin = System.currentTimeMillis();
                TraceHelper.beginSection(">>>>Refocus-insertContent");
                mInsertUri = RefocusHelper.insertContent(RefocusActivity.this, mSourceUri, file,
                        filename);
                TraceHelper.endSection();
                Log.d(TAG, "<SaveBitmapTask><PERF> insertContent costs "
                        + (System.currentTimeMillis() - begin));
            }
            TraceHelper.endSection();
            Log.d(TAG, "<SaveBitmapTask> end");
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            finishSaveBitmap(mInsertUri);
        }
    }

    private void finishSaveBitmap(Uri destUri) {
        Log.d(TAG, "<finishSaveBitmap> destUri:" + destUri);
        if (destUri == null) {
            showSaveFailToast();
            Log.d(TAG, "<finishSaveBitmap> saving fail");
            return;
        }
        setResult(RESULT_OK, new Intent().setData(destUri));
        Log.d(TAG, "<finishSaveBitmap> set result and finish activity");
        finish();
    }

    private boolean initRefocusImages() {
        boolean initResult = false;
        mFilePath = RefocusHelper.getRealFilePathFromURI(getApplicationContext(), mSourceUri);
        long refocusImageInitTimestart = System.currentTimeMillis();
        if (mFilePath == null) {
            Log.d(TAG, "<initRefocusImages> mFilePath is null, mSourceUri: " + mSourceUri);
            mHandler.sendEmptyMessage(MSG_REFOCUS_ERROR);
            return false;
        }
        TraceHelper.beginSection(">>>>Refocus-initRefocusImages-init");
        if (!mRefocusImage.init(mSourceUri, mFilePath, mImageWidth, mImageHeight)) {
            Log.d(TAG, "<initRefocusImages> error, abort init");
            mHandler.sendEmptyMessage(MSG_REFOCUS_ERROR);
            TraceHelper.endSection();
            return false;
        }
        TraceHelper.endSection();
        long refocusImageInitSpentTime = System.currentTimeMillis() - refocusImageInitTimestart;
        Log.d(TAG, "<initRefocusImages><PERF> RefocusImageInitSpent time = "
                + refocusImageInitSpentTime);
        // get dof from exif
        String fNum = RefocusHelper.getApertureData(mFilePath);
        if (fNum != null) {
            for (int num = 0; num < DOFDATA.length; num++) {
                try {
                    // compare with float value instead of string, e.g. 7.0 & 7
                    if (Float.parseFloat(fNum) == Float.parseFloat(DOFDATA[num])) {
                        mCurrentNum = num;
                        break;
                    }
                } catch (NumberFormatException e) {
                    Log.d(TAG, "<initRefocusImages> parse Aperture error, fNum: " + fNum);
                }
            }
        }
        Log.d(TAG, "<initRefocusImages> mCurrentNum:" + mCurrentNum + ",fNum:" + fNum);
        if (ENABLE_DEBUG) {
            int depBufWidth = mRefocusImage.getDepthInfo().depthBufferWidth;
            int depBufHeight = mRefocusImage.getDepthInfo().depthBufferHeight;
            Log.d(TAG, "<initRefocusImages> depBufWidth = " + depBufWidth + ", depBufHeight = "
                    + depBufHeight);
            mRefocusView.setDepthActor(mRefocusImage.getDepthInfo().depthBuffer, 0, 1, depBufWidth,
                    depBufHeight);
        }
        return true;
    }

    private void showSavingProgress(String albumName) {
        DialogFragment fragment;
        if (mSavingProgressDialog != null) {
            fragment = mSavingProgressDialog.get();
            if (fragment != null) {
                fragment.show(getFragmentManager(), null);
                return;
            }
        }
        String progressText;
        if (albumName == null) {
            progressText = getString(R.string.saving_image);
        } else {
            progressText = getString(R.string.m_saving_image, albumName);
        }
        final DialogFragment genProgressDialog = new ProgressFragment(progressText);
        genProgressDialog.setCancelable(false);
        genProgressDialog.show(getFragmentManager(), null);
        genProgressDialog.setStyle(R.style.ProgressDialog, genProgressDialog.getTheme());
        mSavingProgressDialog = new WeakReference<DialogFragment>(genProgressDialog);
    }

    private void showLoadingProgress() {
        DialogFragment fragment;
        if (mLoadingProgressDialog != null) {
            fragment = mLoadingProgressDialog.get();
            if (fragment != null) {
                fragment.show(getFragmentManager(), null);
                return;
            }
        }
        final DialogFragment genProgressDialog = new ProgressFragment(R.string.loading_image);
        genProgressDialog.setCancelable(false);
        genProgressDialog.show(getFragmentManager(), null);
        genProgressDialog.setStyle(R.style.ProgressDialog, genProgressDialog.getTheme());
        mLoadingProgressDialog = new WeakReference<DialogFragment>(genProgressDialog);
    }

    private void hideLoadingProgress() {
        if (mLoadingProgressDialog != null) {
            DialogFragment fragment = mLoadingProgressDialog.get();
            if (fragment != null) {
                fragment.dismissAllowingStateLoss();
            }
        }
    }

    private void hideSavingProgress() {
        if (mSavingProgressDialog != null) {
            DialogFragment progress = mSavingProgressDialog.get();
            if (progress != null) {
                progress.dismissAllowingStateLoss();
            }
        }
    }

    private void showSaveFailToast() {
        CharSequence text = getString(R.string.m_refocus_save_fail);
        Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        toast.show();
    }

    /**
     * Generate refocus image task.
     */
    private class GeneRefocusImageTask extends Thread {
        private volatile boolean mDirty = false;
        private volatile boolean mActive = true;
        private int mIndex = 0;

        public GeneRefocusImageTask() {
            setName("GeneRefocusImageTask");
        }

        @Override
        public void run() {
            while (mActive) {
                synchronized (this) {
                    if (!mDirty && mActive) {
                        Log.d(TAG, "<GeneRefocusImageTask> wait");
                        RefocusHelper.waitWithoutInterrupt(this);
                        continue;
                    }
                }
                mDirty = false;
                Log.d(TAG, "<GeneRefocusImageTask> mCurrentNum = " + mCurrentNum + ",x:" +
                        mTouchBitmapCoord[0] + ",y:" + mTouchBitmapCoord[1]);
                Log.d(TAG, "<GeneRefocusImageTask> mIsShowRefocusImage:" + mIsShowRefocusImage +
                     ", mIsFirstLaunch:" + mIsFirstLaunch + ",mIsSetDepthOnly:" + mIsSetDepthOnly);
                if (mIsCancelThread) {
                    Log.d(TAG, "<GeneRefocusImageTask> cancel generate task.");
                    continue;
                }
                long begin = System.currentTimeMillis();
                TraceHelper.beginSection(">>>>Refocus-generateRefocusImage");
                RefocusImage refocusImage = mRefocusImage.generateRefocusImage(mTouchBitmapCoord[0],
                        mTouchBitmapCoord[1], DEPTHDATA[mCurrentNum]);
                TraceHelper.endSection();
                Log.d(TAG, "<GeneRefocusImageTask><PERF> generateRefocusImage costs "
                        + (System.currentTimeMillis() - begin));
                if (mIsSetDepthOnly) {
                    mRefocusView.setImageActor(refocusImage.image, -1, -1);
                    mIsSetDepthOnly = false;
                } else if (mIsShowRefocusImage) {
                    mRefocusView.setImageActor(refocusImage.image,
                        refocusImage.width, refocusImage.height);
                    mIsShowRefocusImage = false;
                    if (mIsFirstLaunch) {
                        mHandler.sendEmptyMessage(MSG_INIT_FINISH);
                        mIsFirstLaunch = false;
                    }
                } else {
                    mRefocusView.setImageActorNew(refocusImage.image);
                }
                mIsDoingRefocus.compareAndSet(true, false);
                if (!mHasGeneratedEffect) {
                    Log.d(TAG, "<GeneRefocusImageTask> at least generate once");
                    mHasGeneratedEffect = true;
                }
                mHandler.sendEmptyMessage(MSG_GENERATE_DONE);
            }
        }

        public synchronized void notifyDirty() {
            Log.d(TAG, "<GeneRefocusImageTask> notifyDirty");
            mDirty = true;
            notifyAll();
        }

        public synchronized void terminate() {
            mActive = false;
            notifyAll();
        }
    }

    private void initializeViews() {
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.m_refocus_activity);
        initActionBar();
        toggleStatusBarByOrientation();
        mRefocusView = (ReFocusView) findViewById(R.id.refocus_view);
        mRefocusView.setAnimationLevel(RefocusHelper.getMaxAnimationLevelFromCfg(getAssets()));
        mRefocusView.setRefocusListener(this);
        mRefocusView.setZOrderOnTop(false);
        mShowImageTotalDurationTime = RefocusHelper.getTransitionTimeFromCfg(getAssets());
        if (ANIMATION_LEN_FOR_DEBUG > 0) {
            mShowImageTotalDurationTime = ANIMATION_LEN_FOR_DEBUG;
            Log.d(TAG, "<initializeViews> reset animation duration to "
                    + ANIMATION_LEN_FOR_DEBUG);
        }
        Log.d(TAG, "<initializeViews> mShowImagekTotalDurationTime "
                + mShowImageTotalDurationTime);
        mRefocusView.setTransitionTime(mShowImageTotalDurationTime,
                mShowImageFirstDurationTime);
    }

    private void initializeData() {
        setResult(RESULT_CANCELED, new Intent());
        Intent intent = getIntent();
        if (intent == null || !"image/*".equals(intent.getType()) || intent.getData() == null) {
            Log.d(TAG, "<initializeData> intent not correct, finish!!");
            if (mHandler != null) {
                mHandler.sendEmptyMessage(MSG_REFOCUS_ERROR);
            }
            return;
        }
        mSourceUri = intent.getData();
        long begin = System.currentTimeMillis();
        TraceHelper.beginSection(">>>>Refocus-initializeData-querySourceFile");
        if (!querySourceFile()) {
            Log.d(TAG, "<initializeData> querySourceFile fail, finish!!");
            if (mHandler != null) {
                mHandler.sendEmptyMessage(MSG_REFOCUS_ERROR);
            }
            return;
        }
        TraceHelper.endSection();
        Log.d(TAG, "<initializeData><PERF> querySourceFile costs "
                + (System.currentTimeMillis() - begin));
        // set image width and height as default value
        mTouchBitmapCoord[0] = (int) (mImageWidth * DEFAULT_X_COORD_FACTOR);
        mTouchBitmapCoord[1] = (int) (mImageHeight * DEFAULT_Y_COORD_FACTOR);
        mRefocusImage = new ImageRefocus(this);
        mIsShowRefocusImage = RefocusHelper.getFirstGenerateRefocusFlagFromCfg(getAssets());
        startLoadBitmap(mSourceFilePath);
    }

    private void initActionBar() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setBackgroundDrawable(new ColorDrawable(
                    HALF_TRANSPARENT_COLOR));
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            actionBar.setCustomView(R.layout.m_refocus_actionbar);
            mSaveButton = actionBar.getCustomView();
            mSaveButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    saveRefocusBitmap();
                }
            });
        }
    }

    private void errorHandleWhenRefocus() {
        Toast.makeText(this, getString(R.string.m_general_err_tip), Toast.LENGTH_LONG).show();
        finish();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SET_AS) {
            Log.d(TAG, "<onActivityResult> get result from setAs and finish");
            setResult(RESULT_OK, new Intent().setData(mInsertUri));
            finish();
        }
    }

    private boolean querySourceFile() {
        ContentResolver contentResolver = getContentResolver();
        final String[] projection = new String[]{MediaStore.Images.ImageColumns.DATA,
                MediaStore.Images.ImageColumns.WIDTH, MediaStore.Images.ImageColumns.HEIGHT,
                MediaStore.Images.ImageColumns.ORIENTATION};
        Cursor cursor = contentResolver.query(mSourceUri, projection, null, null, null);
        if (cursor == null) {
            Log.d(TAG, "<querySource> cursor is null, return");
            return false;
        }
        try {
            while (cursor.moveToNext()) {
                mSourceFilePath = cursor.getString(0);
                mImageWidth = cursor.getFloat(1);
                mImageHeight = cursor.getFloat(2);
                mImageOrientation = cursor.getInt(3);
                Log.d(TAG, "<querySource> mSourceFilePath: " + mSourceFilePath
                        + ", mImageWidth: " + mImageWidth + ", mImageHeight: " + mImageHeight
                        + ", mImageOrientation: " + mImageOrientation);
            }
        } finally {
            cursor.close();
        }
        return true;
    }
}
