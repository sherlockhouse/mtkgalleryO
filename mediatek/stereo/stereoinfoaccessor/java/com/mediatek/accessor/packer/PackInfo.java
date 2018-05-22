package com.mediatek.accessor.packer;

import java.util.ArrayList;
import java.util.Map;

/**
 * Pack parameter for packer and unpacker.
 *
 * Packing ------------------------------------>------------------------------------------------>
 *                       |serialize               |Cust/XmpPacker           |JpgPacker
 *                       (meta serialize)         (append header)           (append APPX tag)
 *
 * StandardXMP           |byte[]                  |byte[]                   |byte[]
 * ExtendedXMP           |byte[]                  |ArrayList<byte[]>        |ArrayList<byte[]>
 * CustomizedData        |map<String, byte[]>     |ArrayList<byte[]>        |ArrayList<byte[]>
 *
 * <-------------------------------------------<----------------------------------------unpacking
 */
public class PackInfo {
    // input for packing, after packing, result will be saved into "outJpgBuffer"
    public byte[] unpackedJpgBuf;
    public byte[] unpackedBlurImageBuf;
    public byte[] unpackedStandardXmpBuf;
    public byte[] unpackedExtendedXmpBuf;
    public Map<String, byte[]> unpackedCustomizedBufMap;

    // input for unpacking, after unpacking, result will be saved into "in-xxxx"
    public byte[] packedJpgBuf;
    public byte[] packedStandardXmpBuf;
    public ArrayList<byte[]> packedExtendedXmpBufArray;
    public ArrayList<byte[]> packedCustomizedBufArray;
}
