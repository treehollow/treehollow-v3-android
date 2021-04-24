package com.github.treehollow.ui.messages

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.treehollow.base.Event
import com.github.treehollow.base.TreeHollowApplication
import com.github.treehollow.data.MessageState
import com.github.treehollow.network.ApiService
import com.github.treehollow.repository.SimplePostDetailFetcher
import com.github.treehollow.utils.Utils
import com.skydoves.sandwich.coroutines.CoroutinesResponseCallAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class MessageViewModel(
    var adapter: MessageRecyclerViewAdapter,
) : ViewModel() {

    private var token: String = TreeHollowApplication.token!!
    lateinit var listFetcher: SimplePostDetailFetcher
    val errorMsg = MutableLiveData<Event<String>>()
    private var refreshingJob: Job? = null
    val isRefreshing: MutableLiveData<Boolean> = MutableLiveData(false)
    private var page: Int = 1
    var bottom = MutableLiveData(Utils.BottomStatus.REFRESHING)

    private val apiRootUrls =
        TreeHollowApplication.Companion.Config.getConfigItemStringList("api_root_urls")!!
    private val service = Retrofit.Builder()
        .baseUrl(apiRootUrls[0])
        .addConverterFactory(MoshiConverterFactory.create())
        .addCallAdapterFactory(CoroutinesResponseCallAdapterFactory())
        .build().create(ApiService::class.java)

    fun refresh() {
        refreshingJob = viewModelScope.launch {
            page = 1
            clearList()
            adapter.notifyDataSetChanged()
            bottom.value = Utils.BottomStatus.REFRESHING
            refreshingJob?.cancel(CancellationException())
            isRefreshing.value = true
            try {

                val response = service.messageList(token, page, true, -1)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        if (body.code < 0) {
                            throw Exception(body.msg)
                        }
                        val tmp = mutableListOf<MessageState>()
                        for (ent in body.data!!) {
                            tmp.add(MessageState(ent))
                        }

                        for (i in tmp.indices) {
                            tmp[i].index = i

                            if (tmp[i].hasQuote()) {
                                fetchQuote(tmp[i])
                            }
                        }

                        adapter.list.value = tmp
                        bottom.value =
                            if (tmp.isEmpty()) Utils.BottomStatus.NO_MORE else Utils.BottomStatus.IDLE
                        adapter.notifyDataSetChanged()
                        page++
                        isRefreshing.value = false
                    } else {
                        throw Exception("Network response body is null")
                    }
                } else {
                    throw Exception("Network error ${response.code()}: ${response.message()}")
                }
            } catch (e: CancellationException) {
                bottom.value = Utils.BottomStatus.IDLE
                adapter.notifyDataSetChanged()
            } catch (e: Exception) {
                errorMsg.postValue(Event(e.toString()))
                bottom.value = Utils.BottomStatus.IDLE
                adapter.notifyDataSetChanged()
                isRefreshing.value = false
            } finally {
            }
        }
    }

    private fun clearList() {
        for (l in adapter.list.value!!) {
            l.index = null
        }
        adapter.list.value = listOf()
    }

    fun more() {
        refreshingJob?.cancel(CancellationException())
        bottom.value = Utils.BottomStatus.REFRESHING

        val oldListSize = adapter.itemCount
        val oldBottomSize = adapter.hasBottom
        refreshingJob = viewModelScope.launch {
            try {
                val response = service.messageList(token, page, true, -1)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        if (body.code < 0) {
                            throw Exception(body.msg)
                        }
                        val tmp = mutableListOf<MessageState>()
                        for (ent in body.data!!) {
                            tmp.add(MessageState(ent))
                        }

                        for (i in tmp.indices) {
                            tmp[i].index = i + oldListSize - adapter.hasBottom

                            if (tmp[i].hasQuote()) {
                                fetchQuote(tmp[i])
                            }
                        }
                        adapter.list.value = adapter.list.value!! + tmp

                        bottom.value =
                            if (tmp.isEmpty()) Utils.BottomStatus.NO_MORE else Utils.BottomStatus.IDLE
                        adapter.notifyItemRangeInserted(oldListSize, tmp.size + 1 - oldBottomSize)
                        page++
                    } else {
                        throw Exception("Network response body is null")
                    }
                } else {
                    throw Exception("Network error ${response.code()}: ${response.message()}")
                }
            } catch (e: CancellationException) {
                bottom.value = Utils.BottomStatus.IDLE
                adapter.notifyDataSetChanged()
            } catch (e: Exception) {
                errorMsg.postValue(Event(e.toString()))
                if (bottom.value == Utils.BottomStatus.REFRESHING)
                    bottom.value = Utils.BottomStatus.NETWORK_ERROR
            } finally {
            }
        }
    }

    private fun fetchQuote(message: MessageState) = viewModelScope.launch {
        try {
            val resp = SimplePostDetailFetcher(token).fetchPostDetail(message.quoteId!!)
            message.index?.let {
                adapter.list.value?.get(it)?.quotePostState = resp
                adapter.notifyItemChanged(it)
            }
        } catch (e: SimplePostDetailFetcher.NoPostException) {
            message.index?.let {
                adapter.list.value?.get(it)?.quoteId = null
                adapter.notifyItemChanged(it)
            }
        } catch (e: Exception) {
            Log.d("MessageViewModel", e.toString())
            message.index?.let {
                adapter.list.value?.get(it)?.quoteId = null
                adapter.notifyItemChanged(it)
            }
        } finally {
        }
    }
}

class MessageViewModelFactory(
    private val adapter: MessageRecyclerViewAdapter,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MessageViewModel(adapter) as T
    }
}