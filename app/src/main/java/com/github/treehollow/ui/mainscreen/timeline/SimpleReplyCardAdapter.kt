package com.github.treehollow.ui.mainscreen.timeline

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.github.treehollow.data.ReplyListBottom
import com.github.treehollow.data.ReplyListElem
import com.github.treehollow.data.ReplyState
import com.github.treehollow.databinding.CardSimpleReplyBinding
import com.github.treehollow.databinding.RecycleBottomBinding

class SimpleReplyCardAdapter(
    private val replyInit: CardSimpleReplyBinding.() -> Unit,
    private val bottomInit: RecycleBottomBinding.() -> Unit
) : RecyclerView.Adapter<SimpleReplyCardAdapter.ViewHolder>() {
    companion object {
        const val REPLY = 0
        const val BOTTOM = 2
    }

    public var hasBottom: Int = 0

    val list = MutableLiveData(listOf<ReplyState>())

    sealed class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        class ReplyCard(
            private val binding: CardSimpleReplyBinding,
            private val init: CardSimpleReplyBinding.() -> Unit,
        ) : ViewHolder(binding.root) {
            fun bind(item: ReplyState) {
                binding.apply {
                    reply = item
                    init()
                    executePendingBindings()
                }
            }
        }

        class Bottom(
            binding: RecycleBottomBinding,
            init: RecycleBottomBinding.() -> Unit
        ) : ViewHolder(binding.root) {
            init {
                binding.init()
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            Companion.REPLY -> {
                val binding = CardSimpleReplyBinding.inflate(inflater, parent, false)
                ViewHolder.ReplyCard(
                    binding,
                    replyInit
                )
            }
            Companion.BOTTOM -> {
                val binding = RecycleBottomBinding.inflate(inflater, parent, false)
                ViewHolder.Bottom(
                    binding,
                    bottomInit
                )
            }
            else -> error("")
        }
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        when (holder) {
            is ViewHolder.ReplyCard -> {
                holder.setIsRecyclable(false)
                holder.bind(getItem(position) as ReplyState)
            }
            is ViewHolder.Bottom -> Unit
        }
    }

    fun getItem(position: Int): ReplyListElem {
        if (position < list.value!!.size)
            return list.value!![position]
        return ReplyListBottom
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (holder is ViewHolder.ReplyCard)
            (payloads.singleOrNull() as? ReplyState)?.let {
                holder.setIsRecyclable(false)
                holder.bind(it)
            }
                ?: super.onBindViewHolder(holder, position, payloads)
        else super.onBindViewHolder(holder, position, payloads)
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            ReplyListBottom -> Companion.BOTTOM
            is ReplyState -> Companion.REPLY
            else -> -1
        }
    }

    override fun getItemCount() = hasBottom + list.value!!.size
}