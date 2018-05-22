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
package com.mediatek.util;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Rect;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Stereo utile functions.
 */
public class StereoUtils {
    private static final String TAG = Log.Tag(StereoUtils.class.getSimpleName());
    private static final int RECT_LEN = 2000;
    private static final int HALF_RECT_LEN = RECT_LEN / 2;
    private static final int MILLISEC_PER_SEC = 1000;

    /**
     * get face region, converted info image coordinated.
     *
     * @param width
     *            image width
     * @param height
     *            image height
     * @param left
     *            left
     * @param top
     *            top
     * @param right
     *            right
     * @param bottom
     *            bottom
     * @return screen coordinated region
     */
    public static Rect getFaceRect(double width, double height, double left, double top,
                                   double right, double bottom) {
        Log.d(TAG, "<getFaceRect> width:" + width + ",height:" + height + ",orientation:"
                + ",left:" + left + ",top:" + top + ",right:" + right + ",bottom:" + bottom);

        Rect res = new Rect();
        res.left = (int) ((left + HALF_RECT_LEN) * width / RECT_LEN);
        res.top = (int) ((top + HALF_RECT_LEN) * height / RECT_LEN);
        res.right = (int) ((right + HALF_RECT_LEN) * width / RECT_LEN);
        res.bottom = (int) ((bottom + HALF_RECT_LEN) * height / RECT_LEN);
        return res;
    }
    /**
     * transform to image coordinate.
     *
     * @param width
     *            image width
     * @param height
     *            image height
     * @param x
     *            original x coordinate (-100->1000)
     * @param y
     *            original y coordinate (-100 -> 1000)
     * @return image coordinate
     */
    public static int [] getCoordinateSensorToImage(double width, double height,
                                                    double x, double y) {
        int [] coord = new int[2];
        coord[0] = (int) ((x + HALF_RECT_LEN) * width / RECT_LEN);
        coord[1] = (int) ((y + HALF_RECT_LEN) * height / RECT_LEN);
        return coord;
    }

    /**
     * transform image coordinate to sensor coordinate.
     *
     * @param width
     *            image width
     * @param height
     *            image height
     * @param x
     *            image x coordinate
     * @param y
     *            image y coordinate
     * @return sensor coordinate
     */
    public static int[] getCoordinateImageToSensor(double width, double height,
                                                   double x, double y) {
        int[] coord = new int[2];
        coord[0] = (int) (x * RECT_LEN / width - HALF_RECT_LEN);
        coord[1] = (int) (y * RECT_LEN / height - HALF_RECT_LEN);
        return coord;
    }

    /**
     * writeBufferToFile.
     *
     * @param desFile
     *            desFile
     * @param buffer
     *            buffer
     * @return result
     */
    public static boolean writeBufferToFile(String desFile, byte[] buffer) {
        if (buffer == null) {
            Log.d(TAG, "<writeBufferToFile> buffer is null");
            return false;
        }
        File out = new File(desFile);
        if (out.exists()) {
            out.delete();
        }
        FileOutputStream fops = null;
        try {
            if (!(out.createNewFile())) {
                Log.d(TAG, "<writeBufferToFile> createNewFile error");
                return false;
            }
            fops = new FileOutputStream(out);
            fops.write(buffer);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "<writeBufferToFile> IOException", e);
            return false;
        } finally {
            try {
                if (fops != null) {
                    fops.close();
                    fops = null;
                }
            } catch (IOException e) {
                Log.e(TAG, "<writeBufferToFile> close, IOException", e);
            }
        }
    }

    /**
     * readFileToBuffer.
     *
     * @param filePath
     *            filePath
     * @return result
     */
    public static byte[] readFileToBuffer(String filePath) {
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

    /**
     * Update content.
     *
     * @param context
     *            context
     * @param sourceUri
     *            source uri
     * @param file
     *            output file
     * @return source uri
     */
    public static Uri updateContent(Context context, Uri sourceUri, File file) {
        long now = System.currentTimeMillis() / MILLISEC_PER_SEC;
        final ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DATE_MODIFIED, now);
        values.put(MediaStore.Images.Media.DATE_ADDED, now);
        values.put(MediaStore.Images.Media.SIZE, file.length());
        context.getContentResolver().update(sourceUri, values, null, null);
        Log.d(TAG, "<updateContent> sourceUri: " + sourceUri);
        return sourceUri;
    }
}
