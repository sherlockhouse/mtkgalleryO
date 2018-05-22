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

#include <android/bitmap.h>

#include "JniImageSegment.h"

using namespace stereo;

#define BYTES_PER_PIXCEL 4

JniImageSegment::JniImageSegment() {
}

JniImageSegment::~JniImageSegment() {
    LOGD("~JniImageSegment");

    if (NULL != m_pImageSegment) {
        delete m_pImageSegment;
        m_pImageSegment = NULL;
    }
}

bool JniImageSegment::initialize(JNIEnv *env, jobject thiz, jobject config) {
    LOGD("<initialize> begin");

    // TODO how to distinguish this case (for quick mask scale)
    if (config == NULL) {
        // maybe scaleWithoutInit() can be moved to JniFancyColor
        LOGD("<initialize> initialize for quick mask scale");
        return true;
    }

    // find all classes
    jclass configClass = env->FindClass(
        "com/mediatek/stereoapplication/imagesegment/InitConfig");
    if (configClass == NULL) {
        LOGD("<initialize><error> can't find class InitConfig");
        return false;
    }
    jclass faceInfoClass = env->FindClass(
        "com/mediatek/stereoapplication/imagesegment/InitConfig$FaceInfo");
    if (faceInfoClass == NULL) {
        LOGD("<initialize><error> can't find class InitConfig$FaceInfo");
        return false;
    }

    // get all fields
    jfieldID bitmapField = env->GetFieldID(configClass, "bitmap",
            "Landroid/graphics/Bitmap;");
    jfieldID depthBufField = env->GetFieldID(configClass, "depthBuf",
            "Lcom/mediatek/stereoapplication/DepthBuf;");
    jfieldID maskBufField = env->GetFieldID(configClass, "maskBuf",
            "Lcom/mediatek/stereoapplication/MaskBuf;");
    jfieldID imageOrientationField = env->GetFieldID(configClass, "imageOrientation", "I");
    jfieldID faceNumberField = env->GetFieldID(configClass, "faceNum", "I");
    jfieldID faceInfosField = env->GetFieldID(configClass, "faceInfos",
            "[Lcom/mediatek/stereoapplication/imagesegment/InitConfig$FaceInfo;");
    jfieldID faceInfosLeftField = env->GetFieldID(faceInfoClass, "left", "I");
    jfieldID faceInfosTopField = env->GetFieldID(faceInfoClass, "top", "I");
    jfieldID faceInfosRightField = env->GetFieldID(faceInfoClass, "right", "I");
    jfieldID faceInfosBottomField = env->GetFieldID(faceInfoClass, "bottom", "I");
    jfieldID faceInfosRipField = env->GetFieldID(faceInfoClass, "faceOrientation", "I");

    // contruct native objects by java objects
    InitConfig initConfig;

    jobject jbitmap = env->GetObjectField(config, bitmapField);
    if (jbitmap == NULL) {
        LOGD("<initialize><error> bitmap is NULL!!");
        return false;
    }
    convertBitmapToImageBuf(env, jbitmap, &(initConfig.bitmap));

    jobject jdepthBuf = env->GetObjectField(config, depthBufField);
    if (jdepthBuf== NULL) {
        LOGD("<initialize><error> depthBuf is NULL!!");
        return false;
    }
    jbyteArray jdepthWorkingBuffer;
    convertToJniDepthBuf(env, jdepthBuf, &(initConfig.depth), jdepthWorkingBuffer);

    jobject jmaskBuf = env->GetObjectField(config, maskBufField);
    jbyteArray jmaskWorkingBuffer;
    if (jmaskBuf == NULL) {
        LOGD("<initialize> maskBuf is NULL.");
        initConfig.mask.mask = NULL;
        initConfig.mask.rect = NULL;
        initConfig.mask.point = NULL;
    } else {
        convertToJniMaskBuf(env, jmaskBuf, &(initConfig.mask), jmaskWorkingBuffer);
    }

    initConfig.scribbleWidth = initConfig.bitmap.width;
    initConfig.scribbleHeight = initConfig.bitmap.height;
    initConfig.imageOrientation = env->GetIntField(config, imageOrientationField);
    initConfig.faceNum = env->GetIntField(config, faceNumberField);

    jobjectArray faceInfos = (jobjectArray)(env->GetObjectField(config, faceInfosField));
    if (faceInfos != NULL && initConfig.faceNum > 0) {
        Rect * faceRects = (Rect *) malloc(sizeof(Rect) * initConfig.faceNum);
        int * faceRips = (int *) malloc(sizeof(int) * initConfig.faceNum);
        for (int i = 0; i < initConfig.faceNum; i++) {
            jobject faceInfo = env->GetObjectArrayElement(faceInfos, i);
            if (faceInfo == NULL) {
                continue;
            }
            faceRects[i].left = env->GetIntField(faceInfo, faceInfosLeftField);
            faceRects[i].top = env->GetIntField(faceInfo, faceInfosTopField);
            faceRects[i].right = env->GetIntField(faceInfo, faceInfosRightField);
            faceRects[i].bottom = env->GetIntField(faceInfo, faceInfosBottomField);
            faceRips[i] = env->GetIntField(faceInfo, faceInfosRipField);
            LOGD("<initialize> left:%d, top:%d, right:%d, bottom:%d, faceRips:%d",faceRects[i]
            .left, faceRects[i].top, faceRects[i].right, faceRects[i].bottom, faceRips[i]);
        }
        initConfig.faceRect = faceRects;
        initConfig.faceRip = faceRips;
    }
    m_pImageSegment = new ImageSegment();
    bool result = m_pImageSegment->initialize(&initConfig);

    AndroidBitmap_unlockPixels(env, jbitmap);
    env->ReleaseByteArrayElements(jdepthWorkingBuffer, (jbyte *)(initConfig.depth.buffer), 0);
    if (jmaskBuf != NULL) {
        releaseMaskBuf(env, &(initConfig.mask), jmaskWorkingBuffer);
    }
    if (faceInfos != NULL && initConfig.faceNum > 0) {
        delete[] initConfig.faceRect;
        delete[] initConfig.faceRip;
    }
    LOGD("<initialize> end, result %d", result);
    return result;
}

bool JniImageSegment::process(JNIEnv *env, jobject thiz, int actionType, jobject config, jobject result) {
    switch (actionType) {
    case ACTION_DO_SEGMENT:
        return doSegment(env, thiz, config, result);

    case ACTION_UNDO_SEGMENT:
        return undoSegment(env, thiz, config, result);

    case ACTION_REDO_SEGMENT:
        return redoSegment(env, thiz, config, result);

    case ACTION_SCALE_MASK:
        return scaleMask(env, thiz, config, result);

    case ACTION_CUTOUT_FORGROUND_IMG:
        return cutoutForgroundImg(env, thiz, config, result);

    case ACTION_FILL_COVER_IMG:
        return fillCoverImg(env, thiz, config, result);

    default:
        return false;
    }
}

jobject JniImageSegment::getInfo(JNIEnv *env, jobject thiz, int infoType, jobject config) {
    LOGD("<getInfo> no implementation, return NULL");
    return NULL;
}

bool JniImageSegment::doSegment(JNIEnv *env, jobject thiz, jobject config, jobject result) {
    LOGD("<doSegment> begin");
    if (m_pImageSegment == NULL) {
        LOGD("<doSegment><error> invocation before initialize or after release!");
        return false;
    }

    // find all classes
    jclass configClass = env->FindClass(
        "com/mediatek/stereoapplication/imagesegment/DoSegmentConfig");
    if (configClass == NULL) {
        LOGD("<doSegment><error> can't find class DoSegmentConfig");
        return false;
    }

    // get all fields
    jfieldID modeField = env->GetFieldID(configClass, "mode", "I");
    jfieldID scenarioField = env->GetFieldID(configClass, "scenario", "I");
    jfieldID scribbleBufField = env->GetFieldID(configClass, "scribbleBuf", "[B");
    jfieldID scribbleRectField = env->GetFieldID(configClass, "scribbleRect", "Landroid/graphics/Rect;");
    jfieldID selectPointField = env->GetFieldID(configClass, "selectPoint", "Landroid/graphics/Point;");

    // contruct native objects by java objects
    DoSegmentConfig doSegmentConfig;
    doSegmentConfig.mode = env->GetIntField(config, modeField);
    doSegmentConfig.scenario = env->GetIntField(config, scenarioField);
    jbyteArray jscribbleBuffer = (jbyteArray)env->GetObjectField(config, scribbleBufField);
    if (jscribbleBuffer != NULL) {
        doSegmentConfig.scribbleBuf = (MUINT8*)env->GetByteArrayElements(jscribbleBuffer, 0);
    } else {
        doSegmentConfig.scribbleBuf = NULL;
    }
    jobject jscribbleRect = env->GetObjectField(config, scribbleRectField);
    if (jscribbleRect != NULL) {
        convertToJniRect(env, jscribbleRect, &(doSegmentConfig.roiRect));
    }
    jobject jselectPoint = env->GetObjectField(config, selectPointField);
    if (jselectPoint != NULL) {
        convertToJniPoint(env, jselectPoint, &(doSegmentConfig.selectPoint));
    }

    MaskBuf maskBuf;

    bool res = m_pImageSegment->doSegment(&doSegmentConfig, &maskBuf);

    if (jscribbleBuffer != NULL) {
        env->ReleaseByteArrayElements(jscribbleBuffer, (jbyte *)(doSegmentConfig.scribbleBuf), 0);
    }

    // contruct java objects by native objects
    if (res) {
        convertToAppMaskBuf(env, &maskBuf, result);
    }

    LOGD("<doSegment> end, result %d", res);
    return res;
}

bool JniImageSegment::undoSegment(JNIEnv *env, jobject thiz, jobject config, jobject result) {
    if (m_pImageSegment == NULL) {
        LOGD("<undoSegment><error> invocation before initialize or after release!");
        return false;
    }
    MaskBuf maskBuf;
    m_pImageSegment->undoSegment(&maskBuf);
    convertToAppMaskBuf(env, &maskBuf, result);
    return true;
}

bool JniImageSegment::redoSegment(JNIEnv *env, jobject thiz, jobject config, jobject result) {
    if (m_pImageSegment == NULL) {
        LOGD("<redoSegment><error> invocation before initialize or after release!");
        return false;
    }
    MaskBuf maskBuf;
    m_pImageSegment->redoSegment(&maskBuf);
    convertToAppMaskBuf(env, &maskBuf, result);
    return true;
}

bool JniImageSegment::cutoutForgroundImg(JNIEnv *env, jobject thiz, jobject config, jobject result) {
    // TODO assert the below: mask sise = result bitmap size
    if (m_pImageSegment == NULL) {
        LOGD("<cutoutForgroundImg><error> invocation before initialize or after release!");
        return false;
    }

    LOGD("<cutoutForgroundImg> begin");

    BitmapMaskConfig bitmapMaskConfig;
    jbyteArray jworkingBuffer;
    jobject jworkingBitmap;
    convertToJniBitmapMaskConfig(env, config, &bitmapMaskConfig, jworkingBuffer, jworkingBitmap);
    MUINT8* pForground;
    AndroidBitmap_lockPixels(env, result, (void**) (&pForground));

    m_pImageSegment->cutoutForgroundImg(&bitmapMaskConfig, pForground);

    releaseMaskBuf(env, &(bitmapMaskConfig.mask), jworkingBuffer);
    AndroidBitmap_unlockPixels(env, jworkingBitmap);
    AndroidBitmap_unlockPixels(env, result);

    LOGD("<cutoutForgroundImg> end");
    return true;
}

bool JniImageSegment::scaleMask(JNIEnv *env, jobject thiz, jobject config, jobject result) {
    LOGD("<scaleMask> begin");

    BitmapMaskConfig bitmapMaskConfig;
    jbyteArray jworkingBuffer;
    jobject jworkingBitmap;
    convertToJniBitmapMaskConfig(env, config, &bitmapMaskConfig, jworkingBuffer, jworkingBitmap);
    ImageBuf image = bitmapMaskConfig.bitmap;
    MUINT32 width = image.width;
    MUINT32 height = image.height;
    MaskBuf maskBuf;
    maskBuf.mask = new MUINT8[width * height];
    if (m_pImageSegment != NULL) {
        m_pImageSegment->scaleMask(&bitmapMaskConfig, &maskBuf);
    } else {
        m_pImageSegment = new ImageSegment();
        m_pImageSegment->scaleMaskWithoutInit(&bitmapMaskConfig, &maskBuf);
    }

    releaseMaskBuf(env, &(bitmapMaskConfig.mask), jworkingBuffer);
    AndroidBitmap_unlockPixels(env, jworkingBitmap);
    convertToAppMaskBuf(env, &maskBuf, result);

    delete[] maskBuf.mask;

    LOGD("<scaleMask> end");
    return true;
}

bool JniImageSegment::fillCoverImg(JNIEnv *env, jobject thiz, jobject config, jobject result) {
    if (m_pImageSegment == NULL) {
        LOGD("<fillCoverImg><error> invocation before initialize or after release!");
        return false;
    }

    LOGD("<fillCoverImg> begin");
    MUINT8* pCoverImg;
    AndroidBitmap_lockPixels(env, result, (void**) (&pCoverImg));
    m_pImageSegment->fillCoverImg(pCoverImg);
    AndroidBitmap_unlockPixels(env, result);
    LOGD("<fillCoverImg> end");
    return true;
}

bool JniImageSegment::releaseMaskBuf(JNIEnv *env, MaskBuf *pMaskBuf, jbyteArray &jworkingBuffer) {
    env->ReleaseByteArrayElements(jworkingBuffer, (jbyte *)(pMaskBuf->mask), 0);
    delete (pMaskBuf->rect);
    delete (pMaskBuf->point);
    return true;
}

bool JniImageSegment::convertToJniRect(JNIEnv *env, jobject jsrcRect, Rect *pDestRect) {
    if (pDestRect == NULL) {
        LOGE("<convertToJniRect><error> pDestRect is null");
        return false;
    }
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

jobject JniImageSegment::getAppRect(JNIEnv *env, Rect *pSrcRect) {
    if (pSrcRect == NULL) {
        LOGE("<getAppRect><error> pSrcRect is null");
        return NULL;
    }
    jclass rectClass = env->FindClass("android/graphics/Rect");
    if (rectClass== NULL) {
        LOGD("<getAppRect><error> can't find class Rect");
        return NULL;
    }

    jfieldID rectLeftField = env->GetFieldID(rectClass, "left", "I");
    jfieldID rectTopField = env->GetFieldID(rectClass, "top", "I");
    jfieldID rectRightField = env->GetFieldID(rectClass, "right", "I");
    jfieldID rectBottomField = env->GetFieldID(rectClass, "bottom", "I");

    jobject jdestRect = env->AllocObject(rectClass);    // TODO alloc enough?
    env->SetIntField(jdestRect, rectLeftField, pSrcRect->left);
    env->SetIntField(jdestRect, rectTopField, pSrcRect->top);
    env->SetIntField(jdestRect, rectRightField, pSrcRect->right);
    env->SetIntField(jdestRect, rectBottomField, pSrcRect->bottom);

    return jdestRect;
}

bool JniImageSegment::convertToJniPoint(JNIEnv *env, jobject jsrcPoint, Point *pDestPoint) {
    if (pDestPoint == NULL) {
        LOGE("<convertToJniPoint><error> pDestPoint is null");
        return false;
    }
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

jobject JniImageSegment::getAppPoint(JNIEnv *env, Point *pSrcPoint) {
    if (pSrcPoint == NULL) {
        LOGE("<getAppPoint><error> pSrcPoint is null");
        return NULL;
    }
    jclass pointClass = env->FindClass("android/graphics/Point");
    if (pointClass== NULL) {
        LOGD("<convertToAppPoint><error> can't find class Point");
        return NULL;
    }

    jfieldID pointXField = env->GetFieldID(pointClass, "x", "I");
    jfieldID pointYField = env->GetFieldID(pointClass, "y", "I");

    jobject jdestPoint = env->AllocObject(pointClass);  // TODO alloc enough?
    env->SetIntField(jdestPoint, pointXField, pSrcPoint->x);
    env->SetIntField(jdestPoint, pointYField, pSrcPoint->y);

    return jdestPoint;
}

bool JniImageSegment::convertToJniMaskBuf(JNIEnv *env, jobject jsrcMaskBuf,
        MaskBuf *pDestMaskBuf, jbyteArray &jworkingBuffer) {
    if (jsrcMaskBuf == NULL || pDestMaskBuf == NULL) {
        LOGE("<convertToJniMaskBuf><error> jsrcMaskBuf or pDestMaskBuf is null");
        return false;
    }
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

bool JniImageSegment::convertToAppMaskBuf(JNIEnv *env, MaskBuf *pSrcMaskBuf, jobject &jdestMaskBuf) {
    if (pSrcMaskBuf == NULL) {
        LOGE("<convertToAppMaskBuf><error> pSrcMaskBuf is null");
        return false;
    }
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

    jbyteArray jmaskBufferArray = env->NewByteArray(pSrcMaskBuf->bufferSize);
    MUINT8 * pMaskBuffer = (MUINT8 *) env->GetByteArrayElements(jmaskBufferArray, 0);
    memcpy(pMaskBuffer, pSrcMaskBuf->mask, pSrcMaskBuf->bufferSize);
    env->ReleaseByteArrayElements(jmaskBufferArray, (jbyte *) pMaskBuffer, 0);
    env->SetObjectField(jdestMaskBuf, maskBufMaskField, jmaskBufferArray);

    env->SetIntField(jdestMaskBuf, maskBufBufferSizeField, pSrcMaskBuf->bufferSize);

    jobject jmaskRect = getAppRect(env, pSrcMaskBuf->rect);
    env->SetObjectField(jdestMaskBuf, maskBufRectField, jmaskRect);

    jobject jmaskPoint = getAppPoint(env, pSrcMaskBuf->point);
    env->SetObjectField(jdestMaskBuf, maskBufPointField, jmaskPoint);

    return true;
}

bool JniImageSegment::convertToJniDepthBuf(JNIEnv *env, jobject jsrcDepthBuf,
    DepthBuf *pDestDepthBuf, jbyteArray &jworkingBuffer) {
    if (jsrcDepthBuf == NULL || pDestDepthBuf == NULL) {
        LOGE("<convertToJniDepthBuf><error> jsrcDepthBuf or pDestDepthBuf is null");
        return false;
    }
    jclass depBufClass = env->FindClass(
            "com/mediatek/stereoapplication/DepthBuf");
    if (depBufClass == NULL) {
        LOGD("<convertToJniDepthBuf><error> can't find class DepthBuf");
        return false;
    }

    jfieldID depthBufferFieldId = env->GetFieldID(depBufClass, "buffer", "[B");
    jfieldID depthBufferSizeFieldId = env->GetFieldID(depBufClass, "bufferSize", "I");
    jfieldID depthWidthFieldId = env->GetFieldID(depBufClass, "depthWidth", "I");
    jfieldID depthHeightFieldId = env->GetFieldID(depBufClass, "depthHeight", "I");
    jfieldID metaWidthFieldId = env->GetFieldID(depBufClass, "metaWidth", "I");
    jfieldID metaHeightFieldId = env->GetFieldID(depBufClass, "metaHeight", "I");

    jbyteArray bufTemp = (jbyteArray)env->GetObjectField(jsrcDepthBuf, depthBufferFieldId);
     // release depthBufOut at caller
    jworkingBuffer = bufTemp;
    pDestDepthBuf->buffer = (MUINT8*)env->GetByteArrayElements(bufTemp, 0);
    pDestDepthBuf->bufferSize = env->GetIntField(jsrcDepthBuf, depthBufferSizeFieldId);
    pDestDepthBuf->depthWidth = env->GetIntField(jsrcDepthBuf, depthWidthFieldId);
    pDestDepthBuf->depthHeight = env->GetIntField(jsrcDepthBuf, depthHeightFieldId);
    pDestDepthBuf->metaWidth = env->GetIntField(jsrcDepthBuf, metaWidthFieldId);
    pDestDepthBuf->metaHeight = env->GetIntField(jsrcDepthBuf, metaHeightFieldId);

    return true;
}

bool JniImageSegment::convertToJniBitmapMaskConfig(JNIEnv *env, jobject jsrcConfig,
        BitmapMaskConfig *pDestConfig, jbyteArray &jworkingBuffer, jobject &jworkingBitmap) {
    if (jsrcConfig == NULL || pDestConfig == NULL) {
        LOGE("<convertToJniBitmapMaskConfig><error> jsrcConfig or pDestConfig is null");
        return false;
    }
    // find all classes
    jclass configClass = env->FindClass(
        "com/mediatek/stereoapplication/imagesegment/BitmapMaskConfig");
    if (configClass == NULL) {
        LOGD("<convertToJniBitmapMaskConfig><error> can't find class BitmapMaskConfig");
        return false;
    }

    jclass bitmapClass = env->FindClass("android/graphics/Bitmap");
    if (bitmapClass == NULL) {
        LOGD("<convertToJniBitmapMaskConfig><error> can't find class Bitmap");
        return false;
    }

    // get all fields
    jfieldID bitmapField = env->GetFieldID(configClass, "bitmap",
            "Landroid/graphics/Bitmap;");
    jfieldID maskBufField = env->GetFieldID(configClass, "maskBuf",
            "Lcom/mediatek/stereoapplication/MaskBuf;");

    // contruct native objects by java objects
    jworkingBitmap = env->GetObjectField(jsrcConfig, bitmapField);
    if (jworkingBitmap == NULL) {
        LOGD("<convertToJniBitmapMaskConfig><error> bitmap is NULL!!");
        return false;
    }
    convertBitmapToImageBuf(env, jworkingBitmap, &(pDestConfig->bitmap));

    jobject jmaskBuf = env->GetObjectField(jsrcConfig, maskBufField);
    if (jmaskBuf == NULL) {
        LOGD("<convertToJniBitmapMaskConfig><error> maskBuf is NULL.");
        return false;
    } else {
        convertToJniMaskBuf(env, jmaskBuf, &(pDestConfig->mask), jworkingBuffer);
    }

    return true;
}

bool JniImageSegment::convertBitmapToImageBuf(JNIEnv *env, jobject jbitmap, ImageBuf *pImageBuf) {
    if (jbitmap == NULL || pImageBuf == NULL) {
        LOGE("<convertBitmapToImageBuf><error> jbitmap or pImageBuf is null");
        return false;
    }
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


