package com.mediatek.accessor;

import com.mediatek.accessor.data.GoogleStereoInfo;
import com.mediatek.accessor.data.SegmentMaskInfo;
import com.mediatek.accessor.data.StereoBufferInfo;
import com.mediatek.accessor.data.StereoCaptureInfo;
import com.mediatek.accessor.data.StereoConfigInfo;
import com.mediatek.accessor.data.StereoDepthInfo;

public class StereoInfoAccessor_Native implements IAccessor {

    private long mNativeContext;

	static {
		System.loadLibrary("jni_stereoinfoaccessor");
		native_init();
	}

	public StereoInfoAccessor_Native() {
		native_setup();
	}

	@Override
	protected void finalize() {
		native_finalize();
	}

	@Override
    public byte[] writeStereoCaptureInfo(StereoCaptureInfo captureInfo) {
		return native_writeStereoCaptureInfo(captureInfo);
    }

	@Override
    public void writeStereoDepthInfo(String filePath, StereoDepthInfo depthInfo) {
        native_writeStereoDepthInfo(filePath, depthInfo);
    }

	@Override
    public void writeSegmentMaskInfo(String filePath, SegmentMaskInfo maskInfo) {
		native_writeSegmentMaskInfo(filePath, maskInfo);
    }

	@Override
    public void writeRefocusImage(String filePath, StereoConfigInfo configInfo,
                                  byte[] blurImage) {
    	native_writeRefocusImage(filePath, configInfo, blurImage);
    }

	@Override
    public StereoDepthInfo readStereoDepthInfo(String filePath) {
		return native_readStereoDepthInfo(filePath);

    }

	@Override
    public SegmentMaskInfo readSegmentMaskInfo(String filePath) {
		return native_readSegmentMaskInfo(filePath);
    }

	@Override
    public StereoBufferInfo readStereoBufferInfo(String filePath) {
		return native_readStereoBufferInfo(filePath);
    }

	@Override
    public StereoConfigInfo readStereoConfigInfo(String filePath) {
		return native_readStereoConfigInfo(filePath);
    }

	@Override
    public GoogleStereoInfo readGoogleStereoInfo(String filePath) {
		return native_readGoogleStereoInfo(filePath);
    }

	@Override
    public int getGeoVerifyLevel(byte[] configBuffer) {
		return native_getGeoVerifyLevel(configBuffer);
    }

	@Override
    public int getPhoVerifyLevel(byte[] configBuffer) {
		return native_getPhoVerifyLevel(configBuffer);
    }

	@Override
    public int getMtkChaVerifyLevel(byte[] configBuffer) {
		return native_getMtkChaVerifyLevel(configBuffer);
    }

	private static native final void native_init();
	private native final void native_setup();
	private native final void native_finalize();
	private native final byte[] native_writeStereoCaptureInfo(StereoCaptureInfo info);
	private native final void native_writeStereoDepthInfo(String path, StereoDepthInfo info);
	private native final void native_writeSegmentMaskInfo(String path, SegmentMaskInfo info);
	private native final void native_writeRefocusImage(
			String path, StereoConfigInfo info, byte[] blurImage);
	private native final StereoDepthInfo native_readStereoDepthInfo(String path);
	private native final SegmentMaskInfo native_readSegmentMaskInfo(String path);
	private native final StereoBufferInfo native_readStereoBufferInfo(String path);
	private native final StereoConfigInfo native_readStereoConfigInfo(String path);
	private native final GoogleStereoInfo native_readGoogleStereoInfo(String path);
	private native final int native_getGeoVerifyLevel(byte[] buffer);
	private native final int native_getPhoVerifyLevel(byte[] buffer);
	private native final int native_getMtkChaVerifyLevel(byte[] buffer);
}
