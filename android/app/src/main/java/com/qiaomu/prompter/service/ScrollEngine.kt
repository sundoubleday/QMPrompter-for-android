package com.qiaomu.prompter.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

class ScrollEngine(private val scope: CoroutineScope) {
    private val _offset = MutableStateFlow(0f)
    val offset = _offset.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _speed = MutableStateFlow(80f)
    val speed = _speed.asStateFlow()

    private var lineHeight = 84f
    private var averageCharactersPerLine = 18f
    private var maximumOffset = 0f
    private var followTargetOffset: Float? = null

    private var tickJob: Job? = null

    fun configure(speed: Float, lineHeight: Float, averageCharactersPerLine: Float, maximumOffset: Float) {
        this._speed.value = speed.coerceIn(20f, 260f)
        this.lineHeight = lineHeight.coerceAtLeast(40f)
        this.averageCharactersPerLine = averageCharactersPerLine.coerceAtLeast(6f)
        this.maximumOffset = maximumOffset.coerceAtLeast(0f)
        _offset.value = _offset.value.coerceIn(0f, this.maximumOffset)
        followTargetOffset?.let {
            this.followTargetOffset = it.coerceIn(0f, this.maximumOffset)
        }
    }

    fun play() {
        if (_isPlaying.value) return
        followTargetOffset = null
        _isPlaying.value = true
        ensureTicking()
    }

    fun pause() {
        _isPlaying.value = false
    }

    fun toggle() {
        if (_isPlaying.value) pause() else play()
    }

    fun setSpeed(value: Float) {
        _speed.value = value.coerceIn(20f, 260f)
    }

    fun setOffset(value: Float) {
        followTargetOffset = null
        _offset.value = value.coerceIn(0f, maximumOffset)
    }

    fun follow(to: Float) {
        followTargetOffset = to.coerceIn(0f, maximumOffset)
        _isPlaying.value = false
        ensureTicking()
    }

    fun stopFollowing() {
        followTargetOffset = null
    }

    fun reset() {
        _offset.value = 0f
        followTargetOffset = null
        pause()
    }

    private fun ensureTicking() {
        if (tickJob?.isActive == true) return
        tickJob = scope.launch(Dispatchers.Main) {
            var lastTime = System.nanoTime()
            while (isActive && (_isPlaying.value || followTargetOffset != null)) {
                val now = System.nanoTime()
                val delta = (now - lastTime) / 1_000_000_000f
                lastTime = now
                if (delta > 0f && delta < 0.5f) tick(delta)
                delay(16)
            }
        }
    }

    private fun tick(delta: Float) {
        val target = followTargetOffset
        if (target != null) {
            val response = (delta * 12f).coerceAtMost(1f)
            val next = _offset.value + (target - _offset.value) * response
            if (abs(next - target) < 0.5f) {
                _offset.value = target
                followTargetOffset = null
            } else {
                _offset.value = next
            }
            return
        }
        if (_isPlaying.value) {
            val visualTuningFactor = 1.85f
            val linesPerSecond = (_speed.value / averageCharactersPerLine) / 60f * visualTuningFactor
            val pixelsPerSecond = linesPerSecond * lineHeight
            _offset.value = (_offset.value + pixelsPerSecond * delta).coerceIn(0f, maximumOffset)
            if (_offset.value >= maximumOffset) pause()
        }
    }
}
