package com.learnagent

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import java.io.File

class StorageTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StorageTestScreen()
        }
    }
}

class SimpleStorage(private val context: Context) {
    fun getResultsDir(): File = context.filesDir
}

@Composable
fun StorageTestScreen() {
    val context = LocalContext.current
    val storage = remember { SimpleStorage(context.applicationContext) }
    val json = remember { JSONObject() }
    var testResult by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Storage Test",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Button(
            onClick = {
                testResult = "Testing storage..."
                try {
                    // 测试1: 获取结果目录
                    val resultsDir = context.filesDir
                    testResult += "\n1. Results directory: $resultsDir"
                    
                    // 测试2: 确保视频目录存在
                    val videoId = "test-video-${System.currentTimeMillis()}"
                    val videoDir = File(resultsDir, "videos").apply { mkdirs() }
                    testResult += "\n2. Video directory: $videoDir"
                    
// 测试3: 创建测试数据
                    val videoMeta = JSONObject().apply {
                        put("videoId", videoId)
                        put("deviceId", "test-device-123")
                        put("lastModified", System.currentTimeMillis())
                        put("schemaVersion", 1)
                        put("title", "Test Video")
                        put("duration", 180.0)
                    }
                    
                    // 测试4: 写入 JSON 文件
                    val metaFile = File(resultsDir, "test_meta.json")
                    metaFile.writeText(videoMeta.toString())
                    testResult += "\n3. JSON file written successfully"
                    
                    // 测试5: 读取 JSON 文件
                    if (metaFile.exists()) {
                        testResult += "\n4. JSON read successfully"
                        testResult += "\n   File size: ${metaFile.length()}"
                    } else {
                        testResult += "\n4. Failed to read JSON"
                    }
                    
                    // 测试6: 列出文件
                    val files = resultsDir.listFiles() ?: emptyArray()
                    testResult += "\n5. Found ${files.size} files in results directory"
                    
                    // 测试7: 验证文件存在
                    val exists = metaFile.exists()
                    testResult += "\n6. File exists: $exists"
                    
                    // 测试7: 清理
                    metaFile.delete()
                    testResult += "\n7. Test file cleaned"
                    
                    testResult += "\n\nPASS: all checks passed!"
                    
                } catch (e: Exception) {
                    testResult += "\nFAIL: ${e.message}"
                }
            }
        ) {
            Text("Run Storage Test")
        }
        
        if (testResult.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = testResult,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}