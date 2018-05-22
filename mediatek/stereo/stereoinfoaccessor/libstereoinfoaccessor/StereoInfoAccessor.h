#ifndef STEREO_INFO_ACCESSOR_H
#define STEREO_INFO_ACCESSOR_H

#include <utils/RefBase.h>
#include "types.h"
#include "StereoInfo.h"
#include "IPacker.h"
#include "IParser.h"
#include "StereoDepthInfo.h"
#include "StereoCaptureInfo.h"
#include "StereoConfigInfo.h"
#include "SegmentMaskInfo.h"
#include "GoogleStereoInfo.h"
#include "StereoBufferInfo.h"
#include "BufferManager.h"

namespace stereo {

/**
 * StereoInfoAccessor, provide simple utility methods for third application,
 * to read or write some xmp or stereo related information.
 */
class StereoInfoAccessor : public android::RefBase {
public:
    /**
     * write stereo capture info to file buffer.
     * @param captureInfo
     *            StereoCaptureInfo
     * @param [out]jpgBuffer
     */
    void writeStereoCaptureInfo(StereoCaptureInfo &captureInfo, StereoBuffer_t &jpgBuffer);

    /**
     * write stereo depth info to file buffer.
     * @param filePath
     *            file path
     * @param depthInfo
     *            StereoDepthInfo
     */
    void writeStereoDepthInfo(const StereoString &filePath, StereoDepthInfo &depthInfo);

    /**
     * write segment and mask info to file buffer.
     * @param filePath
     *            file path
     * @param maskInfo
     *            SegmentMaskInfo
     */
    void writeSegmentMaskInfo(const StereoString &filePath, SegmentMaskInfo &maskInfo);

    /**
     * write refocus info to file buffer.
     * @param filePath
     *            file path
     * @param configInfo
     *            StereoConfigInfo
     * @param blurImage
     *            buffer
     */
    void writeRefocusImage(const StereoString &filePath, StereoConfigInfo &configInfo,
                                  StereoBuffer_t &blurImage);

    /**
     * Read depth information of stereo image.
     * @param filePath
     *            stereo image file path
     * @return StereoDepthInfo
     */
    StereoDepthInfo* readStereoDepthInfo(const StereoString & filePath);

    /**
     * Read segment mask information of stereo image.
     * @param filePath
     *            stereo image file path
     * @return SegmentMaskInfo
     */
    SegmentMaskInfo* readSegmentMaskInfo(const StereoString & filePath);

    /**
     * Read stereo buffer information of stereo image.
     * @param filePath
     *            stereo image file path
     * @return StereoBufferInfo
     */
    StereoBufferInfo* readStereoBufferInfo(const StereoString & filePath);

    /**
     * Read stereo config information of stereo image.
     * @param filePath
     *            stereo image file path
     * @return SegmentMaskInfo
     */
    StereoConfigInfo* readStereoConfigInfo(const StereoString & filePath);

    /**
     * Read google stereo information of stereo image.
     * @param filePath
     *            stereo image file path
     * @return GoogleStereoInfo
     */
     GoogleStereoInfo* readGoogleStereoInfo(const StereoString & filePath);

    /**
     * Get Geo verify level, debug tool's using.
     * @param configBuffer
     *            json config buffer
     * @return Geo verify level
     */
     int getGeoVerifyLevel(const StereoBuffer_t &configBuffer);

    /**
     * Get Pho verify level, debug tool's using.
     * @param configBuffer
     *            json config buffer
     * @return Pho verify level.
     */
    int getPhoVerifyLevel(const StereoBuffer_t &configBuffer);

    /**
     * Get Cha verify level, debug tool's using.
     * @param configBuffer
     *            json config buffer
     * @return Cha verify level.
     */
    int getMtkChaVerifyLevel(const StereoBuffer_t &configBuffer);

protected:
    virtual ~StereoInfoAccessor() {}

private:
    void serialize(PackInfo &info, IParser &parser);
};

}

#endif