package com.mediatek.accessor.parser;

import com.mediatek.accessor.data.StereoDepthInfo;
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
 * Stereo depth info parser.
 */
public class StereoDepthInfoParser implements IParser {
    private final static String TAG = Log.Tag(StereoDepthInfoParser.class.getSimpleName());

    private static final String NS_GDEPTH = "http://ns.google.com/photos/1.0/depthmap/";
    private static final String NS_STEREO = "http://ns.mediatek.com/refocus/jpsconfig/";
    private static final String PRIFIX_GDEPTH = "GDepth";
    private static final String PRIFIX_STEREO = "MRefocus";
    private static final String ATTRIBUTE_META_BUFFER_WIDTH = "MetaBufferWidth";
    private static final String ATTRIBUTE_META_BUFFER_HEIGHT = "MetaBufferHeight";
    private static final String ATTRIBUTE_TOUCH_COORDX_LAST = "TouchCoordXLast";
    private static final String ATTRIBUTE_TOUCH_COORDY_LAST = "TouchCoordYLast";
    private static final String ATTRIBUTE_DEPTH_OF_FIELD_LAST = "DepthOfFieldLast";
    private static final String ATTRIBUTE_DEPTH_BUFFER_WIDTH = "DepthBufferWidth";
    private static final String ATTRIBUTE_DEPTH_BUFFER_HEIGHT = "DepthBufferHeight";
    private static final String ATTRIBUTE_DEPTH_MAP_WIDTH = "XmpDepthWidth";
    private static final String ATTRIBUTE_DEPTH_MAP_HEIGHT = "XmpDepthHeight";
    private static final String ATTRIBUTE_DEPTH_BUFFER = PackUtils.TYPE_DEPTH_DATA;
    private static final String ATTRIBUTE_DEBUG_BUFFER = PackUtils.TYPE_DEBUG_BUFFER;
    private static final String ATTRIBUTE_DEPTH_MAP = "Data";

    private IMetaOperator mStandardMetaOperator;
    private IMetaOperator mExtendedMetaOperator;
    private IMetaOperator mCustomizedMetaOperator;
    private DataCollections mStandardDataCollections = new DataCollections();
    private DataCollections mExtendardDataCollections = new DataCollections();
    private DataCollections mCustomizedDataCollections = new DataCollections();
    private ArrayList<SimpleItem> mListOfSimpleValue = new ArrayList<SimpleItem>();
    private ArrayList<BufferItem> mListOfBufferItem = new ArrayList<BufferItem>();
    private ArrayList<BufferItem> mListOfCustDataItem = new ArrayList<BufferItem>();
    private StereoDepthInfo mStereoDepthInfo;

    /**
     * StereoDepthInfoParser Constructor.
     * @param standardBuffer
     *            use standardMeta to get or set standard XMP info value
     * @param extendedBuffer
     *            use extendedMeta to get or set extended XMP info value
     * @param customizedBuffer
     *            use custMeta to get or set customer XMP info value
     * @param info
     *            StereoDepthInfo struct for set or get stereo depth info
     */
    public StereoDepthInfoParser(byte[] standardBuffer, byte[] extendedBuffer,
            Map<String, byte[]> customizedBuffer, StereoDepthInfo info) {
        mStandardDataCollections.dest = DataItem.DEST_TYPE_STANDARD_XMP;
        mExtendardDataCollections.dest = DataItem.DEST_TYPE_EXTENDED_XMP;
        mStereoDepthInfo = info;
        initSimpleValue();
        initBufferItem();
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

        mExtendardDataCollections.listOfBufferItem = mListOfBufferItem;
        mExtendedMetaOperator =
                MetaOperatorFactory.getOperatorInstance(
                        MetaOperatorFactory.XMP_META_OPERATOR, extendedBuffer, null);
        try {
            mExtendedMetaOperator.setData(mExtendardDataCollections);
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }

        mCustomizedDataCollections.listOfCustomDataItem = mListOfCustDataItem;
        mCustomizedMetaOperator =
                MetaOperatorFactory.getOperatorInstance(
                        MetaOperatorFactory.CUSTOMIZED_META_OPERATOR, null,
                        customizedBuffer);
        try {
            mCustomizedMetaOperator.setData(mCustomizedDataCollections);
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * StereoDepthInfoParser Constructor.
     * @param standardMetaOperator standard meta operator
     * @param extendedMetaOperator extended meta operator
     * @param customizedMetaOperator cust meta operator
     * @param info StereoDepthInfo
     */
    public StereoDepthInfoParser(IMetaOperator standardMetaOperator,
                                 IMetaOperator extendedMetaOperator,
                                 IMetaOperator customizedMetaOperator, StereoDepthInfo info) {
        mStandardDataCollections.dest = DataItem.DEST_TYPE_STANDARD_XMP;
        mExtendardDataCollections.dest = DataItem.DEST_TYPE_EXTENDED_XMP;
        mStereoDepthInfo = info;
        initSimpleValue();
        initBufferItem();
        initCustDataItem();

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

        mCustomizedDataCollections.listOfCustomDataItem = mListOfCustDataItem;
        mCustomizedMetaOperator = customizedMetaOperator;
        try {
            mCustomizedMetaOperator.setData(mCustomizedDataCollections);
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void read() {
        TraceHelper.beginSection(">>>>StereoDepthInfoParser-read");
        Log.d(TAG, "<read>");
        if (mStandardMetaOperator != null) {
            mStandardMetaOperator.read();
        }
        if (mExtendedMetaOperator != null) {
            mExtendedMetaOperator.read();
        }
        if (mCustomizedMetaOperator != null) {
            mCustomizedMetaOperator.read();
        }
        if (mStereoDepthInfo == null) {
            Log.d(TAG, "<read> mStereoDepthInfo is null!");
            TraceHelper.endSection();
            return;
        }
        readSimpleValue();
        readBufferItem();
        readCustDataItem();
        Log.d(TAG, "<read> " + mStereoDepthInfo);
        dumpValuesAndBuffers("read");
        TraceHelper.endSection();
    }

    @Override
    public void write() {
        TraceHelper.beginSection(">>>>StereoDepthInfoParser-write");
        Log.d(TAG, "<write>");
        if (mStereoDepthInfo == null) {
            Log.d(TAG, "<write> mStereoDepthInfo is null!");
            TraceHelper.endSection();
            return;
        }
        dumpValuesAndBuffers("write");
        writeSimpleValue();
        writeBufferItem();
        writeCustDataItem();
        if (mStandardMetaOperator != null) {
            mStandardMetaOperator.write();
        }
        if (mExtendedMetaOperator != null) {
            mExtendedMetaOperator.write();
        }
        if (mCustomizedMetaOperator != null) {
            mCustomizedMetaOperator.write();
        }
        TraceHelper.endSection();
    }

    @Override
    public SerializedInfo serialize() {
        TraceHelper.beginSection(">>>>StereoDepthInfoParser-serialize");
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
        if (mCustomizedMetaOperator != null) {
            Map<String, byte[]> customizedData = mCustomizedMetaOperator.serialize();
            info.customizedBufMap = customizedData;
        }
        TraceHelper.endSection();
        return info;
    }

    private void initSimpleValue() {
        SimpleItem simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_META_BUFFER_WIDTH;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_META_BUFFER_HEIGHT;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_TOUCH_COORDX_LAST;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_TOUCH_COORDY_LAST;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_DEPTH_OF_FIELD_LAST;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_DEPTH_BUFFER_WIDTH;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_DEPTH_BUFFER_HEIGHT;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_DEPTH_MAP_WIDTH;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_DEPTH_MAP_HEIGHT;
        mListOfSimpleValue.add(simpleValue);
    }

    private void initBufferItem() {
        BufferItem depthMapItem = new BufferItem();
        depthMapItem.dest = DataItem.DEST_TYPE_EXTENDED_XMP;
        depthMapItem.nameSpaceItem = new NameSpaceItem();
        depthMapItem.nameSpaceItem.dest = DataItem.DEST_TYPE_EXTENDED_XMP;
        depthMapItem.nameSpaceItem.nameSpace = NS_GDEPTH;
        depthMapItem.nameSpaceItem.nameSpacePrifix = PRIFIX_GDEPTH;
        depthMapItem.name = ATTRIBUTE_DEPTH_MAP;
        mListOfBufferItem.add(depthMapItem);
    }

    private void initCustDataItem() {
        BufferItem depthBufferItem = new BufferItem();
        depthBufferItem.name = ATTRIBUTE_DEPTH_BUFFER;
        mListOfCustDataItem.add(depthBufferItem);

        BufferItem debugBufferItem = new BufferItem();
        debugBufferItem.name = ATTRIBUTE_DEBUG_BUFFER;
        mListOfCustDataItem.add(debugBufferItem);
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
            if (ATTRIBUTE_META_BUFFER_WIDTH.equals(simpleValue.name)) {
                mStereoDepthInfo.metaBufferWidth = Integer.parseInt(simpleValue.value);
            } else if (ATTRIBUTE_META_BUFFER_HEIGHT.equals(simpleValue.name)) {
                mStereoDepthInfo.metaBufferHeight = Integer.parseInt(simpleValue.value);
            } else if (ATTRIBUTE_TOUCH_COORDX_LAST.equals(simpleValue.name)) {
                mStereoDepthInfo.touchCoordXLast = Integer.parseInt(simpleValue.value);
            } else if (ATTRIBUTE_TOUCH_COORDY_LAST.equals(simpleValue.name)) {
                mStereoDepthInfo.touchCoordYLast = Integer.parseInt(simpleValue.value);
            } else if (ATTRIBUTE_DEPTH_OF_FIELD_LAST.equals(simpleValue.name)) {
                mStereoDepthInfo.depthOfFieldLast = Integer.parseInt(simpleValue.value);
            } else if (ATTRIBUTE_DEPTH_BUFFER_WIDTH.equals(simpleValue.name)) {
                mStereoDepthInfo.depthBufferWidth = Integer.parseInt(simpleValue.value);
            } else if (ATTRIBUTE_DEPTH_BUFFER_HEIGHT.equals(simpleValue.name)) {
                mStereoDepthInfo.depthBufferHeight = Integer.parseInt(simpleValue.value);
            } else if (ATTRIBUTE_DEPTH_MAP_WIDTH.equals(simpleValue.name)) {
                mStereoDepthInfo.depthMapWidth = Integer.parseInt(simpleValue.value);
            } else if (ATTRIBUTE_DEPTH_MAP_HEIGHT.equals(simpleValue.name)) {
                mStereoDepthInfo.depthMapHeight = Integer.parseInt(simpleValue.value);
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
            if (ATTRIBUTE_DEPTH_MAP.equals(bufferItem.name)) {
                mStereoDepthInfo.depthMap = bufferItem.value;
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
            if (ATTRIBUTE_DEPTH_BUFFER.equals(custDataItem.name)) {
                mStereoDepthInfo.depthBuffer = custDataItem.value;
            } else if (ATTRIBUTE_DEBUG_BUFFER.equals(custDataItem.name)
                    && Utils.ENABLE_BUFFER_DUMP) {
                mStereoDepthInfo.debugBuffer = custDataItem.value;
            }
        }
    }

    private void writeSimpleValue() {
        int simpleValueItemCount = mListOfSimpleValue.size();
        for (int i = 0; i < simpleValueItemCount; i++) {
            if (mListOfSimpleValue.get(i) == null) {
                continue;
            }
            if (ATTRIBUTE_META_BUFFER_WIDTH.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mStereoDepthInfo.metaBufferWidth);
            } else if (ATTRIBUTE_META_BUFFER_HEIGHT
                    .equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mStereoDepthInfo.metaBufferHeight);
            } else if (ATTRIBUTE_TOUCH_COORDX_LAST.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mStereoDepthInfo.touchCoordXLast);
            } else if (ATTRIBUTE_TOUCH_COORDY_LAST.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mStereoDepthInfo.touchCoordYLast);
            } else if (ATTRIBUTE_DEPTH_OF_FIELD_LAST
                    .equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mStereoDepthInfo.depthOfFieldLast);
            } else if (ATTRIBUTE_DEPTH_BUFFER_WIDTH
                    .equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mStereoDepthInfo.depthBufferWidth);
            } else if (ATTRIBUTE_DEPTH_BUFFER_HEIGHT
                    .equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mStereoDepthInfo.depthBufferHeight);
            } else if (ATTRIBUTE_DEPTH_MAP_WIDTH.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mStereoDepthInfo.depthMapWidth);
            } else if (ATTRIBUTE_DEPTH_MAP_HEIGHT.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mStereoDepthInfo.depthMapHeight);
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
            if (ATTRIBUTE_DEPTH_MAP.equals(mListOfBufferItem.get(i).name)
                    && null != mStereoDepthInfo.depthMap) {
                mListOfBufferItem.get(i).value = mStereoDepthInfo.depthMap;
            }
        }
        mExtendardDataCollections.listOfBufferItem = mListOfBufferItem;
    }

    private void writeCustDataItem() {
        int custDataItemCount = mListOfCustDataItem.size();
        for (int i = 0; i < custDataItemCount; i++) {
            if (mListOfCustDataItem.get(i) == null) {
                continue;
            }
            if (ATTRIBUTE_DEPTH_BUFFER.equals(mListOfCustDataItem.get(i).name)
                    && null != mStereoDepthInfo.depthBuffer) {
                mListOfCustDataItem.get(i).value = mStereoDepthInfo.depthBuffer;
            } else if (ATTRIBUTE_DEBUG_BUFFER.equals(mListOfCustDataItem.get(i).name)
                    && null != mStereoDepthInfo.debugBuffer) {
                mListOfCustDataItem.get(i).value = mStereoDepthInfo.debugBuffer;
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
        String dumpPath = Utils.DUMP_FILE_FOLDER + "/" + mStereoDepthInfo.debugDir + "/";
        if (mStereoDepthInfo.depthBuffer != null) {
            Utils.writeBufferToFile(dumpPath + "StereoDepthInfo_depthBuffer_" + suffix + ".raw",
                    mStereoDepthInfo.depthBuffer);
        } else {
            Log.d(TAG, "<dumpValuesAndBuffers> depthBuffer is null!");
        }
        if (mStereoDepthInfo.depthMap != null) {
            Utils.writeBufferToFile(dumpPath + "StereoDepthInfo_depthMap_" + suffix + ".raw",
                    mStereoDepthInfo.depthMap);
        } else {
            Log.d(TAG, "<dumpValuesAndBuffers> depthMap is null!");
        }
        if (mStereoDepthInfo.debugBuffer != null) {
            Utils.writeBufferToFile(dumpPath + "StereoDepthInfo_debugBuffer_" + suffix + ".raw",
                    mStereoDepthInfo.debugBuffer);
        } else {
            Log.d(TAG, "<dumpValuesAndBuffers> debugBuffer is null!");
        }
        Utils.writeStringToFile(dumpPath + "StereoDepthInfo_" + suffix + ".txt",
                mStereoDepthInfo.toString());
    }
}
