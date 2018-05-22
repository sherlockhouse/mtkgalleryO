package com.mediatek.accessor.meta;

import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.XMPMetaFactory;
import com.adobe.xmp.XMPSchemaRegistry;
import com.adobe.xmp.options.SerializeOptions;
import com.adobe.xmp.properties.XMPProperty;
import com.mediatek.accessor.util.Log;
import com.mediatek.accessor.util.TraceHelper;

/**
 * XMP meta.
 */
public class XmpMeta {
    private final static String TAG = Log.Tag(XmpMeta.class.getSimpleName());
    private XMPMeta mMeta;
    private XMPSchemaRegistry mRegister;

    /**
     * XmpMeta constructor.
     */
    public XmpMeta() {
    }

    /**
     * Set meta.
     * @param meta
     *            xmp meta
     */
    public void setMeta(XMPMeta meta) {
        mMeta = meta;
    }

    /**
     * Set registry.
     * @param registry
     *            xmp registry
     */
    public void setRegistry(XMPSchemaRegistry registry) {
        mRegister = registry;
    }

    /**
     * getPropertyString.
     * @param nameSpace
     *            nameSpace
     * @param propName
     *            propName
     * @return result
     */
    public String getPropertyString(String nameSpace, String propName) {
        if (mMeta == null) {
            Log.d(TAG, "<getPropertyString> meta is null, return -1!!!");
            return "";
        }
        try {
            return mMeta.getPropertyString(nameSpace, propName);
        } catch (XMPException e) {
            Log.e(TAG, "<getPropertyString> " + nameSpace + ": " + propName, e);
            return "";
        } catch (NullPointerException e) {
            Log.e(TAG, "<getPropertyString> NullPointerException!!!", e);
            return "";
        }
    }

    /**
     * setPropertyString.
     * @param nameSpace
     *            nameSpace
     * @param propName
     *            propName
     * @param value
     *            value
     */
    public void setPropertyString(String nameSpace, String propName, String value) {
        if (mMeta == null) {
            Log.d(TAG, "<setPropertyString> meta is null, return!!!");
            return;
        }
        try {
            mMeta.setProperty(nameSpace, propName, value);
        } catch (XMPException e) {
            Log.e(TAG, "<setPropertyString> " + nameSpace + ": " + propName, e);
        } catch (NullPointerException e) {
            Log.e(TAG, "<setPropertyString> NullPointerException!!!");
        }
    }

    /**
     * getPropertyString.
     * @param nameSpace
     *            nameSpace
     * @param propName
     *            propName
     * @return result
     */
    public byte[] getPropertyBase64(String nameSpace, String propName) {
        if (mMeta == null) {
            Log.d(TAG, "<getPropertyBase64> meta is null, return -1!!!");
            return null;
        }
        TraceHelper.beginSection(">>>>XmpMeta-getPropertyBase64");
        byte[] propValue = null;
        try {
            propValue = mMeta.getPropertyBase64(nameSpace, propName);
        } catch (XMPException e) {
            Log.e(TAG, "<getPropertyBase64> XMPException, " + nameSpace + ": " + propName);
        } catch (NullPointerException e) {
            Log.e(TAG, "<getPropertyBase64> NullPointerException!!!");
        }
        TraceHelper.endSection();
        return propValue;
    }

    /**
     * setPropertyBase64.
     * @param nameSpace
     *            name space
     * @param propName
     *            property name
     * @param value
     *            property value
     */
    public void setPropertyBase64(String nameSpace, String propName, byte[] value) {
        if (mMeta == null) {
            Log.d(TAG, "<setPropertyString> meta is null, return!!!");
            return;
        }
        TraceHelper.beginSection(">>>>XmpMeta-setPropertyBase64");
        try {
            mMeta.setPropertyBase64(nameSpace, propName, value);
        } catch (XMPException e) {
            Log.e(TAG, "<setPropertyBase64> XMPException, " + nameSpace + ": " + propName);
        } catch (NullPointerException e) {
            Log.e(TAG, "<setPropertyBase64> NullPointerException!!!");
        }
        TraceHelper.endSection();
    }

    /**
     * setStructField.
     * @param nameSpace
     *            nameSpace
     * @param structName
     *            structName
     * @param fieldNS
     *            fieldNS
     * @param fieldName
     *            fieldName
     * @param fieldValue
     *            fieldValue
     */
    public void setStructField(String nameSpace, String structName, String fieldNS,
            String fieldName, String fieldValue) {
        if (mMeta == null) {
            Log.d(TAG, "<setStructField> meta is null, return!!!");
            return;
        }
        try {
            mMeta.setStructField(nameSpace, structName, fieldNS, fieldName, fieldValue);
        } catch (XMPException e) {
            Log.e(TAG, "<setStructField> " + structName + ": " + fieldName, e);
        } catch (NullPointerException e) {
            Log.e(TAG, "<setStructField> NullPointerException!!!", e);
        }
    }

    /**
     * getStructField.
     * @param nameSpace
     *            nameSpace
     * @param structName
     *            structName
     * @param fieldNS
     *            fieldNS
     * @param fieldName
     *            fieldName
     * @return result
     */
    public int getStructField(String nameSpace, String structName, String fieldNS,
                              String fieldName) {
        if (mMeta == null) {
            Log.d(TAG, "<getStructFieldInt> meta is null, return -1");
            return -1;
        }
        try {
            XMPProperty property = mMeta.getStructField(nameSpace, structName, fieldNS, fieldName);
            if (property == null) {
                Log.d(TAG, "<getStructFieldInt> " + fieldNS + ":" + fieldName
                        + ", value is null, return -1");
                return -1;
            }
            return Integer.valueOf((String) property.getValue());
        } catch (XMPException e) {
            Log.e(TAG, "<getStructFieldInt> " + structName + ": " + fieldName, e);
            return -1;
        } catch (NullPointerException e) {
            Log.e(TAG, "<getStructFieldInt> NullPointerException!!!", e);
            return -1;
        }
    }

    /**
     * array index: start from 1, not 0.
     * @param nameSpace
     *            nameSpace
     * @param arrayName
     *            arrayName
     * @param index
     *            index
     * @param itemValue
     *            itemValue
     */
    public void setArrayItem(String nameSpace, String arrayName, int index, String itemValue) {
        if (mMeta == null) {
            Log.d(TAG, "<setArrayItem> meta is null, return!!!");
            return;
        }
        try {
            mMeta.setArrayItem(nameSpace, arrayName, index, itemValue);
        } catch (XMPException e) {
            Log.e(TAG, "<setArrayItem> " + nameSpace + ": " + arrayName, e);
        } catch (NullPointerException e) {
            Log.e(TAG, "<setArrayItem> NullPointerException!!!", e);
        }
    }

    /**
     * getArrayItem.
     * @param nameSpace
     *            nameSpace
     * @param arrayName
     *            arrayName
     * @param index
     *            index
     * @return array item
     */
    public String getArrayItem(String nameSpace, String arrayName, int index) {
        if (mMeta == null) {
            Log.d(TAG, "<getArrayItem> meta is null, return!!!");
            return "";
        }
        try {
            XMPProperty property = mMeta.getArrayItem(nameSpace, arrayName, index);
            if (property == null) {
                Log.d(TAG, "<getStructFieldInt> property is null, return -1");
                return "";
            }
            return String.valueOf(property.getValue());
        } catch (XMPException e) {
            Log.e(TAG, "<getArrayItem> " + nameSpace + ": " + arrayName, e);
            return "";
        } catch (NullPointerException e) {
            Log.e(TAG, "<getArrayItem> NullPointerException!!!", e);
            return "";
        }
    }

    /**
     * registerNamespace.
     * @param nameSpace
     *            name space
     * @param prefix
     *            prefix
     */
    public void registerNamespace(String nameSpace, String prefix) {
        try {
            mRegister.registerNamespace(nameSpace, prefix);
        } catch (XMPException e) {
            Log.e(TAG, "<registerNamespace> " + nameSpace + ": " + prefix, e);
        }
    }

    /**
     * serialize.
     * @return result
     */
    public byte[] serialize() {
        TraceHelper.beginSection(">>>>XmpMeta-serialize");
        byte[] buffer = null;
        try {
            buffer = XMPMetaFactory.serializeToBuffer(mMeta, new SerializeOptions()
                    .setUseCompactFormat(true).setOmitPacketWrapper(true));
        } catch (XMPException e) {
            Log.e(TAG, "<serialize> XMPException", e);
        }
        TraceHelper.endSection();
        return buffer;
    }
}