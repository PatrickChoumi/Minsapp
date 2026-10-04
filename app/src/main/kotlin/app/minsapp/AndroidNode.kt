package app.minsapp

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import minsapp.core.Bounds
import minsapp.core.UiNode

/** Exposes an accessibility node to the rules in `core`. Children are fetched once, on demand. */
internal class AndroidNode(val info: AccessibilityNodeInfo) : UiNode {
    override val viewId: String? get() = info.viewIdResourceName
    override val text: CharSequence? get() = info.text
    override val description: CharSequence? get() = info.contentDescription
    override val isClickable: Boolean get() = info.isClickable
    override val isSelected: Boolean get() = info.isSelected
    override val isVisible: Boolean get() = info.isVisibleToUser

    override val bounds: Bounds by lazy {
        val rect = Rect()
        info.getBoundsInScreen(rect)
        Bounds(rect.left, rect.top, rect.right, rect.bottom)
    }

    override val children: List<UiNode> by lazy {
        (0 until info.childCount).mapNotNull { info.getChild(it) }.map(::AndroidNode)
    }
}
