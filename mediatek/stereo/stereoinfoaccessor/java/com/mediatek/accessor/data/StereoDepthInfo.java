package com.mediatek.accessor.data;

/**
 * Stereo depth information.
 */
public class StereoDepthInfo {
    public String debugDir;
    public int metaBufferWidth;
    public int metaBufferHeight;
    // TODO: remove
    public int touchCoordXLast;
    public int touchCoordYLast;
    public int depthOfFieldLast;

    public int depthBufferWidth;
    public int depthBufferHeight;
    public byte[] depthBuffer;

    public int depthMapWidth;
    public int depthMapHeight;
    public byte[] depthMap;

    // add for kibo+
    public byte[] debugBuffer;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SegmentMaskInfo:");
        sb.append("\n    metaBufferWidth  = 0x" + Integer.toHexString(metaBufferWidth) + "("
                + metaBufferWidth + ")");
        sb.append("\n    metaBufferHeight = 0x" + Integer.toHexString(metaBufferHeight) + "("
                + metaBufferHeight + ")");
        sb.append("\n    touchCoordXLast = 0x" + Integer.toHexString(touchCoordXLast) + "("
                + touchCoordXLast + ")");
        sb.append("\n    touchCoordYLast = 0x" + Integer.toHexString(touchCoordYLast) + "("
                + touchCoordYLast + ")");
        sb.append("\n    depthOfFieldLast = 0x" + Integer.toHexString(depthOfFieldLast) + "("
                + depthOfFieldLast + ")");
        sb.append("\n    depthBufferWidth = 0x" + Integer.toHexString(depthBufferWidth) + "("
                + depthBufferWidth + ")");
        sb.append("\n    depthBufferHeight = 0x" + Integer.toHexString(depthBufferHeight)
                + "(" + depthBufferHeight + ")");
        sb.append("\n    depthMapWidth = 0x" + Integer.toHexString(depthMapWidth) + "("
                + depthMapWidth + ")");
        sb.append("\n    depthMapHeight = 0x" + Integer.toHexString(depthMapHeight) + "("
                + depthMapHeight + ")");
        if (depthBuffer != null) {
            sb.append("\n    depthBuffer length = 0x"
                    + Integer.toHexString(depthBuffer.length) + "(" + depthBuffer.length + ")");
        } else {
            sb.append("\n    depthBuffer = null");
        }
        if (depthMap != null) {
            sb.append("\n    depthMap length = 0x" + Integer.toHexString(depthMap.length)
                    + "(" + depthMap.length + ")");
        } else {
            sb.append("\n    depthMap = null");
        }
        if (debugBuffer != null) {
            sb.append("\n    debugBuffer length = 0x" + Integer.toHexString(debugBuffer.length)
                    + "(" + debugBuffer.length + ")");
        } else {
            sb.append("\n    debugBuffer = null");
        }
        return sb.toString();
    }
}
