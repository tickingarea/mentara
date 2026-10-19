#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "WhisperTest"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_learnagent_core_WhisperJNI_initContext(JNIEnv *env, jobject thiz, jstring modelPath) {
    LOGI("WhisperJNI_initContext called with model: %s", modelPath ? "model provided" : "null");
    return (jlong)12345; // 返回模拟的 context 指针
}

JNIEXPORT jstring JNICALL
Java_com_learnagent_core_WhisperJNI_transcribeFile(JNIEnv *env, jobject thiz, jlong ctx, jstring wavPath, jstring language) {
    LOGI("WhisperJNI_transcribeFile called with ctx: %ld, wav: %s, lang: %s", 
         ctx, wavPath ? "wav provided" : "null", language ? language : "auto");
    
    // 返回模拟的 JSON 结果
    const char* jsonResult = "[{\"start\":0.0,\"end\":5.0,\"text\":\"这是测试转录文本\"}]";
    return env->NewStringUTF(jsonResult);
}

JNIEXPORT void JNICALL
Java_com_learnagent_core_WhisperJNI_freeContext(JNIEnv *env, jobject thiz, jlong ctx) {
    LOGI("WhisperJNI_freeContext called with ctx: %ld", ctx);
}

JNIEXPORT void JNICALL
Java_com_learnagent_core_WhisperJNI_setProgressCallback(JNIEnv *env, jobject thiz, jobject callback, jobject user_data) {
    LOGI("WhisperJNI_setProgressCallback called");
}

}