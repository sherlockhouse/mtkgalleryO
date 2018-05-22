package com.mediatek.accessor.parser;

import com.mediatek.accessor.data.GoogleStereoInfo;
import com.mediatek.accessor.data.StereoBufferInfo;
import com.mediatek.accessor.data.StereoCaptureInfo;
import com.mediatek.accessor.data.StereoConfigInfo;
import com.mediatek.accessor.data.StereoConfigInfo.FaceDetectionInfo;
import com.mediatek.accessor.data.StereoConfigInfo.FocusInfo;
import com.mediatek.accessor.data.StereoDepthInfo;
import com.mediatek.accessor.meta.data.DataItem.Rect;
import com.mediatek.accessor.operator.IMetaOperator;
import com.mediatek.accessor.operator.MetaOperatorFactory;
import com.mediatek.accessor.util.Log;
import com.mediatek.accessor.util.StereoInfoJsonParser;
import com.mediatek.accessor.util.TraceHelper;
import com.mediatek.accessor.util.Utils;

import java.util.ArrayList;
import java.util.Map;

/**
 * Stereo capture info parser.
 */
public class StereoCaptureInfoParser implements IParser {
    private final static String TAG = Log.Tag(StereoCaptureInfoParser.class.getSimpleName());

    private StereoCaptureInfo mStereoCaptureInfo;
    private StereoBufferInfoParser mStereoBufferInfoParser;
    private StereoConfigInfoParser mStereoConfigInfoParser;
    private StereoDepthInfoParser mStereoDepthInfoParser;
    private GoogleStereoInfoParser mGoogleStereoInfoParser;
    private StereoBufferInfo mBufferInfo;
    private StereoConfigInfo mConfigInfo;
    private StereoDepthInfo mDepthInfo;
    private GoogleStereoInfo mGoogleStereoInfo;
    private IMetaOperator mStandardMetaOperator;
    private IMetaOperator mExtendedMetaOperator;
    private IMetaOperator mCustomizedMetaOperator;

    /**
     * StereoCaptureInfoParser Constructor.
     * @param standardBuffer
     *            use standardMeta to get or set standard XMP info value
     * @param extendedBuffer
     *            use extendedMeta to get or set extended XMP info value
     * @param customizedBuffer
     *            use custMeta to get or set customer XMP info value
     * @param info
     *            StereoCaptureInfo struct for set or get capture info
     */
    public StereoCaptureInfoParser(byte[] standardBuffer, byte[] extendedBuffer,
            Map<String, byte[]> customizedBuffer, StereoCaptureInfo info) {
        mStereoCaptureInfo = info;
        mBufferInfo = new StereoBufferInfo();
        mConfigInfo = new StereoConfigInfo();
        mDepthInfo = new StereoDepthInfo();
        mGoogleStereoInfo = new GoogleStereoInfo();

        mCustomizedMetaOperator =
                MetaOperatorFactory.getOperatorInstance(
                        MetaOperatorFactory.CUSTOMIZED_META_OPERATOR, null,
                        customizedBuffer);
        mStandardMetaOperator =
                MetaOperatorFactory.getOperatorInstance(
                        MetaOperatorFactory.XMP_META_OPERATOR, standardBuffer, null);
        if (Utils.isGDepthSupported()) {
            mExtendedMetaOperator =
                    MetaOperatorFactory.getOperatorInstance(
                            MetaOperatorFactory.XMP_META_OPERATOR, extendedBuffer, null);
        }
    }

    @Override
    public void read() {
    }

    @Override
    public void write() {
        TraceHelper.beginSection(">>>>StereoCaptureInfoParser-write");
        Log.d(TAG, "<write>");
        if (mStereoCaptureInfo == null) {
            Log.d(TAG, "<write> mStereoCaptureInfo is null!");
            TraceHelper.endSection();
            return;
        }
        dumpJsonBuffer("write");
        writeInfo();
        mStereoBufferInfoParser =
                new StereoBufferInfoParser(mCustomizedMetaOperator, mBufferInfo);
        mStereoBufferInfoParser.write();
        // add for kibo+
        mStereoDepthInfoParser = new StereoDepthInfoParser(mStandardMetaOperator, null,
                mCustomizedMetaOperator, mDepthInfo);
        mStereoDepthInfoParser.write();
        mStereoConfigInfoParser =
                new StereoConfigInfoParser(mStandardMetaOperator, null,
                        mCustomizedMetaOperator, mConfigInfo);
        mStereoConfigInfoParser.write();
        if (Utils.isGDepthSupported()) {
            mGoogleStereoInfoParser =
                    new GoogleStereoInfoParser(mStandardMetaOperator,
                            mExtendedMetaOperator, mGoogleStereoInfo);
            mGoogleStereoInfoParser.write();
        }
        TraceHelper.endSection();
    }

    @Override
    public SerializedInfo serialize() {
        TraceHelper.beginSection(">>>>StereoCaptureInfoParser-serialize");
        Log.d(TAG, "<serialize>");
        SerializedInfo info = new SerializedInfo();
        if (mStandardMetaOperator != null) {
            Map<String, byte[]> standardData = mStandardMetaOperator.serialize();
            info.standardXmpBuf = standardData.get(SerializedInfo.XMP_KEY);
        }
        if (Utils.isGDepthSupported() && mExtendedMetaOperator != null) {
            Map<String, byte[]> extendedData = mExtendedMetaOperator.serialize();
            info.extendedXmpBuf = extendedData.get(SerializedInfo.XMP_KEY);
        }
        if (mCustomizedMetaOperator != null) {
            Map<String, byte[]> customizedData = mCustomizedMetaOperator.serialize();
            info.customizedBufMap = customizedData;
        }
        TraceHelper.endSection();
        return info;
    }

    private void writeInfo() {
        TraceHelper.beginSection(">>>>StereoCaptureInfoParser-writeInfo");
        mBufferInfo.debugDir = mStereoCaptureInfo.debugDir;
        mBufferInfo.jpsBuffer = mStereoCaptureInfo.jpsBuffer;

        mConfigInfo.debugDir = mStereoCaptureInfo.debugDir;
        mConfigInfo.ldcBuffer = mStereoCaptureInfo.ldc;
        if (!Utils.isGDepthSupported()) {
            mConfigInfo.clearImage = mStereoCaptureInfo.clearImage;
        }

        if (mStereoCaptureInfo.configBuffer == null) {
            TraceHelper.endSection();
            return;
        }
        StereoInfoJsonParser stereoInfoJsonParser =
                new StereoInfoJsonParser(mStereoCaptureInfo.configBuffer);
        mBufferInfo.maskBuffer = stereoInfoJsonParser.getMaskBuffer();

        mConfigInfo.jpsWidth = stereoInfoJsonParser.getJpsWidth();
        mConfigInfo.jpsHeight = stereoInfoJsonParser.getJpsHeight();
        mConfigInfo.maskWidth = stereoInfoJsonParser.getMaskWidth();
        mConfigInfo.maskHeight = stereoInfoJsonParser.getMaskHeight();
        mConfigInfo.posX = stereoInfoJsonParser.getPosX();
        mConfigInfo.posY = stereoInfoJsonParser.getPosY();
        mConfigInfo.viewWidth = stereoInfoJsonParser.getViewWidth();
        mConfigInfo.viewHeight = stereoInfoJsonParser.getViewHeight();
        mConfigInfo.imageOrientation = stereoInfoJsonParser.getOrientation();
        mConfigInfo.depthOrientation = stereoInfoJsonParser.getDepthRotation();
        mConfigInfo.mainCamPos = stereoInfoJsonParser.getMainCamPos();
        mConfigInfo.touchCoordX1st = stereoInfoJsonParser.getTouchCoordX1st();
        mConfigInfo.touchCoordY1st = stereoInfoJsonParser.getTouchCoordY1st();
        mConfigInfo.faceCount = stereoInfoJsonParser.getFaceRectCount();
        mConfigInfo.fdInfoArray = prepareFdInfo(stereoInfoJsonParser, mConfigInfo.faceCount);
        FocusInfo focusInfo = new FocusInfo();
        focusInfo.focusType = stereoInfoJsonParser.getFocusType();
        focusInfo.focusLeft = stereoInfoJsonParser.getFocusLeft();
        focusInfo.focusTop = stereoInfoJsonParser.getFocusTop();
        focusInfo.focusRight = stereoInfoJsonParser.getFocusRight();
        focusInfo.focusBottom = stereoInfoJsonParser.getFocusBottom();
        mConfigInfo.focusInfo = focusInfo;
        mConfigInfo.dofLevel = stereoInfoJsonParser.getDof();
        mConfigInfo.convOffset = stereoInfoJsonParser.getConvOffset();
        mConfigInfo.ldcWidth = stereoInfoJsonParser.getLdcWidth();
        mConfigInfo.ldcHeight = stereoInfoJsonParser.getLdcHeight();

        mConfigInfo.isFace = stereoInfoJsonParser.getFaceFlag();
        mConfigInfo.faceRatio = (float) stereoInfoJsonParser.getFaceRatio();
        mConfigInfo.curDac = stereoInfoJsonParser.getCurDac();
        mConfigInfo.minDac = stereoInfoJsonParser.getMinDac();
        mConfigInfo.maxDac = stereoInfoJsonParser.getMaxDac();

        // add for kibo+
        mDepthInfo.debugDir = mStereoCaptureInfo.debugDir;
        mDepthInfo.metaBufferWidth = stereoInfoJsonParser.getMetaBufferWidth();
        mDepthInfo.metaBufferHeight = stereoInfoJsonParser.getMetaBufferHeight();
        mDepthInfo.depthBufferWidth = stereoInfoJsonParser.getDepthBufferWidth();
        mDepthInfo.depthBufferHeight = stereoInfoJsonParser.getDepthBufferHeight();
        mDepthInfo.depthBuffer = mStereoCaptureInfo.depthBuffer;
        mDepthInfo.debugBuffer = mStereoCaptureInfo.debugBuffer;

        // add for GDepth
        // the clear image and depth map will got from camera app
        // other simple value will set default value temporarily
        if (Utils.isGDepthSupported()) {
            mGoogleStereoInfo.clearImage = mStereoCaptureInfo.clearImage;
            // encode depth map to PNG format
            if (mStereoCaptureInfo.depthMap != null) {
                byte[] pngBuffer = Utils.encodePng(mStereoCaptureInfo.depthMap,
                        stereoInfoJsonParser.getGoogleDepthWidth(),
                        stereoInfoJsonParser.getGoogleDepthHeight());
                mGoogleStereoInfo.depthMap = pngBuffer;
            }
            mGoogleStereoInfo.focusBlurAtInfinity = 0.055234075;
            mGoogleStereoInfo.focusFocalDistance = 23.359299;
            mGoogleStereoInfo.focusFocalPointX = 0.5416667;
            mGoogleStereoInfo.focusFocalPointY = 0.4586397;
            mGoogleStereoInfo.imageMime = "image/jpeg";
            mGoogleStereoInfo.depthFormat = "RangeInverse";
            mGoogleStereoInfo.depthNear = 17.3202400207519;
            mGoogleStereoInfo.depthFar = 111.881546020507;
            mGoogleStereoInfo.depthMime = "image/jpeg";
        }
        TraceHelper.endSection();
    }

    private ArrayList<FaceDetectionInfo> prepareFdInfo(StereoInfoJsonParser parser,
                                                       int faceCount) {
        if (faceCount <= 0 || parser == null) {
            Log.d(TAG, "<prepareFdInfo> invalid params!!");
            return null;
        }
        ArrayList<FaceDetectionInfo> faceInfo = new ArrayList<FaceDetectionInfo>();
        Rect fd = null;
        int rip = 0;
        for (int i = 0; i < faceCount; i++) {
            fd = parser.getFaceRect(i);
            rip = parser.getFaceRip(i);
            if (fd != null) {
                FaceDetectionInfo info =
                        new FaceDetectionInfo(fd.left, fd.top, fd.right, fd.bottom, rip);
                faceInfo.add(i, info);
                Log.d(TAG, "<prepareFdInfo> faceInfo-" + "i: " + info);
            }
        }
        return faceInfo;
    }

    private void dumpJsonBuffer(String suffix) {
        if (!Utils.ENABLE_BUFFER_DUMP) {
            return;
        }
        String dumpPath = Utils.DUMP_FILE_FOLDER + "/" + mStereoCaptureInfo.debugDir + "/";
        Log.d(TAG, "<dumpJsonBuffer> dumpPath: " + dumpPath);
        if (mStereoCaptureInfo.configBuffer != null) {
            Utils.writeBufferToFile(dumpPath + "StereoCaptureInfo_jsonConfigBuffer_" + suffix
                    + ".txt", mStereoCaptureInfo.configBuffer);
        } else {
            Log.d(TAG, "<dumpJsonBuffer> mStereoCaptureInfo.configBuffer is null!");
        }
    }
}
