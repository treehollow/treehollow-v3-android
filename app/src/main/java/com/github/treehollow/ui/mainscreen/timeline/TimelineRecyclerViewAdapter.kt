package com.github.treehollow.ui.mainscreen.timeline

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.github.treehollow.data.AnnouncementState
import com.github.treehollow.data.PostListBottom
import com.github.treehollow.data.PostListElem
import com.github.treehollow.data.PostState
import com.github.treehollow.databinding.CardAnnouncementBinding
import com.github.treehollow.databinding.CardPostBinding
import com.github.treehollow.databinding.RecycleBottomBinding

class TimelineRecyclerViewAdapter(
    private val postInit: CardPostBinding.() -> Unit,
    private val topInit: CardAnnouncementBinding.() -> Unit,
    private val bottomInit: RecycleBottomBinding.() -> Unit,
    var hasTop: Int = 0
) : RecyclerView.Adapter<TimelineRecyclerViewAdapter.ViewHolder>() {
    companion object {
        const val POST = 0
        const val TOP = 1
        const val BOTTOM = 2
    }

    var hasBottom: Int = 0

    val list = MutableLiveData(listOf<PostState>())
    var top: AnnouncementState = AnnouncementState("")

    sealed class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        class PostCard(
            private val binding: CardPostBinding,
            private val init: CardPostBinding.() -> Unit,
        ) : ViewHolder(binding.root) {
            fun bind(item: PostState) {
                binding.apply {
                    post = item
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

        class Top(
            private val binding: CardAnnouncementBinding,
            private val init: CardAnnouncementBinding.() -> Unit
        ) : ViewHolder(binding.root) {
            fun bind(item: AnnouncementState) {
                binding.apply {
                    announcement = item
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
            POST -> {
                val binding = CardPostBinding.inflate(inflater, parent, false)
                ViewHolder.PostCard(
                    binding,
                    postInit
                )
            }
            BOTTOM -> {
                val binding = RecycleBottomBinding.inflate(inflater, parent, false)
                ViewHolder.Bottom(
                    binding,
                    bottomInit
                )
            }
            TOP -> {
                val binding = CardAnnouncementBinding.inflate(inflater, parent, false)
                ViewHolder.Top(
                    binding,
                    topInit
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
            is ViewHolder.PostCard -> {
                holder.setIsRecyclable(false)
                holder.bind(getItem(position) as PostState)
            }
            is ViewHolder.Top -> {
                holder.setIsRecyclable(false)
                holder.bind(getItem(position) as AnnouncementState)
            }
            is ViewHolder.Bottom -> Unit
        }
    }

    fun getItem(position: Int): PostListElem {
        if (hasTop == 1 && position == 0)
            return top
        if (position < list.value!!.size + hasTop)
            return list.value!![position - hasTop]
        return PostListBottom
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        when (holder) {
            is ViewHolder.PostCard -> (payloads.singleOrNull() as? PostState)?.let {
                holder.setIsRecyclable(false)
                holder.bind(it)
            }
                ?: super.onBindViewHolder(holder, position, payloads)
            is ViewHolder.Top -> (payloads.singleOrNull() as? AnnouncementState)?.let {
                holder.setIsRecyclable(false)
                holder.bind(it)
            }
                ?: super.onBindViewHolder(holder, position, payloads)
            else -> super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            PostListBottom -> BOTTOM
            is AnnouncementState -> TOP
            is PostState -> POST
            else -> -1
        }
    }

    override fun getItemCount() = hasTop + hasBottom + list.value!!.size
}