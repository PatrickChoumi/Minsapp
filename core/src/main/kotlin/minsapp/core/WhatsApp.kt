package minsapp.core

/**
 * What Minsapp knows about the official WhatsApp Android app's interface.
 * When WhatsApp changes its UI, this is the file to update.
 */
object WhatsApp {
    val packages = setOf("com.whatsapp", "com.whatsapp.w4b")

    /** Resource id suffix of the home screen's bottom navigation bar (`com.whatsapp:id/bottom_nav`). */
    const val BOTTOM_NAV_ID_SUFFIX = ":id/bottom_nav"

    // Tab labels as WhatsApp shows them, in French, English, Spanish, Portuguese, German and Italian.
    // Status and Channels share the "Updates" tab.
    val chatsTab = listOf("Discussions", "Chats", "Conversas", "Chat")
    val updatesTab = listOf("Actus", "Updates", "Novedades", "Atualizações", "Aktuelles", "Aggiornamenti")
    val communitiesTab = listOf("Communautés", "Communities", "Comunidades", "Communitys", "Community")

    /** Names Meta AI goes by: its chat, its floating button and its shortcuts. */
    val metaAi = listOf("Meta AI", "Ask Meta AI", "Demander à Meta AI")

    /** Activity class name endings for the home screen and for a chat. */
    const val HOME_ACTIVITY = ".HomeActivity"
    const val CONVERSATION_ACTIVITY = ".Conversation"
}

/**
 * True when the label, up to its first comma or line break, equals one of [labels] (ignoring case).
 * The cut drops what Android appends after the name, such as a badge ("Actus, 3 nouvelles mises à jour"),
 * while a message preview like "Meta AI c'est quoi ?" does not count as "Meta AI".
 */
fun CharSequence?.matchesLabel(labels: List<String>): Boolean {
    val head = this?.toString()?.split(',', '\n')?.first()?.trim() ?: return false
    if (head.isEmpty()) return false
    return labels.any { it.equals(head, ignoreCase = true) }
}
