package com.qiaomu.prompter.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScriptStore(context: Context) {
    private val prefs = context.getSharedPreferences("qmprompter_scripts", Context.MODE_PRIVATE)
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val key = "scripts_json"

    private val _scripts = MutableStateFlow<List<Script>>(emptyList())
    val scripts = _scripts.asStateFlow()

    init {
        load()
    }

    fun script(id: String): Script? = _scripts.value.find { it.id == id }

    fun createDraft(): Script = Script(title = "未命名文稿", content = "")

    fun createScript(title: String, content: String): Script = Script(
        title = title.trim().ifEmpty { "未命名文稿" },
        content = content
    )

    fun save(script: Script) {
        val updated = script.copy(updatedAt = System.currentTimeMillis())
        val list = _scripts.value.toMutableList()
        val idx = list.indexOfFirst { it.id == updated.id }
        if (idx >= 0) list[idx] = updated else list.add(0, updated)
        list.sortByDescending { it.updatedAt }
        _scripts.value = list
        persist()
    }

    fun delete(script: Script) {
        _scripts.value = _scripts.value.filter { it.id != script.id }
        persist()
    }

    private fun load() {
        val json = prefs.getString(key, null)
        if (json != null) {
            val type = object : TypeToken<List<Script>>() {}.type
            _scripts.value = gson.fromJson(json, type)
        } else {
            _scripts.value = listOf(sampleScript)
            persist()
        }
    }

    private fun persist() {
        prefs.edit().putString(key, gson.toJson(_scripts.value)).apply()
    }

    companion object {
        private val sampleScript = Script(
            title = "试用文稿",
            content = """
大家好，这里是乔木提词器的第一版测试。

这个版本先不录视频，只显示前置摄像头预览，让我可以看着镜头练习表达。

点击屏幕中央可以播放或暂停。
左侧上下滑动可以调整速度。
右侧上下滑动可以跳转进度。

如果这套基础体验顺手，下一步再接远程网页粘贴和同步 API。
            """.trimIndent(),
            fontSize = Script.DEFAULT_FONT_SIZE,
            scrollSpeed = 78f
        )
    }
}
