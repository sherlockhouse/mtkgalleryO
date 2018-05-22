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

#ifndef IMAGESEGMENT_H_
#define IMAGESEGMENT_H_

#include "Image.h"
#include "Log.h"
#include "MTKImageSegment.h"
#include "StereoStruct.h"
#include "../DebugHelper.h"
#include "StereoUtils.h"

#include <stdio.h>
#include <assert.h>
#include <string.h>
#include <cutils/trace.h>

namespace stereo {

#define TAG "ImageSegment"

#define ALIGNMENT_BYTE 4
#define ROI_MARGIN 20

#define ALIGN128(x)  ((x + 127)&(~(128-1)))
#define ALIGN16(x)  ((x + 15)&(~(16-1)))
#define ALIGN4(x)  ((x+3)&(~3))

#define min(x, y) (x > y ? y : x)
#define max(x, y) (x > y ? x : y)

#define SCENARIO_AUTO 0
#define SCENARIO_SELECTION 1
#define SCENARIO_SCRIBBLE_FG 2
#define SCENARIO_SCRIBBLE_BG 3

#define MODE_OBJECT 0
#define MODE_FORGROUND 1

#define ALPHA 0xFF
#define COVER 0xBB
#define SCRIBBLE_BACKGROUND 0x80
#define ATRACE_TAG ATRACE_TAG_ALWAYS

#define SEGMENT_DUMP_PATH "/sdcard/segment/"
#define FILE_NAME_LENGTH 100

typedef struct InitConfig {
    ImageBuf bitmap;
    DepthBuf depth;
    MaskBuf mask;

    MUINT32 scribbleWidth;
    MUINT32 scribbleHeight;
    MUINT32 imageOrientation;

    MUINT32 faceNum;
    Rect* faceRect;
    MINT32* faceRip;
} InitConfig;

typedef struct DoSegmentConfig {
    MUINT32 mode;
    MUINT32 scenario;

    MUINT8* scribbleBuf;
    Rect roiRect;
    Point selectPoint;
};

typedef struct BitmapMaskConfig {
    ImageBuf bitmap;

    MaskBuf mask;
} cutoutForgroundImg;

class ImageSegment {
public:
    ImageSegment();
    virtual ~ImageSegment();

    bool initialize(InitConfig* pConfig);

    bool doSegment(DoSegmentConfig* pConfig, MaskBuf* pMaskBuf);

    bool undoSegment(MaskBuf* pMaskBuf);

    bool redoSegment(MaskBuf* pMaskBuf);

    bool cutoutForgroundImg(BitmapMaskConfig* pConfig, MUINT8* forgroundImg);

    bool scaleMask(BitmapMaskConfig* pConfig, MaskBuf* pMaskBuf);

    bool scaleMaskWithoutInit(BitmapMaskConfig* pConfig, MaskBuf* pMaskBuf);

    bool fillCoverImg(MUINT8* coverImg);

private:
    Rect getRoiRect(DoSegmentConfig* pConfig);

    void getSegmentMask(MaskBuf *ptoMaskBuf, SEGMENT_CORE_RESULT_STRUCT *pfromResult, MUINT32 bufferSize);

    void getSegmentMask(MaskBuf *ptoMaskBuf);
    void initializeImageInfo(ImageBuf &image);

    void initializeDepthInfo(DepthBuf &depth);

    void initializeMaskInfo(MaskBuf &mask);

    void initializeFaceInfo(InitConfig* pConfig);

    const static MUINT32 MAX_UNDO_NUM = 5;
    SEGMENT_CORE_SET_ENV_INFO_STRUCT m_envInfo;
    SEGMENT_CORE_SET_PROC_INFO_STRUCT m_procInfo;
    SEGMENT_CORE_SET_WORK_BUF_INFO_STRUCT m_workBufferInfo;

    MTKImageSegment* m_pImageSegment = NULL;
    MUINT8* m_pImageBuf = NULL;
    MUINT8* m_pOccBuf = NULL;
    MUINT8* m_pDepthBuf = NULL;

    Image<MUINT8> m_userScribbles;
    Image<MUINT8> m_alphaMask[MAX_UNDO_NUM];
    Point m_objPoint[MAX_UNDO_NUM];
    Rect m_objRect[MAX_UNDO_NUM];

    MUINT32 m_editCurrIndex = 0;
    MUINT32 m_imageWidth = 0;
    MUINT32 m_imageHeight = 0;

    bool m_isUndo = false;
    // add for debug
    DebugHelper *m_pDebugHelper = NULL;
    StereoUtils m_stereoUtils;
};

}

#endif /* IMAGESEGMENT_H_ */
