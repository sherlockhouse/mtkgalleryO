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

#include "JniFancyColor.h"
#include "AdvancedFancyColor.h"
#include "BasicFancyColor.h"

#include <android/bitmap.h>
#include <utils/Trace.h>
#include <cutils/trace.h>

using namespace stereo;

#define TAG "Fc/JniFancyColor"
#define ATRACE_TAG ATRACE_TAG_ALWAYS

namespace stereo {
    #define BYTES_PER_PIXCEL 4
}   // namespace stereo

JniFancyColor::JniFancyColor() {
    mFancyColors.push_back(new BasicFancyColor());
    mFancyColors.push_back(new AdvancedFancyColor());
}

JniFancyColor::~JniFancyColor() {
    mFancyColors.clear();
}

bool JniFancyColor::initialize(JNIEnv *env, jobject thiz, jobject config) {
    LOGD("<initialize>");
    // NULL implementation
    return true;
}

bool JniFancyColor::process(JNIEnv *env, jobject thiz, MINT32 actionType, jobject config, jobject result) {
    bool res = false;
    if (actionType == ACTION_GENERATE_EFFECT_IMAGE) {
        ATRACE_BEGIN(">>>>FcJni-generateEffectImg");
        res =  generateEffectImg(env, thiz, config, result);
        ATRACE_END();
    }
    return res;
}

jobject JniFancyColor::getInfo(JNIEnv *env, jobject thiz, MINT32 infoType, jobject config) {
    ATRACE_NAME(">>>>FcJni-getAllEffectsNames");
    if (infoType == INFO_ALL_EFFECTS_NAME) {
        return getAllEffectsNames(env, thiz);
    }
    return NULL;
}

/**********************private***************************/
// Generate effect bitmap for specified effect.
// Note: Make sure pConfig.bitmap as same as result.
bool JniFancyColor::generateEffectImg(JNIEnv *env, jobject thiz, jobject config, jobject result) {
    if(result == NULL) {
        LOGD("<generateEffectImg> parameter invalid.");
        return false;
    }

    bool res = false;
    long startSec;
    long startUsec;
    m_stereoUtils.startMeasureTime(&startSec, &startUsec);
    jbyteArray jworkingBuffer;
    jobject jworkingBitmap;
    GenerateEffectImgConfig * pConfig = new GenerateEffectImgConfig;
    ATRACE_BEGIN(">>>>FcJni-convertToJniGenerateEffectImgConfig");
    convertToJniGenerateEffectImgConfig(env, config, pConfig, jworkingBuffer, jworkingBitmap);
    ATRACE_END();

    MCHAR* pResultBitmap;
    ATRACE_BEGIN(">>>>FcJni-AndroidBitmap_lockPixels");
    AndroidBitmap_lockPixels(env, result, (void**) (&pResultBitmap));
    ATRACE_END();

    LOGD("<generateEffectImg> effectName: %s", pConfig->effectName);
    IFancyColor *fancyColor;
    for (MUINT32 j = 0; j < mFancyColors.size(); j++) {
        fancyColor = mFancyColors.at(j);
        res = fancyColor->generateEffectImg(env, thiz, config, result, pConfig, pResultBitmap);
        if (res) {
            break;
        }
    }

    AndroidBitmap_unlockPixels(env, result);
    releaseMaskBuf(env, &(pConfig->mask), jworkingBuffer);
    AndroidBitmap_unlockPixels(env, jworkingBitmap);
    delete[] pConfig->effectName;
    delete pConfig;
    m_stereoUtils.endMeasureTime("Fc/generateEffectImg()", startSec, startUsec);
    return res;
}

jobjectArray JniFancyColor::getAllEffectsNames(JNIEnv *env, jobject thiz) {
    LOGD("<getAllEffectsNames>");

    jstring jstr;
    IFancyColor *fancyColor;
    vector<jstring> jnames;
    vector<MCHAR*> names;

    for (MUINT32 j = 0; j < mFancyColors.size(); j++) {
        fancyColor = mFancyColors.at(j);
        names = fancyColor->getAllEffectsNames();
        for (MUINT32 i = 0; i < names.size(); i++) {
            jstr = env->NewStringUTF(names.at(i));
            jnames.push_back(jstr);
        }
    }

    MUINT32 nameCount = jnames.size();
    jobjectArray effectArray = env->NewObjectArray(nameCount, env->FindClass("java/lang/String"), 0);
    for (MUINT32 i = 0; i < nameCount; i++) {
        jstr = jnames.at(i);
        env->SetObjectArrayElement(effectArray, i, jstr);
    }

    return effectArray;
}

bool JniFancyColor::convertToJniGenerateEffectImgConfig(JNIEnv *env, jobject jsrcConfig,
        GenerateEffectImgConfig *pDestConfig, jbyteArray &jworkingBuffer, jobject &jworkingBitmap) {
    if (jsrcConfig == NULL || pDestConfig == NULL) {
        LOGD("<convertToJniGenerateEffectImgConfig> parameter invalid.");
        return false;
    }

    // find all classes
    jclass configClass = env->FindClass(
        "com/mediatek/stereoapplication/fancycolor/GenerateEffectImgConfig");
    if (configClass == NULL) {
        LOGD("<convertToJniGenerateEffectImgConfig><error> can't find class GenerateEffectImgConfig");
        return false;
    }

    // get all fields
    jfieldID bitmapField = env->GetFieldID(configClass, "bitmap",
            "Landroid/graphics/Bitmap;");
    jfieldID maskBufField = env->GetFieldID(configClass, "maskBuf",
            "Lcom/mediatek/stereoapplication/MaskBuf;");
    jfieldID effectNameField = env->GetFieldID(configClass, "effectName",
            "Ljava/lang/String;");

    // contruct native objects by java objects
    jworkingBitmap = env->GetObjectField(jsrcConfig, bitmapField);
    if (jworkingBitmap == NULL) {
        LOGD("<convertToJniGenerateEffectImgConfig><error> bitmap is NULL!!");
        return false;
    }
    ATRACE_BEGIN(">>>>FcJni-convertBitmapToImageBuf");
    convertBitmapToImageBuf(env, jworkingBitmap, &(pDestConfig->bitmap));
    ATRACE_END();

    jobject jmaskBuf = env->GetObjectField(jsrcConfig, maskBufField);
    if (jmaskBuf == NULL) {
        LOGD("<convertToJniGenerateEffectImgConfig><error> maskBuf is NULL.");
        return false;
    } else {
        convertToJniMaskBuf(env, jmaskBuf, &(pDestConfig->mask), jworkingBuffer);
    }

    jstring jeffectName = (jstring)(env->GetObjectField(jsrcConfig, effectNameField));
    if (jeffectName == NULL) {
        LOGD("<convertToJniGenerateEffectImgConfig><error> jeffectName is NULL!!");
        return false;
    }

    const MCHAR* peffectName = (const MCHAR*)(env->GetStringUTFChars(jeffectName, NULL));
    pDestConfig->effectName = new MCHAR[strlen(peffectName) + 1];
    strcpy(pDestConfig->effectName, peffectName);
    env->ReleaseStringUTFChars(jeffectName, peffectName);

    return true;
}

bool JniFancyColor::convertToJniMaskBuf(JNIEnv *env, jobject jsrcMaskBuf,
        MaskBuf *pDestMaskBuf, jbyteArray &jworkingBuffer) {
    jclass maskBufClass = env->FindClass("com/mediatek/stereoapplication/MaskBuf");
    if (maskBufClass == NULL) {
        LOGD("<convertToJniMaskBuf><error> can't find class MaskBuf");
        return false;
    }

    // get all fields
    jfieldID maskBufMaskField = env->GetFieldID(maskBufClass, "mask", "[B");
    jfieldID maskBufBufferSizeField = env->GetFieldID(maskBufClass, "bufferSize", "I");
    jfieldID maskBufRectField = env->GetFieldID(maskBufClass, "rect", "Landroid/graphics/Rect;");
    jfieldID maskBufPointField = env->GetFieldID(maskBufClass, "point", "Landroid/graphics/Point;");

    jbyteArray maskBuffer = (jbyteArray)env->GetObjectField(jsrcMaskBuf, maskBufMaskField);
    jworkingBuffer = maskBuffer;
    pDestMaskBuf->mask = (MUINT8*)env->GetByteArrayElements(maskBuffer, 0);
    pDestMaskBuf->bufferSize = env->GetIntField(jsrcMaskBuf, maskBufBufferSizeField);
    jobject maskBufRect = env->GetObjectField(jsrcMaskBuf, maskBufRectField);
    if (maskBufRect != NULL) {
        pDestMaskBuf->rect = new Rect;
        convertToJniRect(env, maskBufRect, pDestMaskBuf->rect);
    } else {
        pDestMaskBuf->rect = NULL;
    }
    jobject maskBufPoint = env->GetObjectField(jsrcMaskBuf, maskBufPointField);
     if (maskBufPoint != NULL) {
        pDestMaskBuf->point = new Point;
        convertToJniPoint(env, maskBufPoint, pDestMaskBuf->point);
    } else {
        pDestMaskBuf->point = NULL;
    }

    return true;
}

bool JniFancyColor::releaseMaskBuf(JNIEnv *env, MaskBuf *pMaskBuf, jbyteArray &jworkingBuffer) {
    env->ReleaseByteArrayElements(jworkingBuffer, (jbyte *)(pMaskBuf->mask), 0);
    delete (pMaskBuf->rect);
    delete (pMaskBuf->point);
    return true;
}

bool JniFancyColor::convertBitmapToImageBuf(JNIEnv *env, jobject jbitmap, ImageBuf *pImageBuf) {
    // SkBitmap* bm = GraphicsJNI::getNativeBitmap(env, jbitmap);
    jclass bitmapClass = env->FindClass("android/graphics/Bitmap");
    jmethodID intValueMethod = env->GetMethodID(bitmapClass, "getWidth", "()I");
    pImageBuf->width = env->CallIntMethod(jbitmap, intValueMethod);
    intValueMethod = env->GetMethodID(bitmapClass, "getHeight", "()I");
    pImageBuf->height = env->CallIntMethod(jbitmap, intValueMethod);
    pImageBuf->bufferSize = pImageBuf->width * pImageBuf->height * BYTES_PER_PIXCEL;
    AndroidBitmap_lockPixels(env, jbitmap, (void**) &(pImageBuf->buffer));    // TODO is this safe?

    return true;
}

bool JniFancyColor::convertToJniRect(JNIEnv *env, jobject jsrcRect, Rect *pDestRect) {
    jclass rectClass = env->FindClass("android/graphics/Rect");
    if (rectClass== NULL) {
        LOGD("<convertToJniRect><error> can't find class Rect");
        return false;
    }

    jfieldID rectLeftField = env->GetFieldID(rectClass, "left", "I");
    jfieldID rectTopField = env->GetFieldID(rectClass, "top", "I");
    jfieldID rectRightField = env->GetFieldID(rectClass, "right", "I");
    jfieldID rectBottomField = env->GetFieldID(rectClass, "bottom", "I");

    pDestRect->left = env->GetIntField(jsrcRect, rectLeftField);
    pDestRect->top = env->GetIntField(jsrcRect, rectTopField);
    pDestRect->right = env->GetIntField(jsrcRect, rectRightField);
    pDestRect->bottom = env->GetIntField(jsrcRect, rectBottomField);

    return true;
}

bool JniFancyColor::convertToJniPoint(JNIEnv *env, jobject jsrcPoint, Point *pDestPoint) {
    jclass pointClass = env->FindClass("android/graphics/Point");
    if (pointClass== NULL) {
        LOGD("<convertToJniPoint><error> can't find class Point");
        return false;
    }

    jfieldID pointXField = env->GetFieldID(pointClass, "x", "I");
    jfieldID pointYField = env->GetFieldID(pointClass, "y", "I");

    pDestPoint->x = env->GetIntField(jsrcPoint, pointXField);
    pDestPoint->y = env->GetIntField(jsrcPoint, pointYField);

    return true;
}

