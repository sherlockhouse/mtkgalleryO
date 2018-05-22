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

#ifndef BASICFANCYCOLOR_H_
#define BASICFANCYCOLOR_H_

#include <stdlib.h>
#include <math.h>
#include <string.h>

#include "Log.h"
#include "IFancyColor.h"
#include "StereoType.h"
#include "../DebugHelper.h"

namespace stereo {

class BasicFancyColor : public IFancyColor {
#define RED i
#define GREEN i+1
#define BLUE i+2
#define ALPHA i+3
#define CLAMP(c) (MAX(0, MIN(255, c)))

#define MIN(a, b) (a < b ? a : b)
#define MAX(a, b) (a > b ? a : b)

#define FANCYCOLOR_DUMP_PATH "/storage/sdcard0/fancycolor/"

public:
    BasicFancyColor();
    virtual ~BasicFancyColor();

    bool generateEffectImg(JNIEnv *env, jobject thiz, jobject config, jobject result,
            GenerateEffectImgConfig* pConfig, MCHAR* pResultBitmap);

    vector<MCHAR*> getAllEffectsNames();

private:
    bool imageFilterNormal(GenerateEffectImgConfig* pConfig, MCHAR* pResultBitmap);

    bool imageFilterNegative(GenerateEffectImgConfig* pConfig, MCHAR* pResultBitmap);

    bool imageFilterBlackBoard(GenerateEffectImgConfig* pConfig, MCHAR* pResultBitmap);

    bool imageFilterWhiteBoard(GenerateEffectImgConfig* pConfig, MCHAR* pResultBitmap);

    bool imageFilterSihouette(GenerateEffectImgConfig* pConfig, MCHAR* pResultBitmap);

    bool imageFilterMonoChrome(JNIEnv *env, jobject thiz,
            GenerateEffectImgConfig* pConfig, MCHAR* pResultBitmap);

    bool imageFilterPosterize(JNIEnv *env, jobject thiz, jobject result,
            GenerateEffectImgConfig* pConfig, MCHAR* pResultBitmap);

    class MyRenderer {
    public:
        MyRenderer();
        virtual ~MyRenderer();

        void imageFilterNegative(MCHAR *bitmap, MINT32 width, MINT32 height);

        void ImageFilterBwFilter(MCHAR *bitmap, MINT32 width, MINT32 height, MINT32 rw, MINT32 gw, MINT32 bw);

        void ImageFilterWBalance(MCHAR *bitmap, MINT32 width, MINT32 height, MINT32 locX, MINT32 locY);

        void ImageFilterBlackBoard(MCHAR *bitmap, MINT32 width, MINT32 height, MFLOAT p);

        void ImageFilterWhiteBoard(MCHAR *bitmap, MINT32 width, MINT32 height, MFLOAT p);

        void ImageFilterKMeans(MCHAR *bitmap, MINT32 widht, MINT32 height,
                MCHAR *large_ds_bitmap, MINT32 lwidth, MINT32 lheight,
                MCHAR *small_ds_bitmap, MINT32 swidth, MINT32 sheight, MINT32 p, MINT32 seed);

        void ImageFilterSihouette(MCHAR *bitmap, MUINT8*mask, MINT32 width, MINT32 height);

        void merge2Bitmap(MCHAR *filterBitmap, MCHAR *oriBitmap, MUINT8*mask, MINT32 width, MINT32 height);

    private:
        void estmateWhite(MUINT8 *src, MINT32 len, MINT32 *wr, MINT32 *wb, MINT32 *wg);

        void estmateWhiteBox(MUINT8 *src, MINT32 iw, MINT32 ih, MINT32 x, MINT32 y, MINT32 *wr, MINT32 *wb, MINT32 *wg);

        template<typename T>
        void initialPickHeuristicRandom(MINT32 k, T values[], MINT32 len,
                MINT32 dimension, MINT32 stride, T dst[], MUINT32 seed);

        template<typename T, typename N>
        MINT32 calculateNewCentroids(MINT32 k, T values[], MINT32 len,
                MINT32 dimension, MINT32 stride, T oldCenters[], T dst[]);

        template<typename T, typename N>
        void runKMeansWithPicks(MINT32 k, T finalCentroids[], T values[], MINT32 len,
                MINT32 dimension, MINT32 stride, MINT32 iterations, T initialPicks[]);

        template<typename T, typename N>
        void runKMeans(MINT32 k, T finalCentroids[], T values[], MINT32 len,
                MINT32 dimension, MINT32 stride, MINT32 iterations, MUINT32 seed);

        template<typename T, typename N>
        void applyCentroids(MINT32 k, T centroids[], T values[], MINT32 len, MINT32 dimension, MINT32 stride);

        MUINT8 clamp(MINT32 c) {
            MINT32 N = 255;
            c &= ~(c >> 31);
            c -= N;
            c &= (c >> 31);
            c += N;
            return (MUINT8) c;
        }

        template<typename T, typename N>
        void sum(T values[], MINT32 len, MINT32 dimension, MINT32 stride, N dst[]) {
            MINT32 x, y;
            // zero out dst vector
            for (x = 0; x < dimension; x++) {
                dst[x] = 0;
            }
            for (x = 0; x < len; x += stride) {
                for (y = 0; y < dimension; y++) {
                    dst[y] += values[x + y];
                }
            }
        }

        template<typename T, typename N>
        void set(T val1[], N val2[], MINT32 dimension) {
            MINT32 x;
            for (x = 0; x < dimension; x++) {
                val1[x] = val2[x];
            }
        }

        template<typename T, typename N>
        void add(T val[], N dst[], MINT32 dimension) {
            MINT32 x;
            for (x = 0; x < dimension; x++) {
                dst[x] += val[x];
            }
        }

        template<typename T, typename N>
        void divide(T dst[], N divisor, MINT32 dimension) {
            MINT32 x;
            if (divisor == 0) {
                return;
            }
            for (x = 0; x < dimension; x++) {
                dst[x] /= divisor;
            }
        }

        /**
         * Calculates euclidean distance.
         */
        template<typename T, typename N>
        N euclideanDist(T val1[], T val2[], MINT32 dimension) {
            MINT32 x;
            N sum = 0;
            for (x = 0; x < dimension; x++) {
                N diff = (N) val1[x] - (N) val2[x];
                sum += diff * diff;
            }
            return sqrt(sum);
        }

        /**
         * Finds index of closet centroid to a value
         */
        template<typename T, typename N>
        MINT32 findClosest(T values[], T oldCenters[], MINT32 dimension, MINT32 stride, MINT32 pop_size) {
            MINT32 best_ind = 0;
            N best_len = euclideanDist<T, N>(values, oldCenters, dimension);
            MINT32 y;
            for (y = stride; y < pop_size; y += stride) {
                N l = euclideanDist<T, N>(values, oldCenters + y, dimension);
                if (l < best_len) {
                    best_len = l;
                    best_ind = y;
                }
            }
            return best_ind;
        }

        DebugHelper *m_pDebugHelper = NULL;
    };  // inner class MyRenderer

    MCHAR *IMAGE_FILTER_NORMAL = "imageFilterNormal";
    MCHAR *IMAGE_FILTER_SIHOUETTE = "imageFilterSihouette";
    MCHAR *IMAGE_FILTER_WHITEBOARD = "imageFilterWhiteBoard";
    MCHAR *IMAGE_FILTER_BLACKBOARD = "imageFilterBlackBoard";
    MCHAR *IMAGE_FILTER_NEGATIVE = "imageFilterNegative";
    MCHAR *IMAGE_FILTER_MONO_CHROME = "imageFilterMonoChrome";
    MCHAR *IMAGE_FILTER_POSTERIZE = "imageFilterPosterize";

    vector<MCHAR*> mEffectNames;

    MyRenderer *m_pBasicRender = NULL;
};  // class BasicFancyColor
}  // namespace stereo

#endif /* BASICFANCYCOLOR_H_ */
