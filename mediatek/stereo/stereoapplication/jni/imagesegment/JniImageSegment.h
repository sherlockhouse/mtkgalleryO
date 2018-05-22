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

#ifndef JNIIMAGESEGMENT_H_
#define JNIIMAGESEGMENT_H_

#include "ImageSegment.h"
#include "IProcessor.h"

namespace stereo {

#define ACTION_DO_SEGMENT 4
#define ACTION_UNDO_SEGMENT 5
#define ACTION_REDO_SEGMENT 6
#define ACTION_SCALE_MASK 7
#define ACTION_CUTOUT_FORGROUND_IMG 8
#define ACTION_FILL_COVER_IMG 9

#define TAG "JniImageSegment"

class JniImageSegment : public IProcessor {
public:
    JniImageSegment();
    virtual ~JniImageSegment();

    bool initialize(JNIEnv *env, jobject thiz, jobject config);

    bool process(JNIEnv *env, jobject thiz, int actionType, jobject config, jobject result);

    jobject getInfo(JNIEnv *env, jobject thiz, int infoType, jobject config);

private:
    bool doSegment(JNIEnv *env, jobject thiz, jobject config, jobject result);

    bool undoSegment(JNIEnv *env, jobject thiz, jobject config, jobject result);

    bool redoSegment(JNIEnv *env, jobject thiz, jobject config, jobject result);

    bool cutoutForgroundImg(JNIEnv *env, jobject thiz, jobject config, jobject result);

    bool scaleMask(JNIEnv *env, jobject thiz, jobject config, jobject result);

    bool fillCoverImg(JNIEnv *env, jobject thiz, jobject config, jobject result);

    static bool releaseMaskBuf(JNIEnv *env, MaskBuf *pMaskBuf, jbyteArray &jworkingBuffer);

    static bool convertToJniRect(JNIEnv *env, jobject jsrcRect, Rect *pDestRect);

    static jobject getAppRect(JNIEnv *env, Rect *pSrcRect);

    static bool convertToJniPoint(JNIEnv *env, jobject jsrcPoint, Point *pDestPoint);

    static jobject getAppPoint(JNIEnv *env, Point *pSrcPoint);

    static bool convertToJniMaskBuf(JNIEnv *env, jobject jsrcMaskBuf,
            MaskBuf *pDestMaskBuf, jbyteArray &jworkingBuffer);

    static bool convertToAppMaskBuf(JNIEnv *env, MaskBuf *pSrcMaskBuf, jobject &jdestMaskBuf);

    static bool convertToJniDepthBuf(JNIEnv *env, jobject jsrcDepthBuf,
        DepthBuf *pDestDepthBuf, jbyteArray &jworkingBuffer);

    static bool convertToJniBitmapMaskConfig(JNIEnv *env, jobject jsrcConfig,
            BitmapMaskConfig *pDestConfig, jbyteArray &jworkingBuffer, jobject &jworkingBitmap);

    static bool convertBitmapToImageBuf(JNIEnv *env, jobject jbitmap, ImageBuf *pImageBuf);

    ImageSegment *m_pImageSegment = NULL;
};
}

#endif /* JNIIMAGESEGMENT_H_ */
