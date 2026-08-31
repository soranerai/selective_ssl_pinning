LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := selective_webview_ca
LOCAL_SRC_FILES := diagnostic_payload.cpp
LOCAL_LDLIBS := -llog
LOCAL_CPP_FEATURES := exceptions rtti
include $(BUILD_SHARED_LIBRARY)
