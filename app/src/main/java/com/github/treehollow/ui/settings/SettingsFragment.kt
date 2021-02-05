package com.github.treehollow.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.github.treehollow.R
import com.github.treehollow.base.DataBindingFragment
import com.github.treehollow.base.TreeHollowApplication
import com.github.treehollow.databinding.FragmentSettingsBinding
import com.github.treehollow.utils.Utils.launchCustomTab
import com.google.android.material.snackbar.Snackbar

class SettingsFragment : DataBindingFragment() {

    val model: SettingsViewModel by activityViewModels()
    private lateinit var fragmentContext: SettingsActivity

    private fun onCheckedChanged() {
        model.changePushType()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        fragmentContext = context as SettingsActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding: FragmentSettingsBinding by binding(
            inflater,
            R.layout.fragment_settings,
            container
        )
        val view = binding.root
        binding.apply {
            lifecycleOwner = this@SettingsFragment
            vm = model
        }


        binding.preferences.setOnClickListener {
            fragmentContext.replaceFragment(PreferenceFragment())
        }
        binding.contactUs.setOnClickListener {
            val mailAddr =
                TreeHollowApplication.Companion.Config.getConfigItemString("contact_email")!!
            sendEmail(mailAddr)
        }
//        binding.update.setOnClickListener {
//            Distribute.checkForUpdate()
//        }
        binding.logout.setOnClickListener {
            model.quitCurrentDevice()
        }

        binding.privacy.setOnClickListener {
            val url =
                TreeHollowApplication.Companion.Config.getConfigItemString("privacy_url")!!
            this.activity?.launchCustomTab(Uri.parse(url))
        }

        binding.tos.setOnClickListener {
            val url =
                TreeHollowApplication.Companion.Config.getConfigItemString("tos_url")!!
            this.activity?.launchCustomTab(Uri.parse(url))
        }

        binding.rules.setOnClickListener {
            val url =
                TreeHollowApplication.Companion.Config.getConfigItemString("rules_url")!!
            this.activity?.launchCustomTab(Uri.parse(url))
        }

        binding.changePw.setOnClickListener {
            val intent = Intent(activity, ChangePasswordActivity::class.java)
            startActivity(intent)
        }

        model.showNotification.observe(viewLifecycleOwner, { event ->
            if (event) {
                binding.pushFavorited.isClickable = true
                binding.pushFavorited.setOnCheckedChangeListener { _: CompoundButton, value: Boolean ->
                    model.pushFavorited.value = value
                    onCheckedChanged()
                }
                binding.pushReplyMe.isClickable = true
                binding.pushReplyMe.setOnCheckedChangeListener { _: CompoundButton, value: Boolean ->
                    model.pushReplyMe.value = value
                    onCheckedChanged()
                }
                binding.pushSystemMsg.isClickable = true
                binding.pushSystemMsg.setOnCheckedChangeListener { _: CompoundButton, value: Boolean ->
                    model.pushSystemMsg.value = value
                    onCheckedChanged()
                }
            }
        })


        model.errorMsg.observe(viewLifecycleOwner, { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                Log.e("SettingsFragment", it)
            }
        })
        model.infoMsg.observe(viewLifecycleOwner, { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
                Snackbar.make(view, it, Snackbar.LENGTH_SHORT).apply {
                    show()
                }
            }
        })
        return view
    }

    private fun sendEmail(addr: String) {
        val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", addr, null))
        try {
            startActivity(Intent.createChooser(emailIntent, "choose email client ..."))
        } catch (ex: android.content.ActivityNotFoundException) {
            Toast.makeText(activity, "There is no email client installed.", Toast.LENGTH_SHORT)
                .show()
        }
    }


}