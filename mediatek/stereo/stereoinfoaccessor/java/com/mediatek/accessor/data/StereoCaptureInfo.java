package com.mediatek.accessor.data;

/**
 * Stereo capture information.
 */
public class StereoCaptureInfo {
    public String debugDir;
    public byte[] jpgBuffer;
    public byte[] jpsBuffer;
    public byte[] configBuffer;
    public byte[] clearImage;
    public byte[] depthMap;
    public byte[] ldc;
    // add for kibo+
    public byte[] depthBuffer; // mtk depth
    public byte[] debugBuffer;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("StereoBufferInfo:");
        if (jpgBuffer != null) {
            sb.append("\n    jpgBuffer length = 0x" + Integer.toHexString(jpgBuffer.length)
                    + "(" + jpgBuffer.length + ")");
        } else {
            sb.append("\n    jpgBuffer = null");
        }
        if (jpsBuffer != null) {
            sb.append("\n    jpsBuffer length = 0x" + Integer.toHexString(jpsBuffer.length)
                    + "(" + jpsBuffer.length + ")");
        } else {
            sb.append("\n    jpsBuffer = null");
        }
        if (configBuffer != null) {
            sb.append("\n    jsonBuffer length = 0x" + Integer.toHexString(configBuffer.length)
                    + "(" + configBuffer.length + ")");
        } else {
            sb.append("\n    jsonBuffer = null");
        }
        if (clearImage != null) {
            sb.append("\n    clearImage length = 0x" + Integer.toHexString(clearImage.length)
                    + "(" + clearImage.length + ")");
        } else {
            sb.append("\n    clearImage = null");
        }
        if (depthMap != null) {
            sb.append("\n    depthMap length = 0x" + Integer.toHexString(depthMap.length)
                    + "(" + depthMap.length + ")");
        } else {
            sb.append("\n    depthMap = null");
        }
        if (depthBuffer != null) {
            sb.append("\n    depthBuffer length = 0x" + Integer.toHexString(depthBuffer.length)
                    + "(" + depthBuffer.length + ")");
        } else {
            sb.append("\n    depthBuffer = null");
        }
        if (ldc != null) {
            sb.append("\n    ldc length = 0x" + Integer.toHexString(ldc.length) + "("
                    + ldc.length + ")");
        } else {
            sb.append("\n    ldc = null");
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
