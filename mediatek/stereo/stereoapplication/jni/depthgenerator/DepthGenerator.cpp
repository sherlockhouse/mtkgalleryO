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

#include "DepthGenerator.h"
#include "JpegCodec.h"

using namespace stereo;

DepthGenerator::DepthGenerator(int32_t ionHandle)
{
    long startSec;
    long startUsec;
    m_stereoUtils.startMeasureTime(&startSec, &startUsec);
    memset(&m_refocusInitInfo, 0, sizeof(m_refocusInitInfo));
    memset(&m_refocusTuningInfo, 0, sizeof(m_refocusTuningInfo));
    memset(&m_refocusImageInfo, 0, sizeof(m_refocusImageInfo));
    m_pJpgCodec = new JpegCodec();
    // add for debug
    if (access(DEPTH_GENERATOR_FOLDER, 0) != -1) {
        m_pDebugHelper = new DebugHelper();
    }
    m_pNativeRefocus = MTKRefocus::createInstance(DRV_REFOCUS_OBJ_SW);
    if (m_pNativeRefocus == NULL)
    {
        LOGD("<DepthGenerator><error> image refocus createRefocusInstance fail ");
        return;
    }
    m_ionHandle = ionHandle;
    m_stereoUtils.endMeasureTime("Rf/new DepthGenerator()", startSec, startUsec);
}

DepthGenerator::~DepthGenerator()
{
    long startSec;
    long startUsec;
    m_stereoUtils.startMeasureTime(&startSec, &startUsec);
    delete m_pJpgCodec;
    delete m_pDebugHelper;
    if (NULL != m_pNativeRefocus) {
        m_pNativeRefocus->RefocusReset();
        m_pNativeRefocus->destroyInstance(m_pNativeRefocus);
        // can't do this, seldom JE
        // delete m_pNativeRefocus;
    }
    delete[] m_pWorkingBuffer;
    delete[] m_pMaskBuffer;
    delete[] m_pLdcBuffer;
    // delete m_refocusImageInfo.TargetImgAddr;  // delete ion buffer instead
    JpegCodec::destroyIon(m_pSrcJpgIonBuffer, m_ionHandle);
    JpegCodec::destroyIon(m_pDstJpgIonBuffer, m_ionHandle);
    // delete m_refocusImageInfo.ImgAddr[0];  // jps
    JpegCodec::destroyIon(m_pSrcJpsIonBuffer, m_ionHandle);
    JpegCodec::destroyIon(m_pDstJpsIonBuffer, m_ionHandle);
    delete[] m_refocusImageInfo.dacInfo.clrTbl;
    m_stereoUtils.endMeasureTime("Rf/~DepthGenerator()", startSec, startUsec);
}

/*
Initialize process.

Parameters:
GeneratorInitConfig *pConfig [IN] config parameters

Returns:
    true if success.
*/
bool DepthGenerator::initialize(GeneratorInitConfig *pConfig)
{
    long startSec;
    long startUsec;
    m_stereoUtils.startMeasureTime(&startSec, &startUsec);
    if(NULL == pConfig)
    {
        LOGD("<initialize><error> pConfig is null");
        return false;
    }

    if(!prepareRefocusImageInfo(pConfig))
    {
        LOGD("<initialize><error> prepareRefocusImageInfo fail");
        return false;
    }

    prepareRefocusTuningInfo();

    prepareRefocusInitInfo();

    if(!initRefocus())
    {
        LOGD("<initialize><error> initRefocus fail");
        return false;
    }

    if(!prepareWorkBuffer())
    {
        LOGD("<initialize><error> prepareWorkBuffer fail");
        return false;
    }
    m_stereoUtils.endMeasureTime("Rf/DepthGenerator.initialize(*)", startSec, startUsec);
    return true;
}

/*
Generate depth.

Parameters:
DepthResult *pResult  [OUT] depth result

Returns:
    true if success.
*/
bool DepthGenerator::generateDepth(DepthInfo *pDepthInfo)
{
    long startSec;
    long startUsec;
    m_stereoUtils.startMeasureTime(&startSec, &startUsec);
    if(!generate())
    {
        LOGD("<generateDepth><error> generate fail");
        return false;
    }
    // TO-DO
    // who will allocate depthBuffer and XmpBuffer?
    // if App does, how could it get to know size?
    if(!getDepthInfo(pDepthInfo))
    {
        LOGD("<generateDepth><error> getDepthInfo fail");
        return false;
    }
    m_stereoUtils.endMeasureTime("Rf/DepthGenerator.generateDepth(*)", startSec, startUsec);
    return true;
}

bool DepthGenerator::setOriginalJpgInfo(MUINT8 *pJpegBuf, MUINT32 bufSize)
{
    long startSec;
    long startUsec;
    m_stereoUtils.startMeasureTime(&startSec, &startUsec);
    LOGD("<setOriginalJpgInfo> begin, pJpegBuf:%p,bufSize:%d",pJpegBuf, bufSize);

    if (NULL != m_pDebugHelper)
    {
        MCHAR name[FILE_NAME_LENGTH];
        sprintf(name, "%s%s", DEPTH_GENERATOR_FOLDER, ORI_JPG_NAME);
        m_pDebugHelper->dumpBufferToFile(name, pJpegBuf, bufSize);
    }

    // use ion instead of M4U
    if (m_pSrcJpgIonBuffer != NULL)
    {
        JpegCodec::destroyIon(m_pSrcJpsIonBuffer, m_ionHandle);
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
        sprintf(name, "%s%s", DEPTH_GENERATOR_FOLDER, JPG_DUMP_NAME);
        m_pDebugHelper->dumpBufferToFile(name, (MUINT8*)m_pDstJpgIonBuffer->virAddr, m_pDstJpgIonBuffer->dataSize);
    }
    m_stereoUtils.endMeasureTime("Rf/DepthGenerator.setOriginalJpgInfo()", startSec, startUsec);
    return true;
}

bool DepthGenerator::setJpsInfo(MUINT8 *pJpsBuf, MUINT32 bufSize)
{
    long startSec;
    long startUsec;
    m_stereoUtils.startMeasureTime(&startSec, &startUsec);
    if (NULL != m_pDebugHelper)
    {
        MCHAR name[FILE_NAME_LENGTH];
        sprintf(name, "%s%s", DEPTH_GENERATOR_FOLDER, ORI_JPS_NAME);
        m_pDebugHelper->dumpBufferToFile(name, pJpsBuf, bufSize);
    }

    // use ion instead of M4U
    if (m_pSrcJpsIonBuffer != NULL)
    {
        JpegCodec::destroyIon(m_pSrcJpsIonBuffer, m_ionHandle);
    }
    m_pSrcJpsIonBuffer = JpegCodec::allocateIon(ALIGN512(bufSize) + 512, m_ionHandle);
    if (m_pSrcJpsIonBuffer == NULL)
    {
        LOGD("<setJpsInfo><error> allocate setJpsInfo fail");
        return false;
    }
    memcpy(m_pSrcJpsIonBuffer->virAddr, (uint8_t *) pJpsBuf, bufSize);

    if (!decodeJpg(m_pSrcJpsIonBuffer, bufSize, DECODE_JPS_SAMPLE_SIZE, &m_pDstJpsIonBuffer,
            m_ionHandle))
    {
        LOGD("<setJpsInfo><error> decodeJps fail");
        return false;
    }
    m_refocusImageInfo.Width = m_pDstJpsIonBuffer->width;
    m_refocusImageInfo.Height = m_pDstJpsIonBuffer->height;
    m_refocusImageInfo.ImgAddr[0] = (MUINT8*)m_pDstJpsIonBuffer->virAddr;
    if (NULL != m_pDebugHelper)
    {
        MCHAR name[FILE_NAME_LENGTH];
        sprintf(name, "%s%s", DEPTH_GENERATOR_FOLDER, JPS_DUMP_NAME);
        m_pDebugHelper->dumpBufferToFile(name, (MUINT8*)m_pDstJpsIonBuffer->virAddr,
                m_pDstJpsIonBuffer->dataSize);
    }
    m_stereoUtils.endMeasureTime("Rf/DepthGenerator.setJpsInfo()", startSec, startUsec);
    return true;
}

void DepthGenerator::prepareRefocusTuningInfo()
{
    long startSec;
    long startUsec;
    m_stereoUtils.startMeasureTime(&startSec, &startUsec);
    //m_refocusTuningInfo = {8, 16, 4, 0, 4, 1, 3.4};
    m_refocusTuningInfo.HorzDownSampleRatio = 4;
    m_refocusTuningInfo.VertDownSampleRatio = 4;
    m_refocusTuningInfo.IterationTimes = 3;
    m_refocusTuningInfo.InterpolationMode = 0;
    m_refocusTuningInfo.CoreNumber = 4;
    m_refocusTuningInfo.NumOfExecution = 1;
    m_refocusTuningInfo.Baseline = 2.0f;
    m_refocusTuningInfo.RFCoreNumber[0] = 4;
    m_refocusTuningInfo.RFCoreNumber[1] = 4;
    m_refocusTuningInfo.RFCoreNumber[2] = 2;
    m_stereoUtils.endMeasureTime("Rf/DepthGenerator.prepareRefocusTuningInfo()",
                                    startSec, startUsec);
}

bool DepthGenerator::prepareRefocusImageInfo(GeneratorInitConfig *pConfig)
{
    long startSec;
    long startUsec;
    m_stereoUtils.startMeasureTime(&startSec, &startUsec);
    m_depthOrientation = pConfig->depthOrientation;

    LOGD("<prepareRefocusImageInfo>GeneratorInitConfig: pImageBuf %p, imageBufSize %d, "
            "jps width %d, jps height %d, mask width %d, mask height %d, ldc width %d, ldc height %d, "
            "posX %d, posY %d, viewWidth %d, viewHeight %d, mainCamPos %d, imageOrientation %d, "
            "depthOrientation %d", pConfig->pImageBuf, pConfig->imageBufSize, pConfig->jps.width,
            pConfig->jps.height, pConfig->mask.width, pConfig->mask.height,
            pConfig->ldc.width, pConfig->ldc.height, pConfig->posX, pConfig->posY,
            pConfig->viewWidth, pConfig->viewHeight, pConfig->mainCamPos,
            pConfig->imageOrientation, pConfig->depthOrientation);

    m_refocusImageInfo.Mode = REFOCUS_MODE_DEPTH_ONLY;
    m_refocusImageInfo.ImgFmt = REFOCUS_IMAGE_YUV420;

    // JPG TargetWidth/TargetHeight/TargetImgAddr
    if (!setOriginalJpgInfo(pConfig->pImageBuf, pConfig->imageBufSize))
    {
        LOGD("<prepareRefocusImageInfo><error> setOriginalJpgInfo fail");
        return false;
    }

    m_refocusImageInfo.ImgNum = 1;
    // JPS Width/Height/ImgAddr
    if (!setJpsInfo((pConfig->jps).buffer, (pConfig->jps).bufferSize))
    {
        LOGD("<prepareRefocusImageInfo><error> setJpsInfo fail");
        return false;
    }

    m_refocusImageInfo.MaskWidth = (pConfig->mask).width;
    m_refocusImageInfo.MaskHeight = (pConfig->mask).height;
    // NOTE: need copy mask buffer for algo's using.
    MUINT32 maskSize = (pConfig->mask).width * (pConfig->mask).height;
    m_pMaskBuffer = new MUINT8[maskSize];
    memcpy(m_pMaskBuffer, (pConfig->mask).buffer, maskSize);
    m_refocusImageInfo.MaskImageAddr = m_pMaskBuffer;

    m_refocusImageInfo.PosX = pConfig->posX;
    m_refocusImageInfo.PosY = pConfig->posY;
    m_refocusImageInfo.ViewWidth = pConfig->viewWidth;
    m_refocusImageInfo.ViewHeight = pConfig->viewHeight;

    m_refocusImageInfo.DepthBufferAddr = NULL;
    // skip to set DepthBufferSize:
    // m_refocusImageInfo.DepthBufferSize = 0;

    m_refocusImageInfo.DRZ_WD = 960;
    m_refocusImageInfo.DRZ_HT = 540;

    // skip to set TouchCoordX/TouchCoordY/DepthOfField:
    // m_refocusImageInfo.TouchCoordX
    // m_refocusImageInfo.TouchCoordY;
    // m_refocusImageInfo.DepthOfField;

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
    m_refocusImageInfo.metaInfo[0].Width = (pConfig->ldc).width;
    m_refocusImageInfo.metaInfo[0].Height = (pConfig->ldc).height;
    m_refocusImageInfo.metaInfo[0].Size = (pConfig->ldc).bufferSize;
    // NOTE: need copy ldc buffer for algo's using.
    m_pLdcBuffer = new MUINT8[(pConfig->ldc).bufferSize];
    memcpy(m_pLdcBuffer, (pConfig->ldc).buffer, (pConfig->ldc).bufferSize);
    m_refocusImageInfo.metaInfo[0].Addr = m_pLdcBuffer;

    m_refocusImageInfo.dacInfo.clrTblSize = 17;
    MINT32 array[] = {1, 3, 6, 9, 12, 16, 24, 30, 36, 42, 48, 48, 48, 48, 48, 48, 48};
    m_refocusImageInfo.dacInfo.clrTbl = new MINT32[sizeof(array)/sizeof(MINT32)];
    memcpy(m_refocusImageInfo.dacInfo.clrTbl, array, sizeof(array));
    m_refocusImageInfo.dacInfo.min = pConfig->minDacData;
    m_refocusImageInfo.dacInfo.max = pConfig->maxDacData;
    m_refocusImageInfo.dacInfo.cur = pConfig->curDacData;
    m_refocusImageInfo.faceInfo.face_num = pConfig->faceNum;
    m_refocusImageInfo.faceInfo.face_pos = pConfig->facePos;
    m_refocusImageInfo.faceInfo.face_rip = pConfig->faceRip;
    m_refocusImageInfo.faceInfo.isFd = pConfig->isFd;
    m_refocusImageInfo.faceInfo.ratio = pConfig->ratio;
    LOGD("<prepareRefocusImageInfo> minDacData:%d,maxDacData:%d,curDacData:%d,faceNum:%d, "
            "facePos:%d, ratio:%f, imageOrientation:%d", pConfig->minDacData, pConfig->maxDacData,
            pConfig->curDacData, pConfig->faceNum, pConfig->facePos,
            pConfig->ratio, pConfig->imageOrientation);
    m_stereoUtils.endMeasureTime("Rf/DepthGenerator.prepareRefocusImageInfo()",
                                    startSec, startUsec);
    return true;
}

void DepthGenerator::prepareRefocusInitInfo()
{
    m_refocusInitInfo.pTuningInfo = &m_refocusTuningInfo;
}

bool DepthGenerator::initRefocus()
{
    long startSec;
    long startUsec;
    m_stereoUtils.startMeasureTime(&startSec, &startUsec);
    if (S_REFOCUS_OK != m_pNativeRefocus->RefocusInit((MUINT32 *)&m_refocusInitInfo, 0))
    {
        LOGD("<initRefocus><error> RefocusInit fail ");
        return false;
    }
    m_stereoUtils.endMeasureTime("Rf/DepthGenerator.initRefocus()", startSec, startUsec);
    return true;
}

bool DepthGenerator::prepareWorkBuffer()
{
    long startSec;
    long startUsec;
    m_stereoUtils.startMeasureTime(&startSec, &startUsec);
    MUINT32 bufferSize;

    MUINT32 result = m_pNativeRefocus->RefocusFeatureCtrl(REFOCUS_FEATURE_GET_WORKBUF_SIZE,
            (void *)&m_refocusImageInfo, (void *)&bufferSize);
    if (result != S_REFOCUS_OK)
    {
        LOGD("<prepareWorkBuffer><error> image refocus GET_WORKBUF_SIZE fail ");
        return false;
    }

    m_pWorkingBuffer = new MUINT8[bufferSize];
    m_refocusInitInfo.WorkingBuffAddr = (MUINT8*)m_pWorkingBuffer;
    result = m_pNativeRefocus->RefocusFeatureCtrl(REFOCUS_FEATURE_SET_WORKBUF_ADDR,
            (void *)&m_refocusInitInfo.WorkingBuffAddr, NULL);
    if (result != S_REFOCUS_OK)
    {
        LOGD("<prepareWorkBuffer><error> image refocus SET_WORKBUF_ADDR fail ");
        return false;
    }
    m_stereoUtils.endMeasureTime("Rf/DepthGenerator.prepareWorkBuffer()", startSec, startUsec);
    return true;
}

bool DepthGenerator::generate()
{
    long startSec;
    long startUsec;
    m_stereoUtils.startMeasureTime(&startSec, &startUsec);
    if (S_REFOCUS_OK != m_pNativeRefocus->RefocusFeatureCtrl(REFOCUS_FEATURE_ADD_IMG,
            (void *)&m_refocusImageInfo, NULL))
    {
        LOGD("<generate><error> image refocus ADD_IMG fail ");
        return false;
    }

    if (S_REFOCUS_OK != m_pNativeRefocus->RefocusMain())
    {
        LOGD("<generate><error> image refocus RefocusMain fail ");
        return false;
    }
    m_stereoUtils.endMeasureTime("Rf/DepthGenerator.generate()", startSec, startUsec);
    return true;
}

bool DepthGenerator::getDepthInfo(DepthInfo *pDepthInfo)
{
    long startSec;
    long startUsec;
    m_stereoUtils.startMeasureTime(&startSec, &startUsec);
    RefocusResultInfo refocusResultInfo;
    memset(&refocusResultInfo, 0, sizeof(refocusResultInfo));

    MUINT32 result = m_pNativeRefocus->RefocusFeatureCtrl(REFOCUS_FEATURE_GET_RESULT, NULL,
            (void *)&refocusResultInfo);
    if (S_REFOCUS_OK != result)
    {
        LOGD("<getDepthInfo><error> image refocus GET_RESULT fail ");
        return false;
    }

    LOGD("<getDepthInfo>DepthBufferWidth %d, DepthBufferHeight %d, "
        "DepthBufferSize %d, MetaBufferWidth %d, MetaBufferHeight %d, "
        "XMPDepthWidth %d, XMPDepthHeight %d",
        refocusResultInfo.DepthBufferWidth, refocusResultInfo.DepthBufferHeight,
        refocusResultInfo.DepthBufferSize, refocusResultInfo.MetaBufferWidth,
        refocusResultInfo.MetaBufferHeight, refocusResultInfo.XMPDepthWidth,
        refocusResultInfo.XMPDepthHeight);

    // NOTE: need rotateDepthBuffer by orientation
    (pDepthInfo->depth).buffer = new MUINT8[refocusResultInfo.DepthBufferSize];
    rotateDepthInfo(&refocusResultInfo, m_depthOrientation, (pDepthInfo->depth).buffer);
    // NOTE: need swap depthWidth/depthHeight, metaWidth/metaHeight by depthOrientation
    (pDepthInfo->depth).bufferSize = refocusResultInfo.DepthBufferSize;
    (pDepthInfo->depth).depthWidth = refocusResultInfo.DepthBufferWidth;
    (pDepthInfo->depth).depthHeight = refocusResultInfo.DepthBufferHeight;
    (pDepthInfo->depth).metaWidth = refocusResultInfo.MetaBufferWidth;
    (pDepthInfo->depth).metaHeight = refocusResultInfo.MetaBufferHeight;
    if (m_depthOrientation == ORIENTATION_90 || m_depthOrientation == ORIENTATION_270)
    {
        swap(&((pDepthInfo->depth).depthWidth), &((pDepthInfo->depth).depthHeight));
        swap(&((pDepthInfo->depth).metaWidth), &((pDepthInfo->depth).metaHeight));
        LOGD("<getDepthInfo> m_depthOrientation %d, AFTER SWAP, depthWidth/depthHeight/metaWidth/metaHeight",
                m_depthOrientation, (pDepthInfo->depth).depthWidth, (pDepthInfo->depth).depthHeight,
                (pDepthInfo->depth).metaWidth, (pDepthInfo->depth).metaHeight);
    }

    (pDepthInfo->xmpDepth).bufferSize = refocusResultInfo.XMPDepthWidth *
            refocusResultInfo.XMPDepthHeight;
    (pDepthInfo->xmpDepth).width = refocusResultInfo.XMPDepthWidth;
    (pDepthInfo->xmpDepth).height = refocusResultInfo.XMPDepthHeight;
    (pDepthInfo->xmpDepth).buffer = new MUINT8[(pDepthInfo->xmpDepth).bufferSize];
    memcpy((pDepthInfo->xmpDepth).buffer, refocusResultInfo.XMPDepthMapAddr,
            (pDepthInfo->xmpDepth).bufferSize);

    if (NULL != m_pDebugHelper)
    {
        MCHAR name[FILE_NAME_LENGTH];
        sprintf(name, "%sdepthout.raw", DEPTH_GENERATOR_FOLDER);
        m_pDebugHelper->dumpBufferToFile(name, (pDepthInfo->depth).buffer,
                (pDepthInfo->depth).bufferSize);
    }
    m_stereoUtils.endMeasureTime("Rf/DepthGenerator.getDepthInfo()", startSec, startUsec);
    return true;
}

/*
Decode jpg.

Parameters:
MUINT8 *pJpegBuf [IN] src jpg buffer
MUINT32 bufSize [IN] jpg buffer size
MUINT32 sampleSize [IN] decode sample size
ImageBuf *pOutBuf [OUT] jpg

Returns:
    true if success.
*/
bool DepthGenerator::decodeJpg(MUINT8 *pJpegBuf, MUINT32 bufSize, MUINT32 sampleSize,
            ImageBuf *pOutBuf)
{
    if (pJpegBuf == NULL)
    {
        LOGD("<decodeJpg><error> null jpeg buffer!!!");
        return false;
    }
    long startSec;
    long startUsec;
    m_stereoUtils.startMeasureTime(&startSec, &startUsec);

    MUINT8 *pFileBuffer = new MUINT8[ALIGN128(bufSize) + 512 + 127];
    MUINT8 *pAlign128FileBuffer =
        (MUINT8 *)((((size_t)pFileBuffer + 127) >> 7) << 7);
    memcpy(pAlign128FileBuffer, pJpegBuf, bufSize);

    if (!m_pJpgCodec->jpgToYv12(pAlign128FileBuffer, bufSize, pOutBuf, sampleSize))
    {
        LOGD("<decodeJpg><error> decode failed!!");
        delete[] pFileBuffer;
        pFileBuffer = NULL;
        return false;
    }

    LOGD("<decodeJpg>decode image end, destBuffer: %p, width: %d, height: %d",
            pOutBuf->buffer, pOutBuf->width, pOutBuf->height);

    delete[] pFileBuffer;
    pFileBuffer = NULL;

    m_stereoUtils.endMeasureTime("Rf/DepthGenerator.decodeJpg()", startSec, startUsec);
    return true;
}

bool DepthGenerator::decodeJpg(IonInfo *pJpegBuf, MUINT32 bufSize, MUINT32 sampleSize,
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

    if (!m_pJpgCodec->jpgToYv12(pJpegBuf, bufSize, pOutBuf, sampleSize, ionHandle))
    {
        LOGD("<decodeJpg><error> decode failed!!");
        return false;
    }

    LOGD("<decodeJpg>decode image end, destBuffer: %p, width: %d, height: %d",
            (*pOutBuf)->virAddr, (*pOutBuf)->width, (*pOutBuf)->height);

    m_stereoUtils.endMeasureTime("<PERF> Rf/DepthGenerator.decodeJpg()", startSec, startUsec);
    return true;
}

 /*
Rotate depth buffer.

Parameters:
RefocusResultInfo refocusResultInfo [IN] refocusResultInfo
MUINT32 depthOrientation [IN] depthOrientation
MUINT8 * pDepthBuffer [OUT] rotated depth buffer

*/
void DepthGenerator::rotateDepthInfo(RefocusResultInfo *pRefocusResultInfo,
        MUINT32 depthOrientation, MUINT8 * pDepthBuffer)
{
    long startSec;
    long startUsec;
    m_stereoUtils.startMeasureTime(&startSec, &startUsec);
    LOGD("<rotateDepthBuffer>DepthBufferSize %d ", pRefocusResultInfo->DepthBufferSize);
    memcpy(pDepthBuffer, (MUINT8*)pRefocusResultInfo->DepthBufferAddr,
            pRefocusResultInfo->DepthBufferSize);

    if (depthOrientation == ORIENTATION_90 || depthOrientation == ORIENTATION_180
            || depthOrientation == ORIENTATION_270)
    {
        MINT32 offset = 0;
        MINT32 bufferWidth = 0;
        MINT32 bufferHeight = 0;
        MINT32 depthBufferWidth = pRefocusResultInfo->DepthBufferWidth;
        MINT32 depthBufferHeight = pRefocusResultInfo->DepthBufferHeight;
        MINT32 metaBufferWidth = pRefocusResultInfo->MetaBufferWidth;
        MINT32 metaBufferHeight = pRefocusResultInfo->MetaBufferHeight;
        MCHAR name[FILE_NAME_LENGTH];
        for (MUINT32 i = 0; i < DEPTH_BUFFER_SECTION_SIZE; i++)
        {
            switch (i)
            {
                case WMI_DEPTH_MAP_INDEX:
                    offset = 0;
                    bufferWidth = depthBufferWidth;
                    bufferHeight = depthBufferHeight;
                    break;
                case DS4_BUFFER_Y_INDEX:
                    offset = depthBufferWidth * depthBufferHeight
                            + (i - 1) * metaBufferWidth * metaBufferHeight;
                    bufferWidth = depthBufferWidth;
                    bufferHeight = depthBufferHeight;
                    break;
                case VAR_BUFFER_INDEX:
                case DVEC_MAP_INDEX:
                    continue;  // no need to rotate
                default:
                    offset = depthBufferWidth * depthBufferHeight
                            + (i - 1)* metaBufferWidth * metaBufferHeight;
                    bufferWidth = metaBufferWidth;
                    bufferHeight = metaBufferHeight;
                    break;
            }

            LOGD("<rotateDepthBuffer>rotate section %d: offset %d, bufferWidth %d, "
                    "bufferHeight %d", i, offset, bufferWidth, bufferHeight);
            if (NULL != m_pDebugHelper)
            {
                sprintf(name, "%ssection_%d_before_rotate.raw", DEPTH_GENERATOR_FOLDER, i);
                m_pDebugHelper->dumpBufferToFile(name, pRefocusResultInfo->DepthBufferAddr + offset,
                         bufferWidth * bufferHeight);
            }

            rotateBuffer((MUINT8*)pRefocusResultInfo->DepthBufferAddr + offset, pDepthBuffer + offset,
                    bufferWidth, bufferHeight,  depthOrientation);

            if (NULL != m_pDebugHelper)
            {
                sprintf(name, "%ssection_%d_after_rotate.raw", DEPTH_GENERATOR_FOLDER, i);
                m_pDebugHelper->dumpBufferToFile(name, pDepthBuffer + offset,
                        bufferWidth * bufferHeight);
            }
        }
    }
    m_stereoUtils.endMeasureTime("Rf/DepthGenerator.rotateDepthInfo()", startSec, startUsec);
}


void DepthGenerator::rotateBuffer(MUINT8*  bufferIn, MUINT8*  bufferOut, MINT32 bufferWidth,
        MINT32 bufferHeight, MUINT32 orientation)
{
    long startSec;
    long startUsec;
    m_stereoUtils.startMeasureTime(&startSec, &startUsec);
    LOGD("<rotateBuffer>bufferWidth %d, bufferHeight %d, orientation %d",
            bufferWidth, bufferHeight, orientation);

    MINT32 index = 0;
    switch (orientation)
    {
        case ORIENTATION_90:
            // rotate 90 degree, clockwise
            index = 0;
            for (MINT32 i = bufferHeight - 1; i >= 0; i--)
            {
                for (MUINT32 j = 0; j < bufferWidth; j++)
                {
                    bufferOut[i + j * bufferHeight] = bufferIn[index];
                    index++;
                }
            }
            break;

        case ORIENTATION_270:
            // rotate 270 degree, clockwise
            index = 0;
            for (MINT32 i = 0; i < bufferHeight; i++)
            {
                for (MINT32 j = bufferWidth - 1; j >= 0; j--)
                {
                    bufferOut[i + j * bufferHeight] = bufferIn[index];
                    index++;
                }
            }
            break;

        case ORIENTATION_180:
            // rotate 180 degree, clockwise
            index = 0;
            for (MINT32 j = bufferHeight - 1; j >= 0; j--)
            {
                for (MINT32 i = bufferWidth - 1; i >= 0; i--)
                {
                    bufferOut[i + j * bufferWidth] = bufferIn[index];
                    index++;
                }
            }
            break;

        default:
            break;
    }
    m_stereoUtils.endMeasureTime("Rf/DepthGenerator.rotateBuffer()", startSec, startUsec);
}

void DepthGenerator::swap(MINT32 *x, MINT32 *y)
{
    MINT32 temp = *x;
    *x = *y;
    *y = temp;
}

void DepthGenerator::updateFacePos(MINT32 imageWidth, MINT32 imageHeight, MINT32 faceNum,
    RectImgSeg* facePos)
{
    for (MINT32 i = 0; i < faceNum; i++)
    {
        getImageRect(imageWidth, imageHeight, facePos[i], &facePos[i]);
    }
}

void DepthGenerator::getImageRect(MINT32 width, MINT32 height, RectImgSeg inRect,
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
