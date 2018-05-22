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
#ifndef ADVANCEDFANCYCOLOR_H_
#define ADVANCEDFANCYCOLOR_H_

#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#include "Log.h"
#include "IFancyColor.h"
#include "MTKFancyColor.h"
#include "StereoType.h"
#include "StereoStruct.h"
#include "../DebugHelper.h"
#include "StereoUtils.h"
#include "utils/Mutex.h"

#define MIN(a, b) (a < b ? a : b)
#define MAX(a, b) (a > b ? a : b)
#define STROKE_DATA 255

#define FANCYCOLOR_DUMP_PATH "/storage/sdcard0/fancycolor/"

namespace stereo {

class AdvancedFancyColor : public IFancyColor {
public:
    AdvancedFancyColor();
    virtual ~AdvancedFancyColor();

    bool generateEffectImg(JNIEnv *env, jobject thiz, jobject config, jobject result,
            GenerateEffectImgConfig* pConfig, MCHAR* pResultBitmap);

    vector<MCHAR*> getAllEffectsNames();

private:
    bool imageFilterStroke(GenerateEffectImgConfig* pConfig, MCHAR* pResultBitmap);

    bool imageFilterRadialBlur(GenerateEffectImgConfig* pConfig, MCHAR* pResultBitmap);

    class MyRenderer {
    public:
        MyRenderer();
        virtual ~MyRenderer();

        bool init(MUINT8* color_img, MUINT8 *alpha_mask, Rect rect, MINT32 img_width, MINT32 img_height,
                MINT32 center_x, MINT32 center_y);

        bool doStrokeEffect(MCHAR *bitmap, MUINT8 *alpha_mask);
        bool doRadialBlurEffect(MCHAR *bitmap);

        void release();

    private:
        void freeMem(MUINT8 *addr);

        FANCY_CORE_SET_ENV_INFO_STRUCT mFancyEnvInfo;
        FANCY_CORE_SET_WORK_BUF_INFO_STRUCT mFancyWorkBufferInfo;
        FANCY_CORE_SET_PROC_INFO_STRUCT mFancyProcInfo;
        FANCY_CORE_RESULT_STRUCT mFancyResult;

        MTKFancyColor *mFancyColor = NULL;
        MUINT8 *m_pImageData = NULL;
        MINT32 mImgWidth = 0;
        MINT32 mImgHeight = 0;

        // debug
        DebugHelper *m_pDebugHelper = NULL;
    };  // inner class MyRenderer

    MCHAR *IMAGE_FILTER_RADIAL_BLUR = "imageFilterRadialBlur";
    MCHAR *IMAGE_FILTER_STOKE = "imageFilterStroke";

    vector<MCHAR*> mEffectNames;

    MyRenderer *m_pStrokeRender = NULL;
    MyRenderer *m_pBlurRender = NULL;
    android::Mutex m_mutex_radialBlur;
    android::Mutex m_mutex_stroke;
    StereoUtils m_stereoUtils;
};  // class AdvancedFancyColor
}  // namespace stereo

#endif /* ADVANCEDFANCYCOLOR_H_ */
