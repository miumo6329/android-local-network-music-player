package com.example.android_local_network_music_player.ui.screen

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.android_local_network_music_player.ui.PlaybackUiState

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlaybackPane(
    playbackState: PlaybackUiState,
    onTogglePlay: () -> Unit,
    onNextTrack: () -> Unit,
    onPrevTrack: () -> Unit,
    onSeekTo: (Long) -> Unit,
    firstControlFocusRequester: FocusRequester? = null,
    onNavigateToList: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val track = playbackState.track

        // アートワーク
        Box(
            modifier = Modifier
                .fillMaxWidth(0.65f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            if (playbackState.artworkUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(playbackState.artworkUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // 曲情報
        if (track != null) {
            Text(
                text = track.title ?: track.filename,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val infoLine = listOfNotNull(track.artist, track.album).joinToString(" — ")
            if (infoLine.isNotEmpty()) {
                Text(
                    text = infoLine,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Text(
                text = "曲を選択してください",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
        }

        Spacer(Modifier.height(20.dp))

        // コントロールボタン
        val playBtnModifier = Modifier.let { base ->
            if (firstControlFocusRequester != null) base.focusRequester(firstControlFocusRequester) else base
        }
        val prevBtnModifier = if (onNavigateToList != null) {
            Modifier.onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    event.key.nativeKeyCode == AndroidKeyEvent.KEYCODE_DPAD_LEFT
                ) {
                    onNavigateToList()
                    true
                } else false
            }
        } else Modifier
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onPrevTrack, enabled = track != null, modifier = prevBtnModifier) {
                Text("|◀")
            }
            Button(
                onClick = onTogglePlay,
                enabled = track != null,
                modifier = playBtnModifier
            ) {
                Text(if (playbackState.isPlaying) "||" else "▶")
            }
            Button(onClick = onNextTrack, enabled = track != null) {
                Text("▶|")
            }
        }

        Spacer(Modifier.height(12.dp))

        // シークバー
        SeekBar(
            positionMs = playbackState.positionMs,
            durationMs = playbackState.durationMs,
            onSeekTo = onSeekTo,
            modifier = Modifier.fillMaxWidth(0.9f)
        )

        Spacer(Modifier.height(4.dp))

        // 経過時間 / 総時間
        Row(
            modifier = Modifier.fillMaxWidth(0.9f),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatMs(playbackState.positionMs),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = formatMs(playbackState.durationMs),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SeekBar(
    positionMs: Long,
    durationMs: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val fraction = remember(positionMs, durationMs) {
        if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    }
    val trackColor = if (isFocused) Color.White.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.25f)
    val progressColor = if (isFocused) Color.Yellow else Color.White

    Box(
        modifier = modifier
            .height(36.dp)
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key.nativeKeyCode) {
                        AndroidKeyEvent.KEYCODE_DPAD_LEFT -> {
                            onSeekTo((positionMs - 10_000L).coerceAtLeast(0L))
                            true
                        }
                        AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> {
                            onSeekTo((positionMs + 10_000L).coerceAtMost(durationMs.coerceAtLeast(0L)))
                            true
                        }
                        else -> false
                    }
                } else false
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(trackColor)
        )
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(4.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(2.dp))
                    .background(progressColor)
            )
        }
    }
}

private fun formatMs(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0L)
    return "%d:%02d".format(s / 60, s % 60)
}
