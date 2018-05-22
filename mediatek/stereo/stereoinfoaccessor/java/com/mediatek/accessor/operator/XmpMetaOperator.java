package com.mediatek.accessor.operator;

import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMetaFactory;
import com.mediatek.accessor.meta.XmpMeta;
import com.mediatek.accessor.meta.data.DataItem.ArrayItem;
import com.mediatek.accessor.meta.data.DataItem.BufferItem;
import com.mediatek.accessor.meta.data.DataItem.DataCollections;
import com.mediatek.accessor.meta.data.DataItem.SimpleItem;
import com.mediatek.accessor.meta.data.DataItem.StructItem;
import com.mediatek.accessor.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Meta operator for standard or extended data.
 */
public class XmpMetaOperator implements IMetaOperator {
    private final static String TAG = Log.Tag(XmpMetaOperator.class.getSimpleName());
    // Keep same with SerializedInfo.XMP_KEY
    private static final String XMP_KEY = "XMP";
    private ArrayList<SimpleItem> mListOfSimpleValue = new ArrayList<SimpleItem>();
    private ArrayList<BufferItem> mListOfBufferItem = new ArrayList<BufferItem>();
    private ArrayList<ArrayItem> mListOfArrayItem = new ArrayList<ArrayItem>();
    private ArrayList<StructItem> mListOfStructItem = new ArrayList<StructItem>();
    private int mDest;
    private XmpMeta mMeta;

    /**
     * MetaOperator constructor.
     * @param xmpBuffer
     *            xmp data buffer
     */
    public XmpMetaOperator(byte[] xmpBuffer) {
        mMeta = new XmpMeta();
        try {
            if (xmpBuffer != null && xmpBuffer.length != 0) {
                mMeta.setMeta(XMPMetaFactory.parseFromBuffer(xmpBuffer));
            } else {
                mMeta.setMeta(XMPMetaFactory.create());
            }
            mMeta.setRegistry(XMPMetaFactory.getSchemaRegistry());
        } catch (XMPException xe) {
            Log.e(TAG, "<XmpMetaOperator> XMPException ", xe);
            mMeta = null;
        }
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
        if (mListOfSimpleValue != null) {
            int simpleValueCount = mListOfSimpleValue.size();
            SimpleItem simpleValue = null;
            for (int i = 0; i < simpleValueCount; i++) {
                simpleValue = mListOfSimpleValue.get(i);
                if (simpleValue.dest == mDest) {
                    simpleValue.value = mMeta.getPropertyString(
                            simpleValue.nameSpaceItem.nameSpace, simpleValue.name);
                    Log.d(TAG, "<read> after simpleValue.nameSpaceItem.nameSpace "
                            + simpleValue.nameSpaceItem.nameSpace + ", simpleValue.name "
                            + simpleValue.name + ", simpleValue.value "
                            + simpleValue.value);
                    mListOfSimpleValue.set(i, simpleValue);
                }
            }
        }
        if (mListOfBufferItem != null) {
            int bufferItemCount = mListOfBufferItem.size();
            BufferItem bufferItem = null;
            for (int i = 0; i < bufferItemCount; i++) {
                bufferItem = mListOfBufferItem.get(i);
                if (bufferItem.dest == mDest) {
                    bufferItem.value = mMeta.getPropertyBase64(
                            bufferItem.nameSpaceItem.nameSpace, bufferItem.name);
                    mListOfBufferItem.set(i, bufferItem);
                }
            }
        }
        if (mListOfArrayItem != null) {
            int arrayItemCount = mListOfArrayItem.size();
            ArrayItem arrayItem = null;
            for (int i = 0; i < arrayItemCount; i++) {
                arrayItem = mListOfArrayItem.get(i);
                if (arrayItem.dest == mDest) {
                    arrayItem.value = mMeta.getArrayItem(arrayItem.nameSpaceItem.nameSpace,
                            arrayItem.arrayName, i);
                    mListOfArrayItem.set(i, arrayItem);
                }
            }
        }
        if (mListOfStructItem != null) {
            int structItemCount = mListOfStructItem.size();
            StructItem structItem = null;
            for (int i = 0; i < structItemCount; i++) {
                structItem = mListOfStructItem.get(i);
                if (structItem.dest == mDest) {
                    mMeta.registerNamespace(structItem.structNameSpaceItem.nameSpace,
                            structItem.structNameSpaceItem.nameSpacePrifix);
                    mMeta.registerNamespace(structItem.fieldNameSpaceItem.nameSpace,
                            structItem.fieldNameSpaceItem.nameSpacePrifix);
                    structItem.fieldValue = String.valueOf(mMeta.getStructField(
                            structItem.structNameSpaceItem.nameSpace, structItem.structName,
                            structItem.fieldNameSpaceItem.nameSpace, structItem.fieldName));
                    mListOfStructItem.set(i, structItem);
                }
            }
        }
    }

    @Override
    public void write() {
        Log.d(TAG, "<write>");
        if (mListOfSimpleValue != null) {
            int simpleValueCount = mListOfSimpleValue.size();
            SimpleItem simpleValue = null;
            for (int i = 0; i < simpleValueCount; i++) {
                simpleValue = mListOfSimpleValue.get(i);
                if (simpleValue.dest == mDest && simpleValue.value != null) {
                    mMeta.registerNamespace(simpleValue.nameSpaceItem.nameSpace,
                            simpleValue.nameSpaceItem.nameSpacePrifix);
                    mMeta.setPropertyString(simpleValue.nameSpaceItem.nameSpace,
                            simpleValue.name, simpleValue.value);
                }
            }
        }
        if (mListOfBufferItem != null) {
            int bufferItemCount = mListOfBufferItem.size();
            BufferItem bufferItem = new BufferItem();
            for (int i = 0; i < bufferItemCount; i++) {
                bufferItem = mListOfBufferItem.get(i);
                if (bufferItem.dest == mDest && bufferItem.value != null) {
                    mMeta.registerNamespace(bufferItem.nameSpaceItem.nameSpace,
                            bufferItem.nameSpaceItem.nameSpacePrifix);
                    mMeta.setPropertyBase64(bufferItem.nameSpaceItem.nameSpace,
                            bufferItem.name, bufferItem.value);
                }
            }
        }
        if (mListOfArrayItem != null) {
            int arrayItemCount = mListOfArrayItem.size();
            ArrayItem arrayItem = new ArrayItem();
            for (int i = 0; i < arrayItemCount; i++) {
                arrayItem = mListOfArrayItem.get(i);
                if (arrayItem.dest == mDest && arrayItem.value != null) {
                    mMeta.registerNamespace(arrayItem.nameSpaceItem.nameSpace,
                            arrayItem.nameSpaceItem.nameSpacePrifix);
                    mMeta.setArrayItem(arrayItem.nameSpaceItem.nameSpace, arrayItem.arrayName,
                            i, arrayItem.value);
                }
            }
        }
        if (mListOfStructItem != null) {
            int structItemCount = mListOfStructItem.size();
            StructItem structItem = new StructItem();
            for (int i = 0; i < structItemCount; i++) {
                structItem = mListOfStructItem.get(i);
                if (structItem.dest == mDest && structItem.fieldValue != null) {
                    mMeta.registerNamespace(structItem.structNameSpaceItem.nameSpace,
                            structItem.structNameSpaceItem.nameSpacePrifix);
                    mMeta.registerNamespace(structItem.fieldNameSpaceItem.nameSpace,
                            structItem.fieldNameSpaceItem.nameSpacePrifix);
                    mMeta.setStructField(structItem.structNameSpaceItem.nameSpace,
                            structItem.structName, structItem.fieldNameSpaceItem.nameSpace,
                            structItem.fieldName, structItem.fieldValue);
                }
            }
        }
    }

    @Override
    public Map<String, byte[]> serialize() {
        Log.d(TAG, "<serialize>");
        Map<String, byte[]> result = new HashMap<String, byte[]>();
        result.put(XMP_KEY, mMeta.serialize());
        return result;
    }

    @Override
    public void setData(DataCollections dataCollections) throws NullPointerException {
        if (dataCollections == null) {
            throw new NullPointerException("dataCollections is null!");
        }
        mDest = dataCollections.dest;
        mListOfSimpleValue = dataCollections.listOfSimpleValue;
        mListOfBufferItem = dataCollections.listOfBufferItem;
        mListOfArrayItem = dataCollections.listOfArrayItem;
        mListOfStructItem = dataCollections.listOfStructItem;
    }
}
