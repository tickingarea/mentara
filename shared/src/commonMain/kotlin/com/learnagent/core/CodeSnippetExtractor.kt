package com.learnagent.core

import com.learnagent.data.CodeSnippet
import com.learnagent.data.LearningUnit

/**
 * 代码片段提取器（规则版，不依赖 LLM）：
 * 1. 识别 ``` 包裹的代码块（可带语言标注）
 * 2. 识别连续 4 空格 / Tab 缩进的行
 * 3. 按语言特征识别语言；注释用规则生成一句话
 */
class CodeSnippetExtractor {

    fun extract(unit: LearningUnit, videoId: String): List<CodeSnippet> {
        val text = unit.screenText.ifBlank { unit.speech }
        if (text.isBlank()) return emptyList()

        val blocks = mutableListOf<Pair<String, String>>() // code to language hint
        FENCED.findAll(text).forEach { m ->
            val hint = m.groupValues[1].trim()
            val code = m.groupValues[2].trim('\n', '\r')
            if (code.isNotBlank()) blocks.add(code to hint)
        }
        if (blocks.isEmpty()) {
            indentedBlock(text)?.let { blocks.add(it to "") }
        }

        return blocks.mapIndexed { i, (code, hint) ->
            val language = detectLanguage(code, hint)
            CodeSnippet(
                id = "${videoId}_snippet_${unit.start.toInt()}_$i",
                language = language,
                purpose = "代码片段（$language）",
                code = code,
                comment = oneLineComment(code),
                sourceVideoId = videoId,
                sourceTimestamp = unit.start,
                tags = unit.keywords.take(5)
            )
        }
    }

    /** 连续 ≥2 行以 4 空格或 Tab 开头（允许空行间隔）时，取为一个代码块。 */
    private fun indentedBlock(text: String): String? {
        val lines = text.lines()
        val collected = mutableListOf<String>()
        var current = mutableListOf<String>()
        for (line in lines) {
            val isIndented = (line.startsWith("    ") || line.startsWith("\t")) && line.trim().isNotEmpty()
            if (isIndented) {
                current.add(line)
            } else {
                if (current.size >= 2) collected.addAll(current)
                current = mutableListOf()
            }
        }
        if (current.size >= 2) collected.addAll(current)
        if (collected.size < 2) return null
        return collected.joinToString("\n").trim()
    }

    private fun detectLanguage(code: String, hint: String): String {
        val h = hint.lowercase().trim()
        if (h.isNotBlank()) return h
        return when {
            Regex("(?m)^\\s*def\\s+\\w+.*:").containsMatchIn(code) -> "python"
            Regex("(?m)^\\s*(public|private|protected)\\s+(class|void|static)").containsMatchIn(code) ||
                code.contains("System.out.println") -> "java"
            Regex("(?m)^\\s*fun\\s+\\w+").containsMatchIn(code) ||
                (code.contains("val ") && code.contains("fun ")) -> "kotlin"
            Regex("(?i)\\b(SELECT|INSERT|UPDATE|DELETE|CREATE\\s+TABLE)\\b").containsMatchIn(code) -> "sql"
            code.contains("function ") || code.contains("const ") || code.contains("=>") -> "javascript"
            code.contains("#include") || code.contains("int main") -> "c"
            else -> "text"
        }
    }

    private fun oneLineComment(code: String): String {
        val first = code.lines().firstOrNull { it.trim().isNotEmpty() }?.trim().orEmpty()
        return if (first.isBlank()) "OCR 提取的代码片段" else "首行：${first.take(60)}"
    }

    private companion object {
        val FENCED = Regex("```([A-Za-z0-9+#]*)[ \\t]*\\n([\\s\\S]*?)```")
    }
}
