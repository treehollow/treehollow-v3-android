package com.github.treehollow.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import com.github.treehollow.R
import com.github.treehollow.base.DataBindingActivity
import com.github.treehollow.databinding.ActivityWelcomeBinding
import com.github.treehollow.utils.Const
import com.microsoft.appcenter.analytics.Analytics
import com.skydoves.bundler.intentOf


class WelcomeActivity : DataBindingActivity() {
    @VisibleForTesting
    val model: WelcomeViewModel by viewModels()
    private val binding: ActivityWelcomeBinding by binding(R.layout.activity_welcome)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.apply {
            lifecycleOwner = this@WelcomeActivity
            vm = model
        }
        val properties: MutableMap<String, String> = HashMap()
        binding.selectThu.setOnClickListener {
            properties["button"] = "thu"
            Analytics.trackEvent("Welcome_button", properties)
            model.loadConfig(Const.ConfigURLs.thu_hollow_config_url)
        }
        binding.selectPku.setOnClickListener {
            properties["button"] = "pku"
            Analytics.trackEvent("Welcome_button", properties)
            model.loadConfig(Const.ConfigURLs.pku_hollow_config_url)
        }
        binding.selectOther.setOnClickListener {
            properties["button"] = "others"
            Analytics.trackEvent("Welcome_button", properties)
            startActivity(Intent(this, OtherHollowActivity::class.java))
        }
        model.config.observe(this) {
            if (it != null) {
                intentOf<CheckEmailActivity> {
                    startActivity(this@WelcomeActivity)
                }
                finish()
            }
        }
        model.networkError.observe(this, { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
                Toast.makeText(applicationContext, it, Toast.LENGTH_SHORT).show()
                Log.e("WelcomeActivity", it)
            }
        })
    }
}