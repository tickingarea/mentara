package com.learnagent.design

/**
 * 设计令牌（PC / 平板 / 手机三端共享）。
 *
 * V3「晨雾与深空」——Turrell 光与空间 × Monet 晨雾色域 × Klimt 金点 × teamLab 流动叠加。
 * 旧字段名全部保留（三端引用不破坏），值升级到 V3 色板；新增字段向后兼容追加。
 */
object DesignTokens {

    // ---------- 配色（ARGB，0xAARRGGBB）V3 ----------
    const val ColorVoid: Long = 0xFF0A0E14         // 背景基色（极淡蓝，非纯黑）
    const val ColorAbyss: Long = 0xFF0F1419         // 渐层更深
    const val ColorBgDeep: Long = 0xFF060A0F        // 最深处
    const val ColorVeil: Long = 0xFF131820          // 卡片底（蓝灰轻纱）
    const val ColorGlimmer: Long = 0xFF1A212B       // 悬浮层微光
    const val ColorPhantom: Long = 0xFFB8A8D4       // 主色·晨雾紫（淡雅不刺眼）
    const val ColorMistPurple: Long = 0xFFB8A8D4   // 同主色（别名）
    const val ColorAureate: Long = 0xFFE8C87A       // 强调·日落金
    const val ColorSunsetGold: Long = 0xFFE8C87A   // 同强调（别名）
    const val ColorLilyGreen: Long = 0xFF7FB89C     // 强调·睡莲绿（莫奈）
    const val ColorDeepSeaBlue: Long = 0xFF4A7AB8   // 强调·深海蓝
    const val ColorMoonSilver: Long = 0xFFC0C8D0   // 辅助·月光银
    const val ColorCoral: Long = 0xFFE89A9A         // 警示·暮色珊瑚
    const val ColorTextHigh: Long = 0xFFE8ECF2     // 文字·主
    const val ColorTextMid: Long = 0xFF8892A0       // 文字·次
    const val ColorTextLow: Long = 0xFF4A5260      // 文字·三
    const val ColorMist: Long = 0xFF232B36         // 描边·雾
    const val ColorCenterGlow: Long = 0xFF0F1419   // 背景径向中心
    const val ColorEdgeDark: Long = 0xFF060A0F     // 背景径向边缘
    const val ColorSuccess: Long = 0xFF7CFFB2      // 成功绿
    const val ColorMorningPurple: Long = 0xFFB8A8D4  // 晨（06-12 的 accent）
    const val ColorNightPurple: Long = 0xFF4A7AB8     // 夜（18-06 的 accent，深海蓝）

    // ---------- 时长（ms） ----------
    const val DurationFastMs: Int = 120
    const val DurationStandardMs: Int = 220
    const val DurationSlowMs: Int = 320
    const val DurationSplashMs: Int = 8000

    // ---------- 圆角（dp） ----------
    const val RadiusCardDp: Int = 20
    const val RadiusChipDp: Int = 6
    const val RadiusButtonDp: Int = 12
    const val RadiusFieldDp: Int = 12

    // ---------- 间距（dp）V3：杂志级留白 ----------
    const val SpaceHeroDp: Int = 64        // 桌面大区块之间
    const val SpaceSectionDp: Int = 48     // 桌面页边距
    const val SpaceGroupDp: Int = 32       // 桌面卡间距/按钮组
    const val SpaceItemDp: Int = 12        // 桌面卡内条目间距
    const val SpaceCardInnerDp: Int = 28  // 桌面卡内边距
    const val SpacePageDp: Int = 32        // Android 页边距

    // ---------- 字号（sp）层级：每级差 ≥ 4sp ----------
    const val SizeDisplayLargeSp: Int = 48   // 品牌/数字标牌（Splash/空态数字）
    const val SizeDisplaySmallSp: Int = 32   // 品牌标题（顶栏 Mentara）
    const val SizeHeadlineLargeSp: Int = 28  // 页面主标（首页欢迎语）
    const val SizeHeadlineMediumSp: Int = 20 // 区块标（面板/卡片区标题）
    const val SizeTitleSp: Int = 15          // 条目标（卡片标题）
    const val SizeBodySp: Int = 15          // 正文
    const val SizeBodyMediumSp: Int = 14    // 次要文
    const val SizeLabelSp: Int = 14         // 按钮
    const val SizeLabelMediumSp: Int = 12   // 标签/chips
    const val SizeCaptionSp: Int = 13      // 时间戳/计数（Mono）

    // ---------- 字距（sp）----------
    const val TrackingDisplayLargeSp: Int = 8
    const val TrackingDisplaySmallSp: Int = 4
    const val TrackingHeadlineLargeSp: Int = 3
    const val TrackingHeadlineMediumSp: Int = 2
    const val TrackingButtonSp: Int = 2
    const val TrackingLabelMediumSp: Int = 2
    const val TrackingCaptionSp: Int = 1
}
