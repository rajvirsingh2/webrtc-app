package com.example.webrtcapp.ui.screens.stage

import android.widget.Button
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.webrtcapp.R
import com.example.webrtcapp.WebRTCSessionState

@Composable
fun StageScreen(
    state: WebRTCSessionState,
    onJoinCall:()-> Unit
){
    Box(modifier = Modifier.fillMaxSize()){
        var enabledCall by remember { mutableStateOf(false) }
        val text = when(state){
            WebRTCSessionState.Ready -> {
                enabledCall = true
                stringResource(R.string.session_ready)
            }
            WebRTCSessionState.Offline -> {
                enabledCall = false
                stringResource(R.string.session_offline)
            }
            WebRTCSessionState.Active -> {
                enabledCall = false
                stringResource(R.string.session_active)
            }
            WebRTCSessionState.Creating -> {
                enabledCall = true
                stringResource(R.string.session_creating)
            }
            WebRTCSessionState.Impossible -> {
                enabledCall = false
                stringResource(R.string.session_impossible)
            }
        }

        Button(
            modifier = Modifier.align(Alignment.Center),
            enabled = enabledCall,
            onClick = {onJoinCall.invoke()}
        ){
            Text(
                text = text,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}