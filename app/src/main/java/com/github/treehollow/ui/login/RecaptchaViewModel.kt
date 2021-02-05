package com.github.treehollow.ui.login

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.treehollow.base.Event
import com.github.treehollow.base.TreeHollowApplication
import com.github.treehollow.network.ApiService
import com.skydoves.sandwich.coroutines.CoroutinesResponseCallAdapterFactory
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory


class RecaptchaViewModel : ViewModel() {
    val errorMsg = MutableLiveData<Event<String>>()
    var result: MutableLiveData<Int> = MutableLiveData(-1)

    val isLoading: MutableLiveData<Boolean> = MutableLiveData(false)

    //    https://developer.android.com/kotlin/coroutines
    fun checkEmail(
        email: String,
        recaptcha_token: String
    ) = viewModelScope.launch {
        isLoading.value = true

        val apiRootUrls =
            TreeHollowApplication.Companion.Config.getConfigItemStringList("api_root_urls")!!
        val retrofit = Retrofit.Builder()
            .baseUrl(apiRootUrls[0])
            .addConverterFactory(MoshiConverterFactory.create())
            .addCallAdapterFactory(CoroutinesResponseCallAdapterFactory())
            .build()
        val service: ApiService = retrofit.create(ApiService::class.java)

        try {
            val response = service.checkEmail(email, recaptcha_token, "v2")
            isLoading.value = false

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    when (body.code) {
                        CheckEmailResponseCode.Login.code -> {
                            result.value = CheckEmailResponseCode.Login.code
                        }
                        CheckEmailResponseCode.Register.code -> {
                            result.value = CheckEmailResponseCode.Register.code
                        }
                        CheckEmailResponseCode.NeedRecaptcha.code -> {
                            throw Exception("Error: Recaptcha verification failed")
                        }
                        else -> {
                            throw Exception("Error: " + body.msg)
                        }
                    }
                } else {
                    throw Exception("Network response body is null")
                }
            } else {
                throw Exception("Network error ${response.code()}: ${response.message()}")
            }
        } catch (e: Exception) {
            isLoading.value = false

            errorMsg.postValue(Event(e.toString()))
        }
    }

    enum class CheckEmailResponseCode(val code: Int) {
        Login(0),
        Register(1),
        NeedRecaptcha(3)
    }
}