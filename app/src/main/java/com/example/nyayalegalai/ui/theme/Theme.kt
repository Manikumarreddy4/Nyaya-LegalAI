package com.example.nyayalegalai.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BluePrimaryDark,
    onPrimary = Color(0xFF1A237E),
    primaryContainer = Color(0xFF283593),
    onPrimaryContainer = Color(0xFFE8EAF6),
    secondary = BlueSecondaryDark,
    onSecondary = Color(0xFF1A237E),
    secondaryContainer = Color(0xFF303F9F),
    onSecondaryContainer = Color(0xFFE8EAF6),
    tertiary = AccentColorDark,
    onTertiary = Color(0xFF1A237E),
    tertiaryContainer = Color(0xFF3D5AFE),
    onTertiaryContainer = Color(0xFFE8EAF6),
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF2A2A3C),
    onSurfaceVariant = TextSecondaryDark,
    outline = DividerColorDark,
    outlineVariant = DividerColorDark,
    error = ColorErrorDark,
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC)
)

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = Color.White,
    primaryContainer = BluePrimaryVariant,
    onPrimaryContainer = Color.White,
    secondary = BlueSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8EAF6),
    onSecondaryContainer = BluePrimary,
    tertiary = AccentColor,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE8EAF6),
    onTertiaryContainer = AccentColor,
    background = WhiteBackground,
    onBackground = TextPrimary,
    surface = GraySurface,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFF0F2F9),
    onSurfaceVariant = TextSecondary,
    outline = DividerColor,
    outlineVariant = DividerColor,
    error = ColorError,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

@Composable
fun NyayaAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // Enabled dynamic colors by default
    themeColorName: String = "Default",
    fontColorName: String = "Default",
    content: @Composable () -> Unit
) {
    // Dynamically calculate theme primary/secondary based on settings selection
    val primaryColor = if (darkTheme) {
        when(themeColorName) {
            "Red" -> Color(0xFFEF9A9A)
            "Green" -> Color(0xFFA5D6A7)
            "Orange" -> Color(0xFFFFCC80)
            "Purple" -> Color(0xFFE1BEE7)
            else -> BluePrimaryDark
        }
    } else {
        when(themeColorName) {
            "Red" -> Color(0xFFC62828)
            "Green" -> Color(0xFF2E7D32)
            "Orange" -> Color(0xFFE65100)
            "Purple" -> Color(0xFF6A1B9A)
            else -> BluePrimary
        }
    }

    val secondaryColor = if (darkTheme) {
        when(themeColorName) {
            "Red" -> Color(0xFFFFCDD2)
            "Green" -> Color(0xFFC8E6C9)
            "Orange" -> Color(0xFFFFE0B2)
            "Purple" -> Color(0xFFF3E5F5)
            else -> BlueSecondaryDark
        }
    } else {
        when(themeColorName) {
            "Red" -> Color(0xFFD32F2F)
            "Green" -> Color(0xFF388E3C)
            "Orange" -> Color(0xFFEF6C00)
            "Purple" -> Color(0xFF7B1FA2)
            else -> BlueSecondary
        }
    }

    // Font color overrides for Text Primary if custom set in settings
    val customFontColor = when(fontColorName) {
        "Dark Blue" -> Color(0xFF0D47A1)
        "Dark Green" -> Color(0xFF1B5E20)
        "Dark Gray" -> Color(0xFF212121)
        else -> null
    }

    // Only apply Android 12+ dynamic wallpaper colors if theme setting is "Default"
    val useDynamicColor = dynamicColor && themeColorName == "Default" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when {
        useDynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme.copy(
            primary = primaryColor,
            secondary = secondaryColor,
            onBackground = customFontColor ?: TextPrimaryDark,
            onSurface = customFontColor ?: TextPrimaryDark
        )
        else -> LightColorScheme.copy(
            primary = primaryColor,
            secondary = secondaryColor,
            onBackground = customFontColor ?: TextPrimary,
            onSurface = customFontColor ?: TextPrimary
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
