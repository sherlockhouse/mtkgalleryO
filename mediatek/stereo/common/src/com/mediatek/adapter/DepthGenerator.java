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
 * MediaTek Inc. (C) 2016. All rights reserved.
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
import android.graphics.Rect;
import android.net.Uri;
import android.os.Environment;

import com.mediatek.accessor.StereoInfoAccessor;
import com.mediatek.accessor.data.StereoBufferInfo;
import com.mediatek.accessor.data.StereoConfigInfo;
import com.mediatek.accessor.data.StereoDepthInfo;
import com.mediatek.stereoapplication.ImageBuf;
import com.mediatek.stereoapplication.StereoApplication;
import com.mediatek.stereoapplication.depthgenerator.ActionType;
import com.mediatek.stereoapplication.depthgenerator.DepthInfo;
import com.mediatek.stereoapplication.depthgenerator.InitConfig;
import com.mediatek.util.Log;
import com.mediatek.util.StereoUtils;
import com.mediatek.util.TraceHelper;
import com.mediatek.util.readwritelock.ReadWriteLockProxy;

import java.io.File;

/**
 * Used to generate depth buffer.
 */
public class DepthGenerator {
    private static final String TAG = Log.Tag("Rf/DepthGeneratorAp");
    // same define with "JniDepthGenerator.h"
    private static final String DEPTH_GENERATOR_NAME = "depthgenerator";
    private static boolean REGEN_DEPTH = false;
    private static final String REGEN_DEPTH_CFG = Environment
            .getExternalStorageDirectory().toString()
            + "/regen";

    private StereoApplication mStereoJni;
    private StereoInfoAccessor mAccessor;
    private StereoDepthInfo mStereoDepthInfo;

    static {
        File cfg = new File(REGEN_DEPTH_CFG);
        if (cfg.exists()) {
            REGEN_DEPTH = true;
        }
    }

    /**
     * Create instance.
     */
    public DepthGenerator() {
        mStereoJni = new StereoApplication();
        mAccessor = new StereoInfoAccessor();
    }

    /**
     * Generate depth buffer.
     * @param context context
     * @param sourceUri sourceUri
     * @param filePath file path
     * @return stereo depth info
     */
    public StereoDepthInfo generateDepth(Context context, Uri sourceUri, String filePath) {
        Log.d(TAG, "<generateDepth> begin, sourceUri:" + sourceUri + ",filePath:" + filePath);
        if (sourceUri == null || context == null || filePath == null) {
            Log.d(TAG, "<generateDepth><error> sourceUri: " + sourceUri + ", filePath: " + filePath
                    + ", context: " + context);
            return null;
        }
        String generator = filePath + "generator";
        ReadWriteLockProxy.startService(context);
        ReadWriteLockProxy.writeLock(generator);
        try {
            if (getDepthInfoFromFile(filePath)) {
                Log.d(TAG, "<generateDepth> depthBuffer exists, return!!");
                return mStereoDepthInfo;
            }
            Log.d(TAG, "<generateDepth> bein generate depth");
            if (!init(filePath)) {
                Log.d(TAG, "<generateDepth> init fail!!");
                return null;
            }
            DepthInfo depthInfo = generate();
            if (depthInfo == null) {
                Log.d(TAG, "<generateDepth> generate fail!!");
                return null;
            }

            ReadWriteLockProxy.writeLock(filePath);
            saveDepthBufToJpg(filePath, depthInfo);
            StereoUtils.updateContent(context, sourceUri, new File(filePath));
            ReadWriteLockProxy.writeUnlock(filePath);
        } finally {
            ReadWriteLockProxy.writeUnlock(generator);
        }

        Log.d(TAG, "<generateDepth> end");
        return mStereoDepthInfo;
    }

    private boolean init(String filePath) {
        Log.d(TAG, "<init> begin");

        TraceHelper.beginSection(">>>>DepthGenerator-init");

        TraceHelper.beginSection(">>>>DepthGenerator-readStereoBufferInfo");
        StereoBufferInfo bufferInfo = mAccessor.readStereoBufferInfo(filePath);
        TraceHelper.endSection();

        TraceHelper.beginSection(">>>>DepthGenerator-readStereoConfigInfo");
        StereoConfigInfo configInfo = mAccessor.readStereoConfigInfo(filePath);
        TraceHelper.endSection();

        TraceHelper.beginSection(">>>>DepthGenerator-initGenerator");
        boolean initResult = initGenerator(filePath, bufferInfo.jpsBuffer, bufferInfo.maskBuffer,
                configInfo);
        TraceHelper.endSection();

        Log.d(TAG, "<init> initGenerator result: " + initResult);
        TraceHelper.endSection();
        return initResult;
    }

    private boolean initGenerator(String filePath, byte[] jpsData, byte[] maskData,
                                  StereoConfigInfo stereoConfig) {
        Log.d(TAG, "<initGenerator> begin");
        if (jpsData == null || maskData == null || stereoConfig == null) {
            Log.d(TAG, "<initGenerator> params error, jpsData: " + jpsData
                    + ", maskData: " + maskData + ", stereoConfig: " + stereoConfig);
            return false;
        }
        Log.d(TAG, "<initGenerator> jpsBufferSize:" + jpsData.length
                + ", maskBufferSize: " + maskData.length);

        InitConfig generatorConfig = new InitConfig();

        ImageBuf jps = new ImageBuf();
        jps.buffer = jpsData;
        jps.bufferSize = jpsData.length;
        jps.width = stereoConfig.jpsWidth;
        jps.height = stereoConfig.jpsHeight;

        ImageBuf mask = new ImageBuf();
        mask.buffer = maskData;
        mask.bufferSize = maskData.length;
        mask.width = stereoConfig.maskWidth;
        mask.height = stereoConfig.maskHeight;

        ImageBuf ldc = null;
        // if ldcBuffer is null, skip to set ldcBuffer
        if (stereoConfig.ldcBuffer != null) {
            ldc = new ImageBuf();
            ldc.buffer = stereoConfig.ldcBuffer;
            ldc.bufferSize = stereoConfig.ldcBuffer.length;
            ldc.width = stereoConfig.ldcWidth;
            ldc.height = stereoConfig.ldcHeight;
        }
        // if clearImage in xmp is null, set current photo as clear image
        if (stereoConfig.clearImage != null) {
            generatorConfig.imageBuf = stereoConfig.clearImage;
            generatorConfig.imageBufSize = stereoConfig.clearImage.length;
        } else {
            byte[] clearImage = StereoUtils.readFileToBuffer(filePath);
            if (clearImage == null) {
                Log.d(TAG, "<initGenerator> read clearImage fail!!");
                return false;
            }
            generatorConfig.imageBuf = clearImage;
            generatorConfig.imageBufSize = clearImage.length;
        }
        Log.d(TAG, "<initGenerator> ldcBuffer: " + stereoConfig.ldcBuffer
                + ", clearImage: " + generatorConfig.imageBuf);

        generatorConfig.jpsBuf = jps;
        generatorConfig.maskBuf = mask;
        generatorConfig.ldcBuf = ldc;
        generatorConfig.posX = stereoConfig.posX;
        generatorConfig.posY = stereoConfig.posY;
        generatorConfig.viewWidth = stereoConfig.viewWidth;
        generatorConfig.viewHeight = stereoConfig.viewHeight;
        generatorConfig.mainCampos = stereoConfig.mainCamPos;
        generatorConfig.imageOrientation = stereoConfig.imageOrientation;
        generatorConfig.depthOrientation = stereoConfig.depthOrientation;

        int[] faceRip = null;
        Rect[] faceRect = null;
        StereoConfigInfo.FaceDetectionInfo fdInfo = null;
        int faceNum = Math.max(0, stereoConfig.faceCount);
        if (faceNum >= 1 && stereoConfig.fdInfoArray != null
                && stereoConfig.fdInfoArray.size() >= 1) {
            faceRip = new int[faceNum];
            faceRect = new Rect[faceNum];
            for (int i = 0; i < faceNum; i++) {
                fdInfo = stereoConfig.fdInfoArray.get(i);
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
        generatorConfig.minDacData = stereoConfig.minDac;
        generatorConfig.maxDacData = stereoConfig.maxDac;
        generatorConfig.curDacData = stereoConfig.curDac;
        generatorConfig.isFd = stereoConfig.isFace;
        generatorConfig.ratio = stereoConfig.faceRatio;
        generatorConfig.faceNum = faceNum;
        generatorConfig.faceRect = faceRect;
        generatorConfig.faceRip = faceRip;

        boolean result = mStereoJni.initialize(DEPTH_GENERATOR_NAME, generatorConfig);
        if (!result) {
            mStereoJni.release();
        }
        Log.d(TAG, "<initGenerator> end, result: " + result);
        return result;
    }

    private DepthInfo generate() {
        Log.d(TAG, "<generate> begin");
        DepthInfo depthInfo = new DepthInfo();
        // avoid proguard, lead to can't find xmpDepth
        depthInfo.xmpDepth = null;

        boolean result = mStereoJni.process(ActionType.ACTION_GENERATE_DEPTH, null, depthInfo);
        Log.d(TAG, "<generate> result: " + result + ", depth buffer size "
                + depthInfo.depthBuf.bufferSize + ", depthWidth "
                + depthInfo.depthBuf.depthWidth + ", depthHeight "
                + depthInfo.depthBuf.depthHeight + ", metaWidth "
                + depthInfo.depthBuf.metaWidth + ", metaHeight "
                + depthInfo.depthBuf.metaHeight);
        mStereoJni.release();

        Log.d(TAG, "<generate> end, result: " + result);
        return result ? depthInfo : null;
    }

    private boolean saveDepthBufToJpg(String filePath, DepthInfo depthInfo) {
        Log.d(TAG, "<saveDepthBufToJpg> begin");
        mStereoDepthInfo = new StereoDepthInfo();

        mStereoDepthInfo.depthBuffer = depthInfo.depthBuf.buffer;
        // mStereoDepthInfo.depthMap = depthInfo.xmpDepth.buffer;
        mStereoDepthInfo.depthBufferHeight = depthInfo.depthBuf.depthHeight;
        mStereoDepthInfo.depthBufferWidth = depthInfo.depthBuf.depthWidth;
        // mStereoDepthInfo.depthMapHeight = depthInfo.xmpDepth.height;
        // mStereoDepthInfo.depthMapWidth = depthInfo.xmpDepth.width;
        mStereoDepthInfo.metaBufferHeight = depthInfo.depthBuf.metaHeight;
        mStereoDepthInfo.metaBufferWidth = depthInfo.depthBuf.metaWidth;

        TraceHelper.beginSection(">>>>DepthGenerator-writeDepthBufferToJpg");
        mAccessor.writeStereoDepthInfo(filePath, mStereoDepthInfo);
        TraceHelper.endSection();

        Log.d(TAG, "<saveDepthBufToJpg> end");
        return true;
    }

    private boolean getDepthInfoFromFile(String filePath) {
        if (REGEN_DEPTH) {
            Log.d(TAG, "<getDepthInfoFromFile> REGEN_DEPTH, return false");
            return false;
        }

        mStereoDepthInfo = mAccessor.readStereoDepthInfo(filePath);
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
}
