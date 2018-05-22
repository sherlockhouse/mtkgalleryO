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

import android.content.Context;
import android.content.res.Resources;
import com.mediatek.gallerybasic.base.*;
import com.mediatek.gallerybasic.gl.GLIdleExecuter;

/**
 * Define for depth image.
 */
public class StereoMember extends MediaMember {
    private Layer mLayer;
    public static int sType;
    private static final int PRIORITY = 10;
    private GLIdleExecuter mGLExecuter;

    /**
     * Constructor.
     *
     * @param context the context is used for create layer.
     * @param exe     the exe is used for create layer.
     */
    public StereoMember(Context context, GLIdleExecuter exe, Resources res) {
        super(context);
        mGLExecuter = exe;
    }

    @Override
    public boolean isMatching(MediaData md) {
        boolean isMatchStereoRule = false;
        if (StereoField.sSupportStereo && md != null) {
            int camera_refocus = 0;
            if (md.extFileds != null) {
                Object field = md.extFileds.getImageField(StereoBottomControl
                        .TYPE_REFOCUS);
                if (null != field) {
                    camera_refocus = (int) field;
                    isMatchStereoRule = camera_refocus == 1;
                }
            }
            // should not show stereo entry if stereo thumbnail
            if (camera_refocus != 2 && !isMatchStereoRule) {
                isMatchStereoRule = StereoBottomControl.TYPE_JPEG.equalsIgnoreCase(md.mimeType);
            }
        }
        return isMatchStereoRule;
    }

    @Override
    public ExtItem getItem(MediaData md) {
        return new StereoItem(md);
    }

    @Override
    public Player getPlayer(MediaData md, ThumbType type) {
        return null;
    }

    @Override
    public Layer getLayer() {
        if (mLayer == null) {
            mLayer = new StereoLayer();
        }
        return mLayer;
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    protected void onTypeObtained(int type) {
        sType = type;
    }
}