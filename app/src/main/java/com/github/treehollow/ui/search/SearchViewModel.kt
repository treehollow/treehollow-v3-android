package com.github.treehollow.ui.search

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.treehollow.base.Event
import com.github.treehollow.data.PostState
import com.github.treehollow.repository.SearchResultListFetcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {
    val errorMsg = MutableLiveData<Event<String>>()
    val infoMsg = MutableLiveData<Event<String>>()
    val isAdvanceSearchDisplay: MutableLiveData<Boolean> = MutableLiveData(false)
    val isSearching: MutableLiveData<Boolean> = MutableLiveData(false)
    val isIncludeComment: MutableLiveData<Boolean> = MutableLiveData(true)
    val startDateString: MutableLiveData<String> = MutableLiveData("")
    val endDateString: MutableLiveData<String> = MutableLiveData("")
    val list = MutableLiveData(listOf<PostState>())
    private var searchJob: Job? = null

    fun search(page: Int, fetcher: SearchResultListFetcher) {

        searchJob = viewModelScope.launch {
            searchJob?.cancel(CancellationException())
            isSearching.value = true
            try {
                val tmp = fetcher.fetchList(page)
                Log.d("SearchViewModel", "fetcher got list! length: ${tmp.size}")
                list.value = tmp
                isSearching.value = false
            } catch (e: CancellationException) {
            } catch (e: Exception) {
                errorMsg.postValue(Event(e.toString()))
                isSearching.value = false
            } finally {
            }
        }
    }
}