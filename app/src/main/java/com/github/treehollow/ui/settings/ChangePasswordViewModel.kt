package com.github.treehollow.ui.settings

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


class ChangePasswordViewModel : ViewModel() {
    val errorMsg = MutableLiveData<Event<String>>()
    var result: MutableLiveData<Boolean> = MutableLiveData(false)
    val isLoading: MutableLiveData<Boolean> = MutableLiveData(false)

    //    https://developer.android.com/kotlin/coroutines
    fun changePassword(
        email: String,
        oldPassword: String,
        newPassword: String
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
            val response = service.changePassword(
                email,
                Utils.hashString(input = Utils.hashString(input = oldPassword)),
                Utils.hashString(input = Utils.hashString(input = newPassword))
            )

            isLoading.value = false
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    if (body.code == 0) {

                        TreeHollowApplication.deleteToken()
                        result.value = true
                    } else
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