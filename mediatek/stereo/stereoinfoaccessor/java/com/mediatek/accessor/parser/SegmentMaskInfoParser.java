package com.mediatek.accessor.parser;

import com.mediatek.accessor.data.SegmentMaskInfo;
import com.mediatek.accessor.meta.data.DataItem;
import com.mediatek.accessor.meta.data.DataItem.BufferItem;
import com.mediatek.accessor.meta.data.DataItem.DataCollections;
import com.mediatek.accessor.meta.data.DataItem.NameSpaceItem;
import com.mediatek.accessor.meta.data.DataItem.SimpleItem;
import com.mediatek.accessor.operator.IMetaOperator;
import com.mediatek.accessor.operator.MetaOperatorFactory;
import com.mediatek.accessor.packer.PackUtils;
import com.mediatek.accessor.util.Log;
import com.mediatek.accessor.util.TraceHelper;
import com.mediatek.accessor.util.Utils;

import java.util.ArrayList;
import java.util.Map;

/**
 * Segment mask info parser.
 */
public class SegmentMaskInfoParser implements IParser {
    private final static String TAG = Log.Tag(SegmentMaskInfoParser.class.getSimpleName());

    private static final String NS_STEREO = "http://ns.mediatek.com/segment/";
    private static final String PRIFIX_STEREO = "MSegment";
    private static final String ATTRIBUTE_SEGMENT_X = "SegmentX";
    private static final String ATTRIBUTE_SEGMENT_Y = "SegmentY";
    private static final String ATTRIBUTE_SEGMENT_LEFT = "SegmentLeft";
    private static final String ATTRIBUTE_SEGMENT_TOP = "SegmentTop";
    private static final String ATTRIBUTE_SEGMENT_RIGHT = "SegmentRight";
    private static final String ATTRIBUTE_SEGMENT_BOTTOM = "SegmentBottom";
    private static final String ATTRIBUTE_SEGMENT_MASK_BUFFER = PackUtils.TYPE_JPS_MASK;
    private static final String ATTRIBUTE_MASK_WIDTH = "SegmentMaskWidth";
    private static final String ATTRIBUTE_MASK_HEIGHT = "SegmentMaskHeight";

    private IMetaOperator mStandardMetaOperator;
    private IMetaOperator mCustomizedMetaOperator;
    private DataCollections mStandardDataCollections = new DataCollections();
    private DataCollections mCustomizedDataCollections = new DataCollections();
    private ArrayList<SimpleItem> mListOfSimpleValue = new ArrayList<SimpleItem>();
    private ArrayList<BufferItem> mListOfCustDataItem = new ArrayList<BufferItem>();
    private SegmentMaskInfo mSegmentMaskInfo;

    /**
     * SegmentMaskInfoParser Constructor.
     * @param standardBuffer
     *            use standardMeta to get or set standard XMP info value
     * @param customBuffer
     *            use custMeta to get or set customer XMP info value
     * @param info
     *            SegmentMaskInfo struct for set or get segment and mask info
     */
    public SegmentMaskInfoParser(byte[] standardBuffer, Map<String, byte[]> customBuffer,
            SegmentMaskInfo info) {
        mStandardDataCollections.dest = DataItem.DEST_TYPE_STANDARD_XMP;
        mSegmentMaskInfo = info;
        initSimpleValue();
        initCustDataItem();

        mStandardDataCollections.listOfSimpleValue = mListOfSimpleValue;
        mStandardMetaOperator =
                MetaOperatorFactory.getOperatorInstance(
                        MetaOperatorFactory.XMP_META_OPERATOR, standardBuffer, null);
        try {
            mStandardMetaOperator.setData(mStandardDataCollections);
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }

        mCustomizedDataCollections.listOfCustomDataItem = mListOfCustDataItem;
        mCustomizedMetaOperator =
                MetaOperatorFactory.getOperatorInstance(
                        MetaOperatorFactory.CUSTOMIZED_META_OPERATOR, null, customBuffer);
        try {
            mCustomizedMetaOperator.setData(mCustomizedDataCollections);
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void read() {
        TraceHelper.beginSection(">>>>SegmentMaskInfoParser-read");
        Log.d(TAG, "<read>");
        if (mStandardMetaOperator != null) {
            mStandardMetaOperator.read();
        }
        if (mCustomizedMetaOperator != null) {
            mCustomizedMetaOperator.read();
        }
        if (mSegmentMaskInfo == null) {
            Log.d(TAG, "<read> mSegmentMaskInfo is null!");
            TraceHelper.endSection();
            return;
        }
        readSimpleValue();
        readCustDataItem();
        Log.d(TAG, "<read> " + mSegmentMaskInfo);
        dumpValuesAndBuffers("read");
        TraceHelper.endSection();
    }

    @Override
    public void write() {
        TraceHelper.beginSection(">>>>SegmentMaskInfoParser-write");
        Log.d(TAG, "<write>");
        if (mSegmentMaskInfo == null) {
            Log.d(TAG, "<write> mSegmentMaskInfo is null!");
            TraceHelper.endSection();
            return;
        }
        dumpValuesAndBuffers("write");
        writeSimpleValue();
        writeCustDataItem();
        if (mStandardMetaOperator != null) {
            mStandardMetaOperator.write();
        }
        if (mCustomizedMetaOperator != null) {
            mCustomizedMetaOperator.write();
        }
        TraceHelper.endSection();
    }

    @Override
    public SerializedInfo serialize() {
        TraceHelper.beginSection(">>>>SegmentMaskInfoParser-serialize");
        Log.d(TAG, "<serialize>");
        SerializedInfo info = new SerializedInfo();
        if (mStandardMetaOperator != null) {
            Map<String, byte[]> standardData = mStandardMetaOperator.serialize();
            info.standardXmpBuf = standardData.get(SerializedInfo.XMP_KEY);
        }
        if (mCustomizedMetaOperator != null) {
            Map<String, byte[]> customizedData = mCustomizedMetaOperator.serialize();
            info.customizedBufMap = customizedData;
        }
        TraceHelper.endSection();
        return info;
    }

    private void initSimpleValue() {
        SimpleItem simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_MASK_WIDTH;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_MASK_HEIGHT;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_SEGMENT_X;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_SEGMENT_Y;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_SEGMENT_LEFT;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_SEGMENT_TOP;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_SEGMENT_RIGHT;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_SEGMENT_BOTTOM;
        mListOfSimpleValue.add(simpleValue);
    }

    private void initCustDataItem() {
        BufferItem custDataItem = new BufferItem();
        custDataItem.name = ATTRIBUTE_SEGMENT_MASK_BUFFER;
        mListOfCustDataItem.add(custDataItem);
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
            if (ATTRIBUTE_MASK_WIDTH.equals(simpleValue.name)) {
                mSegmentMaskInfo.maskWidth = Integer.parseInt(simpleValue.value);
            } else if (ATTRIBUTE_MASK_HEIGHT.equals(simpleValue.name)) {
                mSegmentMaskInfo.maskHeight = Integer.parseInt(simpleValue.value);
            } else if (ATTRIBUTE_SEGMENT_X.equals(simpleValue.name)) {
                mSegmentMaskInfo.segmentX = Integer.parseInt(simpleValue.value);
            } else if (ATTRIBUTE_SEGMENT_Y.equals(simpleValue.name)) {
                mSegmentMaskInfo.segmentY = Integer.parseInt(simpleValue.value);
            } else if (ATTRIBUTE_SEGMENT_LEFT.equals(simpleValue.name)) {
                mSegmentMaskInfo.segmentLeft = Integer.parseInt(simpleValue.value);
            } else if (ATTRIBUTE_SEGMENT_TOP.equals(simpleValue.name)) {
                mSegmentMaskInfo.segmentTop = Integer.parseInt(simpleValue.value);
            } else if (ATTRIBUTE_SEGMENT_RIGHT.equals(simpleValue.name)) {
                mSegmentMaskInfo.segmentRight = Integer.parseInt(simpleValue.value);
            } else if (ATTRIBUTE_SEGMENT_BOTTOM.equals(simpleValue.name)) {
                mSegmentMaskInfo.segmentBottom = Integer.parseInt(simpleValue.value);
            }
        }
    }

    private void readCustDataItem() {
        BufferItem custDataItem = null;
        int custDataItemCount = mListOfCustDataItem.size();
        for (int i = 0; i < custDataItemCount; i++) {
            custDataItem = mListOfCustDataItem.get(i);
            if (custDataItem == null || custDataItem.value == null) {
                continue;
            }
            if (ATTRIBUTE_SEGMENT_MASK_BUFFER.equals(custDataItem.name)) {
                mSegmentMaskInfo.maskBuffer = custDataItem.value;
            }
        }
    }

    private void writeSimpleValue() {
        int simpleValueItemCount = mListOfSimpleValue.size();
        for (int i = 0; i < simpleValueItemCount; i++) {
            if (mListOfSimpleValue.get(i) == null) {
                continue;
            }
            if (ATTRIBUTE_MASK_WIDTH.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mSegmentMaskInfo.maskWidth);
            } else if (ATTRIBUTE_MASK_HEIGHT.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mSegmentMaskInfo.maskHeight);
            } else if (ATTRIBUTE_SEGMENT_X.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mSegmentMaskInfo.segmentX);
            } else if (ATTRIBUTE_SEGMENT_Y.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mSegmentMaskInfo.segmentY);
            } else if (ATTRIBUTE_SEGMENT_LEFT.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mSegmentMaskInfo.segmentLeft);
            } else if (ATTRIBUTE_SEGMENT_TOP.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mSegmentMaskInfo.segmentTop);
            } else if (ATTRIBUTE_SEGMENT_RIGHT.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mSegmentMaskInfo.segmentRight);
            } else if (ATTRIBUTE_SEGMENT_BOTTOM.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mSegmentMaskInfo.segmentBottom);
            }
        }
        mStandardDataCollections.listOfSimpleValue = mListOfSimpleValue;
    }

    private void writeCustDataItem() {
        int custDataItemCount = mListOfCustDataItem.size();
        for (int i = 0; i < custDataItemCount; i++) {
            if (mListOfCustDataItem.get(i) == null) {
                continue;
            }
            if (ATTRIBUTE_SEGMENT_MASK_BUFFER.equals(mListOfCustDataItem.get(i).name)
                    && null != mSegmentMaskInfo.maskBuffer) {
                mListOfCustDataItem.get(i).value = mSegmentMaskInfo.maskBuffer;
            }
        }
        mCustomizedDataCollections.listOfCustomDataItem = mListOfCustDataItem;
    }

    private SimpleItem getSimpleValueInstance() {
        SimpleItem simpleValue = new SimpleItem();
        simpleValue.dest = DataItem.DEST_TYPE_STANDARD_XMP;
        simpleValue.nameSpaceItem = new NameSpaceItem();
        simpleValue.nameSpaceItem.dest = DataItem.DEST_TYPE_STANDARD_XMP;
        simpleValue.nameSpaceItem.nameSpace = NS_STEREO;
        simpleValue.nameSpaceItem.nameSpacePrifix = PRIFIX_STEREO;
        return simpleValue;
    }

    private void dumpValuesAndBuffers(String suffix) {
        if (!Utils.ENABLE_BUFFER_DUMP) {
            return;
        }
        String dumpPath = Utils.DUMP_FILE_FOLDER + "/" + mSegmentMaskInfo.debugDir + "/";
        if (mSegmentMaskInfo.maskBuffer != null) {
            Utils.writeBufferToFile(dumpPath + "SegmentMaskInfo_maskBuffer_" + suffix + ".raw",
                    mSegmentMaskInfo.maskBuffer);
        } else {
            Log.d(TAG, "<dumpValuesAndBuffers> maskBuffer is null!");
        }
        Utils.writeStringToFile(dumpPath + "SegmentMaskInfo_" + suffix + ".txt",
                mSegmentMaskInfo.toString());
    }
}
