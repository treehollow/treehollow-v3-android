package com.github.treehollow.ui.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.github.treehollow.data.DeviceListElem
import com.github.treehollow.data.DeviceState
import com.github.treehollow.databinding.CardDeviceBinding

class DeviceRecyclerViewAdapter(
    private val deviceInit: CardDeviceBinding.() -> Unit,
) : RecyclerView.Adapter<DeviceRecyclerViewAdapter.ViewHolder>() {
//    companion object {
//        const val POST = 0
//        const val BOTTOM = 2
//    }

    val list = MutableLiveData(listOf<DeviceState>())

    sealed class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        class DeviceCard(
            private val binding: CardDeviceBinding,
            private val init: CardDeviceBinding.() -> Unit,
        ) : ViewHolder(binding.root) {
            fun bind(item: DeviceState) {
                binding.apply {
                    device = item
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
        val binding = CardDeviceBinding.inflate(inflater, parent, false)
        return ViewHolder.DeviceCard(
            binding,
            deviceInit
        )
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        when (holder) {
            is ViewHolder.DeviceCard -> {
                holder.setIsRecyclable(false)
                holder.bind(getItem(position) as DeviceState)
            }
        }
    }

    fun getItem(position: Int): DeviceListElem {
        return list.value!![position]
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (holder is ViewHolder.DeviceCard)
            (payloads.singleOrNull() as? DeviceState)?.let {
                holder.setIsRecyclable(false)
                holder.bind(it)
            }
                ?: super.onBindViewHolder(holder, position, payloads)
        else super.onBindViewHolder(holder, position, payloads)
    }

    override fun getItemViewType(position: Int): Int {
        return 0
    }

    override fun getItemCount() = list.value!!.size
}