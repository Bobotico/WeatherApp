package com.bobot.termoapp.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.bobot.termoapp.apiservice.WeatherApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

object RetrofitInstance {
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/") // Ensure it ends with '/'
        .addConverterFactory(GsonConverterFactory.create())
        .client(client)
        .build()

    val api: WeatherApiService by lazy {
        retrofit.create(WeatherApiService::class.java)
    }
}