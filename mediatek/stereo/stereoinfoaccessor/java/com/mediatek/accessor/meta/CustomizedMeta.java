package com.mediatek.accessor.meta;

import com.mediatek.accessor.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Customized meta.
 */
public class CustomizedMeta {
    private final static String TAG = Log.Tag(CustomizedMeta.class.getSimpleName());
    private Map<String, byte[]> mCustData;

    /**
     * CustMeta constructor.
     * @param custData
     *            customized data
     */
    public CustomizedMeta(Map<String, byte[]> custData) {
        if (custData != null && custData.size() != 0) {
            mCustData = custData;
        } else {
            mCustData = new HashMap<String, byte[]>();
        }
    }

    /**
     * Get special kind of customized data.
     * @param name
     *            kind name
     * @return data buffer
     */
    public byte[] getPropertyBuffer(String name) {
        Log.d(TAG, "<getPropertyBuffer> name " + name);
        if (mCustData != null) {
            return mCustData.get(name);
        }
        return null;
    }

    /**
     * Set special kind of customized data.
     * @param name
     *            kind name
     * @param buffer
     *            data buffer
     */
    public void setPropertyBuffer(String name, byte[] buffer) {
        Log.d(TAG, "<setPropertyBuffer> name " + name);
        if (mCustData != null) {
            mCustData.put(name, buffer);
        }
    }

    /**
     * serialize.
     * @return result
     */
    public Map<String, byte[]> serialize() {
        return mCustData;
    }
}