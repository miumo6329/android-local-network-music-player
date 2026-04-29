package com.example.android_local_network_music_player.ui

import com.example.android_local_network_music_player.data.api.dto.FoldersResponse
import com.example.android_local_network_music_player.data.api.dto.InfoResponse

/** ナビゲーションスタックの 1 エントリ。ルートは `path = ""`。 */
data class NavEntry(
    val path: String,
    val label: String
)

/**
 * アプリ全体の UI 状態。
 * - [Connecting]: 起動シーケンス中 (info 取得中 / scan 完了待ち)
 * - [StartupError]: /info 自体が失敗
 * - [Browsing]: ライブラリブラウズ中 (フォルダ取得は内部で loading/error を持つ)
 */
sealed interface AppUiState {

    data class Connecting(val info: InfoResponse? = null) : AppUiState

    data class StartupError(val message: String) : AppUiState

    data class Browsing(val nav: NavigationState) : AppUiState
}

data class NavigationState(
    val stack: List<NavEntry>,
    val isLoading: Boolean = false,
    val error: String? = null,
    val current: FoldersResponse? = null
) {
    val currentEntry: NavEntry get() = stack.last()
    /** 1=アーティスト一覧, 2=アルバム一覧, 3=トラック一覧 */
    val depth: Int get() = stack.size
    val isAtRoot: Boolean get() = stack.size == 1
}
