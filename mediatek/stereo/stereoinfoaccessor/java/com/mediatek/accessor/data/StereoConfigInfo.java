package com.mediatek.accessor.data;

import java.util.ArrayList;

/**
 * Stereo config information.
 */
public class StereoConfigInfo {
    public String debugDir;
    public int jpsWidth;
    public int jpsHeight;
    public int maskWidth;
    public int maskHeight;
    public int posX;
    public int posY;
    public int viewWidth;
    public int viewHeight;
    public int imageOrientation;
    public int depthOrientation;
    public int mainCamPos;
    public int touchCoordX1st;
    public int touchCoordY1st;
    public int faceCount;
    public FocusInfo focusInfo;
    public ArrayList<FaceDetectionInfo> fdInfoArray;
    public int dofLevel;
    public float convOffset;
    public int ldcWidth;
    public int ldcHeight;
    public byte[] ldcBuffer;
    public byte[] clearImage;

    // FocusInfo
    public boolean isFace;
    public float faceRatio;
    public int curDac;
    public int minDac;
    public int maxDac;

    /**
     * Face detection information.
     */
    public static class FaceDetectionInfo {
        public int faceLeft;
        public int faceTop;
        public int faceRight;
        public int faceBottom;
        public int faceRip;

        /**
         * Default constructor.
         */
        public FaceDetectionInfo() {
        }

        /**
         * Construct FaceDetectionInfo with params.
         * @param left left
         * @param top top
         * @param right right
         * @param bottom bottom
         * @param rip rip
         */
        public FaceDetectionInfo(int left, int top, int right, int bottom, int rip) {
            faceLeft = left;
            faceTop = top;
            faceRight = right;
            faceBottom = bottom;
            faceRip = rip;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("FaceDetectionInfo:");
            sb.append("\n    faceLeft = 0x" + Integer.toHexString(faceLeft) + "(" + faceLeft
                    + ")");
            sb.append("\n    faceTop = 0x" + Integer.toHexString(faceTop) + "(" + faceTop
                    + ")");
            sb.append("\n    faceRight = 0x" + Integer.toHexString(faceRight) + "("
                    + faceRight + ")");
            sb.append("\n    faceBottom = 0x" + Integer.toHexString(faceBottom) + "("
                    + faceBottom + ")");
            sb.append("\n    faceRip = 0x" + Integer.toHexString(faceRip) + "(" + faceRip
                    + ")");
            return sb.toString();
        }
    }

    /**
     * Face detection information.
     */
    public static class FocusInfo {
        public int focusType;
        public int focusTop;
        public int focusLeft;
        public int focusRight;
        public int focusBottom;

        /**
         * Default constructor.
         */
        public FocusInfo() {
        }

        /**
         * Construct FocusInfo with params.
         * @param type type
         * @param left left
         * @param top top
         * @param right right
         * @param bottom bottom
         */
        public FocusInfo(int type, int left, int top, int right, int bottom) {
            focusLeft = left;
            focusTop = top;
            focusRight = right;
            focusBottom = bottom;
            focusType = type;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("FocusInfo:");
            sb.append("(left,top,right,bottom|type): ");
            sb.append("(" + focusLeft + "," + focusTop + "," + focusRight + "," + focusBottom
                    + "|" + focusType + ")");
            return sb.toString();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("StereoConfigInfo:");
        sb.append("\n    jpsWidth  = 0x" + Integer.toHexString(jpsWidth) + "(" + jpsWidth
                + ")");
        sb.append("\n    jpsHeight = 0x" + Integer.toHexString(jpsHeight) + "(" + jpsHeight
                + ")");
        sb.append("\n    maskWidth = 0x" + Integer.toHexString(maskWidth) + "(" + maskWidth
                + ")");
        sb.append("\n    maskHeight = 0x" + Integer.toHexString(maskHeight) + "(" + maskHeight
                + ")");
        sb.append("\n    posX = 0x" + Integer.toHexString(posX) + "(" + posX + ")");
        sb.append("\n    posY = 0x" + Integer.toHexString(posY) + "(" + posY + ")");
        sb.append("\n    viewWidth = 0x" + Integer.toHexString(viewWidth) + "(" + viewWidth
                + ")");
        sb.append("\n    viewHeight = 0x" + Integer.toHexString(viewHeight) + "(" + viewHeight
                + ")");
        sb.append("\n    imageOrientation = 0x" + Integer.toHexString(imageOrientation) + "("
                + imageOrientation + ")");
        sb.append("\n    depthOrientation = 0x" + Integer.toHexString(depthOrientation) + "("
                + depthOrientation + ")");
        sb.append("\n    mainCamPos = 0x" + Integer.toHexString(mainCamPos) + "(" + mainCamPos
                + ")");
        sb.append("\n    touchCoordX1st = 0x" + Integer.toHexString(touchCoordX1st) + "("
                + touchCoordX1st + ")");
        sb.append("\n    touchCoordY1st = 0x" + Integer.toHexString(touchCoordY1st) + "("
                + touchCoordY1st + ")");
        sb.append("\n    faceCount = 0x" + Integer.toHexString(faceCount) + "(" + faceCount
                + ")");
        if (focusInfo != null) {
            sb.append("\n    " + focusInfo.toString());
        } else {
            sb.append("\n    focusInfo = null");
        }
        sb.append("\n    dofLevel = 0x" + Integer.toHexString(dofLevel) + "(" + dofLevel + ")");
        sb.append("\n    convOffset = " + convOffset);
        sb.append("\n    ldcWidth = 0x" + Integer.toHexString(ldcWidth) + "(" + ldcWidth + ")");
        sb.append("\n    ldcHeight = 0x" + Integer.toHexString(ldcHeight) + "(" + ldcHeight
                + ")");
        if (ldcBuffer != null) {
            sb.append("\n    ldcBuffer length = 0x" + Integer.toHexString(ldcBuffer.length)
                    + "(" + ldcBuffer.length + ")");
        } else {
            sb.append("\n    ldcBuffer = null");
        }
        if (clearImage != null) {
            sb.append("\n    clearImage length = 0x" + Integer.toHexString(clearImage.length)
                    + "(" + clearImage.length + ")");
        } else {
            sb.append("\n    clearImage = null");
        }
        // focus info
        sb.append("\n    isFace = " + isFace);
        sb.append("\n    faceRatio = " + faceRatio);
        sb.append("\n    curDac = " + curDac);
        sb.append("\n    minDac = " + minDac);
        sb.append("\n    maxDac = " + maxDac);

        if (fdInfoArray != null) {
            int fdInfoCount = fdInfoArray.size();
            for (int i = 0; i < fdInfoCount; i++) {
                if (fdInfoArray.get(i) != null) {
                    sb.append("\n    fdInfoArray[" + i + "] = "
                            + fdInfoArray.get(i).toString());
                } else {
                    sb.append("\n    fdInfoArray[" + i + "] = null");
                }
            }
        } else {
            sb.append("\n    fdInfoArray = null");
        }
        return sb.toString();
    }
}
