package com.dailyastro.app.network

import com.dailyastro.app.BuildConfig
import com.dailyastro.app.model.*
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface AstroApi {
    @POST("api/v1/users/register") suspend fun register(@Body body: RegisterRequest): User
    @POST("api/v1/users/login") suspend fun login(@Body body: LoginRequest): AuthResponse
    @GET("api/v1/languages") suspend fun languages(): List<Language>
    @GET("api/v1/zodiac-signs") suspend fun zodiacSigns(@Query("language") language: String): List<ZodiacSign>
    @GET("api/v1/horoscope/today") suspend fun today(@Query("sign") sign: String, @Query("language") language: String): Horoscope
    @GET("api/v1/horoscope") suspend fun horoscope(@Query("sign") sign: String, @Query("language") language: String, @Query("day") day: String): Horoscope
    @GET("api/v1/horoscope/history") suspend fun history(@Query("sign") sign: String, @Query("language") language: String, @Query("days") days: Int = 7): List<HistoryItem>
    @GET("api/v1/horoscope/period") suspend fun period(@Query("sign") sign: String, @Query("period") period: String, @Query("language") language: String): PeriodReading
    @GET("api/v1/compatibility") suspend fun compatibility(@Query("a") a: String, @Query("b") b: String, @Query("language") language: String): Compatibility
    @POST("api/v1/users/device") suspend fun upsertDevice(@Body body: DeviceProfile): DeviceProfile
    @GET("api/v1/users/{device}/stats") suspend fun stats(@Path("device") device: String): Stats
    @GET("api/v1/users/{device}/saved") suspend fun saved(@Path("device") device: String): List<Bookmark>
    @POST("api/v1/users/{device}/saved") suspend fun save(@Path("device") device: String, @Body body: BookmarkRequest): Stats
    @DELETE("api/v1/users/{device}/saved/{date}/{sign}") suspend fun unsave(@Path("device") device: String, @Path("date") date: String, @Path("sign") sign: String): Stats
    @DELETE("api/v1/users/{device}") suspend fun deleteData(@Path("device") device: String): Map<String, Any>
    @POST("api/v1/feedback") suspend fun feedback(@Body body: FeedbackRequest): Map<String, Any>
}

object ApiProvider {
    private var _client: OkHttpClient = OkHttpClient()
    @Volatile var deviceId: String? = null

    fun init(client: OkHttpClient) { _client = client }

    val service: AstroApi by lazy {
        // Every call carries the device id so the backend can log requests and count reading streaks.
        val client = _client.newBuilder()
            .connectTimeout(15, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder().header("X-App-Version", BuildConfig.VERSION_NAME)
                deviceId?.let { request.header("X-Device-Id", it) }
                chain.proceed(request.build())
            }.build()
        Retrofit.Builder()
            .baseUrl(BuildConfig.BACKEND_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AstroApi::class.java)
    }
}
