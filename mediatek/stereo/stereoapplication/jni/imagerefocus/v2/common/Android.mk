MY_LOCAL_PATH := $(call my-dir)

# NOTE: please DO NOT include $(CLEAR_VARS)
# please DO NOT MODIFY OTHER var except LOCAL_C_INCLUDES & LOCAL_SRC_FILES

# NOTE: imagerefocus/Android.mk will include all mkfile under v2,v3,..
# so only modify LOCAL_C_INCLUDES & LOCAL_SRC_FILES of current platform
# e.g. MT6755<->V2, so add ifeq condition.

ifeq ($(MTK_PLATFORM), MT6755)

$(warning >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>)
$(warning >Enter "imagerefocus/v2/common", MTK_PLATFORM-$(MTK_PLATFORM))
$(warning >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>)

# include algo header and lib
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/legacy/platform/$(shell echo $(MTK_PLATFORM) | tr '[A-Z]' '[a-z]')/include/mtkcam/algorithm/librefocus
LOCAL_SHARED_LIBRARIES += libcamalgo

# power hint lib
LOCAL_SHARED_LIBRARIES += \
    libhidlbase \
    libhidltransport \
    libhwbinder \
    android.hardware.power@1.0 \
    vendor.mediatek.hardware.power@1.1_vendor

# include header files, e.g.ImageRefocusPerf.h/RefocusConfigInfoWrapper.h
# LOCAL_C_INCLUDES is full path
LOCAL_C_INCLUDES += $(MY_LOCAL_PATH)

# include src files
# NOTE: LOCAL_SRC_FILES is related path, need remove parent path(LOCAL_PATH) from MY_LOCAL_PATH
# e.g. /v2/common/ImageRefocusPerf.cpp
# e.g. /v2/common/RefocusConfigInfoWrapper.cpp
LOCAL_SRC_FILES += $(subst $(LOCAL_PATH),,$(MY_LOCAL_PATH))/ImageRefocusPerf.cpp
LOCAL_SRC_FILES += $(subst $(LOCAL_PATH),,$(MY_LOCAL_PATH))/RefocusConfigInfoWrapper.cpp
endif
