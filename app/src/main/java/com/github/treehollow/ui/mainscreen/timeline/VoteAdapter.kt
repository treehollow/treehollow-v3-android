package com.github.treehollow.ui.mainscreen.timeline

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.github.treehollow.data.VoteState
import com.github.treehollow.databinding.VoteOptionItemBinding

class VoteAdapter(
    private val voteInit: VoteOptionItemBinding.() -> Unit,
) : RecyclerView.Adapter<VoteAdapter.ViewHolder>() {

    val list = MutableLiveData(listOf<VoteState>())

    sealed class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        class VoteOption(
            private val binding: VoteOptionItemBinding,
            private val init: VoteOptionItemBinding.() -> Unit,
        ) : ViewHolder(binding.root) {
            fun bind(item: VoteState) {
                binding.apply {
                    vote = item
                    init()
                    executePendingBindings()
                }
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = VoteOptionItemBinding.inflate(inflater, parent, false)
        return ViewHolder.VoteOption(
            binding,
            voteInit
        )
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        holder.setIsRecyclable(false)
        (holder as ViewHolder.VoteOption).bind(getItem(position))
    }

    private fun getItem(position: Int): VoteState {
        return list.value!![position]
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        (payloads.singleOrNull() as? VoteState)?.let {
            holder.setIsRecyclable(false)
            (holder as ViewHolder.VoteOption).bind(it)
        }
            ?: super.onBindViewHolder(holder, position, payloads)
    }

    override fun getItemCount() = list.value!!.size
}