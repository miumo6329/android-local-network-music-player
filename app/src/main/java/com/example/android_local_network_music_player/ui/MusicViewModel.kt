package com.example.android_local_network_music_player.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_local_network_music_player.data.MusicRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class MusicViewModel(
    private val repo: MusicRepository = MusicRepository
) : ViewModel() {

    private val _state = MutableStateFlow<AppUiState>(AppUiState.Connecting())
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    init {
        runStartupSequence()
    }

    /**
     * /info をポーリングし、scanCompletedAt が返ったらルート /folders をロード。
     * /info 自体が失敗した場合は StartupError へ遷移する (ユーザは [retry] で再試行可)。
     */
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

    /**
     * 1 階層戻る。ルートにいる場合は false (上位で終了確認ダイアログを出す)。
     */
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

    companion object {
        private const val SCAN_POLL_INTERVAL_MS = 2000L
        private val ROOT_ENTRY = NavEntry(path = "", label = "アーティスト")
    }
}
