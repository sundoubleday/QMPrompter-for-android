package com.qiaomu.prompter.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Script(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val fontSize: Float = DEFAULT_FONT_SIZE,
    val scrollSpeed: Float = 80f,
    val textColorPreset: TextColorPreset = TextColorPreset.WHITE,
    val overlayOpacity: Float = 0.48f
) {
    val preview: String
        get() = content.replace("\n", " ").trim()

    companion object {
        const val DEFAULT_FONT_SIZE = 42f
    }
}

@Serializable
enum class TextColorPreset(val label: String) {
    WHITE("白"),
    SILVER("银灰"),
    GRAPHITE("深灰");

    companion object {
        val editorChoices = listOf(WHITE, GRAPHITE)
    }
}
