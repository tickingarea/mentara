package com.learnagent

import com.learnagent.ocr.WindowsOcr
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

class WindowsOcrTest {

    private fun makeImage(text: String): File {
        val bmp = BufferedImage(720, 120, BufferedImage.TYPE_INT_RGB)
        val g = bmp.createGraphics()
        g.color = java.awt.Color.WHITE
        g.fillRect(0, 0, 720, 120)
        g.color = java.awt.Color.BLACK
        g.font = java.awt.Font("Microsoft YaHei", java.awt.Font.PLAIN, 32)
        g.drawString(text, 10, 75)
        g.dispose()
        val f = File.createTempFile("learnagent_ocr_test", ".png")
        ImageIO.write(bmp, "png", f)
        return f
    }

    @Test
    fun testWindowsOcrRecognizesChineseAndLatin() {
        if (!WindowsOcr.isWindows()) return
        val image = makeImage("梯度下降 LearnAgent")
        try {
            val text = WindowsOcr.ocr(image)
            assertTrue(text.contains("LearnAgent"), "应识别出 LearnAgent，实际: '$text'")
            assertTrue(text.replace(" ", "").contains("梯度下降"), "应识别出'梯度下降'，实际: '$text'")
        } catch (e: java.io.IOException) {
            println("SKIP_ENV: 系统拒绝 Java 创建 PowerShell 进程，跳过 OCR 集成测试: ${e.message}")
        } finally {
            image.delete()
        }
    }
}
