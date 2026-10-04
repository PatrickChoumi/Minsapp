package app.minsapp

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import minsapp.core.Ban
import minsapp.core.WhatsApp
import kotlin.math.roundToInt

/** Settings screen: guard status, ban list, setup help. */
class MainActivity : Activity() {
    private lateinit var prefs: BanPrefs
    private lateinit var status: TextView
    private lateinit var guardButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = BanPrefs(this)
        setContentView(buildContent())
    }

    override fun onResume() {
        super.onResume()
        val active = isGuardEnabled()
        status.text = if (active) "✅  Gardien actif" else "⚠️  Gardien inactif : active-le pour que les bannissements s’appliquent."
        guardButton.text = if (active) "Réglages d’accessibilité" else "Activer le gardien"
    }

    private fun buildContent(): View {
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24.dp, 24.dp, 24.dp, 24.dp)
        }

        column.addView(text("Minsapp", sizeSp = 28f, bold = true))
        column.addView(text("WhatsApp, sans ce que tu as banni.", topDp = 4))

        status = text("", topDp = 24)
        column.addView(status)
        guardButton = Button(this).apply {
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        column.addView(guardButton, matchWidth(topDp = 8))

        column.addView(text("Bannis", sizeSp = 20f, bold = true, topDp = 32))
        BANS.forEach { (ban, title, detail) -> column.addView(banRow(ban, title, detail)) }
        column.addView(text(
            "Statuts et Chaînes partagent l’onglet « Actus » : il disparaît quand les deux sont bannis.",
            sizeSp = 13f, topDp = 8,
        ))

        column.addView(Button(this).apply {
            text = "Ouvrir WhatsApp"
            setOnClickListener { openWhatsApp() }
        }, matchWidth(topDp = 24))

        column.addView(text("Première installation", sizeSp = 20f, bold = true, topDp = 32))
        column.addView(text(SETUP_HELP, topDp = 8))

        return ScrollView(this).apply {
            fitsSystemWindows = true
            addView(column)
        }
    }

    private fun banRow(ban: Ban, title: String, detail: String): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, 16.dp, 0, 0)
        addView(Switch(this@MainActivity).apply {
            text = title
            textSize = 17f
            isChecked = prefs.isBanned(ban)
            setOnCheckedChangeListener { _, checked -> prefs.setBanned(ban, checked) }
        }, matchWidth())
        addView(text(detail, sizeSp = 13f))
    }

    private fun openWhatsApp() {
        val launch = WhatsApp.packages.firstNotNullOfOrNull { packageManager.getLaunchIntentForPackage(it) }
        if (launch != null) startActivity(launch) else Toast.makeText(this, "WhatsApp n’est pas installé.", Toast.LENGTH_SHORT).show()
    }

    private fun isGuardEnabled(): Boolean {
        val me = ComponentName(this, GuardService::class.java)
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    private fun text(value: String, sizeSp: Float = 15f, bold: Boolean = false, topDp: Int = 0) = TextView(this).apply {
        text = value
        textSize = sizeSp
        if (bold) typeface = Typeface.DEFAULT_BOLD
        setPadding(0, topDp.dp, 0, 0)
    }

    private fun matchWidth(topDp: Int = 0) =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = topDp.dp
        }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).roundToInt()

    private companion object {
        val BANS = listOf(
            Triple(Ban.STATUS, "Statuts", "Le lecteur de statuts se ferme dès qu’il s’ouvre."),
            Triple(Ban.CHANNELS, "Chaînes", "Les écrans des chaînes se ferment dès qu’ils s’ouvrent."),
            Triple(Ban.META_AI, "Meta AI", "Bouton, discussion et écrans Meta AI masqués ou refermés."),
            Triple(Ban.COMMUNITIES, "Communautés", "Onglet Communautés masqué."),
        )

        const val SETUP_HELP = """1. Touche « Activer le gardien », choisis Minsapp et active-le.

2. Si Android affiche « Paramètre restreint » : ouvre Paramètres › Applications › Minsapp, menu ⋮ › « Autoriser les paramètres restreints », puis recommence l’étape 1.

3. Une fois dans WhatsApp, pour finir le ménage : archive (ou supprime) la discussion Meta AI, et si l’option existe chez toi, désactive le bouton Meta AI dans Paramètres › Discussions.

Minsapp examine seulement l’écran de WhatsApp pour repérer ce qui est banni : il n’enregistre rien et n’a pas accès à Internet."""
    }
}
