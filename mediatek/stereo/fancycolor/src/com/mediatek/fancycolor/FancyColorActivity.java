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
package com.mediatek.fancycolor;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Toast;

import com.mediatek.fancycolor.parallel.ThreadPool;
import com.mediatek.fancycolor.utils.FancyColorHelper;
import com.mediatek.fancycolor.utils.PermissionUtil;
import com.mediatek.fancycolor.utils.TraceHelper;
import com.mediatek.util.Log;
import com.mediatek.util.StereoImage;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Activity for fancy color effect.
 */
public class FancyColorActivity extends Activity implements EffectManager.DataLoadingListener,
        EffectManager.EffectListener, EffectView.LayoutListener, EffectView.ItemClickListener,
        SavingRequest.SavingRequestListener, ThumbView.ThumbViewClickListener,
        EffectManager.ReloadMaskListener {
    private static final String TAG = Log.Tag("Fc/FancyColorActivity");

    private static final String ACTION_REFINE = "com.mediatek.segment.action.REFINE";
    private static final int REGISTER_ALL_EFFECTS = -1;
    private static final int REQUEST_REFINE = 1;
    private static final int NORMAL_EFFECT_INDEX = 0;

    private EffectView mEffectView;
    private EffectManager mEffectManager;
    private Handler mHandler;
    private ActionBar mActionBar;
    private View mSaveIcon;
    private Intent mIntent;
    private Uri mSourceUri;
    private String mSourcePath;
    private Bitmap mFinalEffectBitmap;
    private Bitmap mThumbEffectBitmap;

    private WeakReference<DialogFragment> mSavingProgressDialog;
    private AtomicBoolean mIsLoading = new AtomicBoolean(false);
    private ArrayList<String> mEffectNameList;
    private HashMap<String, Integer> mEffectNameId;
    private Bitmap[] mPreviousEffectBitmapPreview;
    private int[] mEffectForLoading;
    private boolean mIsAtPreview = true;
    private boolean mGranted = false;
    private int mCurrentThumbViewIndex;
    private int mEffectCount = 0;
    private int mPreviewBitmapWidth;
    private int mPreviewBitmapHeight;
    private int mThumbBitmapWidth;
    private int mThumbBitmapHeight;
    private int mSourceBitmapWidth;
    private int mSourceBitmapHeight;
    private int mGridViewRowCount;
    private int mGridViewColumCount;
    private Menu mMenu;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        TraceHelper.beginSection(">>>>FancyColor-onCreate");
        Log.d(TAG, "<onCreate> begin!");
        super.onCreate(savedInstanceState);
        if (!checkIntentValid()) {
            Log.d(TAG, "<onCreate> check intent fail.");
            TraceHelper.endSection();
            return;
        }

        mGranted = PermissionUtil.checkAndRequestForPhotoPicker(this);
        if (mGranted) {
            Log.d(TAG, "<onCreate> have granted from permissions check, and do initializations.");
            StereoImage.createDisplayRect(this);
            initializeData();
            initializeView();
            showLoadingProgress();
        }

        mHandler = new Handler() {
            public void handleMessage(Message message) {
                switch (message.what) {
                    case FancyColorHelper.MSG_UPDATE_VIEW:
                        updateView(message.arg1, (Bitmap) message.obj);
                        return;
                    case FancyColorHelper.MSG_LOADING_FINISH:
                        if (message.arg1 == EffectManager.TYPE_PREVIEW_THUMBNAIL) {
                            switchView(true, REGISTER_ALL_EFFECTS);
                        }
                        return;
                    case FancyColorHelper.MSG_SAVING_FINISH:
                        setResult(RESULT_OK, new Intent().setData((Uri) message.obj));
                        finish();
                        return;
                    case FancyColorHelper.MSG_RELOAD_THUMB_VIEW:
                        onThumbViewReady(mCurrentThumbViewIndex);
                        return;
                    case FancyColorHelper.MSG_STATE_ERROR:
                        errorHandle();
                        return;
                    case FancyColorHelper.MSG_HIDE_LOADING_PROGRESS:
                        hideLoadingProgress();
                        break;
                    default:
                        Log.d(TAG, "<Handler> unlawful message, do nothing.");
                        break;
                }
            }
        };

        if (mGranted) {
            mEffectManager.setHandler(mHandler);
        }
        Log.d(TAG, "<onCreate> end!");
        TraceHelper.endSection();
    }

    @Override
    public void onBackPressed() {
        TraceHelper.beginSection(">>>>FancyColor-onBackPressed");
        Log.d(TAG, "<onBackPressed> begin!");
        if (mIsAtPreview) {
            super.onBackPressed();
        } else {
            switchView(true, REGISTER_ALL_EFFECTS);
            showLoadingProgress();
        }
        Log.d(TAG, "<onBackPressed> end!");
        TraceHelper.endSection();
    }

    @Override
    public void onLoadingFinish(Bitmap bitmap, int type) {
        fillEffectNames(mEffectManager.getAllEffectsName());
        if (bitmap == null) {
            Log.d(TAG, "<onLoadingFinish> type " + type + " bitmap is null");
            return;
        }
        if (type == EffectManager.TYPE_PREVIEW_THUMBNAIL) {
            mPreviewBitmapWidth = bitmap.getWidth();
            mPreviewBitmapHeight = bitmap.getHeight();
            mHandler.obtainMessage(FancyColorHelper.MSG_LOADING_FINISH, type, -1, null)
                    .sendToTarget();
        } else if (type == EffectManager.TYPE_THUMBNAIL) {
            mThumbBitmapWidth = bitmap.getWidth();
            mThumbBitmapHeight = bitmap.getHeight();
        }
    }

    @Override
    public void onEffectDone(int index, Bitmap bitmap, int type) {
        Log.d(TAG, "<onEffectDone> index & bitmap: " + index + " " + bitmap);
        if (type == EffectManager.TYPE_HIGH_RES_THUMBNAIL) {
            mIsLoading.compareAndSet(true, false);
            mFinalEffectBitmap = bitmap;
            ThreadPool.getInstance().submit(new SavingRequest((Context) this, mSourceUri,
                    mFinalEffectBitmap, mSourceBitmapWidth, mSourceBitmapHeight, this));
        } else {
            mHandler.obtainMessage(FancyColorHelper.MSG_UPDATE_VIEW, index, -1, bitmap)
                    .sendToTarget();
            if (type == EffectManager.TYPE_THUMBNAIL) {
                mIsLoading.compareAndSet(true, false);
                mHandler.sendEmptyMessage(FancyColorHelper.MSG_HIDE_LOADING_PROGRESS);
            }
            if (type == EffectManager.TYPE_PREVIEW_THUMBNAIL) {
                mEffectCount++;
                if (mEffectCount == mGridViewRowCount * mGridViewColumCount) {
                    mHandler.sendEmptyMessage(FancyColorHelper.MSG_HIDE_LOADING_PROGRESS);
                    mEffectCount = 0;
                }
            }
        }
    }

    @Override
    public void onSavingDone(Uri result) {
        Log.d(TAG, "<onSavingDone> uri: " + result);
        mHandler.obtainMessage(FancyColorHelper.MSG_SAVING_FINISH, result).sendToTarget();
    }

    @Override
    public void onItemClick(int position) {
        TraceHelper.beginSection(">>>>FancyColor-onItemClick");
        Log.d(TAG, "<onItemClick> click position " + position);
        switchView(false, position);
        showLoadingProgress();
        TraceHelper.endSection();
    }

    @Override
    public void onGridViewReady(int index) {
        mEffectManager.requestEffectBitmap(index, EffectManager.TYPE_PREVIEW_THUMBNAIL);
    }

    @Override
    public void onThumbViewReady(int index) {
        mEffectManager.requestEffectBitmap(index, EffectManager.TYPE_THUMBNAIL);
        mCurrentThumbViewIndex = index;
    }

    @Override
    public void onThumbViewClick(int viewW, int viewH, float x, float y) {
        if (mCurrentThumbViewIndex == NORMAL_EFFECT_INDEX) {
            return;
        }
        if (!mIsLoading.compareAndSet(false, true)) {
            Log.d(TAG, "<onThumbViewClick> compareAndSet mIsLoading: " + mIsLoading.get());
            return;
        }
        Log.d(TAG, "<onThumbViewClick> viewW " +
                viewW + ", viewH " + viewH + ", x " + x + ", y " + y);
        Point point = calcClickPosition(viewW, viewH, x, y);
        if (point != null) {
            showLoadingProgress();
            mEffectManager.reloadMask(this, point);
        } else {
            Log.d(TAG, "<onThumbViewClick> point is null, and do reset loading state.");
            mIsLoading.compareAndSet(true, false);
        }
    }

    @Override
    public void onReloadMaskDone() {
        Log.d(TAG, "<onReloadMaskDone> index " + mCurrentThumbViewIndex);
        mHandler.obtainMessage(FancyColorHelper.MSG_RELOAD_THUMB_VIEW, null).sendToTarget();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mIsAtPreview) {
            mEffectView.onOrientationChange();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.m_fancy_color_menu, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int action = item.getItemId();
        switch (action) {
            case R.id.action_refine:
                mEffectManager.setMaskBufferToSegment();
                startRefineActivity();
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mGranted = PermissionUtil.isAllPermissionsGranted(permissions, grantResults);
        Log.d(TAG, "<onRequestPermissionsResult> " + "mGranted: " + mGranted);
        if (mGranted) {
            initializeData();
            initializeView();
            showLoadingProgress();
            mEffectManager.setHandler(mHandler);
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
        Log.d(TAG, "<onResume> begin!");
        super.onResume();
        Log.d(TAG, "<onResume> end!");
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "<onPause> begin!");
        if (null != mMenu) {
            mMenu.close();
        }
        hideSavingProgress();
        super.onPause();
        Log.d(TAG, "<onPause> end!");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        hideLoadingProgress();
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "<onDestroy> begin!");
        super.onDestroy();
        if (null != mEffectManager) {
            mEffectManager.unregisterAllEffect();
            mEffectManager.release();
        }
        releaseBitmapFinal();
        Log.d(TAG, "<onDestroy> end!");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "<onActivityResult> resultCode:" + resultCode);
        if (requestCode == REQUEST_REFINE && resultCode == RESULT_OK) {
            showLoadingProgress();
            mEffectManager.reloadMask(this, null);
        }
    }

    private Point calcClickPosition(int viewW, int viewH, float x, float y) {
        int thumbW = mThumbBitmapWidth;
        int thumbH = mThumbBitmapHeight;
        int gap;
        int mapX;
        int mapY;
        Rect validRange = new Rect();
        if (thumbH >= thumbW) {
            // bitmap height fulfills view height
            gap = (viewW - viewH * thumbW / thumbH) / 2;
            validRange.left = gap;
            validRange.right = viewW - gap;
            validRange.top = 0;
            validRange.bottom = viewH;
            mapX = thumbW * ((int) x - gap) / validRange.width();
            mapY = thumbH * (int) y / validRange.height();
        } else {
            // bitmap width fullfills view width
            gap = (viewH - viewW * thumbH / thumbW) / 2;
            validRange.left = 0;
            validRange.right = viewW;
            validRange.top = gap;
            validRange.bottom = viewH - gap;
            mapX = thumbW * (int) x / validRange.width();
            mapY = thumbH * ((int) y - gap) / validRange.height();
        }
        Log.d(TAG, "<calcClickPosition> thumbW " + thumbW + ", thumbH " + thumbH + ", gap "
                + gap + ", rect lrtb " + validRange.left + ", " + validRange.right + ", "
                + validRange.top + ", " + validRange.bottom + ", mapX " + mapX + ", mapY " + mapY);
        if (!validRange.contains((int) x, (int) y)) {
            Log.d(TAG, "<calcClickPosition> invalid click");
            return null;
        }
        return new Point(mapX, mapY);
    }

    private void releaseBitmap(int index, Bitmap bitmap) {
        Log.d(TAG, "<releaseBitmap> index & bitmap: " + index + " " + bitmap);
        if (mPreviousEffectBitmapPreview != null) {
            if (mPreviousEffectBitmapPreview[index] != null) {
                mPreviousEffectBitmapPreview[index].recycle();
                mPreviousEffectBitmapPreview[index] = null;
            }
            mPreviousEffectBitmapPreview[index] = bitmap;
        } else {
            Log.d(TAG, "<releaseBitmap> mPreviousEffectBitmapPreview is null, release nothing.");
        }
    }

    private void releaseBitmapFinal() {
        Log.d(TAG, "<releaseBitmapFinal>");
        if (mThumbEffectBitmap != null) {
            mThumbEffectBitmap.recycle();
            mThumbEffectBitmap = null;
        }
        if (mFinalEffectBitmap != null) {
            mFinalEffectBitmap.recycle();
            mFinalEffectBitmap = null;
        }
        if (mPreviousEffectBitmapPreview != null) {
            for (Bitmap bitmap : mPreviousEffectBitmapPreview) {
                if (bitmap != null) {
                    bitmap.recycle();
                    bitmap = null;
                }
            }
            mPreviousEffectBitmapPreview = null;
        }
    }

    private void updateView(int index, Bitmap bitmap) {
        mEffectView.updateView(mIsAtPreview, index, bitmap);
        if (mIsAtPreview) {
            releaseBitmap(index, bitmap);
        } else {
            if (mThumbEffectBitmap != null) {
                mThumbEffectBitmap.recycle();
                mThumbEffectBitmap = null;
            }
            mThumbEffectBitmap = bitmap;
        }
    }

    private void registerEffects() {
        mEffectManager.registerEffect(this);
    }

    /**
     * Switch between gridview and imageview.
     *
     * @param isAtPreview indicate at preview or not
     * @param index       if isAtPreview is true, do not care index, register all
     *                    effects if isAtPreview is false, just only register the effect
     *                    of this index
     */
    private void switchView(boolean isAtPreview, int index) {
        Log.d(TAG, "<switchView> isAtPreview & index: " + isAtPreview + " " + index);
        if (isAtPreview) {
            if (mActionBar != null) {
                mActionBar.hide();
            }
            if (!mEffectView.init(this, mPreviewBitmapWidth, mPreviewBitmapHeight)) {
                Log.d(TAG, "<switchView> switch to gridview fail!");
            }
        } else {
            if (mActionBar != null) {
                mActionBar.show();
            }
            if (!mEffectView.init(this, index)) {
                Log.d(TAG, "<switchView> switch to imageview fail!");
            }
        }
        mIsAtPreview = isAtPreview;
    }

    private void setActionBar() {
        mActionBar = getActionBar();
        if (mActionBar != null) {
            mActionBar.hide();
            mActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            mActionBar.setCustomView(R.layout.m_fancy_color_actionbar);
            mSaveIcon = mActionBar.getCustomView();
            mSaveIcon.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    saveImage(mCurrentThumbViewIndex);
                }
            });
        }
    }

    private void initializeData() {
        mSourceUri = mIntent.getData();
        querySourceFile();
        loadSpecFromConfig();
        mPreviousEffectBitmapPreview = new Bitmap[mGridViewRowCount * mGridViewColumCount];
        mEffectManager = new EffectManager(this, mSourceUri, mSourcePath, mEffectForLoading, this);
        registerEffects();
    }

    private void initializeView() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setActionBar();
        mEffectView = new EffectView(this, this);
        mEffectView.setGridViewSpec(mGridViewRowCount, mGridViewColumCount);
    }

    private void saveImage(int index) {
        Log.d(TAG, "<saveImage> inlet");
        if (!mIsLoading.compareAndSet(false, true)) {
            Log.d(TAG, "<saveImage> compareAndSet mIsLoading: " + mIsLoading.get());
            return;
        }

        if (!mIsAtPreview) {
            showSavingProgress(null);
            mEffectManager.requestEffectBitmap(index, EffectManager.TYPE_HIGH_RES_THUMBNAIL);
        }
    }

    private void showSavingProgress(String albumName) {
        DialogFragment fragment;

        if (mSavingProgressDialog != null) {
            fragment = mSavingProgressDialog.get();
            if (fragment != null) {
                if (!fragment.isAdded()) {
                    fragment.show(getFragmentManager(), null);
                }
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

    private void hideSavingProgress() {
        Log.d(TAG, "<hideSavingProgress> inlet");
        if (mSavingProgressDialog != null) {
            DialogFragment progress = mSavingProgressDialog.get();
            if (progress != null) {
                progress.dismissAllowingStateLoss();
            }
        }
    }

    private void showLoadingProgress() {
        Log.d(TAG, "<showLoadingProgress> inlet");
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        DialogFragment fragment =
                (DialogFragment) getFragmentManager().findFragmentByTag("loading");
        if (fragment != null) {
            if (!fragment.isAdded()) {
                fragment.setCancelable(false);
                fragment.show(ft, "loading");
            }
        } else {
            fragment = ProgressFragment.newInstance();
            fragment.setCancelable(false);
            fragment.show(ft, "loading");
        }
    }

    private void hideLoadingProgress() {
        Log.d(TAG, "<hideLoadingProgress> inlet");
        DialogFragment fragment =
                (DialogFragment) getFragmentManager().findFragmentByTag("loading");
        if (fragment != null) {
            fragment.dismissAllowingStateLoss();
        }
    }

    private void errorHandle() {
        Toast.makeText(this, getString(R.string.m_general_err_tip), Toast.LENGTH_LONG).show();
        finish();
    }

    private void fillEffectNames(ArrayList<String> effectNameList) {
        if (mEffectNameList == null || mEffectNameList.isEmpty()) {
            mEffectNameList = new ArrayList<String>();
            mEffectNameId = new HashMap<String, Integer>();
            mEffectNameId.put(FancyColorHelper.EFFECT_NAME_NORMAL,
                    (Integer) R.string.m_fancy_color_effect_normal);
            mEffectNameId.put(FancyColorHelper.EFFECT_NAME_MONO_CHROME,
                    (Integer) R.string.m_fancy_color_effect_monochrome);
            mEffectNameId.put(FancyColorHelper.EFFECT_NAME_POSTERIZE,
                    (Integer) R.string.m_fancy_color_effect_posterize);
            mEffectNameId.put(FancyColorHelper.EFFECT_NAME_RADIAL_BLUR,
                    (Integer) R.string.m_fancy_color_effect_radial_blur);
            mEffectNameId.put(FancyColorHelper.EFFECT_NAME_STROKE,
                    (Integer) R.string.m_fancy_color_effect_stroke);
            mEffectNameId.put(FancyColorHelper.EFFECT_NAME_SIHOUETTE,
                    (Integer) R.string.m_fancy_color_effect_silhouette);
            mEffectNameId.put(FancyColorHelper.EFFECT_NAME_WHITE_BOARD,
                    (Integer) R.string.m_fancy_color_effect_whiteboard);
            mEffectNameId.put(FancyColorHelper.EFFECT_NAME_BLACK_BOARD,
                    (Integer) R.string.m_fancy_color_effect_blackboard);
            mEffectNameId.put(FancyColorHelper.EFFECT_NAME_NEGATIVE,
                    (Integer) R.string.m_fancy_color_effect_negative);
            for (int i = 0; i < effectNameList.size(); i++) {
                String effect = "";
                Integer nameId = mEffectNameId.get(effectNameList.get(i));
                if (nameId != null) {
                    int resId = nameId.intValue();
                    effect = getString(resId);
                }
                mEffectNameList.add(effect);
            }
            mEffectView.setEffectName(mEffectNameList);
        }
    }

    private void loadSpecFromConfig() {
        mGridViewRowCount = FancyColorHelper.getRowCount();
        mGridViewColumCount = FancyColorHelper.getColumCount();
        mEffectForLoading = FancyColorHelper.getEffectsFromConfig();
    }

    private boolean checkIntentValid() {
        mIntent = getIntent();
        if (mIntent == null || !"image/*".equals(mIntent.getType()) || mIntent.getData() == null) {
            Log.d(TAG, "<checkIntentValid> intent not correct, finish!!");
            finish();
            return false;
        }
        return true;
    }

    private void querySourceFile() {
        ContentResolver contentResolver = getContentResolver();
        final String[] projection = new String[]{MediaStore.Images.ImageColumns.DATA,
                MediaStore.Images.ImageColumns.WIDTH, MediaStore.Images.ImageColumns.HEIGHT};
        Cursor cursor = contentResolver.query(mSourceUri, projection, null, null, null);
        if (cursor == null) {
            Log.d(TAG, "<querySourceFile> cursor is null, do nothing.");
            return;
        }

        try {
            while (cursor.moveToNext()) {
                mSourcePath = cursor.getString(0);
                mSourceBitmapWidth = cursor.getInt(1);
                mSourceBitmapHeight = cursor.getInt(2);
                Log.d(TAG, "<querySourceFile> mSourcePath & Width & Height: " +
                        mSourcePath + " " + mSourceBitmapWidth + " " + mSourceBitmapHeight);
            }
        } finally {
            cursor.close();
        }
    }

    private void startRefineActivity() {
        Intent intent = new Intent(ACTION_REFINE);
        intent.setDataAndType(mSourceUri, "image/*");
        Log.d(TAG, "<startRefineActivity> intent: " + intent);
        startActivityForResult(intent, REQUEST_REFINE);
    }
}
