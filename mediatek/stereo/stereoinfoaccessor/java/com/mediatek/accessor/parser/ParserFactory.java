package com.mediatek.accessor.parser;

import com.mediatek.accessor.data.GoogleStereoInfo;
import com.mediatek.accessor.data.SegmentMaskInfo;
import com.mediatek.accessor.data.StereoBufferInfo;
import com.mediatek.accessor.data.StereoCaptureInfo;
import com.mediatek.accessor.data.StereoConfigInfo;
import com.mediatek.accessor.data.StereoDepthInfo;

import java.util.Map;

/**
 * ParserFactory, for create parser instance.
 */
public class ParserFactory {
    public static final int GOOGLE_STEREO_INFO = 0;
    public static final int SEGMENT_MASK_INFO = 1;
    public static final int STEREO_BUFFER_INFO = 2;
    public static final int STEREO_CONFIG_INFO = 3;
    public static final int STEREO_DEPTH_INFO = 4;
    public static final int STEREO_CAPTURE_INFO = 5;

    /**
     * Get parser instance by information type.
     * @param infoType
     *            data info type
     * @param info
     *            data info
     * @param standardBuffer
     *            standard data buffer
     * @param extendedBuffer
     *            extended data buffer
     * @param customizedBuffer
     *            customized data buffer
     * @return Parser
     */
    public static IParser getParserInstance(int infoType, Object info, byte[] standardBuffer,
            byte[] extendedBuffer, Map<String, byte[]> customizedBuffer) {
        switch (infoType) {
        case GOOGLE_STEREO_INFO:
            return new GoogleStereoInfoParser(standardBuffer, extendedBuffer,
                    (GoogleStereoInfo) info);
        case SEGMENT_MASK_INFO:
            return new SegmentMaskInfoParser(standardBuffer, customizedBuffer,
                    (SegmentMaskInfo) info);
        case STEREO_BUFFER_INFO:
            return new StereoBufferInfoParser(customizedBuffer, (StereoBufferInfo) info);
        case STEREO_CONFIG_INFO:
            return new StereoConfigInfoParser(standardBuffer, extendedBuffer, customizedBuffer,
                    (StereoConfigInfo) info);
        case STEREO_DEPTH_INFO:
            return new StereoDepthInfoParser(standardBuffer, extendedBuffer, customizedBuffer,
                    (StereoDepthInfo) info);
        case STEREO_CAPTURE_INFO:
            return new StereoCaptureInfoParser(standardBuffer, extendedBuffer, customizedBuffer,
                    (StereoCaptureInfo) info);
        default:
            return null;
        }
    }
}
