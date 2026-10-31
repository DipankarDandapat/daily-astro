package com.dailyastro.app.data

import android.content.Context
import com.dailyastro.app.model.Horoscope
import com.google.gson.Gson
import java.io.File

/** Last-known readings on disk, so the app still opens to today's horoscope without a connection (Offline mode). */
class HoroscopeCache(context: Context) {
    private val dir = File(context.filesDir, "horoscopes").apply { mkdirs() }
    private val gson = Gson()

    private fun file(sign: String, language: String, day: String) = File(dir, "$sign-$language-$day.json")

    fun put(h: Horoscope) = runCatching { file(h.sign.id, h.language, h.day).writeText(gson.toJson(h)) }

    fun get(sign: String, language: String, day: String): Horoscope? =
        runCatching { gson.fromJson(file(sign, language, day).readText(), Horoscope::class.java) }.getOrNull()

    fun clear() { dir.listFiles()?.forEach { it.delete() } }
}
