# Mentara 视觉基座 V3 ·「晨雾与深空」

> 一处改令牌，三端全局生效。可读性优先：正文对比度 ≥ 4.5:1。

## 设计哲学（引用的艺术家）

- **James Turrell（光与空间）**：背景不是颜色，是光的空间——径向渐变从深处升起，边界消失于虚空
- **Mark Rothko（色域绘画）**：深色块承载情绪，大面留白让内容呼吸
- **Claude Monet（印象派）**：晨雾紫灰、睡莲绿、日落金——颜色永远不是纯色，是雾中的混合
- **Gustav Klimt（金色时期）**：金色只做点缀（SunsetGold），从不大面积铺陈
- **teamLab（数字艺术）**：三层雾状渐变随时间缓慢流转，永不停歇

## 完整色表（DesignTokens，V3）

| Token | 值 | 用途 |
|---|---|---|
| ColorVoid | `#0A0E14` | 背景基色（极淡蓝，非纯黑） |
| ColorAbyss | `#0F1419` | 渐层更深 |
| ColorBgDeep | `#060A0F` | 最深处 |
| ColorVeil | `#131820` | 卡片底（蓝灰轻纱） |
| ColorGlimmer | `#1A212B` | 悬浮层微光 |
| ColorPhantom / ColorMistPurple | `#B8A8D4` | 主色·晨雾紫 |
| ColorAureate / ColorSunsetGold | `#E8C87A` | 强调·日落金 |
| ColorLilyGreen | `#7FB89C` | 强调·睡莲绿（莫奈） |
| ColorDeepSeaBlue | `#4A7AB8` | 强调·深海蓝 |
| ColorMoonSilver | `#C0C8D0` | 辅助·月光银 |
| ColorCoral | `#E89A9A` | 警示·暮色珊瑚 |
| ColorTextHigh / Mid / Low | `#E8ECF2` / `#8892A0` / `#4A5260` | 文字三档 |
| ColorMist | `#232B36` | 描边·雾 |
| ColorSuccess | `#7CFFB2` | 成功 |
| ColorMorningPurple | `#B8A8D4` | 晨 accent（06–12） |
| ColorNightPurple | `#4A7AB8` | 夜 accent（18–06，深海蓝） |

## 使用规则

- **accent 随时间**：晨雾紫（06–12）→ 睡莲绿（12–18）→ 深海蓝（18–06）
- **金色克制**：仅进度条端点、鎏金环、一致性条中档——不做大面积背景
- **正文对比度**：TextHigh `#E8ECF2` on Void `#0A0E14` ≈ 15:1；TextMid `#8892A0` on Veil ≈ 5.1:1；TextLow 仅用于可忽略的辅助信息
- **禁 emoji**：UI 内一律纯英文标签或 Canvas 图标（IconSystem V3 20 个）

## 渐变叠加规则（DynamicBackground 第 1 层 → 雾状三层）

1. 底层径向渐变：Void 中心 → BgDeep 边缘（静态）
2. 三层线性雾（动画层，随 clockTime 缓转方向）：
   - 紫灰雾（accent，alpha 0.14）
   - 绿灰雾（LilyGreen，alpha 0.10）
   - 金灰雾（SunsetGold，alpha 0.08）
   - 每层从中心出发向旋转的端点渐隐；夜间模式整体 × 0.7
3. 粒子 / 轨道 / 呼吸光晕照常叠加

## 留白规则（V3）

| 位置 | 旧 | 新 |
|---|---|---|
| 桌面页边距 | 24dp | 48dp |
| 桌面卡间距 | 16dp | 32dp |
| 桌面卡内边距 | 16dp | 28dp |
| Android 页边距 | 16dp | 32dp（SpacePageDp） |
| Android 卡间距 | 10–12dp | 24dp（SpaceCardGapDp） |
| Android 卡内边距 | 12–14dp | 28dp（SpaceCardInnerDp） |

排版：标题 Thin + 大字距（Brand 4sp / PageTitle 3sp）；正文 Regular；数字 Mono；标签全大写。

## 语言

- 默认 English；中文在独立汉化包 `StringsZh.kt`（设置页 Language 切换，即时生效）
