package com.github.treehollow.ui.login

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import com.github.treehollow.R
import com.github.treehollow.base.DataBindingActivity
import com.github.treehollow.base.TreeHollowApplication
import com.github.treehollow.databinding.ActivityLoginBinding
import com.github.treehollow.ui.mainscreen.MainScreenActivity
import com.github.treehollow.utils.Utils
import com.jaredrummler.android.device.DeviceName
import com.skydoves.bundler.bundle
import com.skydoves.bundler.intentOf

class LoginActivity : DataBindingActivity() {
    private val email: String by bundle("email", "")

    @VisibleForTesting
    val model: LoginViewModel by viewModels()
    private val binding: ActivityLoginBinding by binding(R.layout.activity_login)

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        Utils.setActivityTheme(this)
        super.onCreate(savedInstanceState)

        binding.apply {
            lifecycleOwner = this@LoginActivity
            vm = model
        }

        val pw = binding.loginPasswordTextField

        DeviceName.init(this)

        binding.buttonLoginContinue.setOnClickListener {
            model.login(
                email,
                pw.editText?.text.toString(),
                DeviceName.getDeviceName()
            )
        }

        pw.setOnKeyListener { _, keyCode, event ->
            if ((event.action == KeyEvent.ACTION_DOWN) &&
                (keyCode == KeyEvent.KEYCODE_ENTER)
            ) {
                model.login(
                    email,
                    pw.editText?.text.toString(),
                    DeviceName.getDeviceName()
                )
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }

        model.result.observe(this@LoginActivity) {
            if (it.isNotEmpty()) {
                intentOf<MainScreenActivity> {
                    startActivity(this@LoginActivity)
                }
                finish()
            }
        }

        val email = TreeHollowApplication.Companion.Config.getConfigItemString("contact_email")!!
        binding.forgetPassword.text = "忘记密码？请联系${email}。"

        model.errorMsg.observe(this,
            { event ->
                event?.getContentIfNotHandledOrReturnNull()?.let {
                    Toast.makeText(applicationContext, it, Toast.LENGTH_SHORT).show()
                    Log.e("LoginActivity", it)
                }
            })
    }

}