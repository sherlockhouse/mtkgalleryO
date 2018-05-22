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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.mediatek.fancycolor.parallel.ThreadPool;
import com.mediatek.fancycolor.utils.TraceHelper;
import com.mediatek.util.Log;
import com.mediatek.util.StereoImage;

/**
 * Helper class to encapsulate thumbnail loading work into request.
 */
class ThumbnailLoadRequest implements ThreadPool.Job<Void> {
    private final static String TAG = Log.Tag("Fc/ThumbnailLoadRequest");
    private final static int SAMPLESIZE_HIGH_RES_THUMBNAIL = 1;
    private final static int SAMPLESIZE_PREVIEW_THUMBNAIL = 4;

    private String mPath;
    private int mThumbnailType;
    private DataLoadingListener mDataLoadingListener;

    /**
     * Callback interface when data loading done.
     */
    public interface DataLoadingListener {
        public void onLoadingFinish(Bitmap bitmap, int type);
    }

    public ThumbnailLoadRequest(String path, int type, DataLoadingListener listener) {
        mPath = path;
        mThumbnailType = type;
        mDataLoadingListener = listener;
    }

    @Override
    public Void run(ThreadPool.JobContext jc) {
        long decodeStart = System.currentTimeMillis();
        Bitmap bitmap = decodeBitmap(mPath);
        Log.d(TAG, "<run> decode bitmap costTime:" + (System.currentTimeMillis() - decodeStart));

        if (mDataLoadingListener != null) {
            mDataLoadingListener.onLoadingFinish(bitmap, mThumbnailType);
        } else {
            Log.d(TAG, "<run> mDataLoadingListener is null, cannot do callback.");
        }
        return null;
    }

    private Bitmap decodeBitmap(String path) {
        if (path == null || "".equals(path)) {
            Log.d(TAG, "<decodeBitmap> path is null, do nothing.");
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        TraceHelper.beginSection(">>>>FancyColor-decodeBitmap");

        TraceHelper.beginSection(">>>>FancyColor-decodeBitmap-decode border");
        BitmapFactory.decodeFile(path, options);
        TraceHelper.endSection();

        int w = options.outWidth;
        int h = options.outHeight;
        Log.d(TAG, "<decodeBitmap> path & width & height: " + path + " " + w + " " + h);
        int sampleSize = SAMPLESIZE_PREVIEW_THUMBNAIL;
        if (mThumbnailType == EffectManager.TYPE_HIGH_RES_THUMBNAIL) {
            sampleSize = SAMPLESIZE_HIGH_RES_THUMBNAIL;
        } else if (mThumbnailType == EffectManager.TYPE_THUMBNAIL) {
            sampleSize = StereoImage.getThumbnailSampleSize(w, h);
        }

        TraceHelper.beginSection(">>>>FancyColor-decodeBitmap-decodeStereoImage");
        Bitmap bitmap = StereoImage.decodeStereoImage(path, sampleSize);
        Log.d(TAG, "<decodeBitmap> mThumbnailType & sampleSize & bitmap: " +
                mThumbnailType + " " + sampleSize + " " + bitmap);
        TraceHelper.endSection();
        TraceHelper.endSection();
        return bitmap;
    }
}
