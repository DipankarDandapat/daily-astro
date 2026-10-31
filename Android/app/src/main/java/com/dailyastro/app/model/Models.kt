package com.dailyastro.app.model

data class Language(val code: String, val name: String, val native_name: String)
data class ZodiacSign(val id: String, val name: String, val date_range: String, val symbol: String)
data class Category(val key: String, val title: String, val summary: String)
data class Horoscope(val date: String, val sign: ZodiacSign, val language: String, val summary: String, val special_advice: String, val categories: List<Category>, val lucky_color: String, val lucky_number: Int)
data class RegisterRequest(val device_id: String, val name: String, val password: String)
data class LoginRequest(val identifier: String, val password: String)
data class User(val user_id: String, val device_id: String, val name: String)
data class AuthResponse(val access_token: String, val token_type: String, val user: User)
