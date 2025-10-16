package com.example.webrtcapp.audio

import android.bluetooth.BluetoothHeadset
import android.content.Context
import android.media.AudioManager

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
            audioDevices.contains(selectedDevice)
        } ?: false

    private fun closeListeners() {
        audioDeviceChangeListener = null
        state = State.STOPPED
    }

    fun start(listener: AudioDeviceChangeListener) {
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
        when (state) {
            State.STARTED -> {
                audioManager.cacheAudioState()

                // Always set mute to false for WebRTC
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
        when (state) {
            State.ACTIVATED -> {
                // Restore stored audio state
                audioManager.restoreAudioState()
                state = State.STARTED
            }
            State.STARTED, State.STOPPED -> {
            }
        }
    }

    private fun selectDevice(audioDevice: AudioDevice?) {
        if (selectedDevice != audioDevice) {
            userSelectedDevice = audioDevice
            enumerateDevices()
        }
    }
    private fun activate(audioDevice: AudioDevice) {
        when (audioDevice) {
            is BluetoothHeadset -> audioManager.enableSpeakerphone(false)
            is AudioDevice.Earpiece, is AudioDevice.WiredHeadset -> audioManager.enableSpeakerphone(false)
            is AudioDevice.Speakerphone -> audioManager.enableSpeakerphone(true)
            is AudioDevice.BluetoothHeadset -> TODO()
        }
    }

    private fun enumerateDevices(bluetoothHeadsetName: String? = null) {
        // save off the old state and 'semi'-deep copy the list of audio devices
        val oldAudioDeviceState = AudioDeviceState(mutableAudioDevices.map { it }, selectedDevice)
        // update audio device list and selected device
        addAvailableAudioDevices(bluetoothHeadsetName)

        if (!userSelectedDevicePresent(mutableAudioDevices)) {
            userSelectedDevice = null
        }

        // Select the audio device
        selectedDevice = if (userSelectedDevice != null) {
            userSelectedDevice
        } else if (mutableAudioDevices.size > 0) {
            mutableAudioDevices.first()
        } else {
            null
        }

        // Activate the device if in the active state
        if (state == State.ACTIVATED) {
            activate()
        }
        // trigger audio device change listener if there has been a change
        val newAudioDeviceState = AudioDeviceState(mutableAudioDevices, selectedDevice)
        if (newAudioDeviceState != oldAudioDeviceState) {
            audioDeviceChangeListener?.invoke(mutableAudioDevices, selectedDevice)
        }
    }

    private fun addAvailableAudioDevices(bluetoothHeadsetName: String?) {
        mutableAudioDevices.clear()
        preferredDeviceList.forEach { audioDevice ->
            when (audioDevice) {
                BluetoothHeadset::class.java -> {
                }
                AudioDevice.WiredHeadset::class.java -> {
                    if (wiredHeadsetAvailable) {
                        mutableAudioDevices.add(AudioDevice.WiredHeadset())
                    }
                }
                AudioDevice.Earpiece::class.java -> {
                    val hasEarpiece = audioManager.hasEarpiece()
                    if (hasEarpiece && !wiredHeadsetAvailable) {
                        mutableAudioDevices.add(AudioDevice.Earpiece())
                    }
                }
                AudioDevice.Speakerphone::class.java -> {
                    val hasSpeakerphone = audioManager.hasSpeakerphone()
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
