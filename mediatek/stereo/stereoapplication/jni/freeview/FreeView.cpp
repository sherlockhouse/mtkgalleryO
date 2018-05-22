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

#include "FreeView.h"
#include "DepthParser.h"
#include <utils/Trace.h>
#include <cutils/trace.h>

using namespace stereo;

#define ATRACE_TAG ATRACE_TAG_ALWAYS

FreeView::FreeView() {
    m_pStereoUtils = new StereoUtils();
    long startSec;
    long startUsec;
    m_pStereoUtils->startMeasureTime(&startSec, &startUsec);

    memset(&m_procInfo, 0, sizeof(m_procInfo));
    if (access(FREEVIEW_DEBUG_FOLDER, 0) != -1) {
        m_pDebugHelper = new DebugHelper();
    }
    m_pStereoUtils->endMeasureTime("new FreeView()", startSec, startUsec);
}

FreeView::~FreeView() {
    long startSec;
    long startUsec;
    m_pStereoUtils->startMeasureTime(&startSec, &startUsec);
    if (m_pNativeFreeView != NULL) {
        m_pNativeFreeView->FV3DReset();
        m_pNativeFreeView->destroyInstance(m_pNativeFreeView);
        m_pNativeFreeView = NULL;
     }
    delete[] m_pWorkingBuffer;
    delete[] m_pImgBuf;
    delete m_pDebugHelper;
    delete[] m_pDepthBuf;
    m_pStereoUtils->endMeasureTime("~FreeView()", startSec, startUsec);
    delete m_pStereoUtils;
}

bool FreeView::initialize(FreeViewInitConfig *pConfig) {
    ATRACE_NAME(">>>>FvJni-initialize");
    long startSec;
    long startUsec;
    m_pStereoUtils->startMeasureTime(&startSec, &startUsec);

    if (NULL == pConfig) {
        ALOGD("<initialize><error> pConfig is null, do nothing.");
        return false;
    }
    FV3DInitInfo initInfo;
    initInfo.inputWidth = pConfig->inputWidth;
    initInfo.inputHeight = pConfig->inputHeight;
    initInfo.outputWidth = pConfig->outputWidth;
    initInfo.outputHeight = pConfig->outputHeight;
    initInfo.depthWidth = pConfig->depth.depthWidth;
    initInfo.depthHeight = pConfig->depth.depthHeight;
    initInfo.orientation = pConfig->imageOrientation;
    m_outputWidth = initInfo.outputWidth;
    m_outputHeight = initInfo.outputHeight;
    ALOGD("<initialize> initInfo: inputWidth=%d, inputHeight=%d, outputWidth=%d, outputHeight=%d,"
            "depthWidth:%d, depthHeight:%d,imageOrientation:%d",
            initInfo.inputWidth, initInfo.inputHeight, initInfo.outputWidth, initInfo.outputHeight,
            initInfo.depthWidth, initInfo.depthHeight, initInfo.orientation);

    ATRACE_BEGIN(">>>>FvJni-createInstance");
    m_pNativeFreeView = MTKFV3D::createInstance(DRV_FV3D_OBJ_SW);
    ATRACE_END();

    if (NULL == m_pNativeFreeView) {
        ALOGD("<initialize><error> createInstance fail");
        return false;
    }

    ATRACE_BEGIN(">>>>FvJni-FV3DInit");
    if (S_FV3D_OK != m_pNativeFreeView->FV3DInit((void*) &initInfo, NULL)) {
        ALOGD("<initialize><error> FV3DInit fail");
        ATRACE_END();
        return false;
    }
    ATRACE_END();

    MUINT32 bufferSize = 0;
    ATRACE_BEGIN(">>>>FvJni-FV3DFeatureCtrl-FV3D_FEATURE_GET_WORKBUF_SIZE");
    m_pNativeFreeView->FV3DFeatureCtrl(FV3D_FEATURE_GET_WORKBUF_SIZE, NULL,
            (void*) &bufferSize);
    ATRACE_END();
    if (bufferSize <= 0) {
        ALOGD("<initialize><error> GET_WORKBUF_SIZE fail");
        return false;
    }

    m_pWorkingBuffer = new MUINT8[bufferSize];
    ALOGD("<initialize> malloc working buffer at %p,bufferSize:%d", m_pWorkingBuffer,
            bufferSize);
    initInfo.workingBufferAddr = m_pWorkingBuffer;
    initInfo.workingBufferSize = bufferSize;

    ATRACE_BEGIN(">>>>FvJni-FV3DFeatureCtrl-FV3D_FEATURE_SET_WORKBUF_ADDR");
    if (S_FV3D_OK != m_pNativeFreeView->FV3DFeatureCtrl(FV3D_FEATURE_SET_WORKBUF_ADDR,
            (void*) &initInfo, NULL)) {
        ALOGD("<initialize><error> SET_WORKBUF fail");
        ATRACE_END();
        return false;
    }
    ATRACE_END();

    m_pImgBuf = new MUINT8[m_outputWidth * m_outputHeight * 3];
    if (!RGBAToRGB(pConfig->bitmap, m_pImgBuf, m_outputWidth, m_outputHeight)) {
        ALOGD("<initialize><error> RGBAToRGB fail");
        return false;
    }

    DepthParser *pParser = new DepthParser(&pConfig->depth);
    m_pDepthBuf = pParser->getDepthMap();
    delete pParser;
    if (!setInputImageInfo(m_pImgBuf, m_pDepthBuf)) {
        ALOGD("<initialize><error> setInputImageInfo fail");
        return false;
    }
    m_pStereoUtils->endMeasureTime("FreeView.initialize(*)", startSec, startUsec);
    return true;
}

bool FreeView::shiftPerspective(ShiftPerspectiveConfig *pConfig) {
    ATRACE_NAME(">>>>FvJni-shiftPerspective");
    long startSec;
    long startUsec;
    m_pStereoUtils->startMeasureTime(&startSec, &startUsec);
    if (NULL == pConfig) {
        ALOGD("<shiftPerspective><error> pConfig is null, do nothing.");
        return false;
    }
    m_procInfo.x_coord = pConfig->x;
    m_procInfo.y_coord = pConfig->y;
    m_procInfo.outputTexID = pConfig->outputTextureId;

    ATRACE_BEGIN(">>>>FvJni-FV3DFeatureCtrl-FV3D_FEATURE_SET_PROC_INFO");
    if (S_FV3D_OK != m_pNativeFreeView->FV3DFeatureCtrl(FV3D_FEATURE_SET_PROC_INFO,
            (void*) &m_procInfo, NULL)) {
        ALOGD("<shiftPerspective><error> SET_PROC_INFO fail");
        ATRACE_END();
        return false;
    }
    ATRACE_END();

    ATRACE_BEGIN(">>>>FvJni-FV3DMain");
    if (S_FV3D_OK != m_pNativeFreeView->FV3DMain()) {
        ALOGD("<shiftPerspective><error> FV3DMain fail");
        ATRACE_END();
        return false;
    }
    ATRACE_END();

    if (NULL != m_pDebugHelper) {
         dumpBuffer(pConfig->outputTextureId, pConfig->x, pConfig->y);
    }
    m_pStereoUtils->endMeasureTime("FreeView.shiftPerspective(*)", startSec, startUsec);
    return true;
}

bool FreeView::setInputImageInfo(MUINT8 *imageData, MUINT8 *depthData) {
    ATRACE_NAME(">>>>FvJni-setInputImageInfo");
    long startSec;
    long startUsec;
    m_pStereoUtils->startMeasureTime(&startSec, &startUsec);
    FV3DImageInfo imageInfo;

    ALOGD("<setInputImageInfo> imageData: %p,depthData: %p", imageData, depthData);
    if (NULL == imageData || NULL == depthData) {
        return false;
    }
    imageInfo.inputBufAddr = imageData;
    imageInfo.depthBufAddr = depthData;

    if (S_FV3D_OK != m_pNativeFreeView->FV3DFeatureCtrl(FV3D_FEATURE_SET_INPUT_IMG,
            (void*) &imageInfo, NULL)) {
        ALOGD("<setInputImageInfo><error> SET_INPUT_IMG fail");
        return false;
    }

    m_pStereoUtils->endMeasureTime("FreeView.setInputImageInfo()", startSec, startUsec);
    return true;
}

bool FreeView::RGBAToRGB(MUINT8 *rgba, MUINT8 *rgb, MUINT32 width, MUINT32 height) {
    ATRACE_NAME(">>>>FvJni-RGBAToRGB");
    if (rgba == NULL) {
        ALOGD("<RGBAToRGB> rgba is NULL");
        return false;
    }

    long startSec;
    long startUsec;
    m_pStereoUtils->startMeasureTime(&startSec, &startUsec);
    ALOGD("<RGBAToRGB>begin, src: %p, width:%d,height:%d", rgba, width, height);
    for (MUINT32 i = 0; i < width * height; i++) {
        rgb[i * 3 + 0] = rgba[i * 4 + 0];
        rgb[i * 3 + 1] = rgba[i * 4 + 1];
        rgb[i * 3 + 2] = rgba[i * 4 + 2];
    }
    m_pStereoUtils->endMeasureTime("FreeView.RGBAToRGB()", startSec, startUsec);
    return true;
}

void FreeView::dumpBuffer(MUINT32 coordX, MUINT32 coordY, MUINT32 textureId) {
    long startSec;
    long startUsec;
    m_pStereoUtils->startMeasureTime(&startSec, &startUsec);
    MUINT8 *dumpBuffer = new MUINT8[m_outputWidth * m_outputHeight * 4];
    char outputFileName[256];
    sprintf(outputFileName, "%s%s_id%d_x%d_y%d.raw",
        FREEVIEW_DEBUG_FOLDER, "outText", textureId, coordX, coordY);
    // glReadPixels(0, 0, mOutputWidth, mOutputHeight, GL_RGBA, GL_UNSIGNED_BYTE, dumpBuffer);
    m_pDebugHelper->dumpBufferToFile(outputFileName, dumpBuffer,
            (MINT32) m_outputWidth * m_outputHeight * 4);

    delete[] dumpBuffer;
    dumpBuffer = NULL;
    m_pStereoUtils->endMeasureTime("FreeView.dumpBuffer()", startSec, startUsec);
}
