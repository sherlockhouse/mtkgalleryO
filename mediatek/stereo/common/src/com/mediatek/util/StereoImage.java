package com.mediatek.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.mediatek.accessor.StereoInfoAccessor;
import com.mediatek.accessor.data.StereoConfigInfo;
import com.mediatek.util.readwritelock.ReadWriteLockProxy;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * decode stereo image.
 */
public class StereoImage {
    private static final String TAG = Log.Tag(StereoImage.class.getSimpleName());
    private static final int THUMBNAIL_SAMPLE_SIZE = 2;
    private static RectF sDisplayRect;

    /**
     * Create display rect.
     * @param context
     *            The activity context.
     */
    public static void createDisplayRect(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm =
                (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getRealMetrics(metrics);
        sDisplayRect = new RectF(0, 0, metrics.widthPixels, metrics.heightPixels);
        Log.d(TAG, "<createDisplayRect> sDisplayRect: " + sDisplayRect.toShortString());
    }

    /**
     *  get the window display rect.
     * @return the window display rect
     */
    public static RectF getDisplayRect() {
        return sDisplayRect;
    }

      /**
     * Calculate the sample size of image.
     * @param displayRect
     *            The restrict Rect for the bitmap.
     * @param originalImageBounds
     *            The original bounds of image.
     * @return the sample size
     */
    public static int getSampleSize(RectF displayRect, Rect originalImageBounds) {
        Matrix matric = new Matrix();
        float limit = Math.max(displayRect.width(), displayRect.height());
        RectF limitRect = new RectF(0.0f, 0.0f, limit, limit);
        matric.setRectToRect(new RectF(originalImageBounds), limitRect,
                Matrix.ScaleToFit.CENTER);
        float[] values = new float[9];
        matric.getValues(values);
        int sampleSize = 1;
        if (values[0] < 1.0f) {
            sampleSize = prevPowerOf2((int) Math.ceil(1.0f / values[0]));
            Log.d(TAG, "<getSampleSize> sampleSize = " + sampleSize);
        }
        return sampleSize;
    }

      /**
     * Calculate the sample size of image.
     * @param originalImageBounds
     *            The original bounds of image.
     * @return the sample size
     */
    public static int getSampleSize(Rect originalImageBounds) {
        return getSampleSize(sDisplayRect, originalImageBounds);
    }

    // Returns the previous power of two.
    // Returns the input if it is already power of 2.
    // Throws IllegalArgumentException if the input is <= 0
    private static int prevPowerOf2(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException();
        }
        return Integer.highestOneBit(n);
    }

    /**
     * Get JPEG from app1(XMP),if app1 contain JPEG, decode app1 JPEG, els decode main image.
     * @param filePath
     *            source file path
     * @param sampSize
     *            decode sample size
     * @return success->bitmap, fail->null
     */
    public static Bitmap decodeStereoImage(String filePath, int sampSize) {
        if ((filePath == null)) {
            Log.d(TAG, "<decodeStereoImage> null filePath!!");
            return null;
        }
        Log.d(TAG, "<decodeStereoImage> filePath:" + filePath + ",sampSize:" + sampSize);

        final StereoInfoAccessor accessor = new StereoInfoAccessor();
        StereoConfigInfo configInfo = accessor.readStereoConfigInfo(filePath);
        if (configInfo == null) {
            Log.d(TAG, "<decodeStereoImage> readStereoConfigInfo fail!!");
            return null;
        }
        if (configInfo.clearImage == null) {
            return decodeJpeg(filePath, sampSize);
        } else {
            return decodeJpeg(configInfo.clearImage, sampSize);
        }
    }

    /**
     * decode jpeg to bitmap.
     * @param path
     *            source file path
     * @param sampSize
     *            sample size
     * @return bitmap
     */
    public static Bitmap decodeJpeg(String path, int sampSize) {
        Log.d(TAG, "<decodeJpeg> path:" + path + ",sampSize:" + sampSize);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        options.inSampleSize = sampSize;
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inMutable = true;
        return BitmapFactory.decodeFile(path, options);
    }

    /**
     * decode jpeg to bitmap.
     * @param context
     *            activity context, use to bind service.
     * @param filePath
     *            image file path
     * @param sampleSize
     *            sample size
     * @return bitmap
     */
    public static Bitmap decodeBitmap(Context context, String filePath, int sampleSize) {
        Log.i(TAG, "<decodeBitmap> filePath = " + filePath + ",sampleSize" + sampleSize);
        Bitmap bitmap = null;
        if (filePath == null) {
            throw new IllegalArgumentException("bad argument to loadBitmap");
        }
        ReadWriteLockProxy.startService(context);
        ReadWriteLockProxy.readLock(filePath);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inMutable = true;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(filePath);
            FileDescriptor fd = fis.getFD();
            bitmap = BitmapFactory.decodeFileDescriptor(fd, null, options);
        } catch (FileNotFoundException ex) {
            Log.w(TAG, "<decodeBitmap> FileNotFoundException:" + ex);
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
            Log.w(TAG, "<decodeBitmap> IOException:" + ex);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException t) {
                    Log.w(TAG, "<decodeBitmap>close fail ", t);
                }
            }
        }
        ReadWriteLockProxy.readUnlock(filePath);
        return bitmap;
    }

    /**
     * resize image to target width and height.
     * @param bitmap
     *            original bitmap
     * @param outWidth
     *            target width
     * @param outHeight
     *            target height
     * @return output bitmap
     */
    public static Bitmap resizeImage(Bitmap bitmap, int outWidth, int outHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float scaleWidth = (float) outWidth / width;
        float scaleHeight = (float) outHeight / height;

        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);

        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }

    /**
     * Return common sample size for stereo features to use.<br/>
     * Segment will generate mask based on this sample size and later save the mask to jpeg header.
     * Fancy color can use the same sample size to decode image, and use the saved segment mask
     * directly.
     * @param width
     *            original bitmap width
     * @param height
     *            original bitmap height
     * @return the sample size
     */
    public static int getThumbnailSampleSize(int width, int height) {
        RectF windowsRect = getDisplayRect();
        Rect displayRect = new Rect(0, 0, width, height);
        int sampleSize = getSampleSize(windowsRect, displayRect);
        Log.d(TAG, "<getThumbnailSampleSize> width & height & sampleSize: " +
                                             width + " " + height + " " + sampleSize);
        return sampleSize;
    }

    private static Bitmap decodeJpeg(byte[] data, int sampSize) {
        Log.d(TAG, "<decodeJpeg> data:" + data + ",sampSize:" + sampSize);
        if (data == null) {
            Log.d(TAG, "null jpeg buffer");
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);

        options.inSampleSize = sampSize;
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inMutable = true;
        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }
}
