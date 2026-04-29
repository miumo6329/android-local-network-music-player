package com.example.android_local_network_music_player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.android_local_network_music_player.data.api.dto.FolderEntry
import com.example.android_local_network_music_player.data.api.dto.Track
import com.example.android_local_network_music_player.ui.AppUiState
import com.example.android_local_network_music_player.ui.MusicViewModel
import com.example.android_local_network_music_player.ui.NavigationState
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
                    AppRoot()
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AppRoot(viewModel: MusicViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val s = state) {
        is AppUiState.Connecting -> ConnectingScreen(s)
        is AppUiState.StartupError -> StartupErrorScreen(s, onRetry = viewModel::retry)
        is AppUiState.Browsing -> BrowsingScreen(
            nav = s.nav,
            onEnterFolder = viewModel::enterFolder,
            onGoBack = { viewModel.goBack() },
            onRetry = viewModel::retry
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
        if (info == null) {
            Text(text = "サーバに接続中...")
        } else if (info.scanCompletedAt == null) {
            Text(text = "ライブラリスキャン中... (フォルダ ${info.totalFolders} / トラック ${info.totalTracks})")
        } else {
            Text(text = "ライブラリ読み込み中...")
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

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun BrowsingScreen(
    nav: NavigationState,
    onEnterFolder: (String) -> Unit,
    onGoBack: () -> Unit,
    onRetry: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = nav.stack.joinToString(" / ") { it.label.ifEmpty { "(root)" } })
        if (!nav.isAtRoot) {
            Text(
                text = "← 戻る",
                modifier = Modifier.padding(vertical = 8.dp).clickable { onGoBack() }
            )
        }
        when {
            nav.isLoading -> Text(text = "読み込み中...", modifier = Modifier.padding(top = 8.dp))
            nav.error != null -> Column(modifier = Modifier.padding(top = 8.dp)) {
                Text(text = "エラー: ${nav.error}")
                Text(
                    text = "[再試行]",
                    modifier = Modifier.padding(top = 8.dp).clickable { onRetry() }
                )
            }
            nav.current != null -> FolderContent(
                folders = nav.current.folders,
                tracks = nav.current.tracks,
                onEnterFolder = onEnterFolder
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FolderContent(
    folders: List<FolderEntry>,
    tracks: List<Track>,
    onEnterFolder: (String) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(folders, key = { "f:${it.name}" }) { folder ->
            Text(
                text = "${folder.name}  (${folder.trackCount} tracks)",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { onEnterFolder(folder.name) }
            )
        }
        items(tracks, key = { "t:${it.id}" }) { track ->
            val title = track.title ?: track.filename
            val tn = track.trackNumber?.let { "%02d. ".format(it) }.orEmpty()
            Text(
                text = "$tn$title",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
    }
}
