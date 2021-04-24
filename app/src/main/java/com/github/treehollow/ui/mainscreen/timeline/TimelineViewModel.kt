package com.github.treehollow.ui.mainscreen.timeline

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.treehollow.base.Event
import com.github.treehollow.base.TreeHollowApplication
import com.github.treehollow.data.PostFetcher
import com.github.treehollow.data.PostState
import com.github.treehollow.network.ApiService
import com.github.treehollow.repository.PostDetailFetcher
import com.github.treehollow.utils.MarkdownBuilder
import com.github.treehollow.utils.Utils
import com.skydoves.sandwich.coroutines.CoroutinesResponseCallAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class TimelineViewModel(
    private var listFetcher: PostFetcher?,
    private val adapter: TimelineRecyclerViewAdapter,
) :
    ViewModel() {
    private val token: String = TreeHollowApplication.token!!
    val errorMsg = MutableLiveData<Event<String>>()
    val infoMsg = MutableLiveData<Event<String>>()
    val isRefreshing: MutableLiveData<Boolean> = MutableLiveData(false)
    private var refreshingJob: Job? = null
    private var page: Int = 1
    var bottom = MutableLiveData(Utils.BottomStatus.REFRESHING)

    private val apiRootUrls =
        TreeHollowApplication.Companion.Config.getConfigItemStringList("api_root_urls")!!
    private val service = Retrofit.Builder()
        .baseUrl(apiRootUrls[0])
        .addConverterFactory(MoshiConverterFactory.create())
        .addCallAdapterFactory(CoroutinesResponseCallAdapterFactory())
        .build().create(ApiService::class.java)

    fun setFetcher(fetcher: PostFetcher?) {
        this.listFetcher = fetcher
    }

    private fun clearList() {
        for (l in adapter.list.value!!) {
            l.index = null
        }
        adapter.list.value = listOf()
    }

    fun refresh() {
        refreshingJob = viewModelScope.launch {
            page = 1
            clearList()
            adapter.notifyDataSetChanged()
            bottom.value = Utils.BottomStatus.REFRESHING
            refreshingJob?.cancel(CancellationException())
            isRefreshing.value = true
            try {
                if (listFetcher == null)
                    return@launch
                val tmp = listFetcher!!.fetchList(page)
                for (i in tmp.indices) {
                    tmp[i].index = i + adapter.hasTop
                    if (!tmp[i].hasNoQuote()) {
                        fetchQuote(tmp[i])
                    }
                }
                adapter.list.value = tmp
                bottom.value =
                    if (tmp.isEmpty()) Utils.BottomStatus.NO_MORE else Utils.BottomStatus.IDLE
                adapter.notifyDataSetChanged()
                page++
                isRefreshing.value = false
            } catch (e: CancellationException) {
//                bottom.value = Utils.BottomStatus.IDLE
//                adapter.notifyDataSetChanged()
            } catch (e: Exception) {
                errorMsg.postValue(Event(e.toString()))
                bottom.value = Utils.BottomStatus.IDLE
                adapter.notifyDataSetChanged()
                isRefreshing.value = false
            } finally {
            }
        }
    }

    fun more() {
        refreshingJob?.cancel(CancellationException())
        bottom.value = Utils.BottomStatus.REFRESHING

        val oldListSize = adapter.itemCount
        val oldBottomSize = adapter.hasBottom
        refreshingJob = viewModelScope.launch {
            try {
                if (listFetcher == null)
                    return@launch
                val tmp = listFetcher!!.fetchList(page)
                adapter.list.value = adapter.list.value!! + tmp
                for (i in tmp.indices) {
                    tmp[i].index = i + oldListSize - adapter.hasBottom

                    if (!tmp[i].hasNoQuote()) {
                        fetchQuote(tmp[i])
                    }
                }
                bottom.value =
                    if (tmp.isEmpty()) Utils.BottomStatus.NO_MORE else Utils.BottomStatus.IDLE

                adapter.notifyItemRangeInserted(oldListSize, tmp.size + 1 - oldBottomSize)
                page++
            } catch (e: CancellationException) {
//                bottom.value = Utils.BottomStatus.IDLE
            } catch (e: Exception) {
                errorMsg.postValue(Event(e.toString()))
                if (bottom.value == Utils.BottomStatus.REFRESHING)
                    bottom.value = Utils.BottomStatus.NETWORK_ERROR
            }
        }
    }

    fun star(post: PostState, switch: Int) = viewModelScope.launch {
        try {
            val response = service.editAttention(token, post.post_data.pid, switch)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    if (body.code < 0)
                        throw Exception("Error: ${body.msg}")
                    else {
                        post.index?.let {
                            adapter.list.value?.get(it - adapter.hasTop)?.post_data = body.data!!
                            adapter.notifyItemChanged(it)
                        }
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

    fun doVote(post: PostState, option: String) = viewModelScope.launch {
        try {
            val response = service.sendVote(token, post.post_data.pid, option)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    if (body.code < 0)
                        throw Exception("Error: ${body.msg}")
                    else {
                        post.index?.let {
                            adapter.list.value?.get(it - adapter.hasTop)?.post_data?.vote =
                                body.vote!!
                            adapter.notifyItemChanged(it)
                        }
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

    private fun fetchQuote(post: PostState) = viewModelScope.launch {
        try {
            val resp = PostDetailFetcher(
                token,
                MarkdownBuilder(TreeHollowApplication.instance.applicationContext)
            ).fetchPostDetail(post.quoteId!!, 0)
            post.index?.let {
                adapter.list.value?.get(it - adapter.hasTop)?.quotePostState = resp
                adapter.notifyItemChanged(it)
            }

        } catch (e: PostDetailFetcher.NoPostException) {
            post.index?.let {
                adapter.list.value?.get(it - adapter.hasTop)?.quoteId = null
                adapter.notifyItemChanged(it)
            }
        } catch (e: Exception) {
//            TODO: show error
            post.index?.let {
                adapter.list.value?.get(it - adapter.hasTop)?.quoteId = null
                adapter.notifyItemChanged(it)
            }
        } finally {
        }
    }

    init {
        refresh()
    }
}