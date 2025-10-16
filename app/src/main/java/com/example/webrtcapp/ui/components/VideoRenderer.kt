package com.example.webrtcapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.webrtcapp.sessions.LocalWebRtcSessionManager
import org.webrtc.RendererCommon
import org.webrtc.VideoTrack

@Composable
fun VideoRenderer(
    videoTrack: VideoTrack,
    modifier: Modifier
){
    val trackState: MutableState<VideoTrack?> = remember { mutableStateOf(null) }
    var view: VideoTextureViewRenderer? by remember { mutableStateOf(null) }

    DisposableEffect(videoTrack) {
        onDispose {
            cleanTrack(view, trackState)
        }
    }

    val sessionManager = LocalWebRtcSessionManager.current
    AndroidView(
        factory = {
            VideoTextureViewRenderer(it).apply {
                init(
                    sessionManager.peerConnectionFactory.eglBaseContext,
                    object: RendererCommon.RendererEvents{
                        override fun onFirstFrameRendered() = Unit
                        override fun onFrameResolutionChanged(p0: Int, p1: Int, p2: Int) = Unit
                    }
                )
                setUpVideo(trackState, videoTrack, this)
                view = this
            }
        },
        update = {setUpVideo(trackState, videoTrack, it)},
        modifier = modifier
    )
}

fun cleanTrack(
    view: VideoTextureViewRenderer?,
    trackState: MutableState<VideoTrack?>
){
    view?.let {
        trackState.value?.removeSink(it)
    }
    trackState.value = null
}

fun setUpVideo(
    trackState: MutableState<VideoTrack?>,
    videoTrack: VideoTrack,
    renderer: VideoTextureViewRenderer?
){
    if(trackState.value == videoTrack){
        return
    }
    cleanTrack(renderer, trackState)
    trackState.value = videoTrack
    videoTrack.addSink(renderer)
}