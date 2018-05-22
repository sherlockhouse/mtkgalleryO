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
import com.mediatek.accessor.util.TraceHelper;
import com.mediatek.accessor.util.Utils;

/**
 * StereoInfoAccessor, provide simple utility methods for third application, to read or write some
 * xmp or stereo related information.
 */
public class StereoInfoAccessor_Java implements IAccessor {
    private final static String TAG = Log.Tag(StereoInfoAccessor_Java.class.getSimpleName());

    /**
     * write stereo capture info to file buffer.
     * @param captureInfo
     *            StereoCaptureInfo
     * @return JpgBuffer
     */
    @Override
    public byte[] writeStereoCaptureInfo(StereoCaptureInfo captureInfo) {
        TraceHelper.beginSection(">>>>StereoInfoAccessor-writeStereoCaptureInfo");
        Log.d(TAG, "<writeStereoCaptureInfo> captureInfo " + captureInfo);
        if (captureInfo == null) {
            Log.d(TAG, "<writeStereoCaptureInfo> captureInfo is null!");
            TraceHelper.endSection();
            return null;
        }
        String dumpPath = Utils.DUMP_FILE_FOLDER + "/" + captureInfo.debugDir + "/";
        if (Utils.ENABLE_BUFFER_DUMP) {
            Utils.writeBufferToFile(dumpPath + "StereoCaptureInfo_oriJpgBuffer_write.jpg",
                    captureInfo.jpgBuffer);
        }
        PackInfo packInfo = new PackInfo();
        PackerManager packerManager = new PackerManager();
        packInfo.unpackedJpgBuf = captureInfo.jpgBuffer;
        IParser stereoCaptureInfoParser = ParserFactory.getParserInstance(
                ParserFactory.STEREO_CAPTURE_INFO, captureInfo, packInfo.unpackedStandardXmpBuf,
                packInfo.unpackedExtendedXmpBuf, packInfo.unpackedCustomizedBufMap);
        stereoCaptureInfoParser.write();
        serialize(packInfo, stereoCaptureInfoParser);
        byte[] result = packerManager.pack(packInfo);
        if (Utils.ENABLE_BUFFER_DUMP) {
            Utils.writeBufferToFile(dumpPath + "StereoCaptureInfo_packedJpgBuffer_write.jpg",
                    result);
        }
        TraceHelper.endSection();
        return result;
    }

    /**
     * write stereo depth info to file buffer.
     * @param filePath
     *            file path
     * @param depthInfo
     *            StereoDepthInfo
     */
    @Override
    public void writeStereoDepthInfo(String filePath, StereoDepthInfo depthInfo) {
        TraceHelper.beginSection(">>>>StereoInfoAccessor-writeStereoDepthInfo");
        Log.d(TAG, "<writeStereoDepthInfo> filePath " + filePath + ", depthInfo " + depthInfo);
        if (depthInfo == null) {
            Log.d(TAG, "<writeStereoDepthInfo> depthInfo is null!");
            TraceHelper.endSection();
            return;
        }

        PackerManager packerManager = new PackerManager();
        try {
            ReadWriteLockFileUtils.writeLock(filePath);
            byte[] fileBuffer = Utils.readFileToBuffer(filePath);
            if (fileBuffer == null) {
                Log.d(TAG, "<writeStereoDepthInfo> fileBuffer is null!");
                TraceHelper.endSection();
                return;
            }
            String fileName = Utils.getFileNameFromPath(filePath);
            if (fileName != null && fileName.length() > 0) {
                depthInfo.debugDir = fileName;
            }
            PackInfo packInfo = packerManager.unpack(fileBuffer);
            packInfo.unpackedJpgBuf = fileBuffer;
            IParser stereoDepthInfoParser =
                    ParserFactory.getParserInstance(ParserFactory.STEREO_DEPTH_INFO,
                            depthInfo, packInfo.unpackedStandardXmpBuf,
                            packInfo.unpackedExtendedXmpBuf,
                            packInfo.unpackedCustomizedBufMap);
            stereoDepthInfoParser.write();
            serialize(packInfo, stereoDepthInfoParser);
            packerManager.pack(packInfo);
            Utils.writeBufferToFile(filePath, packInfo.packedJpgBuf);
        } finally {
            ReadWriteLockFileUtils.writeUnlock(filePath);
        }
        TraceHelper.endSection();
    }

    /**
     * write segment and mask info to file buffer.
     * @param filePath
     *            file path
     * @param maskInfo
     *            SegmentMaskInfo
     */
    @Override
    public void writeSegmentMaskInfo(String filePath, SegmentMaskInfo maskInfo) {
        TraceHelper.beginSection(">>>>StereoInfoAccessor-writeSegmentMaskInfo");
        Log.d(TAG, "<writeSegmentMaskInfo> filePath " + filePath + ", maskInfo " + maskInfo);
        if (maskInfo == null) {
            Log.d(TAG, "<writeSegmentMaskInfo> maskInfo is null!");
            TraceHelper.endSection();
            return;
        }

        PackerManager packerManager = new PackerManager();
        try {
            ReadWriteLockFileUtils.writeLock(filePath);
            byte[] fileBuffer = Utils.readFileToBuffer(filePath);
            if (fileBuffer == null) {
                Log.d(TAG, "<writeSegmentMaskInfo> fileBuffer is null!");
                TraceHelper.endSection();
                return;
            }
            String fileName = Utils.getFileNameFromPath(filePath);
            if (fileName != null && fileName.length() > 0) {
                maskInfo.debugDir = fileName;
            }
            PackInfo packInfo = packerManager.unpack(fileBuffer);
            packInfo.unpackedJpgBuf = fileBuffer;
            IParser segmentMaskInfoParser =
                    ParserFactory.getParserInstance(ParserFactory.SEGMENT_MASK_INFO,
                            maskInfo, packInfo.unpackedStandardXmpBuf,
                            packInfo.unpackedExtendedXmpBuf,
                            packInfo.unpackedCustomizedBufMap);
            segmentMaskInfoParser.write();
            serialize(packInfo, segmentMaskInfoParser);
            packerManager.pack(packInfo);
            Utils.writeBufferToFile(filePath, packInfo.packedJpgBuf);
        } finally {
            ReadWriteLockFileUtils.writeUnlock(filePath);
        }
        TraceHelper.endSection();
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
    @Override
    public void writeRefocusImage(String filePath, StereoConfigInfo configInfo,
                                  byte[] blurImage) {
        TraceHelper.beginSection(">>>>StereoInfoAccessor-writeRefocusImage");
        Log.d(TAG, "<writeRefocusImage> filePath " + filePath + ", StereoConfigInfo " + configInfo);
        if (configInfo == null) {
            Log.d(TAG, "<writeRefocusImage> configInfo is null!");
            TraceHelper.endSection();
            return;
        }

        PackerManager packerManager = new PackerManager();
        try {
            ReadWriteLockFileUtils.writeLock(filePath);
            byte[] fileBuffer = Utils.readFileToBuffer(filePath);
            if (fileBuffer == null) {
                Log.d(TAG, "<writeRefocusImage> fileBuffer is null!");
                TraceHelper.endSection();
                return;
            }
            String fileName = Utils.getFileNameFromPath(filePath);
            if (fileName != null && fileName.length() > 0) {
                configInfo.debugDir = fileName;
            }
            PackInfo packInfo = packerManager.unpack(fileBuffer);
            packInfo.unpackedJpgBuf = fileBuffer;
            IParser stereoConfigInfoParser =
                    ParserFactory.getParserInstance(ParserFactory.STEREO_CONFIG_INFO,
                            configInfo, packInfo.unpackedStandardXmpBuf,
                            packInfo.unpackedExtendedXmpBuf,
                            packInfo.unpackedCustomizedBufMap);
            stereoConfigInfoParser.write();
            serialize(packInfo, stereoConfigInfoParser);
            packInfo.unpackedBlurImageBuf = blurImage;
            packerManager.pack(packInfo);
            Utils.writeBufferToFile(filePath, packInfo.packedJpgBuf);
        } finally {
            ReadWriteLockFileUtils.writeUnlock(filePath);
        }
        TraceHelper.endSection();
    }

    /**
     * Read depth information of stereo image.
     * @param filePath
     *            stereo image file path
     * @return StereoDepthInfo
     */
    @Override
    public StereoDepthInfo readStereoDepthInfo(String filePath) {
        TraceHelper.beginSection(">>>>StereoInfoAccessor-readStereoDepthInfo");
        Log.d(TAG, "<readStereoDepthInfo> filePath " + filePath);
        StereoDepthInfo info = new StereoDepthInfo();
        PackerManager packerManager = new PackerManager();
        try {
            ReadWriteLockFileUtils.readLock(filePath);
            byte[] fileBuffer = Utils.readFileToBuffer(filePath);
            if (fileBuffer == null) {
                Log.d(TAG, "<readStereoDepthInfo> fileBuffer is null!");
                TraceHelper.endSection();
                return info;
            }
            String fileName = Utils.getFileNameFromPath(filePath);
            if (fileName != null && fileName.length() > 0) {
                info.debugDir = fileName;
            }
            PackInfo packInfo = packerManager.unpack(fileBuffer);
            packInfo.unpackedJpgBuf = fileBuffer;
            IParser stereoDepthInfoParser =
                    ParserFactory.getParserInstance(ParserFactory.STEREO_DEPTH_INFO,
                            info, packInfo.unpackedStandardXmpBuf,
                            packInfo.unpackedExtendedXmpBuf,
                            packInfo.unpackedCustomizedBufMap);
            stereoDepthInfoParser.read();
        } finally {
            ReadWriteLockFileUtils.readUnlock(filePath);
        }
        TraceHelper.endSection();
        return info;
    }

    /**
     * Read segment mask information of stereo image.
     * @param filePath
     *            stereo image file path
     * @return SegmentMaskInfo
     */
    @Override
    public SegmentMaskInfo readSegmentMaskInfo(String filePath) {
        TraceHelper.beginSection(">>>>StereoInfoAccessor-readSegmentMaskInfo");
        Log.d(TAG, "<readSegmentMaskInfo> filePath " + filePath);
        SegmentMaskInfo info = new SegmentMaskInfo();
        PackerManager packerManager = new PackerManager();
        try {
            ReadWriteLockFileUtils.readLock(filePath);
            byte[] fileBuffer = Utils.readFileToBuffer(filePath);
            if (fileBuffer == null) {
                Log.d(TAG, "<readSegmentMaskInfo> fileBuffer is null!");
                TraceHelper.endSection();
                return info;
            }
            String fileName = Utils.getFileNameFromPath(filePath);
            if (fileName != null && fileName.length() > 0) {
                info.debugDir = fileName;
            }
            PackInfo packInfo = packerManager.unpack(fileBuffer);
            IParser segmentMaskInfoParser =
                    ParserFactory.getParserInstance(ParserFactory.SEGMENT_MASK_INFO,
                            info, packInfo.unpackedStandardXmpBuf,
                            packInfo.unpackedExtendedXmpBuf,
                            packInfo.unpackedCustomizedBufMap);
            segmentMaskInfoParser.read();
        } finally {
            ReadWriteLockFileUtils.readUnlock(filePath);
        }
        TraceHelper.endSection();
        return info;
    }

    /**
     * Read stereo buffer information of stereo image.
     * @param filePath
     *            stereo image file path
     * @return StereoBufferInfo
     */
    @Override
    public StereoBufferInfo readStereoBufferInfo(String filePath) {
        TraceHelper.beginSection(">>>>StereoInfoAccessor-readStereoBufferInfo");
        Log.d(TAG, "<readStereoBufferInfo> filePath " + filePath);
        StereoBufferInfo info = new StereoBufferInfo();
        PackerManager packerManager = new PackerManager();
        try {
            ReadWriteLockFileUtils.readLock(filePath);
            byte[] fileBuffer = Utils.readFileToBuffer(filePath);
            if (fileBuffer == null) {
                Log.d(TAG, "<readStereoBufferInfo> fileBuffer is null!");
                TraceHelper.endSection();
                return info;
            }
            String fileName = Utils.getFileNameFromPath(filePath);
            if (fileName != null && fileName.length() > 0) {
                info.debugDir = fileName;
            }
            PackInfo packInfo = packerManager.unpack(fileBuffer);
            IParser stereoBufferInfoParser =
                    ParserFactory.getParserInstance(ParserFactory.STEREO_BUFFER_INFO,
                            info, packInfo.unpackedStandardXmpBuf,
                            packInfo.unpackedExtendedXmpBuf,
                            packInfo.unpackedCustomizedBufMap);
            stereoBufferInfoParser.read();
        } finally {
            ReadWriteLockFileUtils.readUnlock(filePath);
        }
        TraceHelper.endSection();
        return info;
    }

    /**
     * Read stereo config information of stereo image.
     * @param filePath
     *            stereo image file path
     * @return SegmentMaskInfo
     */
    @Override
    public StereoConfigInfo readStereoConfigInfo(String filePath) {
        TraceHelper.beginSection(">>>>StereoInfoAccessor-readStereoConfigInfo");
        Log.d(TAG, "<readStereoConfigInfo> filePath " + filePath);
        StereoConfigInfo info = new StereoConfigInfo();
        PackerManager packerManager = new PackerManager();
        try {
            ReadWriteLockFileUtils.readLock(filePath);
            byte[] fileBuffer = Utils.readFileToBuffer(filePath);
            if (fileBuffer == null) {
                Log.d(TAG, "<readStereoConfigInfo> fileBuffer is null!");
                TraceHelper.endSection();
                return info;
            }
            String fileName = Utils.getFileNameFromPath(filePath);
            if (fileName != null && fileName.length() > 0) {
                info.debugDir = fileName;
            }
            PackInfo packInfo = packerManager.unpack(fileBuffer);
            IParser stereoConfigInfoParser =
                    ParserFactory.getParserInstance(ParserFactory.STEREO_CONFIG_INFO,
                            info, packInfo.unpackedStandardXmpBuf,
                            packInfo.unpackedExtendedXmpBuf,
                            packInfo.unpackedCustomizedBufMap);
            stereoConfigInfoParser.read();
        } finally {
            ReadWriteLockFileUtils.readUnlock(filePath);
        }
        TraceHelper.endSection();
        return info;
    }

    /**
     * Read segment mask information of stereo image.
     * @param filePath
     *            stereo image file path
     * @return SegmentMaskInfo
     */
    @Override
    public GoogleStereoInfo readGoogleStereoInfo(String filePath) {
        TraceHelper.beginSection(">>>>StereoInfoAccessor-readGoogleStereoInfo");
        Log.d(TAG, "<readGoogleStereoInfo> filePath " + filePath);
        GoogleStereoInfo info = new GoogleStereoInfo();
        PackerManager packerManager = new PackerManager();
        try {
            ReadWriteLockFileUtils.readLock(filePath);
            byte[] fileBuffer = Utils.readFileToBuffer(filePath);
            if (fileBuffer == null) {
                Log.d(TAG, "<readGoogleStereoInfo> fileBuffer is null!");
                TraceHelper.endSection();
                return info;
            }
            String fileName = Utils.getFileNameFromPath(filePath);
            if (fileName != null && fileName.length() > 0) {
                info.debugDir = fileName;
            }
            PackInfo packInfo = packerManager.unpack(fileBuffer);
            IParser googleStereoInfoParser =
                    ParserFactory.getParserInstance(ParserFactory.GOOGLE_STEREO_INFO,
                            info, packInfo.unpackedStandardXmpBuf,
                            packInfo.unpackedExtendedXmpBuf,
                            packInfo.unpackedCustomizedBufMap);
            googleStereoInfoParser.read();
        } finally {
            ReadWriteLockFileUtils.readUnlock(filePath);
        }
        TraceHelper.endSection();
        return info;
    }

    /**
     * Get Geo verify level, debug tool's using.
     * @param configBuffer
     *            json config buffer
     * @return Geo verify level
     */
    @Override
    public int getGeoVerifyLevel(byte[] configBuffer) {
        if (configBuffer == null) {
            Log.d(TAG, "<getGeoVerifyLevel> configBuffer is null!!");
            return -1;
        }
        StereoInfoJsonParser parser = new StereoInfoJsonParser(configBuffer);
        return parser.getGeoVerifyLevel();
    }

    /**
     * Get Pho verify level, debug tool's using.
     * @param configBuffer
     *            json config buffer
     * @return Pho verify level.
     */
    @Override
    public int getPhoVerifyLevel(byte[] configBuffer) {
        if (configBuffer == null) {
            Log.d(TAG, "<getPhoVerifyLevel> configBuffer is null!!");
            return -1;
        }
        StereoInfoJsonParser parser = new StereoInfoJsonParser(configBuffer);
        return parser.getPhoVerifyLevel();
    }

    /**
     * Get Cha verify level, debug tool's using.
     * @param configBuffer
     *            json config buffer
     * @return Cha verify level.
     */
    @Override
    public int getMtkChaVerifyLevel(byte[] configBuffer) {
        if (configBuffer == null) {
            Log.d(TAG, "<getMtkChaVerifyLevel> configBuffer is null!!");
            return -1;
        }
        StereoInfoJsonParser parser = new StereoInfoJsonParser(configBuffer);
        return parser.getMtkChaVerifyLevel();
    }

    private void serialize(PackInfo info, IParser parser) {
        SerializedInfo serializedInfo = parser.serialize();
        info.unpackedStandardXmpBuf = serializedInfo.standardXmpBuf;
        info.unpackedExtendedXmpBuf = serializedInfo.extendedXmpBuf;
        info.unpackedCustomizedBufMap = serializedInfo.customizedBufMap;
    }
}
