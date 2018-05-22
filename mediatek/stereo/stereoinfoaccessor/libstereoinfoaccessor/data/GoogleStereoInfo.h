#ifndef GOOGLE_STEREO_INFO_H
#define GOOGLE_STEREO_INFO_H

#include "types.h"
#include "BufferManager.h"
#include <sstream>

namespace stereo {

class GoogleStereoInfo {
public:
    StereoString debugDir;
    double focusBlurAtInfinity;
    double focusFocalDistance;
    double focusFocalPointX;
    double focusFocalPointY;
    StereoString imageMime;
    StereoString depthFormat;
    double depthNear;
    double depthFar;
    StereoString depthMime;

    StereoBuffer_t clearImage;
    StereoBuffer_t depthMap;

    GoogleStereoInfo() :
        focusBlurAtInfinity(0.0),
        focusFocalDistance(0.0),
        focusFocalPointX(0.0),
        focusFocalPointY(0.0),
        depthNear(0.0),
        depthFar(0.0) {
    }

    virtual ~GoogleStereoInfo();

    StereoString toString() {
        std::stringstream ss;
        ss.precision(std::numeric_limits<double>::digits10);
        ss << "GoogleStereoInfo:";
        ss << "\n    focusBlurAtInfinity = " << focusBlurAtInfinity;
        ss << "\n    focusFocalDistance = " << focusFocalDistance;
        ss << "\n    focusFocalPointX = " << focusFocalPointX;
        ss << "\n    focusFocalPointY = " << focusFocalPointY;
        ss << "\n    imageMime = " << imageMime;
        ss << "\n    depthFormat = " << depthFormat;
        ss << "\n    depthNear = " << depthNear;
        ss << "\n    depthFar = " << depthFar;
        ss << "\n    depthMime = " << depthMime;
        ss << "\n    clearImage length = " << clearImage.size;
        ss << "\n    depthMap length = " << depthMap.size;
        return ss.str();
    }
};

}

#endif

