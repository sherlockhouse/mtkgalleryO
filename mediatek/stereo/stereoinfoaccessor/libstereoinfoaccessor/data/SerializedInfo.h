#ifndef SERIALIZED_INFO_H
#define SERIALIZED_INFO_H

#include "types.h"
#include "BufferManager.h"

/**
 * XMP key.
 * Keep same with XmpMetaOperator.XMP_KEY.
 */
#define XMP_KEY "XMP"

typedef struct SerializedInfo {
    // Used to saving serialized Standard Xmp buffer.
    stereo::StereoBuffer_t standardXmpBuf;
    // Used to saving serialized Extended Xmp buffer.
    stereo::StereoBuffer_t extendedXmpBuf;
    // Used to saving serialized CustomizedBuf map.
    stereo::BufferMapPtr customizedBufMap;

    SerializedInfo() :
        customizedBufMap(nullptr) {
    }
} SerializedInfo_t;

#endif