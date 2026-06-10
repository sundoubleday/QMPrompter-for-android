package com.qiaomu.prompter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qiaomu.prompter.data.Script
import com.qiaomu.prompter.data.ScriptStore
import com.qiaomu.prompter.data.TextColorPreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptEditorScreen(
    scriptId: String,
    scriptStore: ScriptStore,
    onBack: () -> Unit,
    onStartPrompter: (String) -> Unit
) {
    val scripts by scriptStore.scripts.collectAsState()
    var script by remember(scripts, scriptId) {
        mutableStateOf(scriptStore.script(scriptId) ?: Script(title = "未命名文稿", content = ""))
    }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showClearConfirm by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val canStart = script.content.trim().isNotEmpty()

    LaunchedEffect(scripts) {
        val current = scriptStore.script(scriptId)
        if (current != null && current.id == script.id) script = current
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(script.title.ifEmpty { "未命名文稿" }, fontWeight = FontWeight.SemiBold, maxLines = 1)
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (script.content.trim().isNotEmpty()) scriptStore.save(script)
                        onBack()
                    }) { Icon(Icons.Default.ArrowBack, "返回") }
                },
                actions = {
                    TextButton(
                        onClick = {
                            scriptStore.save(script)
                            onBack()
                        },
                        enabled = script.content.trim().isNotEmpty()
                    ) { Text("保存") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("文稿") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("显示") })
            }

            when (selectedTab) {
                0 -> ScriptTab(
                    script = script,
                    onContentChange = { script = script.copy(content = it) },
                    onPaste = {
                        val text = clipboardManager.getText()?.text ?: return@ScriptTab
                        if (text.isNotEmpty()) {
                            script = script.copy(content = if (script.content.isEmpty()) text else script.content + "\n" + text)
                        }
                    },
                    onClear = { script = script.copy(content = "") }
                )
                1 -> DisplayTab(
                    script = script,
                    onFontSizeChange = { script = script.copy(fontSize = it) },
                    onSpeedChange = { script = script.copy(scrollSpeed = it) },
                    onColorChange = { script = script.copy(textColorPreset = it) },
                    onOpacityChange = { script = script.copy(overlayOpacity = it) }
                )
            }

            Spacer(Modifier.weight(1f))

            StartButton(
                enabled = canStart,
                onClick = {
                    scriptStore.save(script)
                    onStartPrompter(script.id)
                }
            )
        }
    }
}

@Composable
private fun ScriptTab(script: Script, onContentChange: (String) -> Unit, onPaste: () -> Unit, onClear: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TextField(
            value = script.content,
            onValueChange = onContentChange,
            placeholder = { Text("输入正文...") },
            modifier = Modifier.weight(1f).fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly) {
            IconButton(onClick = onPaste, modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.Black.copy(0.06f))) {
                Icon(Icons.Default.ContentPaste, "粘贴")
            }
            IconButton(onClick = onClear, modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.Black.copy(0.06f))) {
                Icon(Icons.Default.Delete, "清空")
            }
        }
    }
}

@Composable
private fun DisplayTab(
    script: Script,
    onFontSizeChange: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onColorChange: (TextColorPreset) -> Unit,
    onOpacityChange: (Float) -> Unit
) {
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        SliderRow("字号", "${script.fontSize.toInt()}", script.fontSize, 12f..110f, onFontSizeChange)
        Spacer(Modifier.height(16.dp))
        SliderRow("速度", "${script.scrollSpeed.toInt()} 字/分", script.scrollSpeed, 20f..260f, onSpeedChange)
        Spacer(Modifier.height(16.dp))
        Text("文字颜色", fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            TextColorPreset.editorChoices.forEach { preset ->
                TextButton(
                    onClick = { onColorChange(preset) },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (script.textColorPreset == preset) Color.Black.copy(0.12f) else Color.Transparent)
                ) { Text(preset.label) }
            }
        }
        Spacer(Modifier.height(16.dp))
        SliderRow("摄像头透明度", "${((1f - script.overlayOpacity) * 100).toInt()}%", 1f - script.overlayOpacity, 0.18f..0.82f) {
            onOpacityChange(1f - it)
        }
    }
}

@Composable
private fun SliderRow(label: String, value: String, current: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
            Text(label)
            Text(value, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
        }
        Slider(value = current, onValueChange = onChange, valueRange = range, colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary))
    }
}

@Composable
private fun StartButton(enabled: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(if (enabled) MaterialTheme.colorScheme.primary else Color.Gray.copy(0.3f))
    ) {
        Icon(Icons.Default.PlayArrow, null)
        Spacer(Modifier.width(8.dp))
        Text("开始提词", fontWeight = FontWeight.SemiBold, color = if (enabled) MaterialTheme.colorScheme.onPrimary else Color.Gray)
    }
}
