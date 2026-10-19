# 架构说明（ARCHITECTURE）

Mentara 采用 **Kotlin Multiplatform** 分层：所有平台共享 `shared` 的公共核心，平台侧只负责 I/O 与 UI。

## 模块

```
┌─────────────────────────────────────────────────────────────┐
│                        desktopApp (JVM)                      │
│  Compose Desktop 三栏工作台 / 屏幕 OCR (Windows.Media.Ocr)    │
├─────────────────────────────────────────────────────────────┤
│                        androidApp (Android)                  │
│  Compose + ML Kit OCR / androidMain 平台实现                 │
├─────────────────────────────────────────────────────────────┤
│                          shared (KMP)                        │
│  commonMain: 数据模型 / Merger / Indexer / QaEngine / Sync    │
│  jvm target: 被 desktopApp 依赖                              │
│  android target: 被 androidApp 依赖（仅 commonMain）          │
└─────────────────────────────────────────────────────────────┘
```

- **shared**：唯一真相来源。`commonMain` 不含任何平台 API（仅 `kotlinx.serialization`）。
- **desktopApp**：`kotlin("jvm")` + Compose Desktop，依赖 `shared` 的 jvm 变体。
- **androidApp**：AGP + Compose，依赖 `shared` 的 android 变体。

> 说明：`shared/src/androidMain` 与 `src/desktopMain` 下目前存在未完成的 `actual` 桩文件，与 `commonMain` 的接口不匹配，因此在 `shared/build.gradle.kts` 中 **排除了 `androidMain` 源目录**，Android 变体仅编译 `commonMain`。

## 数据模型（`com.Mentara.data`）

| 类型 | 字段 | 用途 |
|------|------|------|
| `VideoMeta` | videoId, deviceId, lastModified, schemaVersion, title, duration | 视频元数据 |
| `TranscriptSegment` | start, end, text, speaker | 一段语音转写 |
| `FrameOcr` | timestamp, text, imagePath | 一帧的 OCR 结果 |
| `TimelineItem` | start, end, speech, screenText, framePath | 融合后的时间线条目 |

全部使用 `@Serializable`（kotlinx.serialization）。

## 数据流

```
视频文件
  ├── (语音) Transcriber ──► List<TranscriptSegment> ┐
  └── (画面) FrameExtractor ► List<FrameOcr>          │
                                                      ▼
                                          Merger.merge(...)
                                                      │
                                                      ▼
                                          List<TimelineItem>
                                                      │
                        ┌─────────────────────────────┤
                        ▼                             ▼
                 Indexer.index(...)            (持久化为 JSON)
                        │
                        ▼
                 Indexer.search(query) ─► List<TimelineItem>
                        │
                        ▼
        LocalQaEngine.ask(question) ─► QaAnswer(answer, citations)

（可选）KeywordQaEngine + LlmClient（BYOK）在配置 API Key 后启用大模型回答
```

### Merger

`merge(transcript, frames)` 把每个语音段与落在其时间区间内的帧合并为一条 `TimelineItem`；只有画面没有语音的帧会单独成条；相邻重复的 `screenText` 会被去重。结果按开始时间递增。

### Indexer

当前为关键词检索：分词 → 计算命中关键词数 → 按分数与时间排序 → 取 topK。接口设计为后续可替换为向量检索。

### QaEngine

- `QaEngine`：接口，`suspend fun ask(question, videoId?): QaAnswer`
- `LocalQaEngine`：纯本地，用检索结果拼接答案并返回 `mm:ss` 时间戳引用，无需联网
- `KeywordQaEngine`：预留 BYOK LLM，未配置 Key 时返回提示

## 平台实现

| 能力 | 桌面 | Android |
|------|------|---------|
| OCR | Windows.Media.Ocr（PowerShell 调用，离线） | ML Kit text-recognition(-chinese) |
| 帧来源 | `java.awt.Robot` 屏幕抓帧（本环境无 ffmpeg） | 视频解码 |
| 转写 | 预留（Vosk/本地模型） | 预留 |

## 构建

- `:shared:jvmTest`：JVM 平台单元测试
- `:desktopApp:run` / `:desktopApp:build`
- `:androidApp:assembleDebug`

所有命令支持 `--offline`（本环境无外网，依赖来自本地 Gradle 缓存）。
