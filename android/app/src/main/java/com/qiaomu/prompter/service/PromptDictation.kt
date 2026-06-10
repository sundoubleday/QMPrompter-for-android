package com.qiaomu.prompter.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PromptDictation(private val context: Context) {
    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _transcript = MutableStateFlow("")
    val transcript = _transcript.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    fun toggle() {
        if (_isRecording.value) stop() else start()
    }

    fun start() {
        stop()
        _errorMessage.value = null
        _transcript.value = ""

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _errorMessage.value = "当前语音识别不可用。"
            return
        }

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer = recognizer

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { _isRecording.value = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                if (_isRecording.value) {
                    _errorMessage.value = "语音输入中断。"
                    stop()
                }
            }
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                if (text.isNotEmpty()) _transcript.value = text
                if (_isRecording.value) recognizer.startListening(intent)
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
                            break
                        }
                    }
                } else {
                    _transcript.value = text
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        recognizer.startListening(intent)
    }

    fun stop() {
        _isRecording.value = false
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
