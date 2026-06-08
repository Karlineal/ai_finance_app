# DeepSeek API 调用指南

> 基于 [DeepSeek 官方文档](https://api-docs.deepseek.com/zh-cn/) 整理，适用于 iFinance App 项目。

## 目录

- [可用模型](#可用模型)
- [Base URL](#base-url)
- [认证方式](#认证方式)
- [请求格式](#请求格式)
- [思考模式配置](#思考模式配置)
- [响应格式](#响应格式)
- [错误码说明](#错误码说明)
- [价格信息](#价格信息)
- [Kotlin/Retrofit 集成示例](#kotlinretrofit-集成示例)
- [常见问题排查](#常见问题排查)

---

## 可用模型

| 模型名 | 说明 | 上下文 | 最大输出 |
|--------|------|--------|----------|
| `deepseek-v4-flash` | 快速模型，支持思考/非思考模式切换 | 1M | 384K |
| `deepseek-v4-pro` | 专业模型，更强推理能力 | 1M | 384K |
| `deepseek-chat` | ⚠️ 将于 2026/07/24 弃用（= flash 非思考模式） | - | - |
| `deepseek-reasoner` | ⚠️ 将于 2026/07/24 弃用（= flash 思考模式） | - | - |

**推荐：** 新项目直接使用 `deepseek-v4-flash`，性价比最高。

---

## Base URL

| 格式 | URL |
|------|-----|
| OpenAI 兼容格式 | `https://api.deepseek.com` |
| Anthropic 兼容格式 | `https://api.deepseek.com/anthropic` |

---

## 认证方式

```bash
# Header 方式
Authorization: Bearer <your-api-key>
```

API Key 在 [DeepSeek 控制台](https://platform.deepseek.com/) 获取。

---

## 请求格式

### 基础请求

```json
POST /chat/completions
Content-Type: application/json
Authorization: Bearer <your-api-key>

{
  "model": "deepseek-v4-flash",
  "messages": [
    {"role": "system", "content": "你是一个智能助手"},
    {"role": "user", "content": "你好"}
  ],
  "stream": false
}
```

### 完整参数说明

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `model` | string | ✅ | - | `deepseek-v4-flash` 或 `deepseek-v4-pro` |
| `messages` | array | ✅ | - | 消息列表，支持 system/user/assistant/tool 角色 |
| `max_tokens` | integer | ❌ | 模型默认 | 最大生成 token 数 |
| `stream` | boolean | ❌ | `false` | 是否启用流式输出 |
| `thinking` | object | ❌ | `null` | 思考模式配置 |
| `reasoning_effort` | string | ❌ | `"high"` | 思考强度：`"high"` 或 `"max"` |
| `temperature` | number | ❌ | `1` | 温度 (0~2)，**思考模式下不支持** |
| `top_p` | number | ❌ | `1` | 核采样 (0~1)，**思考模式下不支持** |
| `response_format` | object | ❌ | - | `{"type": "json_object"}` 启用 JSON 模式 |
| `stop` | string/array | ❌ | - | 停止序列，最多 16 个 |
| `tools` | array | ❌ | - | 工具/函数定义，最多 128 个 |
| `tool_choice` | string/object | ❌ | `"auto"` | 工具选择策略 |
| `user_id` | string | ❌ | - | 自定义用户 ID |

---

## 思考模式配置

### 启用思考模式

```json
{
  "model": "deepseek-v4-flash",
  "messages": [...],
  "thinking": {
    "type": "enabled"
  },
  "reasoning_effort": "high"
}
```

### 思考模式参数

| 参数 | 值 | 说明 |
|------|-----|------|
| `thinking.type` | `"enabled"` | 启用思考模式 |
| `thinking.type` | `"disabled"` | 禁用思考模式 |
| `reasoning_effort` | `"high"` | 高强度思考（默认） |
| `reasoning_effort` | `"max"` | 最大强度思考 |

### ⚠️ 思考模式限制

**思考模式下以下参数不支持：**
- `temperature`
- `top_p`
- `presence_penalty`
- `frequency_penalty`

设置这些参数不会报错，但不会生效。

### 多轮对话中 reasoning_content 的规则

- 两个 user 消息之间，如果 assistant **没有**工具调用 → 下一轮**不需要**回传 `reasoning_content`
- 两个 user 消息之间，如果 assistant **有**工具调用 → 后续所有轮次**必须**回传 `reasoning_content`（否则 400 报错）

---

## 响应格式

### 非流式响应

```json
{
  "id": "chatcmpl-xxx",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "AI 回复内容",
        "reasoning_content": "思考过程（仅思考模式）"
      },
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 100,
    "completion_tokens": 200,
    "total_tokens": 300,
    "prompt_cache_hit_tokens": 50,
    "prompt_cache_miss_tokens": 50,
    "completion_tokens_details": {
      "reasoning_tokens": 80
    }
  }
}
```

### 响应字段说明

| 字段 | 说明 |
|------|------|
| `choices[].message.content` | AI 回复内容 |
| `choices[].message.reasoning_content` | 思考过程（思考模式独有） |
| `choices[].finish_reason` | `stop`/`length`/`tool_calls` |
| `usage.prompt_cache_hit_tokens` | 缓存命中的 token 数 |
| `usage.completion_tokens_details.reasoning_tokens` | 思考消耗的 token 数 |

---

## 错误码说明

| HTTP 状态码 | 含义 | 常见原因 |
|-------------|------|----------|
| 400 | 请求格式错误 | 参数不正确、thinking 格式错误 |
| 401 | 认证失败 | API Key 无效或缺失 |
| 402 | 余额不足 | 账户余额用完 |
| 422 | 参数校验失败 | 参数类型或范围错误 |
| 429 | 请求过多 | 超过并发限制 |
| 500 | 服务器错误 | DeepSeek 服务异常 |

---

## 价格信息

| 模型 | 输入(缓存命中) | 输入(缓存未命中) | 输出 |
|------|----------------|-----------------|------|
| deepseek-v4-flash | ¥0.02/百万tokens | ¥1/百万tokens | ¥2/百万tokens |
| deepseek-v4-pro | ¥0.025/百万tokens | ¥3/百万tokens | ¥6/百万tokens |

**并发限制：** flash 2500, pro 500

---

## Kotlin/Retrofit 集成示例

### 1. 数据模型

```kotlin
@Serializable
data class DeepSeekRequest(
    val model: String = "deepseek-v4-flash",
    val messages: List<DeepSeekMessage>,
    val max_tokens: Int = 4000,
    val stream: Boolean = false,
    val thinking: ThinkingConfig? = null,
    val reasoning_effort: String? = null,
)

@Serializable
data class ThinkingConfig(
    val type: String = "enabled",
)

@Serializable
data class DeepSeekMessage(
    val role: String,
    val content: String,
)

@Serializable
data class DeepSeekResponse(
    val id: String,
    val choices: List<DeepSeekChoice>,
    val usage: DeepSeekUsage? = null,
)

@Serializable
data class DeepSeekChoice(
    val index: Int,
    val message: DeepSeekMessage,
    val finish_reason: String? = null,
)

@Serializable
data class DeepSeekUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int,
)
```

### 2. Retrofit API 接口

```kotlin
interface DeepSeekApi {
    @POST("chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: DeepSeekRequest,
    ): DeepSeekResponse

    companion object {
        const val BASE_URL = "https://api.deepseek.com/"
    }
}
```

### 3. 调用示例

```kotlin
// 思考模式（推荐）
val request = DeepSeekRequest(
    messages = listOf(
        DeepSeekMessage(role = "system", content = "你是一个智能助手"),
        DeepSeekMessage(role = "user", content = "分析我的财务状况"),
    ),
    thinking = ThinkingConfig(type = "enabled"),
    reasoning_effort = "high",
)

val response = deepSeekApi.chatCompletion(
    authorization = "Bearer ${BuildConfig.DEEPSEEK_API_KEY}",
    request = request,
)

// 非思考模式（快速响应）
val request = DeepSeekRequest(
    messages = messages,
    thinking = ThinkingConfig(type = "disabled"),
)
```

### 4. 处理响应

```kotlin
val content = response.choices.firstOrNull()?.message?.content
val reasoning = response.choices.firstOrNull()?.message?.reasoning_content

// 思考模式下，reasoning_content 包含思考过程
// content 是最终回复
```

---

## 常见问题排查

### Q1: HTTP 400 错误

**可能原因：**
- `thinking` 参数格式错误：应该是 `{"type": "enabled"}` 而非 `{"budget_tokens": N}`
- 思考模式下传了 `temperature` 或 `top_p` 参数
- `model` 名称拼写错误

**解决方案：**
```kotlin
// ❌ 错误
DeepSeekRequest(
    thinking = ThinkingBudget(budget_tokens = 2000),
    temperature = 0.7,
)

// ✅ 正确
DeepSeekRequest(
    thinking = ThinkingConfig(type = "enabled"),
    reasoning_effort = "high",
    // 不要传 temperature
)
```

### Q2: HTTP 401 错误

**可能原因：**
- API Key 无效或已过期
- `Authorization` header 格式错误

**解决方案：**
- 检查 `local.properties` 中的 `DEEPSEEK_API_KEY`
- 确保格式为 `Bearer sk-xxxxx`

### Q3: 思考模式响应很慢

**原因：** 思考模式需要额外的推理时间

**解决方案：**
- 使用 `reasoning_effort: "high"` 而非 `"max"`
- 或禁用思考模式：`thinking.type = "disabled"`

### Q4: 多轮对话报 400

**原因：** 工具调用后未回传 `reasoning_content`

**解决方案：** 在包含工具调用的对话轮次中，必须回传 assistant 的 `reasoning_content`

---

## 更新日志

| 日期 | 变更 |
|------|------|
| 2026-06-08 | 初始版本，基于 deepseek-v4-flash |

---

## 参考链接

- [DeepSeek API 官方文档](https://api-docs.deepseek.com/zh-cn/)
- [DeepSeek 控制台](https://platform.deepseek.com/)
- [DeepSeek 定价页面](https://api-docs.deepseek.com/zh-cn/quick_start/pricing)
