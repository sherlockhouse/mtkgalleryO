LOCAL_PATH := $(call my-dir)
ifeq ($(MTK_CAM_IMAGE_REFOCUS_SUPPORT),yes)
$(info "build all stereo modules")
include $(call all-makefiles-under, $(LOCAL_PATH))
else
$(info "build stereoinfoaccessor")
include $(LOCAL_PATH)/stereoinfoaccessor/Android.mk
endif
