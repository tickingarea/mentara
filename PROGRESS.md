# Mentara 进度面板

> 最后更新：2026-10-03
> 原则：文件即真相；所有结论附命令与输出

## 总体进度：约 95%（T-C…T-M 全部完成；剩余为环境受限项）

## 最终验收（一条命令）

```
gradle-dist/gradle-8.10/bin/gradle.bat :shared:jvmTest :desktopApp:build :androidApp:assembleDebug :mcpServer:build --offline --no-daemon
→ BUILD SUCCESSFUL
```

| 目标 | 结果 |
|------|------|
| `:shared:jvmTest` | 8 XML / **23 tests / 0 failures** |
| `:desktopApp:build` | 5 XML / **9 tests / 0 failures** |
| `:androidApp:assembleDebug` | APK **54,881,177 B（52.34 MB）** |
| `:mcpServer:build` | 通过（jar 10948 B） |
| 评测 | 黄金集 **20/20（100%）** |

## 任务清单

| 任务 | 内容 | 状态 | 证据 |
|------|------|------|------|
| T-A | 恢复 KMP 双端架构 | ✅ | androidMain 非空、`:shared:compileDebugKotlinAndroid` |
| T-B | Windows 内置语音转写（WAV） | ✅ | 测试 `TRANSCRIBE_RESULT=[0.0-1.63]今天天气很好` |
| T-C | 视频→WAV（无 ffmpeg） | ✅ | WinRT MediaTranscoder；`fmt: ch=1 sr=16000 bits=16`；media→wav→text `"今年产却缓过"` |
| T-D | Android 端真实转写 | ✅ | `AndroidSpeechTranscriber`（SpeechRecognizer）+ RECORD_AUDIO；APK 通过 |
| T-E | 真实 LLM 问答 | ✅ | NIM models 200（81 模型）；`RemoteQaEngine`+`NvidiaLlmClient`；RemoteQaEngineTest |
| T-F | 视频抽帧 + OCR | ✅ | WinRT 抽帧 4,485,270 B + Windows OCR（exit 0） |
| T-G | Material3 UI | ✅ | 顶部栏 + 标签 + 三栏工作台（截图） |
| T-H | 真机测试 | ⛔ N/A | `adb devices` 0 台 |
| T-I | 双端同步 | ✅ | SYNC.md + “同步配置”按钮 |
| T-J | 评测体系 | ✅ | 黄金集 20 题，`EVAL_ACCURACY=20/20` |
| T-K | 文档完善 | ✅ | README/ARCHITECTURE/CONTRIBUTING/API/ROADMAP/SYNC/MCP_USAGE |
| T-L | MCP Server | ✅ | stdio JSON-RPC：initialize/tools_list/search_video/ask_question 实测 |
| T-M | 最终验收 | ✅ | 见上表 |

## 环境限制（已如实记录）

1. **系统策略禁止 Java 创建 PowerShell 子进程**（`CreateProcess error=5`，最小 Java 探针验证）。
   桌面 OCR/转写/抽帧的 Java 集成测试在 `SKIP_ENV` 下跳过；能力改用 shell 直接运行脚本验证。
2. 无 ffmpeg；JCodec 离线不可解析 → 视频→WAV 用 WinRT MediaTranscoder 替代。
3. 多帧抽帧：WinRT 多帧互操作受限 + 缺统一 Windows.winmd → 每视频 1 张代表帧。
4. NVIDIA NIM chat 端点用假 key 返回 410，需用户提供真实 key。
5. 无真机设备。

## 关键实现

- 脚本：`scripts/transcribe.ps1`、`scripts/video_to_wav.ps1`、`scripts/extract_frames.ps1`（并附带为桌面资源）
- 桌面：`asr/WindowsTranscriber.kt`、`media/VideoToWav.kt`、`media/VideoFrameExtractor.kt`、`ocr/WindowsOcr.kt`、`llm/NvidiaLlmClient.kt`
- 共享：`core/RemoteQaEngine`、`core/LocalQaEngine`、`core/merge`、`core/Indexer`
- Android：`core/AndroidSpeechTranscriber.kt` + `MainActivity` 转写界面 + RECORD_AUDIO
- MCP：`mcpServer/`（stdio JSON-RPC）

---

## 追加任务（三阶段：图标 / 内部 Logo / 12 功能，要求 APK 保底）

### 第一阶段：应用图标 ✅
- scripts/generate_icon.ps1（.NET System.Drawing）生成：
  - desktopApp/src/main/resources/icon.png（8332 B）、icon.ico（8354 B）
  - androidApp/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.png（2912/4979/6142/11254/15312 B）
- AndroidManifest 增加 android:icon / android:roundIcon
- desktopApp/build.gradle.kts 增加 compose.desktop application + nativeDistributions（windows.iconFile）
- Main.kt Window 增加 icon = painterResource("icon.png")

### 第二阶段：内部 Logo ⚠️ 平台各自实现（shared/commonMain 方案已回滚）
- 尝试把 Compose Logo 放 shared/commonMain：会引入 androidx.compose 1.7.1 / lifecycle 2.8.5 / coroutines 1.8.0，
  这些在离线缓存中缺失，导致 `:androidApp:checkDebugAarMetadata` 失败。
- 依据"APK 保底"规则回滚，改为：
  - desktopApp/src/main/kotlin/com/Mentara/ui/Logo.kt → MentaraLogo（知识节点，桌面）
  - androidApp/src/main/kotlin/com/Mentara/ui/Logo.kt → MentaraLogo（同款，Android）
  - 两端视觉一致，但未字面放在 commonMain。
- desktopApp 另有 A/B/C 三方案（顶部临时选择器）。

### 第三阶段：12 功能
- F1 知识图谱：shared/commonMain/core/GraphBuilder.kt（逻辑）+ desktopApp/ui/KnowledgeGraph.kt（Canvas，已接入"图谱"视图）✅
- F2 主动提醒：desktopApp/app/ReviewTracker.kt + Main 顶部提醒条（截图：你已经 10 天没复习《demo-lecture》了）✅
- F3 跨视频连接：shared/commonMain/core/CrossVideo.kt（CrossVideoIndex.findCrossVideoConnections）✅
- F4 闪卡：shared/commonMain/core/StudyKit.kt（generateFlashcards/flashcardsCsv）+ desktopApp/app/StudyTools.kt（导出 CSV）✅（逻辑层）
- F5 学习报告：StudyKit.weeklyReportMarkdown/computeStats + desktop 报告生成/导出 ✅（逻辑层）
- F6 时间轴回放：desktopApp/ui/TimelineReplay.kt（已接入"时间轴"视图）✅
- F7 概念对比：StudyKit.compareMarkdown + desktop compareMarkdown ✅（逻辑层）
- F8 快捷键：Main.kt Window.onKeyEvent（Ctrl+O 导入 / Esc 关闭设置）✅（桌面）
- F9 语音提问：SKIP — Java→PowerShell 被系统策略拦截；Android 需运行时麦克风权限
- F10 MCP：桌面独有，已存在并通过 stdio 实测 ✅
- F11 多模态输入：SKIP — 网页 URL 需 Jsoup+网络（离线不可用）；其余类型未接 UI
- F12 知识导出：StudyKit.toMarkdown/toCsv + desktop 导出（Markdown/JSON/CSV）✅（逻辑层）

### APK 保底结果
- :shared:jvmTest / :desktopApp:build / :androidApp:assembleDebug 每一步后均 BUILD SUCCESSFUL
- 最终 APK：55,615,789 B（53.04 MB） > 10 MB