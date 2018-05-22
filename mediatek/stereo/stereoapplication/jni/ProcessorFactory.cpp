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

#include "ProcessorFactory.h"
#include "featurecreator/DepthGeneratorCreator.h"
#include "featurecreator/FancyColorCreator.h"
#include "featurecreator/FreeViewCreator.h"
#include "featurecreator/RefocusCreator.h"
#include "featurecreator/SegmentCreator.h"
#include "Log.h"

using namespace stereo;

#define TAG "ProcessorFactory"

ProcessorFactory::ProcessorFactory() {
    ICreator *pCreator = new RefocusCreator();
    s_creatorMap[pCreator->getFeatureName()] = pCreator;
    pCreator = new DepthGeneratorCreator();
    s_creatorMap[pCreator->getFeatureName()] = pCreator;
    pCreator = new FancyColorCreator();
    s_creatorMap[pCreator->getFeatureName()] = pCreator;
    pCreator = new FreeViewCreator();
    s_creatorMap[pCreator->getFeatureName()] = pCreator;
    pCreator = new SegmentCreator();
    s_creatorMap[pCreator->getFeatureName()] = pCreator;
}

ProcessorFactory::~ProcessorFactory() {
    map<const char*, ICreator*>::iterator factory_iter;
    for (factory_iter = s_creatorMap.begin(); factory_iter != s_creatorMap.end(); factory_iter++) {
        delete factory_iter->second;
    }
}

// Create processor object according to feature type.
// Parameters:
//   int featureType  [IN] feature type
// Returns:
//   Success->processor object, fail->NULL
IProcessor* ProcessorFactory::createInstance(const char* featureName) {
    LOGD("<createInstance> feature name:%s", featureName);
    IProcessor* pProcessor;
    map<const char*, ICreator*>::iterator factory_iter;
    for (factory_iter = s_creatorMap.begin(); factory_iter != s_creatorMap.end(); factory_iter++) {
       if(!strcmp(factory_iter->first, featureName)) {
           ICreator* pObject = factory_iter->second;
           pProcessor = (IProcessor*)(pObject->getInstance());
           return pProcessor;
        }
    }
    LOGD("<createInstance><ERROR>create fail!!!");
    return NULL;
}
