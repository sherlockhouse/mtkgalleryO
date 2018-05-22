#ifndef STEREO_CONFIG_H
#define STEREO_CONFIG_H

#include "types.h"
#include <sstream>
#include "StereoInfo.h"

namespace stereo {

class FaceDetectionInfo {
public:
    int faceLeft;
    int faceTop;
    int faceRight;
    int faceBottom;
    int faceRip;

    FaceDetectionInfo() :
        faceLeft(0),
        faceTop(0),
        faceRight(0),
        faceBottom(0),
        faceRip(0) {
    }

    FaceDetectionInfo(int left, int top, int right, int bottom, int rip) {
        faceLeft = left;
        faceTop = top;
        faceRight = right;
        faceBottom = bottom;
        faceRip = rip;
    }

    StereoString toString() {
        std::stringstream ss;
        ss << "FaceDetectionInfo:";
        ss << "\n    faceLeft = " << faceLeft;
        ss << "\n    faceTop = " << faceTop;
        ss << "\n    faceRight = " << faceRight;
        ss << "\n    faceBottom = " << faceBottom;
        ss << "\n    faceRip = " << faceRip;
        return ss.str();
    }
};

class FocusInfo {
public:
    int focusType;
    int focusTop;
    int focusLeft;
    int focusRight;
    int focusBottom;

    FocusInfo() :
        focusType(0),
        focusTop(0),
        focusLeft(0),
        focusRight(0),
        focusBottom(0) {
    }

    FocusInfo(int type, int left, int top, int right, int bottom) {
        focusLeft = left;
        focusTop = top;
        focusRight = right;
        focusBottom = bottom;
        focusType = type;
    }

    StereoString toString() {
        std::stringstream ss;
        ss << "FocusInfo:";
        ss << "(left,top,right,bottom|type): ";
        ss << "(" << focusLeft << "," << focusTop << "," << focusRight << "," << focusBottom
                << "|" << focusType << ")";
        return ss.str();
    }
};

class StereoConfigInfo {
public:
    StereoString debugDir;
    int jpsWidth;
    int jpsHeight;
    int maskWidth;
    int maskHeight;
    int posX;
    int posY;
    int viewWidth;
    int viewHeight;
    int imageOrientation;
    int depthOrientation;
    int mainCamPos;
    int touchCoordX1st;
    int touchCoordY1st;
    int faceCount;
    FocusInfo *focusInfo;
    StereoVector<FaceDetectionInfo*> *fdInfoArray;
    int dofLevel;
    float convOffset;
    int ldcWidth;
    int ldcHeight;
    StereoBuffer_t ldcBuffer;
    StereoBuffer_t clearImage;

    // FocusInfo
    bool isFace;
    float faceRatio;
    int curDac;
    int minDac;
    int maxDac;

    StereoConfigInfo() :
        jpsWidth(0), jpsHeight(0), maskWidth(0), maskHeight(0), posX(0),
        posY(0), viewWidth(0), viewHeight(0), imageOrientation(0), depthOrientation(0),
        mainCamPos(0), touchCoordX1st(0), touchCoordY1st(0), faceCount(0), focusInfo(nullptr),
        fdInfoArray(nullptr), dofLevel(0), convOffset(0.0), ldcWidth(0), ldcHeight(0),
        isFace(false), faceRatio(0.0), curDac(0), minDac(0), maxDac(0) {}

    virtual ~StereoConfigInfo();

    StereoString toString() {
        std::stringstream ss;
        ss << "StereoConfigInfo:";
        ss << "\n    jpsWidth  = " << jpsWidth;
        ss << "\n    jpsHeight = " << jpsHeight;
        ss << "\n    maskWidth = " << maskWidth;
        ss << "\n    maskHeight = " << maskHeight;
        ss << "\n    posX = " << posX ;
        ss << "\n    posY = " << posY;
        ss << "\n    viewWidth = " << viewWidth;
        ss << "\n    viewHeight = " << viewHeight;
        ss << "\n    imageOrientation = " << imageOrientation;
        ss << "\n    depthOrientation = " << depthOrientation;
        ss << "\n    mainCamPos = " << mainCamPos;
        ss << "\n    touchCoordX1st = " << touchCoordX1st;
        ss << "\n    touchCoordY1st = " << touchCoordY1st;
        ss << "\n    faceCount = " << faceCount;
        if (focusInfo != nullptr) {
            ss << "\n    " << focusInfo->toString();
        } else {
            ss << "\n    focusInfo == null";
        }
        ss << "\n    dofLevel = " << dofLevel;
        ss << "\n    convOffset = " << convOffset;
        ss << "\n    ldcWidth = " << ldcWidth;
        ss << "\n    ldcHeight = " << ldcHeight;
        ss << "\n    ldcBuffer length = " << ldcBuffer.size;
        ss << "\n    clearImage length = " << clearImage.size;
        // focus info
        ss << "\n    isFace = " << isFace;
        ss << "\n    faceRatio = " << faceRatio;
        ss << "\n    curDac = " << curDac;
        ss << "\n    minDac = " << minDac;
        ss << "\n    maxDac = " << maxDac;

        if (fdInfoArray != nullptr) {
            int fdInfoCount = fdInfoArray->size();
            for (int i = 0; i < fdInfoCount; i++) {
                ss << "\n    fdInfoArray[" << i << "] = " << (*fdInfoArray)[i]->toString();
            }
        } else {
            ss << "\n    fdInfoArray = null";
        }
        return ss.str();
    }
};

}

#endif
