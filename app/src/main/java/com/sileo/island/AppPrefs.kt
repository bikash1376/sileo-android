package com.sileo.island

import android.content.Context

/**
 * Which apps the user opted into showing as a Sileo island. Defaults to EMPTY —
 * nothing is intercepted until the user picks apps. Stored in SharedPreferences
 * so both the picker UI and the listener service (same process) share it.
 */
object AppPrefs {
    private const val PREFS = "sileo_prefs"
    private const val KEY_APPS = "enabled_apps"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun enabled(ctx: Context): Set<String> =
        prefs(ctx).getStringSet(KEY_APPS, emptySet())?.toSet() ?: emptySet()

    fun isEnabled(ctx: Context, pkg: String): Boolean = enabled(ctx).contains(pkg)

    fun setEnabled(ctx: Context, pkg: String, on: Boolean) {
        val updated = enabled(ctx).toMutableSet().apply { if (on) add(pkg) else remove(pkg) }
        prefs(ctx).edit().putStringSet(KEY_APPS, updated).apply()
    }
}
