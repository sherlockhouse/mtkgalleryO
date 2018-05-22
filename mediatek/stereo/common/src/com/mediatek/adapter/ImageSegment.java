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
package com.mediatek.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Environment;

import com.mediatek.accessor.StereoInfoAccessor;
import com.mediatek.accessor.data.SegmentMaskInfo;
import com.mediatek.accessor.data.StereoConfigInfo;
import com.mediatek.accessor.data.StereoDepthInfo;
import com.mediatek.stereoapplication.DepthBuf;
import com.mediatek.stereoapplication.MaskBuf;
import com.mediatek.stereoapplication.StereoApplication;
import com.mediatek.stereoapplication.imagesegment.ActionType;
import com.mediatek.stereoapplication.imagesegment.BitmapMaskConfig;
import com.mediatek.stereoapplication.imagesegment.DoSegmentConfig;
import com.mediatek.stereoapplication.imagesegment.InitConfig;
import com.mediatek.util.Log;
import com.mediatek.util.StereoUtils;

import junit.framework.Assert;

import java.io.File;
import java.nio.ByteBuffer;

/**
 * use to generate segment object.
 *
 */
public class ImageSegment {
    private static final String TAG = Log.Tag("Segment/ImageSegmentAp");
    // scenario
    public static final int SCENARIO_AUTO = 0;
    public static final int SCENARIO_SELECTION = 1;
    public static final int SCENARIO_SCRIBBLE_FG = 2;
    public static final int SCENARIO_SCRIBBLE_BG = 3;
    // mode
    public static final int MODE_OBJECT = 0;
    public static final int MODE_FOREGROUND = 1;

    private static final Bitmap.Config mConfig = Bitmap.Config.ARGB_8888;

    private static final int RECT_LEN = 2000;
    private static final int HALF_RECT_LEN = RECT_LEN / 2;

    private static final int MASK_COLOR = 0xBBFF6666;
    private static final int MASK_ALPHA = 0xFF000000;
    private static final int MASK_WHITE = 0x00FFFFFF;
    private static final int BYTE_PER_PIXEL = 4;
    private static final int ROI_MARGIN = 20;
    private static final int TEMP_NUM3 = 3;
    // if other component set mask buffer to segment
    // segment use this mask and set to algorithm
    // TODO can other app directly call StereoApplication?
    private static SegmentMaskInfo sMaskInfo = null;
    private final StereoInfoAccessor mXmpOperator;
    // sit
    private final StereoApplication mStereoApp;
    private InitConfig mInitConfig = new InitConfig(); // DODO delete in this class
    private final MaskBuf mMaskBuf; // DODO delete in this class
    private final MaskBuf mNewMaskBuf; // DODO delete in this class
    private Context mContext;
    private Uri mSourceUri;

    // fit
    // TODO not robust. new a specialized file to handle fancy color case
    private boolean mIsInitialized;

    private ByteBuffer mByteBuffer = null;
    private String mFilePath;
    private boolean mIsReadMaskFromXmp = true;

    private int mImageWidth = 0;
    private int mImageHeight = 0;
    private int mNewImageWidth = 0;
    private int mNewImageHeight = 0;
    private int mScribbleWidth = 0;
    private int mScribbleHeight = 0;

    /**
     * constructor, if exist file "NOT_READ_MASK"->regenerate mask. initialize
     * XmpOperator
     * @param context context
     */
    public ImageSegment(Context context) {
        mContext = context;
        File file = new File(Environment.getExternalStorageDirectory(), "NOT_READ_MASK");
        if (file.exists()) {
            mIsReadMaskFromXmp = false;
        }
        mXmpOperator = new StereoInfoAccessor();
        // sit
        mStereoApp = new StereoApplication();
        mInitConfig = new InitConfig();
        mMaskBuf = new MaskBuf();
        mNewMaskBuf = new MaskBuf();
    }

    /**
     * if other component set mask buffer to segment. segment will use this mask
     * replace JPEG header information.
     *
     * @param maskInfo
     *            mask buffer and information
     */
    public static void setMaskBuffer(SegmentMaskInfo maskInfo) {
        sMaskInfo = maskInfo;
    }

    /**
     * initialize segment.
     * @param sourceUri
     *            bitmap uri
     * @param sourceFilePath
     *            bitmap file path
     * @param bitmap
     *            origin bitmap
     * @param orientation
     *            bitmap orientation
     * @return success->true, fail->false
     */
    public boolean initSegment(Uri sourceUri, String sourceFilePath, Bitmap bitmap,
                               int orientation) {

        Log.d(TAG, "[initSegment] bitmap:" + bitmap + ",sourceFilePath:" + sourceFilePath
                + ",imgWidth:" + bitmap.getWidth() + ",imgHeight:" + bitmap.getHeight()
                + ",orientation:" + orientation);
        if (sourceUri == null || sourceFilePath == null || bitmap == null) {
            return false;
        }
        mSourceUri = sourceUri;
        mFilePath = sourceFilePath;
        mImageWidth = bitmap.getWidth();
        mImageHeight = bitmap.getHeight();
        mScribbleWidth = mImageWidth;
        mScribbleHeight = mImageHeight;
        mByteBuffer = ByteBuffer.allocate(mImageWidth * mImageHeight * BYTE_PER_PIXEL);

        StereoDepthInfo depthBufferInfo = mXmpOperator.readStereoDepthInfo(sourceFilePath);

        if (depthBufferInfo == null || depthBufferInfo.depthBuffer == null
                || depthBufferInfo.depthBufferWidth <= 0
                || depthBufferInfo.depthBufferHeight <= 0) {
            DepthGenerator depthGenerator = new DepthGenerator();
            depthBufferInfo = depthGenerator.generateDepth(mContext, mSourceUri, sourceFilePath);
        }
        if (depthBufferInfo == null) {
            return false;
        }
        // if metaBufferWidth & metaBufferHeight is invalid, replace with depth
        if (depthBufferInfo.metaBufferWidth <= 0 || depthBufferInfo.metaBufferHeight <= 0) {
            depthBufferInfo.metaBufferWidth = depthBufferInfo.depthBufferWidth;
            depthBufferInfo.metaBufferHeight = depthBufferInfo.depthBufferHeight;
            Log.i(TAG, "<initSegment> correct metaWidth to " + depthBufferInfo.metaBufferWidth
                    + ", metaHeight to " + depthBufferInfo.metaBufferHeight);
        }
        return initSegmentWithJpsAndDepth(sourceFilePath, bitmap, depthBufferInfo,
                orientation);
    }

    private boolean initSegmentWithJpsAndDepth(String sourceFilePath, Bitmap bitmap,
                                               StereoDepthInfo depthBufferInfo, int orientation) {
        Rect[] faceRect = null;
        int faceNum = 0;
        int[] faceRip = { 0 };
        StereoConfigInfo stereoInfo = mXmpOperator.readStereoConfigInfo(sourceFilePath);
        StereoConfigInfo.FaceDetectionInfo fdInfo = null;
        Log.d(TAG, "<initSegment> faceNum:" + stereoInfo.faceCount);
        if (stereoInfo.faceCount >= 1 && stereoInfo.fdInfoArray != null
                && stereoInfo.fdInfoArray.size() >= 1) {
            faceNum = stereoInfo.faceCount;
            faceRip = new int[faceNum];
            faceRect = new Rect[faceNum];
            for (int i = 0; i < faceNum; i++) {
                fdInfo = stereoInfo.fdInfoArray.get(i);
                if (fdInfo.faceLeft >= -HALF_RECT_LEN && fdInfo.faceLeft <= HALF_RECT_LEN) {
                    faceRect[i] = getFaceRect(mImageWidth, mImageHeight,
                            fdInfo.faceLeft, fdInfo.faceTop, fdInfo.faceRight,
                            fdInfo.faceBottom);
                    faceRip[i] = fdInfo.faceRip;
                    Log.d(TAG, "<initSegment> left:" + faceRect[i].left + ",top:"
                            + faceRect[i].top + ",right:" + faceRect[i].right + ",bottom:"
                            + faceRect[i].bottom +" faceRip:"+ faceRip[i]);
                } else {
                    Assert.assertTrue(false);
                }
            }
        }
        if (depthBufferInfo != null && depthBufferInfo.depthBuffer != null
                && depthBufferInfo.depthBufferWidth > 0 && depthBufferInfo.depthBufferHeight > 0) {
            init(bitmap, mImageWidth, mImageHeight, depthBufferInfo, faceNum, faceRect,
                    faceRip, orientation);

            // sit
            if (sMaskInfo != null) {
                boolean res = initSegmentMask(sMaskInfo);
                sMaskInfo.maskBuffer = null;
                sMaskInfo = null;
                if (res) {
                    Log.d(TAG, "get maskInfo from buffer");
                }
            }
            if (getMaskFromXmp()) {
                Log.d(TAG, "get maskInfo from xmp");
            }
            boolean res = mStereoApp.initialize("imagesegment", mInitConfig);
            // fit
            mIsInitialized = true;

            return res;
        }
        return false;
    }

    /**
     * do segment according to different parameter.
     *
     * @param scenario
     *            SCENARIO_SCRIBBLE_FG SCENARIO_SCRIBBLE_BG SCENARIO_SELECTION
     *            SCENARIO_AUTO
     * @param mode
     *            MODE_OBJECT MODE_FOREGROUND
     * @param scribble
     *            scribble image, foreground->255, background->0
     * @param roiRect
     *            roiRect
     * @param point
     *            point
     * @return success->true, fail->false
     */
    public boolean doSegment(int scenario, int mode, Bitmap scribble, Rect roiRect, Point point) {

        Log.d(TAG, "[doSegment] scenario:" + scenario + ",mode:" + mode + ",roiRect:"
                + roiRect + ",scribble:" + scribble);

        if ((scenario == SCENARIO_SCRIBBLE_FG || SCENARIO_SCRIBBLE_BG == scenario)
                && (scribble == null || roiRect == null)) {
            Log.d(TAG, "null scribble or roiRect!!!!");
            Assert.assertTrue(false);
        }

        if (scenario == SCENARIO_SELECTION && point == null) {
            Log.d(TAG, "null point!!!!");
            Assert.assertTrue(false);
        }

        if (scribble != null && (scribble.getWidth() != mScribbleWidth
                || scribble.getHeight() != mScribbleHeight)) {
            Log.d(TAG, "error scribble size,width:" + scribble.getWidth() + ",height:"
                    + scribble.getHeight());
            Assert.assertTrue(false);
        }

        // sit: seems redundant, but we need this to recover init mask state in some cases
        if (scenario == SCENARIO_AUTO) {
            if (sMaskInfo != null) {
                boolean res = initSegmentMask(sMaskInfo);
                sMaskInfo.maskBuffer = null;
                sMaskInfo = null;
                if (res) {
                    Log.d(TAG, "get maskInfo from buffer");
                    return true;
                }
            }
            if (getMaskFromXmp()) {
                Log.d(TAG, "get maskInfo from xmp");
                return true;
            }
        }

        byte[] scribbleBuf = null;
        if (scribble != null) {
            scribbleBuf = new byte[mScribbleWidth * mScribbleHeight * BYTE_PER_PIXEL];
            mByteBuffer.clear();
            mByteBuffer.rewind();
            scribble.copyPixelsToBuffer(mByteBuffer);
            mByteBuffer.rewind();
            mByteBuffer.get(scribbleBuf);
        }

        // sit begin
        DoSegmentConfig config = new DoSegmentConfig();
        config.mode = mode;
        config.scenario = scenario;
        config.scribbleBuf = scribbleBuf;
        config.scribbleRect = roiRect;
        config.selectPoint = point;
        if (point != null) {
            Log.d(TAG, "point: " + point.x + ", " + point.y);
        }
        return mStereoApp.process(ActionType.ACTION_DO_SEGMENT, config, mMaskBuf);
    }

    /**
     * undo segment, show previous operation image.
     *
     * @return success->true, fail->false
     */
    public boolean undoSegment() {
        // sit
        return mStereoApp.process(ActionType.ACTION_UNDO_SEGMENT, null, mMaskBuf);
    }

    /**
     * get current segment mask.
     *
     * @return mask
     */
    public byte[] getSegmentMask() {
        return mMaskBuf.mask;
    }

    /**
     * get current segment mask point.
     *
     * @return point
     */
    public Point getSegmentPoint() {
        Point point = (Point) getSegmentPoint(false);
        Log.d(TAG, "<getSegmentPoint>,x:" + point.x + ",y:" + point.y);
        return point;
    }

    /**
     * get current segment mask rect.
     *
     * @return mask rect
     */
    public Rect getSegmentRect() {
        Rect rect = (Rect) getSegmentRect(false);
        return rect;
    }

    /**
     * get current cover bitmap, foreground clear show and background blur show.
     *
     * @return cover bitmap
     */
    public Bitmap getCoverBitmap() {
        Bitmap alphaBmp = Bitmap.createBitmap(mImageWidth, mImageHeight, Bitmap.Config.ALPHA_8);
        Canvas canvas = new Canvas(alphaBmp);
        canvas.drawColor(MASK_COLOR & MASK_ALPHA);

        boolean res = fillMaskToImg(alphaBmp);
        if (!res) {
            return null;
        }

        Bitmap resultBmp = Bitmap.createBitmap(mImageWidth, mImageHeight, mConfig);
        Canvas cvs = new Canvas(resultBmp);
        cvs.drawColor(MASK_COLOR & MASK_WHITE);
        cvs.drawBitmap(alphaBmp, new Matrix(), null);
        alphaBmp.recycle();
        cvs.drawColor(MASK_COLOR, PorterDuff.Mode.SRC_IN);

        return resultBmp;
    }

    /**
     * get current segment bitmap.
     *
     * @param bitmap
     *            original bitmap
     * @return segment bitmap
     */
    public Bitmap getSegmentBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            Log.d(TAG, "<getSegmentBitmap> ERROR, null bitmap,fail!!!!");
            return null;
        }
        Rect rect = (Rect) getSegmentRect(false);
        if (rect != null && rect.width() > 0 && rect.height() > 0) {
            Bitmap maskBitmap = Bitmap.createBitmap(rect.width(), rect.height(), mConfig);
            return (Bitmap) getSegmentImg(bitmap, bitmap.getWidth(), bitmap.getHeight(),
                    maskBitmap, maskBitmap.getWidth(), maskBitmap.getHeight(), false);
        } else {
            return null;
        }

    }

    /**
     * set new bitmap to segment, you can get new mask/point/rect from JNI.
     *
     * @param bitmap
     *            new bitmap
     * @param mask
     *            old mask, if you do segment use same instance, set it NULL
     * @param maskWidth
     *            mask width, if mask is null, invalid
     * @param maskHeight
     *            mask height, if mask is null, invalid
     * @return success->true, fail->false
     */
    public boolean setNewBitmap(Bitmap bitmap, byte[] mask, int maskWidth, int maskHeight) {
        mNewImageWidth = bitmap.getWidth();
        mNewImageHeight = bitmap.getHeight();
        if (mask == null) {
            mask = mMaskBuf.mask;
            maskWidth = 1;
            maskHeight = mMaskBuf.bufferSize;
        }
        Log.d(TAG, "<setNewBitmap> bitmap:" + bitmap + ",width:" + mNewImageWidth + ",height:" +
                mNewImageHeight + ",mask:" + mask + ",maskWidth:" + maskWidth + ",maskHeight:"
                + maskHeight);
        return setNewBitmap(bitmap, mNewImageWidth, mNewImageHeight, mask, maskWidth,
                maskHeight);
    }

    /**
     * get new segment mask. use it after setNewBitmap.
     *
     * @return new segment mask
     */
    public byte[] getNewSegmentMask() {
        return getSegmentMask(mNewImageWidth, mNewImageHeight, true);
    }

    /**
     * get new segment rect, use it after setNewBitmap.
     *
     * @return new segmetn rect
     */
    public Rect getNewSegmentRect() {
        return (Rect) getSegmentRect(true);
    }

    /**
     * get new segment point, use it after setNewBitmap.
     *
     * @return new segment point
     */
    public Point getNewSegmentPoint() {
        return (Point) getSegmentPoint(true);
    }

    /**
     * get new segment bitmap, use it after setNewBitmap.
     *
     * @param bitmap
     *            original bitmap
     * @return segment bitmap
     */
    public Bitmap getNewSegmentBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            Log.d(TAG, "<getNewSegmentBitmap> ERROR: null bitmap,fail!!!");
            return null;
        }
        Rect rect = (Rect) getSegmentRect(true);
        if (rect != null && rect.width() > 0 && rect.height() > 0) {
            Bitmap newBitmap = Bitmap.createBitmap(rect.width(), rect.height(), mConfig);
            getSegmentImg(bitmap, bitmap.getWidth(), bitmap.getHeight(),
                    newBitmap, newBitmap.getWidth(), newBitmap.getHeight(), true);
            return newBitmap;
        } else {
            return null;
        }
    }

    /**
     * save mask info to XMP.
     */
    public void saveMaskToXmp() {
        Log.d(TAG, "<saveMaskToXmp>");

        Point point = (Point) getSegmentPoint(false);
        Rect rect = (Rect) getSegmentRect(false);
        byte[] mask = mMaskBuf.mask;

        SegmentMaskInfo maskInfo = new SegmentMaskInfo();
        maskInfo.maskBuffer = mask;
        maskInfo.maskWidth = mImageWidth;
        maskInfo.maskHeight = mImageHeight;
        maskInfo.segmentX = point.x;
        maskInfo.segmentY = point.y;
        maskInfo.segmentBottom = rect.bottom;
        maskInfo.segmentLeft = rect.left;
        maskInfo.segmentTop = rect.top;
        maskInfo.segmentRight = rect.right;

        mXmpOperator.writeSegmentMaskInfo(mFilePath, maskInfo);
        StereoUtils.updateContent(mContext, mSourceUri, new File(mFilePath));
    }

    /**
     * release memory.
     */
    public void release() {
        mStereoApp.release();
    }

    private Rect getRoiRect(int scenario, Rect roiRect, Point point) {
        int left = 0;
        int top = 0;
        int right = mImageWidth - 1;
        int bottom = mImageHeight - 1;

        if (scenario == SCENARIO_SELECTION) {

            left = point.x - ROI_MARGIN;
            top = point.y - ROI_MARGIN;
            right = point.x + ROI_MARGIN;
            bottom = point.y + ROI_MARGIN;

            if (left < 0) {
                right = right - left;
                left = 0;
            }
            if (right > (mImageWidth - 1)) {
                left = left - (right - (mImageWidth - 1));
                right = mImageWidth - 1;
            }

            if (top < 0) {
                bottom = bottom - top;
                top = 0;
            }
            if (bottom > mImageHeight - 1) {
                top = top - (bottom - (mImageHeight - 1));
                bottom = mImageHeight - 1;
            }
        }

        if (scenario == SCENARIO_SCRIBBLE_FG || scenario == SCENARIO_SCRIBBLE_BG) {
            Assert.assertTrue(roiRect != null);
            left = roiRect.left;
            top = roiRect.top;
            right = roiRect.right;
            bottom = roiRect.bottom;
        }
        Rect rect = new Rect(left, top, right, bottom);
        return rect;
    }

    private boolean getMaskFromXmp() {
        if (!mIsReadMaskFromXmp) {
            Log.d(TAG, "<getMaskFromXmp> isReadMaskFromXmp:false!!");
            return false;
        }
        SegmentMaskInfo maskInfo = mXmpOperator.readSegmentMaskInfo(mFilePath);
        return initSegmentMask(maskInfo);
    }

    private boolean initSegmentMask(SegmentMaskInfo maskInfo) {
        if (maskInfo == null || maskInfo.maskWidth <= 0 ||
                maskInfo.maskHeight <= 0 && maskInfo.maskBuffer == null) {
            Log.d(TAG, "<getMaskFromXmp> can't find mask info");
            return false;
        }

        byte[] mask;
        Rect rect;
        Point point;
        Log.d(TAG, "<getMaskFromXmp>, width:" + maskInfo.maskWidth + ",height:"
                + maskInfo.maskHeight + "imageWidth:" + mImageWidth + ",imageHeight:"
                + mImageHeight);

        if (maskInfo.maskWidth != mImageWidth || maskInfo.maskHeight != mImageHeight) {
            return false;
        } else {
            mask = maskInfo.maskBuffer;
            rect = new Rect(maskInfo.segmentLeft, maskInfo.segmentTop, maskInfo.segmentRight,
                    maskInfo.segmentBottom);
            point = new Point(maskInfo.segmentX, maskInfo.segmentY);
        }
        return initSegmentMask(mask, rect, point);
    }

    /**
     * get face region, converted info image coordinated.
     *
     * @param width
     *            image width
     * @param height
     *            image height
     * @param left
     *            left
     * @param top
     *            top
     * @param right
     *            right
     * @param bottom
     *            bottom
     * @return screen coordinated region
     */
    public static Rect getFaceRect(double width, double height, double left, double top,
                                   double right, double bottom) {
        Log.d(TAG, "<getFaceRect> width:" + width + ",height:" + height + ",orientation:"
                + ",left:" + left + ",top:" + top + ",right:" + right + ",bottom:" + bottom);

        Rect res = new Rect();
        res.left = (int) ((left + HALF_RECT_LEN) * width / RECT_LEN);
        res.top = (int) ((top + HALF_RECT_LEN) * height / RECT_LEN);
        res.right = (int) ((right + HALF_RECT_LEN) * width / RECT_LEN);
        res.bottom = (int) ((bottom + HALF_RECT_LEN) * height / RECT_LEN);
        return res;
    }


    // sit begin
    private boolean init(Bitmap bitmap, int imgWidth, int imgHeight,
                               StereoDepthInfo depthBufferInfo, int faceNum, Rect[] faceRect,
                               int[] faceRip, int orientation) {
        InitConfig config = mInitConfig;
        config.bitmap = bitmap;
        DepthBuf depthBuf = new DepthBuf();
        depthBuf.buffer = depthBufferInfo.depthBuffer;
        depthBuf.bufferSize = depthBufferInfo.depthBuffer.length;
        depthBuf.depthWidth = depthBufferInfo.depthBufferWidth;
        depthBuf.depthHeight = depthBufferInfo.depthBufferHeight;
        depthBuf.metaWidth = depthBufferInfo.metaBufferWidth;
        depthBuf.metaHeight = depthBufferInfo.metaBufferHeight;
        config.depthBuf = depthBuf;
        config.imageOrientation = orientation;
        config.faceNum = faceNum;
        InitConfig.FaceInfo[] faceInfos = new InitConfig.FaceInfo[faceNum];
        for (int i = 0; i < faceNum; i++) {
            InitConfig.FaceInfo faceInfo = new InitConfig.FaceInfo();
            faceInfo.left = faceRect[i].left;
            faceInfo.top = faceRect[i].top;
            faceInfo.right = faceRect[i].right;
            faceInfo.bottom = faceRect[i].bottom;
            faceInfo.faceOrientation = faceRip[i];
            faceInfos[i] = faceInfo;
        }
        config.faceInfos = faceInfos;
        return true;
    }

    private boolean initSegmentMask(byte[] mask, Rect rect, Point point) {
        MaskBuf maskBuf = new MaskBuf();
        maskBuf.mask = mask;
        maskBuf.bufferSize = mask.length;
        maskBuf.point = point;
        maskBuf.rect = rect;
        mInitConfig.maskBuf = maskBuf;

        mMaskBuf.mask = maskBuf.mask;
        mMaskBuf.bufferSize = maskBuf.bufferSize;
        mMaskBuf.point = maskBuf.point;
        mMaskBuf.rect = maskBuf.rect;
        return true;
    }

    private byte[] getSegmentMask(int widht, int height, boolean isNew) {
        return (isNew ? mNewMaskBuf.mask : mMaskBuf.mask);
    }

    private Object getSegmentPoint(boolean isNew) {
        return (isNew ? mNewMaskBuf.point : mMaskBuf.point);
    }

    private Object getSegmentRect(boolean isNew) {
        return (isNew ? mNewMaskBuf.rect : mMaskBuf.rect);
    }

    private Object getSegmentImg(Object oriImg, int oriWidth, int oriHeight,
                                       Object newImg, int newWidth, int newHeight, boolean isNew) {
        BitmapMaskConfig newBitmapNewMaskConfig = new BitmapMaskConfig();
        newBitmapNewMaskConfig.bitmap = (Bitmap) oriImg;
        newBitmapNewMaskConfig.maskBuf = (isNew ? mNewMaskBuf : mMaskBuf);
        mStereoApp.process(ActionType.ACTION_CUTOUT_FORGROUND_IMG, newBitmapNewMaskConfig, newImg);
        return newImg;
    }

    private boolean fillMaskToImg(Object bitmap) {
        return mStereoApp.process(ActionType.ACTION_FILL_COVER_IMG, null, bitmap);
    }

    private boolean setNewBitmap(Object bitmap, int bitmapWidth, int bitmapHeight,
                                       byte[] mask, int maskWidth, int maskHeight) {
        BitmapMaskConfig newBitmapOldMaskConfig = new BitmapMaskConfig();
        newBitmapOldMaskConfig.bitmap = (Bitmap) bitmap;
        MaskBuf maskBuf = new MaskBuf();
        maskBuf.mask = mask;
        maskBuf.bufferSize = maskWidth * maskHeight;
        newBitmapOldMaskConfig.maskBuf = maskBuf;
        // fit
        if (!mIsInitialized) {
            maskBuf.rect = new Rect(0, 0, maskWidth, maskHeight);
            mStereoApp.initialize("imagesegment", null);
            mIsInitialized = true;
        }
        boolean res =  mStereoApp
                .process(ActionType.ACTION_SCALE_MASK, newBitmapOldMaskConfig, mNewMaskBuf);
        Log.d(TAG, mNewMaskBuf.mask + ", " + mNewMaskBuf.bufferSize + ", "
                + mNewMaskBuf.point + ", " + mNewMaskBuf.rect);
        return res;
    }
}
