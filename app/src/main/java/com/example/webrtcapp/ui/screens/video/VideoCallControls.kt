package com.example.webrtcapp.ui.screens.video

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun VideoCallControls(
    modifier: Modifier,
    callMediaState: CallMediaState,
    actions: List<VideoCallControls> = buildDefaultCallControlActions(callMediaState),
    onCallAction: (CallAction) -> Unit
){
    LazyRow(
        modifier.padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items(actions){
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(it.background)
            ){
                Icon(
                    modifier = Modifier
                        .padding(10.dp)
                        .align(Alignment.Center)
                        .clickable{onCallAction(it.callAction)},
                    tint = it.iconTint,
                    painter = it.icon,
                    contentDescription = null
                )
            }
        }
    }
}