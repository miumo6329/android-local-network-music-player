package com.example.android_local_network_music_player.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ListItem
import androidx.tv.material3.Text

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MusicListItem(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ListItem(
        selected = false,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        headlineContent = {
            Text(
                title,
                modifier = if (subtitle == null) Modifier.padding(vertical = 10.dp) else Modifier
            )
        },
        supportingContent = subtitle?.let { sub -> { Text(sub) } }
    )
}
