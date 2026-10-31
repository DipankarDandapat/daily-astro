package com.dailyastro.app.network

import com.dailyastro.app.BuildConfig
import com.dailyastro.app.model.*
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface AstroApi {
    @POST("api/v1/users/register") suspend fun register(@Body body: RegisterRequest): User
    @POST("api/v1/users/login") suspend fun login(@Body body: LoginRequest): AuthResponse
    @GET("api/v1/languages") suspend fun languages(): List<Language>
    @GET("api/v1/zodiac-signs") suspend fun zodiacSigns(@Query("language") language: String): List<ZodiacSign>
    @GET("api/v1/horoscope/today") suspend fun today(@Query("sign") sign: String, @Query("language") language: String): Horoscope
}

object ApiProvider {
    private var _client: OkHttpClient = OkHttpClient()

    fun init(client: OkHttpClient) { _client = client }

    val service: AstroApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BACKEND_BASE_URL)
            .client(_client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AstroApi::class.java)
    }
}
