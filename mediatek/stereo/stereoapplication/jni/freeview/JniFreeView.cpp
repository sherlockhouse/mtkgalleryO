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

#include "JniFreeView.h"
#include <utils/Trace.h>
#include <cutils/trace.h>

using namespace stereo;

#define ATRACE_TAG ATRACE_TAG_ALWAYS

JniFreeView::JniFreeView() {
    m_pFreeView = new FreeView();
}

JniFreeView::~JniFreeView() {
    delete m_pFreeView;
}

bool JniFreeView::initialize(JNIEnv *env, jobject thiz, jobject config) {
    ALOGD("<initialize>");
    jclass configClass = env->FindClass(
                 "com/mediatek/stereoapplication/freeview/InitConfig");

    if (configClass == NULL) {
        ALOGD("<initialize><error> can't find class InitConfig");
        return false;
    }

    FreeViewInitConfig initConfig;

    jfieldID bitmapField = env->GetFieldID(configClass, "bitmap", "Landroid/graphics/Bitmap;");
    jfieldID depthBufField = env->GetFieldID(configClass, "depth",
            "Lcom/mediatek/stereoapplication/DepthBuf;");
    jfieldID outWidthField = env->GetFieldID(configClass, "outWidth", "I");
    jfieldID outHeightField = env->GetFieldID(configClass, "outHeight", "I");
    jfieldID imageOrientationField = env->GetFieldID(configClass, "imageOrientation", "I");

    // Get bitmap, and set to FreeViewInitConfig
    jobject bitmap = env->GetObjectField(config, bitmapField);
    if (bitmap == NULL) {
        ALOGD("<initialize><error> bitmap is NULL!!");
        return false;
    }

    jclass bitmapClass = env->FindClass("android/graphics/Bitmap");
    if (bitmapClass == NULL) {
        ALOGD("<initialize><error> can't find class Bitmap");
        return false;
    }

    jmethodID getWidthMethod = env->GetMethodID(bitmapClass, "getWidth", "()I");
    jmethodID getHeightMethod = env->GetMethodID(bitmapClass, "getHeight", "()I");
    initConfig.inputWidth = env->CallIntMethod(bitmap, getWidthMethod);
    initConfig.inputHeight = env->CallIntMethod(bitmap, getHeightMethod);

    MUINT8 *pBitmap;
    AndroidBitmap_lockPixels(env, bitmap, (void **) &pBitmap);
    initConfig.bitmap = pBitmap;

    // Get depthBuf, and set to FreeViewInitConfig
    jobject depthBuf = env->GetObjectField(config, depthBufField);
    if (depthBuf == NULL) {
        AndroidBitmap_unlockPixels(env, bitmap);
        ALOGD("<initialize><error> depthBuf is NULL!!");
        return false;
    }
    jbyteArray depthBufArrayOut;
    if (!convertToJniDepthBuf(env, depthBuf, depthBufArrayOut, &(initConfig.depth))) {
        env->ReleaseByteArrayElements(depthBufArrayOut, (jbyte *)(initConfig.depth.buffer), 0);
        AndroidBitmap_unlockPixels(env, bitmap);
        ALOGD("<initialize><error> convertToJniDepthBuf fail");
        return false;
    }

    // set to outputWidth/outputHeight/imageOrientation
    initConfig.outputWidth = env->GetIntField(config, outWidthField);
    initConfig.outputHeight = env->GetIntField(config, outHeightField);
    initConfig.imageOrientation = env->GetIntField(config, imageOrientationField);

    if (!m_pFreeView->initialize(&initConfig)) {
        env->ReleaseByteArrayElements(depthBufArrayOut, (jbyte *)(initConfig.depth.buffer), 0);
        AndroidBitmap_unlockPixels(env, bitmap);
        ALOGD("<initialize><error> initialize fail");
        return false;
    }

    env->ReleaseByteArrayElements(depthBufArrayOut, (jbyte *)(initConfig.depth.buffer), 0);

    AndroidBitmap_unlockPixels(env, bitmap);

    ALOGD("<initialize> initialize success");
    return true;
}

bool JniFreeView::process(JNIEnv *env, jobject thiz, int actionType, jobject config, jobject result) {
    if (actionType == ACTION_SHIFT_PERSPECTIVE) {
        return shiftPerspective(env, thiz, config, result);
    }
    return false;
}

jobject JniFreeView::getInfo(JNIEnv *env, jobject thiz, int infoType, jobject config) {
    return NULL;
}

bool JniFreeView::shiftPerspective(JNIEnv *env, jobject thiz, jobject config, jobject result) {
    ATRACE_NAME(">>>>FvJni-shiftPerspectives");
    jclass configClass = env->FindClass(
                 "com/mediatek/stereoapplication/freeview/ShiftPerspectiveConfig");
    if (configClass == NULL) {
        ALOGD("<shiftPerspective><error> can't find class ShiftPerspectiveConfig");
        return false;
    }

    ShiftPerspectiveConfig shiftPerspectiveConfig;

    jfieldID xField = env->GetFieldID(configClass, "x", "I");
    jfieldID yField = env->GetFieldID(configClass, "y", "I");
    jfieldID outTextureIdField = env->GetFieldID(configClass, "outTextureId", "I");

    shiftPerspectiveConfig.x = env->GetIntField(config, xField);
    shiftPerspectiveConfig.y = env->GetIntField(config, yField);
    shiftPerspectiveConfig.outputTextureId = env->GetIntField(config, outTextureIdField);

    ATRACE_BEGIN(">>>>FvJni-shiftPerspective");
    if (!m_pFreeView->shiftPerspective(&shiftPerspectiveConfig)) {
        ALOGD("<shiftPerspective><error> shiftPerspective fail");
        ATRACE_END();
        return false;
    }
    ATRACE_END();
    return true;
}

bool JniFreeView::convertToJniDepthBuf(JNIEnv *env, jobject srcDepthBuf,
    jbyteArray &depthBufOut, DepthBuf *pDestDepthBuf) {
    jclass depBufClass = env->FindClass(
            "com/mediatek/stereoapplication/DepthBuf");
    if (depBufClass == NULL) {
        ALOGD("<convertToJniDepthBuf><error> can't find class DepthBuf");
        return false;
    }

    jfieldID depthBufferFieldId = env->GetFieldID(depBufClass, "buffer", "[B");
    jfieldID depthBufferSizeFieldId = env->GetFieldID(depBufClass, "bufferSize", "I");
    jfieldID depthWidthFieldId = env->GetFieldID(depBufClass, "depthWidth", "I");
    jfieldID depthHeightFieldId = env->GetFieldID(depBufClass, "depthHeight", "I");

    jbyteArray bufTemp = (jbyteArray)env->GetObjectField(srcDepthBuf, depthBufferFieldId);
     // release depthBufOut at caller
    depthBufOut = bufTemp;
    pDestDepthBuf->buffer = (MUINT8*)env->GetByteArrayElements(bufTemp, 0);
    pDestDepthBuf->bufferSize = env->GetIntField(srcDepthBuf, depthBufferSizeFieldId);
    pDestDepthBuf->depthWidth = env->GetIntField(srcDepthBuf, depthWidthFieldId);
    pDestDepthBuf->depthHeight = env->GetIntField(srcDepthBuf, depthHeightFieldId);

    return true;
}
