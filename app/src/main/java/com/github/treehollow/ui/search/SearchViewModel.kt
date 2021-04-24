package com.github.treehollow.ui.search

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.treehollow.base.Event

class SearchViewModel : ViewModel() {
    val errorMsg = MutableLiveData<Event<String>>()
    val infoMsg = MutableLiveData<Event<String>>()
    val isAdvanceSearchDisplay: MutableLiveData<Boolean> = MutableLiveData(false)
    val isSearching: MutableLiveData<Boolean> = MutableLiveData(false)
    val isIncludeComment: MutableLiveData<Boolean> = MutableLiveData(true)
    val startDateString: MutableLiveData<String> = MutableLiveData("")
    val endDateString: MutableLiveData<String> = MutableLiveData("")
}