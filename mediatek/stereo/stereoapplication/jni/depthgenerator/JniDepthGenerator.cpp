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

#include "JniDepthGenerator.h"

using namespace stereo;

JniDepthGenerator::JniDepthGenerator()
{
    m_ionHandle = mt_ion_open("jpeg_decoder");
    m_pGenerator = new DepthGenerator(m_ionHandle);
}

JniDepthGenerator::~JniDepthGenerator()
{
    delete m_pGenerator;
    if (m_pFaceRect != NULL) {
        free(m_pFaceRect);
    }
    if (m_ionHandle >= 0) {
        ion_close(m_ionHandle);
    }
}

bool JniDepthGenerator::initialize(JNIEnv *env, jobject thiz, jobject config)
{
    LOGD("<initialize> begin");
    jclass configClass = env->FindClass(
        "com/mediatek/stereoapplication/depthgenerator/InitConfig");
    if (configClass == NULL)
    {
        LOGD("<initialize><error> can't find class InitConfig");
        return false;
    }
    jfieldID imageBufField = env->GetFieldID(configClass, "imageBuf", "[B");
    jfieldID imageBufSizeField = env->GetFieldID(configClass, "imageBufSize", "I");
    jfieldID jpsBufField = env->GetFieldID(configClass, "jpsBuf",
            "Lcom/mediatek/stereoapplication/ImageBuf;");
    jfieldID maskBufField = env->GetFieldID(configClass, "maskBuf",
            "Lcom/mediatek/stereoapplication/ImageBuf;");
    jfieldID ldcBufField = env->GetFieldID(configClass, "ldcBuf",
            "Lcom/mediatek/stereoapplication/ImageBuf;");
    jfieldID posXField = env->GetFieldID(configClass, "posX", "I");
    jfieldID posYField = env->GetFieldID(configClass, "posY", "I");
    jfieldID viewWidthField = env->GetFieldID(configClass, "viewWidth", "I");
    jfieldID viewHeightField = env->GetFieldID(configClass, "viewHeight", "I");
    jfieldID mainCamposField = env->GetFieldID(configClass, "mainCampos", "I");
    jfieldID imageOrientationField = env->GetFieldID(configClass, "imageOrientation", "I");
    jfieldID depthOrientationField = env->GetFieldID(configClass, "depthOrientation", "I");

    jfieldID minDacDataField = env->GetFieldID(configClass, "minDacData", "I");
    jfieldID maxDacDataField = env->GetFieldID(configClass, "maxDacData", "I");
    jfieldID curDacDataField = env->GetFieldID(configClass, "curDacData", "I");
    jfieldID faceNumField = env->GetFieldID(configClass, "faceNum", "I");
    jfieldID faceRectField = env->GetFieldID(configClass, "faceRect",
            "[Landroid/graphics/Rect;");
    jfieldID faceRipField = env->GetFieldID(configClass, "faceRip", "[I");
    jfieldID isFdField = env->GetFieldID(configClass, "isFd", "Z");
    jfieldID ratioField = env->GetFieldID(configClass, "ratio", "F");

    jbyteArray imageBuf = (jbyteArray)env->GetObjectField(config, imageBufField);
    jobject jpsBufObject = env->GetObjectField(config, jpsBufField);
    jobject maskBufObject = env->GetObjectField(config, maskBufField);
    jobject ldcBufObject = env->GetObjectField(config, ldcBufField);
    if (ldcBufObject == NULL)
    {
        LOGD("<initialize><warning> ldcBuf is NULL!!");
    }
    jbyteArray jpsBufArrayOut;
    jbyteArray maskBufArrayOut;
    jbyteArray ldcBufArrayOut;

    GeneratorInitConfig initConfig;
    initConfig.pImageBuf = (MUINT8*)env->GetByteArrayElements(imageBuf, 0);
    initConfig.imageBufSize = env->GetIntField(config, imageBufSizeField);

    if (!convertToJniImageBuf(env, jpsBufObject, jpsBufArrayOut, &(initConfig.jps)) ||
            !convertToJniImageBuf(env, maskBufObject, maskBufArrayOut, &(initConfig.mask)) ||
            !convertToJniImageBuf(env, ldcBufObject, ldcBufArrayOut, &(initConfig.ldc)))
    {
        LOGD("<initialize><error> convertToJniImageBuf fail");
        return false;
    }
    LOGD("<initialize> jps width %d, jps height %d, mask width %d, mask height %d, "
            "ldc width %d, ldc height %d", initConfig.jps.width, initConfig.jps.height,
            initConfig.mask.width, initConfig.mask.height, initConfig.ldc.width,
            initConfig.ldc.height);

    initConfig.posX = env->GetIntField(config, posXField);
    initConfig.posY = env->GetIntField(config, posYField);
    initConfig.viewWidth = env->GetIntField(config, viewWidthField);
    initConfig.viewHeight = env->GetIntField(config, viewHeightField);
    initConfig.mainCamPos = env->GetIntField(config, mainCamposField);
    initConfig.imageOrientation = env->GetIntField(config, imageOrientationField);
    initConfig.depthOrientation = env->GetIntField(config, depthOrientationField);

    initConfig.minDacData = env->GetIntField(config, minDacDataField);
    initConfig.maxDacData = env->GetIntField(config, maxDacDataField);
    initConfig.curDacData = env->GetIntField(config, curDacDataField);
    initConfig.faceNum = env->GetIntField(config, faceNumField);

    jobjectArray faceRectArray = (jobjectArray)env->GetObjectField(config, faceRectField);
    jintArray faceRipArray = (jintArray)env->GetObjectField(config, faceRipField);

    jclass rect_class = env->FindClass("android/graphics/Rect");
    if (rect_class == NULL) {
        return false;
    }
    jfieldID left_field = env->GetFieldID(rect_class, "left", "I");
    jfieldID right_field = env->GetFieldID(rect_class, "right", "I");
    jfieldID top_field = env->GetFieldID(rect_class, "top", "I");
    jfieldID bottom_field = env->GetFieldID(rect_class, "bottom", "I");
    if (m_pFaceRect != NULL) {
        free(m_pFaceRect);
    }
    m_pFaceRect = (RectImgSeg*) malloc(sizeof(RectImgSeg) * initConfig.faceNum);
    for (int i = 0; i < (int)initConfig.faceNum; i++) {
        jobject rect = env->GetObjectArrayElement(faceRectArray, i);
        if (rect == NULL) {
            continue;
        }
        m_pFaceRect[i].left = env->GetIntField(rect, left_field);
        m_pFaceRect[i].top = env->GetIntField(rect, top_field);
        m_pFaceRect[i].right = env->GetIntField(rect, right_field);
        m_pFaceRect[i].bottom = env->GetIntField(rect, bottom_field);
    }
    jint* pFaceRip = initConfig.faceNum > 0 ? env->GetIntArrayElements(faceRipArray, NULL)
            : NULL;
    initConfig.facePos = m_pFaceRect;
    initConfig.faceRip = pFaceRip;
    initConfig.isFd = env->GetBooleanField(config, isFdField);
    initConfig.ratio = env->GetFloatField(config, ratioField);

    bool result = m_pGenerator->initialize(&initConfig);

    env->ReleaseByteArrayElements(imageBuf, (jbyte *)(initConfig.pImageBuf), 0);
    env->ReleaseByteArrayElements(jpsBufArrayOut, (jbyte *)(initConfig.jps.buffer), 0);
    env->ReleaseByteArrayElements(maskBufArrayOut, (jbyte *)(initConfig.mask.buffer), 0);
    if (pFaceRip != NULL)
    {
        env->ReleaseIntArrayElements(faceRipArray, pFaceRip, 0);
    }
    if (m_pFaceRect != NULL)
    {
        // env->ReleaseObjectArrayElement(faceRectArray, m_pFaceRect, 0);//?
    }
    if (initConfig.ldc.buffer != NULL)
    {
        env->ReleaseByteArrayElements(ldcBufArrayOut, (jbyte *)(initConfig.ldc.buffer), 0);
    }
    LOGD("<initialize> end, result %d", result);
    return result;
}

bool JniDepthGenerator::process(JNIEnv *env, jobject thiz, int actionType, jobject config,
        jobject result)
{
    switch (actionType)
    {
        case ACTION_GENERATE_DEPTH:
            return generateDepth(env, thiz, config, result);
        default:
            return NULL;
    }
}

jobject JniDepthGenerator::getInfo(JNIEnv *env, jobject thiz, int infoType, jobject config)
{
    LOGD("<getInfo> no implementation, return NULL");
    return NULL;
}

bool JniDepthGenerator::generateDepth(JNIEnv *env, jobject thiz, jobject config, jobject result)
{
    LOGD("<generateDepth> begin");
    DepthInfo depthInfo;
    jobject xmpDepthObject;
    jobject depthBufObject;

    jclass resultClass = env->FindClass(
            "com/mediatek/stereoapplication/depthgenerator/DepthInfo");
    if (resultClass == NULL)
    {
        LOGD("<generateDepth><error> can't find class DepthInfo");
        return false;
    }

    jfieldID xmpDepthField = env->GetFieldID(resultClass, "xmpDepth",
            "Lcom/mediatek/stereoapplication/ImageBuf;");
    jfieldID depthBufField = env->GetFieldID(resultClass, "depthBuf",
            "Lcom/mediatek/stereoapplication/DepthBuf;");

    if (m_pGenerator->generateDepth(&depthInfo) &&
            convertToAppImageBuf(env, &(depthInfo.xmpDepth), &xmpDepthObject) &&
            convertToAppDepthBuf(env, &(depthInfo.depth), &depthBufObject))
    {
        env->SetObjectField(result, xmpDepthField, xmpDepthObject);
        env->SetObjectField(result, depthBufField, depthBufObject);
        delete depthInfo.xmpDepth.buffer;
        delete depthInfo.depth.buffer;
        LOGD("<generateDepth> generateDepth success");
        return true;
    }

    delete depthInfo.xmpDepth.buffer;
    delete depthInfo.depth.buffer;
    LOGD("<generateDepth><error> generateDepth fail");
    return false;
}

bool JniDepthGenerator::convertToJniImageBuf(JNIEnv *env, jobject srcImageBuf,
    jbyteArray &imageBufOut, ImageBuf *pDestImageBuf)
{
    if (srcImageBuf == NULL)
    {
        pDestImageBuf->buffer = NULL;
        pDestImageBuf->bufferSize = 0;
        pDestImageBuf->width = 0;
        pDestImageBuf->height = 0;
        LOGD("<convertToJniImageBuf> srcImageBuf is null, return invalid Buf");
        return true;
    }

    jclass imageBufClass = env->FindClass("com/mediatek/stereoapplication/ImageBuf");
    if (imageBufClass == NULL)
    {
        LOGD("<convertToJniImageBuf><error> can't find class ImageBuf");
        return false;
    }

    jfieldID bufferField = env->GetFieldID(imageBufClass, "buffer", "[B");
    jfieldID bufferSizeField = env->GetFieldID(imageBufClass, "bufferSize", "I");
    jfieldID widthField = env->GetFieldID(imageBufClass, "width", "I");
    jfieldID heightField = env->GetFieldID(imageBufClass, "height", "I");

    jbyteArray bufTemp = (jbyteArray)env->GetObjectField(srcImageBuf, bufferField);
    // release imageBufOut at caller
    imageBufOut = bufTemp;
    pDestImageBuf->buffer = (MUINT8*)env->GetByteArrayElements(bufTemp, 0);
    pDestImageBuf->bufferSize = env->GetIntField(srcImageBuf, bufferSizeField);
    pDestImageBuf->width = env->GetIntField(srcImageBuf, widthField);
    pDestImageBuf->height = env->GetIntField(srcImageBuf, heightField);

    return true;
}

bool JniDepthGenerator::convertToJniDepthBuf(JNIEnv *env, jobject srcDepthBuf,
    jbyteArray &depthBufOut, DepthBuf *pDestDepthBuf)
{
    jclass depBufClass = env->FindClass(
            "com/mediatek/stereoapplication/DepthBuf");
    if (depBufClass == NULL)
    {
        LOGD("<convertToJniDepthBuf><error> can't find class DepthBuf");
        return false;
    }

    jfieldID depthBufferFieldId = env->GetFieldID(depBufClass, "buffer", "[B");
    jfieldID depthBufferSizeFieldId = env->GetFieldID(depBufClass, "bufferSize", "I");
    jfieldID depthWidthFieldId = env->GetFieldID(depBufClass, "depthWidth", "I");
    jfieldID depthHeightFieldId = env->GetFieldID(depBufClass, "depthHeight", "I");
    jfieldID metaWidthFieldId = env->GetFieldID(depBufClass, "metaWidth", "I");
    jfieldID metaHeightFieldId = env->GetFieldID(depBufClass, "metaHeight", "I");

    jbyteArray bufTemp = (jbyteArray)env->GetObjectField(srcDepthBuf, depthBufferFieldId);
     // release depthBufOut at caller
    depthBufOut = bufTemp;
    pDestDepthBuf->buffer = (MUINT8*)env->GetByteArrayElements(bufTemp, 0);
    pDestDepthBuf->bufferSize = env->GetIntField(srcDepthBuf, depthBufferSizeFieldId);
    pDestDepthBuf->depthWidth = env->GetIntField(srcDepthBuf, depthWidthFieldId);
    pDestDepthBuf->depthHeight = env->GetIntField(srcDepthBuf, depthHeightFieldId);
    pDestDepthBuf->metaWidth = env->GetIntField(srcDepthBuf, metaWidthFieldId);
    pDestDepthBuf->metaHeight = env->GetIntField(srcDepthBuf, metaHeightFieldId);

    return true;
}

bool JniDepthGenerator::convertToAppImageBuf(JNIEnv *env, ImageBuf *pSrcImageBuf,
    jobject *pDestImageBuf)
{
    jclass imageBufClass = env->FindClass("com/mediatek/stereoapplication/ImageBuf");
    if (imageBufClass == NULL)
    {
        LOGD("<convertToAppImageBuf><error> can't find class ImageBuf");
        return false;
    }

    jmethodID imageBufConstructor = env->GetMethodID(imageBufClass, "<init>", "()V");
    jfieldID bufferField = env->GetFieldID(imageBufClass, "buffer", "[B");
    jfieldID bufferSizeField = env->GetFieldID(imageBufClass, "bufferSize", "I");
    jfieldID widthField = env->GetFieldID(imageBufClass, "width", "I");
    jfieldID heightField = env->GetFieldID(imageBufClass, "height", "I");
    *pDestImageBuf = env->NewObject(imageBufClass, imageBufConstructor);

    jbyteArray jBufferArray = env->NewByteArray(pSrcImageBuf->bufferSize);
    MUINT8 * pBuffer = (MUINT8 *) env->GetByteArrayElements(jBufferArray, 0);
    memcpy(pBuffer, pSrcImageBuf->buffer, pSrcImageBuf->bufferSize);
    env->ReleaseByteArrayElements(jBufferArray, (jbyte *) pBuffer, 0);
    env->SetObjectField(*pDestImageBuf, bufferField, jBufferArray);

    env->SetIntField(*pDestImageBuf, bufferSizeField, pSrcImageBuf->bufferSize);
    env->SetIntField(*pDestImageBuf, widthField, pSrcImageBuf->width);
    env->SetIntField(*pDestImageBuf, heightField, pSrcImageBuf->height);

    return true;
}

bool JniDepthGenerator::convertToAppDepthBuf(JNIEnv *env, DepthBuf *pSrcDepthBuf,
    jobject *pDestDepthBuf)
{
    jclass depBufClass = env->FindClass("com/mediatek/stereoapplication/DepthBuf");
    if (depBufClass == NULL)
    {
        LOGD("<convertToAppDepthBuf><error> can't find class DepthBuf");
        return false;
    }

    jmethodID depthBufConstructor = env->GetMethodID(depBufClass, "<init>", "()V");
    jfieldID depthBufferFieldId = env->GetFieldID(depBufClass, "buffer", "[B");
    jfieldID depthBufferSizeFieldId = env->GetFieldID(depBufClass, "bufferSize", "I");
    jfieldID depthWidthFieldId = env->GetFieldID(depBufClass, "depthWidth", "I");
    jfieldID depthHeightFieldId = env->GetFieldID(depBufClass, "depthHeight", "I");
    jfieldID metaWidthFieldId = env->GetFieldID(depBufClass, "metaWidth", "I");
    jfieldID metaHeightFieldId = env->GetFieldID(depBufClass, "metaHeight", "I");
    *pDestDepthBuf = env->NewObject(depBufClass, depthBufConstructor);

    jbyteArray jBufferArray = env->NewByteArray(pSrcDepthBuf->bufferSize);
    MUINT8 * pBuffer = (MUINT8 *) env->GetByteArrayElements(jBufferArray, 0);
    memcpy(pBuffer, pSrcDepthBuf->buffer, pSrcDepthBuf->bufferSize);
    env->ReleaseByteArrayElements(jBufferArray, (jbyte *) pBuffer, 0);
    env->SetObjectField(*pDestDepthBuf, depthBufferFieldId, jBufferArray);

    env->SetIntField(*pDestDepthBuf, depthBufferSizeFieldId, pSrcDepthBuf->bufferSize);
    env->SetIntField(*pDestDepthBuf, depthWidthFieldId, pSrcDepthBuf->depthWidth);
    env->SetIntField(*pDestDepthBuf, depthHeightFieldId, pSrcDepthBuf->depthHeight);
    env->SetIntField(*pDestDepthBuf, metaWidthFieldId, pSrcDepthBuf->metaWidth);
    env->SetIntField(*pDestDepthBuf, metaHeightFieldId, pSrcDepthBuf->metaHeight);
    return true;
}
