package com.mediatek.accessor.packer;

import com.mediatek.accessor.util.Log;
import com.mediatek.accessor.util.TraceHelper;

import java.util.ArrayList;

/**
 * Standard and extended data buffer pack and unpack.
 */
public class XmpPacker implements IPacker {
    private final static String TAG = Log.Tag(XmpPacker.class.getSimpleName());
    private PackInfo mPackInfo;

    /**
     * XmpPacker constructor.
     * @param packInfo
     *            PackInfo
     * @throws NullPointerException
     *             happened if packInfo is null
     */
    public XmpPacker(PackInfo packInfo) throws NullPointerException {
        mPackInfo = packInfo;
        if (mPackInfo == null) {
            throw new NullPointerException("mPackInfo is null!");
        }
    }

    @Override
    public void pack() {
        TraceHelper.beginSection(">>>>XmpPacker-pack");
        Log.d(TAG, "<pack> begin");
        if (mPackInfo == null) {
            Log.d(TAG, "<pack> mPackInfo is null!");
            TraceHelper.endSection();
            return;
        }
        if (mPackInfo.unpackedStandardXmpBuf != null) {
            byte[] bufferOut =
                    new byte[mPackInfo.unpackedStandardXmpBuf.length
                            + PackUtils.XMP_HEADER_START.length()];
            System.arraycopy(PackUtils.XMP_HEADER_START.getBytes(), 0, bufferOut, 0,
                    PackUtils.XMP_HEADER_START.length());
            System.arraycopy(mPackInfo.unpackedStandardXmpBuf, 0, bufferOut,
                    PackUtils.XMP_HEADER_START.length(),
                    mPackInfo.unpackedStandardXmpBuf.length);
            mPackInfo.packedStandardXmpBuf = bufferOut;
        }
        if (mPackInfo.unpackedExtendedXmpBuf != null) {
            mPackInfo.packedExtendedXmpBufArray =
                    makeExtXmpData(mPackInfo.unpackedExtendedXmpBuf);
        }
        Log.d(TAG, "<pack> end");
        TraceHelper.endSection();
    }

    @Override
    public void unpack() {
        TraceHelper.beginSection(">>>>XmpPacker-unpack");
        Log.d(TAG, "<unpack> begin");
        if (mPackInfo == null) {
            Log.d(TAG, "<unpack> mPackInfo is null!");
            TraceHelper.endSection();
            return;
        }
        if (mPackInfo.packedStandardXmpBuf != null) {
            byte[] bufferOut =
                    new byte[mPackInfo.packedStandardXmpBuf.length
                            - PackUtils.XMP_HEADER_START.length()];
            System.arraycopy(mPackInfo.packedStandardXmpBuf, PackUtils.XMP_HEADER_START.length(),
                    bufferOut, 0, bufferOut.length);
            mPackInfo.unpackedStandardXmpBuf = bufferOut;
        }
        if (mPackInfo.packedExtendedXmpBufArray != null) {
            ArrayList<byte[]> extXmpDataArray = mPackInfo.packedExtendedXmpBufArray;
            byte[] section = null;
            byte[] bufferOut = null;
            byte[] bufferTemp = null;
            int extendedXmpLength = 0;
            int totalFormatLength =
                    PackUtils.XMP_EXT_HEADER.length() + PackUtils.MD5_BYTE_COUNT
                            + PackUtils.TOTAL_LENGTH_BYTE_COUNT
                            + PackUtils.PARTITION_OFFSET_BYTE_COUNT + 1;
            int bufferCount = extXmpDataArray.size();
            for (int i = 0; i < bufferCount; i++) {
                section = extXmpDataArray.get(i);
                if (bufferOut != null) {
                    bufferTemp = new byte[extendedXmpLength];
                    bufferTemp = bufferOut;
                    extendedXmpLength += section.length - totalFormatLength;
                    bufferOut = new byte[extendedXmpLength];
                    System.arraycopy(bufferTemp, 0, bufferOut, 0, bufferTemp.length);
                    System.arraycopy(section, totalFormatLength, bufferOut, bufferTemp.length,
                            section.length - totalFormatLength);
                } else {
                    extendedXmpLength = section.length - totalFormatLength;
                    bufferOut = new byte[extendedXmpLength];
                    System.arraycopy(section, totalFormatLength, bufferOut, 0, extendedXmpLength);
                }
            }
            mPackInfo.unpackedExtendedXmpBuf = bufferOut;
        }
        Log.d(TAG, "<unpack> end");
        TraceHelper.endSection();
    }

    private ArrayList<byte[]> makeExtXmpData(byte[] extXmpData) {
        Log.d(TAG, "<makeExtXmpData>");
        ArrayList<byte[]> extXmpDataArray = new ArrayList<byte[]>();
        String valueOfMd5 = PackUtils.getMd5(extXmpData);
        int sectionCount = 0;
        if (extXmpData.length % PackUtils.MAX_LEN_FOR_REAL_XMP_DATA_PER_APP1 == 0) {
            sectionCount =
                    extXmpData.length / PackUtils.MAX_LEN_FOR_REAL_XMP_DATA_PER_APP1;
        } else {
            sectionCount =
                    extXmpData.length / PackUtils.MAX_LEN_FOR_REAL_XMP_DATA_PER_APP1 + 1;
        }
        byte[] commonHeader = null;
        byte[] section = null;
        int currentPos = 0;
        for (int i = 0; i < sectionCount; i++) {
            commonHeader = PackUtils.getXmpCommonHeader(valueOfMd5, extXmpData.length, i);
            if (i == sectionCount - 1
                    && extXmpData.length % PackUtils.MAX_LEN_FOR_REAL_XMP_DATA_PER_APP1 != 0) {
                int sectionLen =
                        extXmpData.length % PackUtils.MAX_LEN_FOR_REAL_XMP_DATA_PER_APP1
                                + commonHeader.length;
                section = new byte[sectionLen];
                // 1. copy header
                System.arraycopy(commonHeader, 0, section, 0, commonHeader.length);
                // 2. copy data
                System.arraycopy(extXmpData, currentPos, section, commonHeader.length,
                        sectionLen - commonHeader.length);
                currentPos += sectionLen - commonHeader.length;
            } else {
                section = new byte[PackUtils.MAX_BYTE_PER_APP1];
                // 1. copy header
                System.arraycopy(commonHeader, 0, section, 0, commonHeader.length);
                // 2. copy data
                System.arraycopy(extXmpData, currentPos, section, commonHeader.length,
                        PackUtils.MAX_BYTE_PER_APP1 - commonHeader.length);
                currentPos += PackUtils.MAX_BYTE_PER_APP1 - commonHeader.length;
            }
            extXmpDataArray.add(i, section);
        }
        return extXmpDataArray;
    }
}