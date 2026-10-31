package com.dailyastro.app.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.store by preferencesDataStore("daily_astro")

data class Settings(
    val deviceId: String = "", val onboarded: Boolean = false, val name: String = "", val language: String = "en", val sign: String? = null,
    val dob: String? = null, val theme: String = "auto", val largeText: Boolean = false, val notifEnabled: Boolean = true,
    val notifFrequency: String = "daily", val notifTime: String = "07:00", val widget: Boolean = false, val offline: Boolean = true,
)

class SettingsStore(private val context: Context) {
    private object K {
        val device = stringPreferencesKey("device_id"); val onboarded = booleanPreferencesKey("onboarded"); val name = stringPreferencesKey("name")
        val language = stringPreferencesKey("language"); val sign = stringPreferencesKey("sign"); val dob = stringPreferencesKey("dob")
        val theme = stringPreferencesKey("theme"); val largeText = booleanPreferencesKey("large_text"); val notif = booleanPreferencesKey("notif")
        val freq = stringPreferencesKey("notif_frequency"); val time = stringPreferencesKey("notif_time"); val widget = booleanPreferencesKey("widget")
        val offline = booleanPreferencesKey("offline")
    }

    val settings: Flow<Settings> = context.store.data.map { p ->
        Settings(p[K.device] ?: "", p[K.onboarded] ?: false, p[K.name] ?: "", p[K.language] ?: "en", p[K.sign], p[K.dob], p[K.theme] ?: "auto",
            p[K.largeText] ?: false, p[K.notif] ?: true, p[K.freq] ?: "daily", p[K.time] ?: "07:00", p[K.widget] ?: false, p[K.offline] ?: true)
    }

    suspend fun current(): Settings = settings.first()

    suspend fun deviceId(): String {
        current().deviceId.takeIf { it.isNotEmpty() }?.let { return it }
        val id = "android-" + UUID.randomUUID().toString()
        context.store.edit { it[K.device] = id }
        return id
    }

    suspend fun update(transform: (Settings) -> Settings) {
        context.store.edit { p ->
            val s = transform(Settings(p[K.device] ?: "", p[K.onboarded] ?: false, p[K.name] ?: "", p[K.language] ?: "en", p[K.sign], p[K.dob],
                p[K.theme] ?: "auto", p[K.largeText] ?: false, p[K.notif] ?: true, p[K.freq] ?: "daily", p[K.time] ?: "07:00", p[K.widget] ?: false, p[K.offline] ?: true))
            p[K.onboarded] = s.onboarded; p[K.name] = s.name; p[K.language] = s.language
            if (s.sign != null) p[K.sign] = s.sign else p.remove(K.sign)
            if (s.dob != null) p[K.dob] = s.dob else p.remove(K.dob)
            p[K.theme] = s.theme; p[K.largeText] = s.largeText; p[K.notif] = s.notifEnabled; p[K.freq] = s.notifFrequency
            p[K.time] = s.notifTime; p[K.widget] = s.widget; p[K.offline] = s.offline
        }
    }

    /** Wipes everything except a fresh device id, so "Delete my data" starts a brand-new anonymous profile. */
    suspend fun clear() { context.store.edit { it.clear() } }
}
