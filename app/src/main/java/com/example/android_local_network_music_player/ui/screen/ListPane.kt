package com.example.android_local_network_music_player.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.android_local_network_music_player.data.api.dto.FolderEntry
import com.example.android_local_network_music_player.data.api.dto.FoldersResponse
import com.example.android_local_network_music_player.data.api.dto.Track
import com.example.android_local_network_music_player.ui.NavigationState
import com.example.android_local_network_music_player.ui.component.MusicListItem

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ListPane(
    nav: NavigationState,
    onEnterFolder: (String) -> Unit,
    onRetry: () -> Unit,
    /** BrowsingScreen から渡す FocusRequester。TvLazyColumn に付けてペイン切り替え時の焦点先にする。 */
    listFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        SectionHeader(nav = nav)
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                nav.isLoading -> Text(
                    text = "読み込み中...",
                    modifier = Modifier.align(Alignment.Center)
                )
                nav.error != null -> ErrorContent(
                    message = nav.error,
                    onRetry = onRetry,
                    modifier = Modifier.align(Alignment.Center)
                )
                nav.current != null -> FolderContent(
                    current = nav.current,
                    depth = nav.depth,
                    onEnterFolder = onEnterFolder,
                    listFocusRequester = listFocusRequester
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SectionHeader(nav: NavigationState) {
    val title = when (nav.depth) {
        1 -> "アーティスト"
        2 -> nav.stack[1].label
        else -> "${nav.stack[1].label}  /  ${nav.stack.last().label}"
    }
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun FolderContent(
    current: FoldersResponse,
    depth: Int,
    onEnterFolder: (String) -> Unit,
    listFocusRequester: FocusRequester?
) {
    val listModifier = Modifier.fillMaxSize().let { base ->
        if (listFocusRequester != null) base.focusRequester(listFocusRequester) else base
    }
    LazyColumn(modifier = listModifier) {
        items(current.folders, key = { "f:${it.name}" }) { folder ->
            FolderItem(folder = folder, depth = depth, onEnterFolder = onEnterFolder)
        }
        items(current.tracks, key = { "t:${it.id}" }) { track ->
            TrackItem(track = track)
        }
    }
}

@Composable
private fun FolderItem(
    folder: FolderEntry,
    depth: Int,
    onEnterFolder: (String) -> Unit
) {
    val subtitle = when (depth) {
        1 -> "アルバム ${folder.folderCount}  /  トラック ${folder.trackCount}"
        else -> "トラック ${folder.trackCount}"
    }
    MusicListItem(
        title = folder.name,
        subtitle = subtitle,
        onClick = { onEnterFolder(folder.name) }
    )
}

@Composable
private fun TrackItem(track: Track) {
    val tn = track.trackNumber?.let { "%02d. ".format(it) }.orEmpty()
    val title = track.title ?: track.filename
    val artistAlbum = listOfNotNull(track.artist, track.album).joinToString(" — ")
    MusicListItem(
        title = "$tn$title",
        subtitle = artistAlbum.ifEmpty { null },
        onClick = { /* フェーズ4: 再生キューセット & 再生開始 */ }
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "エラー: $message")
        Button(onClick = onRetry) {
            Text("再試行")
        }
    }
}
