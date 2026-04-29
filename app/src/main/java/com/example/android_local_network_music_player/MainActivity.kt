package com.example.android_local_network_music_player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.android_local_network_music_player.ui.AppUiState
import com.example.android_local_network_music_player.ui.MusicViewModel
import com.example.android_local_network_music_player.ui.screen.BrowsingScreen
import com.example.android_local_network_music_player.ui.theme.AndroidlocalnetworkmusicplayerTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidlocalnetworkmusicplayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    AppRoot(onExit = { finish() })
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AppRoot(
    viewModel: MusicViewModel = viewModel(),
    onExit: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val s = state) {
        is AppUiState.Connecting -> ConnectingScreen(s)
        is AppUiState.StartupError -> StartupErrorScreen(s, onRetry = viewModel::retry)
        is AppUiState.Browsing -> BrowsingScreen(
            nav = s.nav,
            onEnterFolder = viewModel::enterFolder,
            onGoBack = viewModel::goBack,
            onRetry = viewModel::retry,
            onExit = onExit
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ConnectingScreen(state: AppUiState.Connecting) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        val info = state.info
        when {
            info == null -> Text(text = "サーバに接続中...")
            info.scanCompletedAt == null ->
                Text(text = "ライブラリスキャン中... (フォルダ ${info.totalFolders} / トラック ${info.totalTracks})")
            else -> Text(text = "ライブラリ読み込み中...")
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun StartupErrorScreen(state: AppUiState.StartupError, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "起動に失敗しました")
        Text(text = state.message)
        Text(
            text = "[再試行]",
            modifier = Modifier.padding(top = 16.dp).clickable { onRetry() }
        )
    }
}
