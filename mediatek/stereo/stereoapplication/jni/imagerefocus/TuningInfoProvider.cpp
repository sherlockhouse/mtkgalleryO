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

#include "TuningInfoProvider.h"

using namespace stereo;

TuningInfoProvider::TuningInfoProvider(const char* tuningConfig) {
    m_pDebugHelper = new DebugHelper();
    if (access(REFOCUS_DEBUG_FOLDER, 0) != -1) {
        MCHAR name[FILE_NAME_LENGTH];
        sprintf(name, "%s%s", REFOCUS_DEBUG_FOLDER, HEADER_TUNING_CONFIG_OUT_NAME);
        m_pDebugHelper->dumpBufferToFile(name, (MUINT8 *) tuningConfig, strlen(tuningConfig));
    }

    if (enableRealTimeTuning()) {
        std::ifstream file(REALTIME_TUNING_CONFIG_IN_NAME);
        bool fileExist = file.is_open();
        file.close();
        if (!fileExist) {
            LOGD("<TuningInfoProvider> no realtime_tuning_cfg, create it");
            m_pDebugHelper->dumpBufferToFile(REALTIME_TUNING_CONFIG_IN_NAME, (MUINT8 *) tuningConfig,
                    strlen(tuningConfig));
        }
        m_realTimeConfig = readFile(REALTIME_TUNING_CONFIG_IN_NAME);
        if (m_realTimeConfig == NULL) {
            m_document.Parse(tuningConfig);
            LOGD("<TuningInfoProvider> m_realTimeConfig is null!!");
        } else {
            m_document.Parse(m_realTimeConfig);
        }
        LOGD("<TuningInfoProvider> runtime tuning from [%s]", REALTIME_TUNING_CONFIG_IN_NAME);
    } else {
        m_document.Parse(tuningConfig);
        LOGD("<TuningInfoProvider> read tuningInfo from [camera_custom_stereo_tuning.h]");
    }
}

TuningInfoProvider::~TuningInfoProvider() {
    if (m_realTimeConfig != NULL) {
        delete[] m_realTimeConfig;
        m_realTimeConfig = NULL;
    }
    delete m_pDebugHelper;
    m_pDebugHelper = NULL;
}

void TuningInfoProvider::getTuningInfo(std::vector<std::pair<char* , int>> &tuningInfo) {
    if (!m_document.HasMember(SW_REFOCUS)) {
        LOGD("<getTuningInfo> can not find SW_REFOCUS");
        return;
    }
    const rapidjson::Value& refocusValues = m_document[SW_REFOCUS];

    for(auto &m : refocusValues.GetObject()) {
        if (m.name != CLEAR_RANGE_TABLE) {
            tuningInfo.push_back({copy(m.name.GetString()), m.value.GetInt()});
        }
    }
}

void TuningInfoProvider::getClearTable(std::vector<int> &clearTable) {
    if (!m_document.HasMember(SW_REFOCUS)) {
        LOGD("<getClearTable> can not find SW_REFOCUS");
        return;
    }
    const rapidjson::Value& refocusValues = m_document[SW_REFOCUS];

    for(auto &m : refocusValues.GetObject()) {
        if (m.name == CLEAR_RANGE_TABLE) {
            for(auto &v : m.value.GetArray()) {
                clearTable.push_back(v.GetInt());
            }
            break;
        }
    }
}

char* TuningInfoProvider::copy(const char* str) {
    char *res = new char[strlen(str) + 1];
    strcpy(res, str);
    return res;
}

char* TuningInfoProvider::readFile(const char* filePath) {
    if (filePath == NULL) {
        LOGD("<readFile> filePath is null");
        return NULL;
    }
    std::ifstream file(filePath);
    if (!file.is_open()) {
        LOGD("<readFile> open fail");
        return NULL;
    }
    file.seekg(0, std::ios::end);
    int length = file.tellg();
    if (length <= 0) {
        LOGD("<readFile> filePath: %s, length <= 0", filePath);
        file.close();
        return NULL;
    }
    file.seekg(0, std::ios::beg);
    char* buffer = new char[length];
    file.read(buffer, length);
    file.close();
    if (access(REFOCUS_DEBUG_FOLDER, 0) != -1) {
        MCHAR name[FILE_NAME_LENGTH];
        sprintf(name, "%s%s", REFOCUS_DEBUG_FOLDER, REALTIME_TUNING_CONFIG_OUT_NAME);
        m_pDebugHelper->dumpBufferToFile(name, (MUINT8 *) buffer, strlen(buffer));
    }
    return buffer;
}

bool TuningInfoProvider::enableRealTimeTuning() {
    if (1 == property_get_int32(REALTIME_TUNING_PROPERTY, 0)) {
        LOGD("<enableRealTimeTuning> enable realtime tuning!!");
        return true;
    }
    LOGD("<enableRealTimeTuning> %s, disable realtime tuning!!", REALTIME_TUNING_PROPERTY);
    return false;
}
