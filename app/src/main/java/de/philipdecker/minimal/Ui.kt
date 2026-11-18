package de.philipdecker.minimal

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

val Bg = Color.Black
val Ink = Color.White
val Dim = Color(0xFF8C8C8C)
val Faint = Color(0xFF4A4A4A)

private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN)
private val dateFmt = DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMAN)

sealed interface Screen {
    data object Home : Screen
    data object Apps : Screen
    data object Settings : Screen
    data class Pause(val app: AppEntry) : Screen
}

@Composable
fun T(
    text: String,
    size: TextUnit = 18.sp,
    color: Color = Ink,
    weight: FontWeight = FontWeight.Normal,
    align: TextAlign = TextAlign.Start,
    modifier: Modifier = Modifier,
) = BasicText(text, modifier, style = TextStyle(color = color, fontSize = size, fontWeight = weight, textAlign = align, letterSpacing = 0.2.sp))

/** Antippen ohne Welleneffekt (schlicht) */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.tap(onLong: (() -> Unit)? = null, onClick: () -> Unit): Modifier = composed {
    combinedClickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onLongClick = onLong,
        onClick = onClick,
    )
}

// ───────────────────────── Startbildschirm ─────────────────────────

@Composable
fun HomeScreen(prefs: Prefs, apps: List<AppEntry>, tick: Int, onOpen: (AppEntry) -> Unit, onApps: () -> Unit, onSettings: () -> Unit) {
    val ctx = LocalContext.current
    val now by produceState(LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(60_000L - System.currentTimeMillis() % 60_000L + 50)
        }
    }
    // Akku und Bildschirmzeit bei jedem Zurückkehren und jede Minute neu
    val battery = remember(now, tick) { battery(ctx) }
    val screenTime by produceState<Long?>(null, now, tick, prefs.showScreenTime) {
        value = if (prefs.showScreenTime && Usage.granted(ctx)) kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { Usage.today(ctx).values.sum() } else null
    }
    val byKey = remember(apps) { apps.associateBy { it.key } }
    val favs = prefs.favorites.mapNotNull { byKey[it] }
    var drag by remember { mutableStateOf(0f) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { drag = 0f },
                    onDragEnd = { if (drag < -120f) onApps(); drag = 0f },
                    onDragCancel = { drag = 0f },
                ) { _, dy -> drag += dy }
            }
            .pointerInput(Unit) { detectTapGestures(onLongPress = { onSettings() }) }
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 32.dp, vertical = 24.dp),
    ) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(24.dp))
            T(now.format(timeFmt), size = 76.sp, weight = FontWeight.Light)
            T(now.format(dateFmt), size = 18.sp, color = Dim)
            val info = listOfNotNull(
                if (prefs.showBattery) battery?.let { "Akku $it %" } else null,
                screenTime?.let { "heute ${duration(it)} am Handy" },
            )
            if (info.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                T(info.joinToString("  ·  "), size = 14.sp, color = Dim)
            }

            Spacer(Modifier.weight(1f))
            if (favs.isEmpty()) {
                T("Nach oben wischen für alle Apps.\nLange auf eine App drücken, um sie hier anzuheften.", size = 16.sp, color = Dim)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    favs.forEach { app ->
                        T(
                            app.label, size = 30.sp,
                            color = if (app.pkg in prefs.distracting) Dim else Ink,
                            modifier = Modifier.tap { onOpen(app) },
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            T("Apps", size = 15.sp, color = Faint, modifier = Modifier.align(Alignment.CenterHorizontally).tap { onApps() }.padding(12.dp))
        }
    }
}

// ───────────────────────── Alle Apps ─────────────────────────

@Composable
fun AppsScreen(prefs: Prefs, apps: List<AppEntry>, onOpen: (AppEntry) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var query by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf<AppEntry?>(null) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val q = fold(query.trim())
    val shown = remember(apps, q) {
        if (q.isEmpty()) apps
        else apps.filter { q in it.search }.sortedBy { if (it.search.startsWith(q)) 0 else 1 }
    }
    LaunchedEffect(Unit) { if (prefs.keyboardOnOpen) { focus.requestFocus(); keyboard?.show() } }

    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .windowInsetsPadding(WindowInsets.systemBars)
            .imePadding()
            .padding(horizontal = 32.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        BasicTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            textStyle = TextStyle(color = Ink, fontSize = 24.sp),
            cursorBrush = SolidColor(Ink),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go, capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
            keyboardActions = KeyboardActions(onGo = { shown.firstOrNull()?.let(onOpen) }),
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
            decorationBox = { field ->
                Box {
                    if (query.isEmpty()) T("Suchen …", size = 24.sp, color = Faint)
                    field()
                }
            },
        )
        Spacer(Modifier.height(16.dp))
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(shown, key = { it.key }) { app ->
                T(
                    app.label, size = 22.sp,
                    color = if (app.pkg in prefs.distracting) Dim else Ink,
                    modifier = Modifier.fillMaxWidth().tap(onLong = { menu = app }) { onOpen(app) }.padding(vertical = 10.dp),
                )
            }
            if (shown.isEmpty()) item { T("Keine App gefunden.", size = 16.sp, color = Dim, modifier = Modifier.padding(vertical = 10.dp)) }
        }
        T("Zurück", size = 15.sp, color = Faint, modifier = Modifier.align(Alignment.CenterHorizontally).tap { onBack() }.padding(12.dp))
    }

    menu?.let { app ->
        val fav = app.key in prefs.favorites
        val full = !fav && prefs.favorites.size >= Prefs.MAX_FAVORITES
        Sheet(onDismiss = { menu = null }) {
            T(app.label, size = 22.sp, weight = FontWeight.Medium)
            Spacer(Modifier.height(18.dp))
            Option(if (fav) "Vom Startbildschirm entfernen" else if (full) "Startbildschirm voll (${Prefs.MAX_FAVORITES})" else "Auf den Startbildschirm", enabled = !full) {
                prefs.toggleFavorite(app.key); menu = null
            }
            Option(if (app.pkg in prefs.distracting) "Nicht mehr ablenkend" else "Als ablenkend markieren") {
                prefs.toggleDistracting(app.pkg); menu = null
            }
            Option("App-Info") { Apps.info(ctx, app); menu = null }
            Option("Deinstallieren") { Apps.uninstall(ctx, app); menu = null }
            Option("Abbrechen", color = Dim) { menu = null }
        }
    }
}

@Composable
fun Sheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Bg)
                .border(1.dp, Faint)
                .padding(24.dp),
        ) { content() }
    }
}

@Composable
fun Option(text: String, color: Color = Ink, enabled: Boolean = true, onClick: () -> Unit) {
    T(
        text, size = 18.sp, color = if (enabled) color else Faint,
        modifier = Modifier.fillMaxWidth().tap { if (enabled) onClick() }.padding(vertical = 10.dp),
    )
}

// ───────────────────────── Pause vor ablenkenden Apps ─────────────────────────

@Composable
fun PauseScreen(app: AppEntry, seconds: Int, onCancel: () -> Unit, onOpen: () -> Unit) {
    val ctx = LocalContext.current
    var left by remember(app) { mutableIntStateOf(seconds) }
    LaunchedEffect(app) { while (left > 0) { delay(1000); left-- } }
    val used by produceState<Long?>(null, app) {
        value = if (Usage.granted(ctx)) kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { Usage.today(ctx)[app.pkg] ?: 0L } else null
    }
    // Atemkreis: 4 s ein, 4 s aus
    val breath = rememberInfiniteTransition(label = "atem")
    val scale by breath.animateFloat(
        initialValue = 0.55f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "kreis",
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        T(app.label, size = 28.sp, align = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        T("Brauchst du das gerade wirklich?", size = 17.sp, color = Dim, align = TextAlign.Center)
        used?.let {
            Spacer(Modifier.height(4.dp))
            T("Heute schon ${duration(it)}", size = 15.sp, color = Dim, align = TextAlign.Center)
        }
        Spacer(Modifier.height(48.dp))
        Box(Modifier.size(180.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(180.dp * scale).border(1.dp, Ink, CircleShape))
            T(if (left > 0) "$left" else "", size = 32.sp, weight = FontWeight.Light)
        }
        Spacer(Modifier.height(12.dp))
        T(if (scale > 0.78f) "ausatmen" else "einatmen", size = 14.sp, color = Faint)
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            T("Lieber nicht", size = 18.sp, modifier = Modifier.tap { onCancel() }.padding(12.dp))
            T("Öffnen", size = 18.sp, color = if (left > 0) Faint else Ink, modifier = Modifier.tap { if (left == 0) onOpen() }.padding(12.dp))
        }
    }
}

// ───────────────────────── Einstellungen ─────────────────────────

@Composable
fun SettingsScreen(prefs: Prefs, apps: List<AppEntry>, tick: Int, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val byKey = remember(apps) { apps.associateBy { it.key } }
    val byPkg = remember(apps) { apps.associateBy { it.pkg } }
    val usage = remember(tick) { Usage.granted(ctx) }
    fun open(action: String) = try { ctx.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) { }

    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 24.dp),
    ) {
        T("Einstellungen", size = 30.sp, weight = FontWeight.Light)
        Spacer(Modifier.height(24.dp))
        Option("Als Standard-Startbildschirm festlegen") { open(Settings.ACTION_HOME_SETTINGS) }

        Section("Startbildschirm")
        if (prefs.favorites.isEmpty()) T("Noch keine Apps. In der App-Liste lange auf eine App drücken.", size = 15.sp, color = Dim)
        prefs.favorites.forEachIndexed { i, key ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                T(byKey[key]?.label ?: "(entfernt)", size = 18.sp, modifier = Modifier.weight(1f))
                T("↑", size = 20.sp, color = if (i > 0) Ink else Faint, modifier = Modifier.tap { prefs.moveFavorite(key, -1) }.padding(horizontal = 10.dp))
                T("↓", size = 20.sp, color = if (i < prefs.favorites.lastIndex) Ink else Faint, modifier = Modifier.tap { prefs.moveFavorite(key, 1) }.padding(horizontal = 10.dp))
                T("✕", size = 18.sp, color = Dim, modifier = Modifier.tap { prefs.toggleFavorite(key) }.padding(start = 10.dp))
            }
        }

        Section("Ablenkende Apps")
        Toggle("Pause vor dem Öffnen", "${prefs.pauseSeconds} Sek.") { prefs.cyclePause() }
        if (prefs.distracting.isEmpty()) T("Keine. In der App-Liste lange auf eine App drücken → „Als ablenkend markieren“.", size = 15.sp, color = Dim)
        prefs.distracting.sortedBy { (byPkg[it]?.label ?: it).lowercase() }.forEach { pkg ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                T(byPkg[pkg]?.label ?: pkg, size = 18.sp, modifier = Modifier.weight(1f))
                T("✕", size = 18.sp, color = Dim, modifier = Modifier.tap { prefs.toggleDistracting(pkg) }.padding(start = 10.dp))
            }
        }

        Section("Anzeige")
        Toggle("Akku anzeigen", if (prefs.showBattery) "an" else "aus") { prefs.toggleBattery() }
        Toggle("Bildschirmzeit anzeigen", if (prefs.showScreenTime) "an" else "aus") { prefs.toggleScreenTime() }
        if (!usage) Option("Nutzungszugriff erlauben (für Bildschirmzeit)", color = Dim) { open(Settings.ACTION_USAGE_ACCESS_SETTINGS) }
        Toggle("Tastatur in der App-Liste sofort", if (prefs.keyboardOnOpen) "an" else "aus") { prefs.toggleKeyboard() }

        Spacer(Modifier.height(32.dp))
        T("Minimal ${BuildConfigVersion.name(ctx)}", size = 13.sp, color = Faint)
        Spacer(Modifier.height(16.dp))
        T("Zurück", size = 15.sp, color = Faint, modifier = Modifier.align(Alignment.CenterHorizontally).tap { onBack() }.padding(12.dp))
    }
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(28.dp))
    T(title.uppercase(Locale.GERMAN), size = 12.sp, color = Faint, weight = FontWeight.Medium)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun Toggle(label: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().tap { onClick() }.padding(vertical = 10.dp)) {
        T(label, size = 18.sp, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        T(value, size = 18.sp, color = Dim)
    }
}

object BuildConfigVersion {
    fun name(ctx: android.content.Context): String = try {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: ""
    } catch (_: Exception) { "" }
}
