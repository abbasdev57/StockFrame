package com.abbas57.stockframe.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat


/**
 * Light scheme maps directly onto the Structured Light tokens.
 *
 * Mapping note: MaterialTheme's `error` / `errorContainer` slots are what
 * Material 3 components (like TextField's error state) read automatically.
 * Mapping our own Red500/Red50 onto those slots means built-in components
 * inherit the right alert color for free, instead of manually overriding
 * color on every single error Text() composable.
 */
private val LightColors = lightColorScheme(
    primary = Blue400,
    onPrimary = Neutral0,
    primaryContainer = Blue50,
    onPrimaryContainer = Blue800,

    secondary = Green500,
    onSecondary = Neutral0,
    secondaryContainer = Green50,
    onSecondaryContainer = Green800,

    error = Red500,
    onError = Neutral0,
    errorContainer = Red50,
    onErrorContainer = Red800,

    background = Neutral0,
    onBackground = Neutral900,

    surface = Neutral0,
    onSurface = Neutral900,
    surfaceVariant = Neutral50,
    onSurfaceVariant = Neutral700,

    outline = Neutral300
)

/**
 * V1 ships light-mode only. A dark scheme is intentionally NOT defined
 * yet — per the same scope discipline applied everywhere else in this
 * project, dark mode is a real, valid Phase 2 task, not a V1 requirement.
 * dynamicColor and darkTheme params below are left in place so adding
 * a DarkColors scheme later is a one-line change, not a structural one.
 */
@Composable
fun StockFrameTheme(
    darkTheme: Boolean = false, // deliberately NOT isSystemInDarkTheme() yet — see note above
    content: @Composable () -> Unit
) {
    val colorScheme = LightColors


    MaterialTheme(
        colorScheme = colorScheme,
        typography = StockframeTypography,
        content = content
    )
}
/*
* private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun StockFrameTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
* */