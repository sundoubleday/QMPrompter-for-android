package com.qiaomu.prompter.service

import com.qiaomu.prompter.data.Script
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class DeepSeekScriptGenerator(private val apiKey: String) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun generateScript(prompt: String): String = withContext(Dispatchers.IO) {
        val key = apiKey.trim()
        if (key.isEmpty()) throw Exception("请先填写 DeepSeek API Key。")

        val body = Json.encodeToString(RequestSerializer, DeepSeekRequest(
            model = "deepseek-v4-flash",
            messages = listOf(
                Message(role = "system", content = SYSTEM_PROMPT),
                Message(role = "user", content = "用户的生成需求：\n${prompt.trim()}\n\n请输出一篇可直接放进提词器朗读的中文口播文稿。")
            ),
            max_tokens = 2800,
            stream = false
        ))

        val request = Request.Builder()
            .url("https://api.deepseek.com/chat/completions")
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $key")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("DeepSeek 返回为空。")
        if (!response.isSuccessful) {
            val errorMsg = try {
                json.decodeFromString<ErrorResponse>(responseBody).error.message
            } catch (_: Exception) { null }
            throw Exception(errorMsg ?: "DeepSeek 请求失败：HTTP ${response.code}")
        }

        val decoded = json.decodeFromString<DeepSeekResponse>(responseBody)
        val content = decoded.choices.firstOrNull()?.message?.content?.trim()
            ?: throw Exception("DeepSeek 返回格式异常。")
        if (content.isEmpty()) throw Exception("DeepSeek 没有生成可用正文。")
        return@withContext cleanGeneratedScript(content)
    }

    private fun cleanGeneratedScript(content: String): String {
        var result = content
            .replace("```text", "").replace("```markdown", "").replace("```", "")
            .replace("**", "").replace("`", "")
        val lines = result.lines()
            .map { it.trim() }
            .filter { line ->
                line.isNotEmpty() &&
                !line.startsWith("#") &&
                !line.startsWith("- ") &&
                !line.startsWith("* ") &&
                !line.startsWith(">") &&
                !line.contains("预估时长")
            }
        result = lines.joinToString("\n")
        while (result.contains("\n\n\n")) result = result.replace("\n\n\n", "\n\n")
        return result.trim()
    }

    companion object {
        private const val SYSTEM_PROMPT = """你是向阳乔木的提词器文稿作者，负责把用户的简单想法生成适合直接朗读的中文口播稿。

写作方法参考向阳乔木的读书口播脚本工作流：
从听众真实困境或强认知锚点开始；
如果用户输入的是书名、作者或读书视频主题，先说明作者或这本书为什么值得听，再提炼三到五个真正改变认知的观点；
如果不是书籍主题，也用同样的结构：一个清晰问题、三到五个观点、具体生活场景、自然收束；
每个观点都要用一句话解释，再接一个普通人能立刻看见的场景；
语言要像真人说话，短段落，一句只承载一个意思；
结尾自然，不要强行销售。

输出规则：
只输出口播正文；不要 Markdown；不要代码块；不要标题；不要列表符号；
不要加粗标记；不要小标题；不要"第一点、第二点、首先、其次、最后"；
不要镜头提示、音乐提示、字幕提示或时长说明；不要解释你的写作过程；
文本要适合提词器滚动显示，段落短，换行自然。

避免这些词和句式：震惊、绝了、太牛了、赋能、落地、深度融合、内卷、这个时代、年轻人、精准打击、你知道吗、今天我要告诉你、重点来了、接下来告诉你、划重点。"""
    }
}

@Serializable
private data class DeepSeekRequest(
    val model: String,
    val messages: List<Message>,
    val max_tokens: Int = 2800,
    val stream: Boolean = false
)

@Serializable
private data class Message(val role: String, val content: String)

@Serializable
private data class DeepSeekResponse(val choices: List<Choice>) {
    @Serializable data class Choice(val message: Message)
}

@Serializable
private data class ErrorResponse(val error: ApiError) {
    @Serializable data class ApiError(val message: String)
}

private val RequestSerializer = DeepSeekRequest.serializer()
