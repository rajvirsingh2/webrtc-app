package com.example.webrtcapp

import android.app.Application
import io.getstream.log.android.AndroidStreamLogger

class WebRTCApp: Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidStreamLogger.installOnDebuggableApp(this)
    }
}