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
#define TAG "Rf/RefocusConfigInfoWrapper"

using namespace stereo;

RefocusConfigInfoWrapper::RefocusConfigInfoWrapper()
{
    LOGD("<RefocusConfigInfoWrapper><comp> new RefocusConfigInfoWrapper (v2)");
}

RefocusConfigInfoWrapper::~RefocusConfigInfoWrapper()
{
    LOGD("<~RefocusConfigInfoWrapper><comp> RefocusConfigInfoWrapper (v2)");
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
    LOGD("<prepareRefocusTuningInfo><comp> (v2), RFCoreNumber: %d, %d, %d",
            p_tuningInfo->RFCoreNumber[0], p_tuningInfo->RFCoreNumber[1],
            p_tuningInfo->RFCoreNumber[2]);
}

void RefocusConfigInfoWrapper::prepareRefocusImageInfo(RefocusImageInfo* p_imageInfo,
        RefocusInitConfig* p_config)
{
}

void RefocusConfigInfoWrapper::prepareRefocusInitInfo(RefocusInitInfo* p_initInfo,
        RefocusInitConfig* p_config)
{
}
