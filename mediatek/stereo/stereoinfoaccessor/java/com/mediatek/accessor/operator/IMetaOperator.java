package com.mediatek.accessor.operator;

import com.mediatek.accessor.meta.data.DataItem.DataCollections;

import java.util.Map;

/**
 * Meta operator behavior definition.
 */
public interface IMetaOperator {
    /**
     * Encrypt.
     */
    public void encrypt();

    /**
     * Decrypt.
     */
    public void decrypt();

    /**
     * Read data item information from meta.
     */
    public void read();

    /**
     * Write data item to meta.
     */
    public void write();

    /**
     * Serialize meta information to buffer.
     * @return serialized data
     */
    public Map<String, byte[]> serialize();

    /**
     * Set DataCollections to meta operator.
     * @param dataCollections
     *            DataCollections
     */
    public void setData(DataCollections dataCollections);
}
