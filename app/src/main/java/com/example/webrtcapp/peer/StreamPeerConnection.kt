package com.example.webrtcapp.peer

import com.example.webrtcapp.utils.addRtcIceCandidate
import com.example.webrtcapp.utils.createValue
import com.example.webrtcapp.utils.setValue
import com.example.webrtcapp.utils.stringify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.webrtc.CandidatePairChangeEvent
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.IceCandidateErrorEvent
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.RTCStatsReport
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SessionDescription

class StreamPeerConnection(
    private val coroutineScope: CoroutineScope,
    private val type: StreamPeerType,
    private val mediaConstraints: MediaConstraints,
    private val onStreamAdded:((MediaStream) -> Unit)?,
    private val onNegotiationNeeded:((StreamPeerConnection, StreamPeerType) -> Unit)?,
    private val onIceCandidate: ((IceCandidate, StreamPeerType) -> Unit)?,
    private val onVideoTrack: ((RtpTransceiver?) -> Unit)?
): PeerConnection.Observer {
    private val typeTag=type.stringify()
    lateinit var connection: PeerConnection
        private set
    private var statsJob: Job?=null
    private val pendingIceMutex = Mutex()
    private val pendingIceCandidates = mutableListOf<IceCandidate>()
    private val statsFlow: MutableStateFlow<RTCStatsReport?> = MutableStateFlow(null)

    fun initialize(peerConnection: PeerConnection){
        this.connection=peerConnection
    }

    suspend fun createOffer(): Result<SessionDescription>{
        return createValue{connection.createOffer(it, mediaConstraints)}
    }

    suspend fun createAnswer(): Result<SessionDescription> {
        return createValue { connection.createAnswer(it, mediaConstraints) }
    }

    suspend fun setRemoteDescription(sessionDescription: SessionDescription): Result<Unit> {
        return setValue {
            connection.setRemoteDescription(
                it,
                SessionDescription(
                    sessionDescription.type,
                    sessionDescription.description.mungeCodecs()
                )
            )
        }.also {
            pendingIceMutex.withLock {
                pendingIceCandidates.forEach { iceCandidate ->
                    connection.addRtcIceCandidate(iceCandidate)
                }
                pendingIceCandidates.clear()
            }
        }
    }

    suspend fun setLocalDescription(sessionDescription: SessionDescription): Result<Unit> {
        val sdp = SessionDescription(
            sessionDescription.type,
            sessionDescription.description.mungeCodecs()
        )
        return setValue { connection.setLocalDescription(it, sdp) }
    }

    suspend fun addIceCandidate(iceCandidate: IceCandidate): Result<Unit> {
        if (connection.remoteDescription == null) {
            pendingIceMutex.withLock {
                pendingIceCandidates.add(iceCandidate)
            }
            return Result.failure(RuntimeException("RemoteDescription is not set"))
        }
        return connection.addRtcIceCandidate(iceCandidate)
    }

    override fun onIceCandidate(candidate: IceCandidate?) {
        if (candidate == null) return

        onIceCandidate?.invoke(candidate, type)
    }

    override fun onAddStream(stream: MediaStream?) {
        if (stream != null) {
            onStreamAdded?.invoke(stream)
        }
    }

    override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
        mediaStreams?.forEach { mediaStream ->
            mediaStream.audioTracks?.forEach { remoteAudioTrack ->
                remoteAudioTrack.setEnabled(true)
            }
            onStreamAdded?.invoke(mediaStream)
        }
    }

    override fun onRenegotiationNeeded() {
        onNegotiationNeeded?.invoke(this, type)
    }

    override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
        when (newState) {
            PeerConnection.IceConnectionState.CLOSED,
            PeerConnection.IceConnectionState.FAILED,
            PeerConnection.IceConnectionState.DISCONNECTED -> statsJob?.cancel()
            PeerConnection.IceConnectionState.CONNECTED -> statsJob = observeStats()
            else -> Unit
        }
    }

    override fun onTrack(transceiver: RtpTransceiver?) {
        onVideoTrack?.invoke(transceiver)
    }

    override fun onRemoveTrack(receiver: RtpReceiver?) {}

    override fun onSignalingChange(newState: PeerConnection.SignalingState?) {}

    override fun onIceConnectionReceivingChange(receiving: Boolean) {}

    override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {}

    override fun onIceCandidateError(event: IceCandidateErrorEvent?) {}

    override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {}

    override fun onSelectedCandidatePairChanged(event: CandidatePairChangeEvent?) {}

    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate?>?) {
        TODO("Not yet implemented")
    }

    override fun onRemoveStream(p0: MediaStream?) {
        TODO("Not yet implemented")
    }

    override fun onDataChannel(channel: DataChannel?): Unit = Unit

    override fun toString(): String =
        "StreamPeerConnection(type='$typeTag', constraints=$mediaConstraints)"

    private fun String.mungeCodecs(): String {
        return this.replace("vp9", "VP9").replace("vp8", "VP8").replace("h264", "H264")
    }

    fun getStats(): StateFlow<RTCStatsReport?> {
        return statsFlow
    }

    private fun observeStats() = coroutineScope.launch {
        while (isActive) {
            delay(10_000L)
            connection.getStats {
                statsFlow.value = it
            }
        }
    }
}