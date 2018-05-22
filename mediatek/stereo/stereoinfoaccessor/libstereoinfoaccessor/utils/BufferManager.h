#ifndef BUFFER_MANAGER_H
#define BUFFER_MANAGER_H

#include <utils/Mutex.h>
#include <utils/threads.h>
#include "types.h"

namespace stereo {

typedef struct StereoBuffer {
public:
    StereoBuffer() : data(nullptr), size(0) {}
    StereoBuffer(S_UINT8 *_data, S_UINT32 _size)     : data(_data), size(_size) {}
    bool isValid() const {
        return data != nullptr && size != 0;
    }
    S_UINT8 *data;
    S_UINT32 size;
} StereoBuffer_t;

class StereoBigBuffer {
public:
    // buffer start address
    S_UINT8 *data;
    // buffer total size
    S_UINT32 totalSize;
    // next avaliable buffer start position
    S_UINT32 next;
    // remaining available memory bytes
    S_UINT32 availables;

    StereoBigBuffer(S_UINT32 size);

    virtual ~StereoBigBuffer();

    bool isAvaliable(S_UINT32 size);

    void allocate(S_UINT32 size, StereoBuffer_t &buffer);
};

class BufferManager {

public:
    static void createBuffer(S_UINT32 size, StereoBuffer_t &buffer);
    static void releaseAll();
    static StereoVector<StereoBigBuffer*>* findBigBuffers();
    static StereoVector<StereoBigBuffer*>* popBigBuffers();
    static void pushBigBuffer(StereoBigBuffer* pBuffer);
private:
    static android::Mutex mBufferLock;
    static StereoMap<pid_t, StereoVector<StereoBigBuffer*>*> mBuffers;
};

typedef StereoMap<StereoString, StereoBuffer_t> BufferMap;
typedef BufferMap * BufferMapPtr;

}

#endif