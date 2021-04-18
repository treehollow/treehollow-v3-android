package com.github.treehollow.ui.settings

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.treehollow.base.Event
import com.github.treehollow.base.TreeHollowApplication
import com.github.treehollow.data.DeviceState
import com.github.treehollow.data.model.TreeHollowPreferences
import com.github.treehollow.network.ApiService
import com.skydoves.sandwich.coroutines.CoroutinesResponseCallAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.internal.notifyAll
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class SettingsViewModel : ViewModel() {

    // global variables
    var token: String = TreeHollowApplication.token!!
    val contactEmail = TreeHollowApplication.Companion.Config.getConfigItemString("contact_email")!!

    // info
    private lateinit var currentUuid: String
    lateinit var deviceListAdapter: DeviceRecyclerViewAdapter
    var userPrefs = MutableLiveData<TreeHollowPreferences>()
    var gonnaQuit = MutableLiveData(false)
    val pushSystemMsg = MutableLiveData<Boolean>()
    val pushReplyMe = MutableLiveData<Boolean>()
    val pushFavorited = MutableLiveData<Boolean>()
    var showNotification = MutableLiveData(false)

    // UI
    private var refreshingJob: Job? = null
    val isRefreshing: MutableLiveData<Boolean> = MutableLiveData(false)
    val infoMsg = MutableLiveData<Event<String>>()
    val errorMsg = MutableLiveData<Event<String>>()

    // network
    private val apiRootUrls =
        TreeHollowApplication.Companion.Config.getConfigItemStringList("api_root_urls")!!
    private val retrofit = Retrofit.Builder()
        .baseUrl(apiRootUrls[0])
        .addConverterFactory(MoshiConverterFactory.create())
        .addCallAdapterFactory(CoroutinesResponseCallAdapterFactory())
        .build()
    private val service = retrofit.create(ApiService::class.java)

    fun getDevices() {
        refreshingJob?.cancel(CancellationException())
        refreshingJob = viewModelScope.launch {
            isRefreshing.value = true
            try {
                val response = service.devicesList(token)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        if (body.code < 0) {
                            throw Exception(body.msg)
                        }
                        if (body.this_device != null) {
                            currentUuid = body.this_device
                        }
                        val tot = mutableListOf<DeviceState>()
                        for (ent in body.data!!) {
                            val tmp = DeviceState(ent)
                            if (ent.device_uuid == currentUuid)
                                tmp.isThisDevice = true
                            tot.add(tmp)
                        }
                        deviceListAdapter.list.value = tot
                        deviceListAdapter.notifyDataSetChanged()
                        isRefreshing.value = false
                    } else {
                        throw Exception("Network response body is null")
                    }
                } else {
                    throw Exception("Network error ${response.code()}: ${response.message()}")
                }
            } catch (e: Exception) {
                errorMsg.postValue(Event(e.toString()))
                isRefreshing.value = false
            }
        }
    }

    fun quitDevice(uuid: String) {
        refreshingJob?.cancel(CancellationException())
        refreshingJob = viewModelScope.launch {
            isRefreshing.value = true
            try {
                val response = service.terminateDevice(token, uuid)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        if (body.code < 0) {
                            throw Exception(body.msg)
                        }
                        if (uuid == currentUuid) {
                            TreeHollowApplication.deleteToken()
//                            TODO: delete cache
                            synchronized(gonnaQuit) {
                                gonnaQuit.value = true
                                gonnaQuit.notifyAll()
                            }
                        } else {
                            getDevices()
                        }
                    } else {
                        throw Exception("Network response body is null")
                    }
                } else {
                    throw Exception("Network error ${response.code()}: ${response.message()}")
                }
            } catch (e: Exception) {
                errorMsg.postValue(Event(e.toString()))
                isRefreshing.value = false
            }
        }
    }

    fun quitCurrentDevice() {
        viewModelScope.launch {
            try {
                val response = service.logoutCurrentDevice(token)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        if (body.code < 0 && body.code != -100) {
                            throw Exception(body.msg)
                        }
                        TreeHollowApplication.deleteToken()
                        synchronized(gonnaQuit) {
                            gonnaQuit.value = true
                            gonnaQuit.notifyAll()
                        }

                    } else {
                        throw Exception("Network response body is null")
                    }
                } else {
                    throw Exception("Network error ${response.code()}: ${response.message()}")
                }
            } catch (e: Exception) {
                errorMsg.postValue(Event(e.toString()))
                Log.d("quit", e.toString())
            }
        }
    }

    fun changePushType() {

        Log.d(
            "SettingsViewModel", "changePushType: " +
                    "pushSystemMsg:${pushSystemMsg.value!!}, " +
                    "pushReplyMe:${pushReplyMe.value!!}, " +
                    "pushFavorited:${pushFavorited.value!!}"
        )

        viewModelScope.launch {
            try {
                val response = service.setPush(
                    TreeHollowApplication.token!!,
                    if (pushSystemMsg.value!!) 1 else 0,
                    if (pushReplyMe.value!!) 1 else 0,
                    if (pushFavorited.value!!) 1 else 0
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        if (body.code < 0)
                            throw Exception("Error: ${body.msg}")
                        else {
                            infoMsg.postValue(Event("设置成功"))
                        }
                    } else {
                        throw Exception("Network response body is null")
                    }
                } else {
                    throw Exception("Network error ${response.code()}: ${response.message()}")
                }

            } catch (e: Exception) {
                errorMsg.postValue(Event(e.toString()))
            }
        }
    }

    fun getPushInfo() = viewModelScope.launch {
        try {
            val response = service.getPush(TreeHollowApplication.token!!)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    if (body.code < 0)
                        throw Exception("Error: ${body.msg}")
                    else {
                        Log.d(
                            "SettingsViewModel", "pushSystemMsg: ${body.data.push_system_msg}, " +
                                    "pushFavorited: ${body.data.push_favorited}," +
                                    "pushReplyMe: ${body.data.push_reply_me}"
                        )
                        pushSystemMsg.value = (body.data.push_system_msg == 1)
                        pushFavorited.value = (body.data.push_favorited == 1)
                        pushReplyMe.value = (body.data.push_reply_me == 1)
                        delay(100)
                        showNotification.postValue(true)
                        infoMsg.postValue(Event("获取成功"))
                    }
                } else {
                    throw Exception("Network response body is null")
                }
            } else {
                throw Exception("Network error ${response.code()}: ${response.message()}")
            }

        } catch (e: Exception) {
            Log.d("SettingsViewModel", "$e")
            errorMsg.postValue(Event(e.toString()))
        }
    }
}