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
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.grid.items as gridItems
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
import androidx.compose.runtime.setValue
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.ui.graphics.StrokeCap
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Farben folgen dem System (hell/dunkel). Kontraste nach WCAG 2.2 gegen den Hintergrund:
 * Ink 21:1 · Dim ≥ 12:1 · Faint ≥ 7:1 (AAA) – auch der leiseste Text bleibt gut lesbar.
 */
object Palette { var dark by mutableStateOf(true) }
val Bg: Color get() = if (Palette.dark) Color.Black else Color.White
val Ink: Color get() = if (Palette.dark) Color.White else Color.Black
val Dim: Color get() = if (Palette.dark) Color(0xFFD2D2D8) else Color(0xFF38383F)
val Faint: Color get() = if (Palette.dark) Color(0xFFA8A8B1) else Color(0xFF595962)
val Panel: Color get() = if (Palette.dark) Color(0xFF101015) else Color(0xFFF3F3F6)
val Line: Color get() = if (Palette.dark) Color(0x47FFFFFF) else Color(0x47000000)

private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN)
private val dateFmt = DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMAN)

sealed interface Screen {
    data object Home : Screen
    data object Apps : Screen
    data object Settings : Screen
    data class Pause(val app: AppEntry) : Screen
    data class Folder(val id: String) : Screen
}

/** Eintrag auf dem Startbildschirm: App oder Ordner */
sealed interface HomeItem {
    data class App(val app: AppEntry) : HomeItem
    data class Dir(val folder: de.philipdecker.minimal.Folder) : HomeItem
}

/** Text in der gewählten Schrift und Größe; glow = leichtes Leuchten (Uhr, Überschriften) */
@Composable
fun T(
    text: String,
    size: TextUnit = 18.sp,
    color: Color = Ink,
    weight: FontWeight = FontWeight.Normal,
    align: TextAlign = TextAlign.Start,
    glow: Boolean = false,
    spacing: TextUnit = 0.2.sp,
    modifier: Modifier = Modifier,
) = BasicText(
    text, modifier,
    style = TextStyle(
        color = color, fontSize = size * LocalScale.current, fontWeight = weight, textAlign = align,
        fontFamily = LocalFont.current, letterSpacing = spacing,
        shadow = if (glow) Shadow(color.copy(alpha = if (Palette.dark) 0.45f else 0.18f), Offset.Zero, 28f) else null,
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

/** Sanftes Einschweben (gestaffelt nach Index) */
fun Modifier.floatIn(index: Int): Modifier = composed {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(60L + index * 45L); a.animateTo(1f, tween(520, easing = FastOutSlowInEasing)) }
    graphicsLayer { alpha = a.value; translationY = (1f - a.value) * 28.dp.toPx(); scaleX = 0.96f + 0.04f * a.value; scaleY = scaleX }
}

/** Scrollen → Warp der Sterne */
@Composable
fun rememberWarpScroll(): NestedScrollConnection {
    val warp = LocalWarp.current
    return remember(warp) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                warp.floatValue = (warp.floatValue - consumed.y * 0.5f).coerceIn(-90f, 90f)
                return Offset.Zero
            }
        }
    }
}

/** 3D-Liste: Einträge kippen am Rand nach hinten weg und verblassen (wie eine Walze im All) */
fun Modifier.depth(state: LazyListState, key: Any): Modifier = graphicsLayer {
    val info = state.layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.key == key } ?: return@graphicsLayer
    val half = (info.viewportEndOffset - info.viewportStartOffset) / 2f
    if (half <= 0f) return@graphicsLayer
    val center = info.viewportStartOffset + half
    val d = ((item.offset + item.size / 2f - center) / half).coerceIn(-1f, 1f)
    // auch ganz oben/unten bleibt der Eintrag gut lesbar (min. 65 % Deckkraft ≈ 7:1 Kontrast)
    alpha = (1f - 0.35f * d * d).coerceIn(0.65f, 1f)
    val s = 1f - 0.06f * abs(d)
    scaleX = s; scaleY = s
    rotationX = -d * 18f
    cameraDistance = 10f * density
    transformOrigin = TransformOrigin(0f, 0.5f)
    translationX = abs(d) * d * d * 18.dp.toPx()
}

// ───────────────────────── Startbildschirm ─────────────────────────

@Composable
fun HomeScreen(
    prefs: Prefs, apps: List<AppEntry>, tick: Int,
    onOpen: (AppEntry) -> Unit, onApps: () -> Unit, onSettings: () -> Unit, onSwipe: (SwipeAction) -> Unit,
    onFolder: (String) -> Unit,
) {
    val ctx = LocalContext.current
    val warp = LocalWarp.current
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
    val favs = prefs.favorites.mapNotNull { k ->
        if (k.startsWith("folder:")) prefs.folder(k.removePrefix("folder:"))?.let { HomeItem.Dir(it) }
        else byKey[k]?.let { HomeItem.App(it) }
    }
    var drag by remember { mutableStateOf(Offset.Zero) }
    var editFolder by remember { mutableStateOf<de.philipdecker.minimal.Folder?>(null) }
    var lockSetup by remember { mutableStateOf(false) }
    var renameFolder by remember { mutableStateOf<de.philipdecker.minimal.Folder?>(null) }
    var folderIcon by remember { mutableStateOf<String?>(null) }

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
                ) { _, d ->
                    drag += d
                    warp.floatValue = (warp.floatValue - d.y * 0.6f).coerceIn(-90f, 90f)
                }
            }
            .pointerInput(prefs.doubleTapLock) {
                detectTapGestures(
                    onLongPress = { onSettings() },
                    onDoubleTap = if (prefs.doubleTapLock) { _ -> if (!LockService.lock()) lockSetup = true } else null,
                )
            }
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 32.dp, vertical = 24.dp),
    ) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(28.dp))
            T(now.format(timeFmt), size = 84.sp, weight = FontWeight.Light, glow = true, spacing = (-2).sp,
                modifier = Modifier.floatIn(0).tap { Shortcuts.alarms(ctx) })
            T(now.format(dateFmt), size = 18.sp, color = Dim, spacing = 0.6.sp,
                modifier = Modifier.floatIn(1).tap { Shortcuts.calendar(ctx) })
            val info = listOfNotNull(
                if (prefs.showBattery) battery?.let { "$it %" } else null,
                screenTime?.let { "${duration(it)} heute" },
            )
            if (info.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                T(info.joinToString("   ·   "), size = 13.sp, color = Faint, spacing = 1.2.sp, modifier = Modifier.floatIn(2))
            }

            Spacer(Modifier.weight(1f))
            if (favs.isEmpty()) {
                T("Nach oben wischen für alle Apps.\nLange auf eine App drücken, um sie hier anzuheften.", size = 16.sp, color = Dim, modifier = Modifier.floatIn(3))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    favs.forEachIndexed { i, item ->
                        when (item) {
                            is HomeItem.App -> Labeled(
                                item.app.label, prefs.iconFor(item.app.key), size = 32.sp, weight = FontWeight.Light,
                                color = if (item.app.pkg in prefs.distracting) Dim else Ink,
                                modifier = Modifier.floatIn(3 + i).tap { onOpen(item.app) },
                            )
                            // Ordner: nur der Name als Text
                            is HomeItem.Dir -> Labeled(
                                item.folder.name, prefs.iconFor("folder:${item.folder.id}"), size = 32.sp, weight = FontWeight.Light,
                                modifier = Modifier.floatIn(3 + i).tap(onLong = { editFolder = item.folder }) { onFolder(item.folder.id) },
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            SwipeHint(Modifier.align(Alignment.CenterHorizontally).tap { onApps() })
        }
    }

    editFolder?.let { f ->
        Sheet(onDismiss = { editFolder = null }) {
            T(f.name, size = 24.sp, weight = FontWeight.Light, glow = true)
            T("Ordner · ${f.apps.size} Apps", size = 13.sp, color = Faint, spacing = 1.sp)
            Spacer(Modifier.height(14.dp))
            Option("Öffnen") { editFolder = null; onFolder(f.id) }
            Option("Umbenennen") { renameFolder = f; editFolder = null }
            Option(if ("folder:${f.id}" in prefs.icons) "Symbol ändern" else "Symbol wählen") { folderIcon = f.id; editFolder = null }
            Option("Ordner auflösen") { prefs.deleteFolder(f.id); editFolder = null }
            Option("Abbrechen", color = Dim) { editFolder = null }
        }
    }
    folderIcon?.let { id -> IconPicker(prefs, "folder:$id") { folderIcon = null } }
    if (lockSetup) {
        Sheet(onDismiss = { lockSetup = false }) {
            T("Doppeltipp sperrt", size = 24.sp, weight = FontWeight.Light, glow = true)
            Spacer(Modifier.height(10.dp))
            T(
                if (LockService.supported) "Dafür braucht Minimal einmal die Freigabe als Bedienungshilfe. Sie wird nur zum Sperren genutzt, es werden keine Inhalte gelesen.\n\nEinstellungen → Bedienungshilfen → „Minimal – Doppeltipp sperrt“ → an."
                else "Das Sperren per Doppeltipp braucht Android 9 oder neuer.",
                size = 15.sp, color = Dim,
            )
            Spacer(Modifier.height(14.dp))
            if (LockService.supported) Option("Bedienungshilfen öffnen") {
                lockSetup = false
                try { ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) { }
            }
            Option("Doppeltipp ausschalten", color = Dim) { prefs.toggleDoubleTapLock(); lockSetup = false }
            Option("Abbrechen", color = Dim) { lockSetup = false }
        }
    }
    renameFolder?.let { f ->
        NameSheet("Ordner umbenennen", f.name, onCancel = { renameFolder = null }) { prefs.renameFolder(f.id, it); renameFolder = null }
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
    var folderPick by remember { mutableStateOf<AppEntry?>(null) }
    var iconFor by remember { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val list = rememberLazyListState()
    val warpScroll = rememberWarpScroll()
    val scope = rememberCoroutineScope()
    // Am Anfang der Liste weiter nach unten ziehen → Liste schließt (zurück zum Startbildschirm)
    val haptic = LocalHapticFeedback.current
    val closeAt = with(LocalDensity.current) { 120.dp.toPx() }
    var pull by remember { mutableFloatStateOf(0f) }
    val pullClose = remember(warpScroll, closeAt) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Zurückschieben, solange schon gezogen wurde
                if (pull > 0f && available.y < 0f) {
                    val c = maxOf(available.y, -pull); pull += c
                    return Offset(0f, c)
                }
                return Offset.Zero
            }
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                warpScroll.onPostScroll(consumed, available, source)
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    val before = pull
                    pull += available.y * 0.55f
                    if (before < closeAt && pull >= closeAt) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }
            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pull <= 0f) return Velocity.Zero
                if (pull >= closeAt) onBack()
                else animate(pull, 0f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { v, _ -> pull = v }
                return available
            }
        }
    }
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) {
                            if (query.isEmpty()) T("Suchen", size = 26.sp, color = Faint, weight = FontWeight.Light)
                            field()
                        }
                        // ✕: Suche leeren (Tastatur bleibt offen)
                        if (query.isNotEmpty()) {
                            Box(
                                Modifier.size(40.dp).tap { query = ""; focus.requestFocus() },
                                contentAlignment = Alignment.Center,
                            ) {
                                Canvas(Modifier.size(16.dp)) {
                                    val w = 1.6.dp.toPx()
                                    drawLine(Dim, Offset.Zero, Offset(size.width, size.height), w, StrokeCap.Round)
                                    drawLine(Dim, Offset(size.width, 0f), Offset(0f, size.height), w, StrokeCap.Round)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    // Leuchtende Linie, die zur Seite ausläuft
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(listOf(Ink.copy(alpha = 0.7f), Ink.copy(alpha = 0.08f), Color.Transparent))))
                }
            },
        )
        Spacer(Modifier.height(8.dp))
        // Liste beginnt in der Bildschirmmitte (erster Eintrag = „A“); Sprung per Buchstabe landet ebenfalls dort
        BoxWithConstraints(
            Modifier.weight(1f).fillMaxWidth().graphicsLayer {
                translationY = pull * 0.7f
                alpha = 1f - (pull / closeAt).coerceIn(0f, 1f) * 0.55f
            },
        ) {
            val half = maxHeight / 2
            val index = remember(shown) { letterIndex(shown) }
            LazyColumn(
                Modifier.fillMaxSize().padding(end = if (q.isEmpty()) 28.dp else 0.dp).nestedScroll(pullClose),
                state = list,
                contentPadding = PaddingValues(top = if (q.isEmpty()) half - 24.dp else 12.dp, bottom = half),
            ) {
                items(shown, key = { it.key }) { app ->
                    Labeled(
                        app.label, prefs.iconFor(app.key), size = 24.sp, weight = FontWeight.Light,
                        color = if (app.key in prefs.hidden) Faint else if (app.pkg in prefs.distracting) Dim else Ink,
                        modifier = Modifier.fillMaxWidth().depth(list, app.key).tap(onLong = { menu = app }) { onOpen(app) }.padding(vertical = 11.dp),
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
            T(app.label, size = 24.sp, weight = FontWeight.Light, glow = true)
            Spacer(Modifier.height(18.dp))
            Option(if (fav) "Vom Startbildschirm entfernen" else if (full) "Startbildschirm voll (${Prefs.MAX_FAVORITES})" else "Auf den Startbildschirm", enabled = !full) {
                prefs.toggleFavorite(app.key); menu = null
            }
            Option("Umbenennen") { renaming = app; menu = null }
            Option(if (app.key in prefs.icons) "Symbol ändern" else "Symbol wählen") { iconFor = app.key; menu = null }
            Option("In Ordner legen") { folderPick = app; menu = null }
            Option(if (app.key in prefs.hidden) "Einblenden" else "Ausblenden") { prefs.toggleHidden(app.key); menu = null }
            Option(if (app.pkg in prefs.distracting) "Nicht mehr ablenkend" else "Als ablenkend markieren") {
                prefs.toggleDistracting(app.pkg); menu = null
            }
            Option("App-Info") { Apps.info(ctx, app); menu = null }
            Option("Deinstallieren") { Apps.uninstall(ctx, app); menu = null }
            Option("Abbrechen", color = Dim) { menu = null }
        }
    }
    renaming?.let { app -> RenameSheet(prefs, app) { renaming = null } }
    folderPick?.let { app -> FolderPickSheet(prefs, app) { folderPick = null } }
    iconFor?.let { key -> IconPicker(prefs, key) { iconFor = null } }
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

/** Symbol auswählen: Raster aller weißen Linien-Symbole */
@Composable
fun IconPicker(prefs: Prefs, key: String, onDone: () -> Unit) {
    val current = prefs.icons[key]
    Sheet(onDismiss = onDone) {
        T("Symbol wählen", size = 13.sp, color = Faint, spacing = 2.sp)
        Spacer(Modifier.height(14.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(52.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
        ) {
            gridItems(Glyphs.names, key = { it }) { name ->
                Box(
                    Modifier
                        .size(52.dp)
                        .then(if (name == current) Modifier.border(1.dp, Ink.copy(alpha = 0.6f), RoundedCornerShape(14.dp)) else Modifier)
                        .tap { prefs.setIcon(key, name); onDone() },
                    contentAlignment = Alignment.Center,
                ) { Glyph(name, 24.dp, if (name == current) Ink else Ink.copy(alpha = 0.82f)) }
            }
        }
        Spacer(Modifier.height(10.dp))
        if (current != null) Option("Kein Symbol", color = Dim) { prefs.setIcon(key, null); onDone() }
        Option("Abbrechen", color = Dim) { onDone() }
    }
}

/** App in Ordner legen / herausnehmen; neuer Ordner direkt hier */
@Composable
fun FolderPickSheet(prefs: Prefs, app: AppEntry, onDone: () -> Unit) {
    var naming by remember { mutableStateOf(false) }
    if (naming) {
        NameSheet("Neuer Ordner", "", onCancel = onDone) { name -> prefs.createFolder(name, app.key); onDone() }
        return
    }
    Sheet(onDismiss = onDone) {
        T("In Ordner legen", size = 13.sp, color = Faint, spacing = 2.sp)
        Spacer(Modifier.height(4.dp))
        T(app.label, size = 22.sp, weight = FontWeight.Light)
        Spacer(Modifier.height(12.dp))
        prefs.folders.forEach { f ->
            val inside = app.key in f.apps
            Row(Modifier.fillMaxWidth().tap { prefs.toggleInFolder(f.id, app.key) }.padding(vertical = 11.dp)) {
                T(f.name, size = 18.sp, modifier = Modifier.weight(1f))
                T(if (inside) "✓" else "", size = 18.sp)
            }
        }
        Option("Neuer Ordner …") { naming = true }
        Option("Fertig", color = Dim) { onDone() }
    }
}

/** Name eingeben (Ordner anlegen/umbenennen) */
@Composable
fun NameSheet(title: String, initial: String, onCancel: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(100); focus.requestFocus() }
    Sheet(onDismiss = onCancel) {
        T(title, size = 13.sp, color = Faint, spacing = 2.sp)
        Spacer(Modifier.height(12.dp))
        BasicTextField(
            value = name, onValueChange = { name = it.take(30) }, singleLine = true,
            textStyle = TextStyle(color = Ink, fontSize = 24.sp * LocalScale.current, fontFamily = LocalFont.current, fontWeight = FontWeight.Light),
            cursorBrush = SolidColor(Ink),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, capitalization = KeyboardCapitalization.Sentences),
            keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onSave(name) }),
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
            decorationBox = { field -> Box { if (name.isEmpty()) T("z. B. Nachrichten", size = 24.sp, color = Faint, weight = FontWeight.Light); field() } },
        )
        Box(Modifier.fillMaxWidth().padding(top = 8.dp).height(1.dp).background(Line))
        Spacer(Modifier.height(14.dp))
        Option("Speichern", enabled = name.isNotBlank()) { onSave(name) }
        Option("Abbrechen", color = Dim) { onCancel() }
    }
}

// ───────────────────────── Ordner ─────────────────────────

@Composable
fun FolderScreen(prefs: Prefs, id: String, all: List<AppEntry>, onOpen: (AppEntry) -> Unit, onBack: () -> Unit) {
    val folder = prefs.folder(id)
    val byKey = remember(all) { all.associateBy { it.key } }
    var remove by remember { mutableStateOf<AppEntry?>(null) }
    LaunchedEffect(folder == null) { if (folder == null) onBack() }
    if (folder == null) return
    val apps = folder.apps.mapNotNull { byKey[it] }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 32.dp, vertical = 24.dp),
    ) {
        Spacer(Modifier.weight(1f))
        T(folder.name.uppercase(Locale.GERMAN), size = 12.sp, color = Faint, weight = FontWeight.Medium, spacing = 3.sp, modifier = Modifier.floatIn(0))
        Spacer(Modifier.height(18.dp))
        if (apps.isEmpty()) {
            T("Noch leer. In der App-Liste lange auf eine App drücken → „In Ordner legen“.", size = 16.sp, color = Dim, modifier = Modifier.floatIn(1))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                apps.forEachIndexed { i, app ->
                    Labeled(
                        app.label, prefs.iconFor(app.key), size = 32.sp, weight = FontWeight.Light,
                        color = if (app.pkg in prefs.distracting) Dim else Ink,
                        modifier = Modifier.floatIn(1 + i).tap(onLong = { remove = app }) { onOpen(app) },
                    )
                }
            }
        }
        Spacer(Modifier.weight(1.4f))
        T("Zurück", size = 15.sp, color = Faint, modifier = Modifier.align(Alignment.CenterHorizontally).tap { onBack() }.padding(12.dp))
    }

    remove?.let { app ->
        Sheet(onDismiss = { remove = null }) {
            T(app.label, size = 24.sp, weight = FontWeight.Light, glow = true)
            Spacer(Modifier.height(14.dp))
            val pos = folder.apps.indexOf(app.key)
            Option("Nach oben", enabled = pos > 0) { prefs.moveInFolder(folder.id, app.key, -1); remove = null }
            Option("Nach unten", enabled = pos in 0 until folder.apps.lastIndex) { prefs.moveInFolder(folder.id, app.key, 1); remove = null }
            Option("Aus „${folder.name}“ entfernen") { prefs.toggleInFolder(folder.id, app.key); remove = null }
            Option("Abbrechen", color = Dim) { remove = null }
        }
    }
}

@Composable
fun Sheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .floatIn(0)
                .background(Panel, RoundedCornerShape(24.dp))
                .border(1.dp, Brush.verticalGradient(listOf(Ink.copy(alpha = 0.28f), Ink.copy(alpha = 0.04f))), RoundedCornerShape(24.dp))
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
    // Atmen: Ring wächst 4 s, schrumpft 4 s; ein Mond umkreist ihn
    val t = rememberInfiniteTransition(label = "atem")
    val scale by t.animateFloat(0.55f, 1f, infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "ring")
    val orbit by t.animateFloat(0f, 1f, infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "mond")

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        T(app.label, size = 30.sp, weight = FontWeight.Light, align = TextAlign.Center, glow = true, modifier = Modifier.floatIn(0))
        Spacer(Modifier.height(8.dp))
        T("Brauchst du das gerade wirklich?", size = 17.sp, color = Dim, align = TextAlign.Center, modifier = Modifier.floatIn(1))
        used?.let {
            Spacer(Modifier.height(4.dp))
            T("Heute schon ${duration(it)}", size = 14.sp, color = Faint, align = TextAlign.Center, spacing = 1.sp)
        }
        Spacer(Modifier.height(48.dp))
        Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(220.dp)) {
                val r = size.minDimension / 2f * scale * 0.92f
                // weicher Schein
                drawCircle(Brush.radialGradient(listOf(Ink.copy(alpha = 0.10f), Color.Transparent), center, r * 1.35f), r * 1.35f)
                drawCircle(Ink.copy(alpha = 0.85f), r, style = Stroke(1.2.dp.toPx()))
                val ang = orbit * 2f * PI.toFloat()
                drawCircle(Ink, 3.5.dp.toPx(), Offset(center.x + cos(ang) * r, center.y + sin(ang) * r))
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
    prefs: Prefs, all: List<AppEntry>, tick: Int,
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val byKey = remember(all) { all.associateBy { it.key } }
    val byPkg = remember(all) { all.associateBy { it.pkg } }
    val usage = remember(tick) { Usage.granted(ctx) }
    var picking by remember { mutableStateOf<String?>(null) }
    var newFolder by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    val warp = LocalWarp.current
    LaunchedEffect(scroll) {
        var last = scroll.value
        androidx.compose.runtime.snapshotFlow { scroll.value }.collect { v -> warp.floatValue = (warp.floatValue - (v - last) * 0.5f).coerceIn(-90f, 90f); last = v }
    }
    fun open(action: String) = try { ctx.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) { }
    fun swipeLabel(a: SwipeAction) = when {
        a == "notif" -> "Benachrichtigungen"
        a.startsWith("app:") -> byKey[a.removePrefix("app:")]?.label ?: "App fehlt"
        else -> "nichts"
    }
    val current = remember { currentBuild(ctx) }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(scroll)
            .padding(horizontal = 32.dp, vertical = 24.dp),
    ) {
        T("Einstellungen", size = 32.sp, weight = FontWeight.Light, glow = true, modifier = Modifier.floatIn(0))
        Spacer(Modifier.height(24.dp))
        Option("Als Standard-Startbildschirm festlegen") { open(Settings.ACTION_HOME_SETTINGS) }

        Section("Startbildschirm")
        if (prefs.favorites.isEmpty()) T("Noch keine Apps. In der App-Liste lange auf eine App drücken.", size = 15.sp, color = Dim)
        prefs.favorites.forEachIndexed { i, key ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                val folder = if (key.startsWith("folder:")) prefs.folder(key.removePrefix("folder:")) else null
                T(folder?.let { "${it.name}  ·  Ordner" } ?: byKey[key]?.label ?: "(entfernt)", size = 18.sp, modifier = Modifier.weight(1f))
                T("↑", size = 20.sp, color = if (i > 0) Ink else Faint, modifier = Modifier.tap { prefs.moveFavorite(key, -1) }.padding(horizontal = 10.dp))
                T("↓", size = 20.sp, color = if (i < prefs.favorites.lastIndex) Ink else Faint, modifier = Modifier.tap { prefs.moveFavorite(key, 1) }.padding(horizontal = 10.dp))
                T("✕", size = 18.sp, color = Dim, modifier = Modifier.tap {
                    if (key.startsWith("folder:")) prefs.deleteFolder(key.removePrefix("folder:")) else prefs.toggleFavorite(key)
                }.padding(start = 10.dp))
            }
        }
        Option(if (prefs.favorites.size >= Prefs.MAX_FAVORITES) "Neuer Ordner (Startbildschirm voll)" else "+ Neuer Ordner", color = Dim,
            enabled = prefs.favorites.size < Prefs.MAX_FAVORITES) { newFolder = true }

        Section("Gesten")
        T("Nach oben wischen öffnet alle Apps. Uhrzeit antippen öffnet den Wecker, Datum den Kalender.", size = 14.sp, color = Faint)
        Spacer(Modifier.height(6.dp))
        Toggle("Doppeltipp sperrt den Bildschirm", when { !prefs.doubleTapLock -> "aus"; LockService.enabled(ctx) -> "an"; else -> "an · Freigabe fehlt" }) { prefs.toggleDoubleTapLock() }
        if (prefs.doubleTapLock && LockService.supported && !LockService.enabled(ctx)) Option("Freigabe in den Bedienungshilfen erteilen", color = Dim) { open(Settings.ACTION_ACCESSIBILITY_SETTINGS) }
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
        Toggle("In der App-Liste anzeigen", if (prefs.showHidden) "an (abgedunkelt)" else "aus") { prefs.toggleShowHidden() }
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
        Toggle("Sternenhimmel", when (prefs.stars) { "still" -> "ruhig"; "off" -> "aus"; else -> "bewegt" }) { prefs.cycleStars() }
        Toggle("Sterne beim Scrollen", warpLabel(prefs.warpLevel)) { prefs.cycleWarp() }
        Toggle("Symbole vor Namen", if (prefs.showIcons) "an" else "aus") { prefs.toggleShowIcons() }
        Toggle("Akku anzeigen", if (prefs.showBattery) "an" else "aus") { prefs.toggleBattery() }
        Toggle("Bildschirmzeit anzeigen", if (prefs.showScreenTime) "an" else "aus") { prefs.toggleScreenTime() }
        if (!usage) Option("Nutzungszugriff erlauben (für Bildschirmzeit)", color = Dim) { open(Settings.ACTION_USAGE_ACCESS_SETTINGS) }
        Toggle("Tastatur in der App-Liste sofort", if (prefs.keyboardOnOpen) "an" else "aus") { prefs.toggleKeyboard() }

        Section("Version")
        Toggle("Minimal", "1.0.$current") { }

        Spacer(Modifier.height(24.dp))
        T("Zurück", size = 15.sp, color = Faint, modifier = Modifier.align(Alignment.CenterHorizontally).tap { onBack() }.padding(12.dp))
    }

    if (newFolder) NameSheet("Neuer Ordner", "", onCancel = { newFolder = false }) { prefs.createFolder(it); newFolder = false }
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
                T(app.label, size = 18.sp, modifier = Modifier.fillMaxWidth().depth(list, app.key).tap { onPick("app:${app.key}") }.padding(vertical = 10.dp))
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
