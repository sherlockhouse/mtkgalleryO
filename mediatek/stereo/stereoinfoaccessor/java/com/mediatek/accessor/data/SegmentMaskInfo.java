package com.mediatek.accessor.data;

/**
 * Segment mask information.
 */
public class SegmentMaskInfo {
    public String debugDir;
    public int maskWidth;
    public int maskHeight;
    public int segmentX;
    public int segmentY;
    public int segmentLeft;
    public int segmentTop;
    public int segmentRight;
    public int segmentBottom;
    public byte[] maskBuffer;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SegmentMaskInfo:");
        sb.append("\n    maskWidth  = 0x" + Integer.toHexString(maskWidth) + "(" + maskWidth
                + ")");
        sb.append("\n    maskHeight = 0x" + Integer.toHexString(maskHeight) + "(" + maskHeight
                + ")");
        sb.append("\n    segmentX = 0x" + Integer.toHexString(segmentX) + "(" + segmentX + ")");
        sb.append("\n    segmentY = 0x" + Integer.toHexString(segmentY) + "(" + segmentY + ")");
        sb.append("\n    segmentLeft = 0x" + Integer.toHexString(segmentLeft) + "("
                + segmentLeft + ")");
        sb.append("\n    segmentTop = 0x" + Integer.toHexString(segmentTop) + "(" + segmentTop
                + ")");
        sb.append("\n    segmentRight = 0x" + Integer.toHexString(segmentRight) + "("
                + segmentRight + ")");
        sb.append("\n    segmentBottom = 0x" + Integer.toHexString(segmentBottom) + "("
                + segmentBottom + ")");
        if (maskBuffer != null) {
            sb.append("\n    maskBuffer length = 0x" + Integer.toHexString(maskBuffer.length)
                    + "(" + maskBuffer.length + ")");
        } else {
            sb.append("\n    maskBuffer = null");
        }
        return sb.toString();
    }
}
