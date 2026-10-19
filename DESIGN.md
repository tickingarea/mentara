# Mentara 设计语言 · 幻域级（Ethereal Luxury）

> 2026 · Mentara 不是工具，是一个活着的智慧存在 / Mentara is not a tool — it is a living presence of intelligence.

---

## 一、设计哲学

Mentara 的界面应该像**漂浮在虚空中的光**：没有边框、没有硬边界、没有死寂的纯色。
它在呼吸，它在倾听，它会因为你的存在而改变。

### 五个气质关键词

| 关键词 | 含义 | 视觉手段 |
|--------|------|----------|
| **幻域 Ethereal** | 界面像漂浮在虚空中的光 | 径向渐变背景、无边框卡片、光晕代替阴影 |
| **轻浮 Levity** | 元素有微妙浮动，像水母 | 常驻微动画、粒子漂移、卡片 1-2° 默认倾斜 |
| **高尚 Noble** | 配色深沉高贵，不喧哗 | 深紫黑 + 香槟金 + 月光银，低饱和、低对比 |
| **鲜活 Alive** | 背景永不停歇，像呼吸 | 知识流粒子、10 秒周期呼吸光晕、永不静止 |
| **幻化 Metamorphic** | 界面会因用户行为而改变 | 时间 / 频率 / 时长 / 内容量 四因素驱动 |

### 反面（禁止出现）

死寂、厚重、边界分明、高对比、扁平、纯色块、转圈 loading。

### 参考意象

科幻全息界面 · 东方水墨留白 · 深海生物荧光 · 教堂彩色玻璃光。

---

## 二、配色系统

### 暗色主题（主）

| 角色 | 色值 | 名称 |
|------|------|------|
| 背景·虚空 | `#0B0710` | Void |
| 背景·深渊 | `#140D1F` | Abyss |
| 卡片·轻纱 | `#1A1226` | Veil |
| 悬浮·微光 | `#261A38` | Glimmer |
| 主色·幻紫 | `#C9A8FF` | Phantom |
| 主色·鎏金 | `#E8C87A` | Aureate |
| 辅助·月银 | `#A8B0C0` | MoonSilver |
| 强调·珊瑚 | `#FF7A8A` | Coral |
| 文字·主 | `#F0EAF5` | TextHigh |
| 文字·次 | `#8A80A0` | TextMid |
| 文字·三 | `#4A4058` | TextLow |
| 描边·微 | `#2A1F3A` | Mist |

### 渐变规则（关键）

- **背景**：从中心向外的径向渐变 `#0F0A18 → #060308`（不是纯色）
- **卡片**：微渐变 `#1A1226 → #140D1F`，透明度 0.6，漂浮在虚空上
- **强调元素**：线性渐变 `#C9A8FF → #E8C87A`
- **呼吸光晕**：幻紫径向渐隐，半径 400px，透明度 0.05–0.10

### 时间幻化（因素一）

| 时段 | 主色 | 名称 |
|------|------|------|
| 06–12 晨 | `#D4A8FF` | MoonlightPurple（暖紫） |
| 12–18 昼 | `#C9A8FF` | Phantom（默认） |
| 18–06 夜 | `#A880FF` | DeepPurple（深紫） |

---

## 三、字体系统

全部使用**系统内置字体**，不引入任何 .ttf/.otf。中文按系统回退渲染。

### 桌面字体栈

- 主字体：`Segoe UI Variable Display` → 回退 `Segoe UI` → `Bahnschrift Light`
- 数字：`Cascadia Code` → 回退 `Consolas`
- 运行时通过 AWT 字体枚举自动挑选可用字族。

### Android 字体栈

- 主字体：`sans-serif-light`（Roboto Light）
- 数字：`monospace`

### 字号规格

| 用途 | 字号 | 字重 | 字距 | 颜色 |
|------|------|------|------|------|
| 品牌标题 Mentara | 32sp | Light(300) | 4sp | TextHigh |
| Splash 主标题 | 72sp | Light | 12sp | TextHigh + 辉光 |
| 页面标题 | 20sp | Normal | 1.5sp | TextHigh |
| 副标题 | 14sp | Light | 2sp | MoonSilver |
| 正文 | 15sp | Normal | 0.3sp | TextHigh |
| 时间戳 | 13sp | Mono | 0.5sp | TextMid |
| 按钮 | 14sp | Medium | 1.2sp | 全大写（拉丁字符） |

---

## 四、动效规格

| 名称 | 规格 | 用途 |
|------|------|------|
| fast | tween 120ms FastOutSlowIn | hover / 按压 |
| standard | tween 220ms FastOutSlowIn | 面板切换 |
| slow | tween 320ms FastOutSlowIn | 页面切换 |
| spring | DampingRatio 0.75 / Stiffness Low | 入场 / 回弹 |
| drift | 20–60s 正弦周期 | 粒子漂浮 |
| breathe | 1.4s / 10s 周期 | 光球 / 光晕 |

- 卡片入场：从下方浮起 + 淡入（spring + fadeIn，≈400ms）
- 卡片退场：向下沉 + 淡出（300ms）
- Tab 切换：内容从远处飘来（scale 0.94→1 + fadeIn 260ms）
- 拖动主内容区：rotationY 最多 ±15°，松开 spring 回到 0
- 成功：顶部中心向两侧扩散绿色光带，2 秒后消散
- 错误：界面轻微抖动 ±3px，卡片边缘短暂变珊瑚色
- 等待：呼吸光球（不是转圈）

---

## 五、音效设计

全部用 `javax.sound.sampled.SourceDataLine` **代码实时合成**，44100Hz / 16-bit / 单声道，不引入任何音频文件。
采样技术：Karplus-Strong 拨弦、FM 钟声、ADSR 包络、20–50ms 延迟混响模拟。

| 音效 | 合成 | 时长 | 触发点 |
|------|------|------|--------|
| HOVER | 正弦 880Hz，指数衰减 | 25ms | 鼠标滑过可点击元素 |
| TAP | KS 拨弦 523Hz + 正弦 659Hz 双音 | 80ms | 点击按钮/卡片 |
| SUCCESS | C5→E5→G5 三音琶音 | 300ms | 导入成功、保存 |
| ERROR | 方波 220Hz + 正弦 110Hz | 200ms | 处理失败 |
| OPEN | 正弦 300→800Hz 扫频 | 150ms | 打开对话框 |
| CLOSE | 正弦 800→300Hz 扫频 | 120ms | 关闭对话框 |
| SPLASH_TONE | 220Hz 基频 + 440/660/880 泛音 | 2000ms | Splash 幕 1 / 幕 5 |
| AMBIENT | 60Hz + 90Hz 极低音 | — | 默认关闭 |
| CHIME | FM 钟声 | 900ms | Splash 幕 2 三声叮 |
| DRONE | 110Hz + 165Hz 低频渐起 | 1600ms | Splash 幕 3 |
| WHOOSH | 带通噪声扫频 | 600ms | Splash 幕 4 / 幕 6 |
| REST | 柔和双音提醒 | 420ms | 使用超过 1 小时 |

### 触感反馈（桌面）

- 关键操作时窗口微震（1–3px，约 30–70ms，Window.setLocation）
- 仅用于「成功」与「错误」

---

## 六、人为因素变化规则

| 因素 | 规则 | 效果 |
|------|------|------|
| **时间** | 晨 6–12 / 昼 12–18 / 夜 18–6 | 主色 暖紫 / 幻紫 / 深紫 |
| **操作频率** | 近 60 秒点击次数 | 快 → 粒子速度 +20%；慢 → −20% |
| **空闲** | 无操作 30 秒 | 沉睡模式：粒子变慢、光晕变淡 |
| **使用时长** | > 1 小时 | 柔和音效 + “该休息了 · Time to rest” |
| | > 3 小时 | 夜读模式：整体更暗更柔 |
| **内容量** | 导入视频越多 | 粒子 36 → 最多 80，“知识在生长” |

---

## 七、图标与美术资源

- 8 个图标全部 Canvas 程序化绘制：1.5dp 描边、圆角端点、24×24 基准。
- 美术资源（Splash 背景 / 主背景纹理 / Logo 标记 / 空状态光球 / 噪点）由 `scripts/generate_art.ps1` **程序化生成** PNG，存放于 `desktopApp/src/main/resources/art/`。
- 资源缺失时自动回退到 Kotlin Canvas 程序化绘制。
- 质量底线：噪点抖动消除色带、抗锯齿、渐变混合、60fps。

---

## 八、平台说明

- `Theme.kt`、`Ambience.kt`、`DynamicBackground.kt`、`Float3D.kt`、`SplashScreen.kt`、`Feedback.kt`、`SoundSynth.kt` 位于 desktopApp（Compose Desktop / AWT / javax.sound）。
- Android 仅保留共享逻辑与主题色板（`androidApp/ui/theme`），不引入额外依赖，保证 APK 保底。
- 功能逻辑、数据绑定、包名 `com.learnagent` 一律不变。
