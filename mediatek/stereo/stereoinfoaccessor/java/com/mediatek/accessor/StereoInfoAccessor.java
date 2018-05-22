package com.mediatek.accessor;

import com.mediatek.accessor.data.GoogleStereoInfo;
import com.mediatek.accessor.data.SegmentMaskInfo;
import com.mediatek.accessor.data.StereoBufferInfo;
import com.mediatek.accessor.data.StereoCaptureInfo;
import com.mediatek.accessor.data.StereoConfigInfo;
import com.mediatek.accessor.data.StereoDepthInfo;
import com.mediatek.accessor.packer.PackInfo;
import com.mediatek.accessor.packer.PackerManager;
import com.mediatek.accessor.parser.IParser;
import com.mediatek.accessor.parser.ParserFactory;
import com.mediatek.accessor.parser.SerializedInfo;
import com.mediatek.accessor.util.Log;
import com.mediatek.accessor.util.ReadWriteLockFileUtils;
import com.mediatek.accessor.util.StereoInfoJsonParser;
import com.mediatek.accessor.util.SystemPropertyUtils;
import com.mediatek.accessor.util.TraceHelper;
import com.mediatek.accessor.util.Utils;

/**
 * StereoInfoAccessor, provide simple utility methods for third application, to read or write some
 * xmp or stereo related information.
 */
public class StereoInfoAccessor {
    private final static String TAG = Log.Tag(StereoInfoAccessor.class.getSimpleName());

    private static final int ACCESSOR_TYPE = SystemPropertyUtils.getInt(
            "stereoinfoaccessor.type", AccessorFactory.ACCESSOR_Native);
    private IAccessor mAccessor;

    public StereoInfoAccessor() {
        mAccessor = AccessorFactory.createAccessor(ACCESSOR_TYPE);
        if (mAccessor == null) {
            throw new IllegalArgumentException("invalid accessor type: " + ACCESSOR_TYPE);
        }
    }

    /**
     * write stereo capture info to file buffer.
     * @param captureInfo
     *            StereoCaptureInfo
     * @return JpgBuffer
     */
    public byte[] writeStereoCaptureInfo(StereoCaptureInfo captureInfo) {
        long startTime = System.currentTimeMillis();
        byte[] ret = mAccessor.writeStereoCaptureInfo(captureInfo);
        long elapsedTime = System.currentTimeMillis() - startTime;
        Log.d(TAG, "<writeStereoCaptureInfo> elapsed time = " + elapsedTime + "ms");
        return ret;
    }

    /**
     * write stereo depth info to file buffer.
     * @param filePath
     *            file path
     * @param depthInfo
     *            StereoDepthInfo
     */
    public void writeStereoDepthInfo(String filePath, StereoDepthInfo depthInfo) {
        long startTime = System.currentTimeMillis();
        mAccessor.writeStereoDepthInfo(filePath, depthInfo);
        long elapsedTime = System.currentTimeMillis() - startTime;
        Log.d(TAG, "<writeStereoDepthInfo> elapsed time = " + elapsedTime + "ms");
    }

    /**
     * write segment and mask info to file buffer.
     * @param filePath
     *            file path
     * @param maskInfo
     *            SegmentMaskInfo
     */
    public void writeSegmentMaskInfo(String filePath, SegmentMaskInfo maskInfo) {
        long startTime = System.currentTimeMillis();
        mAccessor.writeSegmentMaskInfo(filePath, maskInfo);
        long elapsedTime = System.currentTimeMillis() - startTime;
        Log.d(TAG, "<writeSegmentMaskInfo> elapsed time = " + elapsedTime + "ms");
    }

    /**
     * write refocus info to file buffer.
     * @param filePath
     *            file path
     * @param configInfo
     *            StereoConfigInfo
     * @param blurImage
     *            buffer
     */
    public void writeRefocusImage(String filePath, StereoConfigInfo configInfo,
                                  byte[] blurImage) {
        long startTime = System.currentTimeMillis();
        mAccessor.writeRefocusImage(filePath, configInfo, blurImage);
        long elapsedTime = System.currentTimeMillis() - startTime;
        Log.d(TAG, "<writeRefocusImage> elapsed time = " + elapsedTime + "ms");
    }

    /**
     * Read depth information of stereo image.
     * @param filePath
     *            stereo image file path
     * @return StereoDepthInfo
     */
    public StereoDepthInfo readStereoDepthInfo(String filePath) {
        long startTime = System.currentTimeMillis();
        StereoDepthInfo ret = mAccessor.readStereoDepthInfo(filePath);
        long elapsedTime = System.currentTimeMillis() - startTime;
        Log.d(TAG, "<readStereoDepthInfo> elapsed time = " + elapsedTime + "ms");
        return ret;
    }

    /**
     * Read segment mask information of stereo image.
     * @param filePath
     *            stereo image file path
     * @return SegmentMaskInfo
     */
    public SegmentMaskInfo readSegmentMaskInfo(String filePath) {
        long startTime = System.currentTimeMillis();
        SegmentMaskInfo ret = mAccessor.readSegmentMaskInfo(filePath);
        long elapsedTime = System.currentTimeMillis() - startTime;
        Log.d(TAG, "<readSegmentMaskInfo> elapsed time = " + elapsedTime + "ms");
        return ret;
    }

    /**
     * Read stereo buffer information of stereo image.
     * @param filePath
     *            stereo image file path
     * @return StereoBufferInfo
     */
    public StereoBufferInfo readStereoBufferInfo(String filePath) {
        long startTime = System.currentTimeMillis();
        StereoBufferInfo ret = mAccessor.readStereoBufferInfo(filePath);
        long elapsedTime = System.currentTimeMillis() - startTime;
        Log.d(TAG, "<readStereoBufferInfo> elapsed time = " + elapsedTime + "ms");
        return ret;
    }

    /**
     * Read stereo config information of stereo image.
     * @param filePath
     *            stereo image file path
     * @return SegmentMaskInfo
     */
    public StereoConfigInfo readStereoConfigInfo(String filePath) {
        long startTime = System.currentTimeMillis();
        StereoConfigInfo ret = mAccessor.readStereoConfigInfo(filePath);
        long elapsedTime = System.currentTimeMillis() - startTime;
        Log.d(TAG, "<readStereoConfigInfo> elapsed time = " + elapsedTime + "ms");
        return ret;
    }

    /**
     * Read segment mask information of stereo image.
     * @param filePath
     *            stereo image file path
     * @return SegmentMaskInfo
     */
    public GoogleStereoInfo readGoogleStereoInfo(String filePath) {
        long startTime = System.currentTimeMillis();
        GoogleStereoInfo ret = mAccessor.readGoogleStereoInfo(filePath);
        long elapsedTime = System.currentTimeMillis() - startTime;
        Log.d(TAG, "<readGoogleStereoInfo> elapsed time = " + elapsedTime + "ms");
        return ret;
    }

    /**
     * Get Geo verify level, debug tool's using.
     * @param configBuffer
     *            json config buffer
     * @return Geo verify level
     */
    public int getGeoVerifyLevel(byte[] configBuffer) {
        long startTime = System.currentTimeMillis();
        int ret = mAccessor.getGeoVerifyLevel(configBuffer);
        long elapsedTime = System.currentTimeMillis() - startTime;
        Log.d(TAG, "<getGeoVerifyLevel> elapsed time = " + elapsedTime + "ms");
        return ret;
    }

    /**
     * Get Pho verify level, debug tool's using.
     * @param configBuffer
     *            json config buffer
     * @return Pho verify level.
     */
    public int getPhoVerifyLevel(byte[] configBuffer) {
        long startTime = System.currentTimeMillis();
        int ret = mAccessor.getPhoVerifyLevel(configBuffer);
        long elapsedTime = System.currentTimeMillis() - startTime;
        Log.d(TAG, "<getPhoVerifyLevel> elapsed time = " + elapsedTime + "ms");
        return ret;
    }

    /**
     * Get Cha verify level, debug tool's using.
     * @param configBuffer
     *            json config buffer
     * @return Cha verify level.
     */
    public int getMtkChaVerifyLevel(byte[] configBuffer) {
        long startTime = System.currentTimeMillis();
        int ret = mAccessor.getMtkChaVerifyLevel(configBuffer);
        long elapsedTime = System.currentTimeMillis() - startTime;
        Log.d(TAG, "<getMtkChaVerifyLevel> elapsed time = " + elapsedTime + "ms");
        return ret;
    }

    public static class AccessorFactory {

        public static final int ACCESSOR_JAVA = 0;
        public static final int ACCESSOR_Native = 1;

        public static IAccessor createAccessor(int type) {
            switch (type) {
                case ACCESSOR_JAVA:
                    Log.d(TAG, "create java accessor");
                    return new StereoInfoAccessor_Java();
                case ACCESSOR_Native:
                    Log.d(TAG, "create native accessor");
                    return new StereoInfoAccessor_Native();
            }
            Log.w(TAG, "cannot create any accessor");
            return null;
        }
    }
}
