package com.example.android_local_network_music_player.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class Track(
    val id: String,
    val filename: String,
    val url: String,
    val mimeType: String,
    val sizeBytes: Long,
    val title: String? = null,
    val artist: String? = null,
    val albumArtist: String? = null,
    val album: String? = null,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val year: Int? = null,
    val genre: String? = null,
    val durationMs: Long? = null
)
