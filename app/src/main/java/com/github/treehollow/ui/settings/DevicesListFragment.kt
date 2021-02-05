package com.github.treehollow.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.treehollow.R
import com.github.treehollow.databinding.FragmentDeviceListBinding

class DevicesListFragment : Fragment() {
    private var _binding: FragmentDeviceListBinding? = null
    private val binding get() = _binding!!
    val model: SettingsViewModel by activityViewModels()

    private val adapter = DeviceRecyclerViewAdapter(deviceInit = {
        this.device?.model = this@DevicesListFragment.model
    })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeviceListBinding.inflate(inflater, container, false)
        val view = binding.root
        model.deviceListAdapter = adapter
        model.isRefreshing.observe(viewLifecycleOwner) {
            binding.swipeRefresh.isRefreshing = it
        }
        model.getDevices()
        binding.deviceListRecycle.apply {
            this.adapter = this@DevicesListFragment.adapter
            layoutManager = LinearLayoutManager(context)
            val itemDec = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            val draw = ContextCompat.getDrawable(context, R.drawable.padding)
                ?: throw IllegalArgumentException("Cannot load drawable")
            itemDec.setDrawable(draw)
            addItemDecoration(itemDec)

        }
        binding.swipeRefresh.setOnRefreshListener {
            model.getDevices()
        }
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}