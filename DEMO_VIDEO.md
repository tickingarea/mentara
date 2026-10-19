# 螺旋线滚动路径 — 视频演示方案（DEMO_VIDEO.md）

> 本文档对应 desktopApp 详情页（`Main.kt` 中 `ContentCards`）的螺旋线滚动路径。
> 螺旋线实现位于 desktopApp；androidApp 详情（FocusTab）结构不同，本次未改动。

## 1. 演示模式说明（已内置，DEMO_MODE: YES）

- **开启**：在详情页（导入内容后的时间轴视图）**长按金色 HERO 标题 3 秒**。
- **表现**：右上角出现 `DEMO ● SPIRAL` 指示；列表开始自动向下滚动（≈560 px/s），
  滚到底部后自动回到顶部继续循环，适合录屏 10 秒。
- **关闭**：再次长按标题 3 秒。
- 演示速度低于“快速滚动”阈值（1200 px/s），所以录制时螺旋幅度保持 100%（20dp / 1.5°），
  便于观察完整效果。
- 人工滚动时依然生效：快速滚动幅度衰减到 30%，停止滚动 300ms 后以 300ms 动画恢复 100%。

## 2. 录制步骤

### 方案 A（推荐，Desktop 端 — 本次实现所在端）

1. 启动桌面端（本机 `./gradlew` 包装器已损坏，使用项目自带 Gradle 8.10）：
   ```powershell
   .\gradle-dist\gradle-8.10\bin\gradle.bat :desktopApp:run
   ```
   或用 IDE 直接运行 `desktopApp/src/main/kotlin/com/learnagent/Main.kt` 的 `main`。
2. 导入/选择一个已有内容的视频，进入详情页（时间轴模式），确保列表条目足够长。
3. 用 OBS Studio（窗口采集）或 Windows 自带 Xbox Game Bar（Win+G）开始录制。
4. **长按金色标题 3 秒** 启动演示模式。
5. 录制 10 秒后停止录制。
6. 导出/保存为 `spiral_demo.mp4`。

### 方案 B（Android 模拟器 — 备选）

> 注意：当前螺旋线只落在 desktopApp 详情页；Android 详情页（FocusTab）结构不同、未改动，
> 直接录制 Android 不会出现螺旋效果。仅当你后续把同一 `SpiralScrollItem` 应用到
> androidApp 详情页后，本方案才有效。

```bash
adb shell screenrecord /sdcard/spiral_demo.mp4
# 滚动 10 秒后 Ctrl+C 停止录制
adb pull /sdcard/spiral_demo.mp4
```

## 3. 预期效果描述

- 每个条目按其在视口中的垂直位置 `focus ∈ [-1, 1]` 变换：
  - 横向正弦偏移：`translationX = sin(focus * PI) * 20dp`（视口中心与上下边缘为 0，1/4、3/4 位置最大）。
  - Z 轴旋转：`rotationZ = focus * 1.5°`（顶部 -1.5° → 底部 +1.5°）。
  - 边缘微缩：`scale = 1 - |focus| * 0.02`（最大 2%）。
- 形成“条目沿螺旋通道滑过”的三维错觉。
- 所有变换只作用于 `graphicsLayer`（绘制层），**不改变布局尺寸、不拦截点击**。
- 快速滚动 → 幅度衰减至 30%；停止 300ms → 300ms 平滑恢复 100%。

## 4. 视频文件命名与存放路径

- 文件名：`spiral_demo.mp4`
- 建议路径：`learnagent/docs/media/spiral_demo.mp4`（如目录不存在请手动创建）
- 备选路径：`learnagent/spiral_demo.mp4`

## 5. 生成说明

- 视频文件**无法由代码自动生成**，必须由用户按上述步骤手动录制。
- 代码侧已提供演示模式（长按 3 秒自动滚动），无需人工持续滚动即可稳定录制。
