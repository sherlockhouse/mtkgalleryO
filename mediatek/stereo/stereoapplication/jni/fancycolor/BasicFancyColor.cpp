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
 * MediaTek Inc. (C) 2015. All rights reserved.
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

#include "BasicFancyColor.h"
#include <android/bitmap.h>

using namespace stereo;

#define TAG "Fc/BasicFancyColor"

namespace stereo {
    #define BYTES_PER_PIXCEL 4

    MINT32 getCurrentTime() {
        struct timeval tv;
        gettimeofday(&tv, NULL);
        return (MINT32)(tv.tv_sec * 1000 + tv.tv_usec / 1000);
    }
}   // namespace stereo

BasicFancyColor::BasicFancyColor() {
    m_pBasicRender = new MyRenderer();
}

BasicFancyColor::~BasicFancyColor() {
    mEffectNames.clear();
    delete m_pBasicRender;
}

bool BasicFancyColor::generateEffectImg(JNIEnv *env, jobject thiz, jobject config, jobject result,
        GenerateEffectImgConfig* pConfig, MCHAR* pResultBitmap) {
    if (pConfig == NULL || pResultBitmap == NULL) {
        LOGD("<generateEffectImg> parameter invalid.");
        return false;
    }

    bool res = false;
    MCHAR* effectName = pConfig->effectName;
    if (strcmp(effectName, IMAGE_FILTER_NORMAL) == 0) {
        res = imageFilterNormal(pConfig, pResultBitmap);
    } else if (strcmp(effectName, IMAGE_FILTER_SIHOUETTE) == 0) {
        res = imageFilterSihouette(pConfig, pResultBitmap);
    } else if (strcmp(effectName, IMAGE_FILTER_WHITEBOARD) == 0) {
        res = imageFilterWhiteBoard(pConfig, pResultBitmap);
    } else if (strcmp(effectName, IMAGE_FILTER_BLACKBOARD) == 0) {
        res = imageFilterBlackBoard(pConfig, pResultBitmap);
    } else if (strcmp(effectName, IMAGE_FILTER_NEGATIVE) == 0) {
        res = imageFilterNegative(pConfig, pResultBitmap);
    } else if (strcmp(effectName, IMAGE_FILTER_MONO_CHROME) == 0) {
        res = imageFilterMonoChrome(env, thiz, pConfig, pResultBitmap);
    } else if (strcmp(effectName, IMAGE_FILTER_POSTERIZE) == 0) {
        res = imageFilterPosterize(env, thiz, result, pConfig, pResultBitmap);
    }

    return res;
}

vector<MCHAR*> BasicFancyColor::getAllEffectsNames() {
    if (mEffectNames.empty()) {
        mEffectNames.push_back(IMAGE_FILTER_NORMAL);
        mEffectNames.push_back(IMAGE_FILTER_SIHOUETTE);
        mEffectNames.push_back(IMAGE_FILTER_WHITEBOARD);
        mEffectNames.push_back(IMAGE_FILTER_BLACKBOARD);
        mEffectNames.push_back(IMAGE_FILTER_NEGATIVE);
        mEffectNames.push_back(IMAGE_FILTER_MONO_CHROME);
        mEffectNames.push_back(IMAGE_FILTER_POSTERIZE);
    }
    return mEffectNames;
}

bool BasicFancyColor::imageFilterNormal(GenerateEffectImgConfig* pConfig, MCHAR* pResultBitmap) {
    LOGD("<imageFilterNormal>");
    return true;
}

bool BasicFancyColor::imageFilterNegative(GenerateEffectImgConfig* pConfig, MCHAR* pResultBitmap) {
    LOGD("<imageFilterNegative>");
    MCHAR* filter = NULL;
    MUINT32 width = pConfig->bitmap.width;
    MUINT32 height = pConfig->bitmap.height;
    MUINT32 len = width * height * 4;
    if (len > 0) {
        filter = new MCHAR[len];
    }
    if (filter == NULL) {
        LOGD("<imageFilterStroke>allocate memory fail!!!");
        return false;
    }

    memcpy(filter, pConfig->bitmap.buffer, len);
    m_pBasicRender->imageFilterNegative(filter, width, height);
    m_pBasicRender->merge2Bitmap(pResultBitmap, filter, pConfig->mask.mask, width, height);

    delete[] filter;
    return true;
}

bool BasicFancyColor::imageFilterBlackBoard(GenerateEffectImgConfig* pConfig, MCHAR* pResultBitmap) {
    LOGD("<imageFilterBlackBoard>");
    MCHAR* filter = NULL;
    MUINT32 width = pConfig->bitmap.width;
    MUINT32 height = pConfig->bitmap.height;
    MUINT32 len = width * height * 4;
    if (len > 0) {
        filter = new MCHAR[len];
    }
    if (filter == NULL) {
        LOGD("<imageFilterBlackBoard>allocate memory fail!!!");
        return false;
    }
    MFLOAT p = 1.0;

    memcpy(filter, pConfig->bitmap.buffer, len);
    m_pBasicRender->ImageFilterBlackBoard(filter, width, height, p);
    m_pBasicRender->merge2Bitmap(pResultBitmap, filter, pConfig->mask.mask, width, height);

    delete[] filter;
    return true;
}

bool BasicFancyColor::imageFilterWhiteBoard(GenerateEffectImgConfig* pConfig, MCHAR* pResultBitmap) {
    LOGD("<imageFilterWhiteBoard>");
    MCHAR* filter = NULL;
    MUINT32 width = pConfig->bitmap.width;
    MUINT32 height = pConfig->bitmap.height;
    MUINT32 len = width * height * 4;
    if (len > 0) {
        filter = new MCHAR[len];
    }
    if (filter == NULL) {
        LOGD("<imageFilterWhiteBoard>allocate memory fail!!!");
        return false;
    }
    MFLOAT p = 1.0;

    memcpy(filter, pConfig->bitmap.buffer, len);
    m_pBasicRender->ImageFilterWhiteBoard(filter, width, height, p);
    m_pBasicRender->merge2Bitmap(pResultBitmap, filter, pConfig->mask.mask, width, height);

    delete[] filter;
    return true;
}

bool BasicFancyColor::imageFilterSihouette(GenerateEffectImgConfig* pConfig, MCHAR* pResultBitmap) {
    LOGD("<imageFilterSihouette>");
    m_pBasicRender->ImageFilterSihouette(pResultBitmap, pConfig->mask.mask,
                                         pConfig->bitmap.width, pConfig->bitmap.height);
    return true;
}

bool BasicFancyColor::imageFilterMonoChrome(JNIEnv *env, jobject thiz,
        GenerateEffectImgConfig* pConfig, MCHAR* pResultBitmap) {
    LOGD("<imageFilterMonoChrome>");
    MCHAR* filter = NULL;
    const static MINT32 H_VALUE = 230;  // 180 + 50;
    const static MINT32 BIT_SHIFT_BLUE = 0;
    const static MINT32 BIT_SHIFT_GREEN = 8;
    const static MINT32 BIT_SHIFT_RED = 16;
    const static MINT32 WHITE_COLOR_MASK = 0xFF;

    jclass colorClass = env->FindClass("android/graphics/Color");
    jmethodID methodField = env->GetStaticMethodID(colorClass, "HSVToColor", "([F)I");
    MFLOAT hsv[3] = { H_VALUE, 1, 1 };
    jfloatArray jhsv = env->NewFloatArray(3);
    env->SetFloatArrayRegion(jhsv, 0, 3, hsv);
    MINT32 rgb = env->CallStaticIntMethod(colorClass, methodField, jhsv);
    MINT32 r = WHITE_COLOR_MASK & (rgb >> BIT_SHIFT_RED);
    MINT32 g = WHITE_COLOR_MASK & (rgb >> BIT_SHIFT_GREEN);
    MINT32 b = WHITE_COLOR_MASK & (rgb >> BIT_SHIFT_BLUE);

    MUINT32 width = pConfig->bitmap.width;
    MUINT32 height = pConfig->bitmap.height;
    MUINT32 len = width * height * 4;
    if (len > 0) {
        filter = new MCHAR[len];
    }
    if (filter == NULL) {
        LOGD("<imageFilterMonoChrome>allocate memory fail!!!");
        return false;
    }

    memcpy(filter, pConfig->bitmap.buffer, len);
    m_pBasicRender->ImageFilterBwFilter(filter, width, height, r, g, b);
    m_pBasicRender->merge2Bitmap(pResultBitmap, filter, pConfig->mask.mask, width, height);

    delete[] filter;
    return true;
}

bool BasicFancyColor::imageFilterPosterize(JNIEnv *env, jobject thiz, jobject result,
        GenerateEffectImgConfig* pConfig, MCHAR* pResultBitmap) {
    const static MINT32 SMALL_BM_THRESHOLD = 64;
    const static MINT32 LARGE_BM_THRESHOLD = 256;
    const static MINT32 K_MEANS_P = 4;
    MCHAR* filter = NULL;
    MINT32 w = pConfig->bitmap.width;
    MINT32 h = pConfig->bitmap.height;

    jobject jlargeBmDs = result;
    jobject jsmallBmDs = result;

    jclass bitmapClass = env->FindClass("android/graphics/Bitmap");
    jmethodID methodField = env->GetStaticMethodID(bitmapClass, "createScaledBitmap",
            "(Landroid/graphics/Bitmap;IIZ)Landroid/graphics/Bitmap;");

    // find width/height for larger downsampled bitmap
    MINT32 lw = w;
    MINT32 lh = h;
    while (lw > LARGE_BM_THRESHOLD && lh > LARGE_BM_THRESHOLD) {
        lw /= 2;
        lh /= 2;
    }
    if (lw != w) {
        jlargeBmDs = env->CallStaticObjectMethod(bitmapClass, methodField, result, lw, lh, JNI_TRUE);
    }

    // find width/height for smaller downsampled bitmap
    MINT32 sw = lw;
    MINT32 sh = lh;
    while (sw > SMALL_BM_THRESHOLD && sh > SMALL_BM_THRESHOLD) {
        sw /= 2;
        sh /= 2;
    }
    if (sw != lw) {
        jsmallBmDs = env->CallStaticObjectMethod(bitmapClass, methodField, jlargeBmDs, sw, sh, JNI_TRUE);
    }

    MINT32 seed = getCurrentTime();
    MCHAR* larger_ds_dst = 0;
    MCHAR* smaller_ds_dst = 0;
    AndroidBitmap_lockPixels(env, jlargeBmDs, (void**) &larger_ds_dst);
    AndroidBitmap_lockPixels(env, jsmallBmDs, (void**) &smaller_ds_dst);

    MUINT32 width = pConfig->bitmap.width;
    MUINT32 height = pConfig->bitmap.height;
    MUINT32 len = width * height * 4;
    if (len > 0) {
        filter = new MCHAR[len];
    }
    if (filter == NULL) {
        LOGD("<imageFilterPosterize>allocate memory fail!!!");
        return false;
    }

    memcpy(filter, pConfig->bitmap.buffer, len);
    m_pBasicRender->ImageFilterKMeans(filter, width, height, larger_ds_dst, lw, lh, smaller_ds_dst, sw,
            sh, K_MEANS_P, seed);
    m_pBasicRender->merge2Bitmap(pResultBitmap, filter, pConfig->mask.mask, width, height);

    AndroidBitmap_unlockPixels(env, jsmallBmDs);
    AndroidBitmap_unlockPixels(env, jlargeBmDs);

    delete[] filter;
    return true;
}

BasicFancyColor::MyRenderer::MyRenderer() {
    if (access(FANCYCOLOR_DUMP_PATH, 0) != -1) {
        m_pDebugHelper = new DebugHelper;   // new DebugUtils(FANCYCOLOR_DUMP_PATH);
    }
}

BasicFancyColor::MyRenderer::~MyRenderer() {
    if (NULL != m_pDebugHelper) {
        delete m_pDebugHelper;
        m_pDebugHelper = NULL;
    }
}

void BasicFancyColor::MyRenderer::imageFilterNegative(MCHAR *bitmap, MINT32 width, MINT32 height) {
    MINT32 tot_len = height * width * 4;
    MINT32 i;
    MCHAR * dst = bitmap;
    for (i = 0; i < tot_len; i += 4) {
        dst[RED] = 255 - dst[RED];
        dst[GREEN] = 255 - dst[GREEN];
        dst[BLUE] = 255 - dst[BLUE];
    }

    if (NULL != m_pDebugHelper) {
        m_pDebugHelper->dumpBufferToFile("imageFilterNegative.ppm", (MUINT8*)bitmap, width * height * 3);
    }
}

void BasicFancyColor::MyRenderer::ImageFilterWBalance(MCHAR *bitmap,
        MINT32 width, MINT32 height, MINT32 locX, MINT32 locY) {
    MINT32 i;
    MINT32 len = width * height * 4;
    MUINT8 * rgb = (MUINT8 *) bitmap;
    MINT32 wr;
    MINT32 wg;
    MINT32 wb;

    if (locX == -1)
        estmateWhite(rgb, len, &wr, &wg, &wb);
    else
        estmateWhiteBox(rgb, width, height, locX, locY, &wr, &wg, &wb);

    MINT32 min = MIN(wr, MIN(wg, wb));
    MINT32 max = MAX(wr, MAX(wg, wb));
    MFLOAT avg = (min + max) / 2.f;
    MFLOAT scaleR = avg / wr;
    MFLOAT scaleG = avg / wg;
    MFLOAT scaleB = avg / wb;

    for (i = 0; i < len; i += 4) {
        MINT32 r = rgb[RED];
        MINT32 g = rgb[GREEN];
        MINT32 b = rgb[BLUE];

        MFLOAT Rc = r * scaleR;
        MFLOAT Gc = g * scaleG;
        MFLOAT Bc = b * scaleB;

        rgb[RED] = clamp(Rc);
        rgb[GREEN] = clamp(Gc);
        rgb[BLUE] = clamp(Bc);
    }
}

void BasicFancyColor::MyRenderer::ImageFilterBwFilter(MCHAR *bitmap, MINT32 width, MINT32 height,
        MINT32 rw, MINT32 gw, MINT32 bw) {
    MUINT8 *rgb = (MUINT8 *) bitmap;
    MFLOAT sr = rw;
    MFLOAT sg = gw;
    MFLOAT sb = bw;

    MFLOAT min = MIN(sg, sb);
    min = MIN(sr, min);
    MFLOAT max = MAX(sg, sb);
    max = MAX(sr, max);
    MFLOAT avg = (min + max) / 2;
    sb /= avg;
    sg /= avg;
    sr /= avg;
    MINT32 i;
    MINT32 len = width * height * 4;

    for (i = 0; i < len; i += 4) {
        MFLOAT r = sr * rgb[RED];
        MFLOAT g = sg * rgb[GREEN];
        MFLOAT b = sb * rgb[BLUE];
        min = MIN(g, b);
        min = MIN(r, min);
        max = MAX(g, b);
        max = MAX(r, max);
        avg = (min + max) / 2;
        rgb[RED] = CLAMP(avg);
        rgb[GREEN] = rgb[RED];
        rgb[BLUE] = rgb[RED];
    }
}

void BasicFancyColor::MyRenderer::ImageFilterBlackBoard(MCHAR *bitmap, MINT32 width, MINT32 height, MFLOAT p) {
    // using contrast function:
    // f(v) = exp(-alpha * v^beta)
    // use beta ~ 1

    MFLOAT const alpha = 5.0f;
    MFLOAT const beta = p;
    MFLOAT const c_min = 100.0f;
    MFLOAT const c_max = 500.0f;

    // pixels must be 4 bytes
    MCHAR * dst = bitmap;

    MINT32 j, k;
    MCHAR *ptr = bitmap;
    MINT32 row_stride = 4 * width;

    // set 2 row buffer (avoids bitmap copy)
    MINT32 buf_len = 2 * row_stride;
    MCHAR buf[buf_len];
    MINT32 buf_row_ring = 0;

    // set initial buffer to black
    memset(buf, 0, buf_len * sizeof(MCHAR));
    // set initial alphas
    for (j = 3; j < buf_len; j += 4) {
        *(buf + j) = 255;
    }

    // apply sobel filter
    for (j = 1; j < height - 1; j++) {
        for (k = 1; k < width - 1; k++) {
            MINT32 loc = j * row_stride + k * 4;
            MFLOAT bestx = 0.0f;
            MINT32 l;
            for (l = 0; l < 3; l++) {
                MFLOAT tmp = 0.0f;
                tmp += *(ptr + (loc - row_stride + 4 + l));
                tmp += *(ptr + (loc + 4 + l)) * 2.0f;
                tmp += *(ptr + (loc + row_stride + 4 + l));
                tmp -= *(ptr + (loc - row_stride - 4 + l));
                tmp -= *(ptr + (loc - 4 + l)) * 2.0f;
                tmp -= *(ptr + (loc + row_stride - 4 + l));
                if (fabs(tmp) > fabs(bestx)) {
                    bestx = tmp;
                }
            }

            MFLOAT besty = 0.0f;
            for (l = 0; l < 3; l++) {
                MFLOAT tmp = 0.0f;
                tmp -= *(ptr + (loc - row_stride - 4 + l));
                tmp -= *(ptr + (loc - row_stride + l)) * 2.0f;
                tmp -= *(ptr + (loc - row_stride + 4 + l));
                tmp += *(ptr + (loc + row_stride - 4 + l));
                tmp += *(ptr + (loc + row_stride + l)) * 2.0f;
                tmp += *(ptr + (loc + row_stride + 4 + l));
                if (fabs(tmp) > fabs(besty)) {
                    besty = tmp;
                }
            }

            // compute gradient magnitude
            MFLOAT mag = sqrt(bestx * bestx + besty * besty);

            // clamp
            mag = MIN(MAX(c_min, mag), c_max);

            // scale to [0, 1]
            mag = (mag - c_min) / (c_max - c_min);

            MFLOAT ret = 1.0f - exp(-alpha * pow(mag, beta));
            ret = 255 * ret;

            MINT32 off = k * 4;
            *(buf + buf_row_ring + off) = ret;
            *(buf + buf_row_ring + off + 1) = ret;
            *(buf + buf_row_ring + off + 2) = ret;
            *(buf + buf_row_ring + off + 3) = *(ptr + loc + 3);
        }

        buf_row_ring += row_stride;
        buf_row_ring %= buf_len;
        if (j - 1 >= 0) {
            memcpy((dst + row_stride * (j - 1)), (buf + buf_row_ring), row_stride * sizeof(MCHAR));
        }
    }
    buf_row_ring += row_stride;
    buf_row_ring %= buf_len;
    MINT32 second_last_row = row_stride * (height - 2);
    memcpy((dst + second_last_row), (buf + buf_row_ring), row_stride * sizeof(MCHAR));

    // set last row to black
    MINT32 last_row = row_stride * (height - 1);
    memset((dst + last_row), 0, row_stride * sizeof(MCHAR));
    // set alphas
    for (j = 3; j < row_stride; j += 4) {
        *(dst + last_row + j) = 255;
    }

    if (NULL != m_pDebugHelper) {
        m_pDebugHelper->dumpBufferToFile("ImageFilterBlackBoard.ppm", (MUINT8*)bitmap, width * height * 3);
    }
}

void BasicFancyColor::MyRenderer::ImageFilterWhiteBoard(MCHAR *bitmap, MINT32 width, MINT32 height, MFLOAT p) {
    ImageFilterBlackBoard(bitmap, width, height, p);
    imageFilterNegative(bitmap, width, height);
}

void BasicFancyColor::MyRenderer::ImageFilterKMeans(MCHAR *bitmap, MINT32 width, MINT32 height,
        MCHAR *large_ds_bitmap, MINT32 lwidth, MINT32 lheight,
        MCHAR *small_ds_bitmap, MINT32 swidth, MINT32 sheight, MINT32 p, MINT32 seed) {
    MUINT8 *dst = (MUINT8 *) bitmap;
    MUINT8 *small_ds = (MUINT8 *) small_ds_bitmap;
    MUINT8 *large_ds = (MUINT8 *) large_ds_bitmap;

    // setting for small bitmap
    MINT32 len = swidth * sheight * 4;
    MINT32 dimension = 3;
    MINT32 stride = 4;
    MINT32 iterations = 20;
    MINT32 k = p;
    MUINT32 s = seed;
    MUINT8 finalCentroids[k * stride];

    // get initial picks from small downsampled image
    runKMeans<MUINT8, MINT32>(k, finalCentroids, small_ds, len, dimension, stride, iterations, s);

    len = lwidth * lheight * 4;
    iterations = 8;
    MUINT8 nextCentroids[k * stride];

    // run kmeans on large downsampled image
    runKMeansWithPicks<MUINT8, MINT32>(k, nextCentroids, large_ds, len, dimension, stride, iterations,
            finalCentroids);

    len = width * height * 4;

    // apply to final image
    applyCentroids<MUINT8, MINT32>(k, nextCentroids, dst, len, dimension, stride);
}

void BasicFancyColor::MyRenderer::ImageFilterSihouette(MCHAR *bitmap, MUINT8 *mask, MINT32 width, MINT32 height) {
    MCHAR *dst = bitmap;
    for (MINT32 i = 0; i < width * height; i++) {
        if (mask[i] > 0) {
            dst[i * 4 + 0] = 0;
            dst[i * 4 + 1] = 0;
            dst[i * 4 + 2] = 0;
        }
    }
}

void BasicFancyColor::MyRenderer::merge2Bitmap(MCHAR *dst, MCHAR *ori, MUINT8 *mask,
                                               MINT32 width, MINT32 height) {
    MINT32 tot_len = height * width * 4;
    MINT32 i, j;
    for (i = 0, j = 0; i < tot_len; i += 4, j++) {
        dst[RED] = dst[RED] * ((MFLOAT) mask[j] / 255) + ori[RED] * (1 - (MFLOAT) mask[j] / 255);
        dst[GREEN] = dst[GREEN] * ((MFLOAT) mask[j] / 255) + ori[GREEN] * (1 - (MFLOAT) mask[j] / 255);
        dst[BLUE] = dst[BLUE] * ((MFLOAT) mask[j] / 255) + ori[BLUE] * (1 - (MFLOAT) mask[j] / 255);
    }
}

void BasicFancyColor::MyRenderer::estmateWhite(MUINT8 *src, MINT32 len, MINT32 *wr, MINT32 *wb, MINT32 *wg) {
    MINT32 STEP = 4;
    MINT32 RANGE = 256;
    MINT32 *histR = new MINT32[256];
    MINT32 *histG = new MINT32[256];;
    MINT32 *histB = new MINT32[256];;
    MINT32 i;
    for (i = 0; i < 255; i++) {
        histR[i] = histG[i] = histB[i] = 0;
    }

    for (i = 0; i < len; i += STEP) {
        histR[(src[RED])]++;
        histG[(src[GREEN])]++;
        histB[(src[BLUE])]++;
    }
    MINT32 min_r = -1, min_g = -1, min_b = -1;
    MINT32 max_r = 0, max_g = 0, max_b = 0;
    MINT32 sum_r = 0, sum_g = 0, sum_b = 0;

    for (i = 1; i < RANGE - 1; i++) {
        MINT32 r = histR[i];
        MINT32 g = histG[i];
        MINT32 b = histB[i];
        sum_r += r;
        sum_g += g;
        sum_b += b;

        if (r > 0) {
            if (min_r < 0)
                min_r = i;
            max_r = i;
        }
        if (g > 0) {
            if (min_g < 0)
                min_g = i;
            max_g = i;
        }
        if (b > 0) {
            if (min_b < 0)
                min_b = i;
            max_b = i;
        }
    }

    MINT32 sum15r = 0, sum15g = 0, sum15b = 0;
    MINT32 count15r = 0, count15g = 0, count15b = 0;
    MINT32 tmp_r = 0, tmp_g = 0, tmp_b = 0;

    for (i = RANGE - 2; i > 0; i--) {
        MINT32 r = histR[i];
        MINT32 g = histG[i];
        MINT32 b = histB[i];
        tmp_r += r;
        tmp_g += g;
        tmp_b += b;

        if ((tmp_r > sum_r / 20) && (tmp_r < sum_r / 5)) {
            sum15r += r * i;
            count15r += r;
        }
        if ((tmp_g > sum_g / 20) && (tmp_g < sum_g / 5)) {
            sum15g += g * i;
            count15g += g;
        }
        if ((tmp_b > sum_b / 20) && (tmp_b < sum_b / 5)) {
            sum15b += b * i;
            count15b += b;
        }
    }
    delete[] histR;
    delete[] histG;
    delete[] histB;

    if ((count15r > 0) && (count15g > 0) && (count15b > 0)) {
        *wr = sum15r / count15r;
        *wb = sum15g / count15g;
        *wg = sum15b / count15b;
    } else {
        *wg = *wb = *wr = 255;
    }
}

void BasicFancyColor::MyRenderer::estmateWhiteBox(MUINT8 *src, MINT32 iw, MINT32 ih,
        MINT32 x, MINT32 y, MINT32 *wr, MINT32 *wb, MINT32 *wg) {
    MINT32 r = 0;
    MINT32 g = 0;
    MINT32 b = 0;
    MINT32 sum = 0;
    MINT32 xp = 0;
    MINT32 yp = 0;
    MINT32 bounds = 5;
    if (x < 0)
        x = bounds;
    if (y < 0)
        y = bounds;
    if (x >= (iw - bounds))
        x = (iw - bounds - 1);
    if (y >= (ih - bounds))
        y = (ih - bounds - 1);
    MINT32 startx = x - bounds;
    MINT32 starty = y - bounds;
    MINT32 endx = x + bounds;
    MINT32 endy = y + bounds;

    for (yp = starty; yp < endy; yp++) {
        for (xp = startx; xp < endx; xp++) {
            MINT32 i = 4 * (xp + yp * iw);
            r += src[RED];
            g += src[GREEN];
            b += src[BLUE];
            sum++;
        }
    }
    if (0 == sum) {
        return;
    }
    *wr = r / sum;
    *wg = g / sum;
    *wb = b / sum;
}

template<typename T>
void BasicFancyColor::MyRenderer::initialPickHeuristicRandom(MINT32 k, T values[], MINT32 len,
        MINT32 dimension, MINT32 stride, T dst[], MUINT32 seed) {
    MINT32 x = 0;
    MINT32 z = 0;
    MINT32 cntr = 0;
    MINT32 num_vals = len / stride;
    srand(seed);
    MUINT32 r_vals[k];
    MUINT32 r;

    memset(r_vals, 0, sizeof(r_vals));
    for (x = 0; x < k; x++) {
        /// M: if num_vals <=  k ,Randomly chosen value should go into infinite loops.@{
        if (num_vals <= k) {
            r = (MUINT32) x % num_vals;
        } else {
            /// @}
            // ensure randomly chosen value is unique
            MINT32 r_check = 0;
            while (r_check == 0) {
                r = (MUINT32) rand() % num_vals;
                r_check = 1;
                for (z = 0; z < x; z++) {
                    if (r == r_vals[z]) {
                        r_check = 0;
                    }
                }
            }
            r_vals[x] = r;
            r *= stride;
        }
        // set dst to be randomly chosen value
        set<T, T>(dst + cntr, values + r, dimension);
        cntr += stride;
    }
}

template<typename T, typename N>
MINT32 BasicFancyColor::MyRenderer::calculateNewCentroids(MINT32 k, T values[], MINT32 len,
        MINT32 dimension, MINT32 stride, T oldCenters[], T dst[]) {
    MINT32 x, pop_size;
    pop_size = k * stride;
    MINT32 popularities[k];
    N tmp[pop_size];

    // zero popularities
    memset(popularities, 0, sizeof(MINT32) * k);
    // zero dst, and tmp
    for (x = 0; x < pop_size; x++) {
        tmp[x] = 0;
    }

    // put summation for each k in tmp
    for (x = 0; x < len; x += stride) {
        MINT32 best = findClosest<T, N>(values + x, oldCenters, dimension, stride, pop_size);
        add<T, N>(values + x, tmp + best, dimension);
        popularities[best / stride]++;
    }

    MINT32 ret = 0;
    MINT32 y;
    // divide to get centroid and set dst to result
    for (x = 0; x < pop_size; x += stride) {
        divide<N, MINT32>(tmp + x, popularities[x / stride], dimension);
        for (y = 0; y < dimension; y++) {
            if ((dst + x)[y] != (T) ((tmp + x)[y])) {
                ret = 1;
            }
        }
        set(dst + x, tmp + x, dimension);
    }
    return ret;
}

template<typename T, typename N>
void BasicFancyColor::MyRenderer::runKMeansWithPicks(MINT32 k, T finalCentroids[], T values[], MINT32 len,
        MINT32 dimension, MINT32 stride, MINT32 iterations, T initialPicks[]) {
    MINT32 k_len = k * stride;
    MINT32 x;

    // zero newCenters
    for (x = 0; x < k_len; x++) {
        finalCentroids[x] = 0;
    }

    T *c1 = initialPicks;
    T *c2 = finalCentroids;
    T *temp;
    MINT32 ret = 1;
    for (x = 0; x < iterations; x++) {
        ret = calculateNewCentroids<T, N>(k, values, len, dimension, stride, c1, c2);
        temp = c1;
        c1 = c2;
        c2 = temp;
        if (ret == 0) {
            x = iterations;
        }
    }
    set<T, T>(finalCentroids, c1, dimension);
}

template<typename T, typename N>
void BasicFancyColor::MyRenderer::runKMeans(MINT32 k, T finalCentroids[], T values[], MINT32 len,
        MINT32 dimension, MINT32 stride, MINT32 iterations, MUINT32 seed) {
    MINT32 k_len = k * stride;
    T initialPicks[k_len];
    initialPickHeuristicRandom<T>(k, values, len, dimension, stride, initialPicks, seed);

    runKMeansWithPicks<T, N>(k, finalCentroids, values, len, dimension, stride, iterations, initialPicks);
}

template<typename T, typename N>
void BasicFancyColor::MyRenderer::applyCentroids(MINT32 k, T centroids[], T values[],
        MINT32 len, MINT32 dimension, MINT32 stride) {
    MINT32 x, pop_size;
    pop_size = k * stride;
    for (x = 0; x < len; x += stride) {
        MINT32 best = findClosest<T, N>(values + x, centroids, dimension, stride, pop_size);
        set<T, T>(values + x, centroids + best, dimension);
    }
}
