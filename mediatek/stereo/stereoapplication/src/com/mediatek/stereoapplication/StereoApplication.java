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
package com.mediatek.stereoapplication;

import android.util.Log;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Stereo application features API, Currently supported feature include
 * depth generator, image refocus, segmentation, fancy color and free view.
 */
public class StereoApplication {
    private static final String TAG = "stereoapplication";

    private long mHandler = -1;
    private ReadWriteLock mReadWriteLock = new ReentrantReadWriteLock();

    static {
        System.loadLibrary("jni_stereoapplication");
    }

    /**
     * Create feature object according to feature name and initialize this feature.<p>
     * @param featureName
     *            Feature name, create feature object according feature name.
     * @param config
     *            This parameter used to initialize this feature.
     * @return Return true if this feature was created and initialized successfully,
     *         else return false.
     */
    public boolean initialize(String featureName, Object config) {
        mHandler = create(featureName);
        return initialize(mHandler, config);
    }

    /**
     * Process certain task according to action type, and can get processed result from
     * the parameter of result.
     * <p>
     * @param actionType
     *            Action type, uniquely identify certain task.
     * @param config
     *            The parameter used to process this task.
     * @param result
     *            Get process result from this parameter.
     * @return Return true if process successfully, else return false.
     */
    public boolean process(int actionType, Object config, Object result) {
        mReadWriteLock.readLock().lock();
        boolean processResult = false;
        if (-1 != mHandler) {
            processResult = process(mHandler, actionType, config, result);
        } else {
            Log.d(TAG, "<process> invalid handle");
        }
        mReadWriteLock.readLock().unlock();
        return processResult;
    }

    /**
     * Get specified information according to information type.
     * <p>
     * @param infoType
     *            Uniquely identify information type.
     * @param config
     *            The parameter used to get information.
     * @return Specified information
     */
    public Object getInfo(int infoType, Object config) {
        mReadWriteLock.readLock().lock();
        Object object = null;
        if (-1 != mHandler) {
            object = getInfo(mHandler, infoType, config);
        } else {
            Log.d(TAG, "<getInfo> invalid handle");
        }
        mReadWriteLock.readLock().unlock();
        return object;
    }

    /**
     * Release the feature object and release memory.
     */
    public void release() {
        mReadWriteLock.writeLock().lock();
        if (-1 != mHandler) {
            release(mHandler);
        } else {
            Log.d(TAG, "<release> invalid handle");
        }
        mHandler = -1;
        mReadWriteLock.writeLock().unlock();
    }

    private native long create(String featureName);

    private native boolean initialize(long handler, Object config);

    private native boolean process(long handler, int actionType, Object config, Object result);

    private native Object getInfo(long handler, int infoType, Object config);

    private native void release(long handler);
}
