package com.example.android_local_network_music_player.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

/** 右ペイン — フェーズ3はプレースホルダ。フェーズ4で再生コントロールに置き換える。 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlaybackPane(
    /** BrowsingScreen から渡す FocusRequester。先頭コントロール (現在はプレースホルダ Button) に付ける。 */
    firstControlFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "再生パネル",
            style = MaterialTheme.typography.headlineMedium
        )
        val btnModifier = Modifier.padding(top = 16.dp).let { base ->
            if (firstControlFocusRequester != null) base.focusRequester(firstControlFocusRequester) else base
        }
        Button(
            onClick = {},
            modifier = btnModifier
        ) {
            Text("フェーズ4で実装")
        }
    }
}
