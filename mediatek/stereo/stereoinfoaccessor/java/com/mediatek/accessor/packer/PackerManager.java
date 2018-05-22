package com.mediatek.accessor.packer;

import com.mediatek.accessor.util.Log;
import com.mediatek.accessor.util.TraceHelper;

/**
 * PackerManager, manage pack work flow.
 */
public class PackerManager {
    private final static String TAG = Log.Tag(PackerManager.class.getSimpleName());

    private IPacker mCustDataPacker;
    private IPacker mJpgPacker;
    private IPacker mXmpPacker;

    /**
     * PackerManager constructor.
     */
    public PackerManager() {
    }

    /**
     * Pack work flow.
     * @param packInfo
     *            PackInfo
     * @return JPG buffer
     */
    public byte[] pack(PackInfo packInfo) {
        Log.d(TAG, "<pack>");
        TraceHelper.beginSection(">>>>PackerManager-pack");
        // 1. append xmp format
        mXmpPacker = new XmpPacker(packInfo);
        mXmpPacker.pack();
        // 2. append cust format
        mCustDataPacker = new CustomizedDataPacker(packInfo);
        mCustDataPacker.pack();
        // 3. append jpg format
        mJpgPacker = new JpgPacker(packInfo);
        mJpgPacker.pack();
        TraceHelper.endSection();
        return packInfo.packedJpgBuf;
    }

    /**
     * Unpack work flow.
     * @param src
     *            JPG buffer
     * @return PackInfo
     */
    public PackInfo unpack(byte[] src) {
        Log.d(TAG, "<unpack>");
        TraceHelper.beginSection(">>>>PackerManager-unpack");
        PackInfo packInfo = new PackInfo();
        packInfo.packedJpgBuf = src;
        // split jpg format
        mJpgPacker = new JpgPacker(packInfo);
        mJpgPacker.unpack();
        // split xmp format
        mXmpPacker = new XmpPacker(packInfo);
        mXmpPacker.unpack();
        // split cust format
        mCustDataPacker = new CustomizedDataPacker(packInfo);
        mCustDataPacker.unpack();
        TraceHelper.endSection();
        return packInfo;
    }
}
