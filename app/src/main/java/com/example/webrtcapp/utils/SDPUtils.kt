package com.example.webrtcapp.utils

import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend inline fun createValue(crossinline call: (SdpObserver)-> Unit): Result<SessionDescription> =
    suspendCoroutine {
        val observer = object: SdpObserver{
            override fun onCreateSuccess(p0: SessionDescription?) {
                if(p0!=null){
                    it.resume(Result.success(p0))
                }else{
                    it.resume(Result.failure(Exception("Session Description is null")))
                }
            }
            override fun onSetSuccess()= Unit
            override fun onCreateFailure(p0: String?)=
                it.resume(Result.failure(RuntimeException(p0)))
            override fun onSetFailure(p0: String?)= Unit
        }
        call(observer)
    }

suspend inline fun setValue(
    crossinline call: (SdpObserver) -> Unit
): Result<Unit> = suspendCoroutine {
    val observer = object : SdpObserver {
        override fun onCreateFailure(p0: String?) = Unit
        override fun onCreateSuccess(p0: SessionDescription?) = Unit
        override fun onSetSuccess() = it.resume(Result.success(Unit))
        override fun onSetFailure(message: String?) =
            it.resume(Result.failure(RuntimeException(message)))
    }

    call(observer)
}
