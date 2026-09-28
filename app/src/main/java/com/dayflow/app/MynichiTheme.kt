package com.dayflow.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource

/** Fixed sakura palette keeps the brand consistent with the launcher icon. */
@Composable
fun MynichiTheme(content: @Composable () -> Unit) {
    val raspberry = colorResource(R.color.mynichi_raspberry)
    val coral = colorResource(R.color.mynichi_coral)
    val pink = colorResource(R.color.mynichi_pale_pink)
    val plum = colorResource(R.color.mynichi_plum)
    val blush = colorResource(R.color.mynichi_blush)
    val surface = colorResource(R.color.mynichi_surface)
    MaterialTheme(colorScheme = lightColorScheme(
        primary = raspberry, onPrimary = pink,
        primaryContainer = blush, onPrimaryContainer = plum,
        secondary = raspberry, onSecondary = pink,
        secondaryContainer = coral, onSecondaryContainer = plum,
        tertiary = plum, onTertiary = pink,
        tertiaryContainer = blush, onTertiaryContainer = plum,
        background = pink, onBackground = plum,
        surface = surface, onSurface = plum,
        surfaceVariant = blush, onSurfaceVariant = colorResource(R.color.mynichi_muted),
        surfaceTint = raspberry,
        surfaceDim = blush, surfaceBright = surface,
        surfaceContainerLowest = surface, surfaceContainerLow = pink,
        surfaceContainer = pink, surfaceContainerHigh = blush,
        surfaceContainerHighest = blush,
        outline = colorResource(R.color.mynichi_outline),
        outlineVariant = colorResource(R.color.mynichi_outline_soft),
        inverseSurface = plum, inverseOnSurface = pink, inversePrimary = coral
    ), content = content)
}
