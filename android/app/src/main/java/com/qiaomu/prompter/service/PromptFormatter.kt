package com.qiaomu.prompter.service

data class PromptLine(val text: String, val characterCount: Int)

object PromptFormatter {
    fun lines(content: String, targetCharactersPerLine: Int = 18): List<PromptLine> {
        val normalized = content.replace("\r\n", "\n").replace("\r", "\n")
        val result = mutableListOf<PromptLine>()

        for (paragraph in normalized.split("\n")) {
            val trimmed = paragraph.trim()
            if (trimmed.isEmpty()) {
                result.add(PromptLine("", targetCharactersPerLine))
                continue
            }
            result.addAll(split(trimmed, targetCharactersPerLine))
        }
        return result.filter { it.text.isNotEmpty() || result.size > 1 }
    }

    private fun split(text: String, target: Int): List<PromptLine> {
        val lines = mutableListOf<PromptLine>()
        var current = ""
        val semanticMin = maxOf(8, target)
        val hardLimit = maxOf(semanticMin, target + 4, (target * 1.35f).toInt())

        for (ch in text) {
            current += ch
            val strong = "。！？；.!?;：:".contains(ch) && current.length >= 4
            val soft = "，、,".contains(ch) && current.length >= semanticMin
            val atLimit = current.length >= hardLimit
            if (strong || soft || atLimit) {
                append(current, lines, target)
                current = ""
            }
        }
        append(current, lines, target)
        return lines
    }

    private fun append(text: String, lines: MutableList<PromptLine>, fallback: Int) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        if (!containsSpeakable(trimmed) && lines.isNotEmpty()) {
            val prev = lines.removeAt(lines.lastIndex)
            val merged = prev.text + trimmed
            lines.add(PromptLine(merged, maxOf(prev.characterCount, merged.length)))
            return
        }
        lines.add(PromptLine(trimmed, maxOf(1, trimmed.length, fallback / 2)))
    }

    private fun containsSpeakable(text: String): Boolean =
        text.any { it.isLetter() || it.isDigit() }
}
