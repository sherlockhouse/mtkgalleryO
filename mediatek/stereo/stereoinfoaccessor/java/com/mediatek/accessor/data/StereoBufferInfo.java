package com.mediatek.accessor.data;

/**
 * Stereo buffer information.
 */
public class StereoBufferInfo {
    public String debugDir;
    public byte[] jpsBuffer;
    public byte[] maskBuffer;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("StereoBufferInfo:");
        if (jpsBuffer != null) {
            sb.append("\n    jpsBuffer length = 0x" + Integer.toHexString(jpsBuffer.length)
                    + "(" + jpsBuffer.length + ")");
        } else {
            sb.append("\n    jpsBuffer = null");
        }
        if (maskBuffer != null) {
            sb.append("\n    maskBuffer length = 0x" + Integer.toHexString(maskBuffer.length)
                    + "(" + maskBuffer.length + ")");
        } else {
            sb.append("\n    maskBuffer = null");
        }
        return sb.toString();
    }
}
