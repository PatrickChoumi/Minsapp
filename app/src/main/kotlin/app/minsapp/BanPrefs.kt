package app.minsapp

import android.content.Context
import android.content.SharedPreferences
import minsapp.core.Ban

/** The user's ban list, shared by the settings screen and the guard service. */
internal class BanPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("bans", Context.MODE_PRIVATE)

    fun isBanned(ban: Ban): Boolean = prefs.getBoolean(ban.name, ban.enabledByDefault)

    fun setBanned(ban: Ban, banned: Boolean) {
        prefs.edit().putBoolean(ban.name, banned).apply()
    }

    fun bans(): Set<Ban> = Ban.entries.filter(::isBanned).toSet()

    /** SharedPreferences keeps listeners weakly: the caller must hold a reference. */
    fun listen(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.registerOnSharedPreferenceChangeListener(listener)

    fun stopListening(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
}
