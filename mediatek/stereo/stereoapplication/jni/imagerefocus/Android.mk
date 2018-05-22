# Copyright Statement:
#
# This software/firmware and related documentation ("MediaTek Software") are
# protected under relevant copyright laws. The information contained herein
# is confidential and proprietary to MediaTek Inc. and/or its licensors.
# Without the prior written permission of MediaTek inc. and/or its licensors,
# any reproduction, modification, use or disclosure of MediaTek Software,
# and information contained herein, in whole or in part, shall be strictly prohibited.

# MediaTek Inc. (C) 2015. All rights reserved.
#
# BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
# THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
# RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
# AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
# EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
# NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
# SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
# SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
# THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
# THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
# CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
# SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
# STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
# CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
# AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
# OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
# MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
#
# The following software/firmware and/or related documentation ("MediaTek Software")
# have been modified by MediaTek Inc. All revisions are subject to any receiver's
# applicable license agreements with MediaTek Inc.

LOCAL_PATH:= $(call my-dir)

# clear variables again to avoid definition of LOCAL_BUILD_MODULE
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_ARM_MODE := arm
CFLAGS+= -fno-strict-aliasing -Os -DNEON_OPT

LOCAL_CFLAGS += -g -fno-omit-frame-pointer -fno-inline -DNEON_OPT

LOCAL_C_INCLUDES += \
    $(JNI_H_INCLUDE) \
    $(LOCAL_PATH)/..\
    $(LOCAL_PATH)/../featurecreator \
    $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/legacy/platform/$(shell echo $(MTK_PLATFORM) | tr '[A-Z]' '[a-z]')/include/mtkcam/algorithm/libcore \
    $(TOP)/$(MTK_PATH_SOURCE)/hardware/mtkcam/legacy/platform/$(shell echo $(MTK_PLATFORM) | tr '[A-Z]' '[a-z]')/include/mtkcam/algorithm/libutility \
    $(TOP)/frameworks/base/core/jni \
    $(TOP)/frameworks/base/core/jni/android/graphics \

LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/jpeg/include
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_SOURCE)/hardware/m4u/$(shell echo $(MTK_PLATFORM) | tr '[A-Z]' '[a-z]')

# JpegDecoder: use ion instead of M4U
LOCAL_C_INCLUDES += $(TOP)/system/core/libion/include
LOCAL_C_INCLUDES += $(MTK_PATH_SOURCE)/external/include
LOCAL_C_INCLUDES += $(MTK_PATH_SOURCE)/external

# Set graphic buffer yuv range
LOCAL_C_INCLUDES += $(TOP)/vendor/mediatek/proprietary/hardware/gralloc_extra/include/ui

# All of the source files that we will compile.
LOCAL_SRC_FILES:= \
  ImageRefocus.cpp\
  JniImageRefocus.cpp\
  RefocusCreator.cpp \
  ../JpegCodec.cpp\
  ../DebugHelper.cpp\
  ../StereoUtils.cpp\
  TuningInfoProvider.cpp

LOCAL_SHARED_LIBRARIES := \
    libutils \
    libcutils \
    libjnigraphics\
    libJpgEncPipe\
    libmhalImageCodec\
    libdpframework\
    libandroid_runtime \
    libui \
    libion\
    libion_mtk\
    liblog

LOCAL_SHARED_LIBRARIES += libmhalImageCodec
LOCAL_SHARED_LIBRARIES += libgralloc_extra

# This is the target being built.
LOCAL_MODULE:= libjni_imagerefocus
#LOCAL_MODULE_RELATIVE_PATH := stereo

# NOTE: DO NOT DELETE
# compatibility: include mkfile under V2/V3/Vn
include $(call all-makefiles-under, $(LOCAL_PATH))

include $(BUILD_SHARED_LIBRARY)