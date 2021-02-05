package com.github.treehollow.ui.postdetail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.github.treehollow.data.ListElem
import com.github.treehollow.data.PostState
import com.github.treehollow.data.ReplyState
import com.github.treehollow.databinding.CardPostDetailBinding
import com.github.treehollow.databinding.CardReplyBinding

class ReplyCardAdapter(
    private val postInit: CardPostDetailBinding.() -> Unit,
    private val replyInit: CardReplyBinding.() -> Unit
) : RecyclerView.Adapter<ReplyCardAdapter.ViewHolder>() {
    companion object {
        const val POST = 0
        const val REPLY = 1
    }

    public var hasPost: Int = 1

    val list = MutableLiveData(listOf<ReplyState>())
    var post = MutableLiveData<PostState>()

    sealed class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        class DetailPostCard(
            private val binding: CardPostDetailBinding,
            private val init: CardPostDetailBinding.() -> Unit,
        ) : ViewHolder(binding.root) {
            fun bind(item: PostState) {
                binding.apply {
                    post = item
                    init()
                    executePendingBindings()
                }
            }
        }

        class ReplyCard(
            private val binding: CardReplyBinding,
            private val init: CardReplyBinding.() -> Unit,
        ) : ViewHolder(binding.root) {
            fun bind(item: ReplyState) {
                binding.apply {
                    reply = item
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
        return when (viewType) {
            Companion.POST -> {
                val binding = CardPostDetailBinding.inflate(inflater, parent, false)
                ViewHolder.DetailPostCard(
                    binding,
                    postInit
                )
            }
            Companion.REPLY -> {
                val binding = CardReplyBinding.inflate(inflater, parent, false)
                ViewHolder.ReplyCard(
                    binding,
                    replyInit
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
            is ViewHolder.DetailPostCard -> {
                holder.setIsRecyclable(false)
                holder.bind(getItem(position) as PostState)
            }
        }
    }

    fun getItem(position: Int): ListElem {
        if (position == 0)
            return post.value!!
        else (position < list.value!!.size)
        return list.value!![position - 1]
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (holder is ViewHolder.ReplyCard) {
            (payloads.singleOrNull() as? ReplyState)?.let {
                holder.setIsRecyclable(false)
                holder.bind(it)
            } ?: super.onBindViewHolder(holder, position, payloads)
        } else if (holder is ViewHolder.DetailPostCard) {
            (payloads.singleOrNull() as? PostState)?.let {
                holder.setIsRecyclable(false)
                holder.bind(it)
            } ?: super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0)
            POST
        else REPLY
    }

    override fun getItemCount(): Int {
        val tmp = if (post.value == null) 0 else 1
        return tmp + list.value!!.size
    }
}