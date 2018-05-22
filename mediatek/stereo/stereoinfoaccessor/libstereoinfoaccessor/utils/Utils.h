#ifndef STEREO_UTILS_H
#define STEREO_UTILS_H

#include "types.h"
#include "BufferManager.h"
#include <sys/time.h>

#define DUMP_FILE_FOLDER "/sdcard/dumpJps"
#define ENABLE_GDEPTH_CFG "sdcard/ENABLE_GDEPTH"

#define PNG_DEFAULT_COLOR 0xFF
#define PNG_BYTES_PER_PIXEL 4
#define PNG_NUM_3 3

namespace stereo {

class Utils {

public:
    static const bool ENABLE_BUFFER_DUMP;
    static const bool ENABLE_GDEPTH;

    static void int2str(const int &intVal, StereoString &strVal);
    static void d2str(const double &dVal, StereoString &strVal);
    static StereoString buffer2Str(const StereoBuffer_t &buffer);
    static StereoString buffer2Str(const StereoBuffer_t &buffer, S_UINT32 size);
    static StereoString buffer2Str(
            const StereoBuffer_t &buffer, S_UINT32  offset, S_UINT32 size);
    static StereoString intToHexString(int val);
    static int isFileExist(const char *filePath);
    static int isDirExist(const char *dirPath);
    static int checkOrCreateDir(const char *dirPath);
    static void writeBufferToFile(const StereoString &destFile, const StereoBuffer_t &buffer);
    static void writeStringToFile(const StereoString &destFile, const StereoString &value);
    static void readFileToBuffer(
        const StereoString &filePath, StereoBuffer_t &outBuffer);
    static StereoString getFileNameFromPath(const StereoString &filePath);
    static void encodePng(const StereoBuffer_t &inputBuffer,
        int width, int height, StereoBuffer_t &outBuffer);

    static long getCurrentTime() {
        struct timeval tv;    
        gettimeofday(&tv,nullptr);    
        return tv.tv_sec * 1000 + tv.tv_usec / 1000;  
    }
};

}

#endif