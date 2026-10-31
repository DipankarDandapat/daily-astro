package com.dailyastro.app.ui

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import com.dailyastro.app.R
import java.util.Locale

/** Design tokens from "New Daily Astro Design .html" (:root and its dark-mode overrides). */
data class AstroColors(
    val navy: Color = Color(0xFF0E2A8A), val navy2: Color = Color(0xFF16399F), val ink: Color, val mut: Color, val bg: Color,
    val card: Color, val line: Color, val acc: Color = Color(0xFFF5A623), val selBg: Color, val selBorder: Color = Color(0xFF4A78E6),
    val selInk: Color, val button: Color = Color(0xFF0D2F86), val gold: Color = Color(0xFFF6C76B), val dark: Boolean,
)

val LightAstro = AstroColors(ink = Color(0xFF1B2340), mut = Color(0xFF6B7494), bg = Color(0xFFF2F4FB), card = Color.White, line = Color(0xFFE5E8F3),
    selBg = Color(0xFFF1F6FF), selInk = Color(0xFF1D3FAE), dark = false)
val DarkAstro = AstroColors(ink = Color(0xFFE8ECFB), mut = Color(0xFF9AA4C8), bg = Color(0xFF10162E), card = Color(0xFF1A2244), line = Color(0xFF2A345E),
    selBg = Color(0xFF243268), selInk = Color(0xFFDCE4FF), dark = true)

val LocalAstro = staticCompositionLocalOf { LightAstro }

val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal), Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold), Font(R.font.poppins_bold, FontWeight.Bold),
)

@OptIn(ExperimentalTextApi::class)
val Playfair = FontFamily(Font(R.font.playfair_display, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))))

fun Context.localized(language: String): Context =
    createConfigurationContext(Configuration(resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) })

@Composable
fun AstroTheme(theme: String, largeText: Boolean, language: String, content: @Composable () -> Unit) {
    val dark = when (theme) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
    val colors = if (dark) DarkAstro else LightAstro
    val base = LocalContext.current
    val context = remember(base, language) { base.localized(language) }
    val density = LocalDensity.current
    val scheme = if (dark) darkColorScheme(primary = colors.navy2, background = colors.bg, surface = colors.card, onSurface = colors.ink)
                 else lightColorScheme(primary = colors.navy2, background = colors.bg, surface = colors.card, onSurface = colors.ink)
    CompositionLocalProvider(
        LocalAstro provides colors,
        LocalContext provides context,
        LocalConfiguration provides context.resources.configuration,
        LocalDensity provides Density(density.density, density.fontScale * if (largeText) 1.18f else 1f),
    ) {
        MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography.let { t ->
            t.copy(bodyLarge = t.bodyLarge.copy(fontFamily = Poppins), bodyMedium = t.bodyMedium.copy(fontFamily = Poppins),
                bodySmall = t.bodySmall.copy(fontFamily = Poppins), labelLarge = t.labelLarge.copy(fontFamily = Poppins),
                titleMedium = t.titleMedium.copy(fontFamily = Poppins), titleLarge = t.titleLarge.copy(fontFamily = Poppins))
        }, content = content)
    }
}
