package com.example.android_local_network_music_player.ui.screen

import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.android_local_network_music_player.data.api.dto.Track
import com.example.android_local_network_music_player.ui.NavigationState
import com.example.android_local_network_music_player.ui.PlaybackUiState

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun BrowsingScreen(
    nav: NavigationState,
    playbackState: PlaybackUiState,
    onEnterFolder: (String) -> Unit,
    onGoBack: () -> Boolean,
    onRetry: () -> Unit,
    onExit: () -> Unit,
    onPlayTrack: (tracks: List<Track>, startIndex: Int) -> Unit,
    onTogglePlay: () -> Unit,
    onNextTrack: () -> Unit,
    onPrevTrack: () -> Unit,
    onSeekTo: (Long) -> Unit
) {
    var showExitDialog by remember { mutableStateOf(false) }
    val leftFocusRequester = remember { FocusRequester() }
    val rightFocusRequester = remember { FocusRequester() }

    BackHandler {
        if (!onGoBack()) {
            showExitDialog = true
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(0.38f)
                .fillMaxSize()
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown &&
                        event.key.nativeKeyCode == AndroidKeyEvent.KEYCODE_DPAD_RIGHT
                    ) {
                        rightFocusRequester.requestFocus()
                        true
                    } else {
                        false
                    }
                }
        ) {
            ListPane(
                nav = nav,
                onEnterFolder = onEnterFolder,
                onRetry = onRetry,
                onPlayTrack = onPlayTrack,
                listFocusRequester = leftFocusRequester
            )
        }

        Box(
            modifier = Modifier
                .weight(0.62f)
                .fillMaxSize()
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown &&
                        event.key.nativeKeyCode == AndroidKeyEvent.KEYCODE_DPAD_LEFT
                    ) {
                        leftFocusRequester.requestFocus()
                        true
                    } else {
                        false
                    }
                }
        ) {
            PlaybackPane(
                playbackState = playbackState,
                onTogglePlay = onTogglePlay,
                onNextTrack = onNextTrack,
                onPrevTrack = onPrevTrack,
                onSeekTo = onSeekTo,
                firstControlFocusRequester = rightFocusRequester
            )
        }
    }

    if (showExitDialog) {
        ExitConfirmDialog(
            onConfirm = onExit,
            onDismiss = { showExitDialog = false }
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ExitConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(32.dp)
                .fillMaxWidth(0.5f)
        ) {
            Text(
                text = "アプリを終了しますか?",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onDismiss) {
                    Text("キャンセル")
                }
                Button(onClick = onConfirm) {
                    Text("終了")
                }
            }
        }
    }
}
