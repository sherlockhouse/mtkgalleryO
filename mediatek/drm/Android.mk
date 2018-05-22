ifeq ($(strip $(MTK_OMADRM_SUPPORT)), yes)
$(info "MTK_OMADRM_SUPPORT=$(MTK_OMADRM_SUPPORT)")

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_PATH := $(PRODUCT_OUT)/$(TARGET_COPY_OUT_VENDOR)/etc/gallery
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_JAVA_LIBRARIES := com.mediatek.gallerybasic
LOCAL_JAVA_LIBRARIES += mediatek-framework
LOCAL_STATIC_JAVA_LIBRARIES += com.mediatek.omadrm.common

LOCAL_PACKAGE_NAME := Gallery2Drm

LOCAL_CERTIFICATE := platform

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

include $(BUILD_PACKAGE)

endif
