package com.github.treehollow.ui.settings

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.core.widget.doAfterTextChanged
import com.github.treehollow.R
import com.github.treehollow.base.DataBindingActivity
import com.github.treehollow.databinding.ActivityChangePasswordBinding
import com.github.treehollow.push.WebSocketService
import com.github.treehollow.ui.MainActivity
import com.github.treehollow.utils.Utils

class ChangePasswordActivity : DataBindingActivity() {
    @VisibleForTesting
    val model: ChangePasswordViewModel by viewModels()
    private val binding: ActivityChangePasswordBinding by binding(R.layout.activity_change_password)

    override fun onCreate(savedInstanceState: Bundle?) {
        Utils.setActivityTheme(this)
        super.onCreate(savedInstanceState)

        binding.apply {
            lifecycleOwner = this@ChangePasswordActivity
            vm = model
        }

        val originPassword = binding.originPassword
        val pw = binding.passwordTextField
        val pw2 = binding.confirmPasswordTextField

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

        binding.buttonContinue.setOnClickListener {
            if (pw.editText!!.text.toString().length < 8)
                Toast.makeText(applicationContext, "密码长度至少8位", Toast.LENGTH_SHORT).show()
            else if (pw2.editText!!.text.toString() != pw.editText?.text.toString())
                Toast.makeText(applicationContext, "密码不匹配", Toast.LENGTH_SHORT).show()
            else
                model.changePassword(
                    binding.emailInput.editText?.text.toString(),
                    originPassword.editText?.text.toString(),
                    pw.editText?.text.toString(),
                )
        }

        model.result.observe(this@ChangePasswordActivity) {
            if (it) {
                stopService(Intent(this, WebSocketService::class.java))
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
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