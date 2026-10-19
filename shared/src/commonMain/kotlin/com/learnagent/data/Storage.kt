package com.learnagent.data

import kotlinx.serialization.json.Json

/**
 * 存储接口定义
 * 
 * 负责本地文件系统的读写操作
 */
interface Storage {
    
    /**
     * 获取结果目录路径
     */
    fun getResultsDir(): String
    
    /**
     * 写入JSON文件
     * 
     * @param path 文件路径
     * @param content JSON内容
     */
    fun writeJson(path: String, content: String)
    
    /**
     * 读取JSON文件
     * 
     * @param path 文件路径
     * @return JSON内容，如果文件不存在返回null
     */
    fun readJson(path: String): String?
    
    /**
     * 列出所有视频ID
     * 
     * @return 视频ID列表
     */
    fun listVideoIds(): List<String>
    
    /**
     * 确保视频目录存在
     * 
     * @param videoId 视频ID
     * @return 视频目录路径
     */
    fun ensureVideoDir(videoId: String): String
    
    /**
     * 删除视频目录
     * 
     * @param videoId 视频ID
     */
    fun deleteVideoDir(videoId: String)
    
    /**
     * 检查文件是否存在
     * 
     * @param path 文件路径
     * @return 文件是否存在
     */
    fun fileExists(path: String): Boolean
    
    /**
     * 删除文件
     * 
     * @param path 文件路径
     */
    fun deleteFile(path: String)
}

/**
 * JSON配置对象
 * 
 * 统一配置 kotlinx.serialization 的 Json 实例
 */
object JsonConfig {
    val json: Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
    }
}