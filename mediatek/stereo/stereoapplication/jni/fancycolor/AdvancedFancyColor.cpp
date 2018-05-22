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

#include "AdvancedFancyColor.h"

using namespace stereo;

#define TAG "Fc/AdvancedFancyColor"

AdvancedFancyColor::AdvancedFancyColor() {
    m_pStrokeRender = new MyRenderer();
    m_pBlurRender = new MyRenderer();
}

AdvancedFancyColor::~AdvancedFancyColor() {
    delete m_pStrokeRender;
    delete m_pBlurRender;
}

bool AdvancedFancyColor::generateEffectImg(JNIEnv *env, jobject thiz, jobject config, jobject result,
        GenerateEffectImgConfig* pConfig, MCHAR* pResultBitmap) {
    if (pConfig == NULL) {
        LOGD("<generateEffectImg> pConfig is null, do nothing.");
        return false;
    }

    bool res = false;
    MCHAR* effectName = pConfig->effectName;
    if (strcmp(effectName, IMAGE_FILTER_RADIAL_BLUR) == 0) {
        res = imageFilterRadialBlur(pConfig, pResultBitmap);
    } else if (strcmp(effectName, IMAGE_FILTER_STOKE) == 0) {
        res = imageFilterStroke(pConfig, pResultBitmap);
    }
    return res;
}

vector<MCHAR*> AdvancedFancyColor::getAllEffectsNames() {
    if (mEffectNames.empty()) {
        mEffectNames.push_back(IMAGE_FILTER_RADIAL_BLUR);
        mEffectNames.push_back(IMAGE_FILTER_STOKE);
    }
    return mEffectNames;
}

bool AdvancedFancyColor::imageFilterStroke(GenerateEffectImgConfig* pConfig, MCHAR* pResultBitmap) {
    LOGD("<imageFilterStroke>");
    long startSec;
    long startUsec;
    m_stereoUtils.startMeasureTime(&startSec, &startUsec);

    android::Mutex::Autolock autolock(m_mutex_stroke);
    MINT32 width = pConfig->bitmap.width;
    MINT32 height = pConfig->bitmap.height;
    bool result = m_pStrokeRender->init(pConfig->bitmap.buffer, pConfig->mask.mask,
            *(pConfig->mask.rect), width, height, pConfig->mask.point->x, pConfig->mask.point->y);

    if (!result) {
        LOGD("<imageFilterStroke> init fail");
        return false;
    }
    result = m_pStrokeRender->doStrokeEffect(pResultBitmap, pConfig->mask.mask);

    if (!result) {
        LOGD("<imageFilterStroke> doStroke fail");
        return false;
    }
    m_pStrokeRender->release();     // TODO seems strange
    m_stereoUtils.endMeasureTime("Fc/imageFilterStroke()", startSec, startUsec);
    return true;
}

bool AdvancedFancyColor::imageFilterRadialBlur(GenerateEffectImgConfig* pConfig, MCHAR* pResultBitmap) {
    LOGD("<imageFilterRadialBlur>");
    long startSec;
    long startUsec;
    m_stereoUtils.startMeasureTime(&startSec, &startUsec);

    android::Mutex::Autolock autolock(m_mutex_radialBlur);
    MUINT32 width = pConfig->bitmap.width;
    MUINT32 height = pConfig->bitmap.height;
    bool result = m_pBlurRender->init(pConfig->bitmap.buffer, pConfig->mask.mask, *(pConfig->mask.rect), width,
                height, pConfig->mask.point->x, pConfig->mask.point->y);

    if (!result) {
        LOGD("<imageFilterStroke> init fail");
        return false;
    }
    result = m_pBlurRender->doRadialBlurEffect(pResultBitmap);

    if (!result) {
        LOGD("<imageFilterStroke> doStroke fail");
        return false;
    }
    m_pBlurRender->release();   // TODO seems strange
    m_stereoUtils.endMeasureTime("Fc/imageFilterRadialBlur()", startSec, startUsec);
    return true;
}

AdvancedFancyColor::MyRenderer::MyRenderer() {
    memset(&mFancyEnvInfo, 0, sizeof(mFancyEnvInfo));
    memset(&mFancyWorkBufferInfo, 0, sizeof(mFancyWorkBufferInfo));
    memset(&mFancyProcInfo, 0, sizeof(mFancyProcInfo));
    memset(&mFancyResult, 0, sizeof(mFancyResult));
    // add for debug
    if (access(FANCYCOLOR_DUMP_PATH, 0) != -1) {
        m_pDebugHelper = new DebugHelper;   // new DebugUtils(FANCYCOLOR_DUMP_PATH);
    }
}

AdvancedFancyColor::MyRenderer::~MyRenderer() {
    if (NULL != m_pDebugHelper) {
        delete m_pDebugHelper;
        m_pDebugHelper = NULL;
    }
}

bool AdvancedFancyColor::MyRenderer::init(MUINT8 *color_img, MUINT8 *alpha_mask, Rect rect,
        MINT32 img_width, MINT32 img_height, MINT32 center_x, MINT32 center_y) {
    LOGD("<init> img_width:%d, img_height:%d,center_x:%d,center_y:%d", img_width, img_height, center_x, center_y);
    mFancyColor = mFancyColor->createInstance(DRV_FANCY_COLOR_OBJ_SW);
    mFancyProcInfo.rect_in.left = rect.left;
    mFancyProcInfo.rect_in.top = rect.top;
    mFancyProcInfo.rect_in.right = rect.right;
    mFancyProcInfo.rect_in.bottom = rect.bottom;
    mImgWidth = img_width;
    mImgHeight = img_height;

    m_pImageData = new MUINT8[img_width * img_height * 3];
    for (MINT32 i = 0; i < img_width * img_height; i++) {
        m_pImageData[i * 3 + 0] = color_img[i * 4 + 0];
        m_pImageData[i * 3 + 1] = color_img[i * 4 + 1];
        m_pImageData[i * 3 + 2] = color_img[i * 4 + 2];
    }

    mFancyEnvInfo.input_color_img_addr = m_pImageData;
    mFancyEnvInfo.input_color_img_height = img_height;
    mFancyEnvInfo.input_color_img_width = img_width;
    mFancyEnvInfo.input_color_img_stride = img_width * 3;
    mFancyEnvInfo.input_alpha_mask_addr = alpha_mask;
    mFancyEnvInfo.input_alpha_mask_height = img_height;
    mFancyEnvInfo.input_alpha_mask_width = img_width;
    mFancyEnvInfo.input_alpha_mask_stride = img_width;
    mFancyEnvInfo.center_x = center_x;
    mFancyEnvInfo.center_y = center_y;

    mFancyColor->Init((void*) &mFancyEnvInfo, NULL);

    MUINT32 buffer_size;
    mFancyColor->FeatureCtrl(FANCY_COLOR_FEATURE_GET_WORKBUF_SIZE, NULL, (void *) &buffer_size);
    LOGD("[FANCY_COLOR_FEATURE_GET_WORKBUF_SIZE] buffer_size: %d", buffer_size);
    mFancyWorkBufferInfo.ext_mem_start_addr = new MUINT8[buffer_size];
    if (mFancyWorkBufferInfo.ext_mem_start_addr == 0) {
        LOGD("[ERROR] Fail to allocate fancy color working buffer");
        return false;
    }
    mFancyWorkBufferInfo.ext_mem_size = buffer_size;

    LOGD("allocate fancy color working buffer size %d, address:%p", buffer_size,
            mFancyWorkBufferInfo.ext_mem_start_addr);

    mFancyColor->FeatureCtrl(FANCY_COLOR_FEATURE_SET_WORKBUF_INFO, (void*) &mFancyWorkBufferInfo, NULL);

    return true;
}

bool AdvancedFancyColor::MyRenderer::doStrokeEffect(MCHAR *bitmap, MUINT8 *alpha_mask) {
    mFancyProcInfo.color_effect = FANCY_COLOR_EFFECT_STROKE;
    if (S_FANCY_COLOR_OK !=
        mFancyColor->FeatureCtrl(FANCY_COLOR_FEATURE_SET_PROC_INFO, (void*) &mFancyProcInfo, NULL)) {
        LOGD("<doStrokeEffect> do FANCY_COLOR_FEATURE_SET_PROC_INFO fail!");
    }
    if (S_FANCY_COLOR_OK != mFancyColor->Main()) {
        LOGD("<doStrokeEffect> do Main() fail!");
    }
    if (S_FANCY_COLOR_OK !=
        mFancyColor->FeatureCtrl(FANCY_COLOR_FEATURE_GET_RESULT, NULL, (void*) &mFancyResult)) {
        LOGD("<doStrokeEffect> do FANCY_COLOR_FEATURE_GET_RESULT fail!");
    }
    LOGD("<doStrokeEffect> rect_out left-top-right-bottom: %d-%d-%d-%d", mFancyResult.rect_out.left,
            mFancyResult.rect_out.top, mFancyResult.rect_out.right, mFancyResult.rect_out.bottom);

    MUINT8 *resultMask = mFancyResult.output_mask_addr;
    if (NULL != m_pDebugHelper) {
        m_pDebugHelper->dumpBufferToFile("StrokeIutputImage.raw", m_pImageData, mImgWidth * mImgHeight * 3);
        m_pDebugHelper->dumpBufferToFile("StrokeOutputMask.raw", resultMask, mImgWidth * mImgHeight);
    }

    for (MINT32 i = mFancyResult.rect_out.top; i < mFancyResult.rect_out.bottom; i++) {
        for (MINT32 j = mFancyResult.rect_out.left; j < mFancyResult.rect_out.right; j++) {
            MINT32 inputMaskData = alpha_mask[i * mImgWidth + j];
            MINT32 outputMaskData = resultMask[i * mImgWidth + j];

            if (inputMaskData == 0 && outputMaskData != 0) {
                memset(&bitmap[(i * mImgWidth + j) * 4], STROKE_DATA, 3);
            } else if (inputMaskData != 0 && inputMaskData != 255) {
                bitmap[(i * mImgWidth + j) * 4] = bitmap[(i * mImgWidth + j) * 4] * (inputMaskData / 255.0)
                        + STROKE_DATA * (1 - inputMaskData / 255.0);
                bitmap[(i * mImgWidth + j) * 4 + 1] = bitmap[(i * mImgWidth + j) * 4 + 1] * (inputMaskData / 255.0)
                        + STROKE_DATA * (1 - inputMaskData / 255.0);
                bitmap[(i * mImgWidth + j) * 4 + 2] = bitmap[(i * mImgWidth + j) * 4 + 2] * (inputMaskData / 255.0)
                        + STROKE_DATA * (1 - inputMaskData / 255.0);
            }
        }
    }

    return true;
}

bool AdvancedFancyColor::MyRenderer::doRadialBlurEffect(MCHAR *bitmap) {
    mFancyProcInfo.color_effect = FANCY_COLOR_EFFECT_RADIAL_BLUR;
    if (S_FANCY_COLOR_OK !=
        mFancyColor->FeatureCtrl(FANCY_COLOR_FEATURE_SET_PROC_INFO, (void*) &mFancyProcInfo, NULL)) {
        LOGD("<doRadialBlurEffect> do FANCY_COLOR_FEATURE_SET_PROC_INFO fail!");
    }
    if (S_FANCY_COLOR_OK != mFancyColor->Main()) {
        LOGD("<doRadialBlurEffect> do Main() fail!");
    }
    if (S_FANCY_COLOR_OK !=
        mFancyColor->FeatureCtrl(FANCY_COLOR_FEATURE_GET_RESULT, NULL, (void*) &mFancyResult)) {
        LOGD("<doRadialBlurEffect> do FANCY_COLOR_FEATURE_GET_RESULT fail!");
    }
    LOGD("<doRadialBlurEffect> rect_out left-top-right-bottom: %d-%d-%d-%d", mFancyResult.rect_out.left,
            mFancyResult.rect_out.top, mFancyResult.rect_out.right, mFancyResult.rect_out.bottom);

    MUINT8 *resultMask = mFancyResult.output_mask_addr;
    MUINT8 *resultImage = mFancyResult.output_img_addr;
    if (NULL != m_pDebugHelper) {
        m_pDebugHelper->dumpBufferToFile("RadialIutputImg.raw", m_pImageData, mImgWidth * mImgHeight * 3);
        m_pDebugHelper->dumpBufferToFile("RadialOutputImg.raw", resultImage, mImgWidth * mImgHeight * 3);
        m_pDebugHelper->dumpBufferToFile("RadialOutputMask.raw", resultMask, mImgWidth * mImgHeight);
    }

    for (MINT32 i = 0; i < mImgHeight; i++) {
        for (MINT32 j = 0; j < mImgWidth; j++) {
            memcpy(&bitmap[(i * mImgWidth + j) * 4], &resultImage[(i * mImgWidth + j) * 3], 3);
        }
    }

    for (MINT32 i = mFancyResult.rect_out.top; i < mFancyResult.rect_out.bottom; i++) {
        for (MINT32 j = mFancyResult.rect_out.left; j < mFancyResult.rect_out.right; j++) {
            if (resultMask[i * mImgWidth + j] != 0) {
                bitmap[(i * mImgWidth + j) * 4] = bitmap[(i * mImgWidth + j) * 4]
                        * (resultMask[i * mImgWidth + j] / 255.0)
                        + bitmap[(i * mImgWidth + j) * 4] * (1 - resultMask[i * mImgWidth + j] / 255.0);
                bitmap[(i * mImgWidth + j) * 4 + 1] = bitmap[(i * mImgWidth + j) * 4 + 1]
                        * (resultMask[i * mImgWidth + j] / 255.0)
                        + bitmap[(i * mImgWidth + j) * 4 + 1] * (1 - resultMask[i * mImgWidth + j] / 255.0);
                bitmap[(i * mImgWidth + j) * 4 + 2] = bitmap[(i * mImgWidth + j) * 4 + 2]
                        * (resultMask[i * mImgWidth + j] / 255.0)
                        + bitmap[(i * mImgWidth + j) * 4 + 2] * (1 - resultMask[i * mImgWidth + j] / 255.0);
            }
        }
    }

    return true;
}

void AdvancedFancyColor::MyRenderer::release() {
    LOGD("<release>");
    if (mFancyColor != NULL) {
        mFancyColor->Reset();
        mFancyColor->destroyInstance(mFancyColor);
        mFancyColor = NULL;
    }
    freeMem(m_pImageData);
    freeMem(mFancyWorkBufferInfo.ext_mem_start_addr);
}

void AdvancedFancyColor::MyRenderer::freeMem(MUINT8* addr) {
    if (addr != NULL) {
        delete addr;
        addr = NULL;
    }
}
