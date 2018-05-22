#ifndef STEREO_CAPTURE_INFO_H
#define STEREO_CAPTURE_INFO_H

#include "types.h"
#include <sstream>

namespace stereo {

class StereoCaptureInfo {
public:
    StereoString debugDir;
    StereoBuffer_t jpgBuffer;
    StereoBuffer_t jpsBuffer;
    StereoBuffer_t configBuffer;
    StereoBuffer_t clearImage;
    StereoBuffer_t depthMap;
    StereoBuffer_t ldc;
    // add for kibo+
    StereoBuffer_t depthBuffer; // mtk depth
    StereoBuffer_t debugBuffer;

    StereoCaptureInfo() {}

    virtual ~StereoCaptureInfo();

    StereoString toString() {
        std::stringstream ss;
        ss << "~StereoCaptureInfo:";
        ss << "\n    jpgBuffer length = " << jpgBuffer.size;
        ss << "\n    jpsBuffer length = " << jpsBuffer.size;
        ss << "\n    jsonBuffer length = " << configBuffer.size;
        ss << "\n    clearImage length = " << clearImage.size;
        ss << "\n    depthMap length = " << depthMap.size;
        ss << "\n    depthBuffer length = " << depthBuffer.size;
        ss << "\n    ldc length = " << ldc.size;
        ss << "\n    debugBuffer length = " << debugBuffer.size;
        return ss.str();
    }
};

}

#endif

