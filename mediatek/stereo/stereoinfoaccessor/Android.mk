LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_STATIC_JAVA_LIBRARIES += xmp_toolkit

LOCAL_SRC_FILES := $(call all-java-files-under, java)

LOCAL_MODULE := stereo_info_accessor

include $(BUILD_STATIC_JAVA_LIBRARY)

include $(call all-makefiles-under,$(LOCAL_PATH))
