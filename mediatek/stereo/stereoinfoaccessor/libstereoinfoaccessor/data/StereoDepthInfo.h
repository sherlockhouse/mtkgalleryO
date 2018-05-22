#ifndef STEREO_DEPTH_INFO_H
#define STEREO_DEPTH_INFO_H

#include "types.h"
#include <sstream>
#include "StereoInfo.h"

namespace stereo {

class StereoDepthInfo {
public:
    StereoString debugDir;

    int metaBufferWidth;
    int metaBufferHeight;
    
    int touchCoordXLast;
    int touchCoordYLast;
    int depthOfFieldLast;

    int depthBufferWidth;
    int depthBufferHeight;
    StereoBuffer_t depthBuffer;

    int depthMapWidth;
    int depthMapHeight;
    StereoBuffer_t depthMap;

    // add for kibo+
    StereoBuffer_t debugBuffer;

    StereoDepthInfo() :
        metaBufferWidth(0),
        metaBufferHeight(0),
        touchCoordXLast(0),
        touchCoordYLast(0),
        depthOfFieldLast(0),
        depthBufferWidth(0),
        depthBufferHeight(0),
        depthMapWidth(0),
        depthMapHeight(0) {}

    virtual ~StereoDepthInfo();

    StereoString toString() {
        std::stringstream ss;
        ss << "StereoDepthInfo:";
        ss << "\n    metaBufferWidth  = " << metaBufferWidth;
        ss << "\n    metaBufferHeight = " << metaBufferHeight;
        ss << "\n    touchCoordXLast = " << touchCoordXLast;
        ss << "\n    touchCoordYLast = " << touchCoordYLast;
        ss << "\n    depthOfFieldLast = " << depthOfFieldLast;
        ss << "\n    depthBufferWidth = " << depthBufferWidth;
        ss << "\n    depthBufferHeight = " << depthBufferHeight;
        ss << "\n    depthMapWidth = " << depthMapWidth;
        ss << "\n    depthMapHeight = " << depthMapHeight;
        ss << "\n    depthBuffer length = " << depthBuffer.size;
        ss << "\n    depthMap length = " << depthMap.size;
        ss << "\n    debugBuffer length = " << debugBuffer.size;
        return ss.str();
    }
};

}

#endif

