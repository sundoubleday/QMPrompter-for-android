package com.qiaomu.prompter.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1C1C1E),
    onPrimary = Color.White,
    surface = Color(0xFFF2F2F7),
    onSurface = Color(0xFF1C1C1E),
    background = Color.White,
    onBackground = Color(0xFF1C1C1E),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color(0xFF1C1C1E),
    surface = Color(0xFF1C1C1E),
    onSurface = Color.White,
    background = Color(0xFF000000),
    onBackground = Color.White,
)

@Composable
fun QMPrompterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
