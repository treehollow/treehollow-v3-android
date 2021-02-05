package com.github.treehollow.ui.messages

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.github.treehollow.data.MessageListBottom
import com.github.treehollow.data.MessageListElem
import com.github.treehollow.data.MessageState
import com.github.treehollow.databinding.MessageItemBinding
import com.github.treehollow.databinding.RecycleBottomBinding

class MessageRecyclerViewAdapter(
    private val messageInit: MessageItemBinding.() -> Unit,
    private val bottomInit: RecycleBottomBinding.() -> Unit,
) : RecyclerView.Adapter<MessageRecyclerViewAdapter.ViewHolder>() {
    companion object {
        const val MESSAGE = 0
        const val BOTTOM = 2
    }

    var hasBottom: Int = 0

    val list = MutableLiveData(listOf<MessageState>())

    sealed class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        class MessageItem(
            private val binding: MessageItemBinding,
            private val init: MessageItemBinding.() -> Unit,
        ) : ViewHolder(binding.root) {
            fun bind(item: MessageState) {
                binding.apply {
                    message = item
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
            MESSAGE -> {
                val binding = MessageItemBinding.inflate(inflater, parent, false)
                ViewHolder.MessageItem(
                    binding,
                    messageInit
                )
            }
            BOTTOM -> {
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
            is ViewHolder.MessageItem -> {
                holder.setIsRecyclable(true)
                holder.bind(getItem(position) as MessageState)
            }
            is ViewHolder.Bottom -> Unit
        }
    }

    fun getItem(position: Int): MessageListElem {
        if (position < list.value!!.size)
            return list.value!![position]
        return MessageListBottom
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (holder is ViewHolder.MessageItem)
            (payloads.singleOrNull() as? MessageState)?.let {
                holder.setIsRecyclable(true)
                holder.bind(it)
            }
                ?: super.onBindViewHolder(holder, position, payloads)
        else super.onBindViewHolder(holder, position, payloads)
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            MessageListBottom -> BOTTOM
            else -> MESSAGE
        }
    }

    override fun getItemCount() = list.value!!.size + hasBottom
}