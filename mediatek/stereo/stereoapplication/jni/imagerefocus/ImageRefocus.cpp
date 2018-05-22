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

#include "ImageRefocus.h"

using namespace stereo;

ImageRefocus::ImageRefocus(int32_t ionHandle)
{
    long startSec;
    long startUsec;
    m_stereoUtils.startMeasureTime(&startSec, &startUsec);

    memset(&m_refocusInitInfo, 0, sizeof(m_refocusInitInfo));
    memset(&m_refocusTuningInfo, 0, sizeof(m_refocusTuningInfo));
    memset(&m_refocusImageInfo, 0, sizeof(m_refocusImageInfo));
    memset(&m_refocusResultInfo, 0, sizeof(m_refocusResultInfo));
    m_pJpgCodec = new JpegCodec();
    m_pRefocusPerf = new ImageRefocusPerf();
    m_pRefocusConfigInfoWrapper = new RefocusConfigInfoWrapper();
    // add for debug
    if (access(REFOCUS_DEBUG_FOLDER, 0) != -1) {
        m_pDebugHelper = new DebugHelper();
    }
    ATRACE_BEGIN(">>>>ImageRefocusJni-createInstance");
    m_pNativeRefocus = MTKRefocus::createInstance(DRV_REFOCUS_OBJ_SW);
    ATRACE_END();
    if (m_pNativeRefocus == NULL)
    {
        LOGD("<ImageRefocus><error> image refocus createRefocusInstance fail ");
        return;
    }
    m_ionHandle = ionHandle;
    m_stereoUtils.endMeasureTime("<PERF> Rf/new ImageRefocus()", startSec, startUsec);
}

ImageRefocus::~ImageRefocus()
{
    long startSec;
    long startUsec;
    m_stereoUtils.startMeasureTime(&startSec, &startUsec);
    delete m_pJpgCodec;
    delete m_pDebugHelper;
    if (NULL != m_pNativeRefocus) {
        ATRACE_BEGIN(">>>>ImageRefocusJni-destroyInstance");
        m_pNativeRefocus->RefocusReset();
        m_pNativeRefocus->destroyInstance(m_pNativeRefocus);
        // use destroy instead of delete, or else cause seldom NE
        // delete m_pNativeRefocus;
        ATRACE_END();
    }
    delete[] m_pWorkingBuffer;
    delete[] m_pDepthBuffer;

    // delete m_refocusImageInfo.TargetImgAddr; // delete ion buffer instead
    JpegCodec::destroyIon(m_pSrcJpgIonBuffer, m_ionHandle);
    JpegCodec::destroyIon(m_pDstJpgIonBuffer, m_ionHandle);

    delete[] m_refocusImageInfo.dacInfo.clrTbl;
    delete[] m_refocusImageInfo.faceInfo.face_rip;
    delete m_pRefocusPerf;
    delete m_pRefocusConfigInfoWrapper;
    m_stereoUtils.endMeasureTime("<PERF> Rf/~ImageRefocus()", startSec, startUsec);
}

bool ImageRefocus::initialize(RefocusInitConfig *pConfig)
{
    ATRACE_BEGIN(">>>>ImageRefocusJni-initialize");
    long startSec;
    long startUsec;
    m_stereoUtils.startMeasureTime(&startSec, &startUsec);
    LOGD("<initialize> begin, pConfig:%p", pConfig);
    if(NULL == pConfig)
    {
        LOGD("<initialize><error> pConfig is null");
        ATRACE_END();
        return false;
    }

    if(!prepareRefocusImageInfo(pConfig))
    {
        LOGD("<initialize><error> prepareRefocusImageInfo fail");
        ATRACE_END();
        return false;
    }

    prepareRefocusTuningInfo(pConfig);

    prepareRefocusInitInfo(pConfig);

    if(!initRefocus())
    {
       LOGD("<initialize><error> initRefocus fail");
       ATRACE_END();
       return false;
    }

    if(!prepareWorkBuffer())
    {
       LOGD("<initialize><error> prepareWorkBuffer fail");
       ATRACE_END();
       return false;
    }
    m_stereoUtils.endMeasureTime("<PERF> Rf/ImageRefocus.initialize(*)", startSec, startUsec);
    ATRACE_END();
    return true;
}

bool ImageRefocus::doRefocus(DoRefocusConfig *pConfig)
{
    ATRACE_BEGIN(">>>>ImageRefocusJni-doRefocus");
    long startSec;
    long startUsec;
    m_stereoUtils.startMeasureTime(&startSec, &startUsec);
    m_refocusImageInfo.TouchCoordX = pConfig->xCoord;
    m_refocusImageInfo.TouchCoordY = pConfig->yCoord;
    m_refocusImageInfo.DepthOfField = pConfig->depthOfField;
    LOGD("<doRefocus> TouchCoordX %d, TouchCoordY %d, DepthOfField %d, ImgFmt %d",
            m_refocusImageInfo.TouchCoordX, m_refocusImageInfo.TouchCoordY,
            m_refocusImageInfo.DepthOfField, pConfig->format);
    if (!generate())
    {
        LOGD("<doRefocus><error> generate fail");
        ATRACE_END();
        return false;
    }
    MUINT32 result = m_pNativeRefocus->RefocusFeatureCtrl(REFOCUS_FEATURE_GET_RESULT, NULL,
            (void *)&m_refocusResultInfo);
    if (S_REFOCUS_OK != result)
    {
        LOGD("<doRefocus><error> image refocus GET_RESULT fail ");
        ATRACE_END();
        return false;
    }
    m_stereoUtils.endMeasureTime("<PERF> Rf/ImageRefocus.doRefocus(*)", startSec, startUsec);
    ATRACE_END();
    return true;
}

bool ImageRefocus::encodeRefocusImg(IonInfo **pOutImgBuf, MUINT32 format)
{
    ATRACE_BEGIN(">>>>ImageRefocusJni-encodeRefocusImg");
    long startSec;
    long startUsec;
    m_stereoUtils.startMeasureTime(&startSec, &startUsec);
    LOGD("<encodeRefocusImg> begin,format:%d", format);
    m_refocusImageInfo.Mode = REFOCUS_MODE_DEPTH_AND_REFOCUS_SAVEAS;

    if (!generate())
    {
        LOGD("<encodeRefocusImg><error> generate fail");
        ATRACE_END();
        return false;
    }

    ImageBuf refocusImage;
    if (!getRefocusImageForSaving(&refocusImage) || NULL == refocusImage.buffer)
    {
        LOGD("<encodeRefocusImg><error> getRefocusImageForSaving fail");
        // delete refocusImage.buffer; // no copy, so no need to delete
        ATRACE_END();
        return false;
    }

    if (NULL != m_pDebugHelper)
    {
        MCHAR name[FILE_NAME_LENGTH];
        sprintf(name, "%s%s", REFOCUS_DEBUG_FOLDER, YUV_DUMP_NAME);
        m_pDebugHelper->dumpBufferToFile(name, refocusImage.buffer, refocusImage.bufferSize);
    }

    long startSec1;
    long startUsec1;
    m_stereoUtils.startMeasureTime(&startSec1, &startUsec1);
    // use ion instead of M4U
    int size = refocusImage.width * refocusImage.height * 3 / 2;
    IonInfo *nv21IonBuffer = JpegCodec::allocateIon(ALIGN512(size) + 512, m_ionHandle);
    if (nv21IonBuffer == NULL)
    {
        LOGD("<encodeRefocusImg><error> allocate nv21IonBuffer fail");
        ATRACE_END();
        return false;
    }
    ATRACE_BEGIN(">>>>ImageRefocusJni-encodeRefocusImg-yuvToNv21");
    // MUINT8* outputBuf = new MUINT8[refocusImage.width * refocusImage.height * 3 / 2];
    if (!m_pJpgCodec->yuvToNv21(refocusImage.buffer, refocusImage.width, refocusImage.height,
            (MUINT8 *)nv21IonBuffer->virAddr))
    {
        LOGD("<encodeRefocusImg><error> yuvToNv21 fail");
        JpegCodec::destroyIon(nv21IonBuffer, m_ionHandle);
        // delete refocusImage.buffer; // no copy, so no need to delete
        ATRACE_END();
        ATRACE_END();
        return false;
    }
    ATRACE_END();
    m_stereoUtils.endMeasureTime("<PERF> Rf/ImageRefocus.encodeRefocusImg().yuvToNv21()",
                                    startSec1, startUsec1);

    long startSec2;
    long startUsec2;
    m_stereoUtils.startMeasureTime(&startSec2, &startUsec2);
    MUINT32 outBufSize = 0;
    *pOutImgBuf = JpegCodec::allocateIon(ALIGN512(refocusImage.width * refocusImage.height) + 512, m_ionHandle);
    if (*pOutImgBuf == NULL)
    {
        LOGD("<encodeRefocusImg><error> allocate pOutImgBuf fail");
        ATRACE_END();
        return false;
    }
    ATRACE_BEGIN(">>>>ImageRefocusJni-encodeRefocusImg-nv21ToJpg");
    if (!m_pJpgCodec->nv21ToJpg(nv21IonBuffer, refocusImage.width, refocusImage.height,
            *pOutImgBuf, refocusImage.width * refocusImage.height, &outBufSize))
    {
        LOGD("<encodeRefocusImg><error> nv21ToJpg fail");
        JpegCodec::destroyIon(nv21IonBuffer, m_ionHandle);
        // delete refocusImage.buffer; // no copy, so no need to delete
        ATRACE_END();
        ATRACE_END();
        return false;
    }
    ATRACE_END();
    m_stereoUtils.endMeasureTime("<PERF> Rf/ImageRefocus.encodeRefocusImg().nv21ToJpg()",
                                    startSec2, startUsec2);

    (*pOutImgBuf)->dataSize = (MINT32) outBufSize;
    (*pOutImgBuf)->width = refocusImage.width;
    (*pOutImgBuf)->height = refocusImage.height;
    LOGD("<encodeRefocusImg> Out bufferSize %d, width %d, height %d", (*pOutImgBuf)->dataSize,
            (*pOutImgBuf)->width, (*pOutImgBuf)->height);

    if (NULL != m_pDebugHelper)
    {
        MCHAR name[FILE_NAME_LENGTH];
        sprintf(name, "%s%s", REFOCUS_DEBUG_FOLDER, SAVED_JPG_DUMP_NAME);
        m_pDebugHelper->dumpBufferToFile(name, (MUINT8*)(*pOutImgBuf)->virAddr, (*pOutImgBuf)->dataSize);
    }
    // delete refocusImage.buffer; // no copy, so no need to delete
    JpegCodec::destroyIon(nv21IonBuffer, m_ionHandle);
    m_stereoUtils.endMeasureTime("<PERF> Rf/ImageRefocus.encodeRefocusImg(*)", startSec, startUsec);
    ATRACE_END();
    return true;
}

void ImageRefocus::prepareRefocusTuningInfo(RefocusInitConfig *pConfig)
{
    m_pRefocusConfigInfoWrapper->prepareRefocusTuningInfo(&m_refocusTuningInfo, pConfig);
}

bool ImageRefocus::prepareRefocusImageInfo(RefocusInitConfig *pConfig)
{
    long startSec;
    long startUsec;
    m_stereoUtils.startMeasureTime(&startSec, &startUsec);
    LOGD("<prepareRefocusImageInfo>GeneratorInitConfig: pImageBuf %p, imageBufSize %d, "
            "depth buffer Size %d, depthWidth %d, depthHeight %d, metaWidth %d, metaHeight %d, "
            "jps width %d, jps height %d, mask width %d, mask height %d, "
            "focusCoordX %d, focusCoordY %d, posX %d, posY %d, viewWidth %d, "
            "viewHeight %d, mainCamPos %d, imageOrientation %d, depthOrientation %d, "
            "dof %d, ldcWidth %d, ldcHeight %d",
            pConfig->pImageBuf, pConfig->imageBufSize, pConfig->depth.bufferSize,
            pConfig->depth.depthWidth, pConfig->depth.depthHeight, pConfig->depth.metaWidth,
            pConfig->depth.metaHeight, pConfig->jpsWidth, pConfig->jpsHeight,
            pConfig->maskWidth, pConfig->maskHeight, pConfig->focusCoordX,
            pConfig->focusCoordY, pConfig->posX, pConfig->posY,
            pConfig->viewWidth, pConfig->viewHeight, pConfig->mainCamPos,
            pConfig->imageOrientation, pConfig->depthOrientation, pConfig->dof,
            (pConfig->ldcBuf).width, (pConfig->ldcBuf).height);

    m_refocusImageInfo.Mode = REFOCUS_MODE_FULL;
    m_refocusImageInfo.ImgFmt = REFOCUS_IMAGE_YUV420;
    ATRACE_BEGIN(">>>>ImageRefocusJni-prepareRefocusImageInfo-setOriginalJpgInfo");
    // JPG TargetWidth/TargetHeight/TargetImgAddr
    if (!setOriginalJpgInfo(pConfig->pImageBuf, pConfig->imageBufSize))
    {
        LOGD("<prepareRefocusImageInfo><error> setOriginalJpgInfo fail");
        ATRACE_END();
        return false;
    }
    ATRACE_END();

    m_refocusImageInfo.ImgNum = 1;
    m_refocusImageInfo.Width = pConfig->jpsWidth;
    m_refocusImageInfo.Height = pConfig->jpsHeight;
    // skip to set jps image:
    // m_refocusImageInfo.ImgAddr[0]

    // skip to set mask buffer:
    m_refocusImageInfo.MaskWidth = pConfig->maskWidth;
    m_refocusImageInfo.MaskHeight = pConfig->maskHeight;
    // m_refocusImageInfo.MaskImageAddr = (pConfig->mask).buffer;

    m_refocusImageInfo.PosX = pConfig->posX;
    m_refocusImageInfo.PosY = pConfig->posY;
    m_refocusImageInfo.ViewWidth = pConfig->viewWidth;
    m_refocusImageInfo.ViewHeight = pConfig->viewHeight;
    // NOTE: need copy mask buffer for algo's using.
    m_pDepthBuffer = new MUINT8[(pConfig->depth).bufferSize];
    memcpy(m_pDepthBuffer, (pConfig->depth).buffer, (pConfig->depth).bufferSize);
    m_refocusImageInfo.DepthBufferAddr = m_pDepthBuffer;
    m_refocusImageInfo.DepthBufferSize = (pConfig->depth).bufferSize;
    if (NULL != m_pDebugHelper)
    {
        m_pDebugHelper->dumpBufferToFile(DUMP_DEPTH_NAME, m_refocusImageInfo.DepthBufferAddr,
                m_refocusImageInfo.DepthBufferSize );
    }

    m_refocusImageInfo.DRZ_WD = 960;
    m_refocusImageInfo.DRZ_HT = 540;

    // TouchCoordX/ TouchCoordY/DepthOfField will be reset at setDoRefocusConfig():
    m_refocusImageInfo.TouchCoordX = pConfig->focusCoordX;
    m_refocusImageInfo.TouchCoordY = pConfig->focusCoordY;
    m_refocusImageInfo.DepthOfField = pConfig->dof;

    m_refocusImageInfo.JPSOrientation = REFOCUS_ORIENTATION_0;

    switch (pConfig->imageOrientation) {
    case 90:
        m_refocusImageInfo.JPGOrientation = REFOCUS_ORIENTATION_90;
        break;
    case 180:
        m_refocusImageInfo.JPGOrientation = REFOCUS_ORIENTATION_180;
        break;
    case 270:
        m_refocusImageInfo.JPGOrientation = REFOCUS_ORIENTATION_270;
        break;
    default:
        m_refocusImageInfo.JPGOrientation = REFOCUS_ORIENTATION_0;
        break;
    }
    m_refocusImageInfo.MainCamPos = (REFOCUS_MAINCAM_POS_ENUM)(pConfig->mainCamPos);

    m_refocusImageInfo.RcfyError = DFT_RCFY_ERROR;
    m_refocusImageInfo.RcfyIterNo = DFT_RCFY_ITER_NO;
    m_refocusImageInfo.Theta[0] = DFT_THETA;
    m_refocusImageInfo.Theta[1] = DFT_THETA;
    m_refocusImageInfo.Theta[2] = DFT_THETA;
    m_refocusImageInfo.DisparityRange = DFT_DISPARITY_RANGE;

    m_refocusImageInfo.NumOfMetadata = 1;
    m_refocusImageInfo.metaInfo[0].Type = REFOCUS_METADATA_TYPE_LDC_MAP;
    m_refocusImageInfo.metaInfo[0].Width = (pConfig->ldcBuf).width;
    m_refocusImageInfo.metaInfo[0].Height = (pConfig->ldcBuf).height;
    m_refocusImageInfo.metaInfo[0].Size = (pConfig->ldcBuf).bufferSize;
    m_refocusImageInfo.metaInfo[0].Addr = (pConfig->ldcBuf).buffer;

    // swap maskWidth/Height,PosX/PosY,ViewWidth/ViewHeight by depthOrientation
    swapConfigInfo(pConfig->depthOrientation);

    m_refocusImageInfo.dacInfo.clrTblSize = 17;
    MINT32 array[] = {1, 3, 6, 9, 12, 16, 24, 30, 36, 42, 48, 48, 48, 48, 48, 48, 48};
    m_refocusImageInfo.dacInfo.clrTbl = new MINT32[sizeof(array)/sizeof(MINT32)];
    memcpy(m_refocusImageInfo.dacInfo.clrTbl, array, sizeof(array));
    m_refocusImageInfo.dacInfo.min = pConfig->minDacData;
    m_refocusImageInfo.dacInfo.max = pConfig->maxDacData;
    m_refocusImageInfo.dacInfo.cur = pConfig->curDacData;
    m_refocusImageInfo.faceInfo.face_num = pConfig->faceNum;
    m_refocusImageInfo.faceInfo.face_pos = pConfig->facePos;
    // copy faceRip to avoid NE caused by GC
    m_refocusImageInfo.faceInfo.face_rip = new int[pConfig->faceNum];
    memcpy(m_refocusImageInfo.faceInfo.face_rip, pConfig->faceRip,
            pConfig->faceNum * sizeof(int));
    m_refocusImageInfo.faceInfo.isFd = pConfig->isFd;
    m_refocusImageInfo.faceInfo.ratio = pConfig->ratio;
    LOGD("<prepareRefocusImageInfo> minDacData:%d,maxDacData:%d,curDacData:%d,faceNum:%d, "
            "facePos:%d, ratio:%f, imageOrientation:%d", pConfig->minDacData, pConfig->maxDacData,
            pConfig->curDacData, pConfig->faceNum, pConfig->facePos,
            pConfig->ratio, pConfig->imageOrientation);
    m_pRefocusConfigInfoWrapper->prepareRefocusImageInfo(&m_refocusImageInfo, pConfig);
    m_stereoUtils.endMeasureTime("<PERF> Rf/ImageRefocus.prepareRefocusImageInfo()",
                                    startSec, startUsec);
    return true;
}

void ImageRefocus::prepareRefocusInitInfo(RefocusInitConfig *pConfig)
{
    m_refocusInitInfo.pTuningInfo = &m_refocusTuningInfo;
    m_pRefocusConfigInfoWrapper->prepareRefocusInitInfo(&m_refocusInitInfo, pConfig);
}

bool ImageRefocus::initRefocus()
{
    long startSec;
    long startUsec;
    m_stereoUtils.startMeasureTime(&startSec, &startUsec);
    ATRACE_BEGIN(">>>>ImageRefocusJni-navtiveRefocusInit");
    if (S_REFOCUS_OK != m_pNativeRefocus->RefocusInit((MUINT32 *)&m_refocusInitInfo, 0))
    {
        LOGD("<initRefocus><error> RefocusInit fail ");
        ATRACE_END();
        return false;
    }
    ATRACE_END();
    m_stereoUtils.endMeasureTime("<PERF> Rf/ImageRefocus.initRefocus()", startSec, startUsec);
    return true;
}

bool ImageRefocus::prepareWorkBuffer()
{
    long startSec;
    long startUsec;
    m_stereoUtils.startMeasureTime(&startSec, &startUsec);
    MUINT32 bufferSize;
    ATRACE_BEGIN(">>>>ImageRefocusJni-GET_WORKBUF_SIZE");
    MUINT32 result = m_pNativeRefocus->RefocusFeatureCtrl(REFOCUS_FEATURE_GET_WORKBUF_SIZE,
            (void *)&m_refocusImageInfo, (void *)&bufferSize);
    ATRACE_END();
    if (result != S_REFOCUS_OK)
    {
        LOGD("<prepareWorkBuffer><error> image refocus GET_WORKBUF_SIZE fail ");
        return false;
    }

    m_pWorkingBuffer = new MUINT8[bufferSize];
    m_refocusInitInfo.WorkingBuffAddr = (MUINT8*)m_pWorkingBuffer;

    LOGD("<prepareWorkBuffer> SET_WORKBUF_ADDR start, bufferSize %d", bufferSize);

    ATRACE_BEGIN(">>>>ImageRefocusJni-SET_WORKBUF_ADDR");
    result = m_pNativeRefocus->RefocusFeatureCtrl(REFOCUS_FEATURE_SET_WORKBUF_ADDR,
            (void *)&m_refocusInitInfo.WorkingBuffAddr, NULL);
    ATRACE_END();
    if (result != S_REFOCUS_OK)
    {
        LOGD("<prepareWorkBuffer><error> image refocus SET_WORKBUF_ADDR fail ");
        return false;
    }
    m_stereoUtils.endMeasureTime("<PERF> Rf/ImageRefocus.prepareWorkBuffer()", startSec, startUsec);
    return true;
}

bool ImageRefocus::generate()
{
    long startSec;
    long startUsec;
    m_stereoUtils.startMeasureTime(&startSec, &startUsec);
    LOGD("<generate> begin");
    ATRACE_BEGIN(">>>>ImageRefocusJni-REFOCUS_FEATURE_ADD_IMG");
    if (S_REFOCUS_OK != m_pNativeRefocus->RefocusFeatureCtrl(REFOCUS_FEATURE_ADD_IMG,
            (void *)&m_refocusImageInfo, NULL))
    {
        LOGD("<generate><error> image refocus ADD_IMG fail ");
        ATRACE_END();
        return false;
    }
    ATRACE_END();

    ATRACE_BEGIN(">>>>ImageRefocusJni-refocusPerfSrvEnable");
    m_pRefocusPerf->refocusPerfSrvEnable();
    ATRACE_BEGIN(">>>>ImageRefocusJni-RefocusMain");
    if (S_REFOCUS_OK != m_pNativeRefocus->RefocusMain())
    {
        LOGD("<generate><error> RefocusMain fail ");
        m_pRefocusPerf->refocusPerfSrvDisable();
        ATRACE_END();
        return false;
    }
    ATRACE_END();
    m_pRefocusPerf->refocusPerfSrvDisable();
    ATRACE_END();
    m_stereoUtils.endMeasureTime("<PERF> Rf/ImageRefocus.generate()", startSec, startUsec);
    return true;
}

bool ImageRefocus::getRefocusImageSize(ImageBuf *pRefocusImage) {
    pRefocusImage->width = m_refocusResultInfo.RefocusImageWidth;
    pRefocusImage->height = m_refocusResultInfo.RefocusImageHeight;
    LOGD("<getRefocusImageSize> width:%d,height:%d", pRefocusImage->width,
            pRefocusImage->height);
    return true;
}

bool ImageRefocus::getRefocusImageBuf(ImageBuf *pRefocusImage, bool isYUV)
{
    LOGD("<getRefocusImageBuf> begin,isYUV:%d", isYUV);
    MUINT32 bufferSize = 0;
    // Algo output YUV buffer, if AP needs YUV, return RefocusedYUVImageAddr directly.
    // if AP needs RGBA, need to transform YUV(RefocusedYUVImageAddr) buffer to RGBA.
    if (isYUV)
    {
        // swap U/V
        MUINT32 width = m_refocusResultInfo.RefocusImageWidth;
        MUINT32 height = m_refocusResultInfo.RefocusImageHeight;
        bufferSize = width * height * 3 / 2;
        MUINT8* yuvBuf = m_refocusResultInfo.RefocusedYUVImageAddr;
        MUINT32 ySize = width * height;
        MUINT32 uSize = width * height / 4;
        MUINT32 vSize = width * height / 4;
        memcpy(pRefocusImage->buffer, yuvBuf, ySize);
        memcpy(pRefocusImage->buffer + ySize, yuvBuf + ySize + uSize, vSize);
        memcpy(pRefocusImage->buffer + ySize + vSize, yuvBuf + ySize, uSize);
    }
    else
    {
        bufferSize = m_refocusResultInfo.RefocusImageWidth *
               m_refocusResultInfo.RefocusImageHeight * 4;
        ATRACE_BEGIN(">>>>ImageRefocusJni-getRefocusImageBuf-yuvToRgba");
        if (!m_pJpgCodec->yuvToRgba(m_refocusResultInfo.RefocusedYUVImageAddr,
                m_refocusResultInfo.RefocusImageWidth,
                m_refocusResultInfo.RefocusImageHeight, pRefocusImage->buffer))
        {
            LOGD("<getRefocusImage><error> image refocus yuvToRgba fail ");
            ATRACE_END();
            return false;
        }
        ATRACE_END();
    }
    pRefocusImage->bufferSize = bufferSize;

    if (NULL != m_pDebugHelper)
    {
        MCHAR name[FILE_NAME_LENGTH];
        sprintf(name, "%s%d_%s", REFOCUS_DEBUG_FOLDER, m_dumpIndex, REFOCUS_IMAGE_DUMP_NAME);
        m_pDebugHelper->dumpBufferToFile(name, pRefocusImage->buffer, pRefocusImage->bufferSize);
        m_dumpIndex++;
    }
    return true;
}

/*
 * 1. Return RefocusedYUVImageAddr directly(skip copying buffer) to improve saving performance.
 * 2. Algo output YUV buffer, so use YUV directly instead of transform to RGBA.
 */
bool ImageRefocus::getRefocusImageForSaving(ImageBuf *pRefocusImage)
{
    long startSec;
    long startUsec;
    m_stereoUtils.startMeasureTime(&startSec, &startUsec);
    ATRACE_BEGIN(">>>>ImageRefocusJni-getRefocusImageForSaving-REFOCUS_FEATURE_GET_RESULT");
    MUINT32 result = m_pNativeRefocus->RefocusFeatureCtrl(REFOCUS_FEATURE_GET_RESULT, NULL,
            (void *)&m_refocusResultInfo);
    ATRACE_END();
    if (S_REFOCUS_OK != result)
    {
        LOGD("<getRefocusImageForSaving><error> image refocus GET_RESULT fail ");
        return false;
    }

    LOGD("<getRefocusImageForSaving>RefocusImageWidth %d, RefocusImageHeight %d",
        m_refocusResultInfo.RefocusImageWidth, m_refocusResultInfo.RefocusImageHeight);
    pRefocusImage->width = m_refocusResultInfo.RefocusImageWidth;
    pRefocusImage->height = m_refocusResultInfo.RefocusImageHeight;
    pRefocusImage->buffer = m_refocusResultInfo.RefocusedYUVImageAddr;
    pRefocusImage->bufferSize = m_refocusResultInfo.RefocusImageWidth *
            m_refocusResultInfo.RefocusImageHeight * 3 / 2;
    if (NULL != m_pDebugHelper)
    {
        MCHAR name[FILE_NAME_LENGTH];
        sprintf(name, "%s%d_%s", REFOCUS_DEBUG_FOLDER, m_dumpIndex, REFOCUS_IMAGE_DUMP_NAME);
        m_pDebugHelper->dumpBufferToFile(name, pRefocusImage->buffer, pRefocusImage->bufferSize);
        m_dumpIndex++;
    }
    m_stereoUtils.endMeasureTime("<PERF> Rf/ImageRefocus.getRefocusImageForSaving()",
                                    startSec, startUsec);
    return true;
}

/*
Decode jpg.

Parameters:
MUINT8 *pJpegBuf [IN] src jpg buffer
MUINT32 bufSize [IN] jpg buffer size
MUINT32 sampleSize [IN] decode sample size
ImageBuf* pOutBuf [OUT] jpg

Returns:
    true if success.
*/
bool ImageRefocus::decodeJpg(MUINT8 *pJpegBuf, MUINT32 bufSize, MUINT32 sampleSize,
            ImageBuf *pOutBuf)
{
    long startSec;
    long startUsec;
    m_stereoUtils.startMeasureTime(&startSec, &startUsec);
    if (pJpegBuf == NULL)
    {
        LOGD("<decodeJpg><error> null jpeg buffer!!!");
        return false;
    }

    MUINT8 *pFileBuffer = new MUINT8[ALIGN128(bufSize) + 512 + 127];
    MUINT8 *pAlign128FileBuffer =
        (MUINT8 *)((((size_t)pFileBuffer + 127) >> 7) << 7);
    memcpy(pAlign128FileBuffer, pJpegBuf, bufSize);
    ATRACE_BEGIN(">>>>ImageRefocusJni-decodeJpg-jpgToYv12");
    if (!m_pJpgCodec->jpgToYv12(pAlign128FileBuffer, bufSize, pOutBuf, sampleSize))
    {
        LOGD("<decodeJpg><error> decode failed!!");
        delete[] pFileBuffer;
        pFileBuffer = NULL;
        ATRACE_END();
        return false;
    }
    ATRACE_END();
    LOGD("<decodeJpg>decode image end, destBuffer: %p, width: %d, height: %d",
            pOutBuf->buffer, pOutBuf->width, pOutBuf->height);

    delete[] pFileBuffer;
    pFileBuffer = NULL;

    m_stereoUtils.endMeasureTime("<PERF> Rf/ImageRefocus.decodeJpg()", startSec, startUsec);
    return true;
}

bool ImageRefocus::decodeJpg(IonInfo *pJpegBuf, MUINT32 bufSize, MUINT32 sampleSize,
            IonInfo **pOutBuf, int32_t ionHandle)
{
    long startSec;
    long startUsec;
    m_stereoUtils.startMeasureTime(&startSec, &startUsec);
    if (pJpegBuf == NULL)
    {
        LOGD("<decodeJpg><error> null jpeg buffer!!!");
        return false;
    }
    ATRACE_BEGIN(">>>>ImageRefocusJni-decodeJpg-jpgToYv12");
    if (!m_pJpgCodec->jpgToYv12(pJpegBuf, bufSize, pOutBuf, sampleSize, ionHandle))
    {
        LOGD("<decodeJpg><error> decode failed!!");
        ATRACE_END();
        return false;
    }
    ATRACE_END();
    LOGD("<decodeJpg>decode image end, destBuffer: %p, width: %d, height: %d",
            (*pOutBuf)->virAddr, (*pOutBuf)->width, (*pOutBuf)->height);

    m_stereoUtils.endMeasureTime("<PERF> Rf/ImageRefocus.decodeJpg()", startSec, startUsec);
    return true;
}

bool ImageRefocus::setOriginalJpgInfo(MUINT8 *pJpegBuf, MUINT32 bufSize)
{
    long startSec;
    long startUsec;
    m_stereoUtils.startMeasureTime(&startSec, &startUsec);
    LOGD("<setOriginalJpgInfo> begin, pJpegBuf:%p,bufSize:%d",pJpegBuf, bufSize);

    if (NULL != m_pDebugHelper)
    {
        MCHAR name[FILE_NAME_LENGTH];
        sprintf(name, "%s%s", REFOCUS_DEBUG_FOLDER, ORI_JPG_NAME);
        m_pDebugHelper->dumpBufferToFile(name, pJpegBuf, bufSize);
    }

    // use ion instead of M4U
    if (m_pSrcJpgIonBuffer != NULL)
    {
        JpegCodec::destroyIon(m_pSrcJpgIonBuffer, m_ionHandle);
    }
    m_pSrcJpgIonBuffer = JpegCodec::allocateIon(ALIGN512(bufSize) + 512, m_ionHandle);
    if (m_pSrcJpgIonBuffer == NULL)
    {
        LOGD("<setOriginalJpgInfo><error> allocate m_pSrcJpgIonBuffer fail");
        return false;
    }
    memcpy(m_pSrcJpgIonBuffer->virAddr, (uint8_t *) pJpegBuf, bufSize);

    if (!decodeJpg(m_pSrcJpgIonBuffer, bufSize, DECODE_JPG_SAMPLE_SIZE, &m_pDstJpgIonBuffer,
            m_ionHandle))
    {
        LOGD("<setOriginalJpgInfo><error> decodeJpg fail");
        return false;
    }
    m_refocusImageInfo.TargetWidth = m_pDstJpgIonBuffer->width;
    m_refocusImageInfo.TargetHeight = m_pDstJpgIonBuffer->height;
    m_refocusImageInfo.TargetImgAddr = (MUINT8*)m_pDstJpgIonBuffer->virAddr;

    updateFacePos(m_pDstJpgIonBuffer->width, m_pDstJpgIonBuffer->height, m_refocusImageInfo.faceInfo.face_num,
            m_refocusImageInfo.faceInfo.face_pos);

    if (NULL != m_pDebugHelper)
    {
        MCHAR name[FILE_NAME_LENGTH];
        sprintf(name, "%s%s", REFOCUS_DEBUG_FOLDER, JPG_DUMP_NAME);
        m_pDebugHelper->dumpBufferToFile(name, (MUINT8*)m_pDstJpgIonBuffer->virAddr, m_pDstJpgIonBuffer->dataSize);
    }
    m_stereoUtils.endMeasureTime("<PERF> Rf/ImageRefocus.setOriginalJpgInfo()",
                                    startSec, startUsec);
    return true;
}

/*
    when photo is captured at landscape mode, and jps is portrait, jpg is land
    and depthbuffer can be generated when jps is portrait. so we need generate
    depthbuffer at portrait mode ,then rotate depthbuffer to land.
    so we do NOT swap maskWidth/maskHeight, PosX/PosY,
    ViewWidth/ViewHeight at initRefocusNoDepthMap because we need portrait depthbuffer.
    after depthbuffer's rotation, we can swap those params to generate land image,
    so we swap here
*/
void ImageRefocus::swapConfigInfo(MUINT32 depthOrientation)
{
    if (depthOrientation == ORIENTATION_90 || depthOrientation == ORIENTATION_270) {
        swap(&(m_refocusImageInfo.MaskWidth), &(m_refocusImageInfo.MaskHeight));
        swap(&(m_refocusImageInfo.PosX), &(m_refocusImageInfo.PosY));
        swap(&(m_refocusImageInfo.ViewWidth), &(m_refocusImageInfo.ViewHeight));
        LOGD("<swapConfigInfo>after swapping, maskWidth %d, maskHeight %d, ViewWidth %d, "
                "ViewHeight %d, PosX %d, PosY %d", m_refocusImageInfo.MaskWidth,
                m_refocusImageInfo.MaskHeight, m_refocusImageInfo.ViewWidth,
                m_refocusImageInfo.ViewHeight, m_refocusImageInfo.PosX, m_refocusImageInfo.PosY);
    }
}

void ImageRefocus::swap(MUINT32 *x, MUINT32 *y)
{
    MUINT32 temp = *x;
    *x = *y;
    *y = temp;
}

void ImageRefocus::updateFacePos(MINT32 imageWidth, MINT32 imageHeight, MINT32 faceNum,
    RectImgSeg* facePos)
{
    for (MINT32 i = 0; i < faceNum; i++)
    {
        getImageRect(imageWidth, imageHeight, facePos[i], &facePos[i]);
    }
}

void ImageRefocus::getImageRect(MINT32 width, MINT32 height, RectImgSeg inRect,
        RectImgSeg* outRect)
{
    outRect->left = (int)((double)(inRect.left + SENSOR_RECT_LEN / 2) * width
            / SENSOR_RECT_LEN);
    outRect->right = (int)((double)(inRect.right + SENSOR_RECT_LEN / 2) * width
            / SENSOR_RECT_LEN);

    outRect->top = (int)((double)(inRect.top + SENSOR_RECT_LEN / 2) * height
            / SENSOR_RECT_LEN);
    outRect->bottom = (int)((double)(inRect.bottom + SENSOR_RECT_LEN / 2) * height
            / SENSOR_RECT_LEN);
}
