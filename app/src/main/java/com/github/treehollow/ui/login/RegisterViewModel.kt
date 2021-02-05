package com.github.treehollow.ui.login

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.treehollow.base.Event
import com.github.treehollow.base.TreeHollowApplication
import com.github.treehollow.network.ApiService
import com.github.treehollow.utils.Utils
import com.skydoves.sandwich.coroutines.CoroutinesResponseCallAdapterFactory
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory


class RegisterViewModel : ViewModel() {
    val errorMsg = MutableLiveData<Event<String>>()
    var result: MutableLiveData<String> = MutableLiveData("")
    val isLoading: MutableLiveData<Boolean> = MutableLiveData(false)

    //    https://developer.android.com/kotlin/coroutines
    fun register(
        email: String,
        password: String,
        device_info: String,
        valid_code: String
    ) = viewModelScope.launch {
        isLoading.value = true

        val apiRootUrl =
            TreeHollowApplication.Companion.Config.getConfigItemStringList("api_root_urls")!!
        val retrofit = Retrofit.Builder()
            .baseUrl(apiRootUrl[0])
            .addConverterFactory(MoshiConverterFactory.create())
            .addCallAdapterFactory(CoroutinesResponseCallAdapterFactory())
            .build()
        val service: ApiService = retrofit.create(ApiService::class.java)

        try {
            val response = service.register(
                email,
                Utils.hashString(input = Utils.hashString(input = password)),
                device_info = device_info,
                valid_code = valid_code
            )

            isLoading.value = false
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val tmp = body.token
                    if (tmp != null)
                        tmp.let {
                            TreeHollowApplication.token = it
                            result.value = it
                        }
                    else
                        throw Exception("Error: " + body.msg)
                } else {
                    throw Exception("Network response body is null")
                }
            } else {
                throw Exception("Network error ${response.code()}: ${response.message()}")
            }
        } catch (e: Exception) {
            if (isLoading.value == true)
                isLoading.value = false
            errorMsg.postValue(Event(e.toString()))
        }
    }
}