package de.philipdecker.minimal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/**
 * Sprache: Englisch ist Standard. Steht das System auf Deutsch, ist die App deutsch.
 * Wird in MainActivity.applyTheme() bei Start und bei jeder Sprachänderung gesetzt.
 */
object Lang {
    var de by mutableStateOf(Locale.getDefault().language == "de")
    val locale: Locale get() = if (de) Locale.GERMAN else Locale.ENGLISH
}

/** Text in Deutsch oder Englisch, je nach Systemsprache */
fun tr(de: String, en: String): String = if (Lang.de) de else en
