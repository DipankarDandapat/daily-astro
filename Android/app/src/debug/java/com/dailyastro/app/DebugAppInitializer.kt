package com.dailyastro.app

import android.app.Application
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.dailyastro.app.network.ApiProvider
import okhttp3.OkHttpClient

class DebugAppInitializer : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiProvider.init(
            OkHttpClient.Builder()
                .addInterceptor(ChuckerInterceptor.Builder(this).build())
                .build()
        )
    }
}
