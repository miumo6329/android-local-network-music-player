package com.example.android_local_network_music_player.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class FoldersResponse(
    val path: String,
    val parent: String? = null,
    val folders: List<FolderEntry> = emptyList(),
    val tracks: List<Track> = emptyList()
)
