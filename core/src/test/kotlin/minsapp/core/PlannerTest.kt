package minsapp.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

private class Node(
    override val bounds: Bounds,
    override val viewId: String? = null,
    override val text: String? = null,
    override val description: String? = null,
    override val isClickable: Boolean = false,
    override val isSelected: Boolean = false,
    override val isVisible: Boolean = true,
    override val children: List<UiNode> = emptyList(),
) : UiNode

private fun b(left: Int, top: Int, right: Int, bottom: Int) = Bounds(left, top, right, bottom)

private val defaultBans = Ban.entries.filter { it.enabledByDefault }.toSet()

/** A 1080×2400 WhatsApp home screen, shaped like the real accessibility tree. */
private class Home(
    selectedTab: Int = 0,
    tabLabels: List<String> = listOf("Discussions", "Actus", "Communautés", "Appels"),
    updatesBadge: String = ", 3 nouvelles mises à jour",
    navId: String? = "com.whatsapp:id/bottom_nav",
    offscreenPage: Boolean = false,
) {
    val tabs: List<Node> = tabLabels.mapIndexed { i, label ->
        val area = b(i * 270, 2200, (i + 1) * 270, 2400)
        val description = if (i == 1) label + updatesBadge else label
        Node(
            area,
            description = description,
            isClickable = i != selectedTab, // Material's selected item is not clickable
            isSelected = i == selectedTab,
            children = listOf(
                Node(b(area.left + 85, 2220, area.right - 85, 2300), viewId = "com.whatsapp:id/navigation_bar_item_icon_view"),
                Node(b(area.left + 40, 2310, area.right - 40, 2360), text = label),
            ),
        )
    }

    val metaAiRow = chatRow(1, "Meta AI", "Pose-moi une question")
    val friendRow = chatRow(2, "Awa", "Meta AI c'est quoi ?")
    val momRow = chatRow(3, "Maman", "À ce soir")
    val metaAiButton = Node(b(940, 1880, 1040, 1980), description = "Meta AI", isClickable = true)
    val newChatButton = Node(b(900, 2010, 1060, 2170), description = "Nouvelle discussion", isClickable = true)

    val root: UiNode = Node(
        b(0, 0, 1080, 2400),
        viewId = "com.whatsapp:id/root_view",
        children = listOfNotNull(
            Node(b(0, 80, 1080, 240), viewId = "com.whatsapp:id/toolbar", children = listOf(Node(b(40, 120, 400, 200), text = "WhatsApp"))),
            Node(b(40, 260, 1040, 380), viewId = "com.whatsapp:id/my_search_bar", isClickable = true, children = listOf(Node(b(120, 290, 900, 350), text = "Demander à Meta AI ou rechercher"))),
            Node(b(0, 400, 1080, 2200), viewId = "com.whatsapp:id/list", children = listOf(metaAiRow, friendRow, momRow)),
            metaAiButton,
            newChatButton,
            Node(b(0, 2200, 1080, 2400), viewId = navId, children = listOf(Node(b(0, 2200, 1080, 2400), children = tabs))),
            if (offscreenPage) Node(b(1080, 400, 2160, 2200), isVisible = false, children = listOf(chatRow(1, "Meta AI", "", left = 1080))) else null,
        ),
    )

    companion object {
        fun chatRow(index: Int, name: String, preview: String, left: Int = 0): Node {
            val top = 400 + (index - 1) * 200
            return Node(
                b(left, top, left + 1080, top + 200),
                isClickable = true,
                children = listOf(
                    Node(b(left + 30, top + 30, left + 170, top + 170), viewId = "com.whatsapp:id/contact_photo", isClickable = true),
                    Node(b(left + 200, top + 40, left + 800, top + 100), viewId = "com.whatsapp:id/conversations_row_contact_name", text = name),
                    Node(b(left + 200, top + 110, left + 1000, top + 170), text = preview),
                ),
            )
        }
    }
}

private fun conversation(title: String): UiNode = Node(
    b(0, 0, 1080, 2400),
    children = listOf(
        Node(b(0, 80, 1080, 240), viewId = "com.whatsapp:id/toolbar", children = listOf(
            Node(b(160, 100, 700, 170), viewId = "com.whatsapp:id/conversation_contact_name", text = title),
        )),
        Node(b(0, 900, 800, 1000), text = "Meta AI"), // a message that merely says "Meta AI"
        Node(b(0, 2250, 1080, 2400), viewId = "com.whatsapp:id/entry", text = "Message"),
    ),
)

class PlannerTest {
    @Test
    fun `default bans hide the Updates tab and every Meta AI entry point, nothing else`() {
        val home = Home()
        val plan = Planner.plan(home.root, Screen.HOME, defaultBans)

        assertEquals(
            setOf(home.tabs[1].bounds, home.metaAiRow.bounds, home.metaAiButton.bounds),
            plan.covers.toSet(),
        )
        assertNull(plan.switchTo)
        assertNull(plan.leave)
    }

    @Test
    fun `a message preview mentioning Meta AI does not hide that chat`() {
        val home = Home()
        val plan = Planner.plan(home.root, Screen.HOME, defaultBans)
        assertTrue(home.friendRow.bounds !in plan.covers)
        assertTrue(home.momRow.bounds !in plan.covers)
    }

    @Test
    fun `the Updates tab stays while either Status or Channels is allowed`() {
        val home = Home()
        assertTrue(home.tabs[1].bounds !in Planner.plan(home.root, Screen.HOME, setOf(Ban.STATUS)).covers)
        assertTrue(home.tabs[1].bounds !in Planner.plan(home.root, Screen.HOME, setOf(Ban.CHANNELS)).covers)
    }

    @Test
    fun `communities are hidden only when banned`() {
        val home = Home()
        assertTrue(home.tabs[2].bounds !in Planner.plan(home.root, Screen.HOME, defaultBans).covers)
        assertTrue(home.tabs[2].bounds in Planner.plan(home.root, Screen.HOME, defaultBans + Ban.COMMUNITIES).covers)
    }

    @Test
    fun `landing on the Updates tab switches back to Chats`() {
        val home = Home(selectedTab = 1)
        val plan = Planner.plan(home.root, Screen.HOME, defaultBans)
        assertSame(home.tabs[0], plan.switchTo)
    }

    @Test
    fun `no switch when the selected tab is allowed`() {
        val home = Home(selectedTab = 3)
        assertNull(Planner.plan(home.root, Screen.HOME, defaultBans).switchTo)
        assertNull(Planner.plan(Home(selectedTab = 1).root, Screen.HOME, setOf(Ban.META_AI)).switchTo)
    }

    @Test
    fun `tabs are still found by label at the bottom of the screen when the nav id changes`() {
        val home = Home(navId = "com.whatsapp:id/renamed_nav")
        val plan = Planner.plan(home.root, Screen.HOME, defaultBans)
        assertTrue(home.tabs[1].bounds in plan.covers)
    }

    @Test
    fun `English interface works too`() {
        val home = Home(tabLabels = listOf("Chats", "Updates", "Communities", "Calls"), updatesBadge = ", 2 new updates")
        val plan = Planner.plan(home.root, Screen.HOME, defaultBans + Ban.COMMUNITIES)
        assertTrue(home.tabs[1].bounds in plan.covers)
        assertTrue(home.tabs[2].bounds in plan.covers)
    }

    @Test
    fun `elements on an off-screen page are ignored`() {
        val home = Home(offscreenPage = true)
        val plan = Planner.plan(home.root, Screen.HOME, defaultBans)
        assertTrue(plan.covers.all { it.right <= 1080 })
    }

    @Test
    fun `nothing banned means nothing touched`() {
        val home = Home(selectedTab = 1)
        assertEquals(Plan(), Planner.plan(home.root, Screen.HOME, emptySet()))
    }

    @Test
    fun `opening the Meta AI chat is undone`() {
        assertEquals(Ban.META_AI, Planner.plan(conversation("Meta AI"), Screen.CONVERSATION, defaultBans).leave)
    }

    @Test
    fun `ordinary chats are left alone, even when someone writes Meta AI`() {
        val plan = Planner.plan(conversation("Maman"), Screen.CONVERSATION, defaultBans)
        assertEquals(Plan(), plan)
    }

    @Test
    fun `the Meta AI chat stays open when Meta AI is allowed`() {
        assertNull(Planner.plan(conversation("Meta AI"), Screen.CONVERSATION, setOf(Ban.STATUS, Ban.CHANNELS)).leave)
    }

    @Test
    fun `huge areas are never covered`() {
        val wholeList = Node(b(0, 0, 1080, 2400), text = "Meta AI", isClickable = true)
        val plan = Planner.plan(Node(b(0, 0, 1080, 2400), children = listOf(wholeList)), Screen.HOME, defaultBans)
        assertTrue(plan.covers.isEmpty())
    }
}

class WindowRulesTest {
    @Test
    fun `banned screens are recognised from their class name`() {
        assertEquals(Ban.STATUS, Planner.bannedWindow("com.whatsapp.status.playback.StatusPlaybackActivity", defaultBans))
        assertEquals(Ban.CHANNELS, Planner.bannedWindow("com.whatsapp.newsletter.ui.NewsletterInfoActivity", defaultBans))
        assertEquals(Ban.META_AI, Planner.bannedWindow("com.whatsapp.bonsai.home.AiHomeActivity", defaultBans))
        assertEquals(Ban.COMMUNITIES, Planner.bannedWindow("com.whatsapp.community.CommunityHomeActivity", defaultBans + Ban.COMMUNITIES))
    }

    @Test
    fun `allowed or unrelated windows pass`() {
        assertNull(Planner.bannedWindow("com.whatsapp.status.playback.StatusPlaybackActivity", setOf(Ban.META_AI)))
        assertNull(Planner.bannedWindow("com.whatsapp.community.CommunityHomeActivity", defaultBans))
        assertNull(Planner.bannedWindow("com.whatsapp.Conversation", defaultBans))
        assertNull(Planner.bannedWindow("com.whatsapp.HomeActivity", defaultBans))
        assertNull(Planner.bannedWindow("com.example.StatusPlaybackActivity", defaultBans))
    }

    @Test
    fun `screens are recognised`() {
        assertEquals(Screen.HOME, Planner.screenOf("com.whatsapp.HomeActivity"))
        assertEquals(Screen.CONVERSATION, Planner.screenOf("com.whatsapp.Conversation"))
        assertEquals(Screen.OTHER, Planner.screenOf("com.whatsapp.settings.Settings"))
        assertNull(Planner.screenOf("android.app.Dialog"))
    }
}

class LabelTest {
    @Test
    fun `labels match up to the first comma or line break`() {
        assertTrue("Actus, 3 nouvelles mises à jour".matchesLabel(WhatsApp.updatesTab))
        assertTrue("actus".matchesLabel(WhatsApp.updatesTab))
        assertTrue("Meta AI\nEn ligne".matchesLabel(WhatsApp.metaAi))
        assertTrue(!"Meta AI c'est quoi ?".matchesLabel(WhatsApp.metaAi))
        assertTrue(!"Chats".matchesLabel(listOf("Chat")))
        assertTrue(!"".matchesLabel(WhatsApp.metaAi))
        assertTrue(!(null as CharSequence?).matchesLabel(WhatsApp.metaAi))
    }
}
