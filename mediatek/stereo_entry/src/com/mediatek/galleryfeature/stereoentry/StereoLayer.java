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
package com.mediatek.galleryfeature.stereoentry;

import android.app.Activity;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;

import android.view.ViewGroup;
import com.mediatek.gallerybasic.base.Layer;
import com.mediatek.gallerybasic.base.MediaData;
import com.mediatek.gallerybasic.base.Player;
import com.mediatek.gallerybasic.gl.MGLView;
import com.mediatek.gallerybasic.util.Log;


/**
 * Control sliding event on photopage, and control action bar menu.
 */
public class StereoLayer extends Layer {
    private final static String TAG = "MTKGallery2/StereoLayer";
    private Activity mActivity;
    private MediaData mMediaData;
    private boolean mResponseForSlidingOperation;

    @Override
    public void onCreate(Activity activity, final ViewGroup root) {
        Log.d(TAG, "<onCreate> activity");
        mActivity = activity;
    }

    @Override
    public boolean onSingleTapUp(float x, float y) {
        return false;
    }

    @Override
    public View getView() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onChange(Player player, int what, int arg, Object obj) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setData(MediaData data) {
        mMediaData = data;
    }

    @Override
    public void setPlayer(Player player) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mResponseForSlidingOperation) {
            menu.clear();
        }
        return true;
    }
    @Override
    public boolean onDoubleTap(float x, float y) {
        return mResponseForSlidingOperation;
    }

    @Override
    public void onDown(float x, float y) {
    }

    @Override
    public void onUp() {

    }

    @Override
    public boolean onScaleBegin(float focusX, float focusY) {
        return mResponseForSlidingOperation;
    }

    @Override
    public boolean onScale(float focusX, float focusY, float scale) {
        return mResponseForSlidingOperation;
    }

    @Override
    public boolean onScroll(float dx, float dy, float totalX, float totalY) {
        return mResponseForSlidingOperation;
    }

    @Override
    public void onResume(boolean isFilmMode) {
    }

    @Override
    public MGLView getMGLView() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean onBackPressed() {
        if (mResponseForSlidingOperation) {
            mActivity.invalidateOptionsMenu();
        }
        return mResponseForSlidingOperation;
    }

    @Override
    public boolean onUpPressed() {
        return onBackPressed();
    }


    @Override
    public void onActivityPause() {
        super.onActivityPause();
    }

    @Override
    public void onPause() {
        //fresh(false);
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public boolean fresh(boolean onActionPresentationMode) {
        mResponseForSlidingOperation = onActionPresentationMode;
        return false;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return mResponseForSlidingOperation;
    }
}
