package com.mediatek.accessor.data;

/**
 * Google stereo information.
 */
public class GoogleStereoInfo {
    public String debugDir;
    public double focusBlurAtInfinity;
    public double focusFocalDistance;
    public double focusFocalPointX;
    public double focusFocalPointY;
    public String imageMime;
    public String depthFormat;
    public double depthNear;
    public double depthFar;
    public String depthMime;

    public byte[] clearImage;
    public byte[] depthMap;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("GoogleStereoInfo:");
        sb.append("\n    focusBlurAtInfinity = " + focusBlurAtInfinity);
        sb.append("\n    focusFocalDistance = " + focusFocalDistance);
        sb.append("\n    focusFocalPointX = " + focusFocalPointX);
        sb.append("\n    focusFocalPointY = " + focusFocalPointY);
        sb.append("\n    imageMime = " + imageMime);
        sb.append("\n    depthFormat = " + depthFormat);
        sb.append("\n    depthNear = " + depthNear);
        sb.append("\n    depthFar = " + depthFar);
        sb.append("\n    depthMime = " + depthMime);
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
        return sb.toString();
    }
}
