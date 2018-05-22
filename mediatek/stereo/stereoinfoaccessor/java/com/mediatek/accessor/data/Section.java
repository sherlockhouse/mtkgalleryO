package com.mediatek.accessor.data;

/**
 * Section.
 */
public class Section {
    // e.g. 0xffe1, exif
    public int marker;
    // marker offset from start of file
    public long offset;
    // app length, follow spec, include 2 length bytes
    public int length;
    public String type = "";
    public byte[] buffer;

    /**
     * Section.
     * @param marker
     *            marker
     * @param offset
     *            offset
     * @param length
     *            length
     */
    public Section(int marker, long offset, int length) {
        this.marker = marker;
        this.offset = offset;
        this.length = length;
    }
}
