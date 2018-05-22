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
package com.mediatek.stereoapplication.imagesegment;

/**
 * List image segment's all action types.
 *
 */
public class ActionType {
    /**
     * Use to generate new foreground mask.<p>
     * Use function:<br>
     * {@link com.mediatek.stereoapplication.StereoApplication#process(int, Object, Object)}<br>
     * actionType : ACTION_DO_SEGMENT<br>
     * config : Include mode type, scenario type and so on, see {@link DoSegmentConfig}<br>
     * result : Foreground mask,see {@link com.mediatek.stereoapplication.MaskBuf}<br>
     * return : Return true if generate successfully, else return false<br>
     */
    public static final int ACTION_DO_SEGMENT = 4;
    /**
     * Cancel current operation and get previous foreground mask<p>
     * Use function:<br>
     * {@link com.mediatek.stereoapplication.StereoApplication#process(int, Object, Object)}<br>
     * actionType : ACTION_UNDO_SEGMENT<br>
     * config : null <br>
     * result : previous foreground mask, see {@link com.mediatek.stereoapplication.MaskBuf}<br>
     * return : Return true if successful, else return false.<br>
     */
    public static final int ACTION_UNDO_SEGMENT = 5;
    /**
     * Restore previous operation, get corresponding mask<p>
     * Use function:<br>
     * {@link com.mediatek.stereoapplication.StereoApplication#process(int, Object, Object)}<br>
     *
     * actionType : ACTION_REDO_SEGMENT<br>
     * config : null<br>
     * result : Previous operation mask, see {@link com.mediatek.stereoapplication.MaskBuf}<br>
     * return : Return true if successful, else return false.
     */
    public static final int ACTION_REDO_SEGMENT = 6;
    /**
     * Resize mask to match new bitmap.<p>
     * Use function:<br>
     * {@link com.mediatek.stereoapplication.StereoApplication#process(int, Object, Object)}<br>
     *
     * actionType : ACTION_SCALE_MASK<br>
     * config : Current mask and new bitmap, see {@link BitmapMaskConfig}<br>
     * result : New foreground mask,see {@link com.mediatek.stereoapplication.MaskBuf}<br>
     * return : Return true if resize , else return false<br>
     */
    public static final int ACTION_SCALE_MASK = 7;
    /**
     * Cut out foreground image from original image<p>
     * Use function:<br>
     * {@link com.mediatek.stereoapplication.StereoApplication#process(int, Object, Object)}<br>
     *
     * actionType : ACTION_CUTOUT_FORGROUND_IMG<br>
     * config : Image and corresponding mask, see {@link BitmapMaskConfig}<br>
     * result : Foreground image(bitmap)<br>
     * return : Return true if cut out successfully, else return false.<br>
     */
    public static final int ACTION_CUTOUT_FORGROUND_IMG = 8;
    /**
     * Fill semi-transparent color on background area to produce cover image <p>
     * Use function:<br>
     * {@link com.mediatek.stereoapplication.StereoApplication#process(int, Object, Object)}<br>
     *
     * actionType : ACTION_FILL_COVER_IMG<br>
     * config : null<br>
     * result : cover image bitmap<br>
     * return : Return true if fill cover bitmap successfully, else return false.<br>
     */
    public static final int ACTION_FILL_COVER_IMG = 9;
}
