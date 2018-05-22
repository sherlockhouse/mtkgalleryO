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

#ifndef TUNINGINFOPROVIDER_H_
#define TUNINGINFOPROVIDER_H_

#include <vector>
#include <fstream>
#include <iostream>
#include <cutils/properties.h>

#include "Log.h"
#include "StereoType.h"
#include "DebugHelper.h"
#include "../rapidjson/document.h"


namespace stereo {

#define TAG "Rf/TuningInfoProvider"
#define SW_REFOCUS "SW_REFOCUS"
#define CLEAR_RANGE_TABLE "ClearRangeTable"
#define REFOCUS_DEBUG_FOLDER "/sdcard/refocusap/"
#define REALTIME_TUNING_PROPERTY "debug.gallery.STEREO.tuning"
#define REALTIME_TUNING_CONFIG_IN_NAME "/sdcard/stereo_tuning.json"
#define REALTIME_TUNING_CONFIG_OUT_NAME "stereo_tuning_out.json"
#define HEADER_TUNING_CONFIG_OUT_NAME "header_tuning_out.json"

#define FILE_NAME_LENGTH 100

class TuningInfoProvider {
public:
    TuningInfoProvider(const char* tuningConfig);

    ~TuningInfoProvider();

    void getTuningInfo(std::vector<std::pair<char* , int>> &tuningInfo);

    void getClearTable(std::vector<int> &clearTable);

private:
    rapidjson::Document m_document;
    DebugHelper *m_pDebugHelper = NULL;
    char* m_realTimeConfig = NULL;
    char* copy(const char* str);
    char* readFile(const char* filePath);
    bool enableRealTimeTuning();
};
}
#endif /* TuningInfoProvider_H_ */
