package com.mediatek.accessor.operator;

import com.mediatek.accessor.meta.CustomizedMeta;
import com.mediatek.accessor.meta.data.DataItem.BufferItem;
import com.mediatek.accessor.meta.data.DataItem.DataCollections;
import com.mediatek.accessor.util.Log;

import java.util.ArrayList;
import java.util.Map;

/**
 * Meta operator for customized data.
 */
public class CustomizedMetaOperator implements IMetaOperator {
    private final static String TAG = Log.Tag(CustomizedMetaOperator.class.getSimpleName());

    private CustomizedMeta mCustMeta;
    private ArrayList<BufferItem> mListOfCustDataItem = new ArrayList<BufferItem>();

    /**
     * CustomizedMetaOperator constructor.
     * @param customizedDataBuffer
     *            customized meta buffer
     */
    public CustomizedMetaOperator(Map<String, byte[]> customizedDataBuffer) {
        mCustMeta = new CustomizedMeta(customizedDataBuffer);
    }

    @Override
    public void encrypt() {
    }

    @Override
    public void decrypt() {
    }

    @Override
    public void read() {
        Log.d(TAG, "<read>");
        int itemCount = mListOfCustDataItem.size();
        for (int i = 0; i < itemCount; i++) {
            if (mListOfCustDataItem.get(i) != null) {
                mListOfCustDataItem.get(i).value =
                        mCustMeta.getPropertyBuffer(mListOfCustDataItem.get(i).name);
            }
        }
    }

    @Override
    public void write() {
        Log.d(TAG, "<write>");
        int itemCount = mListOfCustDataItem.size();
        for (int i = 0; i < itemCount; i++) {
            if (mListOfCustDataItem.get(i) != null
                    && mListOfCustDataItem.get(i).value != null) {
                mCustMeta.setPropertyBuffer(mListOfCustDataItem.get(i).name,
                        mListOfCustDataItem.get(i).value);
            }
        }
    }

    @Override
    public Map<String, byte[]> serialize() {
        Log.d(TAG, "<serialize>");
        return mCustMeta.serialize();
    }

    @Override
    public void setData(DataCollections dataCollections) throws NullPointerException {
        if (dataCollections == null) {
            throw new NullPointerException("dataCollections is null!");
        }
        mListOfCustDataItem = dataCollections.listOfCustomDataItem;
    }
}
