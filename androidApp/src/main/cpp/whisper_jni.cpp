#include <jni.h>
#include <atomic>
#include <string>
#include <vector>
#include <cstdio>
#include <cstring>
#include <cstdint>
#include "whisper.h"

static std::atomic<bool> g_abort_requested{false};

static std::string toStr(JNIEnv* env, jstring s) {
    if (!s) return std::string();
    const char* c = env->GetStringUTFChars(s, nullptr);
    std::string r(c ? c : "");
    if (c) env->ReleaseStringUTFChars(s, c);
    return r;
}

// 模型缓存（同一模型只加载一次；进程退出自动释放）
static whisper_context* g_ctx = nullptr;
static std::string g_model_path;

static whisper_context* getCtx(const std::string& model) {
    if (g_ctx && g_model_path == model) return g_ctx;
    if (g_ctx) { whisper_free(g_ctx); g_ctx = nullptr; g_model_path.clear(); }
    whisper_context_params cp = whisper_context_default_params();
    cp.use_gpu = false;
    g_ctx = whisper_init_from_file_with_params(model.c_str(), cp);
    if (g_ctx) g_model_path = model;
    return g_ctx;
}

extern "C" JNIEXPORT void JNICALL
Java_com_learnagent_asr_WhisperNative_releaseNative(JNIEnv*, jobject) {
    if (g_ctx) { whisper_free(g_ctx); g_ctx = nullptr; }
    g_model_path.clear();
}

static std::string jsonEscape(const std::string& s) {
    std::string o;
    o.reserve(s.size() + 8);
    for (unsigned char ch : s) {
        switch (ch) {
            case '"': o += "\\\""; break;
            case '\\': o += "\\\\"; break;
            case '\n': o += "\\n"; break;
            case '\r': o += "\\r"; break;
            case '\t': o += "\\t"; break;
            default:
                if (ch < 0x20) {
                    char b[8];
                    snprintf(b, sizeof(b), "\\u%04x", ch);
                    o += b;
                } else {
                    o += (char) ch;
                }
        }
    }
    return o;
}

// 解析 16-bit PCM WAV；多声道自动混为单声道；输出 [-1,1] float
static bool readWav(const std::string& path, std::vector<float>& out) {
    FILE* f = fopen(path.c_str(), "rb");
    if (!f) return false;
    fseek(f, 0, SEEK_END);
    long sz = ftell(f);
    fseek(f, 0, SEEK_SET);
    if (sz < 44) { fclose(f); return false; }
    std::vector<unsigned char> buf((size_t) sz);
    if (fread(buf.data(), 1, (size_t) sz, f) != (size_t) sz) { fclose(f); return false; }
    fclose(f);

    int pos = 12;
    int channels = 1, sampleRate = 16000, bits = 16;
    long dataOff = -1, dataLen = 0;
    while (pos <= sz - 8) {
        char id[5] = {0};
        memcpy(id, &buf[pos], 4);
        int32_t size;
        memcpy(&size, &buf[pos + 4], 4);
        if (memcmp(id, "fmt ", 4) == 0) {
            int16_t ch, bps;
            memcpy(&ch, &buf[pos + 10], 2);
            memcpy(&sampleRate, &buf[pos + 12], 4);
            memcpy(&bps, &buf[pos + 22], 2);
            channels = ch; bits = bps;
        } else if (memcmp(id, "data", 4) == 0) {
            dataOff = pos + 8; dataLen = size; break;
        }
        pos += 8 + size + (size & 1);
    }
    if (dataOff < 0 || bits != 16) return false;
    if (channels < 1 || channels > 8) channels = 1;
    // 容错：有些写头的实现声明大小缺失/错误，按实际文件长度截取
    {
        long avail = sz - dataOff;
        if (dataLen <= 0 || dataLen > avail) dataLen = avail;
    }

    int frame = channels * 2;
    int n = (int) (dataLen / frame);
    out.resize(n);
    const int16_t* p = (const int16_t*) (buf.data() + dataOff);
    for (int i = 0; i < n; i++) {
        float acc = 0.f;
        for (int c = 0; c < channels; c++) acc += (float) p[i * channels + c];
        out[i] = acc / (float) channels / 32768.0f;
    }
    (void) sampleRate;
    return true;
}

// ---- 进度回传（whisper 在内部线程触发，需 attach 到 JVM 调 Kotlin）----
struct ProgressCtx {
    JavaVM* vm;
    jobject obj; // WhisperNative.Progress 全局引用
};

static void progress_cb(struct whisper_context*, struct whisper_state*, int progress, void* user_data) {
    auto* pc = static_cast<ProgressCtx*>(user_data);
    if (!pc || !pc->obj) return;
    JNIEnv* env = nullptr;
    bool attached = false;
    if (pc->vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        if (pc->vm->AttachCurrentThread(&env, nullptr) != JNI_OK) return;
        attached = true;
    }
    jclass cls = env->GetObjectClass(pc->obj);
    jmethodID mid = env->GetMethodID(cls, "onProgress", "(I)V");
    if (mid) env->CallVoidMethod(pc->obj, mid, (jint) progress);
    if (attached) pc->vm->DetachCurrentThread();
}

// 取消检查（返回 true → whisper 立即中止）
static bool abort_cb(void*) { return g_abort_requested.load(); }

extern "C" JNIEXPORT void JNICALL
Java_com_learnagent_asr_WhisperNative_cancelNative(JNIEnv*, jobject) {
    g_abort_requested.store(true);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_learnagent_asr_WhisperNative_transcribeNative(JNIEnv* env, jobject,
        jstring jmodel, jstring jwav, jint jthreads, jobject jprogress) {
    g_abort_requested.store(false);
    std::string model = toStr(env, jmodel);
    std::string wav = toStr(env, jwav);

    // 模型常驻缓存：避免分块转写时每块重新加载 1.5GB（一次加载数分钟）
    whisper_context* ctx = getCtx(model);
    if (!ctx) return env->NewStringUTF("{\"error\":\"model_load_failed\"}");

    std::vector<float> pcm;
    if (!readWav(wav, pcm) || pcm.empty()) {
        whisper_free(ctx);
        return env->NewStringUTF("{\"error\":\"bad_wav\"}");
    }

    whisper_full_params wp = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    wp.print_progress = false;
    wp.print_realtime = false;
    wp.print_timestamps = false;
    wp.print_special = false;
    wp.translate = false;
    wp.language = "zh";
    wp.n_threads = jthreads > 0 ? jthreads : 4;
    wp.no_context = true;

    JavaVM* vm = nullptr;
    env->GetJavaVM(&vm);
    ProgressCtx pc{vm, nullptr};
    if (jprogress != nullptr) {
        pc.obj = env->NewGlobalRef(jprogress);
        wp.progress_callback = progress_cb;
        wp.progress_callback_user_data = &pc;
    }
    wp.abort_callback = abort_cb;
    wp.abort_callback_user_data = nullptr;

    int rc = whisper_full(ctx, wp, pcm.data(), (int) pcm.size());
    if (pc.obj) env->DeleteGlobalRef(pc.obj);

    if (rc != 0) {
        whisper_free(ctx);
        // 主动取消 → 返回空串（上层静默停止，不算错误）
        return env->NewStringUTF(g_abort_requested.load() ? "" : "{\"error\":\"run_failed\"}");
    }

    int nseg = whisper_full_n_segments(ctx);
    std::string out = "[";
    for (int i = 0; i < nseg; i++) {
        int64_t t0 = whisper_full_get_segment_t0(ctx, i);
        int64_t t1 = whisper_full_get_segment_t1(ctx, i);
        const char* txt = whisper_full_get_segment_text(ctx, i);
        if (i) out += ",";
        out += "{\"start\":" + std::to_string((double) t0 / 100.0) +
               ",\"end\":" + std::to_string((double) t1 / 100.0) +
               ",\"text\":\"" + jsonEscape(txt ? txt : "") + "\"}";
    }
    out += "]";
    whisper_free(ctx);
    return env->NewStringUTF(out.c_str());
}
