package com.mediatek.accessor;

import com.mediatek.accessor.data.GoogleStereoInfo;
import com.mediatek.accessor.data.SegmentMaskInfo;
import com.mediatek.accessor.data.StereoBufferInfo;
import com.mediatek.accessor.data.StereoCaptureInfo;
import com.mediatek.accessor.data.StereoConfigInfo;
import com.mediatek.accessor.data.StereoDepthInfo;

public interface IAccessor {

    byte[] writeStereoCaptureInfo(StereoCaptureInfo captureInfo);

    void writeStereoDepthInfo(String filePath, StereoDepthInfo depthInfo);

    void writeSegmentMaskInfo(String filePath, SegmentMaskInfo maskInfo);

    void writeRefocusImage(String filePath, StereoConfigInfo configInfo, byte[] blurImage);

    StereoDepthInfo readStereoDepthInfo(String filePath);

    SegmentMaskInfo readSegmentMaskInfo(String filePath);

    StereoBufferInfo readStereoBufferInfo(String filePath);

    StereoConfigInfo readStereoConfigInfo(String filePath);

    GoogleStereoInfo readGoogleStereoInfo(String filePath);

    int getGeoVerifyLevel(byte[] configBuffer);

    int getPhoVerifyLevel(byte[] configBuffer);

    int getMtkChaVerifyLevel(byte[] configBuffer);

}
