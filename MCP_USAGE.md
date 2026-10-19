# MCP 使用说明（MCP_USAGE）

`mcpServer` 是一个**无第三方依赖**的最小 MCP server，通过 stdio（JSON-RPC 2.0，按行分隔）与 Agent 通信。

## 暴露的工具

| 工具 | 参数 | 说明 |
|------|------|------|
| `search_video` | `query: string` | 按关键词检索视频时间线，返回带时间戳的片段 |
| `ask_question` | `question: string` | 基于视频内容回答问题，返回答案 + 时间戳引用 |

## 构建与运行

```bash
gradle-dist/gradle-8.10/bin/gradle.bat :mcpServer:installDist --offline --no-daemon
# 产物：mcpServer/build/install/mcpServer/bin/mcpServer.bat
```

## 在 Claude Desktop / Cursor 中配置

在 MCP 客户端配置文件中加入（路径按实际调整）：

```json
{
  "mcpServers": {
    "Mentara": {
      "command": "C:\\Users\\Administrator\\Documents\\Default Project\\Mentara\\mcpServer\\build\\install\\mcpServer\\bin\\mcpServer.bat"
    }
  }
}
```

Cursor：`Settings → MCP → Add new MCP server`，类型选 `command`，填入上面的 `command`。

## 手动验证（stdio）

```bash
echo {"jsonrpc":"2.0","id":1,"method":"initialize","params":{}} | mcpServer.bat
echo {"jsonrpc":"2.0","id":2,"method":"tools/list"} | mcpServer.bat
```

## 说明

- 当前数据源为**内置示例时间线**；接入真实 `results/` 目录后即可检索真实视频。
- 未依赖官方 MCP SDK（离线环境不可获取），协议按 JSON-RPC 2.0 手动实现，兼容主流 MCP 客户端的 tools 调用。
