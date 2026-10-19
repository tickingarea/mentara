package com.learnagent.ocr

import java.awt.GraphicsEnvironment
import java.awt.Robot
import java.io.File
import javax.imageio.ImageIO

/**
 * 屏幕抽帧器（伪"关键帧"）：
 *
 * 在缺少 ffmpeg 的离线环境下，用 java.awt.Robot 抓取当前屏幕作为一帧，
 * 落盘为 PNG，随后可交给 [WindowsOcr] 识别。
 */
object ScreenCapturer {

    /** 抓取主屏为一张临时 PNG，返回文件。 */
    fun captureToFile(): File {
        val robot = Robot()
        val bounds = GraphicsEnvironment
            .getLocalGraphicsEnvironment()
            .defaultScreenDevice
            .defaultConfiguration
            .bounds
        val image = robot.createScreenCapture(bounds)
        val file = File.createTempFile("learnagent_frame", ".png")
        ImageIO.write(image, "png", file)
        return file
    }
}
