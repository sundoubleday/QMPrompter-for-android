package com.qiaomu.prompter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qiaomu.prompter.data.APIKeyStore
import com.qiaomu.prompter.data.Script
import com.qiaomu.prompter.data.ScriptStore
import com.qiaomu.prompter.service.DeepSeekScriptGenerator
import com.qiaomu.prompter.service.PromptDictation
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIGenerationScreen(
    apiKeyStore: APIKeyStore,
    scriptStore: ScriptStore,
    onGenerated: (String) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val dictation = remember { PromptDictation(context) }
    var prompt by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val isRecording by dictation.isRecording.collectAsState()
    val transcript by dictation.transcript.collectAsState()
    val dictationError by dictation.errorMessage.collectAsState()
    val canGenerate = apiKeyStore.hasDeepSeekAPIKey && prompt.isNotBlank() && !isGenerating

    LaunchedEffect(transcript) { if (transcript.isNotEmpty()) prompt = transcript }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("AI 生成", fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                IconButton(onClick = { dictation.stop(); onBack() }) { Icon(Icons.Default.ArrowBack, "取消") }
            },
            actions = {
                TextButton(onClick = {
                    scope.launch {
                        isGenerating = true
                        errorMessage = null
                        try {
                            val content = DeepSeekScriptGenerator(apiKeyStore.deepSeekAPIKey).generateScript(prompt)
                            val title = prompt.take(16).let { if (it.length < prompt.length) "$it..." else it }
                            val script = Script(title = title, content = content)
                            scriptStore.save(script)
                            onGenerated(script.id)
                        } catch (e: Exception) {
                            errorMessage = e.message
                        } finally {
                            isGenerating = false
                        }
                    }
                }, enabled = canGenerate) {
                    if (isGenerating) Text("生成中...") else Text("生成", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                placeholder = { Text("输入或说出你想生成的内容。") },
                modifier = Modifier.fillMaxWidth().height(260.dp),
                enabled = !isGenerating
            )
            if (!apiKeyStore.hasDeepSeekAPIKey) {
                Spacer(Modifier.height(16.dp))
                Text("请先在首页左上角设置 DeepSeek API Key。", color = Color.Gray, fontSize = 14.sp)
            }
            (errorMessage ?: dictationError)?.let { msg ->
                Spacer(Modifier.height(16.dp))
                Text(msg, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            }
        }

        Box(Modifier.fillMaxSize().padding(bottom = 32.dp), Alignment.BottomCenter) {
            IconButton(
                onClick = { dictation.toggle() },
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (isRecording) 0.36f else 0.26f)),
                enabled = !isGenerating
            ) {
                Icon(
                    if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    null,
                    modifier = Modifier.size(30.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
