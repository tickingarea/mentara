# Mentara V3 Benchmark Report

- 日期：2026-10-05 21:01:27
- 设备：Windows 10 专业版 10.0.19045 | CPU: Intel(R) Xeon(R) CPU E5-2676 v3 @ 2.40GHz | GPU: AMD Radeon RX 580 2048SP | JVM 21.0.5 (jdk-temp) | Compose Desktop/Skia
- 约束：跑分必须真实；无法验证的项标 SKIP + 原因，不编造

## 结果

| 项目 | 分数 | 达标 | 数据来源 |
|---|---|---|---|
| 启动耗时 | 2132 ms | PASS（< 3000 ms） | 实跑 session A（main → 首帧 withFrameNanos） |
| 平均帧时间 | 17.45 ms（57.3 fps） | **MISS**（目标 < 16.7 ms） | 60 秒 withFrameNanos 采样，如实记录 |
| 粒子 80 压测 | 57.8 fps | PASS（> 50 fps） | 实跑 session B（80 粒子，测后已改回默认） |
| ASR WER | SKIP | — | 缺少真实英文音频测试集（eval/benchmark/asr 不存在，仓库无音频样本） |
| OCR CER | SKIP | — | 缺少真实英文图片测试集（eval/benchmark/ocr 不存在） |
| 语义识别准确率 | 100%（20/20） | PASS（> 90%） | eval/golden_set.json 黄金集 20 题，实跑 EvaluationTest（本地检索引擎，离线） |
| 语境识别准确率 | SKIP | — | 缺少语境分类测试集（20 段“定义/例子/推导/结论”标注不存在） |

## 跑分闸门（本轮由用户指示跳过闸门，仅如实记录）

- 识别类 4 项：1 项实测达标（语义）+ 3 项 SKIP → 未满足“至少 2 项达标”（不编造）
- 性能类 3 项：2 项达标（启动、粒子 80）+ 1 项未达标（平均帧 17.45ms）→ 满足“至少 2 项达标”

## 环境限制说明

1. 无真实英文音频/图片/语境标注数据集，ASR / OCR / 语境跑分无法执行，标 SKIP 而非编造。
2. 语义跑分为本地关键词检索引擎（LocalQaEngine，无 LLM、无网络），非大模型语义理解；配置 NVIDIA API Key 后可复跑 RemoteQaEngine 版本。
3. 帧采样在窗口空闲动画（DynamicBackground 常驻动画）下进行，60 秒窗口；未做 GPU 层细分。
4. 粒子压测通过临时开关强制 80 粒子完成，已改回默认（随内容量变化，基准 36）。

## 复现命令

```powershell
.\gradle-dist\gradle-8.10\bin\gradle.bat :desktopApp:run --offline --no-daemon
# 日志出现 STARTUP_TIME_MS= / AVG_FRAME_TIME_MS= / FPS=

.\gradle-dist\gradle-8.10\bin\gradle.bat :shared:jvmTest --offline --no-daemon --tests "com.learnagent.EvaluationTest"
# 报告 shared/build/reports/tests/jvmTest/classes/com.learnagent.EvaluationTest.html 内含 EVAL_ACCURACY
```
