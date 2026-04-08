package com.atb.systemplus
import de.robv.android.xposed.XSharedPreferences
import java.util.Locale

object HookSettings {

    @JvmStatic
    fun isEnabled(prefs: XSharedPreferences, key: String, defaultValue: Boolean): Boolean {
        return try {
            prefs.getBoolean(key, defaultValue)
        } catch (_: Throwable) {
            defaultValue
        }
    }

    @JvmStatic
    fun hasPrivilegeToken(token: String?): Boolean {
        val normalized = token?.trim().orEmpty()
        if (normalized.isEmpty()) {
            return false
        }
        val lower = normalized.lowercase(Locale.US)
        return lower != "null" && lower != "false" && lower != "0"
    }

    @JvmStatic
    fun cameraDurationSeconds(prefs: XSharedPreferences): Int {
        val fallback = 600
        return try {
            prefs.getInt("VideoRecordDurationSec", fallback).coerceIn(30, 7200)
        } catch (_: Throwable) {
            fallback
        }
    }
}

