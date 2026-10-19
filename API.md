# API 参考（API）

核心接口位于 `shared/src/commonMain/kotlin/com/Mentara/`。

## 数据模型（`com.Mentara.data`）

```kotlin
@Serializable data class VideoMeta(videoId, deviceId, lastModified, schemaVersion, title, duration)
@Serializable data class TranscriptSegment(start: Double, end: Double, text: String, speaker: String?)
@Serializable data class FrameOcr(timestamp: Double, text: String, imagePath: String)
@Serializable data class TimelineItem(start, end, speech, screenText, framePath)
```

## 融合（`core.Merger`）

```kotlin
fun merge(transcript: List<TranscriptSegment>, frames: List<FrameOcr>): List<TimelineItem>
```
把语音段与落在其时间区间内的帧合并；仅有画面无语音的帧单独成条；相邻重复 screenText 去重；按开始时间递增。

## 检索（`core.Indexer`）

```kotlin
class Indexer {
    fun index(items: List<TimelineItem>)
    fun search(query: String, topK: Int = 5): List<TimelineItem>
    fun clear()
}
```
当前为关键词检索（分词 + 命中计数 + 排序）。接口设计上可替换为向量检索。

## 问答（`core.QaEngine`）

```kotlin
interface QaEngine { suspend fun ask(question: String, videoId: String? = null): QaAnswer }
data class QaAnswer(val answer: String, val citations: List<String>)

class LocalQaEngine(search: suspend (String, Int) -> List<TimelineItem>) : QaEngine
class RemoteQaEngine(search: suspend (String, Int) -> List<TimelineItem>, llmClient: LlmClient, topK: Int = 8) : QaEngine

interface LlmClient { suspend fun generateResponse(prompt: String, config: LlmConfig): String }
data class LlmConfig(provider, apiKey, baseUrl, model)
```
- `LocalQaEngine`：纯本地，检索结果拼接 + `mm:ss` 引用，离线可用。
- `RemoteQaEngine`：检索构造带时间戳的上下文 → 调用 `LlmClient`（BYOK，如 NVIDIA NIM）。
- 桌面端 `com.Mentara.llm.NvidiaLlmClient` 实现 `LlmClient`（`/v1/chat/completions`）。

## 平台接口（`core`）

```kotlin
interface Transcriber { suspend fun transcribe(videoPath: String): List<TranscriptSegment> }
interface FrameExtractor { suspend fun extract(videoPath: String): List<FrameOcr> }
interface Storage { ... }   // 文件读写 / results 目录
```

## 平台实现

| 能力 | 桌面 | Android |
|------|------|---------|
| 转写 | `asr.WindowsTranscriber`（System.Speech，离线；输入 WAV） | `core.AndroidSpeechTranscriber`（系统 SpeechRecognizer，回调式） |
| 抽帧 | `media.VideoFrameExtractor`（WinRT 代表帧） | 预留 |
| OCR | `ocr.WindowsOcr`（Windows.Media.Ocr） | ML Kit（androidApp） |
| 视频→音频 | `media.VideoToWav`（WinRT MediaTranscoder → 16k 单声道 PCM） | 预留 |

## 平台配置（KMP expect/actual）

```kotlin
expect object PlatformConfig { val platformName: String; val isMobile: Boolean; val isDesktop: Boolean }
```
actual：`jvmMain`（Desktop）、`androidMain`（Android）。
