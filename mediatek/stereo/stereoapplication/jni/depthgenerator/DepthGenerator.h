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

#ifndef DEPTHGENERATOR_H_
#define DEPTHGENERATOR_H_
#define REFOCUS_ENABLE_IMGSEG
#include <string.h>

#include "Log.h"
#include "DebugHelper.h"
#include "StereoStruct.h"
#include "StereoType.h"
#include "JpegCodec.h"
#include "MTKRefocus.h"
#include "StereoUtils.h"

namespace stereo
{
#define TAG "Rf/DepthGenerator"

#define DFT_RCFY_ERROR       0
#define DFT_RCFY_ITER_NO    10
#define DFT_THETA            0
#define DFT_DISPARITY_RANGE 10

#define ALIGN128(x)  ((x + 127)&(~(128-1)))
#define ALIGN16(x)  ((x + 15)&(~(16-1)))

#define DECODE_JPG_SAMPLE_SIZE       1
#define DECODE_JPS_SAMPLE_SIZE       1

#define  ORIENTATION_0 0
#define  ORIENTATION_90 90
#define  ORIENTATION_180 180
#define  ORIENTATION_270 270
#define  SENSOR_RECT_LEN 2000
#define  DEPTH_BUFFER_SECTION_SIZE 9
#define  WMI_DEPTH_MAP_INDEX 0
#define  VAR_BUFFER_INDEX 2  // no need to rotate
#define  DVEC_MAP_INDEX 5  // no need to rotate
#define  DS4_BUFFER_Y_INDEX 8

#define FILE_NAME_LENGTH 100
#define DEPTH_GENERATOR_FOLDER "/sdcard/depth/"
#define JPG_DUMP_NAME "jpg.yuv"
#define JPS_DUMP_NAME "jps.yuv"
#define ORI_JPG_NAME "oriJPG.jpg"
#define ORI_JPS_NAME "oriJPS.jpg"

typedef struct InitConfig
{
    MUINT8 *pImageBuf;
    MUINT32 imageBufSize;

    ImageBuf jps;
    ImageBuf mask;
    ImageBuf ldc;

    MUINT32 posX;
    MUINT32 posY;
    MUINT32 viewWidth;
    MUINT32 viewHeight;

    MUINT32 mainCamPos;

    MUINT32 imageOrientation;
    MUINT32 depthOrientation;

    MUINT32 minDacData;
    MUINT32 maxDacData;
    MUINT32 curDacData;
    MUINT32 faceNum;
    RectImgSeg* facePos;
    int* faceRip;
    bool isFd;
    float ratio;
} GeneratorInitConfig;

typedef struct DepthInfo
{
    ImageBuf xmpDepth;
    DepthBuf depth;
} DepthInfo;

class DepthGenerator
{
public:
    DepthGenerator(int32_t ionHandle);
    virtual ~DepthGenerator();

    bool initialize(GeneratorInitConfig *pConfig);

    bool generateDepth(DepthInfo *pDepthInfo);

private:
    RefocusInitInfo m_refocusInitInfo;
    RefocusTuningInfo m_refocusTuningInfo;
    RefocusImageInfo m_refocusImageInfo;
    MUINT32 m_depthOrientation = ORIENTATION_0;

    MUINT8 *m_pWorkingBuffer = NULL;
    MUINT8 *m_pMaskBuffer = NULL;
    MUINT8 *m_pLdcBuffer = NULL;

    MTKRefocus *m_pNativeRefocus = NULL;
    JpegCodec *m_pJpgCodec = NULL;
    DebugHelper *m_pDebugHelper = NULL;
    StereoUtils m_stereoUtils;

    IonInfo *m_pSrcJpgIonBuffer = NULL;  // allocate ion buffer with normal buffer
    IonInfo *m_pDstJpgIonBuffer = NULL;  // decode srcJpg to this ion buffer
    IonInfo *m_pSrcJpsIonBuffer = NULL;  // allocate ion buffer with normal buffer
    IonInfo *m_pDstJpsIonBuffer = NULL;  // decode srcJps to this ion buffer
    int32_t m_ionHandle;

    bool setOriginalJpgInfo(MUINT8 *pJpegBuf, MUINT32 bufSize);
    bool setJpsInfo(MUINT8 *pJpsBuf, MUINT32 bufSize);
    void prepareRefocusTuningInfo();
    bool prepareRefocusImageInfo(GeneratorInitConfig *pConfig);
    void prepareRefocusInitInfo();
    bool initRefocus();
    bool prepareWorkBuffer();
    bool addImage();
    bool generate();
    bool getDepthInfo(DepthInfo *pDepthInfo);
    bool decodeJpg(MUINT8 *pJpegBuf, MUINT32 bufSize, MUINT32 sampleSize, ImageBuf *pOutBuf);
    bool decodeJpg(IonInfo *pJpegBuf, MUINT32 bufSize, MUINT32 sampleSize,
                IonInfo **pOutBuf, int32_t ionHandle);
    void rotateBuffer(MUINT8*  bufferIn, MUINT8*  bufferOut, MINT32 bufferWidth,
            MINT32 bufferHeight, MUINT32 orientation);
    void rotateDepthInfo(RefocusResultInfo *pRefocusResultInfo,
            MUINT32 depthOrientation, MUINT8 * pDepthBuffer);
    void swap(MINT32 *x, MINT32 *y);
    void updateFacePos(MINT32 imageWidth, MINT32 imageHeight, MINT32 faceNum,
        RectImgSeg* facePos);
    void getImageRect(MINT32 width, MINT32 height, RectImgSeg inRect,
            RectImgSeg* outRect);
};

}

#endif /* DEPTHGENERATOR_H_ */
