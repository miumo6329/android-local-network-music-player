package com.example.android_local_network_music_player.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class FolderEntry(
    val name: String,
    val trackCount: Int,
    val folderCount: Int,
    val yomi: String? = null
)
