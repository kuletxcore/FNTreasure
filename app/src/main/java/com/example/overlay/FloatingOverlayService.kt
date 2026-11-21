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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

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

        val total = intent?.getStringExtra(EXTRA_TOTAL) ?: "0"
        val last = intent?.getStringExtra(EXTRA_LAST) ?: "—"
        val session = intent?.getStringExtra(EXTRA_SESSION) ?: "0"
        val treasures = intent?.getStringExtra(EXTRA_TREASURES) ?: "0"
        val squad = intent?.getStringExtra(EXTRA_SQUAD)
        val style = intent?.getStringExtra(EXTRA_STYLE) ?: "hud"
        val sizePct = (intent?.getIntExtra(EXTRA_SIZE, 100) ?: 100).coerceIn(70, 175)
        val opacityPct = (intent?.getIntExtra(EXTRA_OPACITY, 100) ?: 100).coerceIn(30, 100)
        val live = intent?.getBooleanExtra(EXTRA_LIVE, false) ?: false

        showOrUpdateOverlay(
            total = total,
            last = last,
            session = session,
            treasures = treasures,
            squad = squad,
            style = style,
            sizePct = sizePct,
            opacityPct = opacityPct,
            live = live,
        )
        isRunning = true
        return START_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showOrUpdateOverlay(
        total: String,
        last: String,
        session: String,
        treasures: String,
        squad: String?,
        style: String,
        sizePct: Int,
        opacityPct: Int,
        live: Boolean,
    ) {
        val wm = windowManager ?: (getSystemService(Context.WINDOW_SERVICE) as WindowManager).also {
            windowManager = it
        }
        removeOverlay()

        val scale = (sizePct / 100f).coerceIn(0.7f, 1.5f)
        val isClassic = style == "classic"
        val density = resources.displayMetrics.density

        fun dp(v: Float): Int = (v * density * scale).toInt()

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            alpha = (opacityPct / 100f).coerceIn(0.3f, 1f)
            setPadding(dp(12f), dp(8f), dp(12f), dp(8f))
            background = GradientDrawable().apply {
                cornerRadius = dp(if (isClassic) 14f else 10f).toFloat()
                if (isClassic) {
                    setColor(Color.argb(230, 18, 18, 22))
                    setStroke(dp(1f), Color.rgb(58, 58, 66))
                } else {
                    setColor(Color.argb(145, 11, 15, 24))
                    setStroke(dp(1f), Color.argb(140, 201, 162, 79))
                }
            }
        }

        // Header row: Total + Close button
        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val totalView = TextView(this).apply {
            text = (if (live) "● " else "") + total
            setTextColor(if (live) Color.rgb(110, 224, 150) else Color.rgb(255, 211, 106))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f * scale)
            typeface = Typeface.DEFAULT_BOLD
            setShadowLayer(4f, 1f, 2f, Color.BLACK)
        }
        topRow.addView(totalView)

        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(14f), 1)
        }
        topRow.addView(spacer)

        val closeBtn = TextView(this).apply {
            text = "×"
            setTextColor(Color.rgb(239, 226, 196))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f * scale)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(6f), 0, dp(2f), 0)
            setOnClickListener {
                removeOverlay()
                isRunning = false
                stopSelf()
            }
        }
        topRow.addView(closeBtn)
        container.addView(topRow)

        fun addMetaLine(text: String, color: Int) {
            val tv = TextView(this).apply {
                this.text = text
                setTextColor(color)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f * scale)
                typeface = Typeface.DEFAULT_BOLD
                setShadowLayer(3f, 1f, 1f, Color.BLACK)
            }
            container.addView(tv)
        }

        addMetaLine("Last • $last   Session • $session", Color.WHITE)
        if (!squad.isNullOrBlank()) {
            addMetaLine("Squad • $squad", Color.rgb(220, 220, 230))
        }
        addMetaLine("★ $treasures", Color.rgb(214, 190, 255))

        val lp = layoutParams ?: WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = (16 * density).toInt()
            y = (80 * density).toInt()
        }.also { layoutParams = it }

        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f
        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = lp.x
                    initialY = lp.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    // Since gravity is TOP|END, dragging right decreases x
                    lp.x = initialX - (event.rawX - touchX).toInt()
                    lp.y = initialY + (event.rawY - touchY).toInt()
                    wm.updateViewLayout(container, lp)
                    true
                }
                else -> false
            }
        }

        overlayView = container
        wm.addView(container, lp)
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

        const val EXTRA_TOTAL = "extra_total"
        const val EXTRA_LAST = "extra_last"
        const val EXTRA_SESSION = "extra_session"
        const val EXTRA_TREASURES = "extra_treasures"
        const val EXTRA_SQUAD = "extra_squad"
        const val EXTRA_STYLE = "extra_style"
        const val EXTRA_SIZE = "extra_size"
        const val EXTRA_OPACITY = "extra_opacity"
        const val EXTRA_LIVE = "extra_live"

        fun startOrUpdate(
            context: Context,
            total: String,
            last: String,
            session: String,
            treasures: String,
            squad: String?,
            style: String,
            sizePct: Int,
            opacityPct: Int,
            live: Boolean,
        ) {
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_START_OR_UPDATE
                putExtra(EXTRA_TOTAL, total)
                putExtra(EXTRA_LAST, last)
                putExtra(EXTRA_SESSION, session)
                putExtra(EXTRA_TREASURES, treasures)
                putExtra(EXTRA_SQUAD, squad)
                putExtra(EXTRA_STYLE, style)
                putExtra(EXTRA_SIZE, sizePct)
                putExtra(EXTRA_OPACITY, opacityPct)
                putExtra(EXTRA_LIVE, live)
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingOverlayService::class.java))
        }
    }
}
