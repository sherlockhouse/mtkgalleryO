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
package com.mediatek.stereoapplication.imagerefocus;

import android.graphics.Rect;

import com.mediatek.stereoapplication.DepthBuf;
import com.mediatek.stereoapplication.ImageBuf;

/**
 * This configuration information used to initialize image refocus.<p>
 * Use function:<br>
 * {@link com.mediatek.stereoapplication.StereoApplication#initialize(String, Object)}<br>
 * featureName  : imageRefocus<br>
 * config   : {@link InitConfig}<br>
 */
public class InitConfig {
    /**
     * Clear image buffer, current only support JPEG format.
     */
    public byte[] imageBuf;

    /**
     * Clear image buffer size.
     */
    public int imageBufSize;

    /**
     * Depth information.
     */
    public DepthBuf depthBuf;

    /**
     * JPS width.
     */
    public int jpsWidth;

    /**
     * JPS height.
     */
    public int jpsHeight;

    /**
     * Mask width.
     */
    public int maskWidth;

    /**
     * Mask height.
     */
    public int maskHeight;

    /**
     * X offset between main camera image and sub camera image in JPS.
     */
    public int posX;

    /**
     * Y offset between main camera image and sub camera image in JPS.
     */
    public int posY;

    /**
     * Main camera image width in JPS.
     */
    public int viewWidth;

    /**
     * Main camera image height in JPS.
     */
    public int viewHeight;

    /**
     * Main camera position, left->0, right->1.
     */
    public int mainCamPos;

    /**
     * Focus x coordinate.
     */
    public int focusCoordX;

    /**
     * Focus y coordinate.
     */
    public int focusCoordY;

    /**
     * Image orientation.
     */
    public int imageOrientation;

    /**
     * Depth orientation.
     */
    public int depthOrientation;

    /**
     * Depth of field.
     */
    public int dof;

    /**
     * Lens distortion correction buffer.
     */
    public ImageBuf ldcBuf;

    /**
     * minDacData.
     */
    public int minDacData;

    /**
     * maxDacData.
     */
    public int maxDacData;

    /**
     * curDacData.
     */
    public int curDacData;

    /**
     * faceNum.
     */
    public int faceNum;

    /**
     * faceRect.
     */
    public Rect[] faceRect;

    /**
     * faceRip.
     */
    public int[] faceRip;

    /**
     * isFd.
     */
    public boolean isFd;

    /**
     * ratio.
     */
    public float ratio;

    /**
     * convOffset.
     */
    public float convOffset;

    /**
     * focusType.
     */
    public int focusType;

    /**
     * focusLeft.
     */
    public int focusLeft;

    /**
     * focusTop.
     */
    public int focusTop;

    /**
     * focusRight.
     */
    public int focusRight;

    /**
     * focusBottom.
     */
    public int focusBottom;
}
