package com.qiaomu.prompter.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

enum class SpeechFollowerState {
    IDLE, LISTENING, DENIED, UNAVAILABLE, FAILED
}

class SpeechFollower(private val context: Context) {
    private val _state = MutableStateFlow(SpeechFollowerState.IDLE)
    val state = _state.asStateFlow()

    private val _transcript = MutableStateFlow("")
    val transcript = _transcript.asStateFlow()

    private val _progress = MutableStateFlow(0.0)
    val progress = _progress.asStateFlow()

    val isListening: Boolean get() = _state.value == SpeechFollowerState.LISTENING

    private var speechRecognizer: SpeechRecognizer? = null
    private var scriptIndex = SpeechScriptIndex("")

    val statusText: String
        get() = when (_state.value) {
            SpeechFollowerState.IDLE -> "语音跟随"
            SpeechFollowerState.LISTENING ->
                if (_transcript.value.isEmpty()) "正在听" else "跟随${(_progress.value * 100).toInt()}%"
            SpeechFollowerState.DENIED -> "语音权限未开启"
            SpeechFollowerState.UNAVAILABLE -> "语音识别不可用"
            SpeechFollowerState.FAILED -> "语音识别中断"
        }

    fun start(content: String, initialProgress: Double = 0.0) {
        stop()
        _state.value = SpeechFollowerState.IDLE
        _transcript.value = ""
        _progress.value = initialProgress.coerceIn(0.0, 1.0)
        scriptIndex = SpeechScriptIndex(content, _progress.value)

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.value = SpeechFollowerState.UNAVAILABLE
            return
        }

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer = recognizer

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _state.value = SpeechFollowerState.LISTENING
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                if (_state.value == SpeechFollowerState.LISTENING) {
                    _state.value = SpeechFollowerState.FAILED
                }
            }
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                if (text.isNotEmpty()) {
                    _transcript.value = text
                    _progress.value = scriptIndex.progress(text)
                }
                if (_state.value == SpeechFollowerState.LISTENING) {
                    recognizer.startListening(intent)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults?.getStringArrayList("com.google.android.voicerecognition.EXTRA_PARTIAL_RESULTS")?.firstOrNull() ?: ""
                if (text.isEmpty()) {
                    val allKeys = partialResults?.keySet() ?: emptySet()
                    for (key in allKeys) {
                        val list = partialResults?.getStringArrayList(key)
                        val first = list?.firstOrNull()
                        if (!first.isNullOrEmpty()) {
                            _transcript.value = first
                            _progress.value = scriptIndex.progress(first)
                            break
                        }
                    }
                } else {
                    _transcript.value = text
                    _progress.value = scriptIndex.progress(text)
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        recognizer.startListening(intent)
    }

    fun stop() {
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        if (_state.value == SpeechFollowerState.LISTENING) {
            _state.value = SpeechFollowerState.IDLE
        }
    }

    fun reset() {
        stop()
        _state.value = SpeechFollowerState.IDLE
    }
}

private class SpeechScriptIndex(content: String, initialProgress: Double = 0.0) {
    val normalizedContent: String = normalize(content)
    private var committedOffset: Int = (normalizedContent.length * initialProgress).toInt()
        .coerceIn(0, normalizedContent.length)

    fun progress(transcript: String): Double {
        val spoken = normalize(transcript)
        if (spoken.isEmpty() || normalizedContent.isEmpty()) return atOffset(committedOffset)
        val match = bestMatchEndOffset(spoken)
        if (match != null) committedOffset = maxOf(committedOffset, match)
        else if (committedOffset == 0) committedOffset = maxOf(committedOffset, commonPrefixCount(spoken))
        return atOffset(committedOffset)
    }

    private fun bestMatchEndOffset(spoken: String): Int? {
        var best: Pair<Int, Int>? = null
        for (fragment in candidateFragments(spoken)) {
            for (range in rangesOf(fragment)) {
                val start = range.first
                val end = range.last + 1
                if (!isPlausible(start, end, fragment.length)) continue
                val score = matchScore(start, end, fragment.length)
                if (best == null || score > best.second) best = end to score
            }
        }
        return best?.first
    }

    private fun candidateFragments(spoken: String): List<String> {
        val fragments = mutableListOf<String>()
        fun add(s: String) { if (s.length >= 2 && s !in fragments) fragments.add(s) }
        if (spoken.length <= 96) add(spoken)
        val maxSuffix = minOf(48, spoken.length)
        for (len in maxSuffix downTo 4 step 4) add(spoken.takeLast(len))
        if (spoken.length >= 3) add(spoken.takeLast(3))
        return fragments
    }

    private fun rangesOf(fragment: String): List<IntRange> {
        val ranges = mutableListOf<IntRange>()
        var start = 0
        while (true) {
            val idx = normalizedContent.indexOf(fragment, start)
            if (idx < 0) break
            ranges.add(idx until idx + fragment.length)
            start = idx + 1
        }
        return ranges
    }

    private fun isPlausible(start: Int, end: Int, len: Int): Boolean {
        if (committedOffset == 0) {
            val window = minOf(maxOf(80, normalizedContent.length / 5), maxOf(80, len * 16))
            return len >= 8 || start <= window
        }
        if (end + maxOf(12, len * 2) < committedOffset) return false
        if (len < 12 && start > committedOffset + maxOf(80, len * 18)) return false
        return true
    }

    private fun matchScore(start: Int, end: Int, len: Int): Int {
        val anchor = abs(start - committedOffset)
        val backward = maxOf(0, committedOffset - end)
        val initial = if (committedOffset == 0) start * 3 else 0
        return len * 1000 - anchor * 3 - backward * 8 - initial
    }

    private fun atOffset(offset: Int): Double = (offset.toDouble() / normalizedContent.length.coerceAtLeast(1)).coerceIn(0.0, 1.0)


    private fun commonPrefixCount(spoken: String): Int =
        normalizedContent.zip(spoken).takeWhile { it.first == it.second }.size

    companion object {
        fun normalize(value: String): String =
            value.lowercase().filter { it.isLetter() || it.isDigit() }
    }
}
