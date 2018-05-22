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

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.mediatek.galleryfeature.stereo.segment.ImageShow;
import com.mediatek.util.Log;
import com.mediatek.util.StereoImage;

/**
 * ImageShow for StereoSynthActivity. See
 * com.mediatek.galleryfeature.stereo.segment.ImageShow.
 */
public class ImageShowSynth extends ImageShow implements OverLayController.StateListener {
    private static final String LOGTAG = Log.Tag("Cp/ImageShowSynth");

    public static final int MODE_SRC_FOREMOST = 1;
    public static final int MODE_OBJ_FOREMOST = 2;

    private OverLayController mOverLayController;
    private Bitmap mBitmapCopySource;
    private Bitmap mCurrentForeground;
    private int mMode;

    /**
     * Constructor.
     *
     * @param context
     *            The Context the view is running in, through which it can
     *            access the current theme, resources, etc.
     * @param attrs
     *            The attributes of the XML tag that is inflating the view.
     */
    public ImageShowSynth(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (mOverLayController == null) {
            mOverLayController = new OverLayController(context, this);
        }
    }

    /**
     * Constructor.
     *
     * @param context
     *            The Context the view is running in, through which it can
     *            access the current theme, resources, etc.
     */
    public ImageShowSynth(Context context) {
        super(context);
        if (mOverLayController == null) {
            mOverLayController = new OverLayController(context, this);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (changed && mOverLayController != null) {
            mOverLayController.configChanged();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mOverLayController.getTouchEvent(event);
        super.onTouchEvent(event);
        return true;
    }

    @Override
    public void doDraw(Canvas canvas) {
        Bitmap preview = mMasterImage.getBitmap();

        canvas.save();

        drawImages(canvas, preview);

        if (mBitmapCopySource != null) {
            if (mMode == MODE_SRC_FOREMOST || mMaskSimulator == null) {
                mOverLayController.drawBitmap(canvas, preview, mBitmapCopySource);
            } else {
                mOverLayController.drawBitmap(canvas, preview, mBitmapCopySource);
                if (mCurrentForeground == null) {
                    mCurrentForeground = mMaskSimulator.getForground(preview);
                }
                if (mCurrentForeground != null) {
                    Matrix m = mMasterImage.getImageToScreenMatrix(preview.getWidth(),
                            preview.getHeight(), getWidth(), getHeight());
                    if (m == null) {
                        return;
                    }
                    Rect originRect = new Rect(0, 0, mCurrentForeground.getWidth(),
                            mCurrentForeground.getHeight());
                    RectF forgrounRectF = new RectF();
                    m.mapRect(forgrounRectF, new RectF(mMaskSimulator.getClippingBox()));
                    canvas.drawBitmap(mCurrentForeground,
                            originRect, forgrounRectF, new Paint());
                }
            }
        }

        canvas.restore();
    }

    @Override
    public void invaliable() {
        invalidate();
    }

    @Override
    public void recycle() {
        if (mCurrentForeground != null) {
            mCurrentForeground.recycle();
            mCurrentForeground = null;
        }

    }

    /**
     * Get the synthesized bitmap after editing.
     *
     * @return the background substituted bitmap.
     */
    public Bitmap getSynthBitmap() {
        if (mBitmapCopySource != null) {
            BitmapFactory.Options ops = new BitmapFactory.Options();
            ops.inMutable = true;
            RectF windowsRect = StereoImage.getDisplayRect();
            RectF displayRect =
                    new RectF(0, 0, windowsRect.width() * 2, windowsRect.height() * 2);
            ops.inSampleSize =
                    StereoImage.getSampleSize(displayRect, mMasterImage.getOriginalBounds());
            Log.d(LOGTAG, "SampleSize = " + ops.inSampleSize);
            Bitmap bgBmp = loadBitmap(getContext(), mMasterImage.getUri(), ops);
            if (bgBmp == null) {
                Log.e(LOGTAG, "<getSynthBitmap> loading target bitmap failed!");
                bgBmp = mMasterImage.getBitmap();
            }
            Canvas bgCanvas = new Canvas(bgBmp);

            if (mMode == MODE_SRC_FOREMOST || mMaskSimulator == null) {
               // draw over layer.
                if (mOverLayController != null) {
                    mOverLayController.drawBitmap(bgCanvas);
                }
            } else {
                // get foreground before draw overlay.
                Bitmap foreground = mMaskSimulator.getForground(bgBmp);
                // draw over layer.
                if (mOverLayController != null) {
                    mOverLayController.drawBitmap(bgCanvas);
                }
                // calculate the draw rect of foreground.
                float scale = bgBmp.getWidth() / ((float) (mMasterImage.getBitmap().getWidth()));
                Matrix matrix = new Matrix();
                matrix.postScale(scale, scale);
                RectF drawRect = new RectF();
                matrix.mapRect(drawRect, new RectF(mMaskSimulator.getClippingBox()));
                // draw foreground.
                if (foreground != null) {
                    bgCanvas.drawBitmap(foreground, null, drawRect, mPaint);
                    foreground.recycle();
                    foreground = null;
                }
            }
            return bgBmp;
        }

        Log.d(LOGTAG, "<getSynthBitmap> no source bitmap!");
        return mMasterImage.getBitmap();
    }

    public void setMode(int mode) {
        mMode = mode;
    }

    public void setCopySource(Bitmap src) {
        mBitmapCopySource = src;
    }
}
