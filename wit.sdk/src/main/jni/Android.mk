LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := witvad
LOCAL_SRC_FILES := WITCvad.c
LOCAL_SRC_FILES += WitVadWrapper.c

include $(BUILD_SHARED_LIBRARY)
