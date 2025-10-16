package com.example.webrtcapp.sessions

import com.example.webrtcapp.SignalingClient
import com.example.webrtcapp.peer.StreamPeerConnectionFactory
import kotlinx.coroutines.flow.SharedFlow
import org.webrtc.VideoTrack

interface WebRtcSessionManager {
    val signalClient: SignalingClient
    val peerConnectionFactory: StreamPeerConnectionFactory
    val localVideoTrackFlow: SharedFlow<VideoTrack>
    val remoteVideoTrackFlow: SharedFlow<VideoTrack>

    fun onSessionScreenReady()
    fun flipCamera()
    fun enableMicrophone(enabled: Boolean)
    fun enableCamera(enabled: Boolean)
    fun disconnect()
}