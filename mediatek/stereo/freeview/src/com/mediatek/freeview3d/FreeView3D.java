/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.mediatek.freeview3d;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import com.mediatek.accessor.StereoInfoAccessor;
import com.mediatek.accessor.data.StereoConfigInfo;
import com.mediatek.accessor.data.StereoDepthInfo;
import com.mediatek.adapter.DepthGenerator;
import com.mediatek.stereoapplication.DepthBuf;
import com.mediatek.stereoapplication.StereoApplication;
import com.mediatek.stereoapplication.freeview.ActionType;
import com.mediatek.stereoapplication.freeview.InitConfig;
import com.mediatek.stereoapplication.freeview.ShiftPerspectiveConfig;
import com.mediatek.util.Log;
import com.mediatek.util.TraceHelper;

/*
 * Interact with stereo jni level.
 */
public class FreeView3D {
    private final static String TAG = Log.Tag("Fv/FreeView3D");

    private StereoApplication mStereo;
    private InitConfig mFreeViewInitConfig;
    private StereoInfoAccessor mAccessor;
    private StereoDepthInfo mStereoDepthInfo;
    private ShiftPerspectiveConfig mShiftPerspectiveConfig;

    public FreeView3D() {
        mStereo = new StereoApplication();
        mAccessor = new StereoInfoAccessor();
        mShiftPerspectiveConfig = new ShiftPerspectiveConfig();
    }

    public boolean initialize(String filePath, Bitmap bitmap) {
        boolean result = false;
        TraceHelper.beginSection(">>>>FreeView-initConfig");
        result = initConfig(filePath, bitmap);
        TraceHelper.beginSection("<<<<FreeView-initConfig");
        if (result) {
            result = mStereo.initialize("freeview", mFreeViewInitConfig);
        }
        return result;
    }

    public boolean process(int x, int y, int outputId) {
        mShiftPerspectiveConfig.x = x;
        mShiftPerspectiveConfig.y = y;
        mShiftPerspectiveConfig.outTextureId = outputId;
        return mStereo.process(ActionType.ACTION_SHIFT_PERSPECTIVE, mShiftPerspectiveConfig, null);
    }

    /**
     * initialize image refocus, if no depth, generate depth, if include. depth->transfer it to JNI
     * @param sourceUri
     *            sourceUri
     * @param filePath
     *            file path
     * @return success->true,fail->false
     */
    public boolean generateDepthInfo(Context context, Uri sourceUri, String filePath) {
        Log.d(TAG, "<generateDepthInfo> begin");
        if (!getDepthInfoFromFile(filePath) && !generateDepth(context, sourceUri, filePath)) {
            Log.d(TAG, "<generateDepthInfo> generateDepth fail!!");
            return false;
        }
        Log.d(TAG, "<generateDepthInfo> end, result:" + true);
        return true;
    }

    /**
     * release memory.
     */
    public void release() {
        Log.d(TAG, "<release>");
        mStereo.release();
    }

    private boolean initConfig(String filePath, Bitmap bitmap) {
        Log.d(TAG, "<initConfig> filePath & bitmap: " + filePath + " " + bitmap);
        if (filePath == null || bitmap == null) {
            Log.d(TAG, "<initConfig> params error!!");
            return false;
        }
        TraceHelper.beginSection(">>>>FreeView-initConfig-readStereoConfigInfo");
        StereoConfigInfo stereoConfigInfo = mAccessor.readStereoConfigInfo(filePath);
        TraceHelper.endSection();
        if (mStereoDepthInfo == null || mStereoDepthInfo.depthBuffer == null) {
            Log.d(TAG, "<initConfig> params error!!");
            return false;
        }

        DepthBuf depthBuffer = new DepthBuf();
        depthBuffer.buffer = mStereoDepthInfo.depthBuffer;
        depthBuffer.bufferSize = mStereoDepthInfo.depthBuffer.length;
        depthBuffer.depthWidth = mStereoDepthInfo.depthBufferWidth;
        depthBuffer.depthHeight = mStereoDepthInfo.depthBufferHeight;
        depthBuffer.metaWidth = mStereoDepthInfo.metaBufferWidth;
        depthBuffer.metaHeight = mStereoDepthInfo.metaBufferHeight;

        mFreeViewInitConfig = new InitConfig();
        mFreeViewInitConfig.depth = depthBuffer;
        mFreeViewInitConfig.bitmap = bitmap;
        mFreeViewInitConfig.outWidth = bitmap.getWidth();
        mFreeViewInitConfig.outHeight = bitmap.getHeight();
        mFreeViewInitConfig.imageOrientation = stereoConfigInfo.depthOrientation;
        return true;
    }

    private boolean getDepthInfoFromFile(String filePath) {
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

    private boolean generateDepth(Context context, Uri sourceUri, String filePath) {
        Log.d(TAG, "<generateDepth> sourceUri & filePath: " + sourceUri + " " + filePath);
        TraceHelper.beginSection(">>>>Refocus-generateDepth");
        DepthGenerator generator = new DepthGenerator();
        mStereoDepthInfo = generator.generateDepth(context, sourceUri, filePath);
        TraceHelper.endSection();

        boolean result = mStereoDepthInfo != null ? true : false;
        Log.d(TAG, "<generateDepth> end, result:" + result);
        return result;
    }
}
