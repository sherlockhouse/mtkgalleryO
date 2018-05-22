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

#ifndef _FREEVIEW_H
#define _FREEVIEW_H

#include <string.h>
#include <utils/Log.h>

#include "StereoStruct.h"
#include "MTKFV3D.h"
#include "DebugHelper.h"
#include "StereoUtils.h"

namespace stereo {

#define LOG_TAG "FreeView"

#define FREEVIEW_DEBUG_FOLDER "/sdcard/freeview/"

typedef struct FreeViewInitConfig {
    MUINT8 *bitmap;
    DepthBuf depth;

    MUINT32 inputWidth;
    MUINT32 inputHeight;
    MUINT32 outputWidth;
    MUINT32 outputHeight;

    MUINT32 imageOrientation;
} FreeViewInitConfig;

typedef struct ShiftPerspectiveConfig {
    MUINT32 x;
    MUINT32 y;

    MUINT32 outputTextureId;
} ShiftPerspectiveConfig;

class FreeView {
public:
    FreeView();
    virtual ~FreeView();
    bool initialize(FreeViewInitConfig *pConfig);
    bool shiftPerspective(ShiftPerspectiveConfig* pConfig);

private:
    MTKFV3D *m_pNativeFreeView = NULL;
    MUINT8* m_pWorkingBuffer = NULL;
    MUINT8* m_pImgBuf = NULL;
    MUINT8* m_pDepthBuf = NULL;
    DebugHelper *m_pDebugHelper = NULL;
    StereoUtils *m_pStereoUtils = NULL;

    FV3DProcInfo m_procInfo;
    MUINT32 m_outputWidth;
    MUINT32 m_outputHeight;

    bool RGBAToRGB(MUINT8 *rgba, MUINT8 *rgb, MUINT32 width, MUINT32 height);
    bool setInputImageInfo(MUINT8 *imageData, MUINT8 *depthData);
    void dumpBuffer(MUINT32 coordX, MUINT32 coordY, MUINT32 textureId);
};

}
#endif
