package com.github.treehollow.ui.mainscreen.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.treehollow.data.PostFetcher


class TimelineViewModelFactory(
    private val fetcher: PostFetcher?,
    private val adapter: TimelineRecyclerViewAdapter,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return TimelineViewModel(fetcher, adapter) as T
    }
}