# 评测 Baseline 记录

- 日期：2026-10-03
- 引擎：LocalQaEngine（关键词检索 + 结果拼接，无 LLM）
- 数据：shared 内置示例时间线（4 段语音 + 4 帧 OCR）
- 黄金集：**20 题**（`shared/src/jvmTest/resources/golden_set.json`，另存 `eval/golden_set.json`）

## 结果

```
EVAL_ACCURACY=20/20 (100%)
```

逐题明细见 `shared/build/test-results/jvmTest/TEST-com.learnagent.EvaluationTest.xml`。

## 复现

```bash
gradle-dist/gradle-8.10/bin/gradle.bat :shared:jvmTest --offline --no-daemon --tests "com.learnagent.EvaluationTest"
```

## 备注

LocalQaEngine 同时拼接语音与画面文字，因此仅存在于 OCR 文本中的关键词
（如“学习率”“图像识别”“负梯度”“CNN”）也能被正确回答。
配置 NVIDIA API Key 后，桌面端将改用 RemoteQaEngine（NIM 大模型）。
