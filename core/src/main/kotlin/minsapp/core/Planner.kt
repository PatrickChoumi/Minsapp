package minsapp.core

/** Which WhatsApp screen is in front, as far as the rules care. */
enum class Screen { HOME, CONVERSATION, OTHER }

/** What the guard should do with the WhatsApp screen currently shown. */
data class Plan(
    /** Areas to cover so they can be neither seen nor tapped. */
    val covers: List<Bounds> = emptyList(),
    /** Tab to tap because a banned tab is selected (e.g. the user swiped onto "Actus"). */
    val switchTo: UiNode? = null,
    /** A banned screen is open: press Back to leave it. */
    val leave: Ban? = null,
)

object Planner {
    // Proportions of the WhatsApp window used to tell UI parts apart.
    private const val NAV_FALLBACK_TOP = 0.75 // bottom quarter: where tabs live when the nav id is unknown
    private const val TITLE_BAR_BOTTOM = 0.15 // top strip: a chat's title bar
    private const val TAB_MAX_WIDTH = 0.5 // a single tab is narrower than half the bar
    private const val TARGET_MAX_HEIGHT = 0.3 // a button or a chat row, not a whole list
    private const val COVER_MAX_AREA = 0.25 // never hide more than a quarter of the window

    /** Window class name of a WhatsApp activity → [Screen], or null for anything else (dialogs, popups…). */
    fun screenOf(className: String): Screen? = when {
        !className.startsWith("com.whatsapp") -> null
        className.endsWith(WhatsApp.HOME_ACTIVITY) -> Screen.HOME
        className.endsWith(WhatsApp.CONVERSATION_ACTIVITY) -> Screen.CONVERSATION
        else -> Screen.OTHER
    }

    /** The ban a newly opened WhatsApp window belongs to, judged from its class name alone. */
    fun bannedWindow(className: String, bans: Set<Ban>): Ban? {
        if (!className.startsWith("com.whatsapp")) return null
        val simpleName = className.substringAfterLast('.')
        return when {
            Ban.STATUS in bans && simpleName.contains("StatusPlayback") -> Ban.STATUS
            Ban.CHANNELS in bans && (simpleName.contains("Newsletter") || ".newsletter." in className) -> Ban.CHANNELS
            Ban.META_AI in bans && (".bonsai." in className || simpleName.startsWith("Bonsai")) -> Ban.META_AI
            Ban.COMMUNITIES in bans && (simpleName.startsWith("Community") || ".community." in className) -> Ban.COMMUNITIES
            else -> null
        }
    }

    fun plan(root: UiNode, screen: Screen, bans: Set<Ban>): Plan {
        if (bans.isEmpty()) return Plan()
        val window = root.bounds
        if (window.isEmpty) return Plan()

        val scan = Scan(window).apply { walk(root, ArrayList(), inNav = false) }
        val navBounds = scan.navBounds
        val onHome = navBounds != null || screen == Screen.HOME

        if (!onHome) {
            val leave = if (Ban.META_AI in bans && screen == Screen.CONVERSATION && scan.metaAiInTitleBar) Ban.META_AI else null
            return Plan(leave = leave)
        }

        val tabs = scan.tabs(navBounds?.width ?: window.width)
        val covers = mutableListOf<Bounds>()
        val updatesBanned = Ban.STATUS in bans && Ban.CHANNELS in bans
        val communitiesBanned = Ban.COMMUNITIES in bans
        if (updatesBanned) tabs[Tab.UPDATES]?.let { covers += it.item.bounds }
        if (communitiesBanned) tabs[Tab.COMMUNITIES]?.let { covers += it.item.bounds }
        if (Ban.META_AI in bans) scan.metaAiHits.forEach { covers += it.target(window).bounds }

        val onBannedTab = (updatesBanned && tabs[Tab.UPDATES]?.selected == true) ||
            (communitiesBanned && tabs[Tab.COMMUNITIES]?.selected == true)
        val chats = tabs[Tab.CHATS]
        val switchTo = if (onBannedTab && chats != null && !chats.selected) chats.item else null

        return Plan(covers = tidy(covers, window), switchTo = switchTo)
    }

    /** Clips to the window, drops empty, oversized, duplicate and nested areas. */
    private fun tidy(covers: List<Bounds>, window: Bounds): List<Bounds> {
        val maxArea = window.area * COVER_MAX_AREA
        val clipped = covers.map { it.intersect(window) }
            .filter { !it.isEmpty && it.area <= maxArea }
            .distinct()
        return clipped.filter { cover -> clipped.none { other -> other != cover && other.contains(cover) } }
    }

    private enum class Tab(val labels: List<String>) {
        CHATS(WhatsApp.chatsTab),
        UPDATES(WhatsApp.updatesTab),
        COMMUNITIES(WhatsApp.communitiesTab),
    }

    private class TabState(val item: UiNode, val selected: Boolean)

    /** A node and its ancestors, nearest first. */
    private class Hit(val node: UiNode, val ancestors: List<UiNode>) {
        /** Nearest clickable element around the node (the button or chat row), or the node itself. */
        fun target(window: Bounds): UiNode {
            val maxHeight = window.height * TARGET_MAX_HEIGHT
            return (listOf(node) + ancestors)
                .takeWhile { it.bounds.height <= maxHeight }
                .firstOrNull { it.isClickable }
                ?: node
        }
    }

    private class Scan(private val window: Bounds) {
        var navBounds: Bounds? = null
        var metaAiInTitleBar = false
        val metaAiHits = mutableListOf<Hit>()
        private val navHits = mutableMapOf<Tab, MutableList<Hit>>()
        private val fallbackHits = mutableMapOf<Tab, MutableList<Hit>>()

        fun walk(node: UiNode, ancestors: ArrayList<UiNode>, inNav: Boolean) {
            if (!node.isVisible) return
            val isNav = !inNav && node.viewId?.endsWith(WhatsApp.BOTTOM_NAV_ID_SUFFIX) == true
            if (isNav) navBounds = node.bounds
            val nowInNav = inNav || isNav

            val labels = listOfNotNull(node.description, node.text)
            val tab = Tab.entries.firstOrNull { tab -> labels.any { it.matchesLabel(tab.labels) } }
            if (tab != null) {
                val inBottomStrip = node.bounds.top >= window.top + window.height * NAV_FALLBACK_TOP
                when {
                    nowInNav -> navHits.getOrPut(tab) { mutableListOf() } += hit(node, ancestors)
                    inBottomStrip -> fallbackHits.getOrPut(tab) { mutableListOf() } += hit(node, ancestors)
                }
            } else if (!nowInNav && labels.any { it.matchesLabel(WhatsApp.metaAi) }) {
                metaAiHits += hit(node, ancestors)
                if (node.bounds.bottom <= window.top + window.height * TITLE_BAR_BOTTOM) metaAiInTitleBar = true
            }

            ancestors += node
            node.children.forEach { walk(it, ancestors, nowInNav) }
            ancestors.removeAt(ancestors.lastIndex)
        }

        /** Each tab's whole item (icon + label), using the nav bar when found, the bottom strip otherwise. */
        fun tabs(barWidth: Int): Map<Tab, TabState> {
            val hits = if (navBounds != null) navHits else fallbackHits
            val maxWidth = barWidth * TAB_MAX_WIDTH
            return hits.mapNotNull { (tab, tabHits) ->
                val chains = tabHits.map { hit ->
                    (listOf(hit.node) + hit.ancestors).takeWhile { it.bounds.width < maxWidth }
                }
                val item = chains.mapNotNull { it.lastOrNull() }
                    .filter { !it.bounds.isEmpty }
                    .maxByOrNull { it.bounds.area }
                    ?: return@mapNotNull null
                tab to TabState(item, selected = chains.any { chain -> chain.any { it.isSelected } })
            }.toMap()
        }

        private fun hit(node: UiNode, ancestors: List<UiNode>) = Hit(node, ancestors.asReversed().toList())
    }
}
