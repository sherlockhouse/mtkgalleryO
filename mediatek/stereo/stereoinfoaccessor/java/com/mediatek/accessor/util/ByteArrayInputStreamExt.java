package com.mediatek.accessor.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * ByteArrayInputStreamExt.
 */
public class ByteArrayInputStreamExt extends ByteArrayInputStream {
    private final static String TAG = Log.Tag(ByteArrayInputStreamExt.class.getSimpleName());
    private static final int BIT_SHIFT_COUNT_8 = 8;
    private static final int BIT_SHIFT_COUNT_16 = 16;
    private static final int BIT_SHIFT_COUNT_24 = 24;

    /**
     * ByteArrayInputStreamExt.
     * @param buf
     *            buffer
     */
    public ByteArrayInputStreamExt(byte[] buf) {
        super(buf);
        Log.d(TAG, "<ByteArrayInputStreamExt> new instance, buf count 0x"
                + Integer.toHexString(buf.length));
    }

    /**
     * read unsigned shot.
     * @return read result
     */
    public final int readUnsignedShort() {
        int hByte = read();
        int lByte = read();
        return hByte << BIT_SHIFT_COUNT_8 | lByte;
    }

    /**
     * high byte first int.
     * @return read result
     */
    public final int readInt() {
        int firstByte = read();
        int secondByte = read();
        int thirdByte = read();
        int forthByte = read();
        return firstByte << BIT_SHIFT_COUNT_24 | secondByte << BIT_SHIFT_COUNT_16
                | thirdByte << BIT_SHIFT_COUNT_8 | forthByte;
    }

    /**
     * low byte first int.
     * @return read result
     */
    public final int readReverseInt() {
        int forthByte = read();
        int thirdByte = read();
        int secondByte = read();
        int firstByte = read();
        return firstByte << BIT_SHIFT_COUNT_24 | secondByte << BIT_SHIFT_COUNT_16
                | thirdByte << BIT_SHIFT_COUNT_8 | forthByte;
    }

    /**
     * seek.
     * @param offset
     *            offext
     * @throws IOException
     *             exception
     */
    public synchronized void seek(long offset) throws IOException {
        if (offset > count - 1) {
            throw new IOException("offset out of buffer range: offset " + offset
                    + ", buffer count " + count);
        }
        pos = (int) offset;
    }

    /**
     * get file pointer.
     * @return result
     */
    public synchronized long getFilePointer() {
        return pos;
    }

    /**
     * read.
     * @param buffer
     *            buffer
     * @return result
     */
    public int read(byte[] buffer) {
        return read(buffer, 0, buffer.length);
    }
}