package com.example.android_local_network_music_player.ui

import com.example.android_local_network_music_player.data.api.dto.Track

data class PlaybackUiState(
    val track: Track? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val artworkUrl: String? = null
)
