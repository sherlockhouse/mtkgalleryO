package com.mediatek.accessor.packer;

import com.mediatek.accessor.data.Section;
import com.mediatek.accessor.util.ByteArrayInputStreamExt;
import com.mediatek.accessor.util.ByteArrayOutputStreamExt;
import com.mediatek.accessor.util.Log;
import com.mediatek.accessor.util.TraceHelper;
import com.mediatek.accessor.util.Utils;

import java.io.IOException;
import java.util.ArrayList;

/**
 * JPG packer.
 */
public class JpgPacker implements IPacker {
    private final static String TAG = Log.Tag(JpgPacker.class.getSimpleName());
    private PackInfo mPackInfo;

    /**
     * JpgPacker constructor.
     * @param packInfo
     *            pack information
     * @throws NullPointerException
     *             happened if packInfo is null
     */
    public JpgPacker(PackInfo packInfo) throws NullPointerException {
        mPackInfo = packInfo;
        if (mPackInfo == null) {
            throw new NullPointerException("mPackInfo is null!");
        }
    }

    @Override
    public void pack() {
        TraceHelper.beginSection(">>>>JpgPacker-pack");
        Log.d(TAG, "<pack> begin");
        if (mPackInfo == null) {
            Log.d(TAG, "<pack> mPackInfo is null!");
            TraceHelper.endSection();
            return;
        }
        if (mPackInfo.unpackedJpgBuf == null) {
            Log.d(TAG, "<pack> unpackedJpgBuf is null!");
            return;
        }
        Section standardSection = null;
        ArrayList<Section> extendedSections = new ArrayList<Section>();
        ArrayList<Section> customizedSections = new ArrayList<Section>();
        if (mPackInfo.packedStandardXmpBuf != null) {
            standardSection =
                    new Section(PackUtils.APP1, 0, mPackInfo.packedStandardXmpBuf.length + 2);
            standardSection.buffer = mPackInfo.packedStandardXmpBuf;
            standardSection.type = PackUtils.TYPE_STANDARD_XMP;
        }
        if (mPackInfo.packedExtendedXmpBufArray != null) {
            extendedSections = makeJpgSections(PackUtils.APP1,
                    mPackInfo.packedExtendedXmpBufArray);
        }
        if (mPackInfo.packedCustomizedBufArray != null) {
            customizedSections = makeJpgSections(PackUtils.APP15,
                    mPackInfo.packedCustomizedBufArray);
        }

        ByteArrayInputStreamExt is = new ByteArrayInputStreamExt(mPackInfo.unpackedJpgBuf);
        ByteArrayOutputStreamExt os = new ByteArrayOutputStreamExt();
        pack(is, os, standardSection, extendedSections, customizedSections);
        mPackInfo.packedJpgBuf = os.toByteArray();
        try {
            is.close();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "<pack> end");
        TraceHelper.endSection();
    }

    @Override
    public void unpack() {
        TraceHelper.beginSection(">>>>JpgPacker-unpack");
        Log.d(TAG, "<unpack> begin");
        if (mPackInfo == null) {
            Log.d(TAG, "<unpack> mPackInfo is null!");
            TraceHelper.endSection();
            return;
        }
        if (mPackInfo.packedJpgBuf == null) {
            Log.d(TAG, "<unpack> packedJpgBuf is null!");
            return;
        }
        ByteArrayInputStreamExt inputStream = new ByteArrayInputStreamExt(
                mPackInfo.packedJpgBuf);
        ArrayList<Section> srcJpgSections = PackUtils.parseAppInfoFromStream(inputStream);
        byte[] standardXmp = null;
        ArrayList<byte[]> extendedXmp = new ArrayList<byte[]>();
        ArrayList<byte[]> custDataBuffer = new ArrayList<byte[]>();
        int srcJpgSectionsSize = srcJpgSections.size();
        try {
            for (int i = 0; i < srcJpgSectionsSize; i++) {
                Section sec = srcJpgSections.get(i);
                if ((PackUtils.TYPE_STANDARD_XMP).equals(sec.type)) {
                    inputStream.seek(sec.offset + 4);
                    standardXmp = new byte[sec.length - 2];
                    inputStream.read(standardXmp);
                }
                if ((PackUtils.TYPE_EXTENDED_XMP).equals(sec.type)) {
                    inputStream.seek(sec.offset + 4);
                    byte[] extendardBuffer = new byte[sec.length - 2];
                    inputStream.read(extendardBuffer);
                    extendedXmp.add(extendardBuffer);
                }
                if ((PackUtils.TYPE_DEPTH_DATA).equals(sec.type)
                        || (PackUtils.TYPE_JPS_DATA).equals(sec.type)
                        || (PackUtils.TYPE_JPS_MASK).equals(sec.type)
                        || (PackUtils.TYPE_SEGMENT_MASK).equals(sec.type)
                        || (PackUtils.TYPE_CLEAR_IMAGE).equals(sec.type)
                        || (PackUtils.TYPE_LDC_DATA).equals(sec.type)
                        || (PackUtils.TYPE_DEBUG_BUFFER).equals(sec.type)) {
                    inputStream.seek(sec.offset + 4);
                    byte[] customXmp = new byte[sec.length - 2];
                    inputStream.read(customXmp);
                    custDataBuffer.add(customXmp);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        mPackInfo.packedStandardXmpBuf = standardXmp;
        mPackInfo.packedExtendedXmpBufArray = extendedXmp;
        mPackInfo.packedCustomizedBufArray = custDataBuffer;
        Log.d(TAG, "<unpack> end");
        TraceHelper.endSection();
    }

    private ArrayList<Section> makeJpgSections(int marker, ArrayList<byte[]> sections) {
        Log.d(TAG, "<makeJpgSections>");
        ArrayList<Section> jpgSections = new ArrayList<Section>();
        Section section;
        int bufferCount = sections.size();
        for (int i = 0; i < bufferCount; i++) {
            byte[] buffer = sections.get(i);
            if (buffer == null) {
                continue;
            }
            section = new Section(marker, 0, buffer.length + 2);
            if (marker == PackUtils.APP1) {
                Utils.logD(TAG, "<makeJpgSections> type is TYPE_EXTENDED_XMP");
                section.type = PackUtils.TYPE_EXTENDED_XMP;
            } else {
                String typename = PackUtils.getCustomTypeName(buffer);
                Utils.logD(TAG, "<makeJpgSections> type is " + typename);
                section.type = typename;
            }
            section.buffer = buffer;
            jpgSections.add(section);
        }
        return jpgSections;
    }

    private void pack(ByteArrayInputStreamExt is, ByteArrayOutputStreamExt os,
                      Section standardSection, ArrayList<Section> extendedSections,
                      ArrayList<Section> customizedSections) {
        Log.d(TAG, "<pack> write begin!!!");
        ArrayList<Section> srcJpgSections = PackUtils.parseAppInfoFromStream(is);
        os.writeShort(PackUtils.SOI);
        int writenLocation = PackUtils.findProperLocationForXmp(srcJpgSections);
        boolean hasWritenXmp = false;
        boolean hasWritenCustomizedData = false;
        boolean needWriteBlurImage = (mPackInfo.unpackedBlurImageBuf != null);
        boolean hasWritenBlurImage = false;
        if (writenLocation == PackUtils.WRITE_XMP_AFTER_SOI) {
            // means no APP1
            Log.d(TAG, "<pack> No APP1 information!");
            writeXmp(os, standardSection, extendedSections);
            hasWritenXmp = true;
        }
        for (int i = 0; i < srcJpgSections.size(); i++) {
            Section sec = srcJpgSections.get(i);
            if ((PackUtils.TYPE_EXIF).equals(sec.type)) {
                Log.d(TAG, "<pack> write exif, " + PackUtils.getSectionTag(sec));
                PackUtils.writeSectionToStream(is, os, sec);
                if (!hasWritenXmp) {
                    writeXmp(os, standardSection, extendedSections);
                    hasWritenXmp = true;
                }
            } else {
                if (!hasWritenXmp) {
                    Log.d(TAG, "<pack> write xmp, " + PackUtils.getSectionTag(sec));
                    writeXmp(os, standardSection, extendedSections);
                    hasWritenXmp = true;
                }
                // APPx must be before DQT/DHT
                if (!hasWritenCustomizedData
                        && (sec.marker == PackUtils.DQT
                        || sec.marker == PackUtils.DHT)) {
                    Log.d(TAG, "<pack> write custom, " + PackUtils.getSectionTag(sec));
                    writeCust(os, customizedSections);
                    hasWritenCustomizedData = true;
                }
                // write blur image
                if (needWriteBlurImage && !hasWritenBlurImage
                        && sec.marker == PackUtils.DQT) {
                    Log.d(TAG, "<pack> copy blur image to output stream");
                    Log.d(TAG, "<pack> write blur, " + PackUtils.getSectionTag(sec));
                    ByteArrayInputStreamExt blurImageIs =
                            new ByteArrayInputStreamExt(mPackInfo.unpackedBlurImageBuf);
                    PackUtils.writeImageBuffer(blurImageIs, os);
                    hasWritenBlurImage = true;
                    Log.d(TAG, "<pack> write end!!!");
                    return;
                }
                if ((PackUtils.TYPE_DEPTH_DATA).equals(sec.type)
                        || (PackUtils.TYPE_JPS_DATA).equals(sec.type)
                        || (PackUtils.TYPE_JPS_MASK).equals(sec.type)
                        || (PackUtils.TYPE_SEGMENT_MASK).equals(sec.type)
                        || (PackUtils.TYPE_STANDARD_XMP).equals(sec.type)
                        || (PackUtils.TYPE_EXTENDED_XMP).equals(sec.type)
                        || (PackUtils.TYPE_CLEAR_IMAGE).equals(sec.type)
                        || (PackUtils.TYPE_LDC_DATA).equals(sec.type)
                        || (PackUtils.TYPE_DEBUG_BUFFER).equals(sec.type)) {
                    // skip old data
                    is.skip(sec.length + 2);
                    Utils.logD(TAG, "<pack> skip old data, type: " + sec.type);
                } else {
                    Utils.logD(TAG, "<pack> write other info, " + PackUtils.getSectionTag(sec));
                    PackUtils.writeSectionToStream(is, os, sec);
                }
            }
        }
        // write jps and mask to app15, before sos
        if (!hasWritenCustomizedData) {
            writeCust(os, customizedSections);
            hasWritenCustomizedData = true;
        }
        // write remain whole file (from SOS)
        if (!hasWritenBlurImage) {
            Log.d(TAG, "<pack> write remain whole file (from SOS)");
            PackUtils.copyToStreamWithFixBuffer(is, os);
        }
        Log.d(TAG, "<pack> write end!!!");
    }

    private void writeCust(ByteArrayOutputStreamExt os,
                           ArrayList<Section> customizedSections) {
        int customizedSectionsSize = customizedSections.size();
        Log.d(TAG, "<writeCust> customizedSections size " + customizedSectionsSize);
        if (customizedSectionsSize == 0) {
            return;
        }
        for (int i = 0; i < customizedSectionsSize; i++) {
            PackUtils.writeSectionToStream(os, customizedSections.get(i));
        }
    }

    private void writeXmp(ByteArrayOutputStreamExt os,
                          Section standardSection, ArrayList<Section> extendedSections) {
        if (standardSection != null) {
            Log.d(TAG, "<writeXmp> standardxmp");
            PackUtils.writeSectionToStream(os, standardSection);
        }
        int extendedSectionsSize = extendedSections.size();
        Log.d(TAG, "<writeXmp> extendedSectionsSize size " + extendedSectionsSize);
        if (extendedSectionsSize == 0) {
            return;
        }
        for (int i = 0; i < extendedSectionsSize; i++) {
            PackUtils.writeSectionToStream(os, extendedSections.get(i));
        }
    }
}