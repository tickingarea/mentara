#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "WhisperJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

// Mock implementation for testing - Replace with real whisper.cpp integration

JNIEXPORT jlong JNICALL
Java_com_learnagent_core_WhisperJNI_initContext(JNIEnv *env, jobject thiz, jstring modelPath) {
    const char* modelPathC = modelPath ? env->GetStringUTFChars(modelPath, nullptr) : nullptr;
    if (modelPathC) {
        env->ReleaseStringUTFChars(modelPath, modelPathC);
    }
    LOGI("WhisperJNI_initContext called with model: %s", modelPathC ? modelPathC : "null");
    return (jlong)12345; // Return mock context pointer
}

JNIEXPORT jstring JNICALL
Java_com_learnagent_core_WhisperJNI_transcribeFile(JNIEnv *env, jobject thiz, jlong ctx, jstring wavPath, jstring language) {
    const char* wavPathC = wavPath ? env->GetStringUTFChars(wavPath, nullptr) : nullptr;
    const char* languageC = language ? env->GetStringUTFChars(language, nullptr) : nullptr;
    
    if (wavPathC) {
        env->ReleaseStringUTFChars(wavPath, wavPathC);
    }
    if (languageC) {
        env->ReleaseStringUTFChars(language, languageC);
    }
    
    LOGI("WhisperJNI_transcribeFile called with ctx: %ld, wav: %s, lang: %s", 
         ctx, wavPathC ? wavPathC : "null", languageC ? languageC : "auto");
    
    // Return mock transcription result
    const char* jsonResult = "[{\"start\":0.0,\"end\":5.0,\"text\":\"This is a mock transcription result for testing purposes.\"}]";
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
