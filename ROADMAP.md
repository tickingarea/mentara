# 路线图（ROADMAP）

## 已完成

- [x] KMP 骨架（jvm + android target），expect/actual 平台配置
- [x] 数据模型 + kotlinx.serialization
- [x] Merger / Indexer / LocalQaEngine / RemoteQaEngine
- [x] 桌面端 Compose 三栏工作台（列表 / 详情 / 问答）
- [x] 真实语音转写（桌面：Windows System.Speech，离线）
- [x] 视频 → WAV（桌面：WinRT MediaTranscoder，替代缺失的 ffmpeg）
- [x] 视频抽帧 + OCR（桌面：WinRT + Windows.Media.Ocr）
- [x] Android 端内置 SpeechRecognizer 转写（代码 + RECORD_AUDIO）
- [x] NVIDIA NIM 大模型问答（BYOK，API 可达）
- [x] 评测体系（黄金集 20 题，baseline 100%）
- [x] APK 构建（≈52 MB）

## 进行中 / 受限

- [ ] Java→PowerShell 子进程：本机安全策略当前拒绝（`CreateProcess error=5`），
      桌面 OS 集成测试在 `SKIP_ENV` 下跳过；能力改由 shell 直接验证。
- [ ] 视频多帧抽帧：WinRT 多帧互操作受限 + 无 ffmpeg，目前每视频 1 张代表帧。
- [ ] 真机测试：无连接设备。

## 未来计划

- [ ] 接入 ffmpeg（或 ffmpeg-kit）实现音频提取与按秒抽帧
- [ ] Android 端 whisper.cpp 本地转写（需 native 产物 + 模型）
- [ ] 向量检索（替换 Indexer 的关键词匹配）
- [ ] BYOK 设置持久化 + 更多 LLM provider
- [ ] Syncthing 一键配置与冲突可视化
- [ ] MCP server 暴露 search/ask 工具
- [ ] CI（离线缓存的 GitHub Actions 或本地脚本）
