# 评测体系（eval）

## 黄金测试集

- 位置：`shared/src/jvmTest/resources/golden_set.json`（同时保存于 `eval/golden_set.json`）
- 共 **20 题**，每题：`question` + `expect`（答案中应出现的关键词；无匹配题期望“未提及”）

覆盖：语音命中、画面文字命中（OCR）、以及无匹配。

## 运行评测

```bash
gradle-dist/gradle-8.10/bin/gradle.bat :shared:jvmTest --offline --no-daemon --tests "com.learnagent.EvaluationTest"
```

用例 `com.learnagent.EvaluationTest#testGoldenSetAccuracy`，逐题结果与 `EVAL_ACCURACY`
见 `shared/build/test-results/jvmTest/TEST-com.learnagent.EvaluationTest.xml` 的 `system-out`。

## Baseline

| 日期 | 引擎 | 命中 | 准确率 |
|------|------|------|--------|
| 2026-10-03 | LocalQaEngine（无 LLM） | 20/20 | **100%** |

> 基于本地示例时间线；接入真实转写 / 向量检索 / 大模型后需在新数据上复测。
