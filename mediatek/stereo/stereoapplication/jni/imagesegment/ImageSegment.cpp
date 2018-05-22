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

#include "ImageSegment.h"
#include "DepthParser.h"

using namespace stereo;

// TODO log & some questions
// TODO how to handle fill mask to image

ImageSegment::ImageSegment() {
    memset(&m_envInfo, 0, sizeof(m_envInfo));
    memset(&m_procInfo, 0, sizeof(m_procInfo));
    memset(&m_workBufferInfo, 0, sizeof(m_workBufferInfo));
    memset(&m_userScribbles, 0, sizeof(m_userScribbles));
    memset(m_alphaMask, 0, sizeof(m_alphaMask));
    memset(m_objPoint, 0, sizeof(m_objPoint));
    memset(m_objRect, 0, sizeof(m_objRect));

    // add for debug
    if (access(SEGMENT_DUMP_PATH, 0) != -1) {
        // TODO new DebugHelper(SEGMENT_DUMP_PATH);
        m_pDebugHelper = new DebugHelper();
    }
}

ImageSegment::~ImageSegment() {
    LOGD("<release>");

    if (m_pImageSegment != NULL) {
        m_pImageSegment->Reset();
        m_pImageSegment->destroyInstance(m_pImageSegment);
        m_pImageSegment = NULL;
    }
    if (m_workBufferInfo.ext_mem_start_addr != NULL) {
        delete m_workBufferInfo.ext_mem_start_addr;
        m_workBufferInfo.ext_mem_start_addr = NULL;
    }
    if (NULL != m_pImageBuf) {
        delete[] m_pImageBuf;
        m_pImageBuf = NULL;
    }
    if (NULL != m_pDepthBuf) {
        delete[] m_pDepthBuf;
        m_pDepthBuf = NULL;
    }
    if (NULL != m_pOccBuf) {
        delete[] m_pOccBuf;
        m_pOccBuf = NULL;
    }
    for (MUINT32 i = 0; i < MAX_UNDO_NUM; i++) {
        m_alphaMask[i].release();
    }
    m_userScribbles.release();
    if (m_pDebugHelper != NULL) {
        delete m_pDebugHelper;
        m_pDebugHelper = NULL;
    }
    if (NULL != m_envInfo.face_pos) {
        delete[] m_envInfo.face_pos;
        m_envInfo.face_pos = NULL;
    }
    if (NULL != m_envInfo.face_rip) {
        delete[] m_envInfo.face_rip;
        m_envInfo.face_rip = NULL;
    }
}

bool ImageSegment::initialize(InitConfig* pConfig) {
    // TODO  LOGD
    if (pConfig == NULL) {
        LOGE("<initialize> fail, pConfig is null.\n");
        return false;
    }
    ImageBuf image = pConfig->bitmap;
    DepthBuf depth = pConfig->depth;
    MaskBuf mask = pConfig->mask;

    m_envInfo.debug_level = 0;
    m_envInfo.img_orientation = pConfig->imageOrientation;
    m_envInfo.mem_alignment = ALIGNMENT_BYTE;
    m_envInfo.tuning_para.alpha_mode = 4;
    m_envInfo.tuning_para.seg_ratio = 0.25f;

    initializeImageInfo(image);
    initializeDepthInfo(depth);
    initializeMaskInfo(mask);
    initializeFaceInfo(pConfig);

    if (m_pDebugHelper != NULL) {
        MCHAR name[FILE_NAME_LENGTH] = {'\0'};
        sprintf(name, "%s%dX%d_rgb_%s", SEGMENT_DUMP_PATH, image.width, image.height, "oriimage.raw");
        m_pDebugHelper->dumpBufferToFile(name, m_pImageBuf, image.width * image.height* 3);
        memset(name, 0x00, FILE_NAME_LENGTH);
        sprintf(name, "%s%dX%d_gray_%s", SEGMENT_DUMP_PATH, depth.depthWidth, depth.depthHeight, "depth.raw");
        m_pDebugHelper->dumpBufferToFile(name, m_pDepthBuf, depth.depthWidth * depth.depthHeight);
        memset(name, 0x00, FILE_NAME_LENGTH);
        sprintf(name, "%s%dX%d_gray_%s", SEGMENT_DUMP_PATH, depth.metaWidth, depth.metaHeight, "occ.raw");
        m_pDebugHelper->dumpBufferToFile(name, m_pOccBuf, depth.metaWidth * depth.metaHeight);
        if (mask.mask != NULL) {
            memset(name, 0x00, FILE_NAME_LENGTH);
            sprintf(name, "%s%dX%d_gray_%s", SEGMENT_DUMP_PATH, m_imageWidth, m_imageHeight, "mask_init.raw");
            m_pDebugHelper->dumpBufferToFile(name,
                m_alphaMask[m_editCurrIndex].ptr, m_imageWidth * m_imageHeight);
        }
    }

    m_pImageSegment = MTKImageSegment::createInstance(DRV_IMAGE_SEGMENT_OBJ_SW);
    m_pImageSegment->Init((void*) &m_envInfo, NULL);

    MUINT32 buffer_size;
    m_pImageSegment->FeatureCtrl(IMAGE_SEGMENT_FEATURE_GET_WORKBUF_SIZE, NULL, (void*) &buffer_size);
    m_workBufferInfo.ext_mem_start_addr = new MUINT8[buffer_size];
    if (m_workBufferInfo.ext_mem_start_addr == NULL) {
        LOGD("<initialize> Fail to allocate working buffer\n");
        return false;
    }
    LOGD("<initialize> mem_addr:%p,buffer_size:%d", m_workBufferInfo.ext_mem_start_addr, buffer_size);
    m_workBufferInfo.ext_mem_size = buffer_size;
    m_pImageSegment->FeatureCtrl(IMAGE_SEGMENT_FEATURE_SET_WORKBUF_INFO, (void*) &m_workBufferInfo, NULL);
    return true;
}

bool ImageSegment::doSegment(DoSegmentConfig* pConfig, MaskBuf* pMaskBuf) {
    if (pConfig == NULL) {
        LOGE("<doSegment> fail, pConfig is null.\n");
        return false;
    }
    MUINT32 scenario = pConfig->scenario;
    MUINT32 mode = pConfig->mode;
    MUINT8* scribbleBuf = pConfig->scribbleBuf;
    Rect roiRect = getRoiRect(pConfig);

    LOGD("<doSegment> scenario:%d,mode:%d,scribbleBuf:%p,roiRect.left:%d,top:%d,right:%d,bottom:%d", scenario,
            mode, scribbleBuf, roiRect.left, roiRect.top, roiRect.right, roiRect.bottom);
    if (NULL == m_pImageSegment) {
        LOGD("<doSegment> m_pImageSegment is null, fail!!!");
        return false;
    }

    switch (scenario) {
    case SCENARIO_AUTO:
        m_procInfo.scenario = SEGMENT_SCENARIO_AUTO;
        break;
    case SCENARIO_SELECTION:
        m_procInfo.scenario = SEGMENT_SCENARIO_SELECTION;
        break;
    case SCENARIO_SCRIBBLE_FG:
        m_procInfo.scenario = SEGMENT_SCENARIO_SCRIBBLE_FG;
        break;
    case SCENARIO_SCRIBBLE_BG:
        m_procInfo.scenario = SEGMENT_SCENARIO_SCRIBBLE_BG;
        break;
    default:
        break;
    }

    memset(m_userScribbles.ptr, SCRIBBLE_BACKGROUND, m_userScribbles.size);
    if ((SCENARIO_SCRIBBLE_FG == scenario || SCENARIO_SCRIBBLE_BG == scenario) && scribbleBuf != NULL) {
        MUINT8 *ptr = m_userScribbles.ptr;
        for (MUINT32 i = roiRect.top; i < roiRect.bottom; i++) {
            for (MUINT32 j = roiRect.left; j < roiRect.right; j++) {
                ptr[i * m_imageWidth + j] = scribbleBuf[(i * m_imageWidth + j) * 4];
            }
        }
    }

    if (mode == MODE_OBJECT) {
        m_procInfo.mode = SEGMENT_OBJECT;
    } else {
        m_procInfo.mode = SEGMENT_FOREGROUND;
    }

    if (NULL != m_pDebugHelper) {
        MCHAR name[FILE_NAME_LENGTH] = {'\0'};
        sprintf(name, "%s%dX%d_gray_%s", SEGMENT_DUMP_PATH, m_imageWidth, m_imageHeight, "scribble.raw");
        m_pDebugHelper->dumpBufferToFile(name,
            m_userScribbles.ptr, m_imageWidth * m_imageHeight);
    }

    // if m_isUndo is true means should calculate mask again, instead of using cached mask.
    m_procInfo.undo = m_isUndo ? 1 : 0;
    m_isUndo = false;

    m_procInfo.input_user_roi.left = roiRect.left;
    m_procInfo.input_user_roi.top = roiRect.top;
    m_procInfo.input_user_roi.right = roiRect.right;
    m_procInfo.input_user_roi.bottom = roiRect.bottom;

    m_procInfo.input_scribble_addr = m_userScribbles. ptr;
    m_procInfo.prev_output_mask_addr = m_alphaMask[m_editCurrIndex].ptr;

    LOGD("<doSegment> algorithm start");
    ATRACE_BEGIN(">>>>ImageSegment-doSegment");
    long startSec;
    long startUsec;
    m_stereoUtils.startMeasureTime(&startSec, &startUsec);
    m_pImageSegment->FeatureCtrl(IMAGE_SEGMENT_FEATURE_SET_PROC_INFO, (void*) &m_procInfo, NULL);
    MRESULT result = m_pImageSegment->Main();
    if (S_IMAGE_SEGMENT_OK != result) {
        if (scenario == SCENARIO_SCRIBBLE_BG && E_IMAGE_SEGMENT_NULL_OBJECT == result) {
            LOGD("<doSegment> scribble null object!!");
        } else {
            ATRACE_END();
            LOGD("<doSegment> ERROR: main fail,result:%d", result);
            return false;
        }
    }
    SEGMENT_CORE_RESULT_STRUCT segmentResult;
    m_pImageSegment->FeatureCtrl(IMAGE_SEGMENT_FEATURE_GET_RESULT, NULL, (void*) &segmentResult);
    LOGD("<doSegment> algorithm end");
    ATRACE_END();
    m_stereoUtils.endMeasureTime("<doSegment> algorithm", startSec, startUsec);

    m_editCurrIndex = (m_editCurrIndex + 1) % MAX_UNDO_NUM;
    memcpy(m_alphaMask[m_editCurrIndex].ptr,
        segmentResult.output_mask_addr, m_alphaMask[m_editCurrIndex].size);
    m_objPoint[m_editCurrIndex].x = segmentResult.center.x;
    m_objPoint[m_editCurrIndex].y = segmentResult.center.y;
    m_objRect[m_editCurrIndex].left = max(segmentResult.bbox.left, 0);
    m_objRect[m_editCurrIndex].top = max(segmentResult.bbox.top, 0);
    m_objRect[m_editCurrIndex].right = min(segmentResult.bbox.right, m_imageWidth);
    m_objRect[m_editCurrIndex].bottom = min(segmentResult.bbox.bottom, m_imageHeight);

    LOGD("<doSegment> rect.left:%d,top:%d,right:%d,bottom:%d", segmentResult.bbox.left,
        segmentResult.bbox.top, segmentResult.bbox.right, segmentResult.bbox.bottom);
    LOGD("<doSegment> center.x:%d,center.y:%d", segmentResult.center.x, segmentResult.center.y);
    if (NULL != m_pDebugHelper) {
        MCHAR name[FILE_NAME_LENGTH] = {'\0'};
        sprintf(name, "%s%dX%d_gray_%s", SEGMENT_DUMP_PATH, m_imageWidth, m_imageHeight, "mask_dosegment.raw");
        m_pDebugHelper->dumpBufferToFile(name, m_alphaMask[m_editCurrIndex].ptr,
            m_imageWidth * m_imageHeight);
    }
    getSegmentMask(pMaskBuf);
    return true;
}

bool ImageSegment::undoSegment(MaskBuf* pMaskBuf) {
    m_editCurrIndex = (m_editCurrIndex - 1 + MAX_UNDO_NUM) % MAX_UNDO_NUM;
    m_isUndo = true;
    getSegmentMask(pMaskBuf);
    return true;
}

bool ImageSegment::redoSegment(MaskBuf* pMaskBuf) {
    m_editCurrIndex = (m_editCurrIndex + 1) % MAX_UNDO_NUM;
    // TODO m_isUndo needs changing?
    getSegmentMask(pMaskBuf);
    return true;
}

bool ImageSegment::cutoutForgroundImg(BitmapMaskConfig* pConfig, MUINT8* newImg) {
    ImageBuf image = pConfig->bitmap;
    MaskBuf mask = pConfig->mask;

    Rect* rect = mask.rect;
    MUINT32 width = rect->right - rect->left;
    MUINT32 height = rect->bottom - rect->top;
    MUINT8* oriImg = image.buffer;
    MUINT32 oriWidth = image.width;
    ATRACE_BEGIN(">>>>cutoutForgroundImg");
    for (MUINT32 i = 0; i < height; i++) {
        memcpy((newImg + i * width * 4), (oriImg + ((i + rect->top) * oriWidth + rect->left) * 4), width * 4);
    }

    MUINT8* maskPtr = mask.mask;
    MUINT32 maskValue = 0;
    for (MUINT32 i = 0; i < height; i++) {
        for (MUINT32 j = 0; j < width; j++) {
            maskValue = maskPtr[(i + rect->top) * oriWidth + (j + rect->left)];
            newImg[(i * width + j) * 4 + 0] = newImg[(i * width + j) * 4 + 0] * maskValue / ALPHA;
            newImg[(i * width + j) * 4 + 1] = newImg[(i * width + j) * 4 + 1] * maskValue / ALPHA;
            newImg[(i * width + j) * 4 + 2] = newImg[(i * width + j) * 4 + 2] * maskValue / ALPHA;
            newImg[(i * width + j) * 4 + 3] = maskValue;
        }
    }
    if (NULL != m_pDebugHelper) {
        MCHAR name[FILE_NAME_LENGTH] = {'\0'};
        sprintf(name, "%s%dX%d_rgba_%s", SEGMENT_DUMP_PATH, width, height, "forgroundImg.raw");
        m_pDebugHelper->dumpBufferToFile(name, newImg, width * height * 4);
    }
    ATRACE_END();
    return true;
}

bool ImageSegment::scaleMask(BitmapMaskConfig* pConfig, MaskBuf* pMaskBuf) {
    ImageBuf image = pConfig->bitmap;
    MaskBuf mask = pConfig->mask;
    MUINT8* img = image.buffer;
    MUINT32 width = image.width;
    MUINT32 height = image.height;
    LOGD("<scaleMask> bitmap:%p,width:%d,height:%d", img, width, height);
    long startSec;
    long startUsec;
    m_stereoUtils.startMeasureTime(&startSec, &startUsec);

    if (width <= 0 || height <= 0) {
        LOGD("<scaleMask> ERROR: illegal image width and height!!!");
        return false;
    }

    if (NULL == m_pImageSegment) {
        LOGD("<scaleMask> ERROR: NULL m_pImageSegment object!!!!");
        return false;
    }
    ATRACE_BEGIN(">>>>ImageSegment-scaleMask");
    SEGMENT_CORE_SET_SAVE_INFO_STRUCT saveInfo;
    memset(&saveInfo, 0, sizeof(saveInfo));
    saveInfo.save_width = width;
    saveInfo.save_height = height;
    saveInfo.save_color_img_stride = width * 3;
    saveInfo.save_mask_stride = width;
    m_pImageSegment->FeatureCtrl(IMAGE_SEGMENT_FEATURE_SET_SAVE_INFO, &saveInfo, NULL);

    MUINT32 buffer_size;
    m_pImageSegment->FeatureCtrl(IMAGE_SEGMENT_FEATURE_GET_SAVE_WORKBUF_SIZE, NULL, (void*)&buffer_size);
    MUINT8* pSaveBuffer = new MUINT8[buffer_size];
    if (NULL == pSaveBuffer) {
        ATRACE_END();
        LOGD("<scaleMask> ERROR: allocate pSaveBuffer memory fail!!!");
        return false;
    }
    MUINT8* pImageBuffer = new MUINT8[width * height * 3];
    if (NULL == pImageBuffer) {
        LOGD("<scaleMask> ERROR: allocate pImageData memory fail!!!");
        delete[] pSaveBuffer;
        pSaveBuffer = NULL;
        ATRACE_END();
        return false;
    }
    for (MUINT32 i = 0; i < width * height; i++) {
        pImageBuffer[i * 3 + 0] = img[i * 4 + 0];
        pImageBuffer[i * 3 + 1] = img[i * 4 + 1];
        pImageBuffer[i * 3 + 2] = img[i * 4 + 2];
    }

    memset(&m_procInfo, 0, sizeof(m_procInfo));
    m_procInfo.scenario = SEGMENT_SCENARIO_SAVE;
    m_procInfo.input_color_img_addr = pImageBuffer;
    m_procInfo.prev_output_mask_addr = mask.mask;
    m_procInfo.working_buffer_addr = pSaveBuffer;
    // if m_isUndo is true means should calculate mask again, instead of using cached mask.
    m_procInfo.undo = 1;
    m_isUndo = false;
    LOGD("<scaleMask> m_procInfo.undo=%d", m_procInfo.undo);
    if (NULL != m_pDebugHelper) {
        MCHAR name[FILE_NAME_LENGTH] = {'\0'};
        sprintf(name, "%sgray_%s", SEGMENT_DUMP_PATH, "smallMask.raw");
        m_pDebugHelper->dumpBufferToFile(name, mask.mask, mask.bufferSize);
    }

    m_pImageSegment->FeatureCtrl(IMAGE_SEGMENT_FEATURE_SET_PROC_INFO, (void*)&m_procInfo, NULL);
    m_pImageSegment->Main();

    SEGMENT_CORE_RESULT_STRUCT segmentResult;
    m_pImageSegment->FeatureCtrl(IMAGE_SEGMENT_FEATURE_GET_RESULT, NULL, (void*)&segmentResult);
    ATRACE_END();
    if (NULL != m_pDebugHelper) {
        MCHAR name[FILE_NAME_LENGTH] = {'\0'};
        sprintf(name, "%s%dX%d_gray_%s", SEGMENT_DUMP_PATH, width, height, "newMask.raw");
        m_pDebugHelper->dumpBufferToFile(name,
                segmentResult.output_mask_addr, width * height);
    }

    getSegmentMask(pMaskBuf, &segmentResult, width * height);

    delete[] pSaveBuffer;
    delete[] pImageBuffer;
    pSaveBuffer = NULL;
    pImageBuffer = NULL;
    m_stereoUtils.endMeasureTime("<scaleMask>", startSec, startUsec);

    return true;
}

bool ImageSegment::scaleMaskWithoutInit(BitmapMaskConfig* pConfig, MaskBuf* pMaskBuf) {
    ImageBuf imageBuf = pConfig->bitmap;
    MaskBuf maskBuf = pConfig->mask;
    MUINT8* img = imageBuf.buffer;
    MUINT32 imgWidth = imageBuf.width;
    MUINT32 imgHeight = imageBuf.height;
    MUINT8* mask = maskBuf.mask;
    MUINT32 maskWidth = (maskBuf.rect)->right - (maskBuf.rect)->left;
    MUINT32 maskHeight = (maskBuf.rect)->bottom - (maskBuf.rect)->top;

    LOGD("<scaleMaskWithoutInit> img:%p,imgWidth:%d,imgHeight:%d,mask:%p,maskWidth:%d,maskHeight:%d",
            img, imgWidth, imgHeight, mask, maskWidth, maskHeight);
    long startSec;
    long startUsec;
    m_stereoUtils.startMeasureTime(&startSec, &startUsec);

    if (m_pImageSegment != NULL) {
        assert(false);
        return false;
    }
    if (NULL != m_pDebugHelper) {
        MCHAR name[FILE_NAME_LENGTH] = {'\0'};
        sprintf(name, "%sgray_%s", SEGMENT_DUMP_PATH, "oldMask_wo_init.raw");
        m_pDebugHelper->dumpBufferToFile(name, mask, maskWidth * maskHeight);
    }
    ATRACE_BEGIN(">>>>ImageSegment-scaleMaskWithoutInit");
    m_pImageSegment = m_pImageSegment->createInstance(DRV_IMAGE_SEGMENT_OBJ_SW);
    m_envInfo.mem_alignment = ALIGNMENT_BYTE;
    m_envInfo.input_color_img_width = maskWidth;
    m_envInfo.input_color_img_height = maskHeight;
    m_envInfo.output_mask_width = maskWidth;
    m_envInfo.output_mask_height = maskHeight;
    m_envInfo.output_mask_stride = (maskWidth + ALIGNMENT_BYTE - 1) / ALIGNMENT_BYTE * ALIGNMENT_BYTE;

    m_envInfo.save_width = imgWidth;
    m_envInfo.save_height = imgHeight;
    m_envInfo.save_color_img_stride = imgWidth * 3;
    m_envInfo.save_mask_stride = (imgWidth + ALIGNMENT_BYTE - 1) / ALIGNMENT_BYTE * ALIGNMENT_BYTE;

    m_envInfo.tuning_para.alpha_mode = 4;
    m_envInfo.tuning_para.seg_ratio = 0.25f;

    m_pImageSegment->Init((void*)&m_envInfo, NULL);

    // get buffer size
    MUINT32 buffer_size;
    m_pImageSegment->FeatureCtrl(IMAGE_SEGMENT_FEATURE_GET_WORKBUF_SIZE, NULL, (void*)&buffer_size);
    if (m_workBufferInfo.ext_mem_start_addr != NULL) {
        delete m_workBufferInfo.ext_mem_start_addr;
        m_workBufferInfo.ext_mem_start_addr = NULL;
    }
    m_workBufferInfo.ext_mem_start_addr = new MUINT8[buffer_size];
    if (m_workBufferInfo.ext_mem_start_addr == NULL) {
        LOGD("<scaleMaskWithoutInit> ERROR: allocate m_workBufferInfo.ext_mem_start_addr memory fail!!!");
        ATRACE_END();
        return false;
    }
    m_workBufferInfo.ext_mem_size = buffer_size;
    m_pImageSegment->FeatureCtrl(IMAGE_SEGMENT_FEATURE_SET_WORKBUF_INFO, (void*)&m_workBufferInfo, NULL);

    m_pImageSegment->FeatureCtrl(IMAGE_SEGMENT_FEATURE_GET_SAVE_WORKBUF_SIZE, NULL, (void*)&buffer_size);
    MUINT8* pSaveBuffer = new MUINT8[buffer_size];
    if (NULL == pSaveBuffer) {
        LOGD("<scaleMaskWithoutInit> ERROR: allocate pSaveBuffer memory fail!!!");
        ATRACE_END();
        return false;
    }
    MUINT8* pImageBuffer = new MUINT8[imgWidth * imgHeight * 3];
    if (NULL == pImageBuffer) {
        LOGD("<scaleMaskWithoutInit> ERROR: allocate pImageData memory fail!!!");
        delete[] pSaveBuffer;
        pSaveBuffer = NULL;
        ATRACE_END();
        return false;
    }
    for (MUINT32 i = 0; i < imgWidth * imgHeight; i++) {
        pImageBuffer[i * 3 + 0] = img[i * 4 + 0];
        pImageBuffer[i * 3 + 1] = img[i * 4 + 1];
        pImageBuffer[i * 3 + 2] = img[i * 4 + 2];
    }
    memset(&m_procInfo, 0, sizeof(m_procInfo));
    m_procInfo.scenario = SEGMENT_SCENARIO_SAVE;
    m_procInfo.input_color_img_addr = pImageBuffer;
    m_procInfo.prev_output_mask_addr = mask;
    m_procInfo.working_buffer_addr = pSaveBuffer;

    m_pImageSegment->FeatureCtrl(IMAGE_SEGMENT_FEATURE_SET_PROC_INFO, (void*)&m_procInfo, NULL);
    m_pImageSegment->Main();

    SEGMENT_CORE_RESULT_STRUCT segmentResult;
    m_pImageSegment->FeatureCtrl(IMAGE_SEGMENT_FEATURE_GET_RESULT, NULL, (void*)&segmentResult);
    getSegmentMask(pMaskBuf, &segmentResult, imgWidth * imgHeight);
    ATRACE_END();
    delete[] pSaveBuffer;
    delete[] pImageBuffer;

    if (NULL != m_pDebugHelper) {
        MCHAR name[FILE_NAME_LENGTH] = {'\0'};
        sprintf(name, "%s%dX%d_gray_%s", SEGMENT_DUMP_PATH, imgWidth, imgHeight, "newMask_wo_init.raw");
        m_pDebugHelper->dumpBufferToFile(name, pMaskBuf->mask, pMaskBuf->bufferSize);
    }
    m_stereoUtils.endMeasureTime("<scaleMaskWithoutInit>", startSec, startUsec);

    return true;
}

bool ImageSegment::fillCoverImg(MUINT8* coverImg) {
    LOGD("<fillCoverImg>");
    ATRACE_BEGIN(">>>>fillCoverImg");
    Rect rect = m_objRect[m_editCurrIndex];
    LOGD("<fillCoverImg> left:%d,top:%d,right:%d,bottom:%d", rect.left, rect.top, rect.right, rect.bottom);
    for (int i = rect.top; i <= rect.bottom; i++) {
        for (int j = rect.left; j <= rect.right; j++) {
            coverImg[i * m_imageWidth + j]
                     = (float) (ALPHA - m_alphaMask[m_editCurrIndex].ptr[i * m_imageWidth + j])
                     / ALPHA * COVER;
        }
    }

    if (NULL != m_pDebugHelper) {
        MCHAR name[FILE_NAME_LENGTH] = {'\0'};
        sprintf(name, "%s%dX%d_alpha_%s", SEGMENT_DUMP_PATH, m_imageWidth, m_imageHeight, "coverimg.raw");
        m_pDebugHelper->dumpBufferToFile(name, coverImg, m_imageWidth * m_imageHeight);
    }
    ATRACE_END();
    return true;
}

stereo::Rect ImageSegment::getRoiRect(DoSegmentConfig* pConfig) {
    Rect rect;
    rect.left = 0;
    rect.top = 0;
    rect.right = 0;
    rect.bottom = 0;
    if (pConfig == NULL) {
        LOGE("<getRoiRect> fail, pConfig is null.\n");
        return rect;
    }

    MUINT32 scenario = pConfig->scenario;
    Rect roiRect = pConfig->roiRect;
    Point point = pConfig->selectPoint;

    MINT32 left = 0;
    MINT32 top = 0;
    MINT32 right = m_imageWidth - 1;
    MINT32 bottom = m_imageHeight - 1;

    if (scenario == SCENARIO_SELECTION) {
        left = point.x - ROI_MARGIN;
        top = point.y - ROI_MARGIN;
        right = point.x + ROI_MARGIN;
        bottom = point.y + ROI_MARGIN;

        if (left < 0) {
            right = right - left;
            left = 0;
        }
        if (right > (m_imageWidth - 1)) {
            left = left - (right - (m_imageWidth - 1));
            right = m_imageWidth - 1;
        }

        if (top < 0) {
            bottom = bottom - top;
            top = 0;
        }
        if (bottom > m_imageHeight - 1) {
            top = top - (bottom - (m_imageHeight - 1));
            bottom = m_imageHeight - 1;
        }
        rect.left = left;
        rect.top = top;
        rect.right = right;
        rect.bottom = bottom;
    } else if (scenario == SCENARIO_SCRIBBLE_FG || scenario == SCENARIO_SCRIBBLE_BG) {
        rect.left = roiRect.left;
        rect.top = roiRect.top;
        rect.right = roiRect.right;
        rect.bottom = roiRect.bottom;
    }
    return rect;
}

void ImageSegment::getSegmentMask(MaskBuf *ptoMaskBuf,
        SEGMENT_CORE_RESULT_STRUCT *pfromResult, MUINT32 bufferSize) {
    memcpy(ptoMaskBuf->mask, pfromResult->output_mask_addr, bufferSize);
    Point* pMaskPoint = new Point;
    pMaskPoint->x = pfromResult->center.x;
    pMaskPoint->y = pfromResult->center.y;
    ptoMaskBuf->point = pMaskPoint;
    Rect* pMaskRect = new Rect;
    pMaskRect->left = pfromResult->bbox.left;
    pMaskRect->top = pfromResult->bbox.top;
    pMaskRect->right = pfromResult->bbox.right;
    pMaskRect->bottom = pfromResult->bbox.bottom;
    ptoMaskBuf->rect = pMaskRect;
    ptoMaskBuf->bufferSize = bufferSize;
}

void ImageSegment::getSegmentMask(MaskBuf *ptoMaskBuf) {
        LOGD("<getSegmentMask>......");
        ptoMaskBuf->mask = m_alphaMask[m_editCurrIndex].ptr;
        ptoMaskBuf->point = m_objPoint + m_editCurrIndex;
        ptoMaskBuf->rect = m_objRect + m_editCurrIndex;
        ptoMaskBuf->bufferSize = m_alphaMask[m_editCurrIndex].size;
}

void ImageSegment::initializeImageInfo(ImageBuf &image) {
    m_imageWidth = image.width;
    m_imageHeight = image.height;
    m_pImageBuf = new MUINT8[m_imageWidth * m_imageHeight * 3];
    for (MUINT32 i = 0; i < m_imageWidth * m_imageHeight; i++) {
        m_pImageBuf[i * 3 + 0] = image.buffer[i * 4 + 0];
        m_pImageBuf[i * 3 + 1] = image.buffer[i * 4 + 1];
        m_pImageBuf[i * 3 + 2] = image.buffer[i * 4 + 2];
    }
    m_envInfo.input_color_img_width = m_imageWidth;
    m_envInfo.input_color_img_height = m_imageHeight;
    m_envInfo.input_color_img_stride = m_imageWidth * 3;
    m_envInfo.input_color_img_addr = m_pImageBuf;
}

void ImageSegment::initializeDepthInfo(DepthBuf &depth) {
    DepthParser pParser(&depth);
    m_pDepthBuf = pParser.getDepthMap();
    m_pOccBuf = pParser.getOccMap();
    m_envInfo.input_depth_img_width = depth.depthWidth;
    m_envInfo.input_depth_img_height = depth.depthHeight;
    m_envInfo.input_depth_img_stride = depth.depthWidth;
    m_envInfo.input_depth_img_addr = m_pDepthBuf;
    m_envInfo.input_occ_img_width = depth.metaWidth;
    m_envInfo.input_occ_img_height = depth.metaHeight;
    m_envInfo.input_occ_img_stride = depth.metaWidth;
    m_envInfo.input_occ_img_addr = m_pOccBuf;
}

void ImageSegment::initializeMaskInfo(MaskBuf &mask) {
    m_userScribbles.alloc(m_imageWidth, m_imageHeight, 1);
    for (MUINT32 i = 0; i < MAX_UNDO_NUM; i++) {
        m_alphaMask[i].alloc(m_imageWidth, m_imageHeight, 1);
    }
    if (mask.mask!= NULL) {
        memcpy(m_alphaMask[m_editCurrIndex].ptr, mask.mask, m_alphaMask[m_editCurrIndex].size);
        m_objPoint[m_editCurrIndex].x = (mask.point)->x;
        m_objPoint[m_editCurrIndex].y = (mask.point)->y;

        m_objRect[m_editCurrIndex].left = (mask.rect)->left;
        m_objRect[m_editCurrIndex].top = (mask.rect)->top;
        m_objRect[m_editCurrIndex].right = (mask.rect)->right;
        m_objRect[m_editCurrIndex].bottom = (mask.rect)->bottom;
    }
    m_envInfo.input_scribble_width = m_userScribbles.width;
    m_envInfo.input_scribble_height = m_userScribbles.height;
    m_envInfo.input_scribble_stride = m_userScribbles.stride;
    m_envInfo.output_mask_width = m_alphaMask[m_editCurrIndex].width;
    m_envInfo.output_mask_height = m_alphaMask[m_editCurrIndex].height;
    m_envInfo.output_mask_stride = m_alphaMask[m_editCurrIndex].stride;
}

void ImageSegment::initializeFaceInfo(InitConfig* pConfig) {
    if (pConfig->faceNum > 0) {
        // TODO maybe faceRect & faceRip should be allocated in this class
        m_envInfo.face_num = pConfig->faceNum;
        m_envInfo.face_pos = (::Rect *) malloc(sizeof(::Rect) * pConfig->faceNum);
        m_envInfo.face_rip = (MINT32 *) malloc(sizeof(MINT32) * pConfig->faceNum);
        const Rect* pfaceRect = pConfig->faceRect;
        const MINT32* pfaceRip = pConfig->faceRip;
        for (MUINT32 i = 0; i < pConfig->faceNum; i++) {
            m_envInfo.face_pos[i].left = pfaceRect[i].left;
            m_envInfo.face_pos[i].top = pfaceRect[i].top;
            m_envInfo.face_pos[i].right = pfaceRect[i].right;
            m_envInfo.face_pos[i].bottom = pfaceRect[i].bottom;
            m_envInfo.face_rip[i] = pfaceRip[i];
        }
    } else {
        m_envInfo.face_num = 0;
    }
}

