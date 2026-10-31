package com.dailyastro.app.model

data class Language(val code: String, val name: String, val native_name: String)
data class ZodiacSign(val id: String, val name: String, val date_range: String, val symbol: String)
data class Detail(val label: String, val text: String)
data class Category(val key: String, val title: String, val summary: String, val score: Int? = null, val details: List<Detail>? = null)
data class Lucky(val color: String, val number: Int, val time: String? = null, val planet: String? = null)
data class Sky(
    val sun_sign: String, val moon_sign: String, val moon_phase: String? = null, val moon_illumination: Int? = null,
    val retrogrades: List<String>? = null, val sunrise: String? = null, val sunset: String? = null,
    val rahu_kaal_start: String? = null, val rahu_kaal_end: String? = null, val tithi: String? = null, val nakshatra: String? = null,
    val location: String? = null,
)
data class Horoscope(
    val date: String, val sign: ZodiacSign, val language: String, val summary: String, val special_advice: String,
    val categories: List<Category>, val lucky_color: String, val lucky_number: Int,
    val day: String = "today", val kind: String = "daily", val lucky: Lucky? = null, val energy: Map<String, Int>? = null,
    val affirmation: String? = null, val sky: Sky? = null, val translated: Boolean = false, val attribution: String? = null,
    val attribution_url: String? = null, val fetched_at: String? = null, val saved: Boolean = false,
)
data class HistoryItem(val date: String, val sign: String, val summary: String)
data class PeriodReading(val period: String, val period_key: String, val sign: ZodiacSign, val language: String, val summary: String)
data class RegisterRequest(val device_id: String, val name: String, val password: String)
data class LoginRequest(val identifier: String, val password: String)
data class User(val user_id: String, val device_id: String, val name: String)
data class AuthResponse(val access_token: String, val token_type: String, val user: User)
data class DeviceProfile(
    val device_id: String, val name: String? = null, val language: String? = null, val sign: String? = null, val dob: String? = null,
    val notif_enabled: Boolean? = null, val notif_frequency: String? = null, val notif_time: String? = null,
    val theme: String? = null, val large_text: Boolean? = null,
)
data class Stats(val streak: Int, val read: Int, val saved: Int)
data class BookmarkRequest(val date: String, val sign: String, val language: String)
data class Bookmark(val date: String, val sign: String, val language: String, val summary: String?, val saved_at: String)
data class Compatibility(val a: ZodiacSign, val b: ZodiacSign, val score: Int, val summary: String, val elements: List<String>)
data class FeedbackRequest(val device_id: String?, val rating: Int?, val message: String)
