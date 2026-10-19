package com.learnagent.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.painterResource

/**
 * 美术资源加载：优先加载 resources/art 目录下的 PNG（程序化生成），
 * 文件不存在时返回 null，调用方自动回退到 Canvas 程序化绘制。
 */

private object ArtAnchor

private val artClassLoader: ClassLoader?
    get() = Thread.currentThread().contextClassLoader ?: ArtAnchor::class.java.classLoader

fun artExists(path: String): Boolean =
    runCatching { artClassLoader?.getResource(path) != null }.getOrDefault(false)

@Composable
fun artPainter(path: String): Painter? =
    if (artExists(path)) runCatching { painterResource(path) }.getOrNull() else null

@Composable
fun artImage(path: String): ImageBitmap? =
    remember(path) {
        if (!artExists(path)) {
            null
        } else {
            runCatching {
                artClassLoader?.getResourceAsStream(path)?.use { loadImageBitmap(it) }
            }.getOrNull()
        }
    }
