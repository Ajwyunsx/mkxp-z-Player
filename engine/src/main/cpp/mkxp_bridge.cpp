#include <jni.h>
#include <android/log.h>

#include <mutex>
#include <string>

namespace {
std::mutex g_error_mutex;
std::string g_last_error;

void set_last_error(const std::string& message) {
    std::lock_guard<std::mutex> lock(g_error_mutex);
    g_last_error = message;
}
}

extern "C" JNIEXPORT jint JNICALL
Java_io_github_mkxpz_engine_MkxpNative_nativeStart(
    JNIEnv* env,
    jobject /* thiz */,
    jstring config_path,
    jstring game_path,
    jboolean debug) {

    const char* config = env->GetStringUTFChars(config_path, nullptr);
    const char* game = env->GetStringUTFChars(game_path, nullptr);

    __android_log_print(
        ANDROID_LOG_INFO,
        "mkxpz_bridge",
        "mkxp-z bridge invoked. config=%s game=%s debug=%s",
        config,
        game,
        debug == JNI_TRUE ? "true" : "false");

    env->ReleaseStringUTFChars(config_path, config);
    env->ReleaseStringUTFChars(game_path, game);

    set_last_error(
        "JNI bridge is installed, but the full mkxp-z Android runtime is not linked in this build. "
        "Place the SDL/mkxp-z runtime behind this nativeStart boundary.");
    return -1;
}

extern "C" JNIEXPORT jstring JNICALL
Java_io_github_mkxpz_engine_MkxpNative_nativeLastError(JNIEnv* env, jobject /* thiz */) {
    std::lock_guard<std::mutex> lock(g_error_mutex);
    return env->NewStringUTF(g_last_error.c_str());
}
