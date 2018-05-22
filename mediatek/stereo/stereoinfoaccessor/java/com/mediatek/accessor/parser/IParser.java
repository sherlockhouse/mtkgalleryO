package com.mediatek.accessor.parser;

/**
 * Parser interface, read and write.
 */
public interface IParser {
    /**
     * Parser read.
     */
    public void read();

    /**
     * Parser write.
     */
    public void write();

    /**
     * Parser serialize.
     * @return SerializedInfo
     */
    public SerializedInfo serialize();
}