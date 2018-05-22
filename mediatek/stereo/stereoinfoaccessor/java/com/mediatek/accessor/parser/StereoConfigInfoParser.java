package com.mediatek.accessor.parser;

import com.mediatek.accessor.data.StereoConfigInfo;
import com.mediatek.accessor.data.StereoConfigInfo.FaceDetectionInfo;
import com.mediatek.accessor.data.StereoConfigInfo.FocusInfo;
import com.mediatek.accessor.meta.data.DataItem;
import com.mediatek.accessor.meta.data.DataItem.BufferItem;
import com.mediatek.accessor.meta.data.DataItem.DataCollections;
import com.mediatek.accessor.meta.data.DataItem.NameSpaceItem;
import com.mediatek.accessor.meta.data.DataItem.SimpleItem;
import com.mediatek.accessor.meta.data.DataItem.StructItem;
import com.mediatek.accessor.operator.IMetaOperator;
import com.mediatek.accessor.operator.MetaOperatorFactory;
import com.mediatek.accessor.packer.PackUtils;
import com.mediatek.accessor.util.Log;
import com.mediatek.accessor.util.TraceHelper;
import com.mediatek.accessor.util.Utils;

import java.util.ArrayList;
import java.util.Map;

/**
 * Stereo config info parser.
 */
public class StereoConfigInfoParser implements IParser {
    private final static String TAG = Log.Tag(StereoConfigInfoParser.class.getSimpleName());

    private static final int SUPPORT_FACE_COUNT = 3;
    private static final String NS_GIMAGE = "http://ns.google.com/photos/1.0/image/";
    private static final String NS_STEREO = "http://ns.mediatek.com/refocus/jpsconfig/";
    private static final String NS_FACE_FIELD = "FD";
    private static final String NS_FOCUS_FIELD = "FOC";
    private static final String PRIFIX_GIMAGE = "GImage";
    private static final String PRIFIX_STEREO = "MRefocus";
    private static final String PRIFIX_FACE = "FD";
    private static final String PRIFIX_FOCUS = "FOC";
    private static final String ATTRIBUTE_JPS_WIDTH = "JpsWidth";
    private static final String ATTRIBUTE_JPS_HEIGHT = "JpsHeight";
    private static final String ATTRIBUTE_MASK_WIDTH = "MaskWidth";
    private static final String ATTRIBUTE_MASK_HEIGHT = "MaskHeight";
    private static final String ATTRIBUTE_POS_X = "PosX";
    private static final String ATTRIBUTE_POS_Y = "PosY";
    private static final String ATTRIBUTE_VIEW_WIDTH = "ViewWidth";
    private static final String ATTRIBUTE_VIEW_HEIGHT = "ViewHeight";
    private static final String ATTRIBUTE_ORIENTATION = "Orientation";
    private static final String ATTRIBUTE_DEPTH_ROTATION = "DepthRotation";
    private static final String ATTRIBUTE_MAIN_CAM_POS = "MainCamPos";
    private static final String ATTRIBUTE_TOUCH_COORDX_1ST = "TouchCoordX1st";
    private static final String ATTRIBUTE_TOUCH_COORDY_1ST = "TouchCoordY1st";
    private static final String ATTRIBUTE_FACE_COUNT = "FaceCount";
    private static final String ATTRIBUTE_FOCUSINFO_STRUCT_NAME = "FocusInfo";
    private static final String ATTRIBUTE_FOCUSINFO_LEFT = "FocusLeft";
    private static final String ATTRIBUTE_FOCUSINFO_TOP = "FocusTop";
    private static final String ATTRIBUTE_FOCUSINFO_RIGHT = "FocusRight";
    private static final String ATTRIBUTE_FOCUSINFO_BOTTOM = "FocusBottom";
    private static final String ATTRIBUTE_FOCUSINFO_TYPE = "FocusType";

    private static final String ATTRIBUTE_DOF_LEVEL = "DOF";
    private static final String ATTRIBUTE_CONV_OFFSET = "ConvOffset";
    private static final String ATTRIBUTE_LDC_WIDTH = "LdcWidth";
    private static final String ATTRIBUTE_LDC_HEIGHT = "LdcHeight";
    private static final String ATTRIBUTE_LDC_BUFFER_IN_APP15 = PackUtils.TYPE_LDC_DATA;
    private static final String ATTRIBUTE_CLEAR_IMAGE_IN_APP15 = PackUtils.TYPE_CLEAR_IMAGE;
    private static final String ATTRIBUTE_LDC_BUFFER_IN_APP1 = "LDC";
    private static final String ATTRIBUTE_CLEAR_IMAGE_IN_APP1 = "Data";
    private static final String ATTRIBUTE_FACE_STRUCT_NAME = "FDInfo";
    private static final String ATTRIBUTE_FACE_LEFT = "FaceLeft";
    private static final String ATTRIBUTE_FACE_TOP = "FaceTop";
    private static final String ATTRIBUTE_FACE_RIGHT = "FaceRight";
    private static final String ATTRIBUTE_FACE_BOTTOM = "FaceBottom";
    private static final String ATTRIBUTE_FACE_RIP = "FaceRip";

    private static final String ATTRIBUTE_FACE_FLAG = "IsFace";
    private static final String ATTRIBUTE_FACE_RATIO = "FaceRatio";
    private static final String ATTRIBUTE_CUR_DAC = "CurDac";
    private static final String ATTRIBUTE_MIN_DAC = "MinDac";
    private static final String ATTRIBUTE_MAX_DAC = "MacDac";

    private IMetaOperator mStandardMetaOperator;
    private IMetaOperator mExtendedMetaOperator;
    private IMetaOperator mCustomizedMetaOperator;
    private DataCollections mStandardDataCollections = new DataCollections();
    private DataCollections mExtendardDataCollections = new DataCollections();
    private DataCollections mCustomizedDataCollections = new DataCollections();
    private ArrayList<SimpleItem> mListOfSimpleValue = new ArrayList<SimpleItem>();
    private ArrayList<BufferItem> mListOfBufferItem = new ArrayList<BufferItem>();
    private ArrayList<StructItem> mListOfStructItem = new ArrayList<StructItem>();
    private ArrayList<BufferItem> mListOfCustDataItem = new ArrayList<BufferItem>();
    private StereoConfigInfo mStereoConfigInfo;

    /**
     * StereoConfigInfoParser Constructor.
     * @param standardBuffer
     *            use standardMeta to get or set standard XMP info value
     * @param extendedBuffer
     *            use extendedMeta to get or set extended XMP info value
     * @param info
     *            StereoConfigInfo struct for set or get stereo config info
     */
    public StereoConfigInfoParser(byte[] standardBuffer, byte[] extendedBuffer,
            Map<String, byte[]> customizedBuffer, StereoConfigInfo info) {
        mStandardDataCollections.dest = DataItem.DEST_TYPE_STANDARD_XMP;
        mExtendardDataCollections.dest = DataItem.DEST_TYPE_EXTENDED_XMP;
        mStereoConfigInfo = info;
        initSimpleValue();
        initBufferItem();
        initStructItem();
        initCustDataItem();

        mStandardDataCollections.listOfSimpleValue = mListOfSimpleValue;
        mStandardDataCollections.listOfStructItem = mListOfStructItem;
        mStandardMetaOperator =
                MetaOperatorFactory.getOperatorInstance(
                        MetaOperatorFactory.XMP_META_OPERATOR, standardBuffer, null);
        try {
            mStandardMetaOperator.setData(mStandardDataCollections);
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }

        mExtendardDataCollections.listOfBufferItem = mListOfBufferItem;
        mExtendedMetaOperator =
                MetaOperatorFactory.getOperatorInstance(
                        MetaOperatorFactory.XMP_META_OPERATOR, extendedBuffer, null);
        try {
            mExtendedMetaOperator.setData(mExtendardDataCollections);
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }

        mCustomizedDataCollections.listOfCustomDataItem = mListOfCustDataItem;
        mCustomizedMetaOperator =
                MetaOperatorFactory.getOperatorInstance(
                        MetaOperatorFactory.CUSTOMIZED_META_OPERATOR, null,
                        customizedBuffer);
        try {
            mCustomizedMetaOperator.setData(mCustomizedDataCollections);
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * StereoConfigInfoParser Constructor.
     * @param standardMetaOperator
     *            use standardMeta to get or set standard XMP info value
     * @param extendedMetaOperator
     *            use extendedMeta to get or set extended XMP info value
     * @param info
     *            StereoConfigInfoParser struct for set or get config info
     */
    public StereoConfigInfoParser(IMetaOperator standardMetaOperator,
            IMetaOperator extendedMetaOperator,
            IMetaOperator customizedMetaOperator, StereoConfigInfo info) {
        mStandardDataCollections.dest = DataItem.DEST_TYPE_STANDARD_XMP;
        mExtendardDataCollections.dest = DataItem.DEST_TYPE_EXTENDED_XMP;
        mStereoConfigInfo = info;
        initSimpleValue();
        initBufferItem();
        initStructItem();
        initCustDataItem();
        mStandardDataCollections.listOfSimpleValue = mListOfSimpleValue;
        mStandardDataCollections.listOfStructItem = mListOfStructItem;
        mStandardMetaOperator = standardMetaOperator;
        try {
            mStandardMetaOperator.setData(mStandardDataCollections);
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }

        mExtendardDataCollections.listOfBufferItem = mListOfBufferItem;
        mExtendedMetaOperator = extendedMetaOperator;
        try {
            mExtendedMetaOperator.setData(mExtendardDataCollections);
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }

        mCustomizedDataCollections.listOfCustomDataItem = mListOfCustDataItem;
        mCustomizedMetaOperator = customizedMetaOperator;
        try {
            mCustomizedMetaOperator.setData(mCustomizedDataCollections);
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void read() {
        TraceHelper.beginSection(">>>>StereoConfigInfoParser-read");
        Log.d(TAG, "<read>");
        if (mStandardMetaOperator != null) {
            mStandardMetaOperator.read();
        }
        if (mCustomizedMetaOperator != null) {
            mCustomizedMetaOperator.read();
        }
        if (mStereoConfigInfo == null) {
            Log.d(TAG, "<read> mStereoConfigInfo is null!");
            TraceHelper.endSection();
            return;
        }
        readSimpleValue();
        readStructItem();
        readCustDataItem();
        if ((mStereoConfigInfo.clearImage == null || mStereoConfigInfo.ldcBuffer == null)
                && mExtendedMetaOperator != null) {
            Log.d(Utils.getGDepthTag(), "read clear image from APP1");
            mExtendedMetaOperator.read();
            readBufferItem();
        }
        Log.d(TAG, "<read> " + mStereoConfigInfo);
        dumpValuesAndBuffers("read");
        TraceHelper.endSection();
    }

    @Override
    public void write() {
        TraceHelper.beginSection(">>>>StereoConfigInfoParser-write");
        Log.d(TAG, "<write>");
        if (mStereoConfigInfo == null) {
            Log.d(TAG, "<write> mStereoConfigInfo is null!");
            TraceHelper.endSection();
            return;
        }
        dumpValuesAndBuffers("write");
        writeSimpleValue();
        writeStructItem();
        writeCustDataItem();
        if (mStandardMetaOperator != null) {
            mStandardMetaOperator.write();
        }
        if (mCustomizedMetaOperator != null) {
            mCustomizedMetaOperator.write();
        }
        TraceHelper.endSection();
    }

    @Override
    public SerializedInfo serialize() {
        TraceHelper.beginSection(">>>>StereoConfigInfoParser-serialize");
        Log.d(TAG, "<serialize>");
        SerializedInfo info = new SerializedInfo();
        if (mStandardMetaOperator != null) {
            Map<String, byte[]> standardData = mStandardMetaOperator.serialize();
            info.standardXmpBuf = standardData.get(SerializedInfo.XMP_KEY);
        }
        if (mCustomizedMetaOperator != null) {
            Map<String, byte[]> customizedData = mCustomizedMetaOperator.serialize();
            info.customizedBufMap = customizedData;
        }
        TraceHelper.endSection();
        return info;
    }

    private void initSimpleValue() {
        SimpleItem simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_JPS_WIDTH;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_JPS_HEIGHT;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_MASK_WIDTH;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_MASK_HEIGHT;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_POS_X;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_POS_Y;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_VIEW_WIDTH;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_VIEW_HEIGHT;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_ORIENTATION;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_DEPTH_ROTATION;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_MAIN_CAM_POS;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_TOUCH_COORDX_1ST;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_TOUCH_COORDY_1ST;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_FACE_COUNT;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_DOF_LEVEL;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_LDC_WIDTH;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_LDC_HEIGHT;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_FACE_FLAG;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_FACE_RATIO;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_CUR_DAC;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_MIN_DAC;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_MAX_DAC;
        mListOfSimpleValue.add(simpleValue);

        simpleValue = getSimpleValueInstance();
        simpleValue.name = ATTRIBUTE_CONV_OFFSET;
        mListOfSimpleValue.add(simpleValue);
    }

    private void initBufferItem() {
        BufferItem bufferItem = getBufferItem();
        bufferItem.nameSpaceItem.nameSpace = NS_STEREO;
        bufferItem.nameSpaceItem.nameSpacePrifix = PRIFIX_STEREO;
        bufferItem.name = ATTRIBUTE_LDC_BUFFER_IN_APP1;
        mListOfBufferItem.add(bufferItem);

        bufferItem = getBufferItem();
        bufferItem.nameSpaceItem.nameSpace = NS_GIMAGE;
        bufferItem.nameSpaceItem.nameSpacePrifix = PRIFIX_GIMAGE;
        bufferItem.name = ATTRIBUTE_CLEAR_IMAGE_IN_APP1;
        mListOfBufferItem.add(bufferItem);
    }

    private void initStructItem() {
        StructItem structItem = null;
        for (int i = 0; i < SUPPORT_FACE_COUNT; i++) {
            structItem = getStructItemInstance(NS_FACE_FIELD, PRIFIX_FACE);
            structItem.structName = ATTRIBUTE_FACE_STRUCT_NAME + i;
            structItem.fieldName = ATTRIBUTE_FACE_LEFT;
            mListOfStructItem.add(structItem);

            structItem = getStructItemInstance(NS_FACE_FIELD, PRIFIX_FACE);
            structItem.structName = ATTRIBUTE_FACE_STRUCT_NAME + i;
            structItem.fieldName = ATTRIBUTE_FACE_TOP;
            mListOfStructItem.add(structItem);

            structItem = getStructItemInstance(NS_FACE_FIELD, PRIFIX_FACE);
            structItem.structName = ATTRIBUTE_FACE_STRUCT_NAME + i;
            structItem.fieldName = ATTRIBUTE_FACE_RIGHT;
            mListOfStructItem.add(structItem);

            structItem = getStructItemInstance(NS_FACE_FIELD, PRIFIX_FACE);
            structItem.structName = ATTRIBUTE_FACE_STRUCT_NAME + i;
            structItem.fieldName = ATTRIBUTE_FACE_BOTTOM;
            mListOfStructItem.add(structItem);

            structItem = getStructItemInstance(NS_FACE_FIELD, PRIFIX_FACE);
            structItem.structName = ATTRIBUTE_FACE_STRUCT_NAME + i;
            structItem.fieldName = ATTRIBUTE_FACE_RIP;
            mListOfStructItem.add(structItem);
        }

        structItem = getStructItemInstance(NS_FOCUS_FIELD, PRIFIX_FOCUS);
        structItem.structName = ATTRIBUTE_FOCUSINFO_STRUCT_NAME;
        structItem.fieldName = ATTRIBUTE_FOCUSINFO_LEFT;
        mListOfStructItem.add(structItem);

        structItem = getStructItemInstance(NS_FOCUS_FIELD, PRIFIX_FOCUS);
        structItem.structName = ATTRIBUTE_FOCUSINFO_STRUCT_NAME;
        structItem.fieldName = ATTRIBUTE_FOCUSINFO_TOP;
        mListOfStructItem.add(structItem);

        structItem = getStructItemInstance(NS_FOCUS_FIELD, PRIFIX_FOCUS);
        structItem.structName = ATTRIBUTE_FOCUSINFO_STRUCT_NAME;
        structItem.fieldName = ATTRIBUTE_FOCUSINFO_RIGHT;
        mListOfStructItem.add(structItem);

        structItem = getStructItemInstance(NS_FOCUS_FIELD, PRIFIX_FOCUS);
        structItem.structName = ATTRIBUTE_FOCUSINFO_STRUCT_NAME;
        structItem.fieldName = ATTRIBUTE_FOCUSINFO_BOTTOM;
        mListOfStructItem.add(structItem);

        structItem = getStructItemInstance(NS_FOCUS_FIELD, PRIFIX_FOCUS);
        structItem.structName = ATTRIBUTE_FOCUSINFO_STRUCT_NAME;
        structItem.fieldName = ATTRIBUTE_FOCUSINFO_TYPE;
        mListOfStructItem.add(structItem);
    }

    private void initCustDataItem() {
        BufferItem custDataItem = new BufferItem();
        custDataItem.name = ATTRIBUTE_LDC_BUFFER_IN_APP15;
        mListOfCustDataItem.add(custDataItem);

        custDataItem = new BufferItem();
        custDataItem.name = ATTRIBUTE_CLEAR_IMAGE_IN_APP15;
        mListOfCustDataItem.add(custDataItem);
    }

    private void readSimpleValue() {
        SimpleItem simpleValue = null;
        int simpleValueItemCount = mListOfSimpleValue.size();
        for (int i = 0; i < simpleValueItemCount; i++) {
            simpleValue = mListOfSimpleValue.get(i);
            if (simpleValue == null || simpleValue.value == null
                    || simpleValue.value.length() == 0) {
                continue;
            }
            if (ATTRIBUTE_JPS_WIDTH.equals(simpleValue.name)) {
                mStereoConfigInfo.jpsWidth = Integer.parseInt(simpleValue.value);
            } else if (ATTRIBUTE_JPS_HEIGHT.equals(simpleValue.name)) {
                mStereoConfigInfo.jpsHeight = Integer.parseInt(simpleValue.value);
            } else if (ATTRIBUTE_MASK_WIDTH.equals(simpleValue.name)) {
                mStereoConfigInfo.maskWidth = Integer.parseInt(simpleValue.value);
            } else if (ATTRIBUTE_MASK_HEIGHT.equals(simpleValue.name)) {
                mStereoConfigInfo.maskHeight = Integer.parseInt(simpleValue.value);
            } else if (ATTRIBUTE_POS_X.equals(simpleValue.name)) {
                mStereoConfigInfo.posX = Integer.parseInt(simpleValue.value);
            } else if (ATTRIBUTE_POS_Y.equals(simpleValue.name)) {
                mStereoConfigInfo.posY = Integer.parseInt(simpleValue.value);
            } else if (ATTRIBUTE_VIEW_WIDTH.equals(simpleValue.name)) {
                mStereoConfigInfo.viewWidth = Integer.parseInt(simpleValue.value);
            } else if (ATTRIBUTE_VIEW_HEIGHT.equals(simpleValue.name)) {
                mStereoConfigInfo.viewHeight = Integer.parseInt(simpleValue.value);
            } else if (ATTRIBUTE_ORIENTATION.equals(simpleValue.name)) {
                mStereoConfigInfo.imageOrientation = Integer.parseInt(simpleValue.value);
            } else if (ATTRIBUTE_DEPTH_ROTATION.equals(simpleValue.name)) {
                mStereoConfigInfo.depthOrientation = Integer.parseInt(simpleValue.value);
            } else if (ATTRIBUTE_MAIN_CAM_POS.equals(simpleValue.name)) {
                mStereoConfigInfo.mainCamPos = Integer.parseInt(simpleValue.value);
            } else if (ATTRIBUTE_TOUCH_COORDX_1ST.equals(simpleValue.name)) {
                mStereoConfigInfo.touchCoordX1st = Integer.parseInt(simpleValue.value);
            } else if (ATTRIBUTE_TOUCH_COORDY_1ST.equals(simpleValue.name)) {
                mStereoConfigInfo.touchCoordY1st = Integer.parseInt(simpleValue.value);
            } else if (ATTRIBUTE_FACE_COUNT.equals(simpleValue.name)) {
                mStereoConfigInfo.faceCount = Integer.parseInt(simpleValue.value);
            } else if (ATTRIBUTE_DOF_LEVEL.equals(simpleValue.name)) {
                mStereoConfigInfo.dofLevel = Integer.parseInt(simpleValue.value);
            } else if (ATTRIBUTE_CONV_OFFSET.equals(simpleValue.name)) {
                mStereoConfigInfo.convOffset = Float.valueOf(simpleValue.value);
            } else if (ATTRIBUTE_LDC_WIDTH.equals(simpleValue.name)) {
                mStereoConfigInfo.ldcWidth = Integer.parseInt(simpleValue.value);
            } else if (ATTRIBUTE_LDC_HEIGHT.equals(simpleValue.name)) {
                mStereoConfigInfo.ldcHeight = Integer.parseInt(simpleValue.value);
            } else if (ATTRIBUTE_FACE_FLAG.equals(simpleValue.name)) {
                mStereoConfigInfo.isFace = Boolean.valueOf(simpleValue.value);
            } else if (ATTRIBUTE_FACE_RATIO.equals(simpleValue.name)) {
                mStereoConfigInfo.faceRatio = Float.valueOf(simpleValue.value);
            } else if (ATTRIBUTE_CUR_DAC.equals(simpleValue.name)) {
                mStereoConfigInfo.curDac = Integer.parseInt(simpleValue.value);
            } else if (ATTRIBUTE_MIN_DAC.equals(simpleValue.name)) {
                mStereoConfigInfo.minDac = Integer.parseInt(simpleValue.value);
            } else if (ATTRIBUTE_MAX_DAC.equals(simpleValue.name)) {
                mStereoConfigInfo.maxDac = Integer.parseInt(simpleValue.value);
            }
        }
    }

    private void readBufferItem() {
        Log.d(TAG, "<readBufferItem>");
        BufferItem bufferItem = null;
        int bufferItemCount = mListOfBufferItem.size();
        for (int i = 0; i < bufferItemCount; i++) {
            bufferItem = mListOfBufferItem.get(i);
            if (bufferItem == null || bufferItem.value == null) {
                continue;
            }
            if (ATTRIBUTE_LDC_BUFFER_IN_APP1.equals(bufferItem.name)
                    && mStereoConfigInfo.ldcBuffer == null) {
                mStereoConfigInfo.ldcBuffer = bufferItem.value;
                Log.d(TAG, "<readBufferItem> ldcBuffer get value from APP1.");
            } else if (ATTRIBUTE_CLEAR_IMAGE_IN_APP1.equals(bufferItem.name)
                    && mStereoConfigInfo.clearImage == null) {
                mStereoConfigInfo.clearImage = bufferItem.value;
                Log.d(TAG, "<readBufferItem> clearImage get value from APP1.");
            }
        }
    }

    private void readStructItem() {
        StructItem structItem = null;
        mStereoConfigInfo.fdInfoArray = new ArrayList<FaceDetectionInfo>();
        for (int i = 0; i < SUPPORT_FACE_COUNT; i++) {
            int structItemCount = mListOfStructItem.size();
            mStereoConfigInfo.fdInfoArray.add(i, new FaceDetectionInfo());
            for (int j = 0; j < structItemCount; j++) {
                structItem = mListOfStructItem.get(j);
                if (structItem == null || structItem.fieldName == null
                        || structItem.fieldValue == null
                        || structItem.fieldValue.length() == 0) {
                    continue;
                }
                if ((ATTRIBUTE_FACE_STRUCT_NAME + i)
                        .equals(structItem.structName)) {
                    if (ATTRIBUTE_FACE_LEFT.equals(structItem.fieldName)) {
                        mStereoConfigInfo.fdInfoArray.get(i).faceLeft =
                                Integer.parseInt(structItem.fieldValue);
                    } else if (ATTRIBUTE_FACE_TOP.equals(structItem.fieldName)) {
                        mStereoConfigInfo.fdInfoArray.get(i).faceTop =
                                Integer.parseInt(structItem.fieldValue);
                    } else if (ATTRIBUTE_FACE_RIGHT.equals(structItem.fieldName)) {
                        mStereoConfigInfo.fdInfoArray.get(i).faceRight =
                                Integer.parseInt(structItem.fieldValue);
                    } else if (ATTRIBUTE_FACE_BOTTOM
                            .equals(structItem.fieldName)) {
                        mStereoConfigInfo.fdInfoArray.get(i).faceBottom =
                                Integer.parseInt(structItem.fieldValue);
                    } else if (ATTRIBUTE_FACE_RIP.equals(structItem.fieldName)) {
                        mStereoConfigInfo.fdInfoArray.get(i).faceRip =
                                Integer.parseInt(structItem.fieldValue);
                    }
                }
            }
        }
        mStereoConfigInfo.focusInfo = new FocusInfo();
        int structItemCount = mListOfStructItem.size();
        for (int j = 0; j < structItemCount; j++) {
            structItem = mListOfStructItem.get(j);
            if (structItem == null || structItem.fieldName == null
                    || structItem.fieldValue == null
                    || structItem.fieldValue.length() == 0) {
                continue;
            }
            if (ATTRIBUTE_FOCUSINFO_STRUCT_NAME
                    .equals(structItem.structName)) {
                if (ATTRIBUTE_FOCUSINFO_LEFT.equals(structItem.fieldName)) {
                    mStereoConfigInfo.focusInfo.focusLeft =
                            Integer.parseInt(structItem.fieldValue);
                } else if (ATTRIBUTE_FOCUSINFO_TOP.equals(structItem.fieldName)) {
                    mStereoConfigInfo.focusInfo.focusTop =
                            Integer.parseInt(structItem.fieldValue);
                } else if (ATTRIBUTE_FOCUSINFO_RIGHT.equals(structItem.fieldName)) {
                    mStereoConfigInfo.focusInfo.focusRight =
                            Integer.parseInt(structItem.fieldValue);
                } else if (ATTRIBUTE_FOCUSINFO_BOTTOM
                        .equals(structItem.fieldName)) {
                    mStereoConfigInfo.focusInfo.focusBottom =
                            Integer.parseInt(structItem.fieldValue);
                } else if (ATTRIBUTE_FOCUSINFO_TYPE.equals(structItem.fieldName)) {
                    mStereoConfigInfo.focusInfo.focusType =
                            Integer.parseInt(structItem.fieldValue);
                }
            }
        }
    }

    private void readCustDataItem() {
        BufferItem custDataItem = null;
        int custDataItemCount = mListOfCustDataItem.size();
        for (int i = 0; i < custDataItemCount; i++) {
            custDataItem = mListOfCustDataItem.get(i);
            if (custDataItem == null || custDataItem.value == null) {
                continue;
            }
            if (ATTRIBUTE_CLEAR_IMAGE_IN_APP15.equals(custDataItem.name)) {
                mStereoConfigInfo.clearImage = custDataItem.value;
            }
            if (ATTRIBUTE_LDC_BUFFER_IN_APP15.equals(custDataItem.name)) {
                mStereoConfigInfo.ldcBuffer = custDataItem.value;
            }
        }
    }

    private void writeSimpleValue() {
        int simpleValueItemCount = mListOfSimpleValue.size();
        for (int i = 0; i < simpleValueItemCount; i++) {
            if (mListOfSimpleValue.get(i) == null) {
                Log.d(TAG, "mListOfSimpleValue.get(i) is null!");
                continue;
            }
            if (ATTRIBUTE_JPS_WIDTH.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mStereoConfigInfo.jpsWidth);
            } else if (ATTRIBUTE_JPS_HEIGHT.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mStereoConfigInfo.jpsHeight);
            } else if (ATTRIBUTE_MASK_WIDTH.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mStereoConfigInfo.maskWidth);
            } else if (ATTRIBUTE_MASK_HEIGHT.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mStereoConfigInfo.maskHeight);
            } else if (ATTRIBUTE_POS_X.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value = String.valueOf(mStereoConfigInfo.posX);
            } else if (ATTRIBUTE_POS_Y.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value = String.valueOf(mStereoConfigInfo.posY);
            } else if (ATTRIBUTE_VIEW_WIDTH.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mStereoConfigInfo.viewWidth);
            } else if (ATTRIBUTE_VIEW_HEIGHT.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mStereoConfigInfo.viewHeight);
            } else if (ATTRIBUTE_ORIENTATION.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mStereoConfigInfo.imageOrientation);
            } else if (ATTRIBUTE_DEPTH_ROTATION.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mStereoConfigInfo.depthOrientation);
            } else if (ATTRIBUTE_MAIN_CAM_POS.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mStereoConfigInfo.mainCamPos);
            } else if (ATTRIBUTE_TOUCH_COORDX_1ST.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mStereoConfigInfo.touchCoordX1st);
                Log.d(TAG, "touchCoordX1st.value " + mListOfSimpleValue.get(i).value);
            } else if (ATTRIBUTE_TOUCH_COORDY_1ST.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mStereoConfigInfo.touchCoordY1st);
                Log.d(TAG, "touchCoordY1st.value " + mListOfSimpleValue.get(i).value);
            } else if (ATTRIBUTE_FACE_COUNT.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(Math.min(mStereoConfigInfo.faceCount,
                                SUPPORT_FACE_COUNT));
            } else if (ATTRIBUTE_DOF_LEVEL.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mStereoConfigInfo.dofLevel);
            } else if (ATTRIBUTE_CONV_OFFSET.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mStereoConfigInfo.convOffset);
            } else if (ATTRIBUTE_LDC_WIDTH.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mStereoConfigInfo.ldcWidth);
            } else if (ATTRIBUTE_LDC_HEIGHT.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mStereoConfigInfo.ldcHeight);
            } else if (ATTRIBUTE_FACE_FLAG.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mStereoConfigInfo.isFace);
            } else if (ATTRIBUTE_FACE_RATIO.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mStereoConfigInfo.faceRatio);
            } else if (ATTRIBUTE_CUR_DAC.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mStereoConfigInfo.curDac);
            } else if (ATTRIBUTE_MIN_DAC.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mStereoConfigInfo.minDac);
            } else if (ATTRIBUTE_MAX_DAC.equals(mListOfSimpleValue.get(i).name)) {
                mListOfSimpleValue.get(i).value =
                        String.valueOf(mStereoConfigInfo.maxDac);
            }
        }
        mStandardDataCollections.listOfSimpleValue = mListOfSimpleValue;
    }

    private void writeStructItem() {
        if (mStereoConfigInfo.fdInfoArray != null) {
            int fdInfoCount = Math.min(mStereoConfigInfo.fdInfoArray.size(), SUPPORT_FACE_COUNT);
            for (int i = 0; i < fdInfoCount; i++) {
                int structItemCount = mListOfStructItem.size();
                for (int j = 0; j < structItemCount; j++) {
                    if (mListOfStructItem.get(j) == null) {
                        Log.d(TAG, "mListOfStructItem.get(j) is null!");
                        continue;
                    }
                    if (mStereoConfigInfo.fdInfoArray.get(i) == null) {
                        Log.d(TAG, "mStereoConfigInfo.fdInfoArray.get(i) is null!");
                        continue;
                    }
                    if ((ATTRIBUTE_FACE_STRUCT_NAME + i)
                            .equals(mListOfStructItem.get(j).structName)) {
                        if (ATTRIBUTE_FACE_LEFT.equals(mListOfStructItem.get(j).fieldName)) {
                            mListOfStructItem.get(j).fieldValue =
                                    String.valueOf(mStereoConfigInfo.fdInfoArray.get(i).faceLeft);
                        } else if (ATTRIBUTE_FACE_TOP
                                .equals(mListOfStructItem.get(j).fieldName)) {
                            mListOfStructItem.get(j).fieldValue =
                                    String.valueOf(mStereoConfigInfo.fdInfoArray.get(i).faceTop);
                        } else if (ATTRIBUTE_FACE_RIGHT
                                .equals(mListOfStructItem.get(j).fieldName)) {
                            mListOfStructItem.get(j).fieldValue =
                                    String.valueOf(mStereoConfigInfo.fdInfoArray.get(i).faceRight);
                        } else if (ATTRIBUTE_FACE_BOTTOM
                                .equals(mListOfStructItem.get(j).fieldName)) {
                            mListOfStructItem.get(j).fieldValue =
                                    String.valueOf(mStereoConfigInfo.fdInfoArray.get(i).faceBottom);
                        } else if (ATTRIBUTE_FACE_RIP
                                .equals(mListOfStructItem.get(j).fieldName)) {
                            mListOfStructItem.get(j).fieldValue =
                                    String.valueOf(mStereoConfigInfo.fdInfoArray.get(i).faceRip);
                        }
                    }
                }
            }
        }
        if (mStereoConfigInfo.focusInfo != null) {
            int structItemCount = mListOfStructItem.size();
            for (int j = 0; j < structItemCount; j++) {
                if (mListOfStructItem.get(j) == null) {
                    Log.d(TAG, "mListOfStructItem.get(j) is null!");
                    continue;
                }
                if (ATTRIBUTE_FOCUSINFO_STRUCT_NAME
                        .equals(mListOfStructItem.get(j).structName)) {
                    if (ATTRIBUTE_FOCUSINFO_LEFT.equals(mListOfStructItem.get(j).fieldName)) {
                        mListOfStructItem.get(j).fieldValue =
                                String.valueOf(mStereoConfigInfo.focusInfo.focusLeft);
                    } else if (ATTRIBUTE_FOCUSINFO_TOP
                            .equals(mListOfStructItem.get(j).fieldName)) {
                        mListOfStructItem.get(j).fieldValue =
                                String.valueOf(mStereoConfigInfo.focusInfo.focusTop);
                    } else if (ATTRIBUTE_FOCUSINFO_RIGHT
                            .equals(mListOfStructItem.get(j).fieldName)) {
                        mListOfStructItem.get(j).fieldValue =
                                String.valueOf(mStereoConfigInfo.focusInfo.focusRight);
                    } else if (ATTRIBUTE_FOCUSINFO_BOTTOM
                            .equals(mListOfStructItem.get(j).fieldName)) {
                        mListOfStructItem.get(j).fieldValue =
                                String.valueOf(mStereoConfigInfo.focusInfo.focusBottom);
                    } else if (ATTRIBUTE_FOCUSINFO_TYPE
                            .equals(mListOfStructItem.get(j).fieldName)) {
                        mListOfStructItem.get(j).fieldValue =
                                String.valueOf(mStereoConfigInfo.focusInfo.focusType);
                    }
                }
            }
        }
        mStandardDataCollections.listOfStructItem = mListOfStructItem;
    }

    private void writeCustDataItem() {
        int custDataItemCount = mListOfCustDataItem.size();
        for (int i = 0; i < custDataItemCount; i++) {
            if (mListOfCustDataItem.get(i) == null) {
                continue;
            }
            if (ATTRIBUTE_LDC_BUFFER_IN_APP15.equals(mListOfCustDataItem.get(i).name)
                    && null != mStereoConfigInfo.ldcBuffer) {
                mListOfCustDataItem.get(i).value = mStereoConfigInfo.ldcBuffer;
            } else if (ATTRIBUTE_CLEAR_IMAGE_IN_APP15.equals(mListOfCustDataItem.get(i).name)
                    && null != mStereoConfigInfo.clearImage) {
                mListOfCustDataItem.get(i).value = mStereoConfigInfo.clearImage;
            }
        }
        mCustomizedDataCollections.listOfCustomDataItem = mListOfCustDataItem;
    }

    private SimpleItem getSimpleValueInstance() {
        SimpleItem simpleValue = new SimpleItem();
        simpleValue.dest = DataItem.DEST_TYPE_STANDARD_XMP;
        simpleValue.nameSpaceItem = new NameSpaceItem();
        simpleValue.nameSpaceItem.dest = DataItem.DEST_TYPE_STANDARD_XMP;
        simpleValue.nameSpaceItem.nameSpace = NS_STEREO;
        simpleValue.nameSpaceItem.nameSpacePrifix = PRIFIX_STEREO;
        return simpleValue;
    }

    private StructItem getStructItemInstance(String fieldNS, String fieldPrefix) {
        StructItem structItem = new StructItem();
        structItem.dest = DataItem.DEST_TYPE_STANDARD_XMP;
        structItem.structNameSpaceItem = new NameSpaceItem();
        structItem.structNameSpaceItem.nameSpace = NS_STEREO;
        structItem.structNameSpaceItem.nameSpacePrifix = PRIFIX_STEREO;
        structItem.fieldNameSpaceItem = new NameSpaceItem();
        structItem.fieldNameSpaceItem.nameSpace = fieldNS;
        structItem.fieldNameSpaceItem.nameSpacePrifix = fieldPrefix;
        return structItem;
    }

    private BufferItem getBufferItem() {
        BufferItem bufferItem = new BufferItem();
        bufferItem.dest = DataItem.DEST_TYPE_EXTENDED_XMP;
        bufferItem.nameSpaceItem = new NameSpaceItem();
        bufferItem.nameSpaceItem.dest = DataItem.DEST_TYPE_EXTENDED_XMP;
        return bufferItem;
    }

    private void dumpValuesAndBuffers(String suffix) {
        if (!Utils.ENABLE_BUFFER_DUMP) {
            return;
        }
        String dumpPath = Utils.DUMP_FILE_FOLDER + "/" + mStereoConfigInfo.debugDir + "/";
        if (mStereoConfigInfo.clearImage != null) {
            Utils.writeBufferToFile(dumpPath + "StereoConfigInfo_clearImage_" + suffix + ".raw",
                    mStereoConfigInfo.clearImage);
        }  else {
            Log.d(TAG, "<dumpValuesAndBuffers> clearImage is null!");
        }
        if (mStereoConfigInfo.ldcBuffer != null) {
            Utils.writeBufferToFile(dumpPath + "StereoConfigInfo_ldcBuffer_" + suffix + ".raw",
                    mStereoConfigInfo.ldcBuffer);
        } else {
            Log.d(TAG, "<dumpValuesAndBuffers> ldcBuffer is null!");
        }
        Utils.writeStringToFile(dumpPath + "StereoConfigInfo_" + suffix + ".txt",
                mStereoConfigInfo.toString());
    }
}
