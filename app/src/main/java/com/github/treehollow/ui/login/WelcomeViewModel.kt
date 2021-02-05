package com.github.treehollow.ui.login

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.treehollow.base.Event
import com.github.treehollow.base.Result
import com.github.treehollow.base.TreeHollowApplication
import com.github.treehollow.data.model.TreeHollowConfig
import kotlinx.coroutines.launch

class WelcomeViewModel : ViewModel() {
    var config = MutableLiveData<TreeHollowConfig>(null)
    val networkError = MutableLiveData<Event<String>>()

    val isLoading: MutableLiveData<Boolean> = MutableLiveData(false)

    //    https://developer.android.com/kotlin/coroutines
    fun loadConfig(config_url: String) = viewModelScope.launch {
        isLoading.value = true
        val configResult: Result<TreeHollowConfig> = try {
            TreeHollowApplication.Companion.Config.initConfig(config_url)
        } catch (e: Exception) {
            Result.Error(e)
        }
        isLoading.value = false

        when (configResult) {
            is Result.Success<TreeHollowConfig> -> {
                config.value = configResult.data!!
            }
            is Result.Error -> {
                networkError.postValue(Event(configResult.exception.toString()))
            }
        }
    }
}