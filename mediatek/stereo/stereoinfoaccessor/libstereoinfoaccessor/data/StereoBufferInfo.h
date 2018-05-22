#ifndef STEREO_BUFFER_INFO_H
#define STEREO_BUFFER_INFO_H

#include "types.h"
#include "BufferManager.h"
#include <sstream>

namespace stereo {

class StereoBufferInfo {
public:
    StereoString debugDir;
    StereoBuffer_t jpsBuffer;
    StereoBuffer_t maskBuffer;

    StereoBufferInfo() {}
    virtual ~StereoBufferInfo();

    StereoString toString() {
        std::stringstream ss;
        ss << "StereoBufferInfo:";
        ss << "\n    jpsBuffer length = " << jpsBuffer.size;
        ss << "\n    maskBuffer length = " << maskBuffer.size;
        return ss.str();
    }
};

}

#endif