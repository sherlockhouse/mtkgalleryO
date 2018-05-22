package com.mediatek.accessor.parser;

import java.util.Map;

/**
 * SerializedInfo.
 */
public class SerializedInfo {
    /**
     * XMP key.
     * Keep same with XmpMetaOperator.XMP_KEY.
     */
    public static final String XMP_KEY = "XMP";

    /**
     * Used to saving serialized Standard Xmp buffer.
     */
    public byte[] standardXmpBuf;

    /**
     * Used to saving serialized Extended Xmp buffer.
     */
    public byte[] extendedXmpBuf;

    /**
     * Used to saving serialized CustomizedBuf map.
     */
    public Map<String, byte[]> customizedBufMap;
}
