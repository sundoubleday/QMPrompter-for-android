package com.qiaomu.prompter

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.qiaomu.prompter.data.APIKeyStore
import com.qiaomu.prompter.data.ScriptStore
import com.qiaomu.prompter.service.ScreenRecordService
import com.qiaomu.prompter.ui.theme.QMPrompterTheme
import com.qiaomu.prompter.ui.screens.AIGenerationScreen
import com.qiaomu.prompter.ui.screens.AppSettingsScreen
import com.qiaomu.prompter.ui.screens.ScriptEditorScreen
import com.qiaomu.prompter.ui.screens.ScriptListScreen
import com.qiaomu.prompter.ui.screens.PrompterScreen

class MainActivity : ComponentActivity() {

    private lateinit var screenRecordLauncher: ActivityResultLauncher<Intent>
    private var onScreenRecordResult: ((Int, Intent?) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        screenRecordLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                onScreenRecordResult?.invoke(result.resultCode, result.data)
            }
            onScreenRecordResult = null
        }

        val scriptStore = ScriptStore(this)
        val apiKeyStore = APIKeyStore(this)
        setContent {
            QMPrompterTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation(
                        scriptStore = scriptStore,
                        apiKeyStore = apiKeyStore,
                        requestScreenRecord = { callback ->
                            val manager = getSystemService(android.content.Context.MEDIA_PROJECTION_SERVICE)
                                as android.media.projection.MediaProjectionManager
                            onScreenRecordResult = { code, data ->
                                if (data != null) {
                                    callback(code, data)
                                }
                            }
                            screenRecordLauncher.launch(manager.createScreenCaptureIntent())
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    scriptStore: ScriptStore,
    apiKeyStore: APIKeyStore,
    requestScreenRecord: ((Int, Intent) -> Unit) -> Unit
) {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "scripts") {
        composable("scripts") {
            ScriptListScreen(
                scriptStore = scriptStore,
                apiKeyStore = apiKeyStore,
                onOpenScript = { id -> navController.navigate("editor/$id") },
                onOpenAI = { navController.navigate("ai") },
                onOpenSettings = { navController.navigate("settings") }
            )
        }
        composable(
            "editor/{scriptId}",
            arguments = listOf(navArgument("scriptId") { type = NavType.StringType })
        ) { backStack ->
            val id = backStack.arguments?.getString("scriptId") ?: ""
            ScriptEditorScreen(
                scriptId = id,
                scriptStore = scriptStore,
                onBack = { navController.popBackStack() },
                onStartPrompter = { id -> navController.navigate("prompter/$id") }
            )
        }
        composable(
            "prompter/{scriptId}",
            arguments = listOf(navArgument("scriptId") { type = NavType.StringType })
        ) { backStack ->
            val id = backStack.arguments?.getString("scriptId") ?: ""
            PrompterScreen(
                scriptId = id,
                scriptStore = scriptStore,
                onClose = { navController.popBackStack() },
                requestScreenRecord = requestScreenRecord
            )
        }
        composable("ai") {
            AIGenerationScreen(
                apiKeyStore = apiKeyStore,
                scriptStore = scriptStore,
                onGenerated = { id -> navController.navigate("editor/$id") { popUpTo("scripts") } },
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            AppSettingsScreen(
                apiKeyStore = apiKeyStore,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
