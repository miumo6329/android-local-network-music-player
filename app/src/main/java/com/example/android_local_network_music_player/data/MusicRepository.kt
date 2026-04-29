package com.example.android_local_network_music_player.data

import com.example.android_local_network_music_player.data.api.MusicApi
import com.example.android_local_network_music_player.data.api.MusicApiClient
import com.example.android_local_network_music_player.data.api.dto.FoldersResponse
import com.example.android_local_network_music_player.data.api.dto.InfoResponse
import java.util.concurrent.ConcurrentHashMap

/**
 * Python サーバ API への薄いラッパ。
 * `/folders` 結果はパスごとにメモリキャッシュする (再起動で消える)。
 */
object MusicRepository {

    private val api: MusicApi = MusicApiClient.api

    private val foldersCache = ConcurrentHashMap<String, FoldersResponse>()

    suspend fun getInfo(): InfoResponse = api.getInfo()

    suspend fun getFolders(path: String): FoldersResponse {
        foldersCache[path]?.let { return it }
        val response = api.getFolders(path)
        foldersCache[path] = response
        return response
    }

    fun clearCache() {
        foldersCache.clear()
    }
}
