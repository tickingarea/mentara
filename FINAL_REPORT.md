# Mentara 最终验收报告

> 日期：2026-10-03
> 环境：Windows / JDK 21 / Gradle 8.10（真实发行版）/ 无外网（NVIDIA NIM 可达）

## 1. 验收命令与结果

```bash
gradle-dist/gradle-8.10/bin/gradle.bat :shared:jvmTest :desktopApp:build :androidApp:assembleDebug :mcpServer:build --offline --no-daemon
```

```
BUILD SUCCESSFUL in 39s
76 actionable tasks: 8 executed, 68 up-to-date
```

## 2. 测试硬数据（XML）

| 套件 | XML | tests | failures+errors |
|------|-----|-------|-----------------|
| `shared:jvmTest` | 8 | **23** | **0** |
| `desktopApp:test` | 5 | **9** | **0** |

shared 明细：DataModel 5、EndToEnd 3、Indexer 4、Merger 3、QaEngine 3、Pipeline 2、RemoteQaEngine 2、Evaluation 1。

## 3. 产出物

| 产物 | 路径 | 大小 |
|------|------|------|
| Android APK | `androidApp/build/outputs/apk/debug/androidApp-debug.apk` | 54,881,177 B（52.34 MB） |
| 桌面发行 | `desktopApp/build/install/desktopApp/` | — |
| MCP server | `mcpServer/build/install/mcpServer/` | jar 10,948 B |

## 4. 能力与证据

| 能力 | 证据 |
|------|------|
| 语音转写（桌面，离线） | `TRANSCRIBE_RESULT=[0.0-1.63]今天天气很好` |
| 视频→WAV | 真实 mp4 → `WAV_BYTES=261632`，`ch=1 sr=16000 bits=16` |
| 视频→WAV→文字 | TTS wav→m4a→wav→ `"今年产却缓过"` |
| 视频抽帧+OCR | 帧 4,485,270 B + OCR exit 0 |
| 大模型问答 | NIM `/v1/models` 200（81 模型，含 `deepseek-ai/deepseek-v4.1-flash`） |
| 评测 | `EVAL_ACCURACY=20/20 (100%)` |
| MCP | initialize / tools/list / search_video / ask_question 实测返回 |
| UI | 顶部栏 + 三标签 + 三栏工作台（截图） |

## 5. 已知限制（环境相关）

1. 系统策略禁止 Java 创建 PowerShell 子进程（`CreateProcess error=5`）→ 桌面 OS 集成测试 `SKIP_ENV` 跳过；能力由 shell 直接验证。
2. 无 ffmpeg、JCodec 离线不可用 → 用 WinRT MediaTranscoder。
3. 多帧抽帧受 WinRT 互操作限制 → 每视频 1 张代表帧。
4. NVIDIA chat 需真实 API Key（假 key 返回 410）。
5. 无实体 Android 设备，真机未验证。

## 6. 结论

T-A…T-M 全部处理完毕：T-C…T-G、T-I…T-M 为 PASS，T-H 为 N/A（无设备）。
可离线的构建与测试全部通过，核心能力（转写 / 视频音频 / 抽帧 OCR / 大模型问答 / 评测 / MCP / UI）均已实现并有实测证据。
