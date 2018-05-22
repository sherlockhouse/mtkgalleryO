package com.mediatek.accessor.packer;

import com.mediatek.accessor.util.Log;
import com.mediatek.accessor.util.TraceHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Customized data packer.
 */
public class CustomizedDataPacker implements IPacker {
    private final static String TAG = Log.Tag(CustomizedDataPacker.class.getSimpleName());
    private PackInfo mPackInfo;

    /**
     * CustomizedDataPacker constructor.
     * @param packInfo
     *            PackInfo
     * @throws NullPointerException
     *             happened if packInfo is null
     */
    public CustomizedDataPacker(PackInfo packInfo) throws NullPointerException {
        mPackInfo = packInfo;
        if (mPackInfo == null) {
            throw new NullPointerException("mPackInfo is null!");
        }
    }

    @Override
    public void pack() {
        TraceHelper.beginSection(">>>>CustomizedDataPacker-pack");
        Log.d(TAG, "<pack> begin");
        if (mPackInfo == null) {
            Log.d(TAG, "<pack> mPackInfo is null!");
            TraceHelper.endSection();
            return;
        }
        if (mPackInfo.unpackedCustomizedBufMap == null) {
            Log.d(TAG, "<pack> unpackedCustomizedBufMap is null!");
            TraceHelper.endSection();
            return;
        }
        ArrayList<byte[]> custDst = new ArrayList<byte[]>();
        ArrayList<byte[]> packedCustomizedBufArray = new ArrayList<byte[]>();
        for (Map.Entry<String, byte[]> entry : mPackInfo.unpackedCustomizedBufMap.entrySet()) {
            byte[] typeBuffer = entry.getKey().getBytes();
            if (typeBuffer == null) {
                continue;
            }
            custDst = pack(entry.getValue(), typeBuffer);
            if (!custDst.isEmpty()) {
                packedCustomizedBufArray.addAll(custDst);
            }
        }
        mPackInfo.packedCustomizedBufArray = packedCustomizedBufArray;
        Log.d(TAG, "<pack> end");
        TraceHelper.endSection();
    }

    @Override
    public void unpack() {
        TraceHelper.beginSection(">>>>CustomizedDataPacker-unpack");
        Log.d(TAG, "<unpack> begin");
        if (mPackInfo == null) {
            Log.d(TAG, "<unpack> mPackInfo is null!");
            TraceHelper.endSection();
            return;
        }
        if (mPackInfo.packedCustomizedBufArray == null) {
            Log.d(TAG, "<unpack> packedCustomizedBufArray is null!");
            TraceHelper.endSection();
            return;
        }
        mPackInfo.unpackedCustomizedBufMap = new HashMap<String, byte[]>();
        int bufferCount = mPackInfo.packedCustomizedBufArray.size();
        byte[] section = null;
        byte[] bufferTemp = null;
        byte[] bufferType = new byte[PackUtils.TYPE_BUFFER_COUNT];
        String type = "";
        // Define every kind customized buffer array.
        Map<String, ArrayList<byte[]>> categoryCustomizedBufMap =
                new HashMap<String, ArrayList<byte[]>>();
        for (int i = 0; i < bufferCount; i++) {
            section = mPackInfo.packedCustomizedBufArray.get(i);
            if (section == null) {
                continue;
            }
            bufferTemp = new byte[section.length - PackUtils.CUSTOMIZED_TOTAL_FORMAT_LENGTH];
            // get customized buffer
            System.arraycopy(section, PackUtils.CUSTOMIZED_TOTAL_FORMAT_LENGTH, bufferTemp, 0,
                    bufferTemp.length);
            // get customized data type value
            System.arraycopy(section, PackUtils.CUSTOMIZED_TOTAL_LENGTH, bufferType,
                    0, PackUtils.TYPE_BUFFER_COUNT);
            type = new String(bufferType);
            if (categoryCustomizedBufMap.containsKey(type)) {
                categoryCustomizedBufMap.get(type).add(bufferTemp);
            } else {
                ArrayList<byte[]> customizedBuffer = new ArrayList<byte[]>();
                customizedBuffer.add(bufferTemp);
                categoryCustomizedBufMap.put(type, customizedBuffer);
            }
        }

        for (Map.Entry<String, ArrayList<byte[]>> entry : categoryCustomizedBufMap.entrySet()) {
            String typeName = entry.getKey();
            Log.d(TAG, "<unpack> typeName " + typeName);
            if (typeName != null && entry.getValue() != null) {
                mPackInfo.unpackedCustomizedBufMap.put(typeName, joinArraryBuffer(entry
                        .getValue()));
            }
        }
        Log.d(TAG, "<unpack> end");
        TraceHelper.endSection();
    }

    private byte[] joinArraryBuffer(ArrayList<byte[]> bufferArrary) {
        int bufferLength = 0;
        int count = bufferArrary.size();
        for (int i = 0; i < count; i++) {
            bufferLength += bufferArrary.get(i).length;
        }
        byte[] buffer = new byte[bufferLength];
        int currentPos = 0;
        for (int i = 0; i < count; i++) {
            System.arraycopy(bufferArrary.get(i), 0, buffer, currentPos,
                    bufferArrary.get(i).length);
            currentPos += bufferArrary.get(i).length;
        }
        return buffer;
    }

    private ArrayList<byte[]> pack(byte[] bufferData, byte[] type) {
        String typeName = new String(type);
        Log.d(TAG, "<pack> type name is " + typeName);
        ArrayList<byte[]> custDst = new ArrayList<byte[]>();
        int maxBufferContentLength =
                PackUtils.MAX_BYTE_PER_APP1 - PackUtils.CUSTOMIZED_TOTAL_FORMAT_LENGTH;
        int sectionCount = 0;
        if (bufferData.length % maxBufferContentLength == 0) {
            sectionCount = bufferData.length / maxBufferContentLength;
        } else {
            sectionCount = bufferData.length / maxBufferContentLength + 1;
        }
        byte[] section = null;
        byte[] bufferTotalLength = new byte[PackUtils.TYPE_BUFFER_COUNT];
        byte[] serialNumber = new byte[PackUtils.CUSTOMIZED_SERIAL_NUMBER_LENGTH];
        int bufferCurrentPos = 0;
        int sectionCurrentPos = 0;
        int sectionLen = 0;
        for (int i = 0; i < sectionCount; i++) {
            if (i == sectionCount - 1 && bufferData.length % maxBufferContentLength != 0) {
                sectionLen =
                        bufferData.length % maxBufferContentLength
                                + PackUtils.CUSTOMIZED_TOTAL_FORMAT_LENGTH;
            } else {
                sectionLen = PackUtils.MAX_BYTE_PER_APP1;
            }
            section = new byte[sectionLen];
            // 1. copy total length
            bufferTotalLength =
                    PackUtils.intToByteBuffer(bufferData.length,
                            PackUtils.CUSTOMIZED_TOTAL_LENGTH);
            System.arraycopy(bufferTotalLength, 0, section, sectionCurrentPos,
                    PackUtils.CUSTOMIZED_TOTAL_LENGTH);
            sectionCurrentPos += PackUtils.CUSTOMIZED_TOTAL_LENGTH;
            // 2. copy data type
            System.arraycopy(type, 0, section, sectionCurrentPos,
                    PackUtils.TYPE_BUFFER_COUNT);
            sectionCurrentPos += PackUtils.TYPE_BUFFER_COUNT;
            // 3. copy serial number
            serialNumber =
                    PackUtils.intToByteBuffer(i,
                            PackUtils.CUSTOMIZED_SERIAL_NUMBER_LENGTH);
            System.arraycopy(serialNumber, 0, section, sectionCurrentPos,
                    PackUtils.CUSTOMIZED_SERIAL_NUMBER_LENGTH);
            sectionCurrentPos += PackUtils.CUSTOMIZED_SERIAL_NUMBER_LENGTH;
            int copyDataLength = sectionLen - PackUtils.CUSTOMIZED_TOTAL_FORMAT_LENGTH;
            // 4. copy data buffer
            System.arraycopy(bufferData, bufferCurrentPos, section, sectionCurrentPos,
                    copyDataLength);
            bufferCurrentPos += copyDataLength;
            sectionCurrentPos = 0;
            custDst.add(section);
        }
        return custDst;
    }
}
