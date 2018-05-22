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
package com.mediatek.fancycolor;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;

import com.mediatek.fancycolor.parallel.ThreadPool;
import com.mediatek.fancycolor.utils.TraceHelper;
import com.mediatek.util.Log;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Date;
import java.text.SimpleDateFormat;

/**
 * Helper class to encapsulate image saving work into request.
 */
class SavingRequest implements ThreadPool.Job<Void> {
    private static final String TAG = Log.Tag("Fc/SavingRequest");
    private static final String PREFIX_IMG = "IMG";
    private static final String POSTFIX_JPG = ".jpg";
    private static final String JPEG_MIME_TYPE = "image/jpeg";
    private static final String DEFAULT_SAVE_DIRECTORY = "EditedOnlinePhotos";
    private static final String TIME_STAMP_NAME = "_yyyyMMdd_HHmmss_SSS";
    private static final int QUALITY = 100;
    private static final int TIME_SCALE = 1000;
    private int mSourceBitmapWidth;
    private int mSourceBitmapHeight;
    private Context mContext;
    private Bitmap mBitmap;
    private Uri mUri;
    private SavingRequestListener mListener;

    /**
     * Callback interface when query by ContentResolver.
     */
    public interface ContentResolverQueryCallback {
        void onCursorResult(Cursor cursor);
    }

    /**
     * Callback interface when saving done.
     */
    public interface SavingRequestListener {
        public void onSavingDone(Uri result);
    }

    public SavingRequest(Context context, Uri uri, Bitmap bitmap, int srcBmpWidth,
            int srcBmpHeight, SavingRequestListener listener) {
        mContext = context;
        mUri = uri;
        mBitmap = bitmap;
        mSourceBitmapWidth = srcBmpWidth;
        mSourceBitmapHeight = srcBmpHeight;
        mListener = listener;
    }

    @Override
    public Void run(ThreadPool.JobContext jc) {
        TraceHelper.beginSection(">>>>FancyColor-SavingRequest");
        File out = getNewFile(mContext, mUri);
        Log.d(TAG, "<run> new file path: " + out.getPath());
        mBitmap = resizeBitmap(mBitmap, mSourceBitmapWidth, mSourceBitmapHeight);
        compressBitmap(out, mBitmap, QUALITY);
        Uri result = updateDatabase(mContext, out, mBitmap.getWidth(), mBitmap.getHeight());
        if (mListener != null) {
            mListener.onSavingDone(result);
        }
        TraceHelper.endSection();
        return null;
    }

    private Uri updateDatabase(Context context, File file, int width, int height) {
        Log.d(TAG, "<updateDatabase> File & width & height: " + file + " " + width + " " + height);
        long time = System.currentTimeMillis();
        if (file == null) {
            return null;
        }
        final ContentValues values = getContentValues(context, mUri, file, time);
        values.put(Images.Media.WIDTH, width);
        values.put(Images.Media.HEIGHT, height);
        values.put(Images.Media.SIZE, file.length());
        Uri result = context.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
        Log.d(TAG, "<updateDatabase> file.length & result: " + file.length() + " " + result);
        return result;
    }

    private Bitmap resizeBitmap(Bitmap src, int dstWidth, int dstHeight) {
        Log.d(TAG, "<resizeBitmap> src & width &height: " + src + " " + dstWidth + " " + dstHeight);
        if (src == null || dstWidth <= 0 || dstHeight <= 0) {
            Log.d(TAG, "<resizeBitmap> parameters unlawful, do nothing.");
            return src;
        }
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(src, dstWidth, dstHeight, true);
        if (src != null) {
            src.recycle();
        }
        return resizedBitmap;
    }

    private File getSaveDirectory(Context context, Uri sourceUri) {
        File file = getLocalFileFromUri(context, sourceUri);
        if (file != null) {
            return file.getParentFile();
        } else {
            return null;
        }
    }

    private File getLocalFileFromUri(Context context, Uri srcUri) {
        if (srcUri == null) {
            Log.d(TAG, "<getLocalFileFromUri> srcUri is null.");
            return null;
        }

        String scheme = srcUri.getScheme();
        if (scheme == null) {
            Log.d(TAG, "<getLocalFileFromUri> scheme is null.");
            return null;
        }

        final File[] file = new File[1];
        // sourceUri can be a file path or a content Uri, it need to be handled differently.
        if (scheme.equals(ContentResolver.SCHEME_CONTENT)) {
            if (srcUri.getAuthority().equals(MediaStore.AUTHORITY)) {
                querySource(context, srcUri, new String[] { ImageColumns.DATA },
                        new ContentResolverQueryCallback() {
                            @Override
                            public void onCursorResult(Cursor cursor) {
                                file[0] = new File(cursor.getString(0));
                            }
                        });
            }
        } else if (scheme.equals(ContentResolver.SCHEME_FILE)) {
            file[0] = new File(srcUri.getPath());
        }
        return file[0];
    }

    private File getFinalSaveDirectory(Context context, Uri sourceUri) {
        File saveDirectory = getSaveDirectory(context, sourceUri);
        if ((saveDirectory == null) || !saveDirectory.canWrite()) {
            saveDirectory = new File(Environment.getExternalStorageDirectory(),
                    DEFAULT_SAVE_DIRECTORY);
        }
        // Create the directory if it doesn't exist
        if (!saveDirectory.exists()) {
            saveDirectory.mkdirs();
        }
        return saveDirectory;
    }

    private File getNewFile(Context context, Uri sourceUri) {
        File saveDirectory = getFinalSaveDirectory(context, sourceUri);
        String filename = new SimpleDateFormat(TIME_STAMP_NAME).format(new Date(System
                .currentTimeMillis()));
        Log.d(TAG, "<getNewFile> saveDirectory & filename: " +
                saveDirectory.getPath() + " " + filename);
        return new File(saveDirectory, PREFIX_IMG + filename + POSTFIX_JPG);
    }

    private boolean compressBitmap(File file, Bitmap image, int jpegCompressQuality) {
        boolean ret = false;
        OutputStream s = null;
        try {
            s = new FileOutputStream(file.getAbsolutePath());
            image.compress(Bitmap.CompressFormat.JPEG,
                    (jpegCompressQuality > 0) ? jpegCompressQuality : 1, s);
            s.flush();
            s.close();
            s = null;
            ret = true;
        } catch (FileNotFoundException e) {
            Log.d(TAG, "<compressBitmap> File not found: " + file.getAbsolutePath(), e);
        } catch (IOException e) {
            Log.d(TAG, "<compressBitmap> IOException: ", e);
        } finally {
            closeSilently(s);
        }
        return ret;
    }

    private void closeSilently(Closeable c) {
        if (c == null) {
            return;
        }
        try {
            c.close();
        } catch (IOException t) {
            Log.w(TAG, "<closeSilently> fail to close Closeable", t);
        }
    }

    private void querySource(Context context, Uri sourceUri, String[] projection,
            ContentResolverQueryCallback callback) {
        if (context == null) {
            Log.d(TAG, "<querySource> parameter unlawful, do nothing.");
            return;
        }
        ContentResolver contentResolver = context.getContentResolver();
        querySourceFromContentResolver(contentResolver, sourceUri, projection, callback);
    }

    private void querySourceFromContentResolver(ContentResolver contentResolver, Uri sourceUri,
            String[] projection, ContentResolverQueryCallback callback) {
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(sourceUri, projection, null, null, null);
        } catch (Exception e) {
            Log.w(TAG, "<querySourceFromContentResolver> query exception", e);
        }
        if ((cursor != null) && cursor.moveToNext()) {
            callback.onCursorResult(cursor);
        }
        if (cursor != null) {
            cursor.close();
        }
    }

    private ContentValues getContentValues(Context context, Uri sourceUri, File file, long time) {
        final ContentValues values = new ContentValues();

        time /= TIME_SCALE;
        values.put(Images.Media.TITLE, file.getName());
        values.put(Images.Media.DISPLAY_NAME, file.getName());
        values.put(Images.Media.MIME_TYPE, JPEG_MIME_TYPE);
        values.put(Images.Media.DATE_TAKEN, time);
        values.put(Images.Media.DATE_MODIFIED, time);
        values.put(Images.Media.DATE_ADDED, time);
        values.put(Images.Media.ORIENTATION, 0);
        values.put(Images.Media.DATA, file.getAbsolutePath());
        values.put(Images.Media.SIZE, file.length());
        // This is a workaround to trigger the MediaProvider to re-generate the thumbnail.
        values.put(Images.Media.MINI_THUMB_MAGIC, 0);

        final String[] projection = new String[] { ImageColumns.DATE_TAKEN, ImageColumns.LATITUDE,
                ImageColumns.LONGITUDE, };
        querySource(context, sourceUri, projection, new ContentResolverQueryCallback() {
            @Override
            public void onCursorResult(Cursor cursor) {
                values.put(Images.Media.DATE_TAKEN, cursor.getLong(0));
                double latitude = cursor.getDouble(1);
                double longitude = cursor.getDouble(2);
                // TODO: Change || to && after the default location
                // issue is fixed.
                if ((latitude != 0f) || (longitude != 0f)) {
                    values.put(Images.Media.LATITUDE, latitude);
                    values.put(Images.Media.LONGITUDE, longitude);
                }
            }
        });
        return values;
    }
}
