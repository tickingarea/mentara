# 三端同步（SYNC · PC / 平板 / 手机）

Mentara 的所有产出（元数据、转写、OCR、时间线）都以 JSON 形式存放在统一的 **results 目录**下。
因此只要同步这个目录，即可让 PC / 平板 / 手机三端完全共用同一份知识库。
推荐使用开源工具 [Syncthing](https://syncthing.net/)（点对点、无需云服务器）。

## 目录结构（三端同构）

```
results/
└── <videoId>/
    ├── meta.json         # VideoMeta
    ├── transcript.json   # List<TranscriptSegment>
    ├── frames.json       # List<FrameOcr>
    └── timeline.json     # List<TimelineItem>
```

## 各端实际路径

| 端 | 路径 |
|----|------|
| PC（桌面） | `~/.mentara/results/`（`<user.home>/.mentara/results`） |
| 平板 | 应用私有目录 `filesDir/results/` |
| 手机 | 应用私有目录 `filesDir/results/` |

视觉层三端一致由 `shared/commonMain/.../design/DesignTokens.kt` 保证（配色 / 圆角 / 时长 / 字距全部共享）。
逻辑层三端一致：数据模型、ContextAnalyzer / UnitFusion / QaEngine / Indexer / Harness / DebateEngine 全在 `shared/commonMain`。

## 使用 Syncthing 同步

### 1. 安装

- PC：从官网或系统包管理器安装 Syncthing
- 平板 / 手机：安装 Syncthing-Fork（Android）或 Syncthing

### 2. 添加设备

两端打开 Syncthing，互相添加对方的 **Device ID**，确认配对。

### 3. 共享 results 文件夹

- PC：把 `~/.mentara/results` 添加为共享文件夹
- Android：接受共享，并把本地路径指向应用的 results 目录
  （受 Android 分区存储限制，建议把共享文件夹映射到 `Android/data/com.learnagent/files/results`，
  或由用户授予该路径的 SAF 访问权限）

### 4. 冲突处理

- Syncthing 对同名不同内容会生成 `*.sync-conflict-*` 文件
- 由于每个视频以 `videoId` 为目录、文件内容带 `lastModified`，正常情况按 **修改时间较新者优先** 合并即可
- `schemaVersion` 用于后续兼容不同版本的数据结构

## 三端功能对齐检查表

| 能力 | PC | 平板 | 手机 |
|---|---|---|---|
| 导入视频 / 音频 / 文件夹 | ✅（文件对话框 / 拖拽） | ✅（SAF） | ✅（SAF） |
| 转写 | ✅（System.Speech / Vosk） | ✅（SpeechRecognizer API 31+ 离线） | ✅（同左） |
| 抽帧 OCR | ✅（Windows.Media.Ocr） | ✅（ML Kit 中文识别） | ✅（同左） |
| 四层理解 / 索引 / 检索 | ✅（shared/commonMain） | ✅（同） | ✅（同） |
| 问答 | ✅ | ✅ | ✅（离线 LocalQaEngine 一致） |
| 三模型辩论 | ✅（内容页"深度分析"） | ✅（同共享引擎） | ✅（同共享引擎） |
| 经验记录 / 知识导出 | ✅ | ✅ | ✅（ExperienceStore 平台注入） |

任一项只在单端有 → 补到另一端（规则见 CONTRIBUTING.md）。

## 桌面端快捷操作

桌面端工作台提供 **“设置 → 结果目录 → 打开”** 按钮：点击后用资源管理器打开 results 目录，
把路径复制到 Syncthing 的文件夹路径框即可。

## 手动同步

若暂不使用 Syncthing，也可直接拷贝整个 `results` 目录，或对单条 `timeline.json` 做差量合并。
数据格式稳定，纯文本易于版本管理与审阅。
