package com.mediatek.accessor.util;

import java.io.ByteArrayOutputStream;

/**
 * ByteArrayOutputStreamExt.
 */
public class ByteArrayOutputStreamExt extends ByteArrayOutputStream {
    private static final int BIT_SHIFT_COUNT_8 = 8;
    private static final int BIT_SHIFT_COUNT_16 = 16;
    private static final int BIT_SHIFT_COUNT_24 = 24;
    private static final int BYTE_MASK_FF = 0xff;

    /**
     * write short.
     * @param val
     *            write data
     */
    public final void writeShort(int val) {
        int hByte = val >> BIT_SHIFT_COUNT_8;
        int lByte = val & BYTE_MASK_FF;
        write(hByte);
        write(lByte);
    }

    /**
     * write int.
     * @param val
     *            data
     */
    public final void writeInt(int val) {
        int firstByte = val >> BIT_SHIFT_COUNT_24;
        int secondByte = (val >> BIT_SHIFT_COUNT_16) & BYTE_MASK_FF;
        int thirdByte = (val >> BIT_SHIFT_COUNT_8) & BYTE_MASK_FF;
        int forthByte = val & BYTE_MASK_FF;
        write(firstByte);
        write(secondByte);
        write(thirdByte);
        write(forthByte);
    }
}