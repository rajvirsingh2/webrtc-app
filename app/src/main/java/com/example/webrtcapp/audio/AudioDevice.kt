package com.example.webrtcapp.audio

sealed class AudioDevice {
    abstract val name: String
    @ConsistentCopyVisibility
    data class BluetoothHeadset internal constructor(override val name: String = "Bluetooth"):
            AudioDevice()

    @ConsistentCopyVisibility
    data class WiredHeadset internal constructor(override val name: String = "Wired Headset") :
        AudioDevice()

    @ConsistentCopyVisibility
    data class Earpiece internal constructor(override val name: String = "Earpiece") : AudioDevice()

    @ConsistentCopyVisibility
    data class Speakerphone internal constructor(override val name: String = "Speakerphone") :
        AudioDevice()
}