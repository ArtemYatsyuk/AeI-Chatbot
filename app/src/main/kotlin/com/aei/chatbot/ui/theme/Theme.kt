package com.aei.chatbot.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

data class AeIColors(
    val userBubble: Color,
    val aiBubble: Color,
    val inputBackground: Color,
    val primaryGradientStart: Color,
    val primaryGradientEnd: Color
)

val LocalAeIColors = staticCompositionLocalOf {
    AeIColors(
        userBubble = DarkUserBubble,
        aiBubble = DarkAiBubble,
        inputBackground = DarkInputBackground,
        primaryGradientStart = DarkPrimary,
        primaryGradientEnd = DarkSecondary
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color.White,
    primaryContainer = DarkPrimaryVariant,
    secondary = DarkSecondary,
    onSecondary = Color.Black,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurface,
    error = DarkError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    secondary = LightSecondary,
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurface,
    error = Color(0xFFB00020),
    onError = Color.White
)

@Composable
fun AeITheme(
    themeMode: String = "system",
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    val aeIColors = if (isDark) {
        AeIColors(
            userBubble = DarkUserBubble,
            aiBubble = DarkAiBubble,
            inputBackground = DarkInputBackground,
            primaryGradientStart = DarkPrimary,
            primaryGradientEnd = DarkSecondary
        )
    } else {
        AeIColors(
            userBubble = LightUserBubble,
            aiBubble = LightAiBubble,
            inputBackground = LightInputBackground,
            primaryGradientStart = LightPrimary,
            primaryGradientEnd = LightSecondary
        )
    }

    CompositionLocalProvider(LocalAeIColors provides aeIColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AeITypography,
            shapes = AeIShapes,
            content = content
        )
    }
}
