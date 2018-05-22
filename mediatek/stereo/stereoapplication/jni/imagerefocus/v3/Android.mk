MY_LOCAL_PATH := $(call my-dir)

# NOTE: please DO NOT include $(CLEAR_VARS)

include $(call all-makefiles-under, $(MY_LOCAL_PATH))
