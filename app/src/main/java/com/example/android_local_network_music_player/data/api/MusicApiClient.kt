package com.example.android_local_network_music_player.data.api

import com.example.android_local_network_music_player.BuildConfig
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object MusicApiClient {

    private val baseUrl: String = BuildConfig.MUSIC_SERVER_BASE_URL.let {
        if (it.endsWith("/")) it else "$it/"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val okHttp: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                    }
                )
            }
        }
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttp)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val api: MusicApi = retrofit.create(MusicApi::class.java)

    /**
     * `/albumart?path=<id>` の絶対 URL を返す。Coil 等にそのまま渡せる。
     * `id` は `Track.id` (base_dir からの相対パス、`/` 区切り)。
     */
    fun albumArtUrl(trackId: String): String {
        val encoded = URLEncoder.encode(trackId, "UTF-8")
        return "${baseUrl}albumart?path=$encoded"
    }
}
