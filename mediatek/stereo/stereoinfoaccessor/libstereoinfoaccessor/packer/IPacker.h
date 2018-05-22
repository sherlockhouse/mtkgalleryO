#ifndef IPACKER_H
#define IPACKER_H

#include "types.h"
#include "BufferManager.h"

namespace stereo {

class IPacker {

public:
    virtual ~IPacker() {}
    virtual void pack() = 0;
    virtual void unpack() = 0;
};


/**
 * Pack parameter for packer and unpacker.
 *
 * Packing ------------------------------------>---------------------------------->
 *                 |serialize          |Cust/XmpPacker          |JpgPacker
 *                 (meta serialize)    (append header)          (append APPX tag)
 *
 * StandardXMP     |StereoBuffer       |StereoBuffer            |StereoBuffer
 * ExtendedXMP     |StereoBuffer       |vector<StereoBuffer>    |vector<StereoBuffer>
 * CustomizedData  |BufferMap          |vector<StereoBuffer>    |vector<StereoBuffer>
 *
 * <-------------------------------------------<--------------------------unpacking
 */
class PackInfo {

public:
    // input for packing
    StereoBuffer_t unpackedJpgBuf;
    StereoBuffer_t unpackedBlurImageBuf;
    StereoBuffer_t unpackedStandardXmpBuf;
    StereoBuffer_t unpackedExtendedXmpBuf;
    BufferMapPtr unpackedCustomizedBufMap;

    // input for unpacking
    StereoBuffer_t packedJpgBuf;
    StereoBuffer_t packedStandardXmpBuf;
    StereoVector<StereoBuffer_t>* packedExtendedXmpBufArray;
    StereoVector<StereoBuffer_t>* packedCustomizedBufArray;

    PackInfo() : unpackedCustomizedBufMap(nullptr),
        packedExtendedXmpBufArray(nullptr), packedCustomizedBufArray(nullptr) {}

    ~PackInfo() {
        if (unpackedCustomizedBufMap != nullptr) {
            delete unpackedCustomizedBufMap;
            unpackedCustomizedBufMap = nullptr;
        }
        if (packedExtendedXmpBufArray != nullptr) {
            delete packedExtendedXmpBufArray;
            packedExtendedXmpBufArray = nullptr;
        }
        if (packedCustomizedBufArray != nullptr) {
            delete packedCustomizedBufArray;
            packedCustomizedBufArray = nullptr;
        }
        // other buffers will released in JNI
    }

    void dump() {
        if (unpackedCustomizedBufMap != nullptr) {
            //StereoLogD("<dump> unpackedCustomizedBufMap");
            for (auto iter = unpackedCustomizedBufMap->begin();
                iter != unpackedCustomizedBufMap->end(); iter++) {
                StereoString type = iter->first;
                StereoBuffer_t buffer = iter->second;
                //StereoLogD("type = %s, buffer size = %d", type.c_str(), buffer->size());
            }
        }
    }
};

}

#endif

