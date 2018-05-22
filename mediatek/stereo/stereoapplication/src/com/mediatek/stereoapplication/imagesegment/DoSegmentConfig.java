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

import android.graphics.Point;
import android.graphics.Rect;

/**
 * The information used to do segment and generate mask.
 */
public class DoSegmentConfig {
    /**
     * Mode type, foreground only contain selected object.
     */
    public static final int MODE_OBJECT = 0;

    /**
     * Mode type, foreground contain selected object and all objects on
     * the front of selected object.
     */
    public static final int MODE_FORGROUND = 1;

    /**
     * Scenario type, foreground refer to face information if contain faces, else
     * auto select center object.
     */
    public static final int SCENARIO_ATUO = 0;

    /**
     * Scenario type, foreground refer to select object, {@link DoSegmentConfig#selectPoint} can't
     * be null.
     */
    public static final int SCENARIO_SELECTION = 1;

    /**
     * Scenario type, scribble region will be set as foreground, {@link DoSegmentConfig#scribbleBuf}
     * and {@link DoSegmentConfig#scribbleRect} can't be null.
     */
    public static final int SCENARIO_SCRIBBLE_FG = 2;

    /**
     * Scenario type, scribble region will be set as background, {@link DoSegmentConfig#scribbleBuf}
     * and {@link DoSegmentConfig#scribbleRect} can't be null.
     */
    public static final int SCENARIO_SCRIBBLE_BG = 3;

    /**
     * mode type.
     */
    public int mode;

    /**
     * Scenario type.
     */
    public int scenario;

    /**
     * Scribble buffer, can't be null when scenario type is SCENARIO_SCRIBBLE_FG
     * and SCENARIO_SCRIBBLE_BG.
     */
    public byte[] scribbleBuf;

    /**
     * Scribble region, can't be null when scenario type is SCENARIO_SCRIBBLE_FG
     * and SCENARIO_SCRIBBLE_BG.
     */
    public Rect scribbleRect;

    /**
     * Select point, can't be null when scenario type is SCENARIO_SELECTION.
     */
    public Point selectPoint;
}
