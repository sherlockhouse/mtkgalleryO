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

#include "JniImageRefocus.h"
#include "gralloc_extra.h"
#include <GraphicBuffer.h>

using namespace stereo;
using namespace android;

JniImageRefocus::JniImageRefocus()
{
    LOGD("JniImageRefocus");
    m_ionHandle = mt_ion_open("jpeg_decoder");
    m_pImageRefocus = new ImageRefocus(m_ionHandle);
}

JniImageRefocus::~JniImageRefocus()
{
    LOGD("~JniImageRefocus");
    delete m_pImageRefocus;
    if (m_pFaceRect != NULL) {
        free(m_pFaceRect);
    }
    if (m_ionHandle >= 0) {
        ion_close(m_ionHandle);
    }
}

bool JniImageRefocus::initialize(JNIEnv *env, jobject thiz, jobject config)
{
    jclass configClass = env->FindClass(
             "com/mediatek/stereoapplication/imagerefocus/InitConfig");
    if (configClass == NULL)
    {
        LOGD("<initialize><error> can't find class InitConfig");
        return false;
    }

    RefocusInitConfig initConfig;

    jfieldID imageBufField = env->GetFieldID(configClass, "imageBuf", "[B");
    jfieldID imageBufSizeField = env->GetFieldID(configClass, "imageBufSize", "I");

    jfieldID depthBufField = env->GetFieldID(configClass, "depthBuf",
            "Lcom/mediatek/stereoapplication/DepthBuf;");
    jfieldID jpsWidthField = env->GetFieldID(configClass, "jpsWidth", "I");
    jfieldID jpsHeightField = env->GetFieldID(configClass, "jpsHeight", "I");
    jfieldID maskWidthField = env->GetFieldID(configClass, "maskWidth", "I");
    jfieldID maskHeightField = env->GetFieldID(configClass, "maskHeight", "I");
    jfieldID posXField = env->GetFieldID(configClass, "posX", "I");
    jfieldID posYField = env->GetFieldID(configClass, "posY", "I");
    jfieldID viewWidthField = env->GetFieldID(configClass, "viewWidth", "I");
    jfieldID viewHeightField = env->GetFieldID(configClass, "viewHeight", "I");
    jfieldID mainCamPosField = env->GetFieldID(configClass, "mainCamPos", "I");
    jfieldID focusCoordXField = env->GetFieldID(configClass, "focusCoordX", "I");
    jfieldID focusCoordYField = env->GetFieldID(configClass, "focusCoordY", "I");
    jfieldID imageOrientationField = env->GetFieldID(configClass, "imageOrientation", "I");
    jfieldID depthOrientationField = env->GetFieldID(configClass, "depthOrientation", "I");

    jfieldID dofField = env->GetFieldID(configClass, "dof", "I");
    jfieldID ldcBufField = env->GetFieldID(configClass, "ldcBuf",
            "Lcom/mediatek/stereoapplication/ImageBuf;");

    jfieldID minDacDataField = env->GetFieldID(configClass, "minDacData", "I");
    jfieldID maxDacDataField = env->GetFieldID(configClass, "maxDacData", "I");
    jfieldID curDacDataField = env->GetFieldID(configClass, "curDacData", "I");
    jfieldID faceNumField = env->GetFieldID(configClass, "faceNum", "I");
    jfieldID faceRectField = env->GetFieldID(configClass, "faceRect",
            "[Landroid/graphics/Rect;");
    jfieldID faceRipField = env->GetFieldID(configClass, "faceRip", "[I");
    jfieldID isFdField = env->GetFieldID(configClass, "isFd", "Z");
    jfieldID ratioField = env->GetFieldID(configClass, "ratio", "F");

    jfieldID convOffsetField = env->GetFieldID(configClass, "convOffset", "F");
    jfieldID focusTypeField = env->GetFieldID(configClass, "focusType", "I");
    jfieldID focusLeftField = env->GetFieldID(configClass, "focusLeft", "I");
    jfieldID focusTopField = env->GetFieldID(configClass, "focusTop", "I");
    jfieldID focusRightField = env->GetFieldID(configClass, "focusRight", "I");
    jfieldID focusBottomField = env->GetFieldID(configClass, "focusBottom", "I");

    jbyteArray imageBuf = (jbyteArray)env->GetObjectField(config, imageBufField);
    if (imageBuf == NULL)
    {
        LOGD("<initialize><error> image buffer is NULL!!");
        return false;
    }

    jobject depthBuf = env->GetObjectField(config, depthBufField);
    if (depthBuf == NULL)
    {
        LOGD("<initialize><error> depthBuf is NULL!!");
        return false;
    }

    jobject ldcBuf = env->GetObjectField(config, ldcBufField);
    if (ldcBuf == NULL)
    {
        LOGD("<initialize><warning> ldcBuf is NULL!!");
    }

    initConfig.pImageBuf = (MUINT8*)env->GetByteArrayElements(imageBuf, 0);
    initConfig.imageBufSize = env->GetIntField(config, imageBufSizeField);

    jbyteArray depthBufArrayOut;
    jbyteArray ldcBufArrayOut;
    if (!convertToJniDepthBuf(env, depthBuf, depthBufArrayOut, &(initConfig.depth)))
    {
        LOGD("<initialize><error> convertToJniDepthBuf fail");
        return false;
    }
    if (!convertToJniImageBuf(env, ldcBuf, ldcBufArrayOut, &(initConfig.ldcBuf)))
    {
        LOGD("<initialize><error> convertToJniImageBuf fail");
        return false;
    }
    initConfig.jpsWidth = env->GetIntField(config, jpsWidthField);
    initConfig.jpsHeight = env->GetIntField(config, jpsHeightField);
    initConfig.maskWidth = env->GetIntField(config, maskWidthField);
    initConfig.maskHeight = env->GetIntField(config, maskHeightField);
    initConfig.posX = env->GetIntField(config, posXField);
    initConfig.posY = env->GetIntField(config, posYField);
    initConfig.viewWidth = env->GetIntField(config, viewWidthField);
    initConfig.viewHeight = env->GetIntField(config, viewHeightField);
    initConfig.mainCamPos = env->GetIntField(config, mainCamPosField);
    initConfig.focusCoordX= env->GetIntField(config, focusCoordXField);
    initConfig.focusCoordY = env->GetIntField(config, focusCoordYField);
    initConfig.imageOrientation = env->GetIntField(config, imageOrientationField);
    initConfig.depthOrientation = env->GetIntField(config, depthOrientationField);
    initConfig.dof = env->GetIntField(config, dofField);

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

    initConfig.convOffset = env->GetFloatField(config, convOffsetField);
    initConfig.focusType = env->GetIntField(config, focusTypeField);
    initConfig.focusLeft = env->GetIntField(config, focusLeftField);
    initConfig.focusTop = env->GetIntField(config, focusTopField);
    initConfig.focusRight = env->GetIntField(config, focusRightField);
    initConfig.focusBottom = env->GetIntField(config, focusBottomField);

    LOGD("<initialize> convOffset: %f, focusType: %d, focusLeft: %d, focusTop: %d, focusRight: %d, focusBottom: %d",
            initConfig.convOffset, initConfig.focusType, initConfig.focusLeft, initConfig.focusTop,
            initConfig.focusRight, initConfig.focusBottom);
    bool result = m_pImageRefocus->initialize(&initConfig);

    env->ReleaseByteArrayElements(imageBuf, (jbyte *)(initConfig.pImageBuf), 0);
    env->ReleaseByteArrayElements(depthBufArrayOut, (jbyte *)(initConfig.depth.buffer), 0);
    if (pFaceRip != NULL)
    {
        env->ReleaseIntArrayElements(faceRipArray, pFaceRip, 0);
    }
    if (m_pFaceRect != NULL)
    {
        // env->ReleaseObjectArrayElement(faceRectArray, m_pFaceRect, 0);//?
    }
    if (initConfig.ldcBuf.buffer != NULL)
    {
        env->ReleaseByteArrayElements(ldcBufArrayOut, (jbyte *)(initConfig.ldcBuf.buffer), 0);
    }
    LOGD("<initialize> result %d", result);
    return result;
}

bool JniImageRefocus::process(JNIEnv *env, jobject thiz, int actionType, jobject config,
        jobject result)
{
    switch (actionType)
    {
        case ACTION_DO_REFOCUS:
            return doRefocus(env, thiz, config, result);

        case ACTION_ENCODE_REFOCUS_IMAGE:
            return encodeRefocusImg(env, thiz, config, result);
        default:
            return false;
    }
}

jobject JniImageRefocus::getInfo(JNIEnv *env, jobject thiz, int infoType, jobject config)
{
    return NULL;
}

bool JniImageRefocus::doRefocus(JNIEnv *env, jobject thiz, jobject config, jobject result)
{
    jclass configClass = env->FindClass(
            "com/mediatek/stereoapplication/imagerefocus/DoRefocusConfig");
    if (configClass == NULL)
    {
        LOGD("<doRefocus><error> can't find class DoRefocusConfig");
        return false;
    }
    jfieldID xCoordFieldId = env->GetFieldID(configClass, "xCoord", "I");
    jfieldID yCoordFieldId = env->GetFieldID(configClass, "yCoord", "I");
    jfieldID depthOfFieldFieldId = env->GetFieldID(configClass, "depthOfField", "I");
    jfieldID formatFieldId = env->GetFieldID(configClass, "format", "I");

    DoRefocusConfig doRefocusConfig;
    doRefocusConfig.xCoord = env->GetIntField(config, xCoordFieldId);
    doRefocusConfig.yCoord= env->GetIntField(config, yCoordFieldId);
    doRefocusConfig.depthOfField= env->GetIntField(config, depthOfFieldFieldId);
    doRefocusConfig.format= env->GetIntField(config, formatFieldId);

    if (!m_pImageRefocus->doRefocus(&doRefocusConfig)) {
        LOGD("<doRefocus><error> do refocus fail!!");
        return false;
    }
    ImageBuf refocusImage;
    memset(&refocusImage, 0, sizeof(refocusImage));
    if (!m_pImageRefocus->getRefocusImageSize(&refocusImage)) {
        LOGD("<doRefocus><error> error refocus image size!");
        return false;
    }

    return doRefocusConfig.format == OUT_FORMAT_YUV420 ?
            getYuvRefocusImage(env, &refocusImage, result) :
            getRgbaRefocusImage(env, &refocusImage, result);
}

bool JniImageRefocus::getYuvRefocusImage(JNIEnv *env, ImageBuf* pRefocusImage, jobject result) {
    jclass grahic_class = env->FindClass("android/graphics/GraphicBuffer");
    if (grahic_class == NULL) {
        LOGD("<getYuvRefocusImage><error> can't find class GraphicBuffer");
        return false;
    }
    jmethodID cid = env->GetStaticMethodID(grahic_class, "create", "(IIII)Landroid/graphics/GraphicBuffer;");
    if (cid == NULL) {
        LOGD("<getYuvRefocusImage><error> can't find static method create");
        return false;
    }
    jobject graphic_buf = env->CallStaticObjectMethod(grahic_class, cid, pRefocusImage->width,
            pRefocusImage->height, YV12, HW_TEXTURE | SW_WRITE_RARELY);
    if (graphic_buf == NULL) {
        LOGD("<getYuvRefocusImage><error> call method create fail!!");
        return false;
    }

    sp<GraphicBuffer> gb(graphicBufferForJavaObject(env, graphic_buf));
    gralloc_extra_ion_sf_info_t info;
    gralloc_extra_query(gb->getNativeBuffer()->handle,
            GRALLOC_EXTRA_GET_IOCTL_ION_SF_INFO, &info);
    gralloc_extra_sf_set_status(&info, GRALLOC_EXTRA_MASK_YUV_COLORSPACE,
            GRALLOC_EXTRA_BIT_YUV_BT601_FULL);
    gralloc_extra_perform(gb->getNativeBuffer()->handle,
            GRALLOC_EXTRA_SET_IOCTL_ION_SF_INFO, &info);

    status_t err = NO_ERROR;
    android_ycbcr ycbcr = android_ycbcr();
    err = gb->lockYCbCr(GRALLOC_USAGE_SW_WRITE_RARELY, &ycbcr);
    LOGD("<getYuvRefocusImage> ystride: %d, cstride: %d, gb.width: %d, gb.height: %d",
            ycbcr.ystride, ycbcr.cstride, gb->getWidth(), gb->getHeight());
    memset((uint8_t*)ycbcr.y, 0x00, ycbcr.ystride * gb->getHeight());
    memset((uint8_t*)ycbcr.cb, 0x00, ycbcr.cstride * gb->getHeight()/2);
    memset((uint8_t*) ycbcr.cr, 0x00, ycbcr.cstride * gb->getHeight()/2);

    bool strideEqualsWidth = false;
    if (ycbcr.ystride == gb->getWidth() && ycbcr.cstride == gb->getWidth() / 2) {
        strideEqualsWidth = true;
        pRefocusImage->buffer = (MUINT8*)ycbcr.y;
    } else {
        strideEqualsWidth = false;
        pRefocusImage->buffer = new MUINT8[pRefocusImage->width
                * pRefocusImage->height * 3 / 2];
    }
    LOGD("<getYuvRefocusImage> strideEqualsWidth: %d", strideEqualsWidth);
    m_pImageRefocus->getRefocusImageBuf(pRefocusImage, true);

    // for certain case, stride doesn't equal width:
    // if so, we must copy pixel of width to dest buffer for each row
    // then dest buffer need add offset of stride
    if (!strideEqualsWidth && gb->getWidth() <= ycbcr.ystride
            && (gb->getWidth() / 2) <= ycbcr.cstride) {
        // copy y
        LOGD("<getYuvRefocusImage> start to copy y");
        for (MINT32 i = 0; i < gb->getHeight(); i++) {
            memcpy((uint8_t*) ycbcr.y + i * ycbcr.ystride,
                    pRefocusImage->buffer + i * gb->getWidth(), gb->getWidth());
        }

        // copy v
        LOGD("<getYuvRefocusImage> start to copy v");
        MINT32 vSrcOffset = pRefocusImage->width * pRefocusImage->height;
        for (MINT32 i = 0; i < (gb->getHeight() / 2); i++) {
            memcpy((uint8_t*) ycbcr.cr + i * ycbcr.cstride,
                    pRefocusImage->buffer + vSrcOffset + i * gb->getWidth() / 2,
                    gb->getWidth() / 2);
        }

        // copy u
        LOGD("<getYuvRefocusImage> start to copy u");
        MINT32 uSrcOffset = pRefocusImage->width * pRefocusImage->height * 5 / 4;
        for (MINT32 i = 0; i < (gb->getHeight() / 2); i++) {
            memcpy((uint8_t*) ycbcr.cb + i * ycbcr.cstride,
                    pRefocusImage->buffer + uSrcOffset + i * gb->getWidth() / 2,
                    gb->getWidth() / 2);
        }
        delete[] pRefocusImage->buffer;
        pRefocusImage->buffer = NULL;
    }
    err = gb->unlock();

    jclass refocus_image_class = env->FindClass(
            "com/mediatek/stereoapplication/imagerefocus/RefocusImage");
    if (refocus_image_class == NULL) {
        LOGD("<getYuvRefocusImage><error> can't find class RefocusImage");
        return false;
    }
    jfieldID widthField = env->GetFieldID(refocus_image_class, "width", "I");
    jfieldID heightField = env->GetFieldID(refocus_image_class, "height", "I");
    jfieldID graphicField = env->GetFieldID(refocus_image_class, "image",
            "Ljava/lang/Object;");

    env->SetObjectField(result, graphicField, graphic_buf);
    env->SetIntField(result, widthField, pRefocusImage->width);
    env->SetIntField(result, heightField, pRefocusImage->height);
    return true;
}

bool JniImageRefocus::getRgbaRefocusImage(JNIEnv *env, ImageBuf* pRefocusImage, jobject result) {
    pRefocusImage->bufferSize = pRefocusImage->width * pRefocusImage->height * 4;
    jbyteArray jBufferArray = env->NewByteArray(pRefocusImage->bufferSize);
    MUINT8 * pBuffer = (MUINT8 *) env->GetByteArrayElements(jBufferArray, 0);
    pRefocusImage->buffer = pBuffer;
    if (!m_pImageRefocus->getRefocusImageBuf(pRefocusImage, false)) {
        LOGD("<doRefocus><error> getRefocusImage fail");
        return false;
     }

    jclass imageBufClass = env->FindClass(
            "com/mediatek/stereoapplication/ImageBuf");
    if (imageBufClass == NULL) {
        LOGD("<getRgbaRefocusImage><error> can't find class ImageBuf");
        return false;
    }

    jfieldID bufferField = env->GetFieldID(imageBufClass, "buffer", "[B");
    jfieldID bufferSizeField = env->GetFieldID(imageBufClass, "bufferSize", "I");
    jfieldID widthField = env->GetFieldID(imageBufClass, "width", "I");
    jfieldID heightField = env->GetFieldID(imageBufClass, "height", "I");

    env->ReleaseByteArrayElements(jBufferArray, (jbyte *) pBuffer, 0);
    env->SetObjectField(result, bufferField, jBufferArray);

    env->SetIntField(result, bufferSizeField, pRefocusImage->bufferSize);
    env->SetIntField(result, widthField, pRefocusImage->width);
    env->SetIntField(result, heightField, pRefocusImage->height);
    return true;
}

bool JniImageRefocus::encodeRefocusImg(JNIEnv *env, jobject thiz, jobject config,
        jobject result)
{
    jclass configClass = env->FindClass("java/lang/Integer");
    if (configClass == NULL)
    {
        LOGD("<encodeRefocusImg><error> can't find class Integer");
        return false;
    }
    jmethodID intValueMethod = env->GetMethodID(configClass, "intValue", "()I");
    MUINT32 format = env->CallIntMethod(config, intValueMethod);

    IonInfo *pOutJpgIonBuffer = NULL;
    if (m_pImageRefocus->encodeRefocusImg(&pOutJpgIonBuffer, format))
    {
        LOGD("<encodeRefocusImg> Out bufferSize %d, width %d, height %d",
                pOutJpgIonBuffer->dataSize, pOutJpgIonBuffer->width, pOutJpgIonBuffer->height);

        jclass imageBufClass = env->FindClass("com/mediatek/stereoapplication/ImageBuf");
        if (imageBufClass == NULL)
        {
            LOGD("<encodeRefocusImg><error> can't find class ImageBuf");
            return false;
        }

        jfieldID bufferField = env->GetFieldID(imageBufClass, "buffer", "[B");
        jfieldID bufferSizeField = env->GetFieldID(imageBufClass, "bufferSize", "I");
        jfieldID widthField = env->GetFieldID(imageBufClass, "width", "I");
        jfieldID heightField = env->GetFieldID(imageBufClass, "height", "I");

        jbyteArray jBufferArray = env->NewByteArray(pOutJpgIonBuffer->dataSize);
        MUINT8 * pBuffer = (MUINT8 *) env->GetByteArrayElements(jBufferArray, 0);
        memcpy(pBuffer, (MUINT8 *)pOutJpgIonBuffer->virAddr, pOutJpgIonBuffer->dataSize);
        env->ReleaseByteArrayElements(jBufferArray, (jbyte *) pBuffer, 0);
        env->SetObjectField(result, bufferField, jBufferArray);

        env->SetIntField(result, bufferSizeField, pOutJpgIonBuffer->dataSize);
        env->SetIntField(result, widthField, pOutJpgIonBuffer->width);
        env->SetIntField(result, heightField, pOutJpgIonBuffer->height);

        JpegCodec::destroyIon(pOutJpgIonBuffer, m_ionHandle);
        LOGD("<encodeRefocusImg> encodeRefocusImg success");
        return true;
    }
    JpegCodec::destroyIon(pOutJpgIonBuffer, m_ionHandle);
    LOGD("<encodeRefocusImg><error> encodeRefocusImg fail");
    return false;
}


bool JniImageRefocus::convertToJniDepthBuf(JNIEnv *env, jobject srcDepthBuf,
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


bool JniImageRefocus::convertToJniImageBuf(JNIEnv *env, jobject srcImageBuf,
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
