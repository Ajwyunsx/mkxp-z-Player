#include <android/log.h>
#include <dlfcn.h>
#include <jni.h>
#include <unistd.h>

#include <string>
#include <vector>

namespace {
constexpr const char *kLogTag = "MkxpZInnoCli";

using InnoMain = int (*)(int, char **);

std::string toString(JNIEnv *env, jstring value) {
    if (value == nullptr) {
        return {};
    }
    const char *chars = env->GetStringUTFChars(value, nullptr);
    if (chars == nullptr) {
        return {};
    }
    std::string result(chars);
    env->ReleaseStringUTFChars(value, chars);
    return result;
}
} // namespace

extern "C" JNIEXPORT jint JNICALL
Java_io_github_mkxpz_rpgplayer_domain_NativeInnoExtractor_extract(
    JNIEnv *env,
    jobject,
    jstring installerPath,
    jstring destinationPath
) {
    std::string installer = toString(env, installerPath);
    std::string destination = toString(env, destinationPath);
    if (installer.empty() || destination.empty()) {
        return -1;
    }

    void *handle = dlopen("libinnoextract.so", RTLD_NOW);
    if (handle == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, kLogTag, "dlopen libinnoextract.so failed: %s", dlerror());
        return -2;
    }

    auto mainFn = reinterpret_cast<InnoMain>(dlsym(handle, "main"));
    if (mainFn == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, kLogTag, "dlsym main failed: %s", dlerror());
        return -3;
    }

    std::vector<std::string> args = {
        "innoextract",
        "-d",
        destination,
        installer,
    };
    std::vector<char *> argv;
    argv.reserve(args.size() + 1);
    for (auto &arg : args) {
        argv.push_back(arg.data());
    }
    argv.push_back(nullptr);

    optind = 1;
    int result = mainFn(static_cast<int>(args.size()), argv.data());
    __android_log_print(ANDROID_LOG_INFO, kLogTag, "innoextract returned %d for %s", result, installer.c_str());
    return result;
}
