package de.philipdecker.minimal

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

val Bg = Color.Black
val Ink = Color.White
val Dim = Color(0xFF8E8E98)
val Faint = Color(0xFF4C4C56)
val Panel = Color(0xFF0B0B10)
val Line = Color(0x26FFFFFF)

private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN)
private val dateFmt = DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMAN)

sealed interface Screen {
    data object Home : Screen
    data object Apps : Screen
    data object Settings : Screen
    data class Pause(val app: AppEntry) : Screen
}

/** Text in der gewählten Schrift und Größe */
@Composable
fun T(
    text: String,
    size: TextUnit = 18.sp,
    color: Color = Ink,
    weight: FontWeight = FontWeight.Normal,
    align: TextAlign = TextAlign.Start,
    spacing: TextUnit = 0.2.sp,
    modifier: Modifier = Modifier,
) = BasicText(
    text, modifier,
    style = TextStyle(
        color = color, fontSize = size * LocalScale.current, fontWeight = weight, textAlign = align,
        fontFamily = LocalFont.current, letterSpacing = spacing,
    ),
)

/** Antippen ohne Welleneffekt */
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
fun HomeScreen(
    prefs: Prefs, apps: List<AppEntry>, tick: Int, update: Int?,
    onOpen: (AppEntry) -> Unit, onApps: () -> Unit, onSettings: () -> Unit, onSwipe: (SwipeAction) -> Unit,
) {
    val ctx = LocalContext.current
    val now by produceState(LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(60_000L - System.currentTimeMillis() % 60_000L + 50)
        }
    }
    val battery = remember(now, tick) { battery(ctx) }
    val screenTime by produceState<Long?>(null, now, tick, prefs.showScreenTime) {
        value = if (prefs.showScreenTime && Usage.granted(ctx)) withContext(Dispatchers.IO) { Usage.today(ctx).values.sum() } else null
    }
    val byKey = remember(apps) { apps.associateBy { it.key } }
    val favs = prefs.favorites.mapNotNull { byKey[it] }
    var drag by remember { mutableStateOf(Offset.Zero) }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(prefs.swipeDown, prefs.swipeLeft, prefs.swipeRight) {
                detectDragGestures(
                    onDragStart = { drag = Offset.Zero },
                    onDragCancel = { drag = Offset.Zero },
                    onDragEnd = {
                        val (dx, dy) = drag
                        val t = 110.dp.toPx()
                        if (abs(dy) >= abs(dx)) {
                            if (dy < -t) onApps() else if (dy > t) onSwipe(prefs.swipeDown)
                        } else {
                            if (dx < -t) onSwipe(prefs.swipeLeft) else if (dx > t) onSwipe(prefs.swipeRight)
                        }
                        drag = Offset.Zero
                    },
                ) { _, d -> drag += d }
            }
            .pointerInput(Unit) { detectTapGestures(onLongPress = { onSettings() }) }
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 32.dp, vertical = 24.dp),
    ) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(28.dp))
            T(now.format(timeFmt), size = 84.sp, weight = FontWeight.Light, spacing = (-2).sp,
                modifier = Modifier.tap { Shortcuts.alarms(ctx) })
            T(now.format(dateFmt), size = 18.sp, color = Dim, spacing = 0.6.sp,
                modifier = Modifier.tap { Shortcuts.calendar(ctx) })
            val info = listOfNotNull(
                if (prefs.showBattery) battery?.let { "$it %" } else null,
                screenTime?.let { "${duration(it)} heute" },
            )
            if (info.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                T(info.joinToString("   ·   "), size = 13.sp, color = Faint, spacing = 1.2.sp, modifier = Modifier)
            }

            Spacer(Modifier.weight(1f))
            if (favs.isEmpty()) {
                T("Nach oben wischen für alle Apps.\nLange auf eine App drücken, um sie hier anzuheften.", size = 16.sp, color = Dim, modifier = Modifier)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    favs.forEachIndexed { i, app ->
                        T(
                            app.label, size = 32.sp, weight = FontWeight.Light,
                            color = if (app.pkg in prefs.distracting) Dim else Ink,
                            modifier = Modifier.tap { onOpen(app) },
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            if (update != null) {
                T("Neue Version 1.0.$update  ·  laden", size = 14.sp, color = Ink, spacing = 0.8.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally).tap {
                        try { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Updates.DOWNLOAD)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) { }
                    }.padding(8.dp))
            }
            SwipeHint(Modifier.align(Alignment.CenterHorizontally).tap { onApps() })
        }
    }
}

/** Kleiner pulsierender Strich unten: „hier nach oben wischen“ */
@Composable
private fun SwipeHint(modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "hint")
    val a by t.animateFloat(0.25f, 0.7f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "a")
    Box(modifier.padding(14.dp)) {
        Box(Modifier.width(36.dp).height(3.dp).background(Ink.copy(alpha = a), RoundedCornerShape(2.dp)))
    }
}

// ───────────────────────── Alle Apps ─────────────────────────

@Composable
fun AppsScreen(prefs: Prefs, apps: List<AppEntry>, onOpen: (AppEntry) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var query by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf<AppEntry?>(null) }
    var renaming by remember { mutableStateOf<AppEntry?>(null) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val q = fold(query.trim())
    val shown = remember(apps, q) {
        if (q.isEmpty()) apps
        else apps.filter { q in it.search }.sortedBy { if (it.search.startsWith(q)) 0 else 1 }
    }
    LaunchedEffect(Unit) { if (prefs.keyboardOnOpen) { delay(120); focus.requestFocus(); keyboard?.show() } }
    LaunchedEffect(q) { list.scrollToItem(0) }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .imePadding()
            .padding(horizontal = 32.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        BasicTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            textStyle = TextStyle(color = Ink, fontSize = 26.sp * LocalScale.current, fontFamily = LocalFont.current, fontWeight = FontWeight.Light),
            cursorBrush = SolidColor(Ink),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go, capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
            keyboardActions = KeyboardActions(onGo = { shown.firstOrNull()?.let(onOpen) }),
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
            decorationBox = { field ->
                Column {
                    Box {
                        if (query.isEmpty()) T("Suchen", size = 26.sp, color = Faint, weight = FontWeight.Light)
                        field()
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
                }
            },
        )
        Spacer(Modifier.height(8.dp))
        // Liste beginnt in der Bildschirmmitte (erster Eintrag = „A“); Sprung per Buchstabe landet ebenfalls dort
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val half = maxHeight / 2
            val index = remember(shown) { letterIndex(shown) }
            LazyColumn(
                Modifier.fillMaxSize().padding(end = if (q.isEmpty()) 28.dp else 0.dp),
                state = list,
                contentPadding = PaddingValues(top = if (q.isEmpty()) half - 24.dp else 12.dp, bottom = half),
            ) {
                items(shown, key = { it.key }) { app ->
                    T(
                        app.label, size = 24.sp, weight = FontWeight.Light,
                        color = if (app.pkg in prefs.distracting) Dim else Ink,
                        modifier = Modifier.fillMaxWidth().tap(onLong = { menu = app }) { onOpen(app) }.padding(vertical = 11.dp),
                    )
                }
                if (shown.isEmpty()) item { T("Keine App gefunden.", size = 16.sp, color = Dim, modifier = Modifier.padding(vertical = 10.dp)) }
            }
            if (q.isEmpty() && shown.isNotEmpty()) {
                LetterBar(index, Modifier.align(Alignment.CenterEnd)) { i -> scope.launch { list.scrollToItem(i) } }
            }
        }
    }

    menu?.let { app ->
        val fav = app.key in prefs.favorites
        val full = !fav && prefs.favorites.size >= Prefs.MAX_FAVORITES
        Sheet(onDismiss = { menu = null }) {
            T(app.label, size = 24.sp, weight = FontWeight.Light)
            Spacer(Modifier.height(18.dp))
            Option(if (fav) "Vom Startbildschirm entfernen" else if (full) "Startbildschirm voll (${Prefs.MAX_FAVORITES})" else "Auf den Startbildschirm", enabled = !full) {
                prefs.toggleFavorite(app.key); menu = null
            }
            Option("Umbenennen") { renaming = app; menu = null }
            Option("Ausblenden") { prefs.toggleHidden(app.key); menu = null }
            Option(if (app.pkg in prefs.distracting) "Nicht mehr ablenkend" else "Als ablenkend markieren") {
                prefs.toggleDistracting(app.pkg); menu = null
            }
            Option("App-Info") { Apps.info(ctx, app); menu = null }
            Option("Deinstallieren") { Apps.uninstall(ctx, app); menu = null }
            Option("Abbrechen", color = Dim) { menu = null }
        }
    }
    renaming?.let { app -> RenameSheet(prefs, app) { renaming = null } }
}

@Composable
fun RenameSheet(prefs: Prefs, app: AppEntry, onDone: () -> Unit) {
    var name by remember(app) { mutableStateOf(app.label) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(100); focus.requestFocus() }
    Sheet(onDismiss = onDone) {
        T("Umbenennen", size = 13.sp, color = Faint, spacing = 2.sp)
        Spacer(Modifier.height(12.dp))
        BasicTextField(
            value = name, onValueChange = { name = it.take(40) }, singleLine = true,
            textStyle = TextStyle(color = Ink, fontSize = 24.sp * LocalScale.current, fontFamily = LocalFont.current, fontWeight = FontWeight.Light),
            cursorBrush = SolidColor(Ink),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { prefs.rename(app.key, name); onDone() }),
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
        )
        Box(Modifier.fillMaxWidth().padding(top = 8.dp).height(1.dp).background(Line))
        Spacer(Modifier.height(14.dp))
        Option("Speichern") { prefs.rename(app.key, name); onDone() }
        if (app.key in prefs.renames) Option("Originalnamen verwenden", color = Dim) { prefs.rename(app.key, ""); onDone() }
        Option("Abbrechen", color = Dim) { onDone() }
    }
}

@Composable
fun Sheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                
                .background(Panel, RoundedCornerShape(24.dp))
                .border(1.dp, Line, RoundedCornerShape(24.dp))
                .padding(26.dp),
        ) { content() }
    }
}

@Composable
fun Option(text: String, color: Color = Ink, enabled: Boolean = true, onClick: () -> Unit) {
    T(
        text, size = 18.sp, color = if (enabled) color else Faint,
        modifier = Modifier.fillMaxWidth().tap { if (enabled) onClick() }.padding(vertical = 11.dp),
    )
}

/** Anfangsbuchstabe für die Leiste: A–Z (Umlaute zählen zum Grundbuchstaben), alles andere „#“ */
fun letterOf(label: String): Char {
    val c = fold(label.trim()).firstOrNull()?.uppercaseChar() ?: '#'
    return if (c in 'A'..'Z') c else '#'
}

/** Buchstabe → erster Index in der Liste. Fehlende Buchstaben springen zum nächsten vorhandenen. */
fun letterIndex(apps: List<AppEntry>): List<Pair<Char, Int>> {
    val first = HashMap<Char, Int>()
    apps.forEachIndexed { i, a -> first.putIfAbsent(letterOf(a.label), i) }
    val letters = ('A'..'Z').toList() + '#'
    return letters.map { l ->
        val i = first[l] ?: letters.drop(letters.indexOf(l)).firstNotNullOfOrNull { first[it] } ?: apps.lastIndex
        l to i
    }
}

/** Buchstabenleiste rechts: antippen oder darüberziehen; großer Buchstabe zeigt, wo man ist */
@Composable
fun LetterBar(index: List<Pair<Char, Int>>, modifier: Modifier = Modifier, onJump: (Int) -> Unit) {
    val haptic = LocalHapticFeedback.current
    var active by remember { mutableStateOf<Char?>(null) }
    val present = remember(index) { index.zipWithNext { a, b -> if (a.second != b.second) a.first else null }.filterNotNull().toSet() + index.last().first }
    Box(modifier.fillMaxHeight()) {
        active?.let { l ->
            T(l.toString(), size = 56.sp, weight = FontWeight.Light, modifier = Modifier.align(Alignment.Center).padding(end = 72.dp))
        }
        Column(
            Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(0.86f)
                .width(28.dp)
                .pointerInput(index) {
                    fun pick(y: Float) {
                        val i = ((y / size.height) * index.size).toInt().coerceIn(0, index.lastIndex)
                        val (l, at) = index[i]
                        if (l != active) {
                            active = l
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onJump(at)
                        }
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume(); pick(down.position.y)
                        while (true) {
                            val ev = awaitPointerEvent()
                            val c = ev.changes.firstOrNull() ?: break
                            if (!c.pressed) break
                            c.consume(); pick(c.position.y)
                        }
                        active = null
                    }
                },
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            index.forEach { (l, _) ->
                T(l.toString(), size = 11.sp, weight = FontWeight.Medium, color = if (l == active) Ink else if (l in present) Dim else Faint)
            }
        }
    }
}

// ───────────────────────── Pause vor ablenkenden Apps ─────────────────────────

@Composable
fun PauseScreen(app: AppEntry, seconds: Int, onCancel: () -> Unit, onOpen: () -> Unit) {
    val ctx = LocalContext.current
    var left by remember(app) { mutableIntStateOf(seconds) }
    LaunchedEffect(app) { while (left > 0) { delay(1000); left-- } }
    val used by produceState<Long?>(null, app) {
        value = if (Usage.granted(ctx)) withContext(Dispatchers.IO) { Usage.today(ctx)[app.pkg] ?: 0L } else null
    }
    // Atmen: Ring wächst 4 s, schrumpft 4 s
    val t = rememberInfiniteTransition(label = "atem")
    val scale by t.animateFloat(0.55f, 1f, infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "ring")

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        T(app.label, size = 30.sp, weight = FontWeight.Light, align = TextAlign.Center, modifier = Modifier)
        Spacer(Modifier.height(8.dp))
        T("Brauchst du das gerade wirklich?", size = 17.sp, color = Dim, align = TextAlign.Center, modifier = Modifier)
        used?.let {
            Spacer(Modifier.height(4.dp))
            T("Heute schon ${duration(it)}", size = 14.sp, color = Faint, align = TextAlign.Center, spacing = 1.sp)
        }
        Spacer(Modifier.height(48.dp))
        Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(220.dp)) {
                drawCircle(Ink.copy(alpha = 0.85f), size.minDimension / 2f * scale * 0.92f, style = Stroke(1.2.dp.toPx()))
            }
            T(if (left > 0) "$left" else "", size = 34.sp, weight = FontWeight.Light)
        }
        Spacer(Modifier.height(12.dp))
        T(if (scale > 0.78f) "ausatmen" else "einatmen", size = 13.sp, color = Faint, spacing = 3.sp)
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            T("Lieber nicht", size = 18.sp, modifier = Modifier.tap { onCancel() }.padding(12.dp))
            T("Öffnen", size = 18.sp, color = if (left > 0) Faint else Ink, modifier = Modifier.tap { if (left == 0) onOpen() }.padding(12.dp))
        }
    }
}

// ───────────────────────── Einstellungen ─────────────────────────

@Composable
fun SettingsScreen(
    prefs: Prefs, all: List<AppEntry>, tick: Int, update: Int?, checking: Boolean,
    onCheckUpdate: () -> Unit, onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val byKey = remember(all) { all.associateBy { it.key } }
    val byPkg = remember(all) { all.associateBy { it.pkg } }
    val usage = remember(tick) { Usage.granted(ctx) }
    var picking by remember { mutableStateOf<String?>(null) }
    val scroll = rememberScrollState()
    fun open(action: String) = try { ctx.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) { }
    fun swipeLabel(a: SwipeAction) = when {
        a == "notif" -> "Benachrichtigungen"
        a.startsWith("app:") -> byKey[a.removePrefix("app:")]?.label ?: "App fehlt"
        else -> "nichts"
    }
    val current = remember { Updates.currentBuild(ctx) }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(scroll)
            .padding(horizontal = 32.dp, vertical = 24.dp),
    ) {
        T("Einstellungen", size = 32.sp, weight = FontWeight.Light, modifier = Modifier)
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

        Section("Gesten")
        T("Nach oben wischen öffnet alle Apps. Uhrzeit antippen öffnet den Wecker, Datum den Kalender.", size = 14.sp, color = Faint)
        Spacer(Modifier.height(6.dp))
        Toggle("Nach unten wischen", swipeLabel(prefs.swipeDown)) { picking = "down" }
        Toggle("Nach links wischen", swipeLabel(prefs.swipeLeft)) { picking = "left" }
        Toggle("Nach rechts wischen", swipeLabel(prefs.swipeRight)) { picking = "right" }

        Section("Ablenkende Apps")
        Toggle("Pause vor dem Öffnen", "${prefs.pauseSeconds} Sek.") { prefs.cyclePause() }
        if (prefs.distracting.isEmpty()) T("Keine. In der App-Liste lange auf eine App drücken → „Als ablenkend markieren“.", size = 15.sp, color = Dim)
        prefs.distracting.sortedBy { (byPkg[it]?.label ?: it).lowercase() }.forEach { pkg ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                T(byPkg[pkg]?.label ?: pkg, size = 18.sp, modifier = Modifier.weight(1f))
                T("✕", size = 18.sp, color = Dim, modifier = Modifier.tap { prefs.toggleDistracting(pkg) }.padding(start = 10.dp))
            }
        }

        Section("Ausgeblendete Apps")
        if (prefs.hidden.isEmpty()) T("Keine. In der App-Liste lange auf eine App drücken → „Ausblenden“.", size = 15.sp, color = Dim)
        prefs.hidden.sortedBy { (byKey[it]?.label ?: it).lowercase() }.forEach { key ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                T(byKey[key]?.label ?: key.substringBefore('/'), size = 18.sp, modifier = Modifier.weight(1f))
                T("einblenden", size = 15.sp, color = Dim, modifier = Modifier.tap { prefs.toggleHidden(key) }.padding(start = 10.dp))
            }
        }

        Section("Aussehen")
        Toggle("Schrift", fontLabel(prefs.font)) { prefs.cycleFont() }
        Toggle("Schriftgröße", sizeLabel(prefs.fontSize)) { prefs.cycleFontSize() }
        Toggle("Akku anzeigen", if (prefs.showBattery) "an" else "aus") { prefs.toggleBattery() }
        Toggle("Bildschirmzeit anzeigen", if (prefs.showScreenTime) "an" else "aus") { prefs.toggleScreenTime() }
        if (!usage) Option("Nutzungszugriff erlauben (für Bildschirmzeit)", color = Dim) { open(Settings.ACTION_USAGE_ACCESS_SETTINGS) }
        Toggle("Tastatur in der App-Liste sofort", if (prefs.keyboardOnOpen) "an" else "aus") { prefs.toggleKeyboard() }

        Section("Version")
        Toggle(
            "Minimal 1.0.$current",
            when { checking -> "suche …"; update != null -> "1.0.$update laden"; else -> "aktuell · prüfen" },
        ) {
            if (update != null) {
                try { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Updates.DOWNLOAD)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) { }
            } else onCheckUpdate()
        }

        Spacer(Modifier.height(24.dp))
        T("Zurück", size = 15.sp, color = Faint, modifier = Modifier.align(Alignment.CenterHorizontally).tap { onBack() }.padding(12.dp))
    }

    picking?.let { dir ->
        AppPicker(all, onDismiss = { picking = null }) { action -> prefs.setSwipe(dir, action); picking = null }
    }
}

/** Auswahl für Wischgesten: nichts, Benachrichtigungen oder eine App */
@Composable
private fun AppPicker(apps: List<AppEntry>, onDismiss: () -> Unit, onPick: (SwipeAction) -> Unit) {
    Sheet(onDismiss) {
        Option("Nichts", color = Dim) { onPick("") }
        Option("Benachrichtigungen") { onPick("notif") }
        Box(Modifier.fillMaxWidth().padding(vertical = 8.dp).height(1.dp).background(Line))
        val list = rememberLazyListState()
        LazyColumn(Modifier.heightIn(max = 360.dp), state = list) {
            items(apps, key = { it.key }) { app ->
                T(app.label, size = 18.sp, modifier = Modifier.fillMaxWidth().tap { onPick("app:${app.key}") }.padding(vertical = 10.dp))
            }
        }
    }
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(30.dp))
    T(title.uppercase(Locale.GERMAN), size = 11.sp, color = Faint, weight = FontWeight.Medium, spacing = 2.5.sp)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun Toggle(label: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().tap { onClick() }.padding(vertical = 11.dp)) {
        T(label, size = 18.sp, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        T(value, size = 16.sp, color = Dim)
    }
}
