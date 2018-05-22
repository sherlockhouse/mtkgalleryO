package com.mediatek.accessor.parser;

import com.mediatek.accessor.data.GoogleStereoInfo;
import com.mediatek.accessor.meta.data.DataItem;
import com.mediatek.accessor.meta.data.DataItem.BufferItem;
import com.mediatek.accessor.meta.data.DataItem.DataCollections;
import com.mediatek.accessor.meta.data.DataItem.NameSpaceItem;
import com.mediatek.accessor.meta.data.DataItem.SimpleItem;
import com.mediatek.accessor.operator.IMetaOperator;
import com.mediatek.accessor.operator.MetaOperatorFactory;
import com.mediatek.accessor.util.Log;
import com.mediatek.accessor.util.TraceHelper;
import com.mediatek.accessor.util.Utils;

import java.util.ArrayList;
import java.util.Map;

/**
 * Google stereo info parser.
 */
public class GoogleStereoInfoParser implements IParser {
    private final static String TAG = Log.Tag(GoogleStereoInfoParser.class.getSimpleName());

    private static final String NS_GFOCUS = "http://ns.google.com/photos/1.0/focus/";
    private static final String NS_GIMAGE = "http://ns.google.com/photos/1.0/image/";
    private static final String NS_GDEPTH = "http://ns.google.com/photos/1.0/depthmap/";
    private static final String PRIFIX_GFOCUS = "GFocus";
    private static final String PRIFIX_GIMAGE = "GImage";
    private static final String PRIFIX_GDEPTH = "GDepth";
    private static final String ATTRIBUTE_GFOCUS_BLUR_INFINITY = "BlurAtInfinity";
    private static final String ATTRIBUTE_GFOCUS_FOCALDISTANCE = "FocalDistance";
    private static final String ATTRIBUTE_GFOCUS_FOCALPOINTX = "FocalPointX";
    private static final String ATTRIBUTE_GFOCUS_FOCALPOINTY = "FocalPointY";
    private static final String ATTRIBUTE_GIMAGE_MIME = "Mime";
    private static final String ATTRIBUTE_GDEPTH_FORMAT = "Format";
    private static final String ATTRIBUTE_GDEPTH_NEAR = "Near";
    private static final String ATTRIBUTE_GDEPTH_FAR = "Far";
    private static final String ATTRIBUTE_GDEPTH_MIME = "Mime";
    private static final String ATTRIBUTE_DEPTH_MAP = "Data";
    private static final String ATTRIBUTE_CLEAR_IMAGE = "Data";

    private IMetaOperator mStandardMetaOperator;
    private IMetaOperator mExtendedMetaOperator;
    private DataCollections mStandardDataCollections = new DataCollections();
    private DataCollections mExtendardDataCollections = new DataCollections();
    private ArrayList<SimpleItem> mListOfSimpleValue = new ArrayList<SimpleItem>();
    private ArrayList<BufferItem> mListOfBufferItem = new ArrayList<BufferItem>();
    private GoogleStereoInfo mGoogleStereoInfo;

    /**
     * GoogleStereoInfoParser Constructor.
     * @param standardBuffer
     *            use standardMeta to get or set standard XMP info value
     * @param extendedBuffer
     *            use extendedMeta to get or set extended XMP info value
     * @param info
     *            GoogleStereoInfo struct for set or get Google info
     */
    public GoogleStereoInfoParser(byte[] standardBuffer, byte[] extendedBuffer,
            GoogleStereoInfo info) {
        mStandardDataCollections.dest = DataItem.DEST_TYPE_STANDARD_XMP;
        mExtendardDataCollections.dest = DataItem.DEST_TYPE_EXTENDED_XMP;
        mGoogleStereoInfo = info;
        initSimpleValue();
        initBufferItem();

        mStandardDataCollections.listOfSimpleValue = mListOfSimpleValue;
        mStandardMetaOperator =
                MetaOperatorFactory.getOperatorInstance(
                        MetaOperatorFactory.XMP_META_OPERATOR, standardBuffer, null);
        try {
            mStandardMetaOperator.setData(mStandardDataCollections);
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }

        mExtendardDataCollections.listOfBufferItem = mListOfBufferItem;
        mExtendedMetaOperator =
                MetaOperatorFactory.getOperatorInstance(
                        MetaOperatorFactory.XMP_META_OPERATOR, extendedBuffer, null);
        try {
            mExtendedMetaOperator.setData(mExtendardDataCollections);
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * GoogleStereoInfoParser Constructor.
     * @param standardMetaOperator
     *            use standardMeta to get or set standard XMP info value
     * @param extendedMetaOperator
     *            use extendedMeta to get or set extended XMP info value
     * @param info
     *            GoogleStereoInfo struct for set or get Google info
     */
    public GoogleStereoInfoParser(IMetaOperator standardMetaOperator,
            IMetaOperator extendedMetaOperator, GoogleStereoInfo info) {
        mStandardDataCollections.dest = DataItem.DEST_TYPE_STANDARD_XMP;
        mExtendardDataCollections.dest = DataItem.DEST_TYPE_EXTENDED_XMP;
        mGoogleStereoInfo = info;
        initSimpleValue();
        initBufferItem();

        mStandardDataCollections.listOfSimpleValue = mListOfSimpleValue;
        mStandardMetaOperator = standardMetaOperator;
        try {
            mStandardMetaOperator.setData(mStandardDataCollections);
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }

        mExtendardDataCollections.listOfBufferItem = mListOfBufferItem;
        mExtendedMetaOperator = extendedMetaOperator;
        try {
            mExtendedMetaOperator.setData(mExtendardDataCollections);
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void read() {
        TraceHelper.beginSection(">>>>GoogleStereoInfoParser-read");
        Log.d(TAG, "<read>");
        if (mStandardMetaOperator != null) {
            mStandardMetaOperator.read();
        }
        if (mExtendedMetaOperator != null) {
            mExtendedMetaOperator.read();
        }
        if (mGoogleStereoInfo == null) {
            Log.d(TAG, "<read> mGoogleStereoInfo is null!");
            TraceHelper.endSection();
            return;
        }
        readSimpleValue();
        readBufferItem();
        Log.d(TAG, "<read> " + mGoogleStereoInfo);
        dumpValuesAndBuffers("read");
        TraceHelper.endSection();
    }

    @Override
    public void write() {
        TraceHelper.beginSection(">>>>GoogleStereoInfoParser-write");
        Log.d(TAG, "<write>");
        if (mGoogleStereoInfo == null) {
            Log.d(TAG, "<write> mGoogleStereoInfo is null!");
            TraceHelper.endSection();
            return;
        }
        dumpValuesAndBuffers("write");
        writeSimpleValue();
        writeBufferItem();
        if (mStandardMetaOperator != null) {
            mStandardMetaOperator.write();
        }
        if (mExtendedMetaOperator != null) {
            mExtendedMetaOperator.write();
        }
        TraceHelper.endSection();
    }

    @Override
    public SerializedInfo serialize() {
        TraceHelper.beginSection(">>>>GoogleStereoInfoParser-serialize");
        Log.d(TAG, "<serialize>");
        SerializedInfo info = new SerializedInfo();
        if (mStandardMetaOperator != null) {
            Map<String, byte[]> standardData = mStandardMetaOperator.serialize();
            info.standardXmpBuf = standardData.get(SerializedInfo.XMP_KEY);
        }
        if (mExtendedMetaOperator != null) {
            Map<String, byte[]> extendedData = mExtendedMetaOperator.serialize();
            info.extendedXmpBuf = extendedData.get(SerializedInfo.XMP_KEY);
        }
        TraceHelper.endSection();
        return info;
    }

    private void initSimpleValue() {
        SimpleItem simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_GFOCUS_BLUR_INFINITY;
        simpleValue.nameSpaceItem.nameSpace = NS_GFOCUS;
        simpleValue.nameSpaceItem.nameSpacePrifix = PRIFIX_GFOCUS;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_GFOCUS_FOCALDISTANCE;
        simpleValue.nameSpaceItem.nameSpace = NS_GFOCUS;
        simpleValue.nameSpaceItem.nameSpacePrifix = PRIFIX_GFOCUS;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_GFOCUS_FOCALPOINTX;
        simpleValue.nameSpaceItem.nameSpace = NS_GFOCUS;
        simpleValue.nameSpaceItem.nameSpacePrifix = PRIFIX_GFOCUS;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_GFOCUS_FOCALPOINTY;
        simpleValue.nameSpaceItem.nameSpace = NS_GFOCUS;
        simpleValue.nameSpaceItem.nameSpacePrifix = PRIFIX_GFOCUS;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_GIMAGE_MIME;
        simpleValue.nameSpaceItem.nameSpace = NS_GIMAGE;
        simpleValue.nameSpaceItem.nameSpacePrifix = PRIFIX_GIMAGE;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_GDEPTH_FORMAT;
        simpleValue.nameSpaceItem.nameSpace = NS_GDEPTH;
        simpleValue.nameSpaceItem.nameSpacePrifix = PRIFIX_GDEPTH;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_GDEPTH_NEAR;
        simpleValue.nameSpaceItem.nameSpace = NS_GDEPTH;
        simpleValue.nameSpaceItem.nameSpacePrifix = PRIFIX_GDEPTH;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_GDEPTH_FAR;
        simpleValue.nameSpaceItem.nameSpace = NS_GDEPTH;
        simpleValue.nameSpaceItem.nameSpacePrifix = PRIFIX_GDEPTH;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_GDEPTH_MIME;
        simpleValue.nameSpaceItem.nameSpace = NS_GDEPTH;
        simpleValue.nameSpaceItem.nameSpacePrifix = PRIFIX_GDEPTH;
        mListOfSimpleValue.add(simpleValue);
    }

    private void initBufferItem() {
        BufferItem bufferItem = getBufferItem();
        bufferItem.nameSpaceItem.nameSpace = NS_GIMAGE;
        bufferItem.nameSpaceItem.nameSpacePrifix = PRIFIX_GIMAGE;
        bufferItem.name = ATTRIBUTE_CLEAR_IMAGE;
        mListOfBufferItem.add(bufferItem);

        bufferItem = getBufferItem();
        bufferItem.nameSpaceItem.nameSpace = NS_GDEPTH;
        bufferItem.nameSpaceItem.nameSpacePrifix = PRIFIX_GDEPTH;
        bufferItem.name = ATTRIBUTE_DEPTH_MAP;
        mListOfBufferItem.add(bufferItem);
    }

    private void readSimpleValue() {
        SimpleItem simpleValue = null;
        int simpleValueItemCount = mListOfSimpleValue.size();
        for (int i = 0; i < simpleValueItemCount; i++) {
            simpleValue = mListOfSimpleValue.get(i);
            if (simpleValue == null || simpleValue.value == null
                    || simpleValue.value.length() == 0) {
                continue;
            }
            if (ATTRIBUTE_GFOCUS_BLUR_INFINITY.equals(simpleValue.name)) {
                mGoogleStereoInfo.focusBlurAtInfinity = Double.parseDouble(simpleValue.value);
            } else if (ATTRIBUTE_GFOCUS_FOCALDISTANCE.equals(simpleValue.name)) {
                mGoogleStereoInfo.focusFocalDistance = Double.parseDouble(simpleValue.value);
            } else if (ATTRIBUTE_GFOCUS_FOCALPOINTX.equals(simpleValue.name)) {
                mGoogleStereoInfo.focusFocalPointX = Double.parseDouble(simpleValue.value);
            } else if (ATTRIBUTE_GFOCUS_FOCALPOINTY.equals(simpleValue.name)) {
                mGoogleStereoInfo.focusFocalPointY = Double.parseDouble(simpleValue.value);
            } else if (ATTRIBUTE_GIMAGE_MIME.equals(simpleValue.name)
                    && PRIFIX_GIMAGE.equals(simpleValue.nameSpaceItem.nameSpacePrifix)) {
                mGoogleStereoInfo.imageMime = simpleValue.value;
            } else if (ATTRIBUTE_GDEPTH_FORMAT.equals(simpleValue.name)) {
                mGoogleStereoInfo.depthFormat = simpleValue.value;
            } else if (ATTRIBUTE_GDEPTH_NEAR.equals(simpleValue.name)) {
                mGoogleStereoInfo.depthNear = Double.parseDouble(simpleValue.value);
            } else if (ATTRIBUTE_GDEPTH_FAR.equals(simpleValue.name)) {
                mGoogleStereoInfo.depthFar = Double.parseDouble(simpleValue.value);
            } else if (ATTRIBUTE_GDEPTH_MIME.equals(simpleValue.name)
                    && PRIFIX_GDEPTH.equals(simpleValue.nameSpaceItem.nameSpacePrifix)) {
                mGoogleStereoInfo.depthMime = simpleValue.value;
            }
        }
    }

    private void readBufferItem() {
        BufferItem bufferItem = null;
        int bufferItemCount = mListOfBufferItem.size();
        for (int i = 0; i < bufferItemCount; i++) {
            bufferItem = mListOfBufferItem.get(i);
            if (bufferItem == null || bufferItem.value == null) {
                continue;
            }
            if (ATTRIBUTE_CLEAR_IMAGE.equals(bufferItem.name)
                    && PRIFIX_GIMAGE.equals(bufferItem.nameSpaceItem.nameSpacePrifix)) {
                mGoogleStereoInfo.clearImage = bufferItem.value;
            }
            if (ATTRIBUTE_DEPTH_MAP.equals(bufferItem.name)
                    && PRIFIX_GDEPTH.equals(bufferItem.nameSpaceItem.nameSpacePrifix)) {
                mGoogleStereoInfo.depthMap = bufferItem.value;
            }
        }
    }

    private void writeSimpleValue() {
        int simpleValueItemCount = mListOfSimpleValue.size();
        for (int i = 0; i < simpleValueItemCount; i++) {
            if (mListOfSimpleValue.get(i) == null) {
                continue;
            }
            if (ATTRIBUTE_GFOCUS_BLUR_INFINITY.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mGoogleStereoInfo.focusBlurAtInfinity);
            } else if (ATTRIBUTE_GFOCUS_FOCALDISTANCE
                    .equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mGoogleStereoInfo.focusFocalDistance);
            } else if (ATTRIBUTE_GFOCUS_FOCALPOINTX
                    .equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mGoogleStereoInfo.focusFocalPointX);
            } else if (ATTRIBUTE_GFOCUS_FOCALPOINTY
                    .equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mGoogleStereoInfo.focusFocalPointY);
            } else if (ATTRIBUTE_GIMAGE_MIME.equals(mListOfSimpleValue.get(i).name)
                    && PRIFIX_GIMAGE
                            .equals(mListOfSimpleValue.get(i).nameSpaceItem.nameSpacePrifix)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mGoogleStereoInfo.imageMime);
            } else if (ATTRIBUTE_GDEPTH_FORMAT.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mGoogleStereoInfo.depthFormat);
            } else if (ATTRIBUTE_GDEPTH_NEAR.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mGoogleStereoInfo.depthNear);
            } else if (ATTRIBUTE_GDEPTH_FAR.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mGoogleStereoInfo.depthFar);
            } else if (ATTRIBUTE_GDEPTH_MIME.equals(mListOfSimpleValue.get(i).name)
                    && PRIFIX_GDEPTH
                            .equals(mListOfSimpleValue.get(i).nameSpaceItem.nameSpacePrifix)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mGoogleStereoInfo.depthMime);
            }
        }
        mStandardDataCollections.listOfSimpleValue = mListOfSimpleValue;
    }

    private void writeBufferItem() {
        int bufferItemCount = mListOfBufferItem.size();
        for (int i = 0; i < bufferItemCount; i++) {
            if (mListOfBufferItem.get(i) == null) {
                continue;
            }
            if (ATTRIBUTE_CLEAR_IMAGE.equals(mListOfBufferItem.get(i).name)
                    && null != mGoogleStereoInfo.clearImage
                    && PRIFIX_GIMAGE
                            .equals(mListOfBufferItem.get(i).nameSpaceItem.nameSpacePrifix)) {
                mListOfBufferItem.get(i).value = mGoogleStereoInfo.clearImage;
            }
            if (ATTRIBUTE_DEPTH_MAP.equals(mListOfBufferItem.get(i).name)
                    && null != mGoogleStereoInfo.depthMap
                    && PRIFIX_GDEPTH
                            .equals(mListOfBufferItem.get(i).nameSpaceItem.nameSpacePrifix)) {
                mListOfBufferItem.get(i).value = mGoogleStereoInfo.depthMap;
            }
        }
        mExtendardDataCollections.listOfBufferItem = mListOfBufferItem;
    }

    private SimpleItem getSimpleValueInstance() {
        SimpleItem simpleValue = new SimpleItem();
        simpleValue.dest = DataItem.DEST_TYPE_STANDARD_XMP;
        simpleValue.nameSpaceItem = new NameSpaceItem();
        simpleValue.nameSpaceItem.dest = DataItem.DEST_TYPE_STANDARD_XMP;
        return simpleValue;
    }

    private BufferItem getBufferItem() {
        BufferItem bufferItem = new BufferItem();
        bufferItem.dest = DataItem.DEST_TYPE_EXTENDED_XMP;
        bufferItem.nameSpaceItem = new NameSpaceItem();
        bufferItem.nameSpaceItem.dest = DataItem.DEST_TYPE_EXTENDED_XMP;
        return bufferItem;
    }

    private void dumpValuesAndBuffers(String suffix) {
        if (!Utils.ENABLE_BUFFER_DUMP) {
            return;
        }
        String dumpPath = Utils.DUMP_FILE_FOLDER + "/" + mGoogleStereoInfo.debugDir + "/";
        if (mGoogleStereoInfo.clearImage != null) {
            Utils.writeBufferToFile(dumpPath + "GoogleStereoInfo_clearImage_" + suffix + ".raw",
                    mGoogleStereoInfo.clearImage);
        } else {
            Log.d(TAG, "<dumpValuesAndBuffers> clearImage is null!");
        }
        if (mGoogleStereoInfo.depthMap != null) {
            Utils.writeBufferToFile(dumpPath + "GoogleStereoInfo_depthMap_" + suffix + ".raw",
                    mGoogleStereoInfo.depthMap);
        } else {
            Log.d(TAG, "<dumpValuesAndBuffers> depthMap is null!");
        }
        Utils.writeStringToFile(dumpPath + "GoogleStereoInfo_" + suffix + ".txt",
                mGoogleStereoInfo.toString());
    }
}
