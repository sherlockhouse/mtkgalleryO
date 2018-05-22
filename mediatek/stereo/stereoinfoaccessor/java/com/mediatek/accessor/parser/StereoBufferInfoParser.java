package com.mediatek.accessor.parser;

import com.mediatek.accessor.data.StereoBufferInfo;
import com.mediatek.accessor.meta.data.DataItem.BufferItem;
import com.mediatek.accessor.meta.data.DataItem.DataCollections;
import com.mediatek.accessor.operator.IMetaOperator;
import com.mediatek.accessor.operator.MetaOperatorFactory;
import com.mediatek.accessor.packer.PackUtils;
import com.mediatek.accessor.util.Log;
import com.mediatek.accessor.util.TraceHelper;
import com.mediatek.accessor.util.Utils;

import java.util.ArrayList;
import java.util.Map;

/**
 * Stereo buffer info parser.
 */
public class StereoBufferInfoParser implements IParser {
    private final static String TAG = Log.Tag(StereoBufferInfoParser.class.getSimpleName());

    private static final String ATTRIBUTE_SEGMENT_MASK_BUFFER = PackUtils.TYPE_JPS_MASK;
    private static final String ATTRIBUTE_JPS_BUFFER = PackUtils.TYPE_JPS_DATA;

    private IMetaOperator mCustomizedMetaOperator;
    private DataCollections mCustomizedDataCollections = new DataCollections();
    private ArrayList<BufferItem> mListOfCustDataItem = new ArrayList<BufferItem>();
    private StereoBufferInfo mStereoBufferInfo;

    /**
     * StereoBufferInfoParser Constructor.
     * @param customizedBuffer
     *            use custMeta to get or set customer XMP info value
     * @param info
     *            StereoBufferInfo struct for set or get stereo buffer info
     */
    public StereoBufferInfoParser(Map<String, byte[]> customizedBuffer,
            StereoBufferInfo info) {
        mStereoBufferInfo = info;
        initCustDataItem();
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
     * StereoBufferInfoParser Constructor.
     * @param customizedMetaOperator
     *            use custMeta to get or set customer XMP info value
     * @param info
     *            StereoBufferInfo struct for set or get stereo buffer info
     */
    public StereoBufferInfoParser(IMetaOperator customizedMetaOperator,
            StereoBufferInfo info) {
        mStereoBufferInfo = info;
        initCustDataItem();
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
        TraceHelper.beginSection(">>>>StereoBufferInfoParser-read");
        Log.d(TAG, "<read>");
        if (mCustomizedMetaOperator != null) {
            mCustomizedMetaOperator.read();
        }
        if (mStereoBufferInfo == null) {
            Log.d(TAG, "<read> mStereoBufferInfo is null!");
            TraceHelper.endSection();
            return;
        }
        readBufferItem();
        Log.d(TAG, "<read> " + mStereoBufferInfo);
        dumpValuesAndBuffers("read");
        TraceHelper.endSection();
    }

    @Override
    public void write() {
        TraceHelper.beginSection(">>>>StereoBufferInfoParser-write");
        Log.d(TAG, "<write>");
        if (mStereoBufferInfo == null) {
            Log.d(TAG, "<write> mStereoBufferInfo is null!");
            TraceHelper.endSection();
            return;
        }
        dumpValuesAndBuffers("write");
        writeCustDataItem();
        if (mCustomizedMetaOperator != null) {
            mCustomizedMetaOperator.write();
        }
        TraceHelper.endSection();
    }

    @Override
    public SerializedInfo serialize() {
        TraceHelper.beginSection(">>>>StereoBufferInfoParser-serialize");
        Log.d(TAG, "<serialize>");
        SerializedInfo info = new SerializedInfo();
        if (mCustomizedMetaOperator != null) {
            info.customizedBufMap = mCustomizedMetaOperator.serialize();
        }
        TraceHelper.endSection();
        return info;
    }

    private void initCustDataItem() {
        BufferItem custDataItem = new BufferItem();
        custDataItem.name = ATTRIBUTE_JPS_BUFFER;
        mListOfCustDataItem.add(custDataItem);
        custDataItem = new BufferItem();
        custDataItem.name = ATTRIBUTE_SEGMENT_MASK_BUFFER;
        mListOfCustDataItem.add(custDataItem);
    }

    private void readBufferItem() {
        BufferItem custDataItem = null;
        int custDataItemCount = mListOfCustDataItem.size();
        for (int i = 0; i < custDataItemCount; i++) {
            custDataItem = mListOfCustDataItem.get(i);
            if (custDataItem == null || custDataItem.value == null) {
                continue;
            }
            if (ATTRIBUTE_JPS_BUFFER.equals(custDataItem.name)) {
                mStereoBufferInfo.jpsBuffer = custDataItem.value;
            } else if (ATTRIBUTE_SEGMENT_MASK_BUFFER.equals(custDataItem.name)) {
                mStereoBufferInfo.maskBuffer = custDataItem.value;
            }
        }
    }

    private void writeCustDataItem() {
        int custDataItemCount = mListOfCustDataItem.size();
        for (int i = 0; i < custDataItemCount; i++) {
            if (mListOfCustDataItem.get(i) == null) {
                continue;
            }
            if (ATTRIBUTE_JPS_BUFFER.equals(mListOfCustDataItem.get(i).name)
                    && null != mStereoBufferInfo.jpsBuffer) {
                mListOfCustDataItem.get(i).value = mStereoBufferInfo.jpsBuffer;
            } else if (ATTRIBUTE_SEGMENT_MASK_BUFFER
                    .equals(mListOfCustDataItem.get(i).name)
                    && null != mStereoBufferInfo.maskBuffer) {
                mListOfCustDataItem.get(i).value = mStereoBufferInfo.maskBuffer;
            }
        }
        mCustomizedDataCollections.listOfCustomDataItem = mListOfCustDataItem;
    }

    private void dumpValuesAndBuffers(String suffix) {
        if (!Utils.ENABLE_BUFFER_DUMP) {
            return;
        }
        String dumpPath = Utils.DUMP_FILE_FOLDER + "/" + mStereoBufferInfo.debugDir + "/";
        if (mStereoBufferInfo.maskBuffer != null) {
            Utils.writeBufferToFile(dumpPath + "StereoBufferInfo_maskBuffer_" + suffix + ".raw",
                    mStereoBufferInfo.maskBuffer);
        } else {
            Log.d(TAG, "<dumpValuesAndBuffers> maskBuffer is null!");
        }
        if (mStereoBufferInfo.jpsBuffer != null) {
            Utils.writeBufferToFile(dumpPath + "StereoBufferInfo_jpsBuffer_" + suffix + ".raw",
                    mStereoBufferInfo.jpsBuffer);
        } else {
            Log.d(TAG, "<dumpValuesAndBuffers> jpsBuffer is null!");
        }
        Utils.writeStringToFile(dumpPath + "StereoBufferInfo_" + suffix + ".txt",
                mStereoBufferInfo.toString());
    }
}
