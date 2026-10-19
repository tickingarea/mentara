package com.learnagent.core

import com.learnagent.data.AnswerStandard

data class PracticeResult(
    val question: String,
    val userAnswer: String,
    val standardAnswer: String,
    val highlights: List<Highlight>,
    val suggestions: List<String>
)

data class Highlight(
    val text: String,
    val type: HighlightType,
    val position: IntRange
)

enum class HighlightType { MISSING_REQUIRED, WRONG_ORDER, GOOD, FORBIDDEN }

/** 规范练习：在批改结果上生成高亮（缺失/顺序/命中/禁止）与改进建议。 */
class StandardPracticeEngine(private val teacher: AnswerStandardTeacher) {

    suspend fun evaluate(question: String, standard: AnswerStandard, userAnswer: String): PracticeResult {
        val grading = teacher.gradeWithStandard(standard, userAnswer)
        val highlights = mutableListOf<Highlight>()

        standard.requiredSteps.forEach { step ->
            val pos = teacher.matchPosition(step.content, userAnswer)
            val missing = grading.missingRequired.any { it.order == step.order && it.content == step.content }
            when {
                missing -> highlights.add(Highlight(step.content, HighlightType.MISSING_REQUIRED, 0..0))
                pos >= 0 -> {
                    val cleaned = step.content.filter { it.isLetterOrDigit() }
                    val len = cleaned.length.coerceAtLeast(1)
                    highlights.add(Highlight(step.content, HighlightType.GOOD, pos until (pos + len).coerceAtMost(userAnswer.length)))
                }
            }
        }
        if (!grading.orderCorrect) {
            highlights.add(Highlight("步骤顺序需要调整", HighlightType.WRONG_ORDER, 0..0))
        }
        grading.forbidden.forEach { f ->
            val pos = userAnswer.indexOf(f)
            if (pos >= 0) highlights.add(Highlight(f, HighlightType.FORBIDDEN, pos until (pos + f.length)))
        }

        val suggestions = buildList {
            add(grading.feedback)
            grading.missingRequired.take(3).forEach { add("补上步骤：${it.content}（${it.reason}）") }
            if (!grading.orderCorrect) add("按标准顺序作答：${standard.requiredSteps.joinToString(" → ") { it.content }}")
            if (grading.forbidden.isNotEmpty()) add("去掉禁止内容：${grading.forbidden.joinToString("；")}")
            if (isEmpty()) add("作答规范完整。")
        }
        return PracticeResult(question, userAnswer, standard.standardTemplate, highlights, suggestions)
    }
}
