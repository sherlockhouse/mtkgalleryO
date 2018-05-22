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
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;

import com.mediatek.accessor.StereoInfoAccessor;
import com.mediatek.accessor.data.SegmentMaskInfo;
import com.mediatek.adapter.ImageSegment;
import com.mediatek.fancycolor.utils.FancyColorHelper;
import com.mediatek.stereoapplication.MaskBuf;
import com.mediatek.stereoapplication.StereoApplication;
import com.mediatek.stereoapplication.fancycolor.ActionType;
import com.mediatek.stereoapplication.fancycolor.GenerateEffectImgConfig;
import com.mediatek.util.Log;
import com.mediatek.util.TraceHelper;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Fancy Color.
 */
public class FancyColor {
    private static final String TAG = Log.Tag("Fc/FancyColorAP");
    private static final String FANCY_COLOR_NAME = "fancycolor";
    public static final int TYPE_PREVIEW_THUMBNAIL = 1;
    public static final int TYPE_THUMBNAIL = 2;
    public static final int TYPE_HIGH_RES_THUMBNAIL = 3;

    private byte[] mPreviewMask;
    private Point mPreviewMaskPoint = new Point();
    private Rect mPreviewRect = new Rect();

    private byte[] mImageMask;
    private Point mMaskPoint = new Point();
    private Rect mMaskRect = new Rect();

    private byte[] mHighResMask;
    private Point mHighResMaskPoint = new Point();
    private Rect mHighResMaskRect = new Rect();

    private int mMaskWidth;
    private int mMaskHeight;
    private int mMessageCodeWhenError;

    private String mFilePath;
    private Bitmap mOriPreviewBitmap;
    private Bitmap mOriThumbnailBitmap;
    private ArrayList<String> mAllEffectArray = new ArrayList<String>();
    private ArrayList<String> mSelectedEffectArray = new ArrayList<String>();

    private final StereoApplication mStereoApp;
    private final StereoInfoAccessor mAccessor;
    private Handler mHandler;
    private Context mContext;
    private Uri mSourceUri;
    private ImageSegment mImageSegment = null;
    private boolean mHasInitSegment = false;

    /**
     * Get FancyColor instance.
     *
     * @param context         context
     * @param sourceUri       sourceUri
     * @param filePath        file path
     * @param selectedEffects if selectedEffects is null, load all effects, or else load effects
     *                        of certain index in selectedEffects
     */
    public FancyColor(Context context, Uri sourceUri, String filePath, int[] selectedEffects) {
        mContext = context;
        mSourceUri = sourceUri;
        mFilePath = filePath;
        mAccessor = new StereoInfoAccessor();
        mStereoApp = new StereoApplication();
        mImageSegment = new ImageSegment(mContext);

        if (!mStereoApp.initialize(FANCY_COLOR_NAME, null)) {
            Log.d(TAG, "<FancyColor> initialize fail!!");
            Assert.assertTrue(false);
        }
        if (mAllEffectArray.size() == 0) {
            Object[] effects = getFancyColorEffects();
            if (effects == null) {
                Log.d(TAG, "<FancyColor> getFancyColorEffects fail!!");
                Assert.assertTrue(false);
            }
            int effectCount = effects.length;
            for (int i = 0; i < effectCount; i++) {
                mAllEffectArray.add(i, (String) effects[i]);
                Log.d(TAG, "<FancyColor> effect: " + (String) effects[i]);
            }

            if (selectedEffects == null) {
                mSelectedEffectArray = mAllEffectArray;
            } else {
                for (int i = 0; i < selectedEffects.length; i++) {
                    mSelectedEffectArray.add(i, mAllEffectArray.get(selectedEffects[i]));
                    Log.d(TAG, "<FancyColor> load effect: " + mSelectedEffectArray.get(i));
                }
            }
        }
    }

    /**
     * Set handler to notify back to UI thread.
     * @param handler set handler
     * @param messageCodeWhenError message code
     */
    public void setHandler(Handler handler, int messageCodeWhenError) {
        mHandler = handler;
        mMessageCodeWhenError = messageCodeWhenError;
    }

    /**
     * Get all fancy color effects name.
     * @return list of effects name
     */
    public ArrayList<String> getAllFancyColorEffects() {
        return mSelectedEffectArray;
    }

    /**
     * Get effect image.
     * @param src src bitmap
     * @param effectName effect name
     * @param type thumbnail type
     * @return effect bitmap
     */
    public Bitmap getFancyColorEffectImage(Bitmap src, String effectName, int type) {
        if (src == null || effectName == null || mSelectedEffectArray == null
                || !mSelectedEffectArray.contains(effectName)) {
            Log.d(TAG, "<getFancyColorEffectImage> params check error!");
            return null;
        }

        Bitmap effectBitmap = src.copy(Bitmap.Config.ARGB_8888, true);
        if (effectBitmap == null) {
            Log.d(TAG, "<getFancyColorEffectImage> " + effectName + " copy bitmap error!");
            return null;
        }

        byte[] mask;
        Rect rect;
        Point point;
        if (type == TYPE_PREVIEW_THUMBNAIL) {
            mask = mPreviewMask;
            rect = mPreviewRect;
            point = mPreviewMaskPoint;
        } else if (type == TYPE_HIGH_RES_THUMBNAIL) {
            mask = mHighResMask;
            rect = mHighResMaskRect;
            point = mHighResMaskPoint;
        } else {
            mask = mImageMask;
            rect = mMaskRect;
            point = mMaskPoint;
        }

        TraceHelper.beginSection(">>>>FancyColor-getFancyColorEffectImage-" + effectName);
        Log.d(TAG, "<getFancyColorEffectImage> effectBitmap(w & h) & mask &rect & point:" +
                effectBitmap + "(" + effectBitmap.getWidth() + " " +
                effectBitmap.getHeight() + ")" + " " + mask + " " + rect + " " +  point);
        GenerateEffectImgConfig config = new GenerateEffectImgConfig();
        config.bitmap = effectBitmap;
        MaskBuf maskBuf = new MaskBuf();
        maskBuf.mask = mask;
        maskBuf.bufferSize = mask.length;
        maskBuf.point = point;
        maskBuf.rect = rect;
        config.maskBuf = maskBuf;
        config.effectName = effectName;

        long start = System.currentTimeMillis();
        if (!mStereoApp.process(ActionType.ACTION_GENERATE_EFFECT_IMAGE, config, effectBitmap)) {
            Log.d(TAG, "<getFancyColorEffectImage> do process " + effectName + " fail!!");
        }
        Log.d(TAG, "<getFancyColorEffectImage> costTime: " + (System.currentTimeMillis() - start));
        TraceHelper.endSection();
        return effectBitmap;
    }

    /**
     * Init mask buffer.
     * @param type thumbnail type
     * @param bitmap src bitmap
     * @return true if success, or else return false.
     */
    public boolean initMaskBuffer(int type, Bitmap bitmap) {
        TraceHelper.beginSection(">>>>FancyColor-initMaskBuffer");
        Log.d(TAG, "<initMaskBuffer> type & bitmap & mImageMask: " +
                type + " " + bitmap + " " + mImageMask);
        if (mImageMask == null) {
            getImageMask();
        }

        switch (type) {
            case TYPE_THUMBNAIL:
                mOriThumbnailBitmap = bitmap;
                synchronized (this) {
                    if (!initImageSegment(bitmap)) {
                        Log.d(TAG, "<initMaskBuffer> initImageSegment error, return!!");
                        return false;
                    }
                }
                if (mImageMask == null) {
                    return getThumbnailMask(bitmap, ImageSegment.SCENARIO_AUTO, null);
                }
                break;
            case TYPE_PREVIEW_THUMBNAIL:
                return getPreviewMask(bitmap);
            case TYPE_HIGH_RES_THUMBNAIL:
                return getHightResMask(bitmap);
            default:
                Log.d(TAG, "<initMaskBuffer> do nothing for default in switch.");
                return false;
        }
        TraceHelper.endSection();
        return true;
    }

    /**
     * Set mask buffer to segment.
     */
    public void setMaskBufferToSegment() {
        SegmentMaskInfo maskInfo = new SegmentMaskInfo();
        maskInfo.maskBuffer = mImageMask;
        maskInfo.maskWidth = mMaskWidth;
        maskInfo.maskHeight = mMaskHeight;
        maskInfo.segmentX = mMaskPoint.x;
        maskInfo.segmentY = mMaskPoint.y;
        maskInfo.segmentLeft = mMaskRect.left;
        maskInfo.segmentTop = mMaskRect.top;
        maskInfo.segmentRight = mMaskRect.right;
        maskInfo.segmentBottom = mMaskRect.bottom;
        Log.d(TAG, "<setMaskBufferToSegment> SegmentMaskInfo: " + maskInfo);
        ImageSegment.setMaskBuffer(maskInfo);
    }

    /**
     * Reload mask buffer.
     * @param point point
     * @return true if success, or else return false
     */
    public boolean reloadMaskBuffer(Point point) {
        Log.d(TAG, "<reloadMaskBuffer> point:" + point);
        TraceHelper.beginSection(">>>>FancyColor-reloadMaskBuffer");
        boolean res = false;
        if (point != null) {
            res = getThumbnailMask(mOriThumbnailBitmap, ImageSegment.SCENARIO_SELECTION, point);
        } else {
            res = getImageMask();
        }
        if (!res) {
            Log.d(TAG, "<reloadMaskBuffer> fail!!");
            TraceHelper.endSection();
            return false;
        }
        res = getPreviewMask(mOriPreviewBitmap);
        TraceHelper.endSection();
        return res;
    }

    /**
     * release memory.
     */
    public void release() {
        Log.d(TAG, "<release> release in FancyColor");
        mStereoApp.release();
        synchronized (this) {
            if (mImageSegment != null) {
                mImageSegment.release();
                mImageSegment = null;
            }
        }
    }

    private boolean getImageMask() {
        TraceHelper.beginSection(">>>>FancyColor-getImageMask");
        long s1 = System.currentTimeMillis();
        SegmentMaskInfo maskInfo = mAccessor.readSegmentMaskInfo(mFilePath);
        Log.d(TAG, "<getImageMask> readSegmentMaskInfo costTime: " +
                (System.currentTimeMillis() - s1));
        TraceHelper.endSection();

        if (maskInfo != null && maskInfo.maskBuffer != null &&
                maskInfo.maskWidth > 0 && maskInfo.maskHeight > 0) {
            mImageMask = maskInfo.maskBuffer;
            mMaskWidth = maskInfo.maskWidth;
            mMaskHeight = maskInfo.maskHeight;

            Log.d(TAG, "<getImageMask> mFilePath & mImageMask(w & h): " +
                    mFilePath + " " + mImageMask + "(" + mMaskWidth + " " + mMaskHeight + ")");
            mMaskPoint.x = maskInfo.segmentX;
            mMaskPoint.y = maskInfo.segmentY;
            mMaskRect.left = maskInfo.segmentLeft;
            mMaskRect.top = maskInfo.segmentTop;
            mMaskRect.right = maskInfo.segmentRight;
            mMaskRect.bottom = maskInfo.segmentBottom;
        }
        return true;
    }

    public boolean initImageSegment(Bitmap bitmap) {
        if (mImageSegment == null || bitmap == null) {
            Log.d(TAG, "<initImageSegment> new ImageSegment error, return!!");
            return false;
        }
        if (!mHasInitSegment) {
            long start = System.currentTimeMillis();
            boolean isVal = mImageSegment.initSegment(mSourceUri, mFilePath, bitmap, 0);
            Log.d(TAG, "<initImageSegment> bitmap & isVal & costTime: " + bitmap + " " + isVal +
                    " " + (System.currentTimeMillis() - start));
            if (!isVal) {
                mHandler.sendEmptyMessage(mMessageCodeWhenError);
                Log.d(TAG, "<initImageSegment> init ImageSegment error, return!!");
                mImageSegment.release();
                return false;
            }
            mHasInitSegment = true;
            return true;
        }
        return true;
    }

    private boolean getThumbnailMask(Bitmap bitmap, int scenario, Point point) {
        Log.d(TAG, "<getThumbnailMask> bitmap & scenario & point:" +
                bitmap + " " + scenario + " " + point);
        if (bitmap == null) {
            Log.d(TAG, "<getThumbnailMask> bitmap is null, return!!");
            return false;
        }

        long start = System.currentTimeMillis();
        synchronized (this) {
            TraceHelper.beginSection(">>>>FancyColor-getThumbnailMask");
            if (!initImageSegment(bitmap)) {
                Log.d(TAG, "<getThumbnailMask> initImageSegment error, return!!.");
                return false;
            }
            boolean val =
                    mImageSegment.doSegment(scenario, ImageSegment.MODE_OBJECT, null, null, point);
            if (!val) {
                mHandler.sendEmptyMessage(mMessageCodeWhenError);
                Log.d(TAG, "<getThumbnailMask> doSegment error, return!!");
                mImageSegment.release();
                TraceHelper.endSection();
                return false;
            }

            mImageMask = mImageSegment.getSegmentMask();
            mMaskPoint = mImageSegment.getSegmentPoint();
            mMaskRect = mImageSegment.getSegmentRect();
            mMaskWidth = bitmap.getWidth();
            mMaskHeight = bitmap.getHeight();
            TraceHelper.endSection();
            if (FancyColorHelper.DEBUG_FANCYCOLOR_MASK) {
                Log.d(TAG, "<getThumbnailMask> get thumbnail mask.");
                FancyColorHelper.dumpBuffer(mImageMask);
            }
        }
        Log.d(TAG, "<getThumbnailMask> costTime: " + (System.currentTimeMillis() - start));
        return true;
    }

    private boolean getPreviewMask(Bitmap bitmap) {
        if (mImageMask == null || bitmap == null) {
            Log.d(TAG, "<getPreviewMask> parameters unreasonable, do nothing.");
            return false;
        }

        long start = System.currentTimeMillis();
        TraceHelper.beginSection(">>>>FancyColor-getPreviewMask");
        synchronized (this) {
            mImageSegment.setNewBitmap(bitmap, mImageMask, mMaskWidth, mMaskHeight);
            mPreviewMask = mImageSegment.getNewSegmentMask();
            mPreviewMaskPoint = mImageSegment.getNewSegmentPoint();
            mPreviewRect = mImageSegment.getNewSegmentRect();
            Log.d(TAG, "<getPreviewMask> bitmap & mImageMask(mMaskWidth & mMaskHeight) &" +
                    "mPreviewMask & mPreviewMaskPoint & mPreviewRect:" +
                    bitmap + " " + mImageMask + "(" + mMaskWidth + " " + mMaskHeight + ")" + " " +
                    mPreviewMask + " " + mPreviewMaskPoint + " " + mPreviewRect);
            if (FancyColorHelper.DEBUG_FANCYCOLOR_MASK) {
                Log.d(TAG, "<getPreviewMask> get preview mask.");
                FancyColorHelper.dumpBuffer(mPreviewMask);
            }
        }
        mOriPreviewBitmap = bitmap;
        TraceHelper.endSection();
        Log.d(TAG, "<getPreviewMask> costTime: " + (System.currentTimeMillis() - start));
        return true;
    }

    private boolean getHightResMask(Bitmap bitmap) {
        if (mImageMask == null || bitmap == null) {
            Log.d(TAG, "<getHightResMask> parameters unreasonable, do nothing.");
            return false;
        }

        long start = System.currentTimeMillis();
        TraceHelper.beginSection(">>>>FancyColor-getHightResMask");
        synchronized (this) {
            mImageSegment.setNewBitmap(bitmap, mImageMask, mMaskWidth, mMaskHeight);
            mHighResMask = mImageSegment.getNewSegmentMask();
            mHighResMaskPoint = mImageSegment.getNewSegmentPoint();
            mHighResMaskRect = mImageSegment.getNewSegmentRect();
            mMaskWidth = bitmap.getWidth();
            mMaskHeight = bitmap.getHeight();
            if (FancyColorHelper.DEBUG_FANCYCOLOR_MASK) {
                Log.d(TAG, "<getHightResMask> get high resolution mask.");
                FancyColorHelper.dumpBuffer(mHighResMask);
            }
        }
        Log.d(TAG, "<getHightResMask> width & height & costTime: " + bitmap.getWidth() + " " +
                bitmap.getHeight() + " " + (System.currentTimeMillis() - start));
        TraceHelper.endSection();
        return true;
    }

    private String[] getFancyColorEffects() {
        String[] names = (String[]) mStereoApp.getInfo(ActionType.INFO_ALL_EFFECTS_NAME, null);
        if (names == null) {
            Log.d(TAG, "<getFancyColorEffects> <error> effects is null");
            return null;
        }
        Log.d(TAG, "<getFancyColorEffects> size: " + names.length);
        return names;
    }
}