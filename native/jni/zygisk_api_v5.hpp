// Minimal ABI-compatible subset of Magisk Zygisk API v5.
// It mirrors the public zygisk.hpp layout used by this diagnostic module.
#pragma once

#include <jni.h>
#include <sys/types.h>

#define ZYGISK_API_VERSION 5

namespace zygisk {

struct Api;
struct AppSpecializeArgs;
struct ServerSpecializeArgs;

class ModuleBase {
public:
    virtual void onLoad(Api*, JNIEnv*) {}
    virtual void preAppSpecialize(AppSpecializeArgs*) {}
    virtual void postAppSpecialize(const AppSpecializeArgs*) {}
    virtual void preServerSpecialize(ServerSpecializeArgs*) {}
    virtual void postServerSpecialize(const ServerSpecializeArgs*) {}
};

struct AppSpecializeArgs {
    jint &uid;
    jint &gid;
    jintArray &gids;
    jint &runtime_flags;
    jobjectArray &rlimits;
    jint &mount_external;
    jstring &se_info;
    jstring &nice_name;
    jstring &instruction_set;
    jstring &app_data_dir;
    jintArray *const fds_to_ignore;
    jboolean *const is_child_zygote;
    jboolean *const is_top_app;
    jobjectArray *const pkg_data_info_list;
    jobjectArray *const whitelisted_data_info_list;
    jboolean *const mount_data_dirs;
    jboolean *const mount_storage_dirs;
    jboolean *const mount_sysprop_overrides;
};

struct ServerSpecializeArgs;

enum Option : int {
    FORCE_DENYLIST_UNMOUNT = 0,
    DLCLOSE_MODULE_LIBRARY = 1,
};

namespace internal {
struct api_table;
template <class T> void entry_impl(api_table*, JNIEnv*);
}

struct Api {
    void setOption(Option option);
private:
    internal::api_table *table_ = nullptr;
    template <class T> friend void internal::entry_impl(internal::api_table*, JNIEnv*);
};

namespace internal {

struct module_abi {
    long api_version;
    ModuleBase *impl;
    void (*preAppSpecialize)(ModuleBase*, AppSpecializeArgs*);
    void (*postAppSpecialize)(ModuleBase*, const AppSpecializeArgs*);
    void (*preServerSpecialize)(ModuleBase*, ServerSpecializeArgs*);
    void (*postServerSpecialize)(ModuleBase*, const ServerSpecializeArgs*);

    explicit module_abi(ModuleBase *module) : api_version(ZYGISK_API_VERSION), impl(module) {
        preAppSpecialize = [](auto module, auto args) { module->preAppSpecialize(args); };
        postAppSpecialize = [](auto module, auto args) { module->postAppSpecialize(args); };
        preServerSpecialize = [](auto module, auto args) { module->preServerSpecialize(args); };
        postServerSpecialize = [](auto module, auto args) { module->postServerSpecialize(args); };
    }
};

struct api_table {
    void *impl;
    bool (*registerModule)(api_table*, module_abi*);
    void (*hookJniNativeMethods)(JNIEnv*, const char*, JNINativeMethod*, int);
    void (*pltHookRegister)(dev_t, ino_t, const char*, void*, void**);
    bool (*exemptFd)(int);
    bool (*pltHookCommit)();
    int (*connectCompanion)(void*);
    void (*setOption)(void*, Option);
};

template <class T>
void entry_impl(api_table *table, JNIEnv *env) {
    static Api api;
    api.table_ = table;
    static T module;
    static module_abi abi(&module);
    if (table->registerModule(table, &abi)) module.onLoad(&api, env);
}

} // namespace internal

inline void Api::setOption(Option option) {
    if (table_->setOption) table_->setOption(table_->impl, option);
}

} // namespace zygisk

#define REGISTER_ZYGISK_MODULE(clazz) \
extern "C" __attribute__((visibility("default"))) void zygisk_module_entry( \
    zygisk::internal::api_table *table, JNIEnv *env) { \
    zygisk::internal::entry_impl<clazz>(table, env); \
}
