package com.vfa.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat

/**
 * Text size is one of the guided app's accessibility settings (Standard / Large / XL).
 * Rather than thread a multiplier through every `sp` in the app, we scale the whole
 * composition's font scale — so every piece of text, including anything added later,
 * grows with the setting.
 */
enum class TextScale(val label: String, val factor: Float, val glyphSp: Int) {
    STANDARD("Standard", 1.0f, 15),
    LARGE("Large", 1.15f, 19),
    XL("XL", 1.32f, 24),
}

private val VfaColorScheme = lightColorScheme(
    primary = Coral,
    onPrimary = White,
    secondary = LavMid,
    onSecondary = White,
    tertiary = Green,
    onTertiary = White,
    background = Cream,
    onBackground = Navy,
    surface = White,
    onSurface = Navy,
    surfaceVariant = CreamDeep,
    onSurfaceVariant = Muted,
    outline = Ring,
    error = Amber,
    onError = White,
)

/**
 * [darkChrome] is set while a camera screen is showing: those run full bleed behind a
 * transparent status bar, so the bar's icons have to flip to light. The bar is never
 * given a colour of its own — whatever the screen draws goes all the way up.
 */
@Composable
fun VfaTheme(
    textScale: TextScale = TextScale.STANDARD,
    darkChrome: Boolean = false,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkChrome
        }
    }

    val base = LocalDensity.current
    val scaled = Density(base.density, base.fontScale * textScale.factor)

    CompositionLocalProvider(LocalDensity provides scaled) {
        MaterialTheme(
            colorScheme = VfaColorScheme,
            typography = Typography,
            content = content
        )
    }
}
