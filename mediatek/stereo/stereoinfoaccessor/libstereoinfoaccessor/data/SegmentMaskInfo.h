#ifndef SEGMENT_MASK_INFO_H
#define SEGMENT_MASK_INFO_H

#include "types.h"
#include <sstream>

namespace stereo {

class SegmentMaskInfo {
public:
    StereoString debugDir;
    int maskWidth;
    int maskHeight;
    int segmentX;
    int segmentY;
    int segmentLeft;
    int segmentTop;
    int segmentRight;
    int segmentBottom;
    StereoBuffer_t maskBuffer;

    SegmentMaskInfo() :
        maskWidth(0),
        maskHeight(0),
        segmentX(0),
        segmentY(0),
        segmentLeft(0),
        segmentTop(0),
        segmentRight(0),
        segmentBottom(0) {
    }

    virtual ~SegmentMaskInfo();

    StereoString toString() {
        std::stringstream ss;
        ss << "SegmentMaskInfo:";
        ss << "\n    maskWidth  = "<< maskWidth;
        ss << "\n    maskHeight = " << maskHeight;
        ss << "\n    segmentX = " << segmentX;
        ss << "\n    segmentY = " << segmentY;
        ss << "\n    segmentLeft = " << segmentLeft;
        ss << "\n    segmentTop = " << segmentTop;
        ss << "\n    segmentRight = " << segmentRight;
        ss << "\n    segmentBottom = " << segmentBottom;
        ss << "\n    maskBuffer length = " << maskBuffer.size;
        return ss.str();
    }
};

}

#endif

