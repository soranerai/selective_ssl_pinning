#include <android/log.h>
#include <jni.h>
#include "zygisk_api_v5.hpp"

#include <cstring>

namespace {
constexpr char kTag[] = "SelectiveWebViewCA";
constexpr char kChromePackage[] = "com.android.chrome";

const char* getString(JNIEnv *env, jstring value) {
    return value == nullptr ? "<none>" : env->GetStringUTFChars(value, nullptr);
}

void releaseString(JNIEnv *env, jstring value, const char *text) {
    if (value != nullptr && text != nullptr) env->ReleaseStringUTFChars(value, text);
}

bool isChromeProcess(const char *name) {
    const size_t prefix = sizeof(kChromePackage) - 1;
    return name != nullptr && std::strncmp(name, kChromePackage, prefix) == 0 &&
        (name[prefix] == '\0' || name[prefix] == ':');
}
}

class ChromeDiagnosticModule final : public zygisk::ModuleBase {
public:
    void onLoad(zygisk::Api *api, JNIEnv *env) override {
        api_ = api;
        env_ = env;
    }

    void preAppSpecialize(zygisk::AppSpecializeArgs *args) override {
        const char *name = getString(env_, args->nice_name);
        target_ = isChromeProcess(name);
        if (target_) {
            __android_log_print(ANDROID_LOG_INFO, kTag, "zygisk pre process=%s uid=%d", name, args->uid);
        } else {
            api_->setOption(zygisk::DLCLOSE_MODULE_LIBRARY);
        }
        releaseString(env_, args->nice_name, name);
    }

    void postAppSpecialize(const zygisk::AppSpecializeArgs *args) override {
        if (!target_) return;
        const char *name = getString(env_, args->nice_name);
        __android_log_print(ANDROID_LOG_INFO, kTag, "zygisk post process=%s uid=%d", name, args->uid);
        releaseString(env_, args->nice_name, name);
    }

private:
    zygisk::Api *api_ = nullptr;
    JNIEnv *env_ = nullptr;
    bool target_ = false;
};

REGISTER_ZYGISK_MODULE(ChromeDiagnosticModule)
