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

/*
 * Load JNI and register native methods.
 */

#include "jni.h"
#include "IProcessor.h"
#include "ProcessorFactory.h"
#include "Log.h"

using namespace stereo;

#define TAG "JniStereo"

// Create processor instance with feature name.
// Parameters:
//   JNIEnv *env  [IN]
//   jobject thiz  [IN]
//   jstring featureName  [IN] feature name
// Returns:
//   Return processor
jlong create(JNIEnv *env, jobject thiz, jstring featureName) {
    LOGD("<create>");
    ProcessorFactory factory;

    const char* pFeatureName =  env->GetStringUTFChars(featureName, NULL);
    IProcessor* processor = factory.createInstance(pFeatureName);
    env->ReleaseStringUTFChars(featureName, pFeatureName);
    return reinterpret_cast<jlong>((void*)processor);
}

// Initialize processor.
// Parameters:
//   JNIEnv *env  [IN]
//   jobject thiz  [IN]
//   jobject config  [IN] the required parameter(structure)
// Returns:
//   Success->ture, fail->false
jboolean initialize(JNIEnv *env, jobject thiz, jlong caller, jobject config) {
    LOGD("<initalize>");
    IProcessor* processor = reinterpret_cast<IProcessor*>((long) caller);
    if (NULL != processor) {
        return processor->initialize(env, thiz, config);
    }
    return false;
}

// Process some thing according to action type, and put process result to result parameter.
// Parameters:
//   JNIEnv *env  [IN]
//   jobject thiz  [IN]
//   jint featureType  [IN] feature type
//   jobject config  [IN] the required parameter(structure)
//   jobject result  [OUT] process result
// Returns:
//   Success->ture, fail->false
jboolean process(JNIEnv *env, jobject thiz, jlong caller, jint actionType, jobject config, jobject result) {
    LOGD("<process>");
    IProcessor* processor = reinterpret_cast<IProcessor*>((long) caller);
    if (NULL != processor) {
        return processor->process(env, thiz, actionType, config, result);
    }
    return false;
}


// Get specified information according to information type.
// Parameters:
//   JNIEnv *env  [IN]
//   jobject thiz  [IN]
//   jint infoType  [IN] information type
//   jobject config  [IN] the required parameter(structure)
// Returns:
//   Specified information
jobject getInfo(JNIEnv *env, jobject thiz, jlong caller, jint infoType, jobject config) {
    LOGD("<getInfo>");
    IProcessor* processor = reinterpret_cast<IProcessor*>((long) caller);
    if (NULL != processor) {
        return processor->getInfo(env, thiz, infoType, config);
    }
    return NULL;
}

// Destroy object and release memory
// Parameters:
//   JNIEnv *env  [IN]
//   jobject thiz  [IN]
void release(JNIEnv *env, jobject thiz, jlong caller) {
    LOGD("<release>");
    IProcessor* processor = reinterpret_cast<IProcessor*>((long) caller);
    if (NULL != processor) {
        delete processor;
    }
}


static const char *classPathName = "com/mediatek/stereoapplication/StereoApplication";


static JNINativeMethod methods[] = {
        {"create", "(Ljava/lang/String;)J", (void*) create},
        { "initialize", "(JLjava/lang/Object;)Z", (void*) initialize },
        { "process", "(JILjava/lang/Object;Ljava/lang/Object;)Z", (void*) process },
        { "getInfo", "(JILjava/lang/Object;)Ljava/lang/Object;", (void*) getInfo },
        { "release", "(J)V", (void*) release },
};

/*
 * Register several native methods for one class.
 */
static int registerNativeMethods(JNIEnv* env, const char* className, JNINativeMethod* gMethods, int numMethods) {
    jclass clazz;

    clazz = env->FindClass(className);
    if (clazz == NULL) {
        LOGE("Native registration unable to find class '%s'", className);
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        LOGE("RegisterNatives failed for '%s'", className);
        return JNI_FALSE;
    }

    return JNI_TRUE;
}


/*
 * Register native methods for all classes we know about.
 *
 * returns JNI_TRUE on success.
 */
static int registerNatives(JNIEnv* env) {
    if (!registerNativeMethods(env, classPathName, methods, sizeof(methods) / sizeof(methods[0]))) {
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

/*
 * This is called by the VM when the shared library is first loaded.
 */
typedef union {
    JNIEnv* env;
    void* venv;
} UnionJNIEnvToVoid;

jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    UnionJNIEnvToVoid uenv;
    uenv.venv = NULL;
    jint result = -1;
    JNIEnv* env = NULL;

    LOGD("JNI_OnLoad");

    if (vm->GetEnv(&uenv.venv, JNI_VERSION_1_4) != JNI_OK) {
        LOGE("ERROR: GetEnv failed");
        goto bail;
    }
    env = uenv.env;
    if (registerNatives(env) != JNI_TRUE) {
        LOGE("ERROR: registerNatives failed");
        goto bail;
    }

    result = JNI_VERSION_1_4;

    bail: return result;
}
