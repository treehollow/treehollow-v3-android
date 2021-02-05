package com.github.treehollow.ui.sendpost

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import com.github.treehollow.R
import com.github.treehollow.base.DataBindingFragment
import com.github.treehollow.databinding.FragmentDraftEditBinding
import com.google.android.material.snackbar.Snackbar


class DraftEditFragment : DataBindingFragment() {

    companion object {
        fun newInstance() = DraftEditFragment()
    }


    private val viewModel: DraftViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding: FragmentDraftEditBinding by binding(
            inflater,
            R.layout.fragment_draft_edit,
            container
        )
        val view = binding.root
        binding.apply {
            lifecycleOwner = this@DraftEditFragment
            vm = viewModel
        }
        viewModel.availableTag.observe(viewLifecycleOwner, {
            val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, it)
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)

            binding.draftTag.adapter = adapter
            binding.draftTag.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    viewModel.draftTag.value = it[id.toInt()]
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    viewModel.draftTag.value = null
                }
            }
        })
        binding.draftVoteOption1.setOnFocusChangeListener { _, it ->
            if (it) viewModel.expandVoteCancelOrConfirm()
        }
        binding.draftVoteOption2.setOnFocusChangeListener { _, it ->
            if (it) viewModel.expandVoteCancelOrConfirm()
        }
        binding.draftVoteOption3.setOnFocusChangeListener { _, it ->
            if (it) viewModel.expandVoteCancelOrConfirm()
        }
        binding.draftVoteOption4.setOnFocusChangeListener { _, it ->
            if (it) viewModel.expandVoteCancelOrConfirm()
        }
        binding.draftVoteOption1.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                if (s.isNotEmpty()) {
                    binding.draftVoteButton1.setImageResource(R.drawable.ic_cancel)
                } else {
                    binding.draftVoteButton1.setImageResource(R.drawable.ic_delete_option)
                }
            }
        })
        binding.draftVoteOption2.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                if (s.isNotEmpty()) {
                    binding.draftVoteButton2.setImageResource(R.drawable.ic_cancel)
                } else {
                    binding.draftVoteButton2.setImageResource(R.drawable.ic_delete_option)
                }
            }
        })
        binding.draftVoteOption3.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                if (s.isNotEmpty()) {
                    binding.draftVoteButton3.setImageResource(R.drawable.ic_cancel)
                } else {
                    binding.draftVoteButton3.setImageResource(R.drawable.ic_delete_option)
                }
            }
        })
        binding.draftVoteOption4.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                if (s.isNotEmpty()) {
                    binding.draftVoteButton4.setImageResource(R.drawable.ic_cancel)
                } else {
                    binding.draftVoteButton4.setImageResource(R.drawable.ic_delete_option)
                }
            }
        })
        binding.draftRemoveImage.setOnClickListener { viewModel.removeImage() }
        viewModel.draftImageShow.observe(viewLifecycleOwner, {
            if (it != null) {
                binding.draftImage.setImageBitmap(it)
            }
        })
        viewModel.imageVisible.observe(viewLifecycleOwner, {
            binding.draftImage.visibility = when (it) {
                true -> View.VISIBLE
                false -> View.GONE
            }
            binding.draftRemoveImage.visibility = binding.draftImage.visibility
            binding.draftImageWrapper.visibility = binding.draftImage.visibility
        })
        viewModel.voteEnabled.observe(viewLifecycleOwner, {
            binding.draftVote.visibility = when (it) {
                true -> View.VISIBLE
                false -> View.GONE
            }
        })

        viewModel.compressor.infoMsg.observe(viewLifecycleOwner, { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
                Snackbar.make(view, it, Snackbar.LENGTH_SHORT).apply {
                    show()
                }
            }
        })

        return view
    }

}