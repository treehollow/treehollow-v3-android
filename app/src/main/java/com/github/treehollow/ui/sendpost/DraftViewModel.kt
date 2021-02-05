package com.github.treehollow.ui.sendpost

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.treehollow.base.Event
import com.github.treehollow.base.TreeHollowApplication
import com.github.treehollow.network.ApiInterceptor
import com.github.treehollow.network.ApiService
import com.github.treehollow.utils.ImageUploadCompressor
import com.skydoves.sandwich.coroutines.CoroutinesResponseCallAdapterFactory
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory


class DraftViewModel : ViewModel() {

    companion object {

        const val MAX_VOTE_OPTION_COUNT = 4
    }

    val SUCCESS = 0
    var availableTag = MutableLiveData(ArrayList<String>())
    var draftTag = MutableLiveData<String?>(null)
    private var draftImage = MutableLiveData<Bitmap?>(null)
    var draftImageShow = MutableLiveData<Bitmap?>(null)
    var draftText = MutableLiveData("")

    var imageVisible = MutableLiveData(false)

    var voteEnabled = MutableLiveData(false)
    var voteEditing = MutableLiveData(false)
    var voteOptions = MutableLiveData(listOf("", ""))

    var isSending = MutableLiveData(false)
    var errorCode = MutableLiveData<Int?>(null)
    var responseMsg: String? = null
    var errorMessage = MutableLiveData<Event<String>>(null)
    val compressor: ImageUploadCompressor = ImageUploadCompressor()

    fun addImage(image: Bitmap) {
        draftImage.value = image
        draftImageShow.value = compressor.compressDisplayImage(image)
        imageVisible.value = true
    }

    fun removeImage() {
        draftImage.value = null
        draftImageShow.value = null
        imageVisible.value = false
    }

    private fun enableVote() {
        voteEnabled.value = true
        expandVoteCancelOrConfirm()
    }

    fun disableVote() {
        voteEnabled.value = false
        collapseVoteCancelOrConfirm()
    }

    fun expandVoteCancelOrConfirm() {
        voteEditing.value = true
    }

    fun collapseVoteCancelOrConfirm() {
        voteEditing.value = false
    }

    fun toggleVote() {
        if (voteEnabled.value!!) {
            disableVote()
        } else {
            enableVote()
        }
    }


    fun appendVoteOption() {
        if (voteOptions.value!!.size < MAX_VOTE_OPTION_COUNT) {
            voteOptions.value = voteOptions.value!!.plus("")
        }
    }

    private fun clearVoteOption(index: Int) {
        voteOptions.value = voteOptions.value!!.mapIndexed { i, v ->
            if (i == index) {
                ""
            } else {
                v
            }
        }
    }

    private fun removeVoteOption(index: Int) {
        if (voteOptions.value!!.size > 2) {
            voteOptions.value = voteOptions.value!!.filterIndexed { i, _ -> i != index }
        }
    }

    fun clearOrRemoveVoteOption(index: Int) {
        if (voteOptions.value!![index] != "") {
            clearVoteOption(index)
        } else {
            removeVoteOption(index)
        }
    }

    private fun getVoteData(): List<String>? {
        if (!voteEnabled.value!!) {
            return null
        }
        val trimmedVote = voteOptions.value!!.filter { it != "" }
        if (trimmedVote.size >= 2) {
            return trimmedVote
        }
        return null
    }

    fun submit() = viewModelScope.launch {

        val text = draftText.value!!
        var type = "text"
        val tag = draftTag.value
        val res = compressor.compressDraftImage(draftImage.value)
        val imgData = compressor.encodeImage(res)
        val voteData = getVoteData()
        Log.d("DraftViewModel", "votedata: $voteData")
        if (imgData != null) {
            type = "image"
        }
        if (text == "" && imgData == null && voteData == null) {
            errorMessage.postValue(Event("Cannot send empty content"))
            return@launch
        }

        val token = TreeHollowApplication.token
        isSending.value = true
        if (token != null) {
            val okHttpClientBuilder = OkHttpClient.Builder()
            okHttpClientBuilder.addInterceptor(ApiInterceptor(token))
            val baseUrl =
                TreeHollowApplication.Companion.Config.getConfigItemStringList("api_root_urls")!!
            val retrofit = Retrofit.Builder()
                .client(okHttpClientBuilder.build())
                .baseUrl(baseUrl[0])
                .addConverterFactory(MoshiConverterFactory.create())
                .addCallAdapterFactory(CoroutinesResponseCallAdapterFactory())
                .build()

            val service: ApiService = retrofit.create(ApiService::class.java)

            try {
                val response = service.sendPost(text, type, tag, imgData, voteData)
                isSending.value = false
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val tmp = body.code
                        responseMsg = body.msg
                        errorCode.setValue(tmp)
                    } else {
                        throw Exception("Network response body is null")
                    }
                } else {
                    throw Exception("Network error ${response.code()}: ${response.message()}")
                }
            } catch (e: Exception) {
                if (isSending.value == true)
                    isSending.value = false
                errorMessage.postValue(Event(e.toString()))
            }
        } else {
            if (isSending.value == true)
                isSending.value = false
            errorMessage.postValue(Event("No token found"))
        }
    }
}


