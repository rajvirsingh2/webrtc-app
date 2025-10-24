package com.example.webrtcapp.audio

import android.bluetooth.BluetoothHeadset
import android.content.Context
import android.media.AudioManager
import io.getstream.log.taggedLogger

@Suppress("CAST_NEVER_SUCCEEDS")
class AudioSwitch internal constructor(
    context: Context,
    audioFocusChangeListener: AudioManager.OnAudioFocusChangeListener,
    preferredDeviceList: List<Class<out AudioDevice>>,
    private val audioManager: AudioManagerAdapter = AudioManagerAdapterImpl(
        context,
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager,
        audioFocusChangeListener = audioFocusChangeListener
    )
) {
    private val logger by taggedLogger("Call:AudioSwitch")
    private var audioDeviceChangeListener: AudioDeviceChangeListener? = null
    private var selectedDevice: AudioDevice? = null
    private var userSelectedDevice: AudioDevice? = null
    private var wiredHeadsetAvailable = false
    private val mutableAudioDevices = ArrayList<AudioDevice>()
    private val preferredDeviceList: List<Class<out AudioDevice>>
    private var state: State = State.STOPPED
    internal enum class State{
        STARTED,
        ACTIVATED,
        STOPPED
    }
    init {
        this.preferredDeviceList = getPreferredDeviceList(preferredDeviceList)
    }
    private fun getPreferredDeviceList(preferredDeviceList: List<Class<out AudioDevice>>):
            List<Class<out AudioDevice>> {
        require(hasNoDuplicates(preferredDeviceList))

        return if (preferredDeviceList.isEmpty() || preferredDeviceList == defaultPreferredDeviceList) {
            defaultPreferredDeviceList
        } else {
            val result = defaultPreferredDeviceList.toMutableList()
            result.removeAll(preferredDeviceList)
            preferredDeviceList.forEachIndexed { index, device ->
                result.add(index, device)
            }
            result
        }
    }

    private fun hasNoDuplicates(list: List<Class<out AudioDevice>>) =
        list.groupingBy { it }.eachCount().filter { it.value > 1 }.isEmpty()

    internal data class AudioDeviceState(
        val audioDeviceList: List<AudioDevice>,
        val selectedAudioDevice: AudioDevice?
    )

    private fun userSelectedDevicePresent(audioDevices: List<AudioDevice>) =
        userSelectedDevice?.let { selectedDevice ->
            if (selectedDevice is BluetoothHeadset) {
                audioDevices.find { it is BluetoothHeadset }?.let { newHeadset ->
                    userSelectedDevice = newHeadset
                    true
                } ?: false
            } else {
                audioDevices.contains(selectedDevice)
            }
        } ?: false

    private fun closeListeners() {
        audioDeviceChangeListener = null
        state = State.STOPPED
    }

    fun start(listener: AudioDeviceChangeListener) {
        logger.d { "[start] state: $state" }
        audioDeviceChangeListener = listener
        when (state) {
            State.STOPPED -> {
                enumerateDevices()
                state = State.STARTED
            }
            else -> {
            }
        }
    }

    fun stop() {
        logger.d { "[stop] state: $state" }
        when (state) {
            State.ACTIVATED -> {
                deactivate()
                closeListeners()
            }
            State.STARTED -> {
                closeListeners()
            }
            State.STOPPED -> {
            }
        }
    }

    fun activate() {
        logger.d { "[activate] state: $state" }
        when (state) {
            State.STARTED -> {
                audioManager.cacheAudioState()
                audioManager.mute(false)
                audioManager.setAudioFocus()
                selectedDevice?.let { activate(it) }
                state = State.ACTIVATED
            }
            State.ACTIVATED -> selectedDevice?.let { activate(it) }
            State.STOPPED -> throw IllegalStateException()
        }
    }

    private fun deactivate() {
        logger.d { "[deactivate] state: $state" }
        when (state) {
            State.ACTIVATED -> {
                audioManager.restoreAudioState()
                state = State.STARTED
            }
            State.STARTED, State.STOPPED -> {
            }
        }
    }

    private fun selectDevice(audioDevice: AudioDevice?) {
        logger.d { "[selectDevice] audioDevice: $audioDevice" }
        if (selectedDevice != audioDevice) {
            userSelectedDevice = audioDevice
            enumerateDevices()
        }
    }
    private fun activate(audioDevice: AudioDevice) {
        logger.d { "[activate] audioDevice: $audioDevice" }
        when (audioDevice) {
            is AudioDevice.Earpiece, is AudioDevice.WiredHeadset -> audioManager.enableSpeakerphone(false)
            is AudioDevice.Speakerphone -> audioManager.enableSpeakerphone(true)
            is AudioDevice.BluetoothHeadset -> audioManager.enableSpeakerphone(false)
        }
    }

    private fun enumerateDevices(bluetoothHeadsetName: String? = null) {
        logger.d { "[enumerateDevices] bluetoothHeadsetName: $bluetoothHeadsetName" }
        val oldAudioDeviceState = AudioDeviceState(mutableAudioDevices.map { it }, selectedDevice)
        addAvailableAudioDevices(bluetoothHeadsetName)

        if (!userSelectedDevicePresent(mutableAudioDevices)) {
            userSelectedDevice = null
        }

        selectedDevice = if (userSelectedDevice != null) {
            userSelectedDevice
        } else if (mutableAudioDevices.size > 0) {
            mutableAudioDevices.first()
        } else {
            null
        }
        logger.v { "[enumerateDevices] selectedDevice: $selectedDevice" }

        if (state == State.ACTIVATED) {
            activate()
        }
        val newAudioDeviceState = AudioDeviceState(mutableAudioDevices, selectedDevice)
        if (newAudioDeviceState != oldAudioDeviceState) {
            audioDeviceChangeListener?.invoke(mutableAudioDevices, selectedDevice)
        }
    }

    private fun addAvailableAudioDevices(bluetoothHeadsetName: String?) {
        logger.d {
            "[addAvailableAudioDevices] wiredHeadsetAvailable: $wiredHeadsetAvailable, " +
                    "bluetoothHeadsetName: $bluetoothHeadsetName"
        }
        mutableAudioDevices.clear()
        preferredDeviceList.forEach { audioDevice ->
            when (audioDevice) {
                BluetoothHeadset::class.java -> {
                }
                AudioDevice.WiredHeadset::class.java -> {
                    logger.v {
                        "[addAvailableAudioDevices] #WiredHeadset; wiredHeadsetAvailable: $wiredHeadsetAvailable"
                    }
                    if (wiredHeadsetAvailable) {
                        mutableAudioDevices.add(AudioDevice.WiredHeadset())
                    }
                }
                AudioDevice.Earpiece::class.java -> {
                    val hasEarpiece = audioManager.hasEarpiece()
                    logger.v {
                        "[addAvailableAudioDevices] #Earpiece; hasEarpiece: $hasEarpiece, " +
                                "wiredHeadsetAvailable: $wiredHeadsetAvailable"
                    }
                    if (hasEarpiece && !wiredHeadsetAvailable) {
                        mutableAudioDevices.add(AudioDevice.Earpiece())
                    }
                }
                AudioDevice.Speakerphone::class.java -> {
                    val hasSpeakerphone = audioManager.hasSpeakerphone()
                    logger.v { "[addAvailableAudioDevices] #Speakerphone; hasSpeakerphone: $hasSpeakerphone" }
                    if (hasSpeakerphone) {
                        mutableAudioDevices.add(AudioDevice.Speakerphone())
                    }
                }
            }
        }
    }

    companion object {
        private val defaultPreferredDeviceList by lazy {
            listOf(
                AudioDevice.BluetoothHeadset::class.java,
                AudioDevice.WiredHeadset::class.java,
                AudioDevice.Earpiece::class.java,
                AudioDevice.Speakerphone::class.java
            )
        }
    }
}
