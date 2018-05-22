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

#ifndef IMAGEREFOCUS_H_
#define IMAGEREFOCUS_H_
#define REFOCUS_ENABLE_IMGSEG
#include <string.h>

#include "Log.h"
#include "StereoStruct.h"
#include "IProcessor.h"
#include "JpegCodec.h"
#include "DebugHelper.h"
#include "MTKRefocus.h"
#include "StereoUtils.h"
#include "ImageRefocusPerf.h"
#include "RefocusConfigInfoWrapper.h"
#include <cutils/trace.h>
#include "RefocusStruct.h"

namespace stereo {

#define TAG "Rf/ImageRefocus"

#define ATRACE_TAG ATRACE_TAG_ALWAYS

#define FEATURE_TYPE_IMAGE_REFOCUS 1

//action type
#define ACTION_DO_REFOCUS 2
#define ACTION_ENCODE_REFOCUS_IMAGE 3

//do refocus format
#define OUT_FORMAT_RGBA8888 0
#define OUT_FORMAT_YUV420   1

//encode image format
#define IMAGE_FORMAT_JPG 0

#define DECODE_JPG_SAMPLE_SIZE       1

#define  ORIENTATION_0 0
#define  ORIENTATION_90 90
#define  ORIENTATION_180 180
#define  ORIENTATION_270 270

#define DFT_RCFY_ERROR       0
#define DFT_RCFY_ITER_NO    10
#define DFT_THETA            0
#define DFT_DISPARITY_RANGE 10
#define  SENSOR_RECT_LEN 2000

#define ALIGN128(x)  ((x + 127)&(~(128-1)))
#define FILE_NAME_LENGTH 100

#define REFOCUS_DEBUG_FOLDER "/sdcard/refocusap/"
#define JPG_DUMP_NAME "jpg.yuv"
#define YUV_DUMP_NAME "savedYUV.yuv"
#define SAVED_JPG_DUMP_NAME "savedJPG.jpg"
#define REFOCUS_IMAGE_DUMP_NAME "refocusimage.raw"
#define ORI_JPG_NAME "oriJPG.jpg"
#define DUMP_DEPTH_NAME "depth.raw"

class ImageRefocus {
public:
    ImageRefocus(int32_t ionHandle);
    virtual ~ImageRefocus();

    bool initialize(RefocusInitConfig *pConfig);

    bool doRefocus(DoRefocusConfig *pConfig);

    bool encodeRefocusImg(IonInfo **pOutImgBuf, MUINT32 format);

    bool getRefocusImageSize(ImageBuf *pRefocusImage);

    bool getRefocusImageBuf(ImageBuf *pRefocusImage, bool isYUV);

private:
    MUINT32 m_dumpIndex = 0;
    RefocusInitInfo m_refocusInitInfo;
    RefocusTuningInfo m_refocusTuningInfo;
    RefocusImageInfo m_refocusImageInfo;
    RefocusResultInfo m_refocusResultInfo;

    MUINT8 *m_pWorkingBuffer = NULL;
    MUINT8 *m_pDepthBuffer = NULL;

    MTKRefocus *m_pNativeRefocus = NULL;
    JpegCodec *m_pJpgCodec = NULL;
    DebugHelper *m_pDebugHelper = NULL;
    StereoUtils m_stereoUtils;
    ImageRefocusPerf *m_pRefocusPerf = NULL;
    RefocusConfigInfoWrapper *m_pRefocusConfigInfoWrapper = NULL;

    IonInfo *m_pSrcJpgIonBuffer = NULL;  // allocate ion buffer with normal buffer
    IonInfo *m_pDstJpgIonBuffer = NULL;  // decode srcJpg to this ion buffer
    int32_t m_ionHandle;

    void prepareRefocusTuningInfo(RefocusInitConfig *pConfig);
    bool prepareRefocusImageInfo(RefocusInitConfig *pConfig);
    void prepareRefocusInitInfo(RefocusInitConfig *pConfig);
    bool initRefocus();
    bool prepareWorkBuffer();
    bool generate();
    bool getRefocusImageForSaving(ImageBuf *pRefocusImage);
    bool decodeJpg(MUINT8 *pJpegBuf, MUINT32 bufSize, MUINT32 sampleSize, ImageBuf *pOutBuf);
    bool decodeJpg(IonInfo *pJpegBuf, MUINT32 bufSize, MUINT32 sampleSize, IonInfo **pOutBuf,
            int32_t ionHandle);
    bool setOriginalJpgInfo(MUINT8 *pJpegBuf, MUINT32 bufSize);
    void swapConfigInfo(MUINT32 depthOrientation);
    void swap(MUINT32 *x, MUINT32 *y);
    void updateFacePos(MINT32 imageWidth, MINT32 imageHeight, MINT32 faceNum,
        RectImgSeg* facePos);
    void getImageRect(MINT32 width, MINT32 height, RectImgSeg inRect,
            RectImgSeg* outRect);
};

}
#endif /* IMAGEREFOCUS_H_ */
