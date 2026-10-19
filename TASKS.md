# Mentara 任务清单

> 最后更新：2026-10-02

## 进行中
- [ ] 修复 libs.versions.toml，让 libs.plugins 可解析
- [ ] 让 shared:test 真实编译通过

## 待办
- [ ] MergerTest 和 IndexerTest 被 Gradle 发现并运行
- [ ] 替换 AndroidTranscriber 的 MOCK 为真实转写
- [ ] BYOK 问答引擎接入
- [ ] 编译 APK
- [ ] 真机安装测试

## 已完成
- [x] 项目骨架、数据模型
- [x] Merger.kt / Indexer.kt / FrameExtractor.kt / QaEngine.kt 文件存在
- [x] 真实 Gradle 8.10 + JDK 21 已就位

## 阻塞
- libs.versions.toml 缺失或配置错误
- gradlew.bat 是假的（31KB，正常 63KB），暂时绕过

## 里程碑
- [ ] M1：shared 编译通过
- [ ] M2：所有测试 PASSED
- [ ] M3：APK 编译成功
- [ ] M4：真机运行
- [ ] M5：真实转写替换 MOCK