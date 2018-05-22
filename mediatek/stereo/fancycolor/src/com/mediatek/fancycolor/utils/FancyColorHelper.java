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
package com.mediatek.fancycolor.utils;

import android.os.Environment;

import com.mediatek.accessor.util.Utils;
import com.mediatek.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Helper class for fancy color effect feature.
 */
public class FancyColorHelper {
    private static final String TAG = Log.Tag("Fc/FancyColorHelper");

    public static final int MSG_UPDATE_VIEW = 1;
    public static final int MSG_LOADING_FINISH = 2;
    public static final int MSG_SAVING_FINISH = 3;
    public static final int MSG_RELOAD_THUMB_VIEW = 4;
    public static final int MSG_STATE_ERROR = 5;
    public static final int MSG_HIDE_LOADING_PROGRESS = 6;

    public static final String EFFECT_NAME_NORMAL = "imageFilterNormal";
    public static final String EFFECT_NAME_MONO_CHROME = "imageFilterMonoChrome";
    public static final String EFFECT_NAME_POSTERIZE = "imageFilterPosterize";
    public static final String EFFECT_NAME_RADIAL_BLUR = "imageFilterRadialBlur";
    public static final String EFFECT_NAME_STROKE = "imageFilterStroke";
    public static final String EFFECT_NAME_SIHOUETTE = "imageFilterSihouette";
    public static final String EFFECT_NAME_WHITE_BOARD = "imageFilterWhiteBoard";
    public static final String EFFECT_NAME_BLACK_BOARD = "imageFilterBlackBoard";
    public static final String EFFECT_NAME_NEGATIVE = "imageFilterNegative";

    private static final int DEFAULT_ROW_COUNT = 3;
    private static final int DEFAULT_COLUM_COUNT = 3;

    private static final String KEY_GRID_SPEC = "GridSpec";
    private static final String KEY_ROW_COUNT = "RowCount";
    private static final String KEY_COLUM_COUNT = "ColumCount";
    private static final String KEY_EFFECTS = "Effects";
    private static JsonParser sJsonParser;
    private static final String CONFIG_PATH = Environment.getExternalStorageDirectory().getPath()
            + "/fancy_color_config.txt";
    public final static boolean DEBUG_FANCYCOLOR_MASK =
            SystemPropertyUtils.get("debug.fancycolor.mask").equals("1");
    private static int sCount = 0;
    public static final String DUMP_FILE_FOLDER = "dumpFc";

    /**
     * Get grid view row count.
     *
     * @return row count
     */
    public static int getRowCount() {
        if (sJsonParser == null) {
            byte[] in = readFileToBuffer(CONFIG_PATH);
            if (in == null) {
                return DEFAULT_ROW_COUNT;
            }
            sJsonParser = new JsonParser(in);
        }
        int rowCount = sJsonParser.getValueIntFromObject(KEY_GRID_SPEC, null, KEY_ROW_COUNT);
        if (rowCount <= 0) {
            return DEFAULT_ROW_COUNT;
        }
        return rowCount;
    }

    /**
     * Get grid view colum count.
     *
     * @return colum count
     */
    public static int getColumCount() {
        if (sJsonParser == null) {
            byte[] in = readFileToBuffer(CONFIG_PATH);
            if (in == null) {
                return DEFAULT_COLUM_COUNT;
            }
            sJsonParser = new JsonParser(in);
        }
        int columCount = sJsonParser.getValueIntFromObject(KEY_GRID_SPEC, null, KEY_COLUM_COUNT);
        if (columCount <= 0) {
            return DEFAULT_COLUM_COUNT;
        }
        return columCount;
    }

    public static void dumpBuffer(byte[] maskBuffer) {
        String dumpPath = "/sdcard/" + DUMP_FILE_FOLDER + "/";
        Log.d(TAG, "<dumpBuffer> dumpPath & maskBuffer & index(count): " +
                dumpPath + " " + maskBuffer + " " + sCount);
        if (maskBuffer != null) {
            Utils.writeBufferToFile(dumpPath + "FCMask_" + (sCount++) + ".mask", maskBuffer);
        }  else {
            Log.d(TAG, "<dumpBuffer> mask buffer is null!");
        }
    }

    /**
     * Get config from file.
     *
     * @return int array containing effect loading flag.
     */
    public static int[] getEffectsFromConfig() {
        if (sJsonParser == null) {
            byte[] in = readFileToBuffer(CONFIG_PATH);
            if (in == null) {
                return null;
            }
            sJsonParser = new JsonParser(in);
        }
        return sJsonParser.getIntArrayFromObject(null, KEY_EFFECTS);
    }

    private static byte[] readFileToBuffer(String filePath) {
        File inFile = new File(filePath);
        if (!inFile.exists()) {
            Log.d(TAG, "<readFileToBuffer> " + filePath + " not exists!!!");
            return null;
        }

        RandomAccessFile rafIn = null;
        try {
            rafIn = new RandomAccessFile(inFile, "r");
            int len = (int) inFile.length();
            byte[] buffer = new byte[len];
            rafIn.read(buffer);
            return buffer;
        } catch (IOException e) {
            Log.e(TAG, "<readFileToBuffer> Exception ", e);
            return null;
        } finally {
            try {
                if (rafIn != null) {
                    rafIn.close();
                    rafIn = null;
                }
            } catch (IOException e) {
                Log.e(TAG, "<readFileToBuffer> close IOException ", e);
            }
        }
    }
}
