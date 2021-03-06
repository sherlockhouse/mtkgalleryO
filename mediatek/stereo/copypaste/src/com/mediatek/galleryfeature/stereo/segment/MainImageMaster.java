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

package com.mediatek.galleryfeature.stereo.segment;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.provider.MediaStore.Images;

import com.mediatek.util.Log;
import com.mediatek.util.StereoImage;
import com.mediatek.util.TraceHelper;

/**
 * Master the attributes and operations of <b>the main image in ImageShow</b>.
 */
public class MainImageMaster {
    @SuppressWarnings("unused")
    private static final String LOGTAG = Log.Tag("Cp/MainImageMaster");

    private static final boolean DISABLEZOOM = false;
    private static final int MAX_BITMAP_DIM = 2000;
    private static final float DEFAULT_MAX_SCALE_FACTOR = 3.0f;

    private Bitmap mBitmap = null;
    private Rect mOriginalBounds;
    private Uri mUri = null;

    private final Activity mActivity;
    private float mScaleFactor = 1.0f;
    private float mMaxScaleFactor = DEFAULT_MAX_SCALE_FACTOR;
    private Point mTranslation = new Point();
    private Point mOriginalTranslation = new Point();
    private Point mImageShowSize = new Point();

    /**
     * Constructor.
     * @param activity the Android contexual Activity.
     */
    public MainImageMaster(Activity activity) {
        mActivity = activity;
    }

    /**
     * Get the (downsampled) bitmap of the main image.
     * @return the bitmap.
     */
    public Bitmap getBitmap() {
        return mBitmap;
    }

    /**
     * Get the original bounds of the main image.<br/>
     * Original bounds is the dimension of image without downsampled and is orientation
     * considered.
     * @return dimension of the image.
     */
    public Rect getOriginalBounds() {
        return mOriginalBounds;
    }


    /**
     * Get the uri of the main image.
     * @return the uri.
     */
    public Uri getUri() {
        return mUri;
    }

    /**
     * Load the main image at a given URI that is perperly downsampled.
     * @param uri URI of image.
     * @return true if the main image is successfully loaded.
     */
    public boolean loadBitmap(Uri uri) {
        TraceHelper.beginSection(">>>>MainImageMaster-loadBitmap-getMetadataOrientation");
        int orientation = ImageLoader.getMetadataOrientation(mActivity, uri);
        TraceHelper.endSection();
        Rect originalBounds = new Rect();
        TraceHelper.beginSection(">>>>MainImageMaster-loadBitmap-loadOrientedConstrainedBitmap");
        mBitmap = loadOrientedConstrainedBitmap(uri, mActivity, orientation,
                originalBounds);
        TraceHelper.endSection();
        setOriginalBounds(originalBounds);
        if (mBitmap == null) {
            return false;
        }

        if ((originalBounds.width() > originalBounds.height())
                ^ (mBitmap.getWidth() > mBitmap.getHeight())) {
            originalBounds = new Rect(0, 0, originalBounds.height(), originalBounds.width());
            setOriginalBounds(originalBounds);
        }
        mUri = uri;;
        return true;
    }

    /**
     * Set size of ImageShow.
     * @param w width of ImageShow.
     * @param h height of ImageShow.
     */
    public void setImageShowSize(int w, int h) {
        if (mOriginalBounds == null || mOriginalBounds.height() <= 0 || mOriginalBounds.width()
                <= 0) {
            return;
        }
        if (mImageShowSize.x != w || mImageShowSize.y != h) {
            mImageShowSize.set(w, h);
            float maxWidth = mOriginalBounds.width() / (float) w;
            float maxHeight = mOriginalBounds.height() / (float) h;
            mMaxScaleFactor = Math.max(DEFAULT_MAX_SCALE_FACTOR, Math.max(maxWidth, maxHeight));
        }
    }

    /**
     * Get transform matrix mapping from image to screen(ImageShow).<br/>
     * TODO This is not well designed. No parameter is necessary in the future
     * version, cause they should be obtained from mBitmap & mImageShowSize. But
     * such refactor needs more testing for possible timing reason (especially
     * for parameter w & h).
     *
     * @param bw
     *            the width of the image.
     * @param bh
     *            the height of the image.
     * @param w
     *            the width of the ImageShow.
     * @param h
     *            the height of the ImageShow.
     * @return image to screen transform matrix.
     */
    public Matrix getImageToScreenMatrix(int bw, int bh, int w, int h) {
        Matrix m = new Matrix();
        if (getOriginalBounds() == null || w == 0 || h == 0) {
            return m;
        }

        float scale = 1f;
        float translateX = 0;
        float translateY = 0;

        RectF size = new RectF(0, 0, bw, bh);
        scale = w / size.width();
        if (scale * size.height() > h) {
            scale = h / size.height();
        }
        translateX = (w - (size.width() * scale)) / 2.0f;
        translateY = (h - (size.height() * scale)) / 2.0f;

        Point translation = getTranslation();
        m.postScale(scale, scale);
        m.postTranslate(translateX, translateY);
        m.postScale(getScaleFactor(), getScaleFactor(), w / 2.0f, h / 2.0f);
        m.postTranslate(translation.x * getScaleFactor(), translation.y * getScaleFactor());
        return m;
    }

    /**
     * Get scale factor.
     * @return scle factor.
     */
    public float getScaleFactor() {
        return mScaleFactor;
    }

    /**
     * set scale factor.
     * @param scaleFactor the scale factor.
     */
    public void setScaleFactor(float scaleFactor) {
        if (DISABLEZOOM) {
            return;
        }
        if (scaleFactor == mScaleFactor) {
            return;
        }
        mScaleFactor = scaleFactor;
    }

    /**
     * Get translation.
     * @return translation.
     */
    public Point getTranslation() {
        return mTranslation;
    }

    /**
     * Set translation.
     * @param translation translation to set.
     */
    public void setTranslation(Point translation) {
        if (DISABLEZOOM) {
            mTranslation.x = 0;
            mTranslation.y = 0;
            return;
        }
        mTranslation.x = translation.x;
        mTranslation.y = translation.y;
    }

    /**
     * Get the translation when start an animation or gesture.
     * @return translation corresponding to the start of an animation or gesture.
     */
    public Point getOriginalTranslation() {
        return mOriginalTranslation;
    }

    /**
     * Set the translation corresponding to the start an animation or gesture.<br/>
     * Original translation is used during an animation (gesture resulted animation included.
     * @param originalTranslation the translation.
     */
    public void setOriginalTranslation(Point originalTranslation) {
        if (DISABLEZOOM) {
            return;
        }
        mOriginalTranslation.x = originalTranslation.x;
        mOriginalTranslation.y = originalTranslation.y;
    }

    /**
     * Reset translation, which is equal to call setTranslation().
     */
    public void resetTranslation() {
        mTranslation.x = 0;
        mTranslation.y = 0;
    }

    /**
     * Get the max scale factor for the main image in the ImageShow.
     * @return max scale factor.
     */
    public float getMaxScaleFactor() {
        return DISABLEZOOM ? 1 : mMaxScaleFactor;
    }

    private void setOriginalBounds(Rect r) {
        mOriginalBounds = r;
    }

    /**
     * Load a bitmap at a given URI that is downsampled so that both sides are
     * smaller than maxSideLength. The Bitmap's original dimensions are stored
     * in the rect originalBounds. The output is also transformed to the given
     * orientation.
     *
     * @param uri
     *            URI of image to open.
     * @param context
     *            context whose ContentResolver to use.
     * @param orientation
     *            the orientation to transform the bitmap to.
     * @param originalBounds
     *            set to the actual bounds of the stored bitmap.
     * @return downsampled bitmap or null if this operation failed.
     */
    private static Bitmap loadOrientedConstrainedBitmap(Uri uri, Context context,
            int orientation, Rect originalBounds) {
        if (uri == null || context == null) {
            throw new IllegalArgumentException("bad argument to getScaledBitmap");
        }
        // Get width and height of stored bitmap
        Rect storedBounds = ImageLoader.loadBitmapBounds(context, uri);
        if (originalBounds != null) {
            originalBounds.set(storedBounds);
        }
        int w = storedBounds.width();
        int h = storedBounds.height();
        if (w <= 0 || h <= 0) {
            return null;
        }

        int sampleSize  = StereoImage.getSampleSize(storedBounds);
        Log.d(LOGTAG , "sampleSize = " + sampleSize);
        Bitmap bmap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        options.inSampleSize = sampleSize;

        android.database.Cursor c = null;
        boolean isDepth = false;
        String filePath = null;
        try {
            c = context.getContentResolver().query(uri,
                    new String[] { SegmentUtils.CAMERA_REFOCUS, Images.ImageColumns.DATA },
                    null, null, null);
            if (c != null && c.moveToFirst()) {
                isDepth = (c.getInt(0) == 1);
                filePath = c.getString(1);
            }
        } catch (IllegalStateException e) {
            Log.e(LOGTAG,
                    "<loadBitmap> Exception when trying to fetch DATA info", e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
        if (isDepth && filePath != null) {
            bmap = StereoImage.decodeStereoImage(filePath, sampleSize);
        } else {
            bmap = ImageLoader.loadBitmap(context, uri, options);
        }

        if (bmap != null) {
            bmap = SegmentUtils.orientBitmap(bmap, orientation);
            if (bmap.getConfig() != Bitmap.Config.ARGB_8888) {
                bmap = bmap.copy(Bitmap.Config.ARGB_8888, true);
            }
        }
        return bmap;
    }
}
