package com.mediatek.accessor.operator;

import java.util.Map;

/**
 * Meta operator factory, for create meta operator.
 */
public class MetaOperatorFactory {
    public static final int XMP_META_OPERATOR = 0;
    public static final int CUSTOMIZED_META_OPERATOR = 1;

    /**
     * Get meta operator instance by meta type.
     * @param metaType
     *            meta type
     * @param xmpBuffer
     *            standard or extended data buffer
     * @param customizedBuffer
     *            customized data buffer
     * @return IMetaOperator
     */
    public static IMetaOperator getOperatorInstance(int metaType, byte[] xmpBuffer,
            Map<String, byte[]> customizedBuffer) {
        switch (metaType) {
        case XMP_META_OPERATOR:
            return new XmpMetaOperator(xmpBuffer);
        case CUSTOMIZED_META_OPERATOR:
            return new CustomizedMetaOperator(customizedBuffer);
        default:
            return null;
        }
    }
}
