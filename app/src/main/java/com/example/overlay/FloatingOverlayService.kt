package com.example.overlay

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.data.local.AppContainer
import com.example.data.local.ChestCounterRepository
import com.example.domain.AppConfig
import com.example.domain.L
import com.example.domain.LiveMatchState
import com.example.domain.MatchRecordDto
import com.example.domain.OverlayModel
import com.example.domain.OverlayView
import com.example.domain.Profiles
import com.example.ui.components.TreasureChoices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Instant

object OverlayPermissionHelper {
    fun canDrawOverlays(context: Context): Boolean =
        Settings.canDrawOverlays(context)

    fun createManageOverlayIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

class FloatingOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var collectJob: Job? = null

    private lateinit var repo: ChestCounterRepository

    private var currentConfig: AppConfig = AppConfig()
    private var currentLive: LiveMatchState = LiveMatchState()
    private var currentMatches: List<MatchRecordDto> = emptyList()
    private var currentProfileKey: String = ""
    private var currentPlayerName: String? = null
    private var currentView: OverlayView = OverlayView(
        total = "0",
        current = null,
        currentCaption = "",
        currentLive = false,
        badge = null,
        rows = emptyList(),
        treasures = "0",
        session = "0",
        last = null,
        squad = null,
    )

    private var treasurePickerOpen: Boolean = false
    private var actionsMenuOpen: Boolean = false
    private var compactMode: Boolean = false

    override fun onCreate() {
        super.onCreate()
        repo = AppContainer.repository(applicationContext)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!OverlayPermissionHelper.canDrawOverlays(this)) {
            isRunning = false
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent?.action == ACTION_STOP) {
            removeOverlay()
            isRunning = false
            stopSelf()
            return START_NOT_STICKY
        }

        isRunning = true
        ensureObserving()
        renderOverlay()
        return START_STICKY
    }

    private fun ensureObserving() {
        if (collectJob?.isActive == true) return
        collectJob = serviceScope.launch {
            repo.settings.ensureSessionInitialized()
            combine(
                repo.settings.configFlow,
                repo.settings.liveMatchFlow,
                repo.matchesFlow,
                repo.manualEntriesFlow,
                repo.obtainedPrizesFlow,
            ) { config, live, matches, manualMap, obtainedMap ->
                val lang = config.language
                val profile = Profiles.current(matches, config.selectedProfile, lang)
                val trackerView = Profiles.buildView(matches, manualMap, obtainedMap, config.selectedProfile, lang)
                val overlay = OverlayModel.buildLive(
                    view = trackerView,
                    liveMatch = live,
                    inProgressHint = live.active,
                    sessionNumber = config.sessionNumber,
                    sessionStartedEpochMs = config.sessionStartedEpochMs,
                    showSquad = config.showSquadTotal,
                    lang = lang,
                )
                Snapshot(config, live, matches, profile?.key ?: "", profile?.name, overlay)
            }.collect { snap ->
                currentConfig = snap.config
                currentLive = snap.live
                currentMatches = snap.matches
                currentProfileKey = snap.profileKey
                currentPlayerName = snap.playerName
                currentView = snap.overlay
                if (isRunning) {
                    renderOverlay()
                }
            }
        }
    }

    private data class Snapshot(
        val config: AppConfig,
        val live: LiveMatchState,
        val matches: List<MatchRecordDto>,
        val profileKey: String,
        val playerName: String?,
        val overlay: OverlayView,
    )

    @SuppressLint("ClickableViewAccessibility")
    private fun renderOverlay() {
        val wm = windowManager ?: (getSystemService(Context.WINDOW_SERVICE) as WindowManager).also {
            windowManager = it
        }
        removeOverlay()

        val cfg = currentConfig
        val live = currentLive
        val ov = currentView
        val lang = cfg.language

        val scale = (cfg.overlaySize / 100f).coerceIn(0.7f, 1.5f)
        val isClassic = cfg.overlayStyle == "classic"
        val density = resources.displayMetrics.density
        fun dp(v: Float): Int = (v * density * scale).toInt().coerceAtLeast(1)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            alpha = (cfg.overlayOpacity / 100f).coerceIn(0.3f, 1f)
            setPadding(dp(12f), dp(8f), dp(12f), dp(8f))
            background = GradientDrawable().apply {
                cornerRadius = dp(if (isClassic) 16f else 18f).toFloat()
                if (isClassic) {
                    setColor(Color.argb(232, 18, 20, 28))
                    setStroke(dp(1.2f), Color.rgb(74, 78, 92))
                } else {
                    setColor(Color.argb(188, 12, 16, 26))
                    setStroke(dp(1.2f), Color.argb(175, 201, 162, 79))
                }
            }
        }

        // Main horizontal HUD pill row
        val mainRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // Left stats column
        val statsCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, dp(10f), 0)
        }

        val heroLine = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val heroCountText = if (live.active) {
            "${ov.liveChests} ●"
        } else {
            ov.last ?: "0"
        }
        val countTv = TextView(this).apply {
            text = "🧰 $heroCountText"
            setTextColor(if (live.active) Color.rgb(255, 211, 106) else Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f * scale)
            typeface = Typeface.DEFAULT_BOLD
            setShadowLayer(4f, 1f, 2f, Color.BLACK)
        }
        heroLine.addView(countTv)
        statsCol.addView(heroLine)

        if (!compactMode) {
            val matchSessionLabel = if (live.active) {
                "${L.t(lang, "Partie", "Match", "Partida")} ${ov.matchNumber} · ${L.t(lang, "Session", "Session", "Sesión")} ${ov.sessionNumber}"
            } else if (ov.lastMatchNumber != null) {
                "${L.t(lang, "Dernière P", "Last M", "Última P")}${ov.lastMatchNumber} · ${L.t(lang, "Session", "Session", "Sesión")} ${ov.sessionNumber}"
            } else {
                "${L.t(lang, "Prêt : Partie", "Ready: Match", "Listo: Partida")} ${ov.matchNumber} · ${L.t(lang, "Session", "Session", "Sesión")} ${ov.sessionNumber}"
            }
            val matchSessionTv = TextView(this).apply {
                text = matchSessionLabel
                setTextColor(Color.rgb(245, 208, 120))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f * scale)
                typeface = Typeface.DEFAULT_BOLD
                setShadowLayer(3f, 1f, 1f, Color.BLACK)
            }
            statsCol.addView(matchSessionTv)

            val subStats = buildString {
                if (live.active) {
                    append("${L.t(lang, "Session", "Session", "Sesión")}: ${ov.session}")
                } else {
                    append("${L.t(lang, "Session", "Session", "Sesión")}: ${ov.session} · ${L.t(lang, "Total", "Total", "Total")}: ${ov.total}")
                }
                append(" · ★ ${ov.treasures}")
            }
            val subTv = TextView(this).apply {
                text = subStats
                setTextColor(Color.rgb(225, 226, 236))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f * scale)
                typeface = Typeface.DEFAULT_BOLD
                setShadowLayer(3f, 1f, 1f, Color.BLACK)
            }
            statsCol.addView(subTv)

            if (live.treasures.isNotEmpty()) {
                val chosenLine = live.treasures.joinToString(", ") { it.displayName(lang) }
                val pickedTv = TextView(this).apply {
                    text = "★ $chosenLine"
                    setTextColor(Color.rgb(214, 190, 255))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f * scale)
                    maxLines = 1
                    setShadowLayer(3f, 1f, 1f, Color.BLACK)
                }
                statsCol.addView(pickedTv)
            }
        }

        mainRow.addView(statsCol)

        fun createCircleButton(
            label: String,
            bgColor: Int,
            strokeColor: Int,
            textColor: Int,
            sizeDp: Float = 40f,
            textSp: Float = 14f,
            contentDesc: String,
            onClick: () -> Unit,
        ): TextView {
            val btnSize = dp(sizeDp)
            return TextView(this).apply {
                text = label
                contentDescription = contentDesc
                setTextColor(textColor)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, textSp * scale)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(bgColor)
                    setStroke(dp(1.2f), strokeColor)
                }
                layoutParams = LinearLayout.LayoutParams(btnSize, btnSize).apply {
                    marginStart = dp(6f)
                }
                setOnClickListener { onClick() }
            }
        }

        // +1 Chest button (warm gold)
        mainRow.addView(
            createCircleButton(
                label = "+1",
                bgColor = Color.rgb(245, 200, 76),
                strokeColor = Color.rgb(255, 236, 170),
                textColor = Color.rgb(42, 24, 10),
                sizeDp = 44f,
                textSp = 15f,
                contentDesc = L.t(lang, "+1 coffre", "+1 chest", "+1 cofre"),
            ) {
                serviceScope.launch {
                    repo.incrementLiveChest(1, lang)
                }
            }
        )

        if (!compactMode) {
            // ★ Treasure Picker toggle button (purple)
            mainRow.addView(
                createCircleButton(
                    label = "★",
                    bgColor = if (treasurePickerOpen) Color.rgb(156, 112, 240) else Color.rgb(122, 79, 194),
                    strokeColor = Color.rgb(214, 190, 255),
                    textColor = Color.WHITE,
                    sizeDp = 40f,
                    textSp = 16f,
                    contentDesc = L.t(lang, "Choisir un trésor", "Pick treasure", "Elegir tesoro"),
                ) {
                    treasurePickerOpen = !treasurePickerOpen
                    actionsMenuOpen = false
                    renderOverlay()
                }
            )

            // ■ Finish Match button (coral/orange, visible while active)
            if (live.active || live.chests > 0 || live.treasures.isNotEmpty()) {
                mainRow.addView(
                    createCircleButton(
                        label = "■",
                        bgColor = Color.rgb(235, 134, 42),
                        strokeColor = Color.rgb(255, 205, 140),
                        textColor = Color.WHITE,
                        sizeDp = 40f,
                        textSp = 15f,
                        contentDesc = L.t(lang, "Terminer la partie", "Finish match", "Terminar partida"),
                    ) {
                        serviceScope.launch {
                            treasurePickerOpen = false
                            actionsMenuOpen = false
                            repo.finishLiveMatch(currentProfileKey, currentPlayerName, lang)
                        }
                    }
                )
            }
        }

        // 👁 Compact toggle button
        mainRow.addView(
            createCircleButton(
                label = if (compactMode) "◉" else "👁",
                bgColor = Color.argb(190, 28, 34, 48),
                strokeColor = Color.rgb(201, 162, 79),
                textColor = Color.rgb(239, 226, 196),
                sizeDp = 38f,
                textSp = 13f,
                contentDesc = L.t(lang, "Mode compact", "Toggle compact", "Modo compacto"),
            ) {
                compactMode = !compactMode
                if (compactMode) {
                    treasurePickerOpen = false
                    actionsMenuOpen = false
                }
                renderOverlay()
            }
        )

        if (!compactMode) {
            // ↻ Undo / Session actions toggle
            mainRow.addView(
                createCircleButton(
                    label = "↻",
                    bgColor = if (actionsMenuOpen) Color.rgb(52, 62, 86) else Color.argb(190, 28, 34, 48),
                    strokeColor = Color.rgb(201, 162, 79),
                    textColor = Color.rgb(239, 226, 196),
                    sizeDp = 38f,
                    textSp = 15f,
                    contentDesc = L.t(lang, "Options partie / session", "Match & session options", "Opciones de partida / sesión"),
                ) {
                    actionsMenuOpen = !actionsMenuOpen
                    treasurePickerOpen = false
                    renderOverlay()
                }
            )

            // × Close overlay button
            val closeTv = TextView(this).apply {
                text = "✕"
                contentDescription = L.t(lang, "Fermer l'overlay", "Close overlay", "Cerrar overlay")
                setTextColor(Color.rgb(210, 210, 220))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f * scale)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setPadding(dp(8f), dp(4f), dp(2f), dp(4f))
                setOnClickListener {
                    removeOverlay()
                    isRunning = false
                    stopSelf()
                }
            }
            mainRow.addView(closeTv)
        }

        root.addView(mainRow)

        // Expanded Treasure Picker drawer
        if (treasurePickerOpen && !compactMode) {
            val pickerHeader = TextView(this).apply {
                text = L.t(
                    lang,
                    "Choisir un trésor (Partie ${ov.matchNumber}) :",
                    "Pick treasure for Match ${ov.matchNumber}:",
                    "Elegir tesoro (Partida ${ov.matchNumber}):",
                )
                setTextColor(Color.rgb(214, 190, 255))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f * scale)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, dp(6f), 0, dp(4f))
            }
            root.addView(pickerHeader)

            val scroll = HorizontalScrollView(this).apply {
                isHorizontalScrollBarEnabled = false
            }
            val chipRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val options = TreasureChoices.quickOverlayOptions(Instant.now(), lang)
            options.forEach { opt ->
                val pill = TextView(this).apply {
                    text = if (opt.fromGoldenChest) "★★ ${opt.title}" else "★ ${opt.title}"
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f * scale)
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(dp(10f), dp(6f), dp(10f), dp(6f))
                    background = GradientDrawable().apply {
                        cornerRadius = dp(14f).toFloat()
                        setColor(if (opt.fromGoldenChest) Color.rgb(112, 74, 24) else Color.rgb(86, 52, 146))
                        setStroke(dp(1f), if (opt.fromGoldenChest) Color.rgb(245, 200, 76) else Color.rgb(200, 170, 255))
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply { marginEnd = dp(6f) }
                    setOnClickListener {
                        serviceScope.launch {
                            repo.addLiveTreasure(
                                itemName = opt.itemName,
                                prizeId = opt.prizeId,
                                fromGoldenChest = opt.fromGoldenChest,
                                profileKey = currentProfileKey,
                                lang = lang,
                            )
                            treasurePickerOpen = false
                            renderOverlay()
                        }
                    }
                }
                chipRow.addView(pill)
            }
            scroll.addView(chipRow)
            root.addView(scroll)

            if (live.treasures.isNotEmpty()) {
                val removeScroll = HorizontalScrollView(this).apply {
                    isHorizontalScrollBarEnabled = false
                    setPadding(0, dp(4f), 0, 0)
                }
                val removeRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                }
                live.treasures.forEachIndexed { idx, find ->
                    val pill = TextView(this).apply {
                        text = "${find.displayName(lang)} ✕"
                        setTextColor(Color.rgb(255, 205, 205))
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f * scale)
                        setPadding(dp(8f), dp(4f), dp(8f), dp(4f))
                        background = GradientDrawable().apply {
                            cornerRadius = dp(12f).toFloat()
                            setColor(Color.rgb(92, 32, 36))
                            setStroke(dp(1f), Color.rgb(220, 100, 100))
                        }
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                        ).apply { marginEnd = dp(6f) }
                        setOnClickListener {
                            serviceScope.launch { repo.removeLiveTreasure(idx) }
                        }
                    }
                    removeRow.addView(pill)
                }
                removeScroll.addView(removeRow)
                root.addView(removeScroll)
            }
        }

        // Expanded Match/Session Undo & Reset Drawer
        if (actionsMenuOpen && !compactMode) {
            val actionRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(6f), 0, 0)
            }

            fun addActionPill(text: String, bg: Int, stroke: Int, onClick: () -> Unit) {
                val tv = TextView(this).apply {
                    this.text = text
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f * scale)
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(dp(10f), dp(6f), dp(10f), dp(6f))
                    background = GradientDrawable().apply {
                        cornerRadius = dp(12f).toFloat()
                        setColor(bg)
                        setStroke(dp(1f), stroke)
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply { marginEnd = dp(6f) }
                    setOnClickListener { onClick() }
                }
                actionRow.addView(tv)
            }

            addActionPill(
                text = L.t(lang, "-1 coffre", "-1 chest", "-1 cofre"),
                bg = Color.rgb(44, 52, 72),
                stroke = Color.rgb(201, 162, 79),
            ) {
                serviceScope.launch {
                    repo.incrementLiveChest(-1, lang)
                    actionsMenuOpen = false
                }
            }
            addActionPill(
                text = L.t(lang, "Effacer partie", "Reset match", "Reiniciar partida"),
                bg = Color.rgb(120, 42, 42),
                stroke = Color.rgb(230, 120, 120),
            ) {
                serviceScope.launch {
                    repo.resetLiveMatch(lang)
                    actionsMenuOpen = false
                }
            }
            addActionPill(
                text = L.t(lang, "Nouvelle session", "New session", "Nueva sesión"),
                bg = Color.rgb(31, 127, 122),
                stroke = Color.rgb(130, 230, 220),
            ) {
                serviceScope.launch {
                    repo.startNewSession(lang)
                    actionsMenuOpen = false
                }
            }

            root.addView(actionRow)
        }

        val lp = layoutParams ?: WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = (16 * density).toInt()
            y = (72 * density).toInt()
        }.also { layoutParams = it }

        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f
        root.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = lp.x
                    initialY = lp.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = initialX - (event.rawX - touchX).toInt()
                    lp.y = initialY + (event.rawY - touchY).toInt()
                    try {
                        wm.updateViewLayout(root, lp)
                    } catch (_: Exception) {
                    }
                    true
                }
                else -> false
            }
        }

        overlayView = root
        wm.addView(root, lp)
    }

    private fun removeOverlay() {
        val v = overlayView
        if (v != null) {
            try {
                windowManager?.removeView(v)
            } catch (_: Exception) {
            }
            overlayView = null
        }
    }

    override fun onDestroy() {
        collectJob?.cancel()
        serviceScope.cancel()
        removeOverlay()
        isRunning = false
        super.onDestroy()
    }

    companion object {
        @Volatile
        var isRunning: Boolean = false
            private set

        const val ACTION_START_OR_UPDATE = "com.example.overlay.START_OR_UPDATE"
        const val ACTION_STOP = "com.example.overlay.STOP"

        fun startOrUpdate(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_START_OR_UPDATE
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingOverlayService::class.java))
        }
    }
}
