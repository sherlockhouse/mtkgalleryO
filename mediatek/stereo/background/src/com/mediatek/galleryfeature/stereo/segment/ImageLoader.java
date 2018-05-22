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

package com.mediatek.galleryfeature.stereo.segment;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.webkit.MimeTypeMap;

import com.mediatek.util.Log;
import com.mediatek.util.StereoImage;
import com.mediatek.util.TraceHelper;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class to load a image or some of its attributes by a given Uri.
 */
public final class ImageLoader {
    private static final String LOGTAG = Log.Tag("Bg/ImageLoader");

    public static final String JPEG_MIME_TYPE = "image/jpeg";

    private static final int BITMAP_LOAD_BACKOUT_ATTEMPTS = 5;
    private static final float OVERDRAW_ZOOM = 1.2f;

    private ImageLoader() {
    }

    /**
     * Return the Mime type for a Url. Safe to use with Urls that do not come
     * from Gallery's content provider.
     *
     * @param src
     *            the Uri.
     * @return Mime type string.
     */
    public static String getMimeType(Uri src) {
        if (src == null) {
            Log.w(LOGTAG, "src can not be null");
            return  null;
        }
        String postfix = MimeTypeMap.getFileExtensionFromUrl(src.toString());
        String ret = null;
        if (postfix != null) {
            ret = MimeTypeMap.getSingleton().getMimeTypeFromExtension(postfix);
        }
        return ret;
    }

    /**
     * Return the image's orientation flag.
     *
     * @param context
     *            Android Context.
     * @param uri
     *            the image uri.
     * @return
     *         orientation code defined in ExifInterface.
     *         ExifInterface.ORIENTATION_NORMAL if no valid orientation was
     *         found.
     */
    public static int getMetadataOrientation(Context context, Uri uri) {
        if (uri == null || context == null) {
            throw new IllegalArgumentException("bad argument to getOrientation");
        }
        // First try to find orientation data in Gallery's ContentProvider.
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri,
                    new String[] { MediaStore.Images.ImageColumns.ORIENTATION }, null, null, null);
            if (cursor != null && cursor.moveToNext()) {
                int ori = cursor.getInt(0);
                switch (ori) {
                case SegmentUtils.ROTATE_90:
                    return ExifInterface.ORIENTATION_ROTATE_90;
                case SegmentUtils.ROTATE_270:
                    return ExifInterface.ORIENTATION_ROTATE_270;
                case SegmentUtils.ROTATE_180:
                    return ExifInterface.ORIENTATION_ROTATE_180;
                default:
                    return ExifInterface.ORIENTATION_NORMAL;
                }
            }
        } catch (SQLiteException e) {
            // Do nothing
        } catch (IllegalArgumentException e) {
            // Do nothing
        } catch (IllegalStateException e) {
            // Do nothing
        } finally {
            closeSilently(cursor);
        }
        ExifInterface exif;
        InputStream is = null;
        // Fall back to checking EXIF tags in file or input stream.
        try {
            if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
                String mimeType = getMimeType(uri);
                if (!JPEG_MIME_TYPE.equals(mimeType)) {
                    return ExifInterface.ORIENTATION_NORMAL;
                }
                String path = uri.getPath();
                exif = new ExifInterface(path);
            } else {
                // TODO can run into this condition?
                // is = context.getContentResolver().openInputStream(uri);
                // exif = new ExifInterface(is);
                return ExifInterface.ORIENTATION_NORMAL;
            }
            return parseExifOrientation(exif);
        } catch (IOException e) {
            Log.w(LOGTAG, "<getMetadataOrientation> Failed to read EXIF orientation", e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                Log.w(LOGTAG, "<getMetadataOrientation> Failed to close InputStream", e);
            }
        }
        return ExifInterface.ORIENTATION_NORMAL;
    }

    /**
     * Return the bounds of the bitmap stored at a given Url.
     * We never consider orientation info here.
     *
     * @param context
     *            the Android Context.
     * @param uri
     *            the image uri.
     * @return Rect representing the image dimension.
     */
    public static Rect loadBitmapBounds(Context context, Uri uri) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        loadBitmap(context, uri, o);
        return new Rect(0, 0, o.outWidth, o.outHeight);
    }

    /**
     * Load a bitmap from a given url by options.<br/>
     * Typical routine to invoke BitmapFactory to decode an image, with extra
     * considerations for wbmp files.<br/>
     * Be attentioned that we never consider orientation info here.
     *
     * @param context
     *            the Android Context.
     * @param uri
     *            the image uri.
     * @param o
     *            the BitmapFactory.Options.
     * @return the Bitmap.
     * @exception  NullPointerException If <code>context</code> is <code>null</code>
     *      or <code>uri</code> is <code>null</code>.
     */
    public static Bitmap loadBitmap(Context context, Uri uri, BitmapFactory.Options o) {
        if (uri == null || context == null) {
            throw new IllegalArgumentException("bad argument to loadBitmap");
        }
        Log.d(LOGTAG, "<loadBitmap> uri = " + uri);
        InputStream is = null;
        Bitmap result = null;
        try {
            is = context.getContentResolver().openInputStream(uri);
            result = BitmapFactory.decodeStream(is, null, o);
            // wbmp bitmap format is index8 ,and the format do not
            // support BitmapFactory resize;
            // So We should resize the bitmap for avoid out of memory.
            if (null != result && null == result.getConfig()) {
                if ((o == null) || (o.inSampleSize == 0)) {
                    return result;
                }
                result = SegmentUtils.resizeBitmapByScale(result, 1.0f / o.inSampleSize, true);
            }
            return result;
        } catch (FileNotFoundException e) {
            Log.e(LOGTAG, "<loadBitmap> FileNotFoundException for " + uri, e);
        } finally {
            closeSilently(is);
        }
        return null;
    }

    /**
     * Load a bitmap from a given url by options, considering its orientation info.<br/>
     *
     * @param context
     *            the Android Context.
     * @param uri
     *            the image uri.
     * @param o
     *            the BitmapFactory.Options.
     * @return the Bitmap.
     */
    public static Bitmap loadOrientedClearBitmap(Context context, Uri uri,
            BitmapFactory.Options o) {
        TraceHelper.beginSection(">>>>ImageLoader-loadOrientedBitmap-getMetadataOrientation");
        if (uri == null) {
            Log.w(LOGTAG, "<loadOrientedBitmap> uri can not be null");
            return null;
        }
        int orientation = getMetadataOrientation(context, uri);
        TraceHelper.endSection();
        TraceHelper.beginSection(">>>>ImageLoader-loadOrientedBitmap-loadBitmap");
        Bitmap bitmap = loadClearBitmap(context, uri, o);
        TraceHelper.endSection();
        if (bitmap == null) {
            Log.d(LOGTAG, "<loadOrientedBitmap> return null");
            return null;
        }
        TraceHelper.beginSection(">>>>ImageLoader-loadOrientedBitmap-orientBitmap");
        Bitmap orientedBitmap = SegmentUtils.orientBitmap(bitmap, orientation);
        TraceHelper.endSection();
        if (bitmap != orientedBitmap) {
            bitmap.recycle();
        }
        return orientedBitmap;
    }

    private static Bitmap loadClearBitmap(Context context, Uri uri, BitmapFactory.Options o) {
        if (uri == null || context == null) {
            throw new IllegalArgumentException("bad argument to loadBitmap");
        }
        Log.d(LOGTAG, "<loadClearBitmap> uri = " + uri);
        Bitmap result = null;
        android.database.Cursor c = null;
        String filePath = null;
        boolean isDepth = false;
        try {
            c = context.getContentResolver().query(uri,
                    new String[] {Images.ImageColumns.DATA,
                            SegmentUtils.CAMERA_REFOCUS }, null, null, null);
            if (c != null && c.moveToFirst()) {
                filePath = c.getString(0);
                isDepth = ((c.getInt(1)) == 1);
            }
        } catch (IllegalStateException e) {
            Log.e(LOGTAG, "<loadBitmap> Exception when trying to fetch and DATA info", e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
        int sampleSize = ((o == null) ? 1 : o.inSampleSize);
        if (isDepth && filePath != null) {
            result = StereoImage.decodeStereoImage(filePath, sampleSize);
        } else {
            result =  loadBitmap(context, uri, o);
        }
        // wbmp bitmap format is index8 ,and the format do not
        // support BitmapFactory resize;
        // So We should resize the bitmap for avoid out of memory.
        if (null != result && null == result.getConfig()) {
            if ((o == null) || (o.inSampleSize == 0)) {
                return result;
            }
            result = SegmentUtils.resizeBitmapByScale(result, 1.0f / o.inSampleSize, true);
        }
        return result;
    }

    private static int parseExifOrientation(ExifInterface exif) {
        Integer tagval = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL);
        int orientation = tagval;
        switch (orientation) {
        case ExifInterface.ORIENTATION_NORMAL:
        case ExifInterface.ORIENTATION_ROTATE_90:
        case ExifInterface.ORIENTATION_ROTATE_180:
        case ExifInterface.ORIENTATION_ROTATE_270:
        case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
        case ExifInterface.ORIENTATION_FLIP_VERTICAL:
        case ExifInterface.ORIENTATION_TRANSPOSE:
        case ExifInterface.ORIENTATION_TRANSVERSE:
            return orientation;
        default:
            return ExifInterface.ORIENTATION_NORMAL;
        }
    }

    private static void closeSilently(Closeable c) {
        if (c == null) {
            return;
        }
        try {
            c.close();
        } catch (IOException t) {
            Log.w(LOGTAG, "<closeSilently> fail to close Closeable", t);
        }
    }
}
