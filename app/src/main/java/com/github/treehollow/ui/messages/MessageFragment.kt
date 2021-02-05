package com.github.treehollow.ui.messages

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.treehollow.base.TreeHollowApplication
import com.github.treehollow.databinding.FragmentMessageBinding
import com.github.treehollow.repository.SimplePostDetailFetcher
import com.github.treehollow.utils.Utils

class MessageFragment : Fragment() {
    companion object {
        fun newInstance() = MessageFragment()
    }

    private var _binding: FragmentMessageBinding? = null
    private val binding get() = _binding!!
    private val adapter = MessageRecyclerViewAdapter(messageInit = {
//        Quote
        if (message!!.hasQuote()) {
            postCard.setOnClickListener {
                val tmp = message?.quoteId
                (this@MessageFragment.activity as MessageActivity).onPostCardClicked(tmp)
            }
        }

        if (message?.hasQuote() == true) {
            val tmp = message!!
            model.fetchQuote(tmp)
        }

        messageCard.invalidate()
        expanded.invalidate()
    }, bottomInit = {
        loadMore.setOnClickListener { model.more() }
        networkError.setOnClickListener { model.more() }
        model.bottom.observe(this@MessageFragment.viewLifecycleOwner) {
            fun visibility(b: Boolean) = if (b) View.VISIBLE else View.INVISIBLE
            binding.apply {
                loadMore.visibility = visibility(it == Utils.BottomStatus.IDLE)
                noMore.visibility = visibility(it == Utils.BottomStatus.NO_MORE)
                networkError.visibility = visibility(it == Utils.BottomStatus.NETWORK_ERROR)
                bottomLoading.visibility = visibility(it == Utils.BottomStatus.REFRESHING)
            }
        }
    })
    val model: MessageViewModel by viewModels {
        MessageViewModelFactory(adapter)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentMessageBinding.inflate(inflater, container, false)
        val view = binding.root
        model.adapter = adapter
        model.listFetcher = SimplePostDetailFetcher(TreeHollowApplication.token!!)

        model.errorMsg.observe(viewLifecycleOwner, { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                Log.e("DeviceListFragment", it)
            }
        })

        model.isRefreshing.observe(viewLifecycleOwner) {
            binding.swipeRefresh.isRefreshing = it
        }

        // bind adapter to RecyclerView
        binding.messageListRecycle.apply {
            this.adapter = model.adapter
            layoutManager = LinearLayoutManager(context)

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE &&
                        model.bottom.value == Utils.BottomStatus.IDLE &&
                        (((layoutManager as LinearLayoutManager).run {
                            findLastVisibleItemPosition() >= itemCount - 6
                        }))
                    ) model.more()
                }
            })
        }

        adapter.list.observe(viewLifecycleOwner) {
            if (it.isNotEmpty() || model.bottom.value == Utils.BottomStatus.NO_MORE || model.bottom.value == Utils.BottomStatus.NETWORK_ERROR)
                adapter.hasBottom = 1
            else
                adapter.hasBottom = 0
//            adapter.notifyDataSetChanged()
        }

        binding.swipeRefresh.setOnRefreshListener {
            model.refresh()
        }

        // update adapter.list with HTTP response
        model.refresh()
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}