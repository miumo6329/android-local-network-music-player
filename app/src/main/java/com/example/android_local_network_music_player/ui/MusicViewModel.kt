package com.example.android_local_network_music_player.ui

import android.app.Application
import android.content.ComponentName
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.android_local_network_music_player.PlaybackService
import com.example.android_local_network_music_player.data.MusicRepository
import com.example.android_local_network_music_player.data.api.MusicApiClient
import com.example.android_local_network_music_player.data.api.dto.Track
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repo: MusicRepository = MusicRepository

    private val _state = MutableStateFlow<AppUiState>(AppUiState.Connecting())
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    private val _playbackState = MutableStateFlow(PlaybackUiState())
    val playbackState: StateFlow<PlaybackUiState> = _playbackState.asStateFlow()

    private var _controller: MediaController? = null
    private var _currentQueue: List<Track> = emptyList()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            syncPosition()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val idx = _controller?.currentMediaItemIndex ?: return
            val track = _currentQueue.getOrNull(idx)
            _playbackState.update {
                it.copy(
                    track = track,
                    artworkUrl = track?.let { t -> MusicApiClient.albumArtUrl(t.id) }
                )
            }
        }

        override fun onPlaybackStateChanged(state: Int) {
            syncPosition()
        }
    }

    init {
        connectPlayer()
        runStartupSequence()
    }

    private fun connectPlayer() {
        val app = getApplication<Application>()
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        future.addListener({
            try {
                _controller = future.get().also { it.addListener(playerListener) }
                startPositionPolling()
            } catch (_: Exception) {}
        }, ContextCompat.getMainExecutor(app))
    }

    private fun startPositionPolling() {
        viewModelScope.launch {
            while (true) {
                delay(500)
                syncPosition()
            }
        }
    }

    private fun syncPosition() {
        val c = _controller ?: return
        _playbackState.update {
            it.copy(
                isPlaying = c.isPlaying,
                positionMs = c.currentPosition.coerceAtLeast(0L),
                durationMs = c.duration.coerceAtLeast(0L)
            )
        }
    }

    // --- 再生コマンド ---

    fun playAlbum(tracks: List<Track>, startIndex: Int) {
        val controller = _controller ?: return
        _currentQueue = tracks
        val items = tracks.map { t ->
            MediaItem.Builder()
                .setUri(t.url)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(t.title)
                        .setArtist(t.artist)
                        .setAlbumTitle(t.album)
                        .build()
                )
                .build()
        }
        controller.setMediaItems(items, startIndex, 0L)
        controller.prepare()
        controller.play()
        val startTrack = tracks.getOrNull(startIndex)
        _playbackState.update {
            it.copy(
                track = startTrack,
                artworkUrl = startTrack?.let { t -> MusicApiClient.albumArtUrl(t.id) }
            )
        }
    }

    fun togglePlay() {
        val c = _controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun nextTrack() {
        _controller?.seekToNextMediaItem()
    }

    fun previousTrack() {
        val c = _controller ?: return
        if (c.currentPosition < 3_000L) {
            c.seekToPreviousMediaItem()
        } else {
            c.seekTo(0L)
        }
    }

    fun seekTo(positionMs: Long) {
        _controller?.seekTo(positionMs)
    }

    fun stopPlayback() {
        _controller?.stop()
        _controller?.clearMediaItems()
    }

    // --- ナビゲーション ---

    private fun runStartupSequence() {
        _state.value = AppUiState.Connecting()
        viewModelScope.launch {
            while (true) {
                val info = try {
                    repo.getInfo()
                } catch (e: Exception) {
                    _state.value = AppUiState.StartupError(formatError(e))
                    return@launch
                }
                _state.value = AppUiState.Connecting(info)
                if (info.scanCompletedAt != null) break
                delay(SCAN_POLL_INTERVAL_MS)
            }
            loadPath(stack = listOf(ROOT_ENTRY))
        }
    }

    fun enterFolder(folderName: String) {
        val current = (_state.value as? AppUiState.Browsing) ?: return
        val parentPath = current.nav.currentEntry.path
        val newPath = if (parentPath.isEmpty()) folderName else "$parentPath/$folderName"
        loadPath(stack = current.nav.stack + NavEntry(path = newPath, label = folderName))
    }

    fun goBack(): Boolean {
        val current = (_state.value as? AppUiState.Browsing) ?: return false
        if (current.nav.isAtRoot) return false
        loadPath(stack = current.nav.stack.dropLast(1))
        return true
    }

    fun retry() {
        when (val s = _state.value) {
            is AppUiState.StartupError -> runStartupSequence()
            is AppUiState.Browsing -> if (s.nav.error != null) loadPath(s.nav.stack)
            is AppUiState.Connecting -> Unit
        }
    }

    private fun loadPath(stack: List<NavEntry>) {
        val path = stack.last().path
        _state.value = AppUiState.Browsing(NavigationState(stack = stack, isLoading = true))
        viewModelScope.launch {
            val newNav = try {
                val folders = repo.getFolders(path)
                NavigationState(stack = stack, isLoading = false, current = folders)
            } catch (e: Exception) {
                NavigationState(stack = stack, isLoading = false, error = formatError(e))
            }
            _state.value = AppUiState.Browsing(newNav)
        }
    }

    private fun formatError(e: Exception): String = when (e) {
        is HttpException -> "サーバエラー (HTTP ${e.code()})"
        is IOException -> "サーバに接続できません (${e.message ?: e::class.simpleName})"
        else -> e.message ?: (e::class.simpleName ?: "不明なエラー")
    }

    override fun onCleared() {
        _controller?.removeListener(playerListener)
        _controller?.release()
        super.onCleared()
    }

    companion object {
        private const val SCAN_POLL_INTERVAL_MS = 2000L
        private val ROOT_ENTRY = NavEntry(path = "", label = "アーティスト")
    }
}
