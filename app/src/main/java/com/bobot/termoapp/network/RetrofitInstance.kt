package com.bobot.termoapp.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.bobot.termoapp.apiservice.WeatherApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Singleton object responsible for providing a configured Retrofit instance
 * for making network requests to the Open-Meteo API.
 *
 * This object ensures that the Retrofit client is initialized only once
 * and can be easily accessed throughout the application to fetch weather data.
 */
object RetrofitInstance {

    /**
     * Configures an HTTP logging interceptor.
     * This interceptor is used to log network request and response details (headers, body, etc.)
     * to the Logcat, which is extremely useful for debugging network issues during development.
     * The `Level.BODY` setting ensures that the full request and response bodies are logged.
     */
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    /**
     * Builds an OkHttpClient instance.
     * OkHttpClient is an HTTP client that Retrofit uses internally to perform network operations.
     * Here, we add the `logging` interceptor to this client, so all requests made
     * through this client will have their details logged.
     */
    private val client = OkHttpClient.Builder()
        .addInterceptor(logging) // Add the logging interceptor to the OkHttpClient
        .build()

    /**
     * Initializes the Retrofit builder.
     * Retrofit is a type-safe HTTP client for Android and Java. It simplifies the process
     * of making network requests by converting API endpoints into Java/Kotlin interfaces.
     *
     * - `baseUrl`: Sets the base URL for the API. All relative URLs defined in the
     * `WeatherApiService` will be appended to this base URL. It's crucial for it to end with '/'.
     * - `addConverterFactory`: Specifies a converter factory to handle data serialization
     * and deserialization. `GsonConverterFactory.create()` uses Gson to convert JSON
     * responses into Kotlin objects and vice-versa.
     * - `client`: Assigns the custom OkHttpClient (with logging) to Retrofit.
     */
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/") // Base URL for the Open-Meteo API
        .addConverterFactory(GsonConverterFactory.create()) // Use Gson for JSON serialization/deserialization
        .client(client) // Use the custom OkHttpClient with logging
        .build()

    /**
     * Provides a lazily initialized instance of `WeatherApiService`.
     * The `by lazy` delegate ensures that the `WeatherApiService` instance is created
     * only when it's accessed for the first time, optimizing resource usage.
     *
     * This `api` property is the main entry point for making API calls defined
     * in the `WeatherApiService` interface.
     */
    val api: WeatherApiService by lazy {
        retrofit.create(WeatherApiService::class.java)
    }
}