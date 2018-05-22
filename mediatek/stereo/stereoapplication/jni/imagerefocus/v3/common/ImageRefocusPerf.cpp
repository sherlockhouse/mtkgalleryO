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

#include "ImageRefocusPerf.h"
#define TAG "Rf/ImageRefocusPerf"

using namespace stereo;

ImageRefocusPerf::ImageRefocusPerf()
{
    m_powerHal = IPower::getService();
    LOGD("<ImageRefocusPerf><comp> ImageRefocusPerf(v3), m_powerHal: %p",
            &m_powerHal);
}

void ImageRefocusPerf::refocusPerfSrvEnable()
{
    if (m_powerHal == NULL)
    {
        LOGD("<refocusPerfSrvEnable><comp> m_powerHal == NULL, return");
        return;
    }
    LOGD("<refocusPerfSrvEnable><comp> ImageRefocusPerf(v3), scenario: %d",
            GALLERY_STEREO_SCENARIO);
    m_powerHal->mtkPowerHint(GALLERY_STEREO_SCENARIO, DEFAULT_TIME_OUT_10S);
}

void ImageRefocusPerf::refocusPerfSrvDisable()
{
    if (m_powerHal == NULL)
    {
        LOGD("<refocusPerfSrvDisable><comp> m_powerHal == NULL, return");
        return;
    }
    LOGD("<refocusPerfSrvDisable><comp> ImageRefocusPerf(v3), scenario: %d",
            GALLERY_STEREO_SCENARIO);
    m_powerHal->mtkPowerHint(GALLERY_STEREO_SCENARIO, 0);
}

int ImageRefocusPerf::getCpuCoreNumOfCluster(int clusterNum)
{
    if (m_powerHal == NULL)
    {
        LOGD("<getCpuCoreNumOfCluster><comp> m_powerHal == NULL, return");
        return DEFAULT_CORE_NUM_OF_CLUSTER_0;
    }
    // get cluster number
    int numOfCluster = m_powerHal->querySysInfo(MtkQueryCmd::CMD_GET_CLUSTER_NUM, 0);
    LOGD("<getCpuCoreNumOfCluster> total cluster num: %d, clusterNum: %d", numOfCluster, clusterNum);
    if (clusterNum < 0 || clusterNum >= numOfCluster)
    {
        LOGD("<getCpuCoreNumOfCluster> params error, clusterNum: %d", clusterNum);
        if (clusterNum == 0)
        {
            return DEFAULT_CORE_NUM_OF_CLUSTER_0;
        } else if (clusterNum == 1)
        {
            return DEFAULT_CORE_NUM_OF_CLUSTER_1;
        } else
        {
            return  DEFAULT_CORE_NUM_OF_CLUSTER_2;
        }
    }

    // get CPU number
    int cpuNum = m_powerHal->querySysInfo(MtkQueryCmd::CMD_GET_CLUSTER_CPU_NUM, clusterNum);
    LOGD("<getCpuCoreNumOfCluster> cpuNum: %d, clusterNum: %d", cpuNum, clusterNum);
    return cpuNum;
}
