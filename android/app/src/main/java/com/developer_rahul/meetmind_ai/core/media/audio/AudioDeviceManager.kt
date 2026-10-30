package com.developer_rahul.meetmind_ai.core.media.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build

object AudioDeviceManager {

    fun getAvailableAudioDevices(context: Context): List<String> {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val devices = mutableListOf("Speaker", "Earpiece")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            var hasBluetooth = false
            var hasHeadset = false
            for (device in audioDevices) {
                when (device.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_BLE_HEADSET -> hasBluetooth = true

                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                    AudioDeviceInfo.TYPE_USB_HEADSET -> hasHeadset = true
                }
            }
            if (hasHeadset && !devices.contains("Headset")) {
                devices.add(0, "Headset")
            }
            if (hasBluetooth && !devices.contains("Bluetooth")) {
                devices.add(0, "Bluetooth")
            }
        }
        return devices
    }

    fun selectAudioDevice(context: Context, deviceName: String) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val commDevices = audioManager.availableCommunicationDevices
            val targetType = when (deviceName) {
                "Speaker" -> AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                "Earpiece" -> AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
                "Bluetooth" -> AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                "Headset" -> AudioDeviceInfo.TYPE_WIRED_HEADSET
                else -> AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            }

            val matchingDevice = commDevices.find { it.type == targetType }
                ?: (if (deviceName == "Bluetooth") commDevices.find { it.type == AudioDeviceInfo.TYPE_BLE_HEADSET } else null)
                ?: (if (deviceName == "Headset") commDevices.find { it.type == AudioDeviceInfo.TYPE_USB_HEADSET || it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES } else null)

            if (matchingDevice != null) {
                val success = audioManager.setCommunicationDevice(matchingDevice)
                if (success) {
                    return
                }
            }
        }

        // Fallback for API < 31 or if setCommunicationDevice returns false
        when (deviceName) {
            "Speaker" -> {
                audioManager.isSpeakerphoneOn = true
                if (audioManager.isBluetoothScoOn) {
                    audioManager.stopBluetoothSco()
                    audioManager.isBluetoothScoOn = false
                }
            }
            "Earpiece" -> {
                audioManager.isSpeakerphoneOn = false
                if (audioManager.isBluetoothScoOn) {
                    audioManager.stopBluetoothSco()
                    audioManager.isBluetoothScoOn = false
                }
            }
            "Bluetooth" -> {
                audioManager.isSpeakerphoneOn = false
                audioManager.startBluetoothSco()
                audioManager.isBluetoothScoOn = true
            }
            "Headset" -> {
                audioManager.isSpeakerphoneOn = false
                if (audioManager.isBluetoothScoOn) {
                    audioManager.stopBluetoothSco()
                    audioManager.isBluetoothScoOn = false
                }
            }
        }
    }

    fun resetToNormalMode(context: Context) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager.clearCommunicationDevice()
            }
            if (audioManager.isBluetoothScoOn) {
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
            }
            audioManager.isSpeakerphoneOn = false
            audioManager.mode = AudioManager.MODE_NORMAL
        } catch (e: Exception) {
            // Ignored
        }
    }
}
