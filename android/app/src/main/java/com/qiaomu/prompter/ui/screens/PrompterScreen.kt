package com.qiaomu.prompter.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.qiaomu.prompter.data.Script
import com.qiaomu.prompter.data.ScriptStore
import com.qiaomu.prompter.data.TextColorPreset
import com.qiaomu.prompter.service.PromptFormatter
import com.qiaomu.prompter.service.PromptLine
import com.qiaomu.prompter.service.ScreenRecordService
import com.qiaomu.prompter.service.ScrollEngine
import com.qiaomu.prompter.service.SpeechFollower
import com.qiaomu.prompter.service.SpeechFollowerState
import com.qiaomu.prompter.ui.GlassCircleButton
import com.qiaomu.prompter.ui.GlassPanel
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun PrompterScreen(
    scriptId: String,
    scriptStore: ScriptStore,
    onClose: () -> Unit,
    requestScreenRecord: ((Int, Intent) -> Unit) -> Unit
) {
    val scripts by scriptStore.scripts.collectAsState()
    var script by remember(scripts, scriptId) {
        mutableStateOf(scriptStore.script(scriptId) ?: Script(title = "", content = ""))
    }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val engine = remember { ScrollEngine(scope) }
    val speechFollower = remember { SpeechFollower(context) }
    var showSettings by remember { mutableStateOf(false) }
    var cameraPermission by remember { mutableStateOf(checkCameraPermission(context)) }
    var isRecording by remember { mutableStateOf(ScreenRecordService.isRecording) }
    var recordDuration by remember { mutableStateOf(0) }

    val promptLines = remember(script.content, script.fontSize) {
        PromptFormatter.lines(script.content, targetCharactersForLine(script.fontSize))
    }
    val lineHeight = script.fontSize * 1.34f
    val contentHeight = promptLines.sumOf { estimatedRowHeight(it.text, script.fontSize).toDouble() }.toFloat()
    val topPaddingFraction = 0.40f
    val bottomPaddingFraction = if (showSettings) 0.40f else 0.34f

    val engineOffset by engine.offset.collectAsState()
    val enginePlaying by engine.isPlaying.collectAsState()
    val engineSpeed by engine.speed.collectAsState()
    val speechState by speechFollower.state.collectAsState()
    val speechProgress by speechFollower.progress.collectAsState()

    val totalHeight = contentHeight + topPaddingFraction * contentHeight + bottomPaddingFraction * contentHeight
    val maxOffset = (totalHeight - contentHeight).coerceAtLeast(0f)

    val initialProgress = script.scrollSpeed / 260f
    LaunchedEffect(Unit) { engine.setSpeed(script.scrollSpeed) }

    DisposableEffect(Unit) {
        onDispose {
            speechFollower.stop()
            // engine cleanup handled by DisposableEffect
        }
    }

    LaunchedEffect(speechProgress) {
        if (speechState == SpeechFollowerState.LISTENING && speechProgress > 0) {
            engine.setOffset((speechProgress * maxOffset).toFloat().coerceIn(0f, maxOffset))
        }
    }

    // 录制时长计时
    LaunchedEffect(isRecording) {
        while (isRecording) {
            kotlinx.coroutines.delay(1000)
            recordDuration = ScreenRecordService.durationSeconds
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {

        // 摄像头背景
        if (cameraPermission) {
            CameraBackground(lifecycleOwner)
        }

        // 半透明遮罩
        Box(
            Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = script.overlayOpacity))
        )

        // 提词文字层
        TextScrollLayer(
            promptLines = promptLines,
            fontSize = script.fontSize,
            lineHeight = lineHeight,
            textColor = script.textColorPreset.color,
            engineOffset = engineOffset,
            topPaddingFraction = topPaddingFraction,
            showSettings = showSettings,
            speechState = speechState,
            speechProgress = speechProgress,
            lineHeightDp = script.fontSize * 1.34f
        )

        // 手势交互层
        InteractionLayer(
            onTap = { showSettings = !showSettings },
            onSpeedDrag = { delta ->
                val newSpeed = (engineSpeed - delta * 0.5f).coerceIn(20f, 260f)
                engine.setSpeed(newSpeed)
            },
            onProgressDrag = { delta ->
                val newOffset = (engineOffset - delta * 1.5f).coerceIn(0f, maxOffset)
                engine.setOffset(newOffset)
            }
        )

        // 顶部工具栏
        AnimatedVisibility(
            visible = showSettings,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopBar(
                onClose = onClose,
                onToggleSettings = { showSettings = false },
                showSettings = true,
                modeText = speechFollower.statusText,
                onModeToggle = {
                    if (speechState != SpeechFollowerState.IDLE) {
                        speechFollower.stop()
                    } else {
                        val progress = if (maxOffset > 0) engineOffset / maxOffset else 0.0
                        speechFollower.start(script.content, progress.toDouble())
                    }
                },
                isRecording = isRecording,
                onRecordToggle = {
                    if (isRecording) {
                        stopScreenRecord(context)
                        isRecording = false
                    } else {
                        startScreenRecord(context, requestScreenRecord) { started ->
                            if (started) {
                                isRecording = true
                                recordDuration = 0
                            }
                        }
                    }
                },
                recordDuration = recordDuration
            )
        }

        // 录制指示器（录制中始终显示）
        if (isRecording && !showSettings) {
            RecordIndicator(
                recordDuration = recordDuration,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 8.dp)
            )
        }

        // 底部控制面板
        AnimatedVisibility(
            visible = showSettings,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            SettingsPanel(
                engine = engine,
                script = script,
                maxOffset = maxOffset,
                onScriptUpdate = { updated ->
                    script = updated
                    scriptStore.save(updated)
                },
                isPlaying = enginePlaying
            )
        }
    }
}

// === 录制功能 ===

private fun startScreenRecord(
    context: Context,
    requestScreenRecord: ((Int, Intent) -> Unit) -> Unit,
    onStarted: (Boolean) -> Unit
) {
    // 检查音频权限
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        != PackageManager.PERMISSION_GRANTED
    ) {
        Toast.makeText(context, "需要麦克风权限才能录制", Toast.LENGTH_SHORT).show()
        onStarted(false)
        return
    }

    requestScreenRecord { resultCode, data ->
        val intent = Intent(context, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_START
            putExtra(ScreenRecordService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenRecordService.EXTRA_RESULT_DATA, data)
        }
        context.startForegroundService(intent)
        ScreenRecordService.setOnRecordingComplete { path ->
            val msg = if (path != null) "录制已保存" else "录制失败"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
        onStarted(true)
    }
}

private fun stopScreenRecord(context: Context) {
    val intent = Intent(context, ScreenRecordService::class.java).apply {
        action = ScreenRecordService.ACTION_STOP
    }
    context.startService(intent)
}

@Composable
private fun RecordIndicator(recordDuration: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Red.copy(alpha = 0.3f))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            Icons.Default.FiberManualRecord,
            contentDescription = null,
            tint = Color.Red,
            modifier = Modifier.size(10.dp)
        )
        Text(
            formatDuration(recordDuration),
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}

// === 摄像头 ===

@Composable
private fun CameraBackground(lifecycleOwner: androidx.lifecycle.LifecycleOwner) {
    val context = LocalContext.current
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview)
                } catch (_: Exception) {}
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

// === 文字滚动层 ===

@Composable
private fun TextScrollLayer(
    promptLines: List<PromptLine>,
    fontSize: Float,
    lineHeight: Float,
    textColor: Color,
    engineOffset: Float,
    topPaddingFraction: Float,
    showSettings: Boolean,
    speechState: SpeechFollowerState,
    speechProgress: Double,
    lineHeightDp: Float
) {
    val scrollState = rememberScrollState()
    val speechLineIdx = if (speechState == SpeechFollowerState.LISTENING)
        (speechProgress * promptLines.size).toInt().coerceIn(0, promptLines.lastIndex)
    else -1

    LaunchedEffect(engineOffset) {
        val target = engineOffset.toInt()
        if (abs(scrollState.value - target) > 2) {
            scrollState.scrollTo(target)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState, enabled = false)
            .padding(top = (topPaddingFraction * lineHeight * promptLines.size).dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height((topPaddingFraction * lineHeight * promptLines.size).dp))
        promptLines.forEachIndexed { i, line ->
            val isSpeechHighlight = speechState == SpeechFollowerState.LISTENING && i == speechLineIdx && line.text.any { it.isLetter() || it.isDigit() }
            Text(
                text = line.text,
                color = if (isSpeechHighlight) Color(0xFF4FC3F7) else textColor,
                fontSize = fontSize.sp,
                lineHeight = lineHeight.sp,
                fontWeight = if (isSpeechHighlight) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .padding(vertical = (lineHeight * 0.12f).dp)
                    .then(
                        if (isSpeechHighlight) Modifier.background(Color.White.copy(0.08f), RoundedCornerShape(6.dp))
                        else Modifier
                    )
            )
        }
        Spacer(Modifier.height(400.dp))
    }
}

// === 交互层 ===

@Composable
private fun InteractionLayer(onTap: () -> Unit, onSpeedDrag: (Float) -> Unit, onProgressDrag: (Float) -> Unit) {
    Row(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxSize().pointerInput(Unit) {
            detectDragGestures { _, dragAmount -> onSpeedDrag(dragAmount.y) }
            detectTapGestures { onTap() }
        })
        Box(Modifier.weight(1f).fillMaxSize().pointerInput(Unit) {
            detectDragGestures { _, dragAmount -> onProgressDrag(dragAmount.y) }
            detectTapGestures { onTap() }
        })
    }
}

// === 顶栏 ===

@Composable
private fun TopBar(
    onClose: () -> Unit,
    onToggleSettings: () -> Unit,
    showSettings: Boolean,
    modeText: String,
    onModeToggle: () -> Unit,
    isRecording: Boolean,
    onRecordToggle: () -> Unit,
    recordDuration: Int
) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassCircleButton {
            IconButton(onClick = onClose, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Close, "关闭", tint = Color.White)
            }
        }
        Spacer(Modifier.weight(1f))

        // 录制按钮
        GlassCircleButton(
            modifier = Modifier.size(44.dp)
        ) {
            IconButton(
                onClick = onRecordToggle,
                modifier = Modifier.size(40.dp)
            ) {
                if (isRecording) {
                    Icon(Icons.Default.Stop, "停止录制", tint = Color.Red, modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Default.FiberManualRecord, "开始录制", tint = Color.Red, modifier = Modifier.size(20.dp))
                }
            }
        }

        if (isRecording) {
            Spacer(Modifier.width(6.dp))
            Text(formatDuration(recordDuration), color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.weight(1f))

        TextButton(
            onClick = onModeToggle,
            modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(0.15f))
        ) {
            Text(modeText, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.weight(1f))

        GlassCircleButton {
            IconButton(onClick = onToggleSettings, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Settings, "设置", tint = Color.White)
            }
        }
    }
}

// === 设置面板 ===

@Composable
private fun SettingsPanel(engine: ScrollEngine, script: Script, maxOffset: Float, onScriptUpdate: (Script) -> Unit, isPlaying: Boolean) {
    val speed by engine.speed.collectAsState()
    val offset by engine.offset.collectAsState()
    GlassPanel(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                GlassCircleButton {
                    IconButton(onClick = { engine.setOffset(maxOf(0f, offset - 240f)) }, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Default.FastRewind, "后退", tint = Color.White)
                    }
                }
                GlassCircleButton {
                    IconButton(onClick = { engine.toggle() }, modifier = Modifier.size(48.dp)) {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, if (isPlaying) "暂停" else "播放", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
                GlassCircleButton {
                    IconButton(onClick = { engine.setOffset(offset + 240f) }, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Default.FastForward, "前进", tint = Color.White)
                    }
                }
                Spacer(Modifier.width(4.dp))
                GlassCircleButton {
                    IconButton(onClick = { engine.reset() }, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Default.Replay, "回到开头", tint = Color.White)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            SliderRow("字号", "${script.fontSize.toInt()}", script.fontSize, 12f..110f) {
                onScriptUpdate(script.copy(fontSize = it))
            }
            Spacer(Modifier.height(6.dp))
            SliderRow("速度", "${speed.toInt()} 字/分", speed, 20f..260f) {
                engine.setSpeed(it)
                onScriptUpdate(script.copy(scrollSpeed = it))
            }
            Spacer(Modifier.height(6.dp))
            val progress = if (maxOffset > 0) offset / maxOffset else 0f
            SliderRow("进度", "${(progress * 100).toInt()}%", offset, 0f..maxOf(1f, maxOffset)) {
                engine.setOffset(it)
            }
        }
    }
}

@Composable
private fun SliderRow(label: String, value: String, current: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White.copy(0.72f), fontSize = 11.sp)
            Text(value, color = Color.White, fontSize = 11.sp)
        }
        Slider(value = current, onValueChange = onChange, valueRange = range, colors = SliderDefaults.colors(thumbColor = Color.White.copy(0.86f), activeTrackColor = Color.White.copy(0.86f)))
    }
}

// === 工具函数 ===

private fun targetCharactersForLine(fontSize: Float): Int {
    return when {
        fontSize < 20 -> 24
        fontSize < 30 -> 20
        fontSize < 45 -> 16
        fontSize < 65 -> 12
        fontSize < 85 -> 9
        else -> 7
    }
}

private fun estimatedRowHeight(text: String, fontSize: Float): Float {
    val lineCount = (text.length.toFloat() / targetCharactersForLine(fontSize)).let { if (it < 1f) 1f else kotlin.math.ceil(it) }
    return fontSize * 1.34f * lineCount
}

private fun scrollingIndex(offset: Float, lines: List<PromptLine>, lineHeight: Float, topPadding: Float): Int {
    val linesTop = topPadding * lineHeight * lines.size
    val currentOffset = offset - linesTop
    val idx = (currentOffset / (lineHeight * 1.34f)).toInt()
    return idx.coerceIn(0, lines.lastIndex)
}

private fun checkCameraPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
}

private val TextColorPreset.color: Color
    get() = when (this) {
        TextColorPreset.WHITE -> Color.White
        TextColorPreset.SILVER -> Color(0xFFC7CCD0)
        TextColorPreset.GRAPHITE -> Color(0xFF808589)
    }
