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

package com.mediatek.refocus;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;

import com.mediatek.accessor.StereoInfoAccessor;
import com.mediatek.accessor.data.StereoConfigInfo;
import com.mediatek.accessor.data.StereoConfigInfo.FocusInfo;
import com.mediatek.accessor.data.StereoDepthInfo;
import com.mediatek.adapter.DepthGenerator;
import com.mediatek.stereoapplication.DepthBuf;
import com.mediatek.stereoapplication.ImageBuf;
import com.mediatek.stereoapplication.StereoApplication;
import com.mediatek.stereoapplication.imagerefocus.ActionType;
import com.mediatek.stereoapplication.imagerefocus.DoRefocusConfig;
import com.mediatek.stereoapplication.imagerefocus.InitConfig;
import com.mediatek.stereoapplication.imagerefocus.RefocusImage;
import com.mediatek.util.Log;
import com.mediatek.util.StereoUtils;

import java.nio.ByteBuffer;

/**
 * communicate with refocus JNI and XMP. get JPS,debug info, mask, depth from XMP, generate new
 * image from algorithm.
 */
public class ImageRefocus {
    private final static String TAG = Log.Tag("Rf/ImageRefocusAp");
    private static final String TIME_STAMP_NAME = "'IMG'_yyyyMMdd_HHmmss";
    private static final String IMAGE_REFOCUS_NAME = "imagerefocus";
    private static final int TEMP_NUM4 = 4;
    private final Bitmap.Config mConfig = Bitmap.Config.ARGB_8888;

    private StereoApplication mStereoJni;
    private StereoInfoAccessor mAccessor;
    private StereoDepthInfo mStereoDepthInfo;
    private StereoConfigInfo mStereoConfigInfo;
    private Bitmap mBitmap;
    private ByteBuffer mBitmapBuf;
    private Context mContext;
    private int mDefaultDofLevel;
    private int mTouchCoordX1st;
    private int mTouchCoordY1st;
    private float mOriImageWidth;
    private float mOriImageHeight;

    /**
     * Create instance.
     * @param context context
     */
    public ImageRefocus(Context context) {
        mStereoJni = new StereoApplication();
        mAccessor = new StereoInfoAccessor();
        mContext = context;
    }

    /**
     * initialize image refocus, if no depth, generate depth, if include. depth->transfer it to JNI
     * @param sourceUri
     *            sourceUri
     * @param filePath
     *            file path
     * @param oriImageWidth
     *            original ImageWidth
     * @param oriImageHeight
     *            original ImageHeight
     * @return success->true,fail->false
     */
    public boolean init(Uri sourceUri, String filePath, float oriImageWidth, float oriImageHeight) {
        Log.d(TAG, "<init> begin");
        boolean result = false;
        if (sourceUri == null || filePath == null || oriImageWidth <= 0 || oriImageHeight <= 0) {
            Log.d(TAG, "<init> params error, sourceUri: " + sourceUri + ", filePath: "
                    + filePath + ", oriImageWidth: " + oriImageWidth + ", oriImageHeight: "
                    + oriImageHeight);
            return false;
        }
        if (!getDepthInfoFromFile(filePath) && !generateDepth(mContext, sourceUri, filePath)) {
            Log.d(TAG, "<init> generateDepth fail!!");
            return false;
        }
        mOriImageWidth = oriImageWidth;
        mOriImageHeight = oriImageHeight;
        result = initImageRefocus(filePath);
        Log.d(TAG, "<init> end, result:" + result);
        return result;
    }

    /**
     * generate new refocus image under the new image coordinates and depth of filed.
     * @param xCoord
     *            x coordinates
     * @param yCoord
     *            y coordinates
     * @param depthofFiled
     *            depth of filed
     * @return new bitmap
     */
    public RefocusImage generateRefocusBitmap(int xCoord, int yCoord, int depthofFiled) {
        Log.d(TAG, "<generateRefocusBitmap> begin");

        TraceHelper.beginSection(">>>>Refocus-generateRefocusImage");
        long startTime = System.currentTimeMillis();
        RefocusImage refocusImage = new RefocusImage();
        ImageBuf image = new ImageBuf();
        DoRefocusConfig config = new DoRefocusConfig();
        config.xCoord = xCoord;
        config.yCoord = yCoord;
        config.depthOfField = depthofFiled;
        config.format = DoRefocusConfig.OUT_FORMAT_RGBA8888;

        TraceHelper.beginSection(">>>>Refocus-generateRefocusImage-StereoJni.ACTION_DO_REFOCUS");
        boolean result = mStereoJni.process(ActionType.ACTION_DO_REFOCUS, config, image);
        TraceHelper.endSection();

        Log.d(TAG, "<generateRefocusBitmap> ACTION_DO_REFOCUS result: " + result
                + ", image width " + image.width + ", height " + image.height + ", " + image);

        Log.d(TAG, "<generateRefocusBitmap><PERF> ACTION_DO_REFOCUS costs: "
                + (System.currentTimeMillis() - startTime));

        int length = image.width * image.height * TEMP_NUM4;
        if (!result || length == 0) {
            Log.d(TAG, "<generateRefocusBitmap> ACTION_DO_REFOCUS, error!!");
            return null;
        }

        TraceHelper.beginSection(">>>>Refocus-generateRefocusBitmap-bmpRewind");
        startTime = System.currentTimeMillis();
        mBitmap = Bitmap.createBitmap(image.width, image.height, mConfig);
        mBitmapBuf = ByteBuffer.allocate(length);
        mBitmapBuf.put(image.buffer);
        mBitmapBuf.rewind();
        mBitmap.copyPixelsFromBuffer(mBitmapBuf);
        TraceHelper.endSection();

        Log.d(TAG, "<generateRefocusBitmap><PERF> bmpRewind costs: "
                + (System.currentTimeMillis() - startTime));

        TraceHelper.endSection();
        Log.d(TAG, "<generateRefocusBitmap> end, result: " + result);
        refocusImage.width = image.width;
        refocusImage.height = image.height;
        refocusImage.image = mBitmap;
        return refocusImage;
    }

    public RefocusImage generateRefocusImage(int xCoord, int yCoord, int depthofFiled) {
        Log.d(TAG, "<generateRefocusImage> begin");

        long startTime = System.currentTimeMillis();
        RefocusImage image = new RefocusImage();
        DoRefocusConfig config = new DoRefocusConfig();
        config.xCoord = xCoord;
        config.yCoord = yCoord;
        config.depthOfField = depthofFiled;
        config.format = DoRefocusConfig.OUT_FORMAT_YUV420;

        TraceHelper.beginSection(">>>>Refocus-generateRefocusImage-ACTION_DO_REFOCUS");
        boolean result = mStereoJni.process(ActionType.ACTION_DO_REFOCUS, config, image);
        TraceHelper.endSection();
        Log.d(TAG, "<generateRefocusImage><PERF> ACTION_DO_REFOCUS costs: "
                + (System.currentTimeMillis() - startTime));

        return image;
    }

    /**
     * save refocus image to specified path.
     * @param filePath
     *            filePath
     * @param touchBitmapCoord
     *            touchBitmapCoord
     * @param dof
     *            dof
     * @param imageWidth
     *            imageWidth
     * @param imageHeight
     *            imageHeight
     * @param replaceBlurImage
     *            if set as true, clear image will be replaced with blur image
     * @return new Uri
     */
    public boolean saveRefocusImage(String filePath, int[] touchBitmapCoord,
                                    float imageWidth, float imageHeight,
                                    boolean replaceBlurImage) {
        if (filePath == null || touchBitmapCoord == null || touchBitmapCoord.length < 2
                || imageWidth <= 0 || imageHeight <= 0) {
            Log.d(TAG, "<saveRefocusImage> params error, filePath: " + filePath
                    + ", touchBitmapCoord: " + touchBitmapCoord + ", imageWidth: "
                    + imageWidth + ", imageHeight: " + imageHeight);
            return false;
        }
        long begin = System.currentTimeMillis();

        // generate refocus image
        InitConfig config = new InitConfig();
        ImageBuf image = new ImageBuf();
        TraceHelper.beginSection(">>>>Refocus-saveRefocusImage-ACTION_ENCODE_REFOCUS_IMAGE");
        boolean result = mStereoJni.process(ActionType.ACTION_ENCODE_REFOCUS_IMAGE, new Integer(1),
                image);
        TraceHelper.endSection();
        if (!result || image == null || image.width <= 0 || image.height <= 0) {
            Log.d(TAG, "<saveRefocusImage> process fail!!");
            return false;
        }

        Log.d(TAG, "<saveRefocusImage><PERF> saveRefocusImage costs "
                + (System.currentTimeMillis() - begin));

        if (replaceBlurImage) {
            begin = System.currentTimeMillis();
            TraceHelper.beginSection(">>>>Refocus-saveRefocusImage-readStereoConfigInfo");
            StereoConfigInfo configInfo = mAccessor.readStereoConfigInfo(filePath);
            TraceHelper.endSection();
            if (configInfo != null && configInfo.clearImage == null) {
                // if no clear image in header, use main image instead
                configInfo.clearImage = StereoUtils.readFileToBuffer(filePath);
                Log.d(TAG,
                        "<saveRefocusImage> no clear image in header, use main image instead");
            }
            Log.d(TAG, "<saveRefocusImage><PERF> readStereoConfigInfo costs "
                    + (System.currentTimeMillis() - begin));
            Log.d(TAG, "<saveRefocusImage> ori touchCoordX1st " + configInfo.touchCoordX1st
                    + ", touchCoordY1st " + configInfo.touchCoordY1st);
            // just update "TOUCH_COORDX_1ST / TOUCH_COORDY_1ST" in xmp
            int[] sensorTouchCoord = StereoUtils.getCoordinateImageToSensor(
                    imageWidth, imageHeight, touchBitmapCoord[0], touchBitmapCoord[1]);
            Log.d(TAG, "<saveRefocusImage> update sensorTouchCoordX " + sensorTouchCoord[0]
                    + ", sensorTouchCoordY " + sensorTouchCoord[1]);
            configInfo.touchCoordX1st = sensorTouchCoord[0];
            configInfo.touchCoordY1st = sensorTouchCoord[1];
            begin = System.currentTimeMillis();
            TraceHelper.beginSection(">>>>Refocus-saveRefocusImage-writeRefocusImage");
            mAccessor.writeRefocusImage(filePath, configInfo, image.buffer);
            TraceHelper.endSection();
            Log.d(TAG, "<saveRefocusImage><PERF> writeRefocusImage costs "
                    + (System.currentTimeMillis() - begin));
        } else {
            begin = System.currentTimeMillis();
            TraceHelper.beginSection(">>>>Refocus-saveRefocusImage-writeBufferToFile");
            StereoUtils.writeBufferToFile(filePath, image.buffer);
            TraceHelper.endSection();
            Log.d(TAG, "<saveRefocusImage><PERF> writeBufferToFile costs "
                    + (System.currentTimeMillis() - begin));
        }
        return true;
    }

    /**
     * release memory.
     */
    public void release() {
        Log.d(TAG, "<release>");
        mStereoJni.release();
    }

    /**
     * get depth from file.
     * @param filePath
     *            file path
     * @return success->true,fail->false
     */
    public boolean getDepthInfoFromFile(String filePath) {
        if (filePath == null) {
            Log.d(TAG, "<getDepthInfoFromFile> params error, filePath: " + filePath);
            return false;
        }
        long begin = System.currentTimeMillis();
        TraceHelper.beginSection(">>>>Refocus-getDepthInfoFromFile-readStereoDepthInfo");
        mStereoDepthInfo = mAccessor.readStereoDepthInfo(filePath);
        TraceHelper.endSection();
        Log.d(TAG, "<getDepthInfoFromFile><PERF> readStereoDepthInfo costs "
                + (System.currentTimeMillis() - begin));
        if (mStereoDepthInfo != null && mStereoDepthInfo.depthBuffer != null) {
            // if metaBufferWidth & metaBufferHeight is invalid, replace with depth
            if (mStereoDepthInfo.metaBufferWidth <= 0
                    || mStereoDepthInfo.metaBufferHeight <= 0) {
                mStereoDepthInfo.metaBufferWidth = mStereoDepthInfo.depthBufferWidth;
                mStereoDepthInfo.metaBufferHeight = mStereoDepthInfo.depthBufferHeight;
                Log.i(TAG, "<getDepthInfoFromFile> correct metaWidth to "
                        + mStereoDepthInfo.metaBufferWidth + ", metaHeight to "
                        + mStereoDepthInfo.metaBufferHeight);
            }
            return true;
        }
        return false;
    }

    /**
     * get depth buffer.
     * @return depth
     */
    public StereoDepthInfo getDepthInfo() {
        return mStereoDepthInfo;
    }

    /**
     * get default depth of field level.
     * @return dof level
     */
    public int getDefaultDofLevel() {
        return mDefaultDofLevel;
    }

    /**
     * get default touch coordinate.
     * @param width
     *            image width
     * @param height
     *            image height
     * @return touch coordinate
     */
    public int[] getDefaultFocusCoord(int width, int height) {
        return StereoUtils.getCoordinateSensorToImage(width, height,
                mTouchCoordX1st, mTouchCoordY1st);
    }

    /**
     * get face region.
     * @param imageWidth
     *            image width
     * @param imageHeight
     *            image height
     * @return face region
     */
    public Rect getDefaultFaceRect(int imageWidth, int imageHeight) {
        if (mStereoConfigInfo == null) {
            Log.i(TAG, "<getDefaultFaceRect> mStereoConfigInfo is null!!");
            return null;
        }
        if (mStereoConfigInfo.faceCount > 0 && mStereoConfigInfo.fdInfoArray != null
                && mStereoConfigInfo.fdInfoArray.size() > 0) {
            StereoConfigInfo.FaceDetectionInfo fdInfo = mStereoConfigInfo.fdInfoArray.get(0);
            return StereoUtils.getFaceRect(imageWidth, imageHeight, fdInfo.faceLeft,
                    fdInfo.faceTop, fdInfo.faceRight, fdInfo.faceBottom);
        }
        return null;
    }

    private boolean initImageRefocus(String filePath) {
        Log.d(TAG, "<initImageRefocus> begin");
        boolean result = false;
        TraceHelper.beginSection(">>>>Refocus-initImageRefocus");

        long begin = System.currentTimeMillis();
        TraceHelper.beginSection(">>>>Refocus-initImageRefocus-readStereoConfigInfo");
        mStereoConfigInfo = mAccessor.readStereoConfigInfo(filePath);
        TraceHelper.endSection();
        Log.d(TAG, "<initImageRefocus><PERF> readStereoConfigInfo costs "
                + (System.currentTimeMillis() - begin));

        if (mStereoDepthInfo == null || mStereoDepthInfo.depthBuffer == null) {
            Log.d(TAG, "<initImageRefocus> params error!!");
            return false;
        }

        ImageBuf ldc = null;
        // if ldcBuffer is null, skip to set ldcBuffer
        if (mStereoConfigInfo.ldcBuffer != null) {
            ldc = new ImageBuf();
            ldc.buffer = mStereoConfigInfo.ldcBuffer;
            ldc.bufferSize = mStereoConfigInfo.ldcBuffer.length;
            ldc.width = mStereoConfigInfo.ldcWidth;
            ldc.height = mStereoConfigInfo.ldcHeight;
        }

        InitConfig config = new InitConfig();
        // if clearImage in xmp is null, set current photo as clear image
        if (mStereoConfigInfo.clearImage != null) {
            config.imageBuf = mStereoConfigInfo.clearImage;
            config.imageBufSize = mStereoConfigInfo.clearImage.length;
        } else {
            byte[] clearImage = StereoUtils.readFileToBuffer(filePath);
            if (clearImage == null) {
                Log.d(TAG, "<initImageRefocus> read clearImage fail!!");
                return false;
            }
            config.imageBuf = clearImage;
            config.imageBufSize = clearImage.length;
        }
        Log.d(TAG, "<initImageRefocus> ldcBuffer: " + mStereoConfigInfo.ldcBuffer
                + ", clearImage: " + config.imageBuf);

        config.depthBuf = new DepthBuf();
        config.depthBuf.buffer = mStereoDepthInfo.depthBuffer;
        config.depthBuf.bufferSize = mStereoDepthInfo.depthBuffer.length;
        config.depthBuf.depthWidth = mStereoDepthInfo.depthBufferWidth;
        config.depthBuf.depthHeight = mStereoDepthInfo.depthBufferHeight;
        config.depthBuf.metaWidth = mStereoDepthInfo.metaBufferWidth;
        config.depthBuf.metaHeight = mStereoDepthInfo.metaBufferHeight;

        config.jpsWidth = mStereoConfigInfo.jpsWidth;
        config.jpsHeight = mStereoConfigInfo.jpsHeight;
        config.maskWidth = mStereoConfigInfo.maskWidth;
        config.maskHeight = mStereoConfigInfo.maskHeight;
        config.posX = mStereoConfigInfo.posX;
        config.posY = mStereoConfigInfo.posY;
        config.viewWidth = mStereoConfigInfo.viewWidth;
        config.viewHeight = mStereoConfigInfo.viewHeight;
        config.mainCamPos = mStereoConfigInfo.mainCamPos;
        config.focusCoordX = mStereoConfigInfo.touchCoordX1st;
        config.focusCoordY = mStereoConfigInfo.touchCoordY1st;
        config.imageOrientation = mStereoConfigInfo.imageOrientation;
        config.depthOrientation = mStereoConfigInfo.depthOrientation;

        config.ldcBuf = ldc;

        int[] faceRip = null;
        Rect[] faceRect = null;
        StereoConfigInfo.FaceDetectionInfo fdInfo = null;
        int faceNum = Math.max(0, mStereoConfigInfo.faceCount);
        if (faceNum >= 1 && mStereoConfigInfo.fdInfoArray != null
                && mStereoConfigInfo.fdInfoArray.size() >= 1) {
            faceRip = new int[faceNum];
            faceRect = new Rect[faceNum];
            for (int i = 0; i < faceNum; i++) {
                fdInfo = mStereoConfigInfo.fdInfoArray.get(i);
                // NOTE: need to do type-convert in jni
                faceRect[i] =
                        new Rect(fdInfo.faceLeft, fdInfo.faceTop, fdInfo.faceRight,
                                fdInfo.faceBottom);
                faceRip[i] = fdInfo.faceRip;
            }
        } else {
            faceNum = 0;
            Log.d(TAG, "<initGenerator> no face info!");
        }
        // focus info
        config.minDacData = mStereoConfigInfo.minDac;
        config.maxDacData = mStereoConfigInfo.maxDac;
        config.curDacData = mStereoConfigInfo.curDac;
        config.isFd = mStereoConfigInfo.isFace;
        config.ratio = mStereoConfigInfo.faceRatio;
        config.faceNum = faceNum;
        config.faceRect = faceRect;
        config.faceRip = faceRip;
        config.convOffset = mStereoConfigInfo.convOffset;
        config.focusType = mStereoConfigInfo.focusInfo != null ?
                mStereoConfigInfo.focusInfo.focusType : -1;
        // remap focus rect
        prepareFocusInfo(mOriImageWidth, mOriImageHeight, mStereoConfigInfo.focusInfo, config);
        // copy values
        mDefaultDofLevel = config.dof;
        mTouchCoordX1st = config.focusCoordX;
        mTouchCoordY1st = config.focusCoordY;

        begin = System.currentTimeMillis();
        TraceHelper.beginSection(">>>>Refocus-initImageRefocus-StereoJni.initialize");
        result = mStereoJni.initialize(IMAGE_REFOCUS_NAME, config);
        TraceHelper.endSection();
        Log.d(TAG, "<initImageRefocus><PERF> mStereoJni.initialize costs "
                + (System.currentTimeMillis() - begin));

        Log.d(TAG, "<initImageRefocus> end, result:" + result);
        TraceHelper.endSection();
        return result;
    }

    private boolean generateDepth(Context context, Uri sourceUri, String filePath) {
        Log.d(TAG, "<generateDepth> begin");

        long begin = System.currentTimeMillis();
        TraceHelper.beginSection(">>>>Refocus-generateDepth");
        DepthGenerator generator = new DepthGenerator();
        mStereoDepthInfo = generator.generateDepth(context, sourceUri, filePath);
        TraceHelper.endSection();
        Log.d(TAG, "<generateDepth><PERF> generateDepth costs "
                + (System.currentTimeMillis() - begin));

        boolean result = mStereoDepthInfo != null ? true : false;
        Log.d(TAG, "<generateDepth> end, result:" + result);
        return result;
    }

    public int getViewWidth() {
        Log.d(TAG, "<getViewWidth>:" + mStereoConfigInfo.viewWidth);
        return mStereoConfigInfo.viewWidth;
    }

    public int getViewHeigth() {
        Log.d(TAG, "<getViewHeigth>:" + mStereoConfigInfo.viewHeight);
        return mStereoConfigInfo.viewHeight;
    }

    private void prepareFocusInfo(float imageWidth, float imageHeight, FocusInfo focusInfo,
                                  InitConfig config) {
        if (imageWidth <= 0 || imageHeight <= 0 || focusInfo == null || config == null) {
            Log.d(TAG, "<prepareFocusInfo> invalid params, imageWidth: " + imageWidth
                    + ", imageHeight: " + imageHeight + ", " + focusInfo + ", config: "
                    + config);
            return;
        }
        int[] point1 =
                StereoUtils.getCoordinateSensorToImage(imageWidth, imageHeight,
                        focusInfo.focusLeft, focusInfo.focusTop);
        int[] point2 =
                StereoUtils.getCoordinateSensorToImage(imageWidth, imageHeight,
                        focusInfo.focusRight, focusInfo.focusBottom);
        config.focusLeft = point1[0];
        config.focusTop = point1[1];
        config.focusRight = point2[0];
        config.focusBottom = point2[1];
        Log.d(TAG, "<prepareFocusInfo> focusLeft: " + config.focusLeft + ", focusTop: "
                + config.focusTop + ", focusRight: " + config.focusRight + ", focusBottom: "
                + config.focusBottom);
    }
}
