package com.learnagent.core

import com.learnagent.data.AnswerStandard
import com.learnagent.data.AnswerStep
import com.learnagent.data.QuestionType
import com.learnagent.data.ScoringPoint

/**
 * 规范答题库：五科（数学/物理/化学/语文/英语）共 10 套内置标准，不依赖 LLM。
 */
object AnswerStandardLibrary {

    val MATH_CALCULATION = AnswerStandard(
        id = "math_calculation",
        questionType = QuestionType.CALCULATION,
        subject = "数学",
        requiredSteps = listOf(
            AnswerStep(1, "写\"解：\"", "标记解题开始", 0),
            AnswerStep(2, "写出已知条件", "让阅卷人知道你的起点", 1),
            AnswerStep(3, "写出所用公式", "得公式分", 2),
            AnswerStep(4, "代入数据", "得代入分", 2),
            AnswerStep(5, "计算过程", "关键步骤必须有", 1),
            AnswerStep(6, "得出结果", "得结果分", 1),
            AnswerStep(7, "写\"答：\"", "标记解题结束", 0)
        ),
        forbiddenSteps = listOf("写无关内容", "跳步"),
        scoringPoints = listOf(
            ScoringPoint("公式正确", 2, true),
            ScoringPoint("代入正确", 2, true),
            ScoringPoint("结果正确", 1, true)
        )
    )

    val MATH_PROOF = AnswerStandard(
        id = "math_proof",
        questionType = QuestionType.PROOF,
        subject = "数学",
        requiredSteps = listOf(
            AnswerStep(1, "写\"证明：\"", "标记证明开始", 0),
            AnswerStep(2, "写出已知", "引述题目条件", 1),
            AnswerStep(3, "写出求证", "明确目标", 1),
            AnswerStep(4, "推导过程", "每一步必须有依据", 3),
            AnswerStep(5, "写\"证毕\"或\"得证\"", "标记证明结束", 0)
        ),
        scoringPoints = listOf(
            ScoringPoint("每步有依据", 3, true),
            ScoringPoint("逻辑连贯", 2, true)
        )
    )

    val PHYSICS_CALCULATION = AnswerStandard(
        id = "physics_calculation",
        questionType = QuestionType.CALCULATION,
        subject = "物理",
        requiredSteps = listOf(
            AnswerStep(1, "写出已知量 + 单位", "物理必须有单位", 1),
            AnswerStep(2, "写出所求量", "明确目标", 1),
            AnswerStep(3, "写出所用公式", "得公式分", 2),
            AnswerStep(4, "代入数据（带单位）", "单位错了扣分", 2),
            AnswerStep(5, "计算结果（带单位）", "结果必须带单位", 2),
            AnswerStep(6, "写出答案", "规范收尾", 0)
        ),
        scoringPoints = listOf(
            ScoringPoint("公式正确", 2, true),
            ScoringPoint("单位正确", 3, true),
            ScoringPoint("结果正确", 2, true)
        )
    )

    val PHYSICS_EXPERIMENT = AnswerStandard(
        id = "physics_experiment",
        questionType = QuestionType.SHORT_ANSWER,
        subject = "物理",
        requiredSteps = listOf(
            AnswerStep(1, "实验目的", "明确目标", 1),
            AnswerStep(2, "实验原理", "引用定律", 2),
            AnswerStep(3, "实验器材", "列出器材", 1),
            AnswerStep(4, "实验步骤", "分步描述", 3),
            AnswerStep(5, "数据处理", "记录 + 分析", 2),
            AnswerStep(6, "实验结论", "总结", 1)
        )
    )

    val CHEMISTRY_EQUATION = AnswerStandard(
        id = "chemistry_equation",
        questionType = QuestionType.FILL_BLANK,
        subject = "化学",
        requiredSteps = listOf(
            AnswerStep(1, "反应条件", "写在上方", 1),
            AnswerStep(2, "配平系数", "必须配平", 3),
            AnswerStep(3, "状态符号", "↑↓标注", 1)
        ),
        scoringPoints = listOf(
            ScoringPoint("配平正确", 3, true),
            ScoringPoint("条件标注", 1, true),
            ScoringPoint("状态符号", 1, true)
        )
    )

    val CHEMISTRY_CALCULATION = AnswerStandard(
        id = "chemistry_calculation",
        questionType = QuestionType.CALCULATION,
        subject = "化学",
        requiredSteps = listOf(
            AnswerStep(1, "写出化学方程式", "必须配平", 2),
            AnswerStep(2, "写出已知量", "物质的量/质量/体积", 1),
            AnswerStep(3, "列出比例关系", "方程式系数比", 2),
            AnswerStep(4, "计算结果", "带单位", 2),
            AnswerStep(5, "写出答案", "规范收尾", 0)
        )
    )

    val CHINESE_READING = AnswerStandard(
        id = "chinese_reading",
        questionType = QuestionType.SHORT_ANSWER,
        subject = "语文",
        requiredSteps = listOf(
            AnswerStep(1, "点明观点", "亮明态度", 2),
            AnswerStep(2, "引用原文依据", "必须有引文", 3),
            AnswerStep(3, "分析论证", "解释引文与观点的关系", 2),
            AnswerStep(4, "总结升华", "加分项", 1)
        )
    )

    val CHINESE_ESSAY = AnswerStandard(
        id = "chinese_essay",
        questionType = QuestionType.SHORT_ANSWER,
        subject = "语文",
        requiredSteps = listOf(
            AnswerStep(1, "立意", "观点明确", 3),
            AnswerStep(2, "结构", "开头-主体-结尾", 2),
            AnswerStep(3, "论据", "至少 2 个例子", 2),
            AnswerStep(4, "结论", "呼应开头", 1)
        )
    )

    val ENGLISH_CLOZE = AnswerStandard(
        id = "english_cloze",
        questionType = QuestionType.SINGLE_CHOICE,
        subject = "英语",
        requiredSteps = listOf(
            AnswerStep(1, "上下文分析", "看前后句", 1),
            AnswerStep(2, "语法判断", "时态/语态/搭配", 2),
            AnswerStep(3, "词义辨析", "近义词区别", 2),
            AnswerStep(4, "逻辑一致", "语义通顺", 1)
        )
    )

    val ENGLISH_WRITING = AnswerStandard(
        id = "english_writing",
        questionType = QuestionType.SHORT_ANSWER,
        subject = "英语",
        requiredSteps = listOf(
            AnswerStep(1, "审题", "时态/人称/体裁", 1),
            AnswerStep(2, "列提纲", "要点齐全", 2),
            AnswerStep(3, "写正文", "开头-主体-结尾", 3),
            AnswerStep(4, "检查", "语法/拼写/标点", 2)
        )
    )

    /** 全部内置标准（供 UI/巡检遍历）。 */
    val all: List<AnswerStandard> = listOf(
        MATH_CALCULATION, MATH_PROOF, PHYSICS_CALCULATION, PHYSICS_EXPERIMENT,
        CHEMISTRY_EQUATION, CHEMISTRY_CALCULATION, CHINESE_READING, CHINESE_ESSAY,
        ENGLISH_CLOZE, ENGLISH_WRITING
    )

    fun get(subject: String, type: QuestionType): AnswerStandard? = when {
        subject == "数学" && type == QuestionType.CALCULATION -> MATH_CALCULATION
        subject == "数学" && type == QuestionType.PROOF -> MATH_PROOF
        subject == "物理" && type == QuestionType.CALCULATION -> PHYSICS_CALCULATION
        subject == "物理" -> PHYSICS_EXPERIMENT
        subject == "化学" && type == QuestionType.FILL_BLANK -> CHEMISTRY_EQUATION
        subject == "化学" -> CHEMISTRY_CALCULATION
        subject == "语文" && type == QuestionType.SHORT_ANSWER -> CHINESE_READING
        subject == "英语" && type == QuestionType.SINGLE_CHOICE -> ENGLISH_CLOZE
        subject == "英语" -> ENGLISH_WRITING
        else -> null
    }
}
