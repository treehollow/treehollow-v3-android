package com.github.treehollow.data

import android.util.Log
import com.github.treehollow.network.ApiResponse
import com.github.treehollow.ui.settings.SettingsViewModel
import java.io.Serializable

data class DeviceState(
    val data: ApiResponse.DeviceEntry,
) : DeviceListElem(), Serializable {
    var deviceName: String
    var isThisDevice: Boolean = false
    lateinit var model: SettingsViewModel

    private var deviceOS: String? = null

    init {
        val s = data.device_info.split(",")
        deviceName = s[0]
        if (s.size > 1) {
            deviceOS = s[1]
        }
    }

    fun id() = data.device_uuid
    fun os(): String {

        return when (data.device_type) {
            0 -> {
                "Web"
            }
            1 -> {
                "Android"
            }
            2 -> {
                "iOS"
            }
            else -> {
                "UNKNOWN"
            }
        }
    }

    fun quit() {
        Log.d("click", "quit$deviceName")
        model.quitDevice(data.device_uuid)
    }
}

sealed class DeviceListElem : Serializable
