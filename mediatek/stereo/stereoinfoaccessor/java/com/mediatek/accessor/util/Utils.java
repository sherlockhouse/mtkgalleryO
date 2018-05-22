package com.mediatek.accessor.util;

import android.graphics.Bitmap;
import android.os.Environment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * File operator tools.
 */
public class Utils {
    private final static String TAG = Log.Tag(Utils.class.getSimpleName());

    private static final int DEFAULT_COMPRESS_QUALITY = 100;
    private static final byte DEFAULT_COLOR = (byte) 255;
    private static final int BYTES_PER_PIXEL = 4;
    private static final int NUM_3 = 3;

    public static boolean ENABLE_BUFFER_DUMP = false;
    public static boolean REGEN_DEPTH = false;
    public static boolean XMP_DEBUG_LOG = false;
    public static boolean ENABLE_GDEPTH = false;
    public static final String DUMP_FILE_FOLDER = Environment
            .getExternalStorageDirectory().toString()
            + "/dumpJps";
    private static final String REGEN_DEPTH_CFG = Environment
            .getExternalStorageDirectory().toString()
            + "/regen";
    private static final String XMP_LOG_CFG = Environment
            .getExternalStorageDirectory().toString()
            + "/xmplog";

    private static final String ENABLE_GDEPTH_CFG = Environment
            .getExternalStorageDirectory().toString()
            + "/ENABLE_GDEPTH";

    static {
        File inFile = new File(DUMP_FILE_FOLDER);
        if (inFile.exists()) {
            ENABLE_BUFFER_DUMP = true;
            Log.i(TAG, "ENABLE_BUFFER_DUMP: " + ENABLE_BUFFER_DUMP);
        }
        inFile = new File(REGEN_DEPTH_CFG);
        if (inFile.exists()) {
            REGEN_DEPTH = true;
            Log.i(TAG, "REGEN_DEPTH: " + REGEN_DEPTH);
        }
        inFile = new File(XMP_LOG_CFG);
        if (inFile.exists()) {
            XMP_DEBUG_LOG = true;
            Log.i(TAG, "XMP_DEBUG_LOG: " + XMP_DEBUG_LOG);
        }
        inFile = new File(ENABLE_GDEPTH_CFG);
        if (inFile.exists()) {
            ENABLE_GDEPTH = true;
        }
        Log.d(getGDepthTag(), "ENABLE_GDEPTH: " + ENABLE_GDEPTH);
    }

    /**
     * Read file to buffer.
     * @param filePath
     *            filePath
     * @return result
     */
    public static byte[] readFileToBuffer(String filePath) {
        TraceHelper.beginSection(">>>>Utils-readFileToBuffer");
        File inFile = new File(filePath);
        if (!inFile.exists()) {
            Log.d(TAG, "<readFileToBuffer> " + filePath + " not exists!!!");
            TraceHelper.endSection();
            return null;
        }

        RandomAccessFile rafIn = null;
        try {
            rafIn = new RandomAccessFile(inFile, "r");
            int len = (int) inFile.length();
            byte[] buffer = new byte[len];
            rafIn.read(buffer);
            TraceHelper.endSection();
            return buffer;
        } catch (IOException e) {
            Log.e(TAG, "<readFileToBuffer> Exception ", e);
            TraceHelper.endSection();
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
     * writeBufferToFile.
     * @param desFile
     *            desFile
     * @param buffer
     *            buffer
     * @return result
     */
    public static boolean writeBufferToFile(String desFile, byte[] buffer) {
        TraceHelper.beginSection(">>>>Utils-writeBufferToFile");
        if (desFile == null) {
            return false;
        }
        String parentPath = desFile.substring(0, desFile.lastIndexOf("/"));
        File parentFile = new File(parentPath);
        if (!parentFile.exists()) {
            parentFile.mkdir();
        }
        if (buffer == null) {
            Log.d(TAG, "<writeBufferToFile> buffer is null");
            TraceHelper.endSection();
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
                TraceHelper.endSection();
                return false;
            }
            fops = new FileOutputStream(out);
            fops.write(buffer);
            TraceHelper.endSection();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "<writeBufferToFile> IOException", e);
            TraceHelper.endSection();
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
     * writeStringToFile.
     *
     * @param desFile
     *            desFile
     * @param value
     *            value
     */
    public static void writeStringToFile(String desFile, String value) {
        if (value == null) {
            Log.d(TAG, "<writeStringToFile> input string is null, return!!!");
            return;
        }
        File out = new File(desFile);
        PrintStream ps = null;
        try {
            if (out.exists()) {
                out.delete();
            }
            if (!(out.createNewFile())) {
                Log.d(TAG, "<writeStringToFile> createNewFile error");
                return;
            }
            ps = new PrintStream(out);
            ps.println(value);
            ps.flush();
        } catch (IOException e) {
            Log.e(TAG, "<writeStringToFile> Exception ", e);
        } finally {
            out = null;
            if (ps != null) {
                ps.close();
                ps = null;
            }
        }
    }

    /**
     * Get file name from file path.
     * @param filePath
     *            file path
     * @return file name
     */
    public static String getFileNameFromPath(String filePath) {
        TraceHelper.beginSection(">>>>Utils-getFileNameFromPath");
        if (filePath == null) {
            TraceHelper.endSection();
            return null;
        }
        int start = filePath.lastIndexOf("/");
        if (start < 0 || start > filePath.length()) {
            TraceHelper.endSection();
            return filePath;
        }
        String path = filePath.substring(start);
        TraceHelper.endSection();
        return path;
    }

    /**
     * If XMP_DEBUG_LOG is true, print log, otherwise not.
     * @param tag log tag
     * @param msg message
     */
    public static void logD(String tag, String msg) {
        if (XMP_DEBUG_LOG) {
            Log.d(tag, msg);
        }
    }

    /**
     * is GDepth supported, save clear image and google depth map to APP1
     * if not, save clear image to APP15, and use mtk depth buffer
     * @return
     */
    public static boolean isGDepthSupported() {
        return ENABLE_GDEPTH;
    }

    public static String getGDepthTag() {
        return "GDepth/Performance";
    }

    /**
     * encode mask to PNG, first generate bitmap, then compress to bitmap.
     *
     * @param data
     *            mask data
     * @param width
     *            mask width
     * @param height
     *            mask height
     * @return encode data
     */
    public static byte[] encodePng(byte[] data, int width, int height) {

        Log.d(TAG, "<encodePng> data:" + data + ",width:" + width + ",height:" + height);

        if (data == null) {
            Log.d(TAG, "<encodePng> null data, return null!");
            return null;
        }

        long startTime = System.currentTimeMillis();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        byte[] array = new byte[data.length * BYTES_PER_PIXEL];
        Arrays.fill(array, DEFAULT_COLOR);
        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                array[(h * width + w) * BYTES_PER_PIXEL + NUM_3] = data[h * width + w];
            }
        }

        ByteBuffer buffer = ByteBuffer.allocate(array.length);
        buffer.put(array);
        buffer.rewind();
        bitmap.copyPixelsFromBuffer(buffer);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, DEFAULT_COMPRESS_QUALITY, stream);
        byte[] res = stream.toByteArray();
        long endTime = System.currentTimeMillis();
        Log.d(getGDepthTag(), "<encodePng> buffer size: "  + data.length
                + ", spend time:" + (endTime - startTime));

        return res;
    }

}
