package com.example.webrtcapp.ui.screens.video

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.example.webrtcapp.R
import com.example.webrtcapp.ui.theme.Primary

sealed class CallAction{
    data class ToggleMicrophone(
        val isEnabled: Boolean
    ): CallAction()

    data class ToggleCamera(
        val isEnabled: Boolean
    ): CallAction()

    object FlipCamera: CallAction()
    object LeaveCall: CallAction()
}

data class VideoCallControls(
    val icon: Painter,
    val iconTint: Color,
    val background: Color,
    val callAction: CallAction
)

@Composable
fun buildDefaultCallControlActions(
    callMediaState: CallMediaState
): List<VideoCallControls> {
    val microphoneIcon =
        painterResource(
            id = if (callMediaState.isMicrophoneEnabled) {
                R.drawable.ic_mic_on
            } else {
                R.drawable.ic_mic_off
            }
        )

    val cameraIcon = painterResource(
        id = if (callMediaState.isCameraEnabled) {
            R.drawable.ic_videocam_on
        } else {
            R.drawable.ic_videocam_off
        }
    )

    return listOf(
        VideoCallControls(
            microphoneIcon,
            Color.White,
            Primary,
            CallAction.ToggleMicrophone(callMediaState.isMicrophoneEnabled)
        ),
        VideoCallControls(
            cameraIcon,
            Color.White,
            Primary,
            CallAction.ToggleCamera(callMediaState.isCameraEnabled)
        ),
        VideoCallControls(
            painterResource(R.drawable.ic_camera_flip),
            Color.White,
            Primary,
            CallAction.FlipCamera
        ),
        VideoCallControls(
            painterResource(R.drawable.ic_call_end),
            Color.White,
            Primary,
            CallAction.LeaveCall
        )
    )
}
