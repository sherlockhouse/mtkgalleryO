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

#include "RefocusConfigInfoWrapper.h"
#include "camera_custom_stereo_tuning.h"
#include "MdpCallbackFunc.h"

#define TAG "Rf/RefocusConfigInfoWrapper"

using namespace stereo;

RefocusConfigInfoWrapper::RefocusConfigInfoWrapper()
{
    LOGD("<RefocusConfigInfoWrapper><comp> new RefocusConfigInfoWrapper (v3)");
    #ifdef REFOCUS_CUSTOM_PARAM
    if (p_mProvider == NULL) {
        p_mProvider = new TuningInfoProvider(DEFAULT_STEREO_TUNING);
    }
    #endif
}

RefocusConfigInfoWrapper::~RefocusConfigInfoWrapper()
{
    LOGD("<~RefocusConfigInfoWrapper><comp> RefocusConfigInfoWrapper (v3)");
    #ifdef REFOCUS_CUSTOM_PARAM
    for (int i = 0; i < mTuningInfo.size(); i++) {
        delete[] mTuningInfo[i].first;
    }
    mTuningInfo.clear();
    LOGD("<~RefocusConfigInfoWrapper><comp> clear mTuningInfo");
    mClearTable.clear();
    LOGD("<~RefocusConfigInfoWrapper><comp> clear mClearTable");
    delete p_mProvider;
    #endif
}

void RefocusConfigInfoWrapper::prepareRefocusTuningInfo(RefocusTuningInfo* p_tuningInfo,
        RefocusInitConfig* p_config)
{
    if (p_tuningInfo == NULL)
    {
        LOGD("<prepareRefocusTuningInfo><comp> p_tuningInfo invalid!!");
        return;
    }
    ImageRefocusPerf RefocusPerf;
    p_tuningInfo->HorzDownSampleRatio = 4;
    p_tuningInfo->VertDownSampleRatio = 4;
    p_tuningInfo->IterationTimes = 3;
    p_tuningInfo->InterpolationMode = 0;
    p_tuningInfo->CoreNumber = 4;
    p_tuningInfo->NumOfExecution = 1;
    p_tuningInfo->Baseline = 2.0f;
    p_tuningInfo->RFCoreNumber[0] = RefocusPerf.getCpuCoreNumOfCluster(0);
    p_tuningInfo->RFCoreNumber[1] = RefocusPerf.getCpuCoreNumOfCluster(1);
    p_tuningInfo->RFCoreNumber[2] = RefocusPerf.getCpuCoreNumOfCluster(2);
    LOGD("<prepareRefocusTuningInfo><comp> (v3), RFCoreNumber: %d, %d, %d",
            p_tuningInfo->RFCoreNumber[0], p_tuningInfo->RFCoreNumber[1],
            p_tuningInfo->RFCoreNumber[2]);
    #ifdef REFOCUS_CUSTOM_PARAM
    p_mProvider->getTuningInfo(mTuningInfo);
    if (mTuningInfo.empty()) {
        LOGD("<prepareRefocusTuningInfo><comp> mTuningInfo is empty");
        return;
    }
    p_tuningInfo->NumOfParam = mTuningInfo.size();
    p_tuningInfo->params = (RefocusTuningParam *) &mTuningInfo[0];
    for (int i = 0; i < mTuningInfo.size(); i++) {
        LOGD("<prepareRefocusTuningInfo><tuningparams> %s, %d", mTuningInfo[i].first,
                mTuningInfo[i].second);
    }
    #endif
}

void RefocusConfigInfoWrapper::prepareRefocusImageInfo(RefocusImageInfo* p_imageInfo,
        RefocusInitConfig* p_config)
{
    if (p_imageInfo == NULL || p_config == NULL)
    {
        LOGD("<prepareRefocusImageInfo><comp> p_imageInfo or p_config invalid!!");
        return;
    }
    LOGD("<prepareRefocusImageInfo><comp> RefocusConfigInfoWrapper (v3)");

    #ifdef REFOCUS_CUSTOM_PARAM
    p_mProvider->getClearTable(mClearTable);
    if (!mClearTable.empty()) {
        int tableSize = mClearTable.size();
        p_imageInfo->dacInfo.clrTblSize = tableSize;
        delete[] p_imageInfo->dacInfo.clrTbl;
        p_imageInfo->dacInfo.clrTbl = new MINT32[tableSize];
        memcpy(p_imageInfo->dacInfo.clrTbl, &mClearTable[0], sizeof(int) * tableSize);
        for (int i = 0; i < tableSize; i++) {
            LOGD("<prepareRefocusImageInfo><tuningparams> mClearTable[%d], %d", i,
                *(p_imageInfo->dacInfo.clrTbl + i));
        }
    } else {
        LOGD("<prepareRefocusImageInfo><comp> mClearTable is empty");
    }
    #endif

    p_imageInfo->COffset = p_config->convOffset;
    p_imageInfo->afInfo.afType = (REFOCUS_AF_TYPE_ENUM) p_config->focusType;
    p_imageInfo->afInfo.x1 = p_config->focusLeft;
    p_imageInfo->afInfo.y1 = p_config->focusTop;
    p_imageInfo->afInfo.x2 = p_config->focusRight;
    p_imageInfo->afInfo.y2 = p_config->focusBottom;
    LOGD("<prepareRefocusImageInfo><comp> COffset: %f, afType: %d, x1: %d, y1: %d, x2: %d, y2: %d",
       p_imageInfo->COffset, p_imageInfo->afInfo.afType, p_imageInfo->afInfo.x1, p_imageInfo->afInfo.y1,
       p_imageInfo->afInfo.x2, p_imageInfo->afInfo.y2);
}

void RefocusConfigInfoWrapper::prepareRefocusInitInfo(RefocusInitInfo* p_initInfo,
        RefocusInitConfig* p_config)
{
    if (p_initInfo == NULL)
    {
        LOGD("<prepareRefocusInitInfo><comp> p_initInfo invalid!!");
        return;
    }

    p_initInfo->p_DpStream_cb = &mdpCallbackFunc;
}
