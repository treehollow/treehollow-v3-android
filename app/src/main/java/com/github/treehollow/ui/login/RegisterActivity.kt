package com.github.treehollow.ui.login

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.core.widget.doAfterTextChanged
import com.github.treehollow.R
import com.github.treehollow.base.DataBindingActivity
import com.github.treehollow.base.TreeHollowApplication
import com.github.treehollow.databinding.ActivityRegisterBinding
import com.github.treehollow.ui.mainscreen.MainScreenActivity
import com.github.treehollow.utils.Utils
import com.jaredrummler.android.device.DeviceName
import com.skydoves.bundler.bundle
import com.skydoves.bundler.intentOf

class RegisterActivity : DataBindingActivity() {
    private val email: String by bundle("email", "")

    @VisibleForTesting
    val model: RegisterViewModel by viewModels()
    private val binding: ActivityRegisterBinding by binding(R.layout.activity_register)

    override fun onCreate(savedInstanceState: Bundle?) {
        Utils.setActivityTheme(this)
        super.onCreate(savedInstanceState)

        binding.apply {
            lifecycleOwner = this@RegisterActivity
            vm = model
        }

        val pw = binding.registerPasswordTextField
        val pw2 = binding.registerConfirmPasswordTextField
        val checkbox = binding.registerAgreeCheckbox
        val validCode = binding.validCodeTextField

        DeviceName.init(this)

        pw.editText?.doAfterTextChanged { text ->
            if (text.toString().length < 8) {
                pw.error = "密码长度至少8位"
            } else {
                pw.error = null
            }
        }
        pw2.editText?.doAfterTextChanged { text ->
            if (text.toString() != pw.editText?.text.toString()) {
                pw2.error = "密码不匹配"
            } else {
                pw2.error = null
            }
        }

        val tosUrl = TreeHollowApplication.Companion.Config.getConfigItemString("tos_url")!!
        val privacyUrl = TreeHollowApplication.Companion.Config.getConfigItemString("privacy_url")!!
        val rulesUrl = TreeHollowApplication.Companion.Config.getConfigItemString("rules_url")!!
        Utils.addClickableSpan(
            checkbox,
            "我已经阅读并同意了" +
                    " <a href='${tosUrl}'>服务协议</a>、" +
                    "<a href='${privacyUrl}'>隐私政策</a>和<a href='${rulesUrl}'>社区规范</a>",
            Utils.HtmlAnchorClickListenerImpl(this)
        )

        binding.buttonRegisterContinue.setOnClickListener {
            if (pw.editText!!.text.toString().length < 8)
                Toast.makeText(applicationContext, "密码长度至少8位", Toast.LENGTH_SHORT).show()
            else if (pw2.editText!!.text.toString() != pw.editText?.text.toString())
                Toast.makeText(applicationContext, "密码不匹配", Toast.LENGTH_SHORT).show()
            else if (!checkbox.isChecked)
                Toast.makeText(applicationContext, "请先同意条款", Toast.LENGTH_SHORT).show()
            else
                model.register(
                    email,
                    pw.editText?.text.toString(),
                    DeviceName.getDeviceName(),
                    validCode.editText?.text.toString()
                )
        }

        model.result.observe(this@RegisterActivity) {
            if (it.isNotEmpty()) {
                intentOf<MainScreenActivity> {
                    startActivity(this@RegisterActivity)
                }
                finish()
            }
        }


        model.errorMsg.observe(this,
            { event ->
                event?.getContentIfNotHandledOrReturnNull()?.let {
                    Toast.makeText(applicationContext, it, Toast.LENGTH_SHORT).show()
                    Log.e("RegisterActivity", it)
                }
            })
    }
}