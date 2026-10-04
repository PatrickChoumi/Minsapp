package app.minsapp

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import minsapp.core.Bounds

/**
 * Opaque patches drawn above WhatsApp. Each one hides an area and swallows taps on it,
 * so a banned button can be neither seen nor used.
 */
internal class Overlays(private val service: AccessibilityService) {
    private class Patch(val view: View, var area: Bounds)

    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val patches = mutableListOf<Patch>()

    var color: Int = 0
        set(value) {
            field = value
            patches.forEach { it.view.setBackgroundColor(value) }
        }

    val isEmpty: Boolean get() = patches.isEmpty()

    fun show(areas: List<Bounds>) {
        while (patches.size > areas.size) remove(patches.removeAt(patches.lastIndex))
        areas.forEachIndexed { i, area ->
            val patch = patches.getOrNull(i)
            when {
                patch == null -> add(area)
                patch.area != area -> {
                    patch.area = area
                    windowManager.updateViewLayout(patch.view, layoutFor(area))
                }
            }
        }
    }

    fun clear() = show(emptyList())

    @SuppressLint("ClickableViewAccessibility")
    private fun add(area: Bounds) {
        val view = View(service).apply {
            setBackgroundColor(color)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            setOnTouchListener { _, _ -> true }
        }
        windowManager.addView(view, layoutFor(area))
        patches += Patch(view, area)
    }

    private fun remove(patch: Patch) {
        // The system may already have dropped the window if the service was turned off.
        runCatching { windowManager.removeView(patch.view) }
    }

    private fun layoutFor(area: Bounds) = WindowManager.LayoutParams(
        area.width,
        area.height,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.OPAQUE,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = area.left
        y = area.top
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }
}
