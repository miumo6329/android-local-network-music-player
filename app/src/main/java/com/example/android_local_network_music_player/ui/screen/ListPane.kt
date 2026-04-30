package com.example.android_local_network_music_player.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.android_local_network_music_player.data.api.dto.FolderEntry
import com.example.android_local_network_music_player.data.api.dto.FoldersResponse
import com.example.android_local_network_music_player.data.api.dto.Track
import com.example.android_local_network_music_player.ui.NavigationState
import com.example.android_local_network_music_player.ui.component.MusicListItem

// ─── アーティストグループ分け ───────────────────────────────────────────────────

/**
 * アーティスト名からグループキー ("A"～"Z" / "あ" / "1") を返す。
 * "The "/"the " で始まる名前は先頭4文字を除いた後の文字で判定する。
 */
private fun artistGroupKey(name: String): String {
    val effective = if (name.startsWith("the ", ignoreCase = true)) name.drop(4).trimStart() else name
    val first = effective.firstOrNull() ?: return "1"
    return when {
        first in 'A'..'Z' || first in 'a'..'z' -> first.uppercaseChar().toString()
        isJapaneseChar(first) -> "あ"
        else -> "1"
    }
}

/**
 * グループ内ソート用キー。
 * "The "/"the " で始まる名前は除去後の文字列で比較し、
 * "The Beatles" が B グループ内で "Beatles" 相当の位置に来るようにする。
 */
private fun artistSortKey(name: String): String =
    (if (name.startsWith("the ", ignoreCase = true)) name.drop(4).trimStart() else name)
        .lowercase()

private fun isJapaneseChar(c: Char): Boolean {
    val block = Character.UnicodeBlock.of(c)
    return block == Character.UnicodeBlock.HIRAGANA ||
        block == Character.UnicodeBlock.KATAKANA ||
        block == Character.UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS ||
        block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
        block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ||
        block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B ||
        block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
}

/** グループの表示順: A–Z → あ → 1 */
private fun groupSortRank(label: String): Int = when {
    label.length == 1 && label[0] in 'A'..'Z' -> label[0].code // 65–90
    label == "あ" -> 200
    else -> 300 // "1"
}

// ─── ListPane ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ListPane(
    nav: NavigationState,
    onEnterFolder: (String) -> Unit,
    onRetry: () -> Unit,
    onPlayTrack: (tracks: List<Track>, startIndex: Int) -> Unit,
    listFocusRequester: FocusRequester? = null,
    listState: LazyListState = rememberLazyListState(),
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
                    onPlayTrack = onPlayTrack,
                    listFocusRequester = listFocusRequester,
                    listState = listState
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
    onPlayTrack: (tracks: List<Track>, startIndex: Int) -> Unit,
    listFocusRequester: FocusRequester?,
    listState: LazyListState
) {
    if (depth == 1 && current.folders.isNotEmpty()) {
        GroupedArtistList(
            folders = current.folders,
            onEnterFolder = onEnterFolder,
            listFocusRequester = listFocusRequester,
            listState = listState
        )
    } else {
        val listModifier = Modifier.fillMaxSize().let { base ->
            if (listFocusRequester != null) base.focusRequester(listFocusRequester) else base
        }
        LazyColumn(state = listState, modifier = listModifier) {
            items(current.folders, key = { "f:${it.name}" }) { folder ->
                FolderItem(folder = folder, depth = depth, onEnterFolder = onEnterFolder)
            }
            itemsIndexed(current.tracks, key = { _, t -> "t:${t.id}" }) { index, track ->
                TrackItem(track = track, onClick = { onPlayTrack(current.tracks, index) })
            }
        }
    }
}

// ─── 折り畳みアーティスト一覧 ────────────────────────────────────────────────

@Composable
private fun GroupedArtistList(
    folders: List<FolderEntry>,
    onEnterFolder: (String) -> Unit,
    listFocusRequester: FocusRequester?,
    listState: LazyListState
) {
    // シングル展開モデル: 同時に開けるグループは1つのみ
    var expandedGroup by remember { mutableStateOf<String?>(null) }
    // 戻るボタン後にフォーカスを戻すための遅延ターゲット
    var pendingFocusLabel by remember { mutableStateOf<String?>(null) }

    val groups: List<Pair<String, List<FolderEntry>>> = remember(folders) {
        folders
            .groupBy { artistGroupKey(it.name) }
            .entries
            .sortedBy { groupSortRank(it.key) }
            .map { (label, artists) -> label to artists.sortedBy { artistSortKey(it.name) } }
    }

    val headerFocusRequesters: Map<String, FocusRequester> = remember(groups) {
        groups.associate { (label, _) -> label to FocusRequester() }
    }

    // グループ展開中に戻るボタンを押すと折り畳み、ヘッダーにフォーカスを戻す
    BackHandler(enabled = expandedGroup != null) {
        pendingFocusLabel = expandedGroup
        expandedGroup = null
    }

    // 折り畳み後に対象ヘッダーへスクロール & フォーカス
    LaunchedEffect(pendingFocusLabel) {
        val label = pendingFocusLabel ?: return@LaunchedEffect
        // 折り畳み後は全グループが閉じているため、ヘッダーのフラットインデックス = groups 内の位置
        val headerIndex = groups.indexOfFirst { (k, _) -> k == label }
        if (headerIndex >= 0) {
            listState.scrollToItem(headerIndex)
            headerFocusRequesters[label]?.requestFocus()
        }
        pendingFocusLabel = null
    }

    val listModifier = Modifier.fillMaxSize().let { base ->
        if (listFocusRequester != null) base.focusRequester(listFocusRequester) else base
    }

    LazyColumn(state = listState, modifier = listModifier) {
        groups.forEach { (groupLabel, artists) ->
            val isExpanded = groupLabel == expandedGroup

            item(key = "header:$groupLabel") {
                ArtistGroupHeader(
                    label = groupLabel,
                    count = artists.size,
                    isExpanded = isExpanded,
                    focusRequester = headerFocusRequesters[groupLabel] ?: FocusRequester(),
                    onClick = {
                        expandedGroup = if (isExpanded) null else groupLabel
                    }
                )
            }

            if (isExpanded) {
                items(artists, key = { "f:${it.name}" }) { folder ->
                    MusicListItem(
                        title = folder.name,
                        subtitle = "アルバム ${folder.folderCount}  /  トラック ${folder.trackCount}",
                        modifier = Modifier.padding(start = 24.dp),
                        onClick = { onEnterFolder(folder.name) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ArtistGroupHeader(
    label: String,
    count: Int,
    isExpanded: Boolean,
    focusRequester: FocusRequester,
    onClick: () -> Unit
) {
    ListItem(
        selected = false,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
        leadingContent = {
            Text(
                text = if (isExpanded) "▼" else "▶",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        headlineContent = {
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge
            )
        },
        supportingContent = {
            Text(text = "${count}件")
        }
    )
}

// ─── アルバム・トラック用アイテム ──────────────────────────────────────────────

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
private fun TrackItem(track: Track, onClick: () -> Unit) {
    val tn = track.trackNumber?.let { "%02d. ".format(it) }.orEmpty()
    val title = track.title ?: track.filename
    val artistAlbum = listOfNotNull(track.artist, track.album).joinToString(" — ")
    MusicListItem(
        title = "$tn$title",
        subtitle = artistAlbum.ifEmpty { null },
        onClick = onClick
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
