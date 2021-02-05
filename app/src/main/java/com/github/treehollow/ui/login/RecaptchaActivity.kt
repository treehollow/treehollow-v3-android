package com.github.treehollow.ui.login

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import com.github.treehollow.R
import com.github.treehollow.base.DataBindingActivity
import com.github.treehollow.base.TreeHollowApplication
import com.github.treehollow.databinding.ActivityRecaptchaBinding
import com.github.treehollow.utils.Utils
import com.skydoves.bundler.bundle
import com.skydoves.bundler.intentOf


class RecaptchaActivity : DataBindingActivity() {
    @VisibleForTesting
    val model: RecaptchaViewModel by viewModels()
    private val binding: ActivityRecaptchaBinding by binding(R.layout.activity_recaptcha)

    private val email: String by bundle("email", "")


    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        Utils.setActivityTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recaptcha)

        binding.apply {
            lifecycleOwner = this@RecaptchaActivity
            vm = model
        }

        model.errorMsg.observe(this, { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
                Toast.makeText(applicationContext, it, Toast.LENGTH_SHORT).show()
                Log.e("RecaptchaActivity", it)
            }
        })


        model.result.observe(this) {
            when (it) {
                CheckEmailViewModel.CheckEmailResponseCode.Login.code -> {
                    intentOf<LoginActivity> {
                        putExtra(
                            "email" to email
                        )
                        startActivity(this@RecaptchaActivity)
                    }
                }
                CheckEmailViewModel.CheckEmailResponseCode.Register.code -> {
                    intentOf<RegisterActivity> {
                        putExtra(
                            "email" to email
                        )
                        startActivity(this@RecaptchaActivity)
                    }
                }
            }
        }

        val myWebView: WebView = findViewById(R.id.recaptcha_web_view)
        val recaptchaUrl =
            TreeHollowApplication.Companion.Config.getConfigItemString("recaptcha_url")!!
        myWebView.loadUrl(recaptchaUrl)
        val webSettings = myWebView.settings
        webSettings.javaScriptEnabled = true

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val nightModeFlags =
                resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                webSettings.forceDark = WebSettings.FORCE_DARK_ON
            }
        }

        myWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                model.isLoading.value = false
                super.onPageFinished(view, url)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                url: String?
            ): Boolean {
                if (url != null) {
                    try {
                        model.checkEmail(
                            email,
                            url.split("recaptcha_token=")[1]
                        )
                    } catch (e: Exception) {
                    }
                }
                return true
            }
        }
    }
}