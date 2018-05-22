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

#ifndef IPROCESSOR_H_
#define IPROCESSOR_H_

#include "jni.h"

namespace stereo {

class IProcessor {
public:
    IProcessor() {
    }

    virtual ~IProcessor() {
    }
    // initialize processor with configuration parameter.
    // Parameters:
    //   JNIEnv *env  [IN]
    //   jobject thiz  [IN]
    //   jint featureType  [IN] feature type
    //   jobject config  [IN] the required parameter(structure)
    // Returns:
    //   Success->ture, fail->false
    virtual bool initialize(JNIEnv *env, jobject thiz, jobject config) = 0;

    // Process some thing according to action type, and put process result to result parameter.
    // Parameters:
    //   JNIEnv *env  [IN]
    //   jobject thiz  [IN]
    //   jint featureType  [IN] feature type
    //   jobject config  [IN] the required parameter(structure)
    //   jobject result  [OUT] process result
    // Returns:
    //   Success->ture, fail->false
    virtual bool process(JNIEnv *env, jobject thiz, jint actionType, jobject config, jobject result) = 0;

    // Get specified information according to information type.
    // Parameters:
    //   JNIEnv *env  [IN]
    //   jobject thiz  [IN]
    //   jint infoType  [IN] information type
    //   jobject config  [IN] the required parameter(structure)
    // Returns:
    //   Specified information
    virtual jobject getInfo(JNIEnv *env, jobject thiz, jint infoType, jobject config) = 0;
};

}  // namespace stereo
#endif /* IPROCESSOR_H_ */
