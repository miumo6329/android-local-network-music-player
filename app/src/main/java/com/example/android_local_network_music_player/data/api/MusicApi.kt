package com.example.android_local_network_music_player.data.api

import com.example.android_local_network_music_player.data.api.dto.FoldersResponse
import com.example.android_local_network_music_player.data.api.dto.InfoResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface MusicApi {

    @GET("info")
    suspend fun getInfo(): InfoResponse

    @GET("folders")
    suspend fun getFolders(
        @Query("path") path: String = ""
    ): FoldersResponse
}
