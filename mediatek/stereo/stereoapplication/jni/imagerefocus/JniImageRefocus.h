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

#ifndef JNIIMAGEREFOCUS_H_
#define JNIIMAGEREFOCUS_H_

#include "IProcessor.h"
#include "ImageRefocus.h"
#include "Log.h"

namespace stereo
{
#define TAG "Rf/JniImageRefocus"

#define FEATURE_TYPE_IMAGE_REFOCUS 0

#define ACTION_DO_REFOCUS 1

#define ACTION_ENCODE_REFOCUS_IMAGE 2

#define OUT_FORMAT_RGBA8888 0
#define OUT_FORMAT_YUV420 1
// refer to java class ImageFormat.YV12
#define YV12 0x32315659
// refer to java class GraphicBuffer.USAGE_HW_TEXTURE
#define HW_TEXTURE 0x100
// refer to java class GraphicBuffer.USAGE_SW_WRITE_RARELY
#define SW_WRITE_RARELY 0x20

// const char* FEATURE_NAME = "ImageRefocus";

class JniImageRefocus : public IProcessor
{
public:
    JniImageRefocus();
    virtual ~JniImageRefocus();

    bool initialize(JNIEnv *env, jobject thiz, jobject config);

    bool process(JNIEnv *env, jobject thiz, int actionType, jobject config, jobject result);

    jobject getInfo(JNIEnv *env, jobject thiz, int infoType, jobject config);

private:
    ImageRefocus* m_pImageRefocus = NULL;
    RectImgSeg *m_pFaceRect = NULL;
    int32_t m_ionHandle;

    bool doRefocus(JNIEnv *env, jobject thiz, jobject config, jobject result);
    bool encodeRefocusImg(JNIEnv *env, jobject thiz, jobject config, jobject result);
    bool convertToJniImageBuf(JNIEnv *env, jobject srcImageBuf, jbyteArray &imageBufOut,
            ImageBuf *pDestImageBuf);
    bool convertToJniDepthBuf(JNIEnv *env, jobject srcDepthBuf, jbyteArray &depthBufOut,
            DepthBuf *pDestDepthBuf);
    bool getYuvRefocusImage(JNIEnv *env, ImageBuf* pRefocusImage, jobject result);
    bool getRgbaRefocusImage(JNIEnv *env, ImageBuf* pRefocusImage, jobject result);
};
}

#endif /* JNIIMAGEREFOCUS_H_ */
