//#define LOG_NDEBUG 0
#define LOG_TAG "StereoInfoAccessor-JNI"
#include "StereoLog.h"

#include "jni.h"
#include "JNIHelp.h"
#include "android_runtime/AndroidRuntime.h"
#include "StereoInfoAccessor.h"
#include "BufferManager.h"
#include <string>
#include <vector>
#include "Utils.h"
#include <utils/Trace.h>

using namespace android;
using namespace stereo;
using namespace std;

#define ATRACE_TAG ATRACE_TAG_ALWAYS

#define FIND_CLASS(var, className) \
    var = env->FindClass(className); \
    LOG_FATAL_IF(! var, "Unable to find class " className);

#define GET_FIELD_ID(var, clazz, fieldName, fieldDescriptor) \
    var = env->GetFieldID(clazz, fieldName, fieldDescriptor); \
    LOG_FATAL_IF(! var, "Unable to find field " fieldName);

#define GET_METHOD_ID(var, clazz, fieldName, fieldDescriptor) \
    var = env->GetMethodID(clazz, fieldName, fieldDescriptor); \
    LOG_FATAL_IF(! var, "Unable to find method " fieldName);

#define GET_STATIC_FIELD_ID(var, clazz, fieldName, fieldDescriptor) \
    var = env->GetStaticFieldID(clazz, fieldName, fieldDescriptor); \
    LOG_FATAL_IF(! var, "Unable to find static field " fieldName);

#define GET_STATIC_METHOD_ID(var, clazz, fieldName, fieldDescriptor) \
    var = env->GetStaticMethodID(clazz, fieldName, fieldDescriptor); \
    LOG_FATAL_IF(! var, "Unable to find static method " fieldName);


struct StereoCaptureInfoFields {
    jfieldID debugDirField;
    jfieldID jpgBufferField;
    jfieldID jpsBufferField;
    jfieldID configBufferField;
    jfieldID clearImageField;
    jfieldID depthMapField;
    jfieldID ldcField;
    jfieldID depthBufferField;
    jfieldID debugBufferField;
};

struct StereoDepthInfoFields {
    jfieldID debugDirField;
    jfieldID metaBufferWidthField;
    jfieldID metaBufferHeightField;
    jfieldID touchCoordXLastField;
    jfieldID touchCoordYLastField;
    jfieldID depthOfFieldLastField;
    jfieldID depthBufferWidthField;
    jfieldID depthBufferHeightField;
    jfieldID depthBufferField;
    jfieldID depthMapWidthField;
    jfieldID depthMapHeightField;
    jfieldID depthMapField;
    jfieldID debugBufferField;
};

struct SegmentMaskInfoFields {
    jfieldID debugDirField;
    jfieldID maskWidthField;
    jfieldID maskHeightField;
    jfieldID segmentXField;
    jfieldID segmentYField;
    jfieldID segmentLeftField;
    jfieldID segmentTopField;
    jfieldID segmentRightField;
    jfieldID segmentBottomField;
    jfieldID maskBufferField;
};

struct StereoConfigInfoFields {
    jfieldID debugDirField;
    jfieldID jpsWidthField;
    jfieldID jpsHeightField;
    jfieldID maskWidthField;
    jfieldID maskHeightField;
    jfieldID posXField;
    jfieldID posYField;
    jfieldID viewWidthField;
    jfieldID viewHeightField;
    jfieldID imageOrientationField;
    jfieldID depthOrientationField;
    jfieldID mainCamPosField;
    jfieldID touchCoordX1stField;
    jfieldID touchCoordY1stField;
    jfieldID faceCountField;
    jfieldID focusInfoField;
    jfieldID fdInfoArrayField;
    jfieldID dofLevelField;
    jfieldID convOffsetField;
    jfieldID ldcWidthField;
    jfieldID ldcHeightField;
    jfieldID ldcBufferField;
    jfieldID clearImageField;
    jfieldID isFaceField;
    jfieldID faceRatioField;
    jfieldID curDacField;
    jfieldID minDacField;
    jfieldID maxDacField;
};

struct FocusInfoFields {
    jfieldID focusInfoTypeField;
    jfieldID focusInfoTopField;
    jfieldID focusInfoLeftField;
    jfieldID focusInfoRightField;
    jfieldID focusInfoBottomField;
};

struct FaceDetectionInfoFields {
    jfieldID fdInfoFaceLeftField;
    jfieldID fdInfoFaceTopField;
    jfieldID fdInfoFaceRightField;
    jfieldID fdInfoFaceBottomField;
    jfieldID fdInfoFaceRipField;
};

struct ArrayListFields {
    jmethodID initMethod;
    jmethodID sizeMethod;
    jmethodID getMethod;
    jmethodID addMethod;
};

struct StereoBufferInfoFields {
    jfieldID debugDirField;
    jfieldID jpsBufferField;
    jfieldID maskBufferField;
};

struct GoogleStereoInfoFields {
    jfieldID debugDirField;
    jfieldID focusBlurAtInfinityField;
    jfieldID focusFocalDistanceField;
    jfieldID focusFocalPointXField;
    jfieldID focusFocalPointYField;
    jfieldID imageMimeField;
    jfieldID depthFormatField;
    jfieldID depthNearField;
    jfieldID depthFarField;
    jfieldID depthMimeField;
    jfieldID clearImageField;
    jfieldID depthMapField;
};


struct fields_t {
    jfieldID context;
    StereoCaptureInfoFields captureInfo;
    StereoDepthInfoFields depthInfo;
    SegmentMaskInfoFields maskInfo;
    StereoConfigInfoFields configInfo;
    FocusInfoFields focusInfo;
    FaceDetectionInfoFields fdInfo;
    ArrayListFields arrayList;
    StereoBufferInfoFields bufferInfo;
    GoogleStereoInfoFields googleInfo;
};

static fields_t gFields;

static sp<StereoInfoAccessor> setObject(
        JNIEnv *env, jobject thiz, const sp<StereoInfoAccessor> &accessor) {
    sp<StereoInfoAccessor> old =
        (StereoInfoAccessor *)env->GetLongField(thiz, gFields.context);

    if (accessor != nullptr) {
        accessor->incStrong(thiz);
    }
    if (old != nullptr) {
        old->decStrong(thiz);
    }
    env->SetLongField(thiz, gFields.context, (jlong)accessor.get());

    return old;
}

static sp<StereoInfoAccessor> getObject(JNIEnv *env, jobject thiz) {
    return (StereoInfoAccessor *)env->GetLongField(thiz, gFields.context);
}

// jstring to std::string
// only support ASCII code
static string jstringToString(JNIEnv *env, jstring const &jStr){
    if (jStr == nullptr) {
        return string();
    }
    const char *cstr = env->GetStringUTFChars(jStr, nullptr);
    string str(cstr);
    env->ReleaseStringUTFChars(jStr, cstr);
    return str;
}

static void jbyteArrayToStereoBuffer(
        JNIEnv *env, jbyteArray const &byteArray, StereoBuffer_t &outBuffer) {
    if (byteArray == nullptr) {
        return;
    }
    size_t length = env->GetArrayLength(byteArray);
    char* buffer = (char*) env->GetByteArrayElements(byteArray, nullptr);
    BufferManager::createBuffer(length, outBuffer);
    memcpy(outBuffer.data, buffer, length);
    env->ReleaseByteArrayElements(byteArray, (jbyte*)buffer, 0);
}

static jbyteArray stereoBufferToJbyteArray(JNIEnv *env, const StereoBuffer_t &buffer) {
    if (!buffer.isValid()) {
        return nullptr;
    }
    StereoLogV("<StereoBufferToJbyteArray> length: %d, buffer: %p", buffer.size, buffer.data);
    jbyteArray result = env->NewByteArray(buffer.size);
    if (result != nullptr) {
        env->SetByteArrayRegion(result, 0, buffer.size, (jbyte *)buffer.data);
    }
    return result;
}


// init should invoked when load jni library
static void com_mediatek_stereo_StereoInfoAccessor_native_init(JNIEnv *env) {
    ATRACE_NAME(">>>>JNI-native_init");
    StereoLogI("<native_init>");
    jclass clazz;

    FIND_CLASS(clazz, "com/mediatek/accessor/StereoInfoAccessor_Native");
    GET_FIELD_ID(gFields.context, clazz, "mNativeContext", "J");

    FIND_CLASS(clazz, "com/mediatek/accessor/data/StereoCaptureInfo");
    GET_FIELD_ID(gFields.captureInfo.debugDirField, clazz, "debugDir", "Ljava/lang/String;");
    GET_FIELD_ID(gFields.captureInfo.jpgBufferField, clazz, "jpgBuffer", "[B");
    GET_FIELD_ID(gFields.captureInfo.jpsBufferField, clazz, "jpsBuffer", "[B");
    GET_FIELD_ID(gFields.captureInfo.configBufferField, clazz, "configBuffer", "[B");
    GET_FIELD_ID(gFields.captureInfo.clearImageField, clazz, "clearImage", "[B");
    GET_FIELD_ID(gFields.captureInfo.depthMapField, clazz, "depthMap", "[B");
    GET_FIELD_ID(gFields.captureInfo.ldcField, clazz, "ldc", "[B");
    GET_FIELD_ID(gFields.captureInfo.depthBufferField, clazz, "depthBuffer", "[B");
    GET_FIELD_ID(gFields.captureInfo.debugBufferField, clazz, "debugBuffer", "[B");

    FIND_CLASS(clazz, "com/mediatek/accessor/data/StereoDepthInfo");
    GET_FIELD_ID(gFields.depthInfo.debugDirField, clazz, "debugDir", "Ljava/lang/String;");
    GET_FIELD_ID(gFields.depthInfo.metaBufferWidthField, clazz, "metaBufferWidth", "I");
    GET_FIELD_ID(gFields.depthInfo.metaBufferHeightField, clazz, "metaBufferHeight", "I");
    GET_FIELD_ID(gFields.depthInfo.touchCoordXLastField, clazz, "touchCoordXLast", "I");
    GET_FIELD_ID(gFields.depthInfo.touchCoordYLastField, clazz, "touchCoordYLast", "I");
    GET_FIELD_ID(gFields.depthInfo.depthOfFieldLastField, clazz, "depthOfFieldLast", "I");
    GET_FIELD_ID(gFields.depthInfo.depthBufferWidthField, clazz, "depthBufferWidth", "I");
    GET_FIELD_ID(gFields.depthInfo.depthBufferHeightField, clazz, "depthBufferHeight", "I");
    GET_FIELD_ID(gFields.depthInfo.depthBufferField, clazz, "depthBuffer", "[B");
    GET_FIELD_ID(gFields.depthInfo.depthMapWidthField, clazz, "depthMapWidth", "I");
    GET_FIELD_ID(gFields.depthInfo.depthMapHeightField, clazz, "depthMapHeight", "I");
    GET_FIELD_ID(gFields.depthInfo.depthMapField, clazz, "depthMap", "[B");
    GET_FIELD_ID(gFields.depthInfo.debugBufferField, clazz, "debugBuffer", "[B");

    FIND_CLASS(clazz, "com/mediatek/accessor/data/SegmentMaskInfo");
    GET_FIELD_ID(gFields.maskInfo.debugDirField, clazz, "debugDir", "Ljava/lang/String;");
    GET_FIELD_ID(gFields.maskInfo.maskWidthField, clazz, "maskWidth", "I");
    GET_FIELD_ID(gFields.maskInfo.maskHeightField, clazz, "maskHeight", "I");
    GET_FIELD_ID(gFields.maskInfo.segmentXField, clazz, "segmentX", "I");
    GET_FIELD_ID(gFields.maskInfo.segmentYField, clazz, "segmentY", "I");
    GET_FIELD_ID(gFields.maskInfo.segmentLeftField, clazz, "segmentLeft", "I");
    GET_FIELD_ID(gFields.maskInfo.segmentTopField, clazz, "segmentTop", "I");
    GET_FIELD_ID(gFields.maskInfo.segmentRightField, clazz, "segmentRight", "I");
    GET_FIELD_ID(gFields.maskInfo.segmentBottomField, clazz, "segmentBottom", "I");
    GET_FIELD_ID(gFields.maskInfo.maskBufferField, clazz, "maskBuffer", "[B");

    FIND_CLASS(clazz, "com/mediatek/accessor/data/StereoConfigInfo");
    GET_FIELD_ID(gFields.configInfo.debugDirField, clazz, "debugDir", "Ljava/lang/String;");
    GET_FIELD_ID(gFields.configInfo.jpsWidthField, clazz, "jpsWidth", "I");
    GET_FIELD_ID(gFields.configInfo.jpsHeightField, clazz, "jpsHeight", "I");
    GET_FIELD_ID(gFields.configInfo.maskWidthField, clazz, "maskWidth", "I");
    GET_FIELD_ID(gFields.configInfo.maskHeightField, clazz, "maskHeight", "I");
    GET_FIELD_ID(gFields.configInfo.posXField, clazz, "posX", "I");
    GET_FIELD_ID(gFields.configInfo.posYField, clazz, "posY", "I");
    GET_FIELD_ID(gFields.configInfo.viewWidthField, clazz, "viewWidth", "I");
    GET_FIELD_ID(gFields.configInfo.viewHeightField, clazz, "viewHeight", "I");
    GET_FIELD_ID(gFields.configInfo.imageOrientationField, clazz, "imageOrientation", "I");
    GET_FIELD_ID(gFields.configInfo.depthOrientationField, clazz, "depthOrientation", "I");
    GET_FIELD_ID(gFields.configInfo.mainCamPosField, clazz, "mainCamPos", "I");
    GET_FIELD_ID(gFields.configInfo.touchCoordX1stField, clazz, "touchCoordX1st", "I");
    GET_FIELD_ID(gFields.configInfo.touchCoordY1stField, clazz, "touchCoordY1st", "I");
    GET_FIELD_ID(gFields.configInfo.faceCountField, clazz, "faceCount", "I");
    GET_FIELD_ID(gFields.configInfo.focusInfoField, clazz,
            "focusInfo", "Lcom/mediatek/accessor/data/StereoConfigInfo$FocusInfo;");
    GET_FIELD_ID(gFields.configInfo.fdInfoArrayField, clazz,
            "fdInfoArray", "Ljava/util/ArrayList;");
    GET_FIELD_ID(gFields.configInfo.dofLevelField, clazz, "dofLevel", "I");
    GET_FIELD_ID(gFields.configInfo.convOffsetField, clazz, "convOffset", "F");
    GET_FIELD_ID(gFields.configInfo.ldcWidthField, clazz, "ldcWidth", "I");
    GET_FIELD_ID(gFields.configInfo.ldcHeightField, clazz, "ldcHeight", "I");
    GET_FIELD_ID(gFields.configInfo.ldcBufferField, clazz, "ldcBuffer", "[B");
    GET_FIELD_ID(gFields.configInfo.clearImageField, clazz, "clearImage", "[B");
    GET_FIELD_ID(gFields.configInfo.isFaceField, clazz, "isFace", "Z");
    GET_FIELD_ID(gFields.configInfo.faceRatioField, clazz, "faceRatio", "F");
    GET_FIELD_ID(gFields.configInfo.curDacField, clazz, "curDac", "I");
    GET_FIELD_ID(gFields.configInfo.minDacField, clazz, "minDac", "I");
    GET_FIELD_ID(gFields.configInfo.maxDacField, clazz, "maxDac", "I");

    FIND_CLASS(clazz, "com/mediatek/accessor/data/StereoConfigInfo$FocusInfo");
    GET_FIELD_ID(gFields.focusInfo.focusInfoTypeField, clazz, "focusType", "I");
    GET_FIELD_ID(gFields.focusInfo.focusInfoTopField, clazz, "focusTop", "I");
    GET_FIELD_ID(gFields.focusInfo.focusInfoLeftField, clazz, "focusLeft", "I");
    GET_FIELD_ID(gFields.focusInfo.focusInfoRightField, clazz, "focusRight", "I");
    GET_FIELD_ID(gFields.focusInfo.focusInfoBottomField, clazz, "focusBottom", "I");

    FIND_CLASS(clazz, "com/mediatek/accessor/data/StereoConfigInfo$FaceDetectionInfo");
    GET_FIELD_ID(gFields.fdInfo.fdInfoFaceLeftField, clazz, "faceLeft", "I");
    GET_FIELD_ID(gFields.fdInfo.fdInfoFaceTopField, clazz, "faceTop", "I");
    GET_FIELD_ID(gFields.fdInfo.fdInfoFaceRightField, clazz, "faceRight", "I");
    GET_FIELD_ID(gFields.fdInfo.fdInfoFaceBottomField, clazz, "faceBottom", "I");
    GET_FIELD_ID(gFields.fdInfo.fdInfoFaceRipField, clazz, "faceRip", "I");

    FIND_CLASS(clazz, "java/util/ArrayList");
    GET_METHOD_ID(gFields.arrayList.initMethod, clazz, "<init>", "()V");
    GET_METHOD_ID(gFields.arrayList.sizeMethod, clazz, "size", "()I");
    GET_METHOD_ID(gFields.arrayList.getMethod, clazz, "get", "(I)Ljava/lang/Object;");
    GET_METHOD_ID(gFields.arrayList.addMethod, clazz, "add", "(Ljava/lang/Object;)Z");

    FIND_CLASS(clazz, "com/mediatek/accessor/data/StereoBufferInfo");
    GET_FIELD_ID(gFields.bufferInfo.debugDirField, clazz, "debugDir", "Ljava/lang/String;");
    GET_FIELD_ID(gFields.bufferInfo.jpsBufferField, clazz, "jpsBuffer", "[B");
    GET_FIELD_ID(gFields.bufferInfo.maskBufferField, clazz, "maskBuffer", "[B");

    FIND_CLASS(clazz, "com/mediatek/accessor/data/GoogleStereoInfo");
    GET_FIELD_ID(gFields.googleInfo.debugDirField, clazz, "debugDir", "Ljava/lang/String;");
    GET_FIELD_ID(gFields.googleInfo.focusBlurAtInfinityField, clazz, "focusBlurAtInfinity", "D");
    GET_FIELD_ID(gFields.googleInfo.focusFocalDistanceField, clazz, "focusFocalDistance", "D");
    GET_FIELD_ID(gFields.googleInfo.focusFocalPointXField, clazz, "focusFocalPointX", "D");
    GET_FIELD_ID(gFields.googleInfo.focusFocalPointYField, clazz, "focusFocalPointY", "D");
    GET_FIELD_ID(gFields.googleInfo.imageMimeField, clazz, "imageMime", "Ljava/lang/String;");
    GET_FIELD_ID(gFields.googleInfo.depthFormatField, clazz, "depthFormat", "Ljava/lang/String;");
    GET_FIELD_ID(gFields.googleInfo.depthNearField, clazz, "depthNear", "D");
    GET_FIELD_ID(gFields.googleInfo.depthFarField, clazz, "depthFar", "D");
    GET_FIELD_ID(gFields.googleInfo.depthMimeField, clazz, "depthMime", "Ljava/lang/String;");
    GET_FIELD_ID(gFields.googleInfo.clearImageField, clazz, "clearImage", "[B");
    GET_FIELD_ID(gFields.googleInfo.depthMapField, clazz, "depthMap", "[B");
}

// setup should invoked when execute StereoInfoAccessor constructor method
static void com_mediatek_stereo_StereoInfoAccessor_native_setup(
    JNIEnv *env, jobject thiz) {
    StereoLogI("<native_setup>");
    sp<StereoInfoAccessor> accessor = new StereoInfoAccessor();
    setObject(env, thiz, accessor);
}

// finalize should invoked when execute StereoInfoAccessor finalized method by gc
static void com_mediatek_stereo_StereoInfoAccessor_native_finalize(
        JNIEnv *env, jobject thiz) {
    StereoLogI("<native_finalize>");
    setObject(env, thiz, nullptr);
}

static jbyteArray com_mediatek_stereo_StereoInfoAccessor_native_writeStereoCaptureInfo(
        JNIEnv *env, jobject thiz, jobject captureInfo) {
    ATRACE_NAME(">>>>JNI-native_writeStereoCaptureInfo");
    StereoLogI("<native_writeStereoCaptureInfo>");
    if (captureInfo == nullptr) {
        StereoLogW("captureInfo is nullptr");
        return nullptr;
    }

    long startTime = Utils::getCurrentTime();
    // construct native objects from java objects
    jstring debugDirJStr = (jstring)env->GetObjectField(
            captureInfo, gFields.captureInfo.debugDirField);
    jbyteArray jpgBufferJBArr = (jbyteArray)env->GetObjectField(
            captureInfo, gFields.captureInfo.jpgBufferField);
    jbyteArray jpsBufferJBArr = (jbyteArray)env->GetObjectField(
            captureInfo, gFields.captureInfo.jpsBufferField);
    jbyteArray configBufferJBArr = (jbyteArray)env->GetObjectField(
            captureInfo, gFields.captureInfo.configBufferField);
    jbyteArray clearImageJBArr = (jbyteArray)env->GetObjectField(
            captureInfo, gFields.captureInfo.clearImageField);
    jbyteArray depthMapJBArr = (jbyteArray)env->GetObjectField(
            captureInfo, gFields.captureInfo.depthMapField);
    jbyteArray ldcJBArr = (jbyteArray)env->GetObjectField(
            captureInfo, gFields.captureInfo.ldcField);
    jbyteArray depthBufferJBArr = (jbyteArray)env->GetObjectField(
            captureInfo, gFields.captureInfo.depthBufferField);
    jbyteArray debugBufferJBArr = (jbyteArray)env->GetObjectField(
            captureInfo, gFields.captureInfo.debugBufferField);

    // construct native StereoCaptureInfo
    StereoCaptureInfo stereoCaptureInfo;
    stereoCaptureInfo.debugDir = jstringToString(env, debugDirJStr);
    jbyteArrayToStereoBuffer(env, jpgBufferJBArr, stereoCaptureInfo.jpgBuffer);
    jbyteArrayToStereoBuffer(env, jpsBufferJBArr, stereoCaptureInfo.jpsBuffer);
    jbyteArrayToStereoBuffer(env, configBufferJBArr, stereoCaptureInfo.configBuffer);
    jbyteArrayToStereoBuffer(env, clearImageJBArr, stereoCaptureInfo.clearImage);
    jbyteArrayToStereoBuffer(env, depthMapJBArr, stereoCaptureInfo.depthMap);
    jbyteArrayToStereoBuffer(env, ldcJBArr, stereoCaptureInfo.ldc);
    jbyteArrayToStereoBuffer(env, depthBufferJBArr, stereoCaptureInfo.depthBuffer);
    jbyteArrayToStereoBuffer(env, debugBufferJBArr, stereoCaptureInfo.debugBuffer);

    // writeStereoCaptureInfo
    sp<StereoInfoAccessor> accessor = getObject(env, thiz);
    if (accessor == nullptr) {
        StereoLogW("<native_writeStereoCaptureInfo><error> can't get accessor");
        BufferManager::releaseAll();
        return nullptr;
    }
    StereoBuffer_t packedJpgBuffer;
    accessor->writeStereoCaptureInfo(stereoCaptureInfo, packedJpgBuffer);
    if (!packedJpgBuffer.isValid()) {
        StereoLogW("<native_writeStereoCaptureInfo><error> can't get packedJpgBuffer");
        BufferManager::releaseAll();
        return nullptr;
    }

    // convert result to jbyteArray
    jbyteArray result = stereoBufferToJbyteArray(env, packedJpgBuffer);

    // release
    BufferManager::releaseAll();
    long elapsedTime = Utils::getCurrentTime() - startTime;
    StereoLogI("<native_writeStereoCaptureInfo> end, elapsed time = %d ms", elapsedTime);
    return result;
}

static void com_mediatek_stereo_StereoInfoAccessor_native_writeStereoDepthInfo(
        JNIEnv *env, jobject thiz, jstring filePath, jobject depthInfo) {
    ATRACE_NAME(">>>>JNI-native_writeStereoDepthInfo");
    StereoLogI("<native_writeStereoDepthInfo>");
    if (filePath == nullptr || depthInfo == nullptr) {
        StereoLogW("<native_writeStereoDepthInfo> filePath or depthInfo is nullptr");
        return;
    }

    long startTime = Utils::getCurrentTime();
    // construct native objects from java objects
    jstring debugDirJStr = (jstring)env->GetObjectField(
            depthInfo, gFields.depthInfo.debugDirField);
    jbyteArray depthBufferJBArr = (jbyteArray)env->GetObjectField(
            depthInfo, gFields.depthInfo.depthBufferField);
    jbyteArray depthMapJBArr = (jbyteArray)env->GetObjectField(
            depthInfo, gFields.depthInfo.depthMapField);
    jbyteArray debugBufferJBArr = (jbyteArray)env->GetObjectField(
            depthInfo, gFields.depthInfo.debugBufferField);

    // construct native StereoDepthInfo
    StereoDepthInfo stereoDepthInfo;
    stereoDepthInfo.debugDir = jstringToString(env, debugDirJStr);
    stereoDepthInfo.metaBufferWidth = env->GetIntField(
            depthInfo, gFields.depthInfo.metaBufferWidthField);
    stereoDepthInfo.metaBufferHeight = env->GetIntField(
            depthInfo, gFields.depthInfo.metaBufferHeightField);
    stereoDepthInfo.touchCoordXLast = env->GetIntField(
            depthInfo, gFields.depthInfo.touchCoordXLastField);
    stereoDepthInfo.touchCoordYLast = env->GetIntField(
            depthInfo, gFields.depthInfo.touchCoordYLastField);
    stereoDepthInfo.depthOfFieldLast = env->GetIntField(
            depthInfo, gFields.depthInfo.depthOfFieldLastField);
    stereoDepthInfo.depthBufferWidth = env->GetIntField(
            depthInfo, gFields.depthInfo.depthBufferWidthField);
    stereoDepthInfo.depthBufferHeight = env->GetIntField(
            depthInfo, gFields.depthInfo.depthBufferHeightField);
    jbyteArrayToStereoBuffer(env, depthBufferJBArr, stereoDepthInfo.depthBuffer);
    stereoDepthInfo.depthMapWidth = env->GetIntField(
            depthInfo, gFields.depthInfo.depthMapWidthField);
    stereoDepthInfo.depthMapHeight = env->GetIntField(
            depthInfo, gFields.depthInfo.depthMapHeightField);
    jbyteArrayToStereoBuffer(env, depthMapJBArr, stereoDepthInfo.depthMap);
    jbyteArrayToStereoBuffer(env, debugBufferJBArr, stereoDepthInfo.debugBuffer);

    // writeStereoDepthInfo
    sp<StereoInfoAccessor> accessor = getObject(env, thiz);
    if (accessor == nullptr) {
        StereoLogW("<native_writeStereoDepthInfo><error> can't get accessor");
        BufferManager::releaseAll();
        return;
    }

    accessor->writeStereoDepthInfo(jstringToString(env, filePath), stereoDepthInfo);
    BufferManager::releaseAll();
    long elapsedTime = Utils::getCurrentTime() - startTime;
    StereoLogI("<native_writeStereoDepthInfo> end, elapsed time = %d ms", elapsedTime);
}

static void com_mediatek_stereo_StereoInfoAccessor_native_writeSegmentMaskInfo(
        JNIEnv *env, jobject thiz, jstring filePath, jobject maskInfo) {
    ATRACE_NAME(">>>>JNI-native_writeSegmentMaskInfo");
    StereoLogI("<native_writeSegmentMaskInfo>");
    if (filePath == nullptr || maskInfo == nullptr) {
        StereoLogW("<native_writeSegmentMaskInfo> filePath or depthInfo is nullptr");
        return;
    }

    long startTime = Utils::getCurrentTime();
    // construct native objects from java objects
    jstring debugDirJStr = (jstring)env->GetObjectField(maskInfo, gFields.maskInfo.debugDirField);
    jbyteArray maskBufferJBArr = (jbyteArray)env->GetObjectField(
            maskInfo, gFields.maskInfo.maskBufferField);

    // construct native StereoCaptureInfo
    SegmentMaskInfo segmentMaskInfo;
    segmentMaskInfo.debugDir = jstringToString(env, debugDirJStr);
    segmentMaskInfo.maskWidth = env->GetIntField(maskInfo, gFields.maskInfo.maskWidthField);
    segmentMaskInfo.maskHeight = env->GetIntField(maskInfo, gFields.maskInfo.maskHeightField);
    segmentMaskInfo.segmentX = env->GetIntField(maskInfo, gFields.maskInfo.segmentXField);
    segmentMaskInfo.segmentY = env->GetIntField(maskInfo, gFields.maskInfo.segmentYField);
    segmentMaskInfo.segmentLeft = env->GetIntField(maskInfo, gFields.maskInfo.segmentLeftField);
    segmentMaskInfo.segmentTop = env->GetIntField(maskInfo, gFields.maskInfo.segmentTopField);
    segmentMaskInfo.segmentRight = env->GetIntField(maskInfo, gFields.maskInfo.segmentRightField);
    segmentMaskInfo.segmentBottom = env->GetIntField(
            maskInfo, gFields.maskInfo.segmentBottomField);
    jbyteArrayToStereoBuffer(env, maskBufferJBArr, segmentMaskInfo.maskBuffer);

    // writeStereoCaptureInfo
    sp<StereoInfoAccessor> accessor = getObject(env, thiz);
    if (accessor == nullptr) {
        StereoLogW("<native_writeSegmentMaskInfo><error> can't get accessor");
        BufferManager::releaseAll();
        return;
    }
    accessor->writeSegmentMaskInfo(jstringToString(env, filePath), segmentMaskInfo);
    BufferManager::releaseAll();
    long elapsedTime = Utils::getCurrentTime() - startTime;
    StereoLogI("<native_writeSegmentMaskInfo> end, elapsed time = %d ms", elapsedTime);
}

static void com_mediatek_stereo_StereoInfoAccessor_native_writeRefocusImage(
        JNIEnv *env, jobject thiz, jstring filePath, jobject configInfo, jbyteArray blurImage) {
    ATRACE_NAME(">>>>JNI-native_writeRefocusImage");
    StereoLogI("<native_writeRefocusImage>");
    if (filePath == nullptr || configInfo == nullptr) {
        StereoLogW("<native_writeRefocusImage> filePath or configInfo is nullptr");
        return;
    }

    long startTime = Utils::getCurrentTime();
    // construct native objects from java objects
    jstring debugDirJStr = (jstring)env->GetObjectField(
            configInfo, gFields.configInfo.debugDirField);
    jobject focusInfoJObj = env->GetObjectField(
            configInfo, gFields.configInfo.focusInfoField);
    jobject fdInfoArrayJObj = env->GetObjectField(
            configInfo, gFields.configInfo.fdInfoArrayField);
    jbyteArray ldcBufferJBArr = (jbyteArray)env->GetObjectField(
            configInfo, gFields.configInfo.ldcBufferField);
    jbyteArray clearImageJBArr = (jbyteArray)env->GetObjectField(
            configInfo, gFields.configInfo.clearImageField);

    // construct native StereoCaptureInfo
    StereoConfigInfo stereoConfigInfo;
    stereoConfigInfo.debugDir = jstringToString(env, debugDirJStr);
    stereoConfigInfo.jpsWidth = env->GetIntField(configInfo, gFields.configInfo.jpsWidthField);
    stereoConfigInfo.jpsHeight = env->GetIntField(configInfo, gFields.configInfo.jpsHeightField);
    stereoConfigInfo.maskWidth = env->GetIntField(configInfo, gFields.configInfo.maskWidthField);
    stereoConfigInfo.maskHeight = env->GetIntField(configInfo, gFields.configInfo.maskHeightField);
    stereoConfigInfo.posX = env->GetIntField(configInfo, gFields.configInfo.posXField);
    stereoConfigInfo.posY = env->GetIntField(configInfo, gFields.configInfo.posYField);
    stereoConfigInfo.viewWidth = env->GetIntField(configInfo, gFields.configInfo.viewWidthField);
    stereoConfigInfo.viewHeight = env->GetIntField(configInfo, gFields.configInfo.viewHeightField);
    stereoConfigInfo.imageOrientation = env->GetIntField(configInfo, gFields.configInfo.imageOrientationField);
    stereoConfigInfo.depthOrientation = env->GetIntField(configInfo, gFields.configInfo.depthOrientationField);
    stereoConfigInfo.mainCamPos = env->GetIntField(configInfo, gFields.configInfo.mainCamPosField);
    stereoConfigInfo.touchCoordX1st = env->GetIntField(configInfo, gFields.configInfo.touchCoordX1stField);
    stereoConfigInfo.touchCoordY1st = env->GetIntField(configInfo, gFields.configInfo.touchCoordY1stField);
    stereoConfigInfo.faceCount = env->GetIntField(configInfo, gFields.configInfo.faceCountField);
    if (focusInfoJObj != nullptr) {
        FocusInfo* focusInfo = new FocusInfo();
        focusInfo->focusType = env->GetIntField(focusInfoJObj, gFields.focusInfo.focusInfoTypeField);
        focusInfo->focusLeft = env->GetIntField(focusInfoJObj, gFields.focusInfo.focusInfoLeftField);
        focusInfo->focusTop = env->GetIntField(focusInfoJObj, gFields.focusInfo.focusInfoTopField);
        focusInfo->focusRight = env->GetIntField(focusInfoJObj, gFields.focusInfo.focusInfoRightField);
        focusInfo->focusBottom = env->GetIntField(focusInfoJObj, gFields.focusInfo.focusInfoBottomField);
        stereoConfigInfo.focusInfo = focusInfo;
    }
    if (fdInfoArrayJObj != nullptr) {
        vector<FaceDetectionInfo*> *fdInfoArray = new vector<FaceDetectionInfo*>();
        jint len = env->CallIntMethod(fdInfoArrayJObj, gFields.arrayList.sizeMethod);
        for (int i = 0; i < len; i++) {
            jobject fdInfoItemJObj = env->CallObjectMethod(
                fdInfoArrayJObj, gFields.arrayList.getMethod, i);
            if (fdInfoItemJObj != nullptr) {
                FaceDetectionInfo *fdInfoItem = new FaceDetectionInfo();
                fdInfoItem->faceLeft = env->GetIntField(fdInfoItemJObj, gFields.fdInfo.fdInfoFaceLeftField);
                fdInfoItem->faceTop = env->GetIntField(fdInfoItemJObj, gFields.fdInfo.fdInfoFaceTopField);
                fdInfoItem->faceRight = env->GetIntField(fdInfoItemJObj, gFields.fdInfo.fdInfoFaceRightField);
                fdInfoItem->faceBottom = env->GetIntField(fdInfoItemJObj, gFields.fdInfo.fdInfoFaceBottomField);
                fdInfoItem->faceRip = env->GetIntField(fdInfoItemJObj, gFields.fdInfo.fdInfoFaceRipField);
                fdInfoArray->push_back(fdInfoItem);
            }
        }
        stereoConfigInfo.fdInfoArray = fdInfoArray;
    }
    stereoConfigInfo.dofLevel = env->GetIntField(configInfo, gFields.configInfo.dofLevelField);
    stereoConfigInfo.convOffset = env->GetFloatField(configInfo, gFields.configInfo.convOffsetField);
    stereoConfigInfo.ldcWidth = env->GetIntField(configInfo, gFields.configInfo.ldcWidthField);
    stereoConfigInfo.ldcHeight = env->GetIntField(configInfo, gFields.configInfo.ldcHeightField);
    jbyteArrayToStereoBuffer(env, ldcBufferJBArr, stereoConfigInfo.ldcBuffer);
    jbyteArrayToStereoBuffer(env, clearImageJBArr, stereoConfigInfo.clearImage);
    stereoConfigInfo.isFace = env->GetBooleanField(configInfo, gFields.configInfo.isFaceField);
    stereoConfigInfo.faceRatio = env->GetFloatField(configInfo, gFields.configInfo.faceRatioField);
    stereoConfigInfo.curDac = env->GetIntField(configInfo, gFields.configInfo.curDacField);
    stereoConfigInfo.minDac = env->GetIntField(configInfo, gFields.configInfo.minDacField);
    stereoConfigInfo.maxDac = env->GetIntField(configInfo, gFields.configInfo.maxDacField);

    StereoBuffer_t blurImageBuf;
    jbyteArrayToStereoBuffer(env, blurImage, blurImageBuf);

    // writeStereoCaptureInfo
    sp<StereoInfoAccessor> accessor = getObject(env, thiz);
    if (accessor == nullptr) {
        StereoLogW("<native_writeRefocusImage><error> can't get accessor");
        BufferManager::releaseAll();
        return;
    }
    accessor->writeRefocusImage(jstringToString(env, filePath), stereoConfigInfo, blurImageBuf);

    BufferManager::releaseAll();
    long elapsedTime = Utils::getCurrentTime() - startTime;
    StereoLogI("<native_writeRefocusImage> end, elapsed time = %d ms", elapsedTime);
}

static jobject com_mediatek_stereo_StereoInfoAccessor_native_readStereoDepthInfo(
        JNIEnv *env, jobject thiz, jstring filePath) {
    ATRACE_NAME(">>>>JNI-native_readStereoDepthInfo");
    StereoLogI("<native_readStereoDepthInfo>");
    if (filePath == nullptr) {
        StereoLogW("<native_readStereoDepthInfo> filePath is nullptr");
        return nullptr;
    }

    long startTime = Utils::getCurrentTime();
    // readStereoDepthInfo
    sp<StereoInfoAccessor> accessor = getObject(env, thiz);
    if (accessor == nullptr) {
        StereoLogW("<native_readStereoDepthInfo><error> can't get accessor");
        return nullptr;
    }
    StereoDepthInfo* depthInfo = accessor->readStereoDepthInfo(jstringToString(env, filePath));
    if (depthInfo == nullptr) {
        StereoLogW("<native_readStereoDepthInfo><error> can't get depthInfo");
        BufferManager::releaseAll();
        return nullptr;
    }

    // Fill out return obj
    jclass clazz;
    FIND_CLASS(clazz, "com/mediatek/accessor/data/StereoDepthInfo");
    jobject infoObject = nullptr;
    if (clazz) {
        infoObject = env->AllocObject(clazz);
        jstring debugDirJStr = env->NewStringUTF(depthInfo->debugDir.c_str());
        env->SetObjectField(infoObject, gFields.depthInfo.debugDirField, debugDirJStr);
        env->SetIntField(infoObject, gFields.depthInfo.metaBufferWidthField,
                depthInfo->metaBufferWidth);
        env->SetIntField(infoObject, gFields.depthInfo.metaBufferHeightField,
                depthInfo->metaBufferHeight);
        env->SetIntField(infoObject, gFields.depthInfo.touchCoordXLastField,
                depthInfo->touchCoordXLast);
        env->SetIntField(infoObject, gFields.depthInfo.touchCoordYLastField,
                depthInfo->touchCoordYLast);
        env->SetIntField(infoObject, gFields.depthInfo.depthOfFieldLastField,
                depthInfo->depthOfFieldLast);
        env->SetIntField(infoObject, gFields.depthInfo.depthBufferWidthField,
                depthInfo->depthBufferWidth);
        env->SetIntField(infoObject, gFields.depthInfo.depthBufferHeightField,
                depthInfo->depthBufferHeight);
        jbyteArray depthBufferJBArr = stereoBufferToJbyteArray(env, depthInfo->depthBuffer);
        env->SetObjectField(infoObject, gFields.depthInfo.depthBufferField,
                depthBufferJBArr);
        env->SetIntField(infoObject, gFields.depthInfo.depthMapWidthField,
                depthInfo->depthMapWidth);
        env->SetIntField(infoObject, gFields.depthInfo.depthMapHeightField,
                depthInfo->depthMapHeight);
        jbyteArray depthMapJBArr = stereoBufferToJbyteArray(env, depthInfo->depthMap);
        env->SetObjectField(infoObject, gFields.depthInfo.depthMapField,
                depthMapJBArr);
        jbyteArray debugBufferJBArr = stereoBufferToJbyteArray(env, depthInfo->debugBuffer);
        env->SetObjectField(infoObject, gFields.depthInfo.debugBufferField,
                debugBufferJBArr);
    }

    // release
    if (depthInfo != nullptr) {
        delete depthInfo;
    }

    BufferManager::releaseAll();
    long elapsedTime = Utils::getCurrentTime() - startTime;
    StereoLogI("<native_readStereoDepthInfo> end, elapsed time = %d ms", elapsedTime);
    return infoObject;
}

static jobject com_mediatek_stereo_StereoInfoAccessor_native_readSegmentMaskInfo(
        JNIEnv *env, jobject thiz, jstring filePath) {
    ATRACE_NAME(">>>>JNI-native_readSegmentMaskInfo");
    StereoLogI("<native_readSegmentMaskInfo>");
    if (filePath == nullptr) {
        StereoLogW("<native_readSegmentMaskInfo> filePath is nullptr");
        return nullptr;
    }

    long startTime = Utils::getCurrentTime();
    // readSegmentMaskInfo
    sp<StereoInfoAccessor> accessor = getObject(env, thiz);
    if (accessor == nullptr) {
        StereoLogW("<native_readSegmentMaskInfo><error> can't get accessor");
        return nullptr;
    }
    SegmentMaskInfo* maskInfo = accessor->readSegmentMaskInfo(jstringToString(env, filePath));
    if (maskInfo == nullptr) {
        StereoLogW("<native_readSegmentMaskInfo><error> can't get maskInfo");
        BufferManager::releaseAll();
        return nullptr;
    }

    // Fill out return obj
    jclass clazz;
    FIND_CLASS(clazz, "com/mediatek/accessor/data/SegmentMaskInfo");
    jobject infoObject = nullptr;
    if (clazz) {
        infoObject = env->AllocObject(clazz);
        jstring debugDirJStr = env->NewStringUTF(maskInfo->debugDir.c_str());
        env->SetObjectField(infoObject, gFields.maskInfo.debugDirField, debugDirJStr);
        env->SetIntField(infoObject, gFields.maskInfo.maskWidthField,
                maskInfo->maskWidth);
        env->SetIntField(infoObject, gFields.maskInfo.maskHeightField,
                maskInfo->maskHeight);
        env->SetIntField(infoObject, gFields.maskInfo.segmentXField,
                maskInfo->segmentX);
        env->SetIntField(infoObject, gFields.maskInfo.segmentYField,
                maskInfo->segmentY);
        env->SetIntField(infoObject, gFields.maskInfo.segmentLeftField,
                maskInfo->segmentLeft);
        env->SetIntField(infoObject, gFields.maskInfo.segmentTopField,
                maskInfo->segmentTop);
        env->SetIntField(infoObject, gFields.maskInfo.segmentRightField,
                maskInfo->segmentRight);
        env->SetIntField(infoObject, gFields.maskInfo.segmentBottomField,
                maskInfo->segmentBottom);
        jbyteArray maskBufferJBArr = stereoBufferToJbyteArray(env, maskInfo->maskBuffer);
        env->SetObjectField(infoObject, gFields.maskInfo.maskBufferField,
                maskBufferJBArr);
    }

    // release
    if (maskInfo != nullptr) {
        delete maskInfo;
    }

    BufferManager::releaseAll();
    long elapsedTime = Utils::getCurrentTime() - startTime;
    StereoLogI("<native_readSegmentMaskInfo> end, elapsed time = %d ms", elapsedTime);
    return infoObject;
}

static jobject com_mediatek_stereo_StereoInfoAccessor_native_readStereoBufferInfo(
        JNIEnv *env, jobject thiz, jstring filePath) {
    ATRACE_NAME(">>>>JNI-native_readStereoBufferInfo");
    StereoLogI("<native_readStereoBufferInfo>");
    if (filePath == nullptr) {
        StereoLogW("<native_readStereoBufferInfo> filePath is nullptr");
        return nullptr;
    }

    long startTime = Utils::getCurrentTime();
    // readSegmentMaskInfo
    sp<StereoInfoAccessor> accessor = getObject(env, thiz);
    if (accessor == nullptr) {
        StereoLogW("<native_readStereoBufferInfo><error> can't get accessor");
        return nullptr;
    }
    StereoBufferInfo* bufferInfo = accessor->readStereoBufferInfo(jstringToString(env, filePath));
    if (bufferInfo == nullptr) {
        StereoLogW("<native_readStereoBufferInfo><error> can't get bufferInfo");
        BufferManager::releaseAll();
        return nullptr;
    }

    // Fill out return obj
    jclass clazz;
    FIND_CLASS(clazz, "com/mediatek/accessor/data/StereoBufferInfo");
    jobject infoObject = nullptr;
    if (clazz) {
        infoObject = env->AllocObject(clazz);
        jstring debugDirJStr = env->NewStringUTF(bufferInfo->debugDir.c_str());
        env->SetObjectField(infoObject, gFields.bufferInfo.debugDirField, debugDirJStr);
        jbyteArray jpsBufferJBArr = stereoBufferToJbyteArray(env, bufferInfo->jpsBuffer);
        env->SetObjectField(infoObject, gFields.bufferInfo.jpsBufferField,
                jpsBufferJBArr);
        jbyteArray maskBufferJBArr = stereoBufferToJbyteArray(env, bufferInfo->maskBuffer);
        env->SetObjectField(infoObject, gFields.bufferInfo.maskBufferField,
                maskBufferJBArr);
    }

    // release
    if (bufferInfo != nullptr) {
        delete bufferInfo;
    }

    BufferManager::releaseAll();
    long elapsedTime = Utils::getCurrentTime() - startTime;
    StereoLogI("<native_readStereoBufferInfo> end, elapsed time = %d ms", elapsedTime);
    return infoObject;
}

static jobject com_mediatek_stereo_StereoInfoAccessor_native_readStereoConfigInfo(
        JNIEnv *env, jobject thiz, jstring filePath) {
    ATRACE_NAME(">>>>JNI-native_readStereoConfigInfo");
    StereoLogI("<native_readStereoConfigInfo>");
    if (filePath == nullptr) {
        StereoLogW("<native_readStereoConfigInfo> filePath is nullptr");
        return nullptr;
    }

    long startTime = Utils::getCurrentTime();
    // readStereoConfigInfo
    sp<StereoInfoAccessor> accessor = getObject(env, thiz);
    if (accessor == nullptr) {
        StereoLogW("<native_readStereoConfigInfo><error> can't get accessor");
        return nullptr;
    }
    StereoConfigInfo* configInfo = accessor->readStereoConfigInfo(jstringToString(env, filePath));
    if (configInfo == nullptr) {
        StereoLogW("<native_readStereoConfigInfo><error> can't get configInfo");
        BufferManager::releaseAll();
        return nullptr;
    }

    // Fill out return obj
    jclass clazz;
    FIND_CLASS(clazz, "com/mediatek/accessor/data/StereoConfigInfo");
    jobject infoObject = nullptr;
    if (clazz) {
        infoObject = env->AllocObject(clazz);
        jstring debugDirJStr = env->NewStringUTF(configInfo->debugDir.c_str());
        env->SetObjectField(infoObject, gFields.configInfo.debugDirField, debugDirJStr);
        env->SetIntField(infoObject, gFields.configInfo.jpsWidthField, configInfo->jpsWidth);
        env->SetIntField(infoObject, gFields.configInfo.jpsHeightField, configInfo->jpsHeight);
        env->SetIntField(infoObject, gFields.configInfo.maskWidthField, configInfo->maskWidth);
        env->SetIntField(infoObject, gFields.configInfo.maskHeightField, configInfo->maskHeight);
        env->SetIntField(infoObject, gFields.configInfo.posXField, configInfo->posX);
        env->SetIntField(infoObject, gFields.configInfo.posYField, configInfo->posY);
        env->SetIntField(infoObject, gFields.configInfo.viewWidthField, configInfo->viewWidth);
        env->SetIntField(infoObject, gFields.configInfo.viewHeightField, configInfo->viewHeight);
        env->SetIntField(infoObject, gFields.configInfo.imageOrientationField, configInfo->imageOrientation);
        env->SetIntField(infoObject, gFields.configInfo.depthOrientationField, configInfo->depthOrientation);
        env->SetIntField(infoObject, gFields.configInfo.mainCamPosField, configInfo->mainCamPos);
        env->SetIntField(infoObject, gFields.configInfo.touchCoordX1stField, configInfo->touchCoordX1st);
        env->SetIntField(infoObject, gFields.configInfo.touchCoordY1stField, configInfo->touchCoordY1st);
        env->SetIntField(infoObject, gFields.configInfo.faceCountField, configInfo->faceCount);

        // set focus info
        if (configInfo->focusInfo != nullptr) {
            FIND_CLASS(clazz, "com/mediatek/accessor/data/StereoConfigInfo$FocusInfo");
            jobject focusInfoObj = nullptr;
            if (clazz) {
                focusInfoObj = env->AllocObject(clazz);
                env->SetIntField(focusInfoObj, gFields.focusInfo.focusInfoTypeField, configInfo->focusInfo->focusType);
                env->SetIntField(focusInfoObj, gFields.focusInfo.focusInfoTopField, configInfo->focusInfo->focusTop);
                env->SetIntField(focusInfoObj, gFields.focusInfo.focusInfoLeftField, configInfo->focusInfo->focusLeft);
                env->SetIntField(focusInfoObj, gFields.focusInfo.focusInfoRightField, configInfo->focusInfo->focusRight);
                env->SetIntField(focusInfoObj, gFields.focusInfo.focusInfoBottomField, configInfo->focusInfo->focusBottom);
                env->SetObjectField(infoObject, gFields.configInfo.focusInfoField, focusInfoObj);
            }
        }

        // set fd info array
        if (configInfo->fdInfoArray != nullptr) {
            FIND_CLASS(clazz, "java/util/ArrayList");
            jobject arrayListObj = nullptr;
            if (clazz) {
                // TODO check difference of AllocObject and NewObject
                //arrayListObj = env->AllocObject(clazz);
                arrayListObj = env->NewObject(clazz, gFields.arrayList.initMethod, "");
                FIND_CLASS(clazz, "com/mediatek/accessor/data/StereoConfigInfo$FaceDetectionInfo");
                if (clazz) {
                    for (auto iter = configInfo->fdInfoArray->begin(); iter != configInfo->fdInfoArray->end(); iter++) {
                        FaceDetectionInfo* fdInfo = *iter;
                        // new fdinfo object
                        jobject fdInfoItemObj = env->AllocObject(clazz);
                        env->SetIntField(fdInfoItemObj, gFields.fdInfo.fdInfoFaceLeftField, fdInfo->faceLeft);
                        env->SetIntField(fdInfoItemObj, gFields.fdInfo.fdInfoFaceTopField, fdInfo->faceTop);
                        env->SetIntField(fdInfoItemObj, gFields.fdInfo.fdInfoFaceRightField, fdInfo->faceRight);
                        env->SetIntField(fdInfoItemObj, gFields.fdInfo.fdInfoFaceBottomField, fdInfo->faceBottom);
                        env->SetIntField(fdInfoItemObj, gFields.fdInfo.fdInfoFaceRipField, fdInfo->faceRip);
                        // add to arraylist
                        env->CallBooleanMethod(arrayListObj, gFields.arrayList.addMethod, fdInfoItemObj);
                    }
                }
                env->SetObjectField(infoObject, gFields.configInfo.fdInfoArrayField, arrayListObj);
            }
        }
        env->SetIntField(infoObject, gFields.configInfo.dofLevelField, configInfo->dofLevel);
        env->SetFloatField(infoObject, gFields.configInfo.convOffsetField, configInfo->convOffset);
        env->SetIntField(infoObject, gFields.configInfo.ldcWidthField, configInfo->ldcWidth);
        env->SetIntField(infoObject, gFields.configInfo.ldcHeightField, configInfo->ldcHeight);

        jbyteArray ldcBufferJBArr = stereoBufferToJbyteArray(env, configInfo->ldcBuffer);
        env->SetObjectField(infoObject, gFields.configInfo.ldcBufferField, ldcBufferJBArr);

        jbyteArray clearImageJBArr = stereoBufferToJbyteArray(env, configInfo->clearImage);
        env->SetObjectField(infoObject, gFields.configInfo.clearImageField, clearImageJBArr);
        
        env->SetBooleanField(infoObject, gFields.configInfo.isFaceField, configInfo->isFace);
        env->SetFloatField(infoObject, gFields.configInfo.faceRatioField, configInfo->faceRatio);
        env->SetIntField(infoObject, gFields.configInfo.curDacField, configInfo->curDac);
        env->SetIntField(infoObject, gFields.configInfo.minDacField, configInfo->minDac);
        env->SetIntField(infoObject, gFields.configInfo.maxDacField, configInfo->maxDac);
    }

    // release
    if (configInfo != nullptr) {
        delete configInfo;
    }
    BufferManager::releaseAll();
    long elapsedTime = Utils::getCurrentTime() - startTime;
    StereoLogI("<native_readStereoConfigInfo> end, elapsed time = %d ms", elapsedTime);
    return infoObject;
}


static jobject com_mediatek_stereo_StereoInfoAccessor_native_readGoogleStereoInfo(
        JNIEnv *env, jobject thiz, jstring filePath) {
    ATRACE_NAME(">>>>JNI-native_readGoogleStereoInfo");
    StereoLogI("<native_readGoogleStereoInfo>");
    if (filePath == nullptr) {
        StereoLogW("<native_readGoogleStereoInfo> filePath is nullptr");
        return nullptr;
    }

    long startTime = Utils::getCurrentTime();
    // readGoogleStereoInfo
    sp<StereoInfoAccessor> accessor = getObject(env, thiz);
    if (accessor == nullptr) {
        StereoLogW("<native_readGoogleStereoInfo><error> can't get accessor");
        return nullptr;
    }
    GoogleStereoInfo* googleInfo = accessor->readGoogleStereoInfo(jstringToString(env, filePath));
    if (googleInfo == nullptr) {
        StereoLogW("<native_readGoogleStereoInfo><error> can't get googleInfo");
        BufferManager::releaseAll();
        return nullptr;
    }

    // Fill out return obj
    jclass clazz;
    FIND_CLASS(clazz, "com/mediatek/accessor/data/GoogleStereoInfo");
    jobject infoObject = nullptr;
    if (clazz) {
        infoObject = env->AllocObject(clazz);
        jstring debugDirJStr = env->NewStringUTF(googleInfo->debugDir.c_str());
        env->SetObjectField(infoObject, gFields.googleInfo.debugDirField, debugDirJStr);
        env->SetDoubleField(infoObject, gFields.googleInfo.focusBlurAtInfinityField,
                googleInfo->focusBlurAtInfinity);
        env->SetDoubleField(infoObject, gFields.googleInfo.focusFocalDistanceField,
                googleInfo->focusFocalDistance);
        env->SetDoubleField(infoObject, gFields.googleInfo.focusFocalPointXField,
                googleInfo->focusFocalPointX);
        env->SetDoubleField(infoObject, gFields.googleInfo.focusFocalPointYField,
                googleInfo->focusFocalPointY);
        jstring imageMimeJStr = env->NewStringUTF(googleInfo->imageMime.c_str());
        env->SetObjectField(infoObject, gFields.googleInfo.imageMimeField, imageMimeJStr);
        jstring depthFormatJStr = env->NewStringUTF(googleInfo->depthFormat.c_str());
        env->SetObjectField(infoObject, gFields.googleInfo.depthFormatField, depthFormatJStr);
        env->SetDoubleField(infoObject, gFields.googleInfo.depthNearField, googleInfo->depthNear);
        env->SetDoubleField(infoObject, gFields.googleInfo.depthFarField, googleInfo->depthFar);
        jstring depthMimeJStr = env->NewStringUTF(googleInfo->depthMime.c_str());
        env->SetObjectField(infoObject, gFields.googleInfo.depthMimeField, depthMimeJStr);
        jbyteArray clearImageJBArr = stereoBufferToJbyteArray(env, googleInfo->clearImage);
        env->SetObjectField(infoObject, gFields.googleInfo.clearImageField, clearImageJBArr);
        jbyteArray depthMapJBArr = stereoBufferToJbyteArray(env, googleInfo->depthMap);
        env->SetObjectField(infoObject, gFields.googleInfo.depthMapField, depthMapJBArr);
    }

    // release
    if (googleInfo != nullptr) {
        delete googleInfo;
    }
    BufferManager::releaseAll();
    long elapsedTime = Utils::getCurrentTime() - startTime;
    StereoLogI("<native_readGoogleStereoInfo> end, elapsed time = %d ms", elapsedTime);
    return infoObject;
}

static jint com_mediatek_stereo_StereoInfoAccessor_native_getGeoVerifyLevel(
        JNIEnv *env, jobject thiz, jbyteArray bufferJBArr) {
    ATRACE_NAME(">>>>JNI-native_getGeoVerifyLevel");
    StereoLogI("<native_getGeoVerifyLevel>");
    if (bufferJBArr == nullptr) {
        StereoLogW("<native_getGeoVerifyLevel> buffer is nullptr");
        return -1;
    }

    // getGeoVerifyLevel
    sp<StereoInfoAccessor> accessor = getObject(env, thiz);
    if (accessor == nullptr) {
        StereoLogW("<native_getGeoVerifyLevel><error> can't get accessor");
        return -1;
    }

    StereoBuffer_t configBuffer;
    jbyteArrayToStereoBuffer(env, bufferJBArr, configBuffer);
    int level = accessor->getGeoVerifyLevel(configBuffer);

    // release
    BufferManager::releaseAll();
    StereoLogI("<native_getGeoVerifyLevel> end");
    return level;
}

static jint com_mediatek_stereo_StereoInfoAccessor_native_getPhoVerifyLevel(
    JNIEnv *env, jobject thiz, jbyteArray bufferJBArr) {
    ATRACE_NAME(">>>>JNI-native_getPhoVerifyLevel");
    StereoLogI("<native_getPhoVerifyLevel>");
    if (bufferJBArr == nullptr) {
        StereoLogW("<native_getPhoVerifyLevel> buffer is nullptr");
        return -1;
    }

    // getPhoVerifyLevel
    sp<StereoInfoAccessor> accessor = getObject(env, thiz);
    if (accessor == nullptr) {
        StereoLogW("<native_getPhoVerifyLevel><error> can't get accessor");
        return -1;
    }

    StereoBuffer_t configBuffer;
    jbyteArrayToStereoBuffer(env, bufferJBArr, configBuffer);
    int level = accessor->getPhoVerifyLevel(configBuffer);

    // release
    BufferManager::releaseAll();
    StereoLogI("<native_getPhoVerifyLevel> end");
    return level;
}

static jint com_mediatek_stereo_StereoInfoAccessor_native_getMtkChaVerifyLevel(
        JNIEnv *env, jobject thiz, jbyteArray bufferJBArr) {
    ATRACE_NAME(">>>>JNI-native_getMtkChaVerifyLevel");
    StereoLogI("<native_getMtkChaVerifyLevel>");
    if (bufferJBArr == nullptr) {
        StereoLogW("<native_getMtkChaVerifyLevel> buffer is nullptr");
        return -1;
    }

    // getMtkChaVerifyLevel
    sp<StereoInfoAccessor> accessor = getObject(env, thiz);
    if (accessor == nullptr) {
        StereoLogW("<native_getMtkChaVerifyLevel><error> can't get accessor");
        return -1;
    }

    StereoBuffer_t configBuffer;
    jbyteArrayToStereoBuffer(env, bufferJBArr, configBuffer);
    int level = accessor->getMtkChaVerifyLevel(configBuffer);

    BufferManager::releaseAll();
    StereoLogI("<native_getMtkChaVerifyLevel> end");
    return level;
}

static const JNINativeMethod gMethods[] = {
    {"native_init", "()V",
        (void *)com_mediatek_stereo_StereoInfoAccessor_native_init},

    {"native_setup", "()V",
        (void *)com_mediatek_stereo_StereoInfoAccessor_native_setup},

    {"native_finalize", "()V",
        (void *)com_mediatek_stereo_StereoInfoAccessor_native_finalize},

    {"native_writeStereoCaptureInfo",
        "(Lcom/mediatek/accessor/data/StereoCaptureInfo;)[B", 
        (void *)com_mediatek_stereo_StereoInfoAccessor_native_writeStereoCaptureInfo},

    {"native_writeStereoDepthInfo",
        "(Ljava/lang/String;Lcom/mediatek/accessor/data/StereoDepthInfo;)V", 
        (void *)com_mediatek_stereo_StereoInfoAccessor_native_writeStereoDepthInfo},

    {"native_writeSegmentMaskInfo",
        "(Ljava/lang/String;Lcom/mediatek/accessor/data/SegmentMaskInfo;)V", 
        (void *)com_mediatek_stereo_StereoInfoAccessor_native_writeSegmentMaskInfo},

    {"native_writeRefocusImage",
        "(Ljava/lang/String;Lcom/mediatek/accessor/data/StereoConfigInfo;[B)V", 
        (void *)com_mediatek_stereo_StereoInfoAccessor_native_writeRefocusImage},

    {"native_readStereoDepthInfo",
        "(Ljava/lang/String;)Lcom/mediatek/accessor/data/StereoDepthInfo;", 
        (void *)com_mediatek_stereo_StereoInfoAccessor_native_readStereoDepthInfo},

    {"native_readSegmentMaskInfo",
        "(Ljava/lang/String;)Lcom/mediatek/accessor/data/SegmentMaskInfo;", 
        (void *)com_mediatek_stereo_StereoInfoAccessor_native_readSegmentMaskInfo},

    {"native_readStereoBufferInfo",
        "(Ljava/lang/String;)Lcom/mediatek/accessor/data/StereoBufferInfo;", 
        (void *)com_mediatek_stereo_StereoInfoAccessor_native_readStereoBufferInfo},

    {"native_readStereoConfigInfo",
        "(Ljava/lang/String;)Lcom/mediatek/accessor/data/StereoConfigInfo;", 
        (void *)com_mediatek_stereo_StereoInfoAccessor_native_readStereoConfigInfo},

    {"native_readGoogleStereoInfo",
        "(Ljava/lang/String;)Lcom/mediatek/accessor/data/GoogleStereoInfo;", 
        (void *)com_mediatek_stereo_StereoInfoAccessor_native_readGoogleStereoInfo},

    {"native_getGeoVerifyLevel","([B)I", 
        (void *)com_mediatek_stereo_StereoInfoAccessor_native_getGeoVerifyLevel},

    {"native_getPhoVerifyLevel","([B)I", 
        (void *)com_mediatek_stereo_StereoInfoAccessor_native_getPhoVerifyLevel},

    {"native_getMtkChaVerifyLevel","([B)I", 
        (void *)com_mediatek_stereo_StereoInfoAccessor_native_getMtkChaVerifyLevel}
};

int register_com_mediatek_stereo_StereoInfoAccessor(JNIEnv *env) {
    return AndroidRuntime::registerNativeMethods(env,
            "com/mediatek/accessor/StereoInfoAccessor_Native", gMethods, NELEM(gMethods));
}

jint JNI_OnLoad(JavaVM* vm, void* /* reserved */) {
    JNIEnv* env = nullptr;
    jint result = -1;

    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        StereoLogE("ERROR: GetEnv failed\n");
        return result;
    }

    if (register_com_mediatek_stereo_StereoInfoAccessor(env) < 0) {
        StereoLogE("ERROR: native registration failed\n");
        return result;
    }

    return JNI_VERSION_1_4;
}

