package com.example.webrtcapp.utils

import org.webrtc.AddIceObserver
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun PeerConnection.addRtcIceCandidate(iceCandidate: IceCandidate): Result<Unit>{
    return suspendCoroutine {
        addIceCandidate(
            iceCandidate,
            object: AddIceObserver{
                override fun onAddSuccess() {
                    it.resume(Result.success(Unit))
                }

                override fun onAddFailure(p0: String?) {
                    it.resume(Result.failure(RuntimeException(p0)))
                }
            }
        )
    }
}