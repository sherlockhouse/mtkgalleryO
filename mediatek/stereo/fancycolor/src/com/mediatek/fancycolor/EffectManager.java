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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.Handler;

import com.mediatek.fancycolor.parallel.ThreadPool;
import com.mediatek.fancycolor.utils.FancyColorHelper;
import com.mediatek.util.Log;

import java.util.ArrayList;

/**
 * EffectManager manages fancy color effects and requests.
 */
class EffectManager implements EffectRequest.EffectRequestListener,
        ThumbnailLoadRequest.DataLoadingListener, ReloadMaskRequest.ReloadMaskListener {
    private static final String TAG = Log.Tag("Fc/EffectManager");

    public static final int TYPE_PREVIEW_THUMBNAIL = FancyColor.TYPE_PREVIEW_THUMBNAIL;
    public static final int TYPE_THUMBNAIL = FancyColor.TYPE_THUMBNAIL;
    public static final int TYPE_HIGH_RES_THUMBNAIL = FancyColor.TYPE_HIGH_RES_THUMBNAIL;

    private int mSavingEffectIndex;
    private Bitmap mOriPreviewBitmap;
    private Bitmap mOriThumbnailBitmap;
    private Bitmap mOriHiResBitmap;
    private String mSourcePath;
    private FancyColor mFancyColor;
    private Handler mHandler;
    private ArrayList<String> mAllEffectList;
    private int[] mSelectedEffects;
    private EffectListener mEffectListener;
    private DataLoadingListener mDataLoadingListener;
    private ReloadMaskListener mReloadMaskListener;
    private Context mContext;
    private Uri mSourceUri;

    /**
     * Create EffectManager.
     *
     * @param filePath
     *            source path
     * @param selectedEffects
     *            indicates which effect will be loaded e.g.[1,2,5,6] means
     *            effect 1, 2, 5,6 in mAllEffectList will be loaded
     * @param dataLoadingListener
     *            data listener
     */
    public EffectManager(Context context, Uri sourceUri, String filePath, int[] selectedEffects,
                         DataLoadingListener dataLoadingListener) {
        mContext = context;
        mSourceUri = sourceUri;
        mSourcePath = filePath;
        mSelectedEffects = selectedEffects;
        mDataLoadingListener = dataLoadingListener;
        ThreadPool.getInstance().submit(new ThumbnailLoadRequest(mSourcePath,
                FancyColor.TYPE_THUMBNAIL, this));
    }

    @Override
    public void onEffectRequestDone(int index, Bitmap bitmap, int type) {
        if (mEffectListener != null) {
            mEffectListener.onEffectDone(index, bitmap, type);
            Log.d(TAG, "<onEffectRequestDone> index " + index + ", type " + type + ", bitmap "
                    + bitmap);
        }
    }

    @Override
    public void onLoadingFinish(Bitmap bitmap, int type) {
        Log.d(TAG, "<onLoadingFinish> [inlet] bitmap & type: " + bitmap + " " + type);
        if (type == FancyColor.TYPE_PREVIEW_THUMBNAIL) {
            mOriPreviewBitmap = bitmap;
        } else if (type == FancyColor.TYPE_THUMBNAIL) {
            mOriThumbnailBitmap = bitmap;
        } else if (type == FancyColor.TYPE_HIGH_RES_THUMBNAIL) {
            mOriHiResBitmap = bitmap;
        }
        initFancyColor();

        if (!mFancyColor.initMaskBuffer(type, bitmap)) {
            Log.d(TAG, "<onLoadingFinish> initMaskBuffer error!");
            return;
        }

        Log.d(TAG, "<onLoadingFinish> type & bitmap: " + type + " " + bitmap);
        if (mDataLoadingListener != null) {
            mDataLoadingListener.onLoadingFinish(bitmap, type);
        }
        if (type == FancyColor.TYPE_THUMBNAIL) {
            ThreadPool.getInstance().submit(new ThumbnailLoadRequest(mSourcePath,
                    FancyColor.TYPE_PREVIEW_THUMBNAIL, this));
        }
        if (type == FancyColor.TYPE_HIGH_RES_THUMBNAIL) {
            requestEffectBitmap(mSavingEffectIndex, type);
        }
    }

    @Override
    public void onReloadMaskDone() {
        if (mReloadMaskListener != null) {
            mReloadMaskListener.onReloadMaskDone();
        }
    }

    public void reloadMask(ReloadMaskListener reloadMaskListener, Point point) {
        mReloadMaskListener = reloadMaskListener;
        ThreadPool.getInstance().submit(new ReloadMaskRequest(
                mFancyColor, point, this));
    }

    public void registerEffect(EffectListener listener) {
        if (listener != null) {
            mEffectListener = listener;
        }
    }

    public ArrayList<String> getAllEffectsName() {
        return mAllEffectList;
    }

    public void unregisterAllEffect() {
        mEffectListener = null;
        mDataLoadingListener = null;
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    public void setMaskBufferToSegment() {
        mFancyColor.setMaskBufferToSegment();
    }

    public void requestEffectBitmap(int index, int type) {
        Log.d(TAG, "<requestEffectBitmap> index & type: " + index + " " + type);
        Bitmap bitmap = null;
        if (type == FancyColor.TYPE_PREVIEW_THUMBNAIL && mOriPreviewBitmap != null) {
            bitmap = mOriPreviewBitmap;
        } else if (type == FancyColor.TYPE_THUMBNAIL && mOriThumbnailBitmap != null) {
            bitmap = mOriThumbnailBitmap;
        } else if (type == FancyColor.TYPE_HIGH_RES_THUMBNAIL) {
            if (mOriHiResBitmap == null) {
                mSavingEffectIndex = index;
                ThreadPool.getInstance().submit(new ThumbnailLoadRequest(mSourcePath,
                        FancyColor.TYPE_HIGH_RES_THUMBNAIL, this));
                return;
            }
            bitmap = mOriHiResBitmap;
        } else {
            Log.d(TAG, "<requestEffectBitmap> error, index & type: " + index + " " + type);
            return;
        }
        ThreadPool.getInstance().submit(new EffectRequest(mFancyColor, index,
                mAllEffectList.get(index), bitmap, type, this));
    }

    /**
     * When effect done, this listener will be called back.
     */
    public interface EffectListener {
        public void onEffectDone(int index, Bitmap bitmap, int type);
    }

    /**
     * When loading finish, this listener will be called back.
     */
    public interface DataLoadingListener {
        public void onLoadingFinish(Bitmap bitmap, int type);
    }

    /**
     * When reloadMaskDone, this listener will be called back.
     */
    public interface ReloadMaskListener {
        public void onReloadMaskDone();
    }

    public void release() {
        if (null != mFancyColor) {
            mFancyColor.release();
        }
        Bitmap[] bitmaps = new Bitmap[] {mOriPreviewBitmap, mOriThumbnailBitmap, mOriHiResBitmap};
        for (Bitmap bitmap : bitmaps) {
            if (bitmap != null) {
                bitmap.recycle();
                bitmap = null;
            }
        }
        bitmaps = null;
    }

    private void initFancyColor() {
        if (mFancyColor == null) {
            mFancyColor = new FancyColor(mContext, mSourceUri, mSourcePath, mSelectedEffects);
            mFancyColor.setHandler(mHandler, FancyColorHelper.MSG_STATE_ERROR);
            mAllEffectList = mFancyColor.getAllFancyColorEffects();
        }
    }
}
