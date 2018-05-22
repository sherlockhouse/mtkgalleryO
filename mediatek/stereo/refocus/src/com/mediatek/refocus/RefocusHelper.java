/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediatek.refocus;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.os.SystemProperties;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;

import com.mediatek.accessor.util.JsonParser;
import com.mediatek.util.Log;
import com.mediatek.util.StereoImage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Date;
import java.text.SimpleDateFormat;

import org.json.JSONException;

/**
 * Tool class for refocus.
 */
public class RefocusHelper {
    private static final String TAG = Log.Tag("Rf/RefocusHelper");
    private static final String DEFAULT_SAVE_DIRECTORY = "RefocusLocalImages";
    private static final String TIME_STAMP_NAME = "_yyyyMMdd_HHmmss";
    private static final String PREFIX_IMG = "IMG";
    private static final String POSTFIX_JPG = ".jpg";
    private static final int MILLISEC_PER_SEC = 1000;
    private static final int INSAMPLESIZE = 2;
    private static final String BITAMP_DUMP_FOLDER = "/.GalleryIssue/";
    public static final String BITMAP_DUMP_PATH = Environment
            .getExternalStorageDirectory().toString()
            + BITAMP_DUMP_FOLDER;
    private static final int DEFAULT_COMPRESS_QUALITY = 100;

    private static final String CURRENT_PLATFORM = SystemProperties.get("ro.mediatek.platform");
    private static final String ANIMATION_LEVEL = "animation_level";
    private static final String ANIMATION_LEVEL_DEFAULT_KEY = "default";
    private static final float ANIMATION_LEVEL_DEFAULT_VALUE = 25f;

    private static final int TRANSITION_TIME_DEFAULT_VALUE = 600;
    private static final String TRANSITION_TIME = "transition_time";
    private static final String TRANSITION_TIME_DEFAULT_KEY = "default";

    private static final boolean FIRST_GENERATE_REFOCUS_FLAG_DEFAULT_VALUE = true;
    private static final String FIRST_GENERATE_REFOCUS_FLAG = "first_generate_refocus_flag";
    private static final String FIRST_GENERATE_REFOCUS_FLAG_DEFAULT_KEY = "default";

    /**
     * Content resolver query callback.
     */
    private interface ContentResolverQueryCallback {
        void onCursorResult(Cursor cursor);
    }

    /**
     * Get abstract file path for URI.
     * @param context The context, through which it can do DB operation.
     * @param uri The relative uri of the image.
     * @return The file path.
     */
    public static String getRealFilePathFromURI(Context context, Uri uri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = null;
        String filePath = null;

        cursor = context.getContentResolver().query(uri, proj, null, null, null);
        if (cursor == null) {
            Log.d(TAG, "getImageRealPathFromURI, cursor is null");
            return null;
        }
        int colummIndex = cursor
                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        if (!cursor.moveToFirst()) {
            Log.d(TAG, "getImageRealPathFromURI, moveToFirst fail");
            return null;
        }
        filePath = cursor.getString(colummIndex);
        Log.d(TAG, "getImageRealPathFromURI colummIndex= " + filePath);

        if (cursor != null) {
            cursor.close();
        }
        return filePath;
    }

    /**
     * Insert the information to DB.
     * @param context The context, through which it can do DB operation.
     * @param sourceUri The source uri.
     * @param file The file object.
     * @param saveFileName The file name.
     * @return DB uri.
     */
    public static Uri insertContent(Context context, Uri sourceUri, File file,
            String saveFileName) {
        long now = System.currentTimeMillis() / MILLISEC_PER_SEC;

        final ContentValues values = new ContentValues();
        values.put(Images.Media.TITLE, saveFileName);
        values.put(Images.Media.DISPLAY_NAME, file.getName());
        values.put(Images.Media.MIME_TYPE, "image/jpeg");
        values.put(Images.Media.DATE_TAKEN, now);
        values.put(Images.Media.DATE_MODIFIED, now);
        values.put(Images.Media.DATE_ADDED, now);
        values.put(Images.Media.ORIENTATION, 0);
        values.put(Images.Media.DATA, file.getAbsolutePath());
        values.put(Images.Media.SIZE, file.length());
        try {
            ExifInterface exif = new ExifInterface(file.getAbsolutePath());
            int imageLength = exif.getAttributeInt(
                    ExifInterface.TAG_IMAGE_LENGTH, 0);
            int imageWidth = exif.getAttributeInt(
                    ExifInterface.TAG_IMAGE_WIDTH, 0);
            values.put(Images.Media.WIDTH, imageWidth);
            values.put(Images.Media.HEIGHT, imageLength);
        } catch (IOException ex) {
            Log.w(TAG, "ExifInterface throws IOException", ex);
        }

        final String[] projection = new String[] { ImageColumns.DATE_TAKEN,
                ImageColumns.LATITUDE, ImageColumns.LONGITUDE, };
        querySource(context, sourceUri, projection,
                new ContentResolverQueryCallback() {

                    @Override
                    public void onCursorResult(Cursor cursor) {
                        values.put(Images.Media.DATE_TAKEN, cursor.getLong(0));

                        double latitude = cursor.getDouble(1);
                        double longitude = cursor.getDouble(2);
                        if ((latitude != 0f) || (longitude != 0f)) {
                            values.put(Images.Media.LATITUDE, latitude);
                            values.put(Images.Media.LONGITUDE, longitude);
                        }
                    }
                });
        Uri insertUri = context.getContentResolver().insert(
                Images.Media.EXTERNAL_CONTENT_URI, values);
        Log.d(TAG, "insertUri = " + insertUri);
        return insertUri;
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
    public static Uri updateContent(Context context, Uri sourceUri, File file, String aperture) {
        long now = System.currentTimeMillis() / MILLISEC_PER_SEC;
        final ContentValues values = new ContentValues();
        values.put(Images.Media.DATE_MODIFIED, now);
        values.put(Images.Media.DATE_ADDED, now);
        values.put(Images.Media.SIZE, file.length());

        long begin = System.currentTimeMillis();
        TraceHelper.beginSection(">>>>Refocus-updateContent-updateDB");
        context.getContentResolver().update(sourceUri, values, null, null);
        TraceHelper.endSection();
        Log.d(TAG, "<updateContent><PERF> update db costs "
                + (System.currentTimeMillis() - begin));

        begin = System.currentTimeMillis();
        TraceHelper.beginSection(">>>>Refocus-updateContent-writeExif");
        try {
            ExifInterface exif = new ExifInterface(file.getAbsolutePath());
            exif.setAttribute(ExifInterface.TAG_APERTURE, aperture);
            exif.saveAttributes();
        } catch (IOException ex) {
            Log.w(TAG, "ExifInterface throws IOException", ex);
        }
        TraceHelper.endSection();
        Log.d(TAG, "<updateContent><PERF> write exif costs "
                + (System.currentTimeMillis() - begin));
        return sourceUri;
    }

    /**
     * Get aperture data from EXIF.
     *
     * @param filePath
     *            file path
     * @return aperture data
     */
    public static String getApertureData(String filePath) {
        long begin = System.currentTimeMillis();
        String aperture = null;
        TraceHelper.beginSection(">>>>Refocus-getApertureData-readExif");
        try {
            ExifInterface exif = new ExifInterface(filePath);
            aperture = exif.getAttribute(ExifInterface.TAG_APERTURE);
        } catch (IOException ex) {
            Log.w(TAG, "ExifInterface throws IOException", ex);
        }
        TraceHelper.endSection();
        Log.d(TAG, "<getApertureData><PERF> read exif costs "
                + (System.currentTimeMillis() - begin));
        return aperture;
    }

    private static void querySource(Context context, Uri sourceUri,
            String[] projection, ContentResolverQueryCallback callback) {
        TraceHelper.beginSection(">>>>Refocus-querySource-queryDB");
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = null;

        cursor = contentResolver.query(sourceUri, projection, null, null,
                null);
        if ((cursor != null) && cursor.moveToNext()) {
            callback.onCursorResult(cursor);
        }
        if (cursor != null) {
            cursor.close();
        }
        TraceHelper.endSection();
    }

    /**
     * get new file.
     * @param context The context, through which it can do DB operation.
     * @param sourceUri The source uri for new File.
     * @param saveFileName the new file name.
     * @return The File object is created by the new file.
     */
    public static File getNewFile(Context context, Uri sourceUri,
            String saveFileName) {
        File saveDirectory = getFinalSaveDirectory(context, sourceUri);
        return new File(saveDirectory, saveFileName + ".JPG");
    }

    /**
     * Get final save directory.
     * @param context The context, through which it can do DB operation.
     * @param sourceUri The source uri for save directory.
     * @return The save directory File object.
     */
    public static File getFinalSaveDirectory(Context context, Uri sourceUri) {
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

    /**
     * Get file path with uri.
     *
     * @param context
     *            context
     * @param sourceUri
     *            uri
     * @return file path
     */
    public static String getFilePathFromUri(Context context, Uri sourceUri) {
        final String[] path = new String[1];
        querySource(context, sourceUri, new String[] { ImageColumns.DATA },
                new ContentResolverQueryCallback() {
                    @Override
                    public void onCursorResult(Cursor cursor) {
                        path[0] = cursor.getString(0);
                    }
                });
        return path[0];
    }

    private static File getSaveDirectory(Context context, Uri sourceUri) {
        final File[] dir = new File[1];
        querySource(context, sourceUri, new String[] { ImageColumns.DATA },
                new ContentResolverQueryCallback() {
                    @Override
                    public void onCursorResult(Cursor cursor) {
                        dir[0] = new File(cursor.getString(0)).getParentFile();
                    }
                });
        return dir[0];
    }

    /**
     * Decode Bitmap.
     * @param uri The bitmap file uri.
     * @param context The context, through which it can do DB operation.
     * @return The bitmap decoded from the uri.
     */
    public static Bitmap decodeBitmap(Uri uri, Context context) {
        Log.d(TAG, "uri = " + uri);
        Bitmap bitmap = null;
        if (uri == null || context == null) {
            throw new IllegalArgumentException("bad argument to loadBitmap");
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = INSAMPLESIZE;
        options.inScaled = true;
        InputStream is = null;
        try {
            is = context.getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "file not found! is = " + is, e);
            return null;
        }
        bitmap = BitmapFactory.decodeStream(is, null, options);
        try {
            if (is != null) {
                is.close();
            }
        } catch (IOException ex) {
            Log.e(TAG, "exception is = " + ex);
        }
        return bitmap;
    }

    /**
     * decode stereo image.
     *
     * @param filePath
     *            source file path
     * @return decode file
     */
    public static Bitmap decodeBitmap(String filePath, int viewWidth, int viewHeight) {
        if (filePath == null) {
            Log.d(TAG, "<decodeBitmap> params error");
            return null;
        }
        File file = new File(filePath);
        if (!file.exists()) {
            Log.d(TAG, "<decodeBitmap> file doesn't exists");
            return null;
        }
        Bitmap bitmap = StereoImage.decodeJpeg(filePath, 2);
        if (bitmap == null) {
            Log.d(TAG, "<decodeBitmap> decodeJpeg fail");
            return null;
        }
        if (viewWidth <= 0 || viewHeight <= 0) {
            Log.d(TAG, "<decodeBitmap> viewWidth|viewHeight is invalid, " + viewHeight + ","
                    + viewHeight);
            return bitmap;
        }
        if (bitmap.getWidth() > bitmap.getHeight()) {
            return StereoImage.resizeImage(bitmap, Math.max(viewWidth, viewHeight), Math.min(
                    viewWidth, viewHeight));
        } else {
            return StereoImage.resizeImage(bitmap, Math.min(viewWidth, viewHeight), Math.max(
                    viewWidth, viewHeight));
        }
    }

    /**
     * Get new file from the uri.
     * @param context The context, through which it can do DB operation.
     * @param sourceUri the file uri.
     * @return The new File object.
     */
    public static File getNewFile(Context context, Uri sourceUri) {
        File saveDirectory = getFinalSaveDirectory(context, sourceUri);
        String filename = new SimpleDateFormat(TIME_STAMP_NAME)
                .format(new Date(System.currentTimeMillis()));
        return new File(saveDirectory, PREFIX_IMG + filename + POSTFIX_JPG);
    }

    /**
     * Used to dump bitmap.
     * @param bitmap bitmap to dump
     * @param fileName dump name
     */
    public static void dumpBitmap(Bitmap bitmap, String fileName) {
        fileName = fileName + ".png";
        File galleryIssueFilePath = new File(BITMAP_DUMP_PATH);
        if (!galleryIssueFilePath.exists()) {
            Log.d(TAG, "<dumpBitmap> create  galleryIssueFilePath");
            galleryIssueFilePath.mkdir();
        }
        File file = new File(BITMAP_DUMP_PATH, fileName);
        OutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, DEFAULT_COMPRESS_QUALITY, fos);
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "<dumpBitmap> IOException", e.getCause());
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "<dumpBitmap> close FileOutputStream", e.getCause());
            }
        }
    }

    /**
     * Object wait.
     * @param object object
     */
    public static void waitWithoutInterrupt(Object object) {
        try {
            object.wait();;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get ReFocusView max animation level.
     * @param am assetManager
     * @return max animation level
     */
    public static float getMaxAnimationLevelFromCfg(AssetManager am) {
        if (CURRENT_PLATFORM == null || " ".equals(CURRENT_PLATFORM) || am == null) {
            Log.d(TAG, "<getMaxAnimationLevelFromCfg> CURRENT_PLATFORM: " + CURRENT_PLATFORM);
            return ANIMATION_LEVEL_DEFAULT_VALUE;
        }
        Log.d(TAG, "<getMaxAnimationLevelFromCfg> CURRENT_PLATFORM: " + CURRENT_PLATFORM);
        InputStream is = null;
        byte[] configBuffer = null;
        try {
            is = am.open("refocus.cfg");
            if (is == null) {
                Log.d(TAG, "<getMaxAnimationLevelFromCfg> is == null");
                return ANIMATION_LEVEL_DEFAULT_VALUE;
            }
            int len = is.available();
            if (len <= 0) {
                Log.d(TAG, "<getMaxAnimationLevelFromCfg> len <= 0");
                return ANIMATION_LEVEL_DEFAULT_VALUE;
            }
            configBuffer = new byte[len];
            is.read(configBuffer);
        } catch (IOException e) {
            Log.e(TAG, "<getMaxAnimationLevelFromCfg> IOException", e);
            return ANIMATION_LEVEL_DEFAULT_VALUE;
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "<getMaxAnimationLevelFromCfg> close IOException", e);
            }
        }
        if (configBuffer == null || configBuffer.length <= 0) {
            Log.d(TAG,
                    "<getMaxAnimationLevelFromCfg> configBuffer check error, configBuffer: "
                            + configBuffer);
            return ANIMATION_LEVEL_DEFAULT_VALUE;
        }
        JsonParser parser = new JsonParser(configBuffer);
        double value =
                parser.getValueDoubleFromObject(ANIMATION_LEVEL, null, CURRENT_PLATFORM);
        if (value == -1.0) {
            value =
                    parser.getValueDoubleFromObject(ANIMATION_LEVEL, null,
                            ANIMATION_LEVEL_DEFAULT_KEY);
            Log.d(TAG, "<getMaxAnimationLevelFromCfg> value == -1.0, get default value: "
                    + value);
        }
        Log.d(TAG, "<getMaxAnimationLevelFromCfg><comp> animation level: " + value);
        return (float) value;
    }

    /**
     * Get ReFocusView transition time.
     * @param am assetManager
     * @return transition time
     */
    public static int getTransitionTimeFromCfg(AssetManager am) {
        if (CURRENT_PLATFORM == null || " ".equals(CURRENT_PLATFORM) || am == null) {
            Log.d(TAG, "<getTransitionTimeFromCfg> CURRENT_PLATFORM: " + CURRENT_PLATFORM);
            return TRANSITION_TIME_DEFAULT_VALUE;
        }
        Log.d(TAG, "<getTransitionTimeFromCfg> CURRENT_PLATFORM: " + CURRENT_PLATFORM);
        InputStream is = null;
        byte[] configBuffer = null;
        try {
            is = am.open("refocus.cfg");
            if (is == null) {
                Log.d(TAG, "<getTransitionTimeFromCfg> is == null");
                return TRANSITION_TIME_DEFAULT_VALUE;
            }
            int len = is.available();
            if (len <= 0) {
                Log.d(TAG, "<getTransitionTimeFromCfg> len <= 0");
                return TRANSITION_TIME_DEFAULT_VALUE;
            }
            configBuffer = new byte[len];
            is.read(configBuffer);
        } catch (IOException e) {
            Log.e(TAG, "<getTransitionTimeFromCfg> IOException", e);
            return TRANSITION_TIME_DEFAULT_VALUE;
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                Log.e(TAG, "<getTransitionTimeFromCfg> close IOException", e);
            }
        }
        JsonParser parser = new JsonParser(configBuffer);
        int value = parser.getValueIntFromObject(TRANSITION_TIME, null, CURRENT_PLATFORM);
        if (value == -1) {
            value =
                    parser.getValueIntFromObject(TRANSITION_TIME, null,
                            TRANSITION_TIME_DEFAULT_KEY);
            Log.d(TAG, "<getTransitionTimeFromCfg> value == -1, get default value: " + value);
        }
        Log.d(TAG, "<getTransitionTimeFromCfg><comp> animation level: " + value);
        return value;
    }

    /**
     * Get first generate refocus flag.
     * @param am assetManager
     * @return true -> generate refocus image when 1st enter refocus
     *         false -> do not generate refocus image when 1st enter refocus
     */
    public static boolean getFirstGenerateRefocusFlagFromCfg(AssetManager am) {
        if (CURRENT_PLATFORM == null || " ".equals(CURRENT_PLATFORM) || am == null) {
            Log.d(TAG, "<getFirstGenerateRefocusFlagFromCfg> CURRENT_PLATFORM: "
                    + CURRENT_PLATFORM);
            return FIRST_GENERATE_REFOCUS_FLAG_DEFAULT_VALUE;
        }
        Log.d(TAG, "<getFirstGenerateRefocusFlagFromCfg> CURRENT_PLATFORM: "
                + CURRENT_PLATFORM);
        InputStream is = null;
        byte[] configBuffer = null;
        try {
            is = am.open("refocus.cfg");
            if (is == null) {
                Log.d(TAG, "<getFirstGenerateRefocusFlagFromCfg> is == null");
                return FIRST_GENERATE_REFOCUS_FLAG_DEFAULT_VALUE;
            }
            int len = is.available();
            if (len <= 0) {
                Log.d(TAG, "<getFirstGenerateRefocusFlagFromCfg> len <= 0");
                return FIRST_GENERATE_REFOCUS_FLAG_DEFAULT_VALUE;
            }
            configBuffer = new byte[len];
            is.read(configBuffer);
        } catch (IOException e) {
            Log.e(TAG, "<getFirstGenerateRefocusFlagFromCfg> IOException", e);
            return FIRST_GENERATE_REFOCUS_FLAG_DEFAULT_VALUE;
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                Log.e(TAG, "<getFirstGenerateRefocusFlagFromCfg> close IOException", e);
            }
        }
        JsonParser parser = new JsonParser(configBuffer);
        boolean value = false;
        try {
            value =
                    parser.getValueBooleanOrThrow(FIRST_GENERATE_REFOCUS_FLAG, null,
                            CURRENT_PLATFORM);
        } catch (JSONException e1) {
            Log.d(TAG, "<getFirstGenerateRefocusFlagFromCfg> no platform cfg: "
                    + CURRENT_PLATFORM);
            try {
                value =
                        parser.getValueBooleanOrThrow(FIRST_GENERATE_REFOCUS_FLAG, null,
                                FIRST_GENERATE_REFOCUS_FLAG_DEFAULT_KEY);
            } catch (JSONException e2) {
                value = false;
                Log.d(TAG, "<getFirstGenerateRefocusFlagFromCfg> no default cfg");
            }
            Log.d(TAG, "<getFirstGenerateRefocusFlagFromCfg> get default value: " + value);
        }
        Log.d(TAG, "<getFirstGenerateRefocusFlagFromCfg><comp> FirstGenerateRefocusFlag: " + value);
        return value;
    }
}
