package com.example.webrtcapp

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.webrtcapp.peer.StreamPeerConnectionFactory
import com.example.webrtcapp.sessions.LocalWebRtcSessionManager
import com.example.webrtcapp.sessions.WebRtcSessionManager
import com.example.webrtcapp.sessions.WebRtcSessionManagerImpl
import com.example.webrtcapp.ui.screens.stage.StageScreen
import com.example.webrtcapp.ui.screens.video.VideoCallScreen
import com.example.webrtcapp.ui.theme.WebrtcappTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), 0)
        val manager: WebRtcSessionManager = WebRtcSessionManagerImpl(
            this,
            StreamPeerConnectionFactory(this),
            SignalingClient()
        )

        setContent {
            WebrtcappTheme {
                CompositionLocalProvider(LocalWebRtcSessionManager provides manager) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        var onCallScreen by remember { mutableStateOf(false) }
                        val state by manager.signalClient.sessionStateFlow.collectAsState()
                        if(!onCallScreen){
                            StageScreen(state) { onCallScreen=true }
                        }else{
                            VideoCallScreen()
                        }
                    }
                }
            }
        }
    }
}