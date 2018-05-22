LOCAL_PATH:= $(call my-dir)

# =========================================
# shared library

include $(CLEAR_VARS)

SOURCE_FILES := $(wildcard $(LOCAL_PATH)/XMPCore/source/*.cpp)
SOURCE_FILES += $(wildcard $(LOCAL_PATH)/source/*.cpp)
SOURCE_FILES += $(wildcard $(LOCAL_PATH)/third-party/zuid/interfaces/*.cpp)
SOURCE_FILES += $(wildcard $(LOCAL_PATH)/test/*.cpp)

LOCAL_SRC_FILES := $(SOURCE_FILES:$(LOCAL_PATH)/%=%)

LOCAL_C_INCLUDES +=                             \
    $(LOCAL_PATH)/public/include                \
    $(LOCAL_PATH)/public/include/client-glue    \
    $(LOCAL_PATH)/source/                       \
    $(LOCAL_PATH)/third-party/zuid/interfaces/  \
    $(LOCAL_PATH)/XMPCore/source/               \
    $(LOCAL_PATH)/XMPCore/resource/linux/       \
    $(LOCAL_PATH)/build/                        \
    $(LOCAL_PATH)/test/                         \
    $(TOP)/external/zlib/                       \
    $(TOP)/external/expat/lib
    
LOCAL_CFLAGS += -DHAVE_MEMMOVE -DUNIX_ENV -DSTDC -fexceptions

APP_STL := stlport_shared

LOCAL_SHARED_LIBRARIES := libexpat libz libcutils liblog

LOCAL_MODULE := libxmp
include $(BUILD_SHARED_LIBRARY)

# =========================================
# static library

include $(CLEAR_VARS)

SOURCE_FILES := $(wildcard $(LOCAL_PATH)/XMPCore/source/*.cpp)
SOURCE_FILES += $(wildcard $(LOCAL_PATH)/source/*.cpp)
SOURCE_FILES += $(wildcard $(LOCAL_PATH)/third-party/zuid/interfaces/*.cpp)
SOURCE_FILES += $(wildcard $(LOCAL_PATH)/test/*.cpp)

LOCAL_SRC_FILES := $(SOURCE_FILES:$(LOCAL_PATH)/%=%)

LOCAL_C_INCLUDES +=                             \
    $(LOCAL_PATH)/public/include                \
    $(LOCAL_PATH)/public/include/client-glue    \
    $(LOCAL_PATH)/source/                       \
    $(LOCAL_PATH)/third-party/zuid/interfaces/  \
    $(LOCAL_PATH)/XMPCore/source/               \
    $(LOCAL_PATH)/XMPCore/resource/linux/       \
    $(LOCAL_PATH)/build/                        \
    $(LOCAL_PATH)/test/                         \
    $(TOP)/external/zlib/                       \
    $(TOP)/external/expat/lib

LOCAL_CFLAGS += -DHAVE_MEMMOVE -DUNIX_ENV -DSTDC -fexceptions

APP_STL := stlport_shared

LOCAL_SHARED_LIBRARIES := libexpat libz libcutils liblog

LOCAL_MODULE := libxmp
include $(BUILD_STATIC_LIBRARY)

# =========================================
# test binary
ifeq ($(XMP_PERFORMANCE_TEST), yes)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := test/PerformanceTest.cpp

LOCAL_C_INCLUDES +=                             \
    $(LOCAL_PATH)/public/include                \
    $(LOCAL_PATH)/public/include/client-glue    \
    $(LOCAL_PATH)/test/                         \

LOCAL_CFLAGS += -DHAVE_MEMMOVE -DUNIX_ENV -DSTDC -fexceptions -std=c99

LOCAL_SHARED_LIBRARIES := libxmp libcutils
LOCAL_MODULE := xmp_test
include $(BUILD_EXECUTABLE)

endif
