package com.github.treehollow.ui.postdetail

import android.graphics.Bitmap
import android.util.Log
import android.widget.EditText
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.treehollow.base.Event
import com.github.treehollow.base.TreeHollowApplication
import com.github.treehollow.data.ReplyState
import com.github.treehollow.network.ApiInterceptor
import com.github.treehollow.network.ApiService
import com.github.treehollow.repository.PostDetailFetcher
import com.github.treehollow.utils.ImageUploadCompressor
import com.github.treehollow.utils.MarkdownBuilder
import com.skydoves.sandwich.coroutines.CoroutinesResponseCallAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class PostDetailViewModel(
    var pid: Int,
    var adapter: ReplyCardAdapter,
    var needRefresh: Boolean
) : ViewModel() {

    companion object {
        val reportTypes = mapOf(
            "fold" to "举报折叠",
            "delete" to "删除",
            "undelete_unban" to "撤销删除",
            "delete_ban" to "删帖禁言",
            "unban" to "解封",
            "set_tag" to "打tag",
            "report" to "举报删除",
        )
    }

    private val token = TreeHollowApplication.token!!
    private var postFetcher: PostDetailFetcher = PostDetailFetcher(
        token, MarkdownBuilder(TreeHollowApplication.instance.applicationContext)
    )
    val errorMsg = MutableLiveData<Event<String>>()
    val infoMsg = MutableLiveData<Event<String>>()
    val compressor: ImageUploadCompressor = ImageUploadCompressor()
    val sending = MutableLiveData(false)
    val hasReplyImage = MutableLiveData(false)
    private var draftImage = MutableLiveData<Bitmap?>(null)
    private var refreshingJob: Job? = null
    val isRefreshing: MutableLiveData<Boolean> = MutableLiveData(false)
    private val apiRootUrls =
        TreeHollowApplication.Companion.Config.getConfigItemStringList("api_root_urls")!!
    private val service = Retrofit.Builder()
        .baseUrl(apiRootUrls[0])
        .addConverterFactory(MoshiConverterFactory.create())
        .addCallAdapterFactory(CoroutinesResponseCallAdapterFactory())
        .build().create(ApiService::class.java)

    fun refresh() {
        refreshingJob = viewModelScope.launch {
            clearList()
            adapter.notifyDataSetChanged()
            refreshingJob?.cancel(CancellationException())
            isRefreshing.value = true
            try {
                val tmp = postFetcher.fetchPostDetail(pid, 1)
                Log.d(
                    "PostDetailViewModel",
                    "pid: ${tmp.id()}, has_comments: ${!tmp.hasNoComments()}"
                )
                adapter.post.value = tmp
                if (!tmp.hasNoComments())
                    adapter.list.value = tmp.comments!!
                adapter.notifyDataSetChanged()
                isRefreshing.value = false
            } catch (e: PostDetailFetcher.NoPostException) {
                errorMsg.postValue(Event("找不到这条树洞"))
                adapter.notifyDataSetChanged()
                isRefreshing.value = false
            } catch (e: CancellationException) {
            } catch (e: Exception) {
                errorMsg.postValue(Event(e.toString()))
                adapter.notifyDataSetChanged()
                isRefreshing.value = false
            } finally {
            }
        }
    }

    fun star(switch: Int) = viewModelScope.launch {
        try {
            val response = service.editAttention(token, pid, switch)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    if (body.code < 0)
                        throw Exception("Error: ${body.msg}")
                    else {
                        adapter.post.value?.post_data = body.data!!
                        adapter.notifyDataSetChanged()
                        infoMsg.postValue(
                            Event(
                                if (switch == 1) {
                                    "关注成功"
                                } else {
                                    "取消关注成功"
                                }
                            )
                        )
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

    fun doVote(option: String) = viewModelScope.launch {
        try {
            val response = service.sendVote(token, pid, option)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    if (body.code < 0)
                        throw Exception("Error: ${body.msg}")
                    else {
                        adapter.post.value?.post_data?.vote = body.vote!!
                        adapter.notifyDataSetChanged()
                        infoMsg.postValue(Event("投票成功"))
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

    init {
        if (needRefresh)
            refresh()
    }

    private fun clearList() {
        for (l in adapter.list.value!!) {
            l.index = null
        }
        adapter.list.value = listOf()
    }

    fun reply(editText: EditText, reply: ReplyState?, pid: Int) = viewModelScope.launch {

        sending.value = true

        val res = compressor.compressDraftImage(draftImage.value)
        val imgData = compressor.encodeImage(res)
        val text = editText.text!!.toString()
        var type = "text"
        if (imgData != null) {
            type = "image"
        }

        if (editText.text.toString().isBlank() && imgData == null) {
            errorMsg.postValue(Event("内容不能空"))
            sending.value = false
            return@launch
        }

        try {
            val token = TreeHollowApplication.token
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
                val response = service.replyPost(text, type, pid, reply?.comment_data?.cid, imgData)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        if (body.code != 0)
                            throw Exception(body.msg!!)
                        else {
                            editText.setText("")
                            removeImage()
                            refresh()
                        }
                    } else {
                        throw Exception("Network response body is null")
                    }
                } else {
                    throw Exception("Network error ${response.code()}: ${response.message()}")
                }

            } else {
                throw Exception("Token not found")
            }

        } catch (e: Exception) {
            errorMsg.postValue(Event(e.toString()))
        } finally {
            sending.value = false
        }
    }

    fun doReport(type: String, id: Int, reason: String, isPost: Boolean = true) =
        viewModelScope.launch {
            try {
                val token = TreeHollowApplication.token
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
                    val response = if (isPost) {
                        service.reportPost(token, id, type, reason)
                    } else {
                        service.reportComment(token, id, type, reason)
                    }
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null) {
                            if (body.code != 0)
                                throw Exception(body.msg!!)
                            else {
                                errorMsg.postValue(Event("${reportTypes[type]}成功"))
                                refresh()
                            }
                        } else {
                            throw Exception("Network response body is null")
                        }
                    } else {
                        throw Exception("Network error ${response.code()}: ${response.message()}")
                    }

                } else {
                    throw Exception("Token not found")
                }

            } catch (e: Exception) {
                errorMsg.postValue(Event(e.toString()))
            }
        }

    fun addImage(image: Bitmap) {
        draftImage.value = image
        hasReplyImage.value = true
    }

    fun removeImage() {
        draftImage.value = null
        hasReplyImage.value = false
    }
}

class PostDetailViewModelFactory(
    private val pid: Int,
    private val adapter: ReplyCardAdapter,
    private val needRefresh: Boolean
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return PostDetailViewModel(pid, adapter, needRefresh) as T
    }
}