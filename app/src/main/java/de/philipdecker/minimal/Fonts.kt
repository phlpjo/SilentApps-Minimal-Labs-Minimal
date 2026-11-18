package de.philipdecker.minimal

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

// ───────────────────────── Schrift ─────────────────────────

val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_300, FontWeight.Light),
    Font(R.font.space_grotesk_400, FontWeight.Normal),
    Font(R.font.space_grotesk_500, FontWeight.Medium),
)
val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_300, FontWeight.Light),
    Font(R.font.jetbrains_mono_400, FontWeight.Normal),
    Font(R.font.jetbrains_mono_500, FontWeight.Medium),
)

fun fontFor(key: String): FontFamily = when (key) {
    "system" -> FontFamily.Default
    "mono" -> JetBrainsMono
    else -> SpaceGrotesk
}
fun fontLabel(key: String) = when (key) { "system" -> "System"; "mono" -> "Mono"; else -> "Space Grotesk" }
fun sizeFactor(i: Int) = when (i) { 0 -> 0.88f; 2 -> 1.16f; else -> 1f }
fun sizeLabel(i: Int) = when (i) { 0 -> "klein"; 2 -> "groß"; else -> "normal" }

val LocalFont = compositionLocalOf { SpaceGrotesk }
val LocalScale = compositionLocalOf { 1f }

