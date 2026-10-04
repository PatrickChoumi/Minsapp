package minsapp.core

/** Screen rectangle, in screen pixels. */
data class Bounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val area: Long get() = if (isEmpty) 0 else width.toLong() * height

    val isEmpty: Boolean get() = width <= 0 || height <= 0

    fun contains(other: Bounds): Boolean =
        left <= other.left && top <= other.top && right >= other.right && bottom >= other.bottom

    fun intersect(other: Bounds): Bounds = Bounds(
        maxOf(left, other.left),
        maxOf(top, other.top),
        minOf(right, other.right),
        minOf(bottom, other.bottom),
    )
}

/**
 * What the rules need to know about an on-screen element.
 * On the phone this wraps an AccessibilityNodeInfo; in tests it is a plain object.
 */
interface UiNode {
    val viewId: String?
    val text: CharSequence?
    val description: CharSequence?
    val bounds: Bounds
    val isClickable: Boolean
    val isSelected: Boolean
    val isVisible: Boolean
    val children: List<UiNode>
}
