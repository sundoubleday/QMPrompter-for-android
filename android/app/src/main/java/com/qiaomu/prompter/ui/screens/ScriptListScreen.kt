package com.qiaomu.prompter.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qiaomu.prompter.data.APIKeyStore
import com.qiaomu.prompter.data.Script
import com.qiaomu.prompter.data.ScriptStore
import com.qiaomu.prompter.ui.GlassCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptListScreen(
    scriptStore: ScriptStore,
    apiKeyStore: APIKeyStore,
    onOpenScript: (String) -> Unit,
    onOpenAI: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val scripts by scriptStore.scripts.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showCreatePanel by remember { mutableStateOf(false) }

    val filtered = remember(scripts, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) scripts else scripts.filter {
            it.title.contains(q, true) || it.content.contains(q, true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("搜索文稿", color = Color.Gray) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, "设置")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreatePanel = !showCreatePanel }) {
                        Icon(Icons.Default.Add, "新建文稿")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (filtered.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(filtered, key = { it.id }) { script ->
                        ScriptCardItem(
                            script = script,
                            onClick = { onOpenScript(script.id) },
                            onDelete = { scriptStore.delete(script) }
                        )
                    }
                }
            }

            AnimatedVisibility(visible = showCreatePanel, modifier = Modifier.align(Alignment.BottomCenter)) {
                CreatePanel(
                    onDismiss = { showCreatePanel = false },
                    onManualCreate = {
                        val draft = scriptStore.createDraft()
                        scriptStore.save(draft)
                        showCreatePanel = false
                        onOpenScript(draft.id)
                    },
                    onAICreate = {
                        showCreatePanel = false
                        onOpenAI()
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("+", fontSize = 48.sp, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
            Text("还没有文稿", color = Color.Gray)
            Text("新建一篇正文后即可开始提词。", color = Color.Gray, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ScriptCardItem(script: Script, onClick: () -> Unit, onDelete: () -> Unit) {
    val dateFormatter = remember { SimpleDateFormat("M月d日 HH:mm", Locale.CHINA) }
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    script.title.ifEmpty { "未命名文稿" },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    script.preview.ifEmpty { "还没有正文" },
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "更新 ${dateFormatter.format(Date(script.updatedAt))}",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    fontSize = 11.sp
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.06f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("${script.content.length} 字", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun CreatePanel(onDismiss: () -> Unit, onManualCreate: () -> Unit, onAICreate: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("新建文稿", fontWeight = FontWeight.SemiBold, fontSize = 17.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Close, "关闭", modifier = Modifier.size(14.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        PanelRow(Icons.Default.Edit, "手动输入", "从空白文稿开始", onManualCreate)
        Spacer(Modifier.height(10.dp))
        PanelRow(Icons.Default.SmartToy, "AI 生成", "输入或语音描述主题，生成口播正文", onAICreate)
    }
}

@Composable
private fun PanelRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .background(Color.White.copy(alpha = 0.44f))
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.42f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(13.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Text(subtitle, color = Color.Gray, fontSize = 13.sp)
        }
    }
}
