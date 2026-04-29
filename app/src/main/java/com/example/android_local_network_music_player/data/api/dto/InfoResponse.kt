package com.example.android_local_network_music_player.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class InfoResponse(
    val name: String,
    val version: String,
    val baseDir: String,
    val totalTracks: Int,
    val totalFolders: Int,
    val scanCompletedAt: String? = null
)
