package app.minsapp

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.annotation.TargetApi
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.Toast
import minsapp.core.Ban
import minsapp.core.Bounds
import minsapp.core.Planner
import minsapp.core.Screen
import minsapp.core.WhatsApp

/**
 * Watches the official WhatsApp app and keeps banned features out of reach:
 * covers their buttons, steps off banned tabs and backs out of banned screens.
 * It never touches messages and has no network access.
 */
class GuardService : AccessibilityService(), SharedPreferences.OnSharedPreferenceChangeListener {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var prefs: BanPrefs
    private lateinit var overlays: Overlays

    private var bans: Set<Ban> = emptySet()
    private var screen = Screen.OTHER
    private var refreshPending = false
    private var lastBackAt = 0L
    private var lastSwitchAt = 0L
    private var lastNoticeAt = 0L
    private var sampling = false
    private var colorSampledAt: Long? = null

    private val refreshTask = Runnable {
        refreshPending = false
        refresh()
    }

    override fun onServiceConnected() {
        prefs = BanPrefs(this)
        bans = prefs.bans()
        prefs.listen(this)
        overlays = Overlays(this).apply { color = themeColor() }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!::overlays.isInitialized) return
        val fromWhatsApp = event.packageName?.toString() in WhatsApp.packages
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                if (fromWhatsApp) {
                    val className = event.className?.toString().orEmpty()
                    val banned = Planner.bannedWindow(className, bans)
                    if (banned != null) {
                        overlays.clear()
                        leave(banned)
                        return
                    }
                    Planner.screenOf(className)?.let { screen = it }
                }
                scheduleRefresh()
            }
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> scheduleRefresh()
            else -> if (fromWhatsApp) scheduleRefresh()
        }
    }

    override fun onInterrupt() = Unit

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        bans = prefs.bans()
        scheduleRefresh()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!::overlays.isInitialized) return
        // Light/dark switch or rotation: redraw, re-reading WhatsApp's background colour.
        colorSampledAt = null
        overlays.color = themeColor()
        overlays.clear()
        scheduleRefresh()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        if (::overlays.isInitialized) {
            overlays.clear()
            prefs.stopListening(this)
        }
        super.onDestroy()
    }

    /** Coalesces the flood of UI events into at most one refresh every [REFRESH_DELAY_MS]. */
    private fun scheduleRefresh() {
        if (refreshPending) return
        refreshPending = true
        handler.postDelayed(refreshTask, REFRESH_DELAY_MS)
    }

    private fun refresh() {
        val root = rootInActiveWindow
        if (root == null || root.packageName?.toString() !in WhatsApp.packages) {
            overlays.clear()
            return
        }
        val plan = Planner.plan(AndroidNode(root), screen, bans)
        plan.leave?.let {
            overlays.clear()
            leave(it)
            return
        }
        plan.switchTo?.let { switchTab(it as AndroidNode) }
        val above = windowsAbove(root.windowId)
        cover(plan.covers.filter { area -> above.none { !area.intersect(it).isEmpty } })
    }

    /**
     * Windows drawn over WhatsApp (notification shade, keyboard, floating video, another app…).
     * Patches would show on top of them, so areas they overlap are left uncovered.
     * The thin status and navigation bars do not count.
     */
    private fun windowsAbove(whatsAppWindowId: Int): List<Bounds> {
        val all = windows
        val whatsApp = all.firstOrNull { it.id == whatsAppWindowId } ?: return emptyList()
        val screenHeight = resources.displayMetrics.heightPixels
        val rect = Rect()
        return all.filter { it.layer > whatsApp.layer && it.type != AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY }
            .map { window ->
                window.getBoundsInScreen(rect)
                window.type to Bounds(rect.left, rect.top, rect.right, rect.bottom)
            }
            .filterNot { (type, area) -> type == AccessibilityWindowInfo.TYPE_SYSTEM && area.height < screenHeight * SYSTEM_BAR_MAX_SHARE }
            .map { it.second }
    }

    private fun cover(areas: List<Bounds>) {
        when {
            areas.isEmpty() -> overlays.clear()
            sampling -> Unit // the refresh scheduled when sampling ends will draw them
            overlays.isEmpty && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && colorIsStale() -> sampleColor(areas.first())
            else -> overlays.show(areas)
        }
    }

    /**
     * Reads WhatsApp's background colour next to the first area, before anything is drawn on top,
     * so the patches blend in whatever theme WhatsApp uses. Android 11+ only; older versions keep [themeColor].
     */
    private fun colorIsStale(): Boolean {
        val sampledAt = colorSampledAt ?: return true
        return SystemClock.uptimeMillis() - sampledAt > COLOR_TTL_MS
    }

    @TargetApi(Build.VERSION_CODES.R)
    private fun sampleColor(area: Bounds) {
        sampling = true
        takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor, object : TakeScreenshotCallback {
            override fun onSuccess(screenshot: ScreenshotResult) {
                pixelAt(screenshot, area.left + SAMPLE_INSET, area.top + SAMPLE_INSET)?.let { overlays.color = it }
                done()
            }

            override fun onFailure(errorCode: Int) = done()

            private fun done() {
                colorSampledAt = SystemClock.uptimeMillis()
                sampling = false
                scheduleRefresh()
            }
        })
    }

    @TargetApi(Build.VERSION_CODES.R)
    private fun pixelAt(screenshot: ScreenshotResult, x: Int, y: Int): Int? {
        val buffer = screenshot.hardwareBuffer
        try {
            val hardware = Bitmap.wrapHardwareBuffer(buffer, screenshot.colorSpace) ?: return null
            val software = hardware.copy(Bitmap.Config.ARGB_8888, false)
            hardware.recycle()
            val pixel = software.getPixel(x.coerceIn(0, software.width - 1), y.coerceIn(0, software.height - 1))
            software.recycle()
            return pixel
        } finally {
            buffer.close()
        }
    }

    private fun leave(ban: Ban) {
        val now = SystemClock.uptimeMillis()
        if (now - lastBackAt < BACK_COOLDOWN_MS) return
        lastBackAt = now
        performGlobalAction(GLOBAL_ACTION_BACK)
        notice(ban)
    }

    private fun switchTab(tab: AndroidNode) {
        val now = SystemClock.uptimeMillis()
        if (now - lastSwitchAt < SWITCH_COOLDOWN_MS) return
        lastSwitchAt = now
        if (!tab.info.performAction(AccessibilityNodeInfo.ACTION_CLICK)) tap(tab.bounds)
    }

    private fun tap(area: Bounds) {
        val path = Path().apply { moveTo((area.left + area.right) / 2f, (area.top + area.bottom) / 2f) }
        val stroke = GestureDescription.StrokeDescription(path, 0, TAP_DURATION_MS)
        dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
    }

    private fun notice(ban: Ban) {
        val now = SystemClock.uptimeMillis()
        if (now - lastNoticeAt < NOTICE_COOLDOWN_MS) return
        lastNoticeAt = now
        val message = when (ban) {
            Ban.STATUS -> "Minsapp : Statuts bloqués"
            Ban.CHANNELS -> "Minsapp : Chaînes bloquées"
            Ban.META_AI -> "Minsapp : Meta AI bloqué"
            Ban.COMMUNITIES -> "Minsapp : Communautés bloquées"
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /** WhatsApp's background when the screenshot trick is unavailable. */
    private fun themeColor(): Int {
        val night = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        return if (night) WHATSAPP_DARK_BACKGROUND else Color.WHITE
    }

    private companion object {
        const val REFRESH_DELAY_MS = 80L
        const val BACK_COOLDOWN_MS = 800L
        const val SWITCH_COOLDOWN_MS = 600L
        const val NOTICE_COOLDOWN_MS = 4_000L
        const val COLOR_TTL_MS = 10 * 60_000L
        const val TAP_DURATION_MS = 50L
        const val SAMPLE_INSET = 4
        const val SYSTEM_BAR_MAX_SHARE = 0.15
        const val WHATSAPP_DARK_BACKGROUND = 0xFF0B141A.toInt()
    }
}
