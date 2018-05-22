package com.mediatek.accessor.packer;

import com.mediatek.accessor.data.Section;
import com.mediatek.accessor.util.ByteArrayInputStreamExt;
import com.mediatek.accessor.util.ByteArrayOutputStreamExt;
import com.mediatek.accessor.util.Log;
import com.mediatek.accessor.util.Utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

/**
 * Pack Utils.
 */
public class PackUtils {
    private final static String TAG = Log.Tag(PackUtils.class.getSimpleName());
    private static final int LOW_HALF_BYTE_MASK = 0X0F;
    private static final int HIGH_HALF_BYTE_MASK = 0XF0;
    private static final int SHIFT_BIT_COUNT_4 = 4;

    public static final int SOI = 0xFFD8;
    public static final int SOS = 0xFFDA;
    public static final int APP1 = 0xFFE1;
    public static final int APP15 = 0xFFEF;
    public static final int DQT = 0xFFDB;
    public static final int DHT = 0xFFC4;
    public static final int APPXTAG_PLUS_LENGTHTAG_BYTE_COUNT = 4;
    public static final int APP15_LENGTHTAG_BYTE_COUNT = 4;
    public static final int TYPE_BUFFER_COUNT = 7;
    public static final int WRITE_XMP_AFTER_SOI = 0;
    public static final int WRITE_XMP_BEFORE_FIRST_APP1 = 1;
    public static final int WRITE_XMP_AFTER_FIRST_APP1 = 2;
    public static final int FIXED_BUFFER_SIZE = 1024 * 10;

    public static final String XMP_EXT_HEADER = "http://ns.adobe.com/xmp/extension/";
    public static final int MAX_BYTE_PER_APP1 = 0XFFB2;
    public static final int MD5_BYTE_COUNT = 32;
    public static final int TOTAL_LENGTH_BYTE_COUNT = 4;
    public static final int PARTITION_OFFSET_BYTE_COUNT = 4;
    public static final int XMP_COMMON_HEADER_LEN = XMP_EXT_HEADER.getBytes().length + 1
            + MD5_BYTE_COUNT + TOTAL_LENGTH_BYTE_COUNT + PARTITION_OFFSET_BYTE_COUNT;
    public static final int MAX_LEN_FOR_REAL_XMP_DATA_PER_APP1 = MAX_BYTE_PER_APP1
            - XMP_COMMON_HEADER_LEN;
    private static final int XMP_HEADER_TAIL_BYTE = 0X0;
    private static final int BYTE_COUNT_4 = 4;
    private static final int BYTE_MASK = 0XFF;
    private static final int SHIFT_BIT_COUNT_8 = 8;

    public static final String APP_SECTION_MAX_LENGTH = "0xffb2";
    public static final String TYPE_JPS_DATA = "JPSDATA";
    public static final String TYPE_JPS_MASK = "JPSMASK";
    public static final String TYPE_DEPTH_DATA = "DEPTHBF";
    public static final String TYPE_DEBUG_BUFFER = "DEBUGBF";
    public static final String TYPE_XMP_DEPTH = "XMPDEPT";
    public static final String TYPE_SEGMENT_MASK = "SEGMASK";
    public static final String TYPE_CLEAR_IMAGE = "CLRIMAG";
    public static final String TYPE_LDC_DATA = "LDCDATA";

    public static final String TYPE_STANDARD_XMP = "standardXmp";
    public static final String TYPE_EXTENDED_XMP = "extendedXmp";
    public static final String TYPE_UNKNOW_APP15 = "unknownApp15";
    public static final String TYPE_EXIF = "exif";
    public static final String EXIF_HEADER = "Exif";
    public static final String XMP_HEADER_START = "http://ns.adobe.com/xap/1.0/\0";

    public static final int CUSTOMIZED_TOTAL_LENGTH = 4;
    public static final int CUSTOMIZED_SERIAL_NUMBER_LENGTH = 1;
    public static final int CUSTOMIZED_TOTAL_FORMAT_LENGTH =
            PackUtils.CUSTOMIZED_TOTAL_LENGTH
            + PackUtils.TYPE_BUFFER_COUNT
            + PackUtils.CUSTOMIZED_SERIAL_NUMBER_LENGTH;

    /**
     * Parse input stream to section collection.
     * @param is
     *            input stream
     * @return result
     */
    public static ArrayList<Section> parseAppInfoFromStream(ByteArrayInputStreamExt is) {
        if (is == null) {
            Log.d(TAG, "<parseAppInfoFromStream> input stream is null!!!");
            return new ArrayList<Section>();
        }
        try {
            // reset position at the file start
            is.seek(0);
            int value = is.readUnsignedShort();
            if (value != SOI) {
                Log.d(TAG, "<parseAppInfoFromStream> error, find no SOI");
                return new ArrayList<Section>();
            }
            Log.d(TAG, "<parseAppInfoFromStream> parse begin!!!");
            int marker = -1;
            long offset = -1;
            int length = -1;
            ArrayList<Section> sections = new ArrayList<Section>();

            while ((value = is.readUnsignedShort()) != -1 && value != SOS) {
                marker = value;
                offset = is.getFilePointer() - 2;
                length = is.readUnsignedShort();
                sections.add(new Section(marker, offset, length));
                is.skip(length - 2);
            }

            // write exif/isXmp flag
            for (int i = 0; i < sections.size(); i++) {
                checkAppSectionTypeInStream(is, sections.get(i));
                Utils.logD(TAG, "<parseAppInfoFromStream> " + getSectionTag(sections.get(i)));
            }
            // reset position at the file start
            is.seek(0);
            Log.d(TAG, "<parseAppInfoFromStream> parse end!!!");
            return sections;
        } catch (IOException e) {
            Log.e(TAG, "<parseAppInfoFromStream> IOException ", e);
            return new ArrayList<Section>();
        }
    }

    /**
     * Return marker: write xmp after this marker.
     * @param sections
     *            section array
     * @return result
     */
    public static int findProperLocationForXmp(ArrayList<Section> sections) {
        for (int i = 0; i < sections.size(); i++) {
            Section sec = sections.get(i);
            if (sec.marker == APP1) {
                if ((PackUtils.TYPE_EXIF).equals(sec.type)) {
                    return WRITE_XMP_AFTER_FIRST_APP1;
                } else {
                    return WRITE_XMP_BEFORE_FIRST_APP1;
                }
            }
        }
        // means no app1, write after SOI
        return WRITE_XMP_AFTER_SOI;
    }

    /**
     * Write section to output stream.
     * @param is
     *            is
     * @param os
     *            os
     * @param sec
     *            sec
     */
    public static void writeSectionToStream(ByteArrayInputStreamExt is, ByteArrayOutputStreamExt os,
            Section sec) {
        Utils.logD(TAG, "<writeSectionToStream> sec.type " + sec.type);
        try {
            os.writeShort(sec.marker);
            os.writeShort(sec.length);
            is.seek(sec.offset + APPXTAG_PLUS_LENGTHTAG_BYTE_COUNT);
            byte[] buffer = null;
            buffer = new byte[sec.length - 2];
            is.read(buffer, 0, buffer.length);
            os.write(buffer);
        } catch (IOException e) {
            Log.e(TAG, "<writeSectionToStream> IOException", e);
        }
    }

    /**
     * Write section to output stream.
     * @param os
     *            os
     * @param sec
     *            sec
     */
    public static void writeSectionToStream(ByteArrayOutputStreamExt os, Section sec) {
        Utils.logD(TAG, "<writeSectionToStream> sec.type " + sec.type);
        try {
            os.writeShort(sec.marker);
            os.writeShort(sec.length);
            os.write(sec.buffer);
        } catch (IOException e) {
            Log.e(TAG, "<writeSectionToStream> IOException", e);
        }
    }

    /**
     * Copy buffer from input stream to output stream.
     * @param is
     *            is
     * @param os
     *            os
     */
    public static void copyToStreamWithFixBuffer(ByteArrayInputStreamExt is,
            ByteArrayOutputStreamExt os) {
        byte[] readBuffer = new byte[FIXED_BUFFER_SIZE];
        int readCount = 0;
        try {
            while ((readCount = is.read(readBuffer)) != -1) {
                if (readCount == FIXED_BUFFER_SIZE) {
                    os.write(readBuffer);
                } else {
                    os.write(readBuffer, 0, readCount);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "<copyToStreamWithFixBuffer> Exception", e);
        }
    }

    /**
     * Copy image blur buffer to output stream.
     * @param blurImageIs
     *            blur image input stream
     * @param os
     *            output stream
     */
    public static void writeImageBuffer(ByteArrayInputStreamExt blurImageIs,
                                        ByteArrayOutputStreamExt os) {
        if (blurImageIs == null) {
            Log.d(TAG, "<writeImageBuffer> input stream is null!!!");
            return;
        }
        try {
            // reset position at the file start
            blurImageIs.seek(0);
            int value = blurImageIs.readUnsignedShort();
            int length = -1;

            while ((value = blurImageIs.readUnsignedShort()) != -1 && value != DQT) {
                length = blurImageIs.readUnsignedShort();
                blurImageIs.skip(length - 2);
            }
            blurImageIs.seek(blurImageIs.getFilePointer() - 2);
            byte[] readBuffer = new byte[FIXED_BUFFER_SIZE];
            int readCount = 0;
            while ((readCount = blurImageIs.read(readBuffer)) != -1) {
                if (readCount == FIXED_BUFFER_SIZE) {
                    os.write(readBuffer);
                } else {
                    os.write(readBuffer, 0, readCount);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get md5 value.
     * @param in
     *            byte array
     * @return md5 value
     */
    public static String getMd5(byte[] in) {
        byte[] md5 = createMd5(in);
        if (md5 == null || md5.length <= 0) {
            Log.d(TAG, "<getMd5> error!!");
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int len = md5.length;
        for (int i = 0; i < len; i++) {
            int high = (md5[i] & HIGH_HALF_BYTE_MASK) >> SHIFT_BIT_COUNT_4;
            int low = md5[i] & LOW_HALF_BYTE_MASK;
            sb.append(Integer.toHexString(high));
            sb.append(Integer.toHexString(low));
        }
        return sb.toString().toUpperCase();
    }

    /**
     * Get extend xmp header.
     * @param md5
     *            md5 value
     * @param totalLength
     *            total length
     * @param sectionNumber
     *            section number
     * @return header
     */
    public static byte[] getXmpCommonHeader(String md5, int totalLength, int sectionNumber) {
        int offset = MAX_LEN_FOR_REAL_XMP_DATA_PER_APP1 * sectionNumber;
        byte[] header = new byte[XMP_COMMON_HEADER_LEN];
        // 1. copy header
        System.arraycopy(XMP_EXT_HEADER.getBytes(), 0, header, 0, XMP_EXT_HEADER.length());
        int currentPos = XMP_EXT_HEADER.length();
        // 2. copy tail byte
        header[currentPos] = XMP_HEADER_TAIL_BYTE;
        currentPos += 1;
        // 3. copy md5
        byte[] md5Buffer = md5.getBytes();
        System.arraycopy(md5Buffer, 0, header, currentPos, md5Buffer.length);
        currentPos += md5Buffer.length;
        // 4. copy 4 bytes totalLen
        byte[] totalLenBuffer = intToByteBuffer(totalLength, BYTE_COUNT_4);
        System.arraycopy(totalLenBuffer, 0, header, currentPos, totalLenBuffer.length);
        currentPos += totalLenBuffer.length;
        // 5. copy 4 bytes offset
        byte[] offsetBuffer = intToByteBuffer(offset, BYTE_COUNT_4);
        System.arraycopy(offsetBuffer, 0, header, currentPos, offsetBuffer.length);
        return header;
    }

    /**
     * Convert Integer to byte array.
     * @param value
     *            Integer
     * @param byteCount
     *            byte length
     * @return byte array
     */
    public static byte[] intToByteBuffer(int value, int byteCount) {
        int in = value;
        byte[] byteBuffer = new byte[byteCount];
        for (int i = byteCount - 1; i >= 0; i--) {
            byteBuffer[i] = (byte) (in & BYTE_MASK);
            in = in >> SHIFT_BIT_COUNT_8;
        }
        return byteBuffer;
    }

    /**
     * Convert byte array to Integer.
     * @param bRefArr
     *            byte array
     * @return Integer value
     */
    public static int byteToInt(byte[] bRefArr) {
        int iOutcome = 0;
        byte bLoop;

        for (int i = 0; i < bRefArr.length; i++) {
            bLoop = bRefArr[i];
            iOutcome += (bLoop & BYTE_MASK) << (SHIFT_BIT_COUNT_8 * (bRefArr.length - 1 - i));
        }
        return iOutcome;
    }

    /**
     * Get customized data buffer type name.
     * @param buffer
     *            data buffer
     * @return type name
     */
    public static String getCustomTypeName(final byte[] buffer) {
        byte[] type = new byte[TYPE_BUFFER_COUNT];
        System.arraycopy(buffer, CUSTOMIZED_TOTAL_LENGTH, type, 0,
                TYPE_BUFFER_COUNT);
        String typeName = new String(type);
        if (typeName != null) {
            return typeName;
        }
        return TYPE_UNKNOW_APP15;
    }

    /**
     * Print section detail information.
     * @param section
     *            instance of Section
     * @return section detail information
     */
    public static String getSectionTag(Section section) {
        String tag =
                "marker 0x" + Integer.toHexString(section.marker) + ", offset 0x"
                        + Long.toHexString(section.offset) + ", length 0x"
                        + Integer.toHexString(section.length) + ", type " + section.type;
        return tag;
    }

    private static void checkAppSectionTypeInStream(ByteArrayInputStreamExt is, Section section) {
        if (is == null || section == null) {
            Log.d(TAG, "<checkAppSectionTypeInStream> input stream or section is null!!!");
            return;
        }
        byte[] buffer = null;
        int type = -1;
        String str = null;
        try {
            if (section.marker == APP15) {
                is.seek(section.offset + APPXTAG_PLUS_LENGTHTAG_BYTE_COUNT
                        + CUSTOMIZED_TOTAL_LENGTH);
                buffer = new byte[TYPE_BUFFER_COUNT];
                is.read(buffer, 0, buffer.length);
                str = new String(buffer);
                if (TYPE_JPS_DATA.equals(str)
                        || TYPE_JPS_MASK.equals(str)
                        || TYPE_DEPTH_DATA.equals(str)
                        || TYPE_SEGMENT_MASK.equals(str)
                        || TYPE_CLEAR_IMAGE.equals(str)
                        || TYPE_LDC_DATA.equals(str)
                        || TYPE_DEBUG_BUFFER.equals(str)) {
                    section.type = str;
                    return;
                }
                section.type = TYPE_UNKNOW_APP15;
                return;
            } else if (section.marker == APP1) {
                is.seek(section.offset + APPXTAG_PLUS_LENGTHTAG_BYTE_COUNT);
                buffer = new byte[XMP_EXT_HEADER.length()];
                is.read(buffer, 0, buffer.length);
                str = new String(buffer);
                if (XMP_EXT_HEADER.equals(str)) {
                    // ext main header is same as ext slave header
                    section.type = TYPE_EXTENDED_XMP;
                    return;
                }
                str = new String(buffer, 0, XMP_HEADER_START.length());
                if (XMP_HEADER_START.equals(str)) {
                    section.type = TYPE_STANDARD_XMP;
                    return;
                }
                str = new String(buffer, 0, EXIF_HEADER.length());
                if (EXIF_HEADER.equals(str)) {
                    section.type = TYPE_EXIF;
                    return;
                }
            }
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "<checkAppSectionTypeInStream> UnsupportedEncodingException" + e);
        } catch (IOException e) {
            Log.e(TAG, "<checkAppSectionTypeInStream> IOException" + e);
        }
    }

    private static byte[] createMd5(byte[] in) {
        if (in == null || in.length <= 0) {
            Log.d(TAG, "<createMd5> input error!!");
            return null;
        }
        byte[] out = null;
        try {
            MessageDigest digestor = MessageDigest.getInstance("MD5");
            digestor.update(in);
            out = digestor.digest();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "<createMd5> NoSuchAlgorithmException ", e);
        }
        return out;
    }
}
