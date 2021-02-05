package com.github.treehollow.ui.login

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import com.github.treehollow.R
import com.github.treehollow.base.DataBindingActivity
import com.github.treehollow.base.TreeHollowApplication
import com.github.treehollow.databinding.ActivityCheckEmailBinding
import com.github.treehollow.utils.Utils
import com.skydoves.bundler.intentOf

class CheckEmailActivity : DataBindingActivity() {

    @VisibleForTesting
    val model: CheckEmailViewModel by viewModels()
    private val binding: ActivityCheckEmailBinding by binding(R.layout.activity_check_email)

    override fun onCreate(savedInstanceState: Bundle?) {
        Utils.setActivityTheme(this)
        super.onCreate(savedInstanceState)
        binding.apply {
            lifecycleOwner = this@CheckEmailActivity
            vm = model
        }
        val emailSuffixTextField = binding.emailSuffixMenu
        val emailTextField = binding.emailTextField

        val emailSuffixes =
            TreeHollowApplication.Companion.Config.getConfigItemStringList("email_suffixes")!!
        val items = emailSuffixes.map { "@$it" }
        val adapter = ArrayAdapter(this@CheckEmailActivity, R.layout.list_item, items)
        (emailSuffixTextField.editText as? AutoCompleteTextView)?.setAdapter(adapter)
        (emailSuffixTextField.editText as? AutoCompleteTextView)?.setText(
            items[0],
            false
        )

        binding.buttonCheckEmailContinue.setOnClickListener {
            model.checkEmail(
                emailTextField.editText?.text.toString(),
                emailSuffixTextField.editText?.text.toString()
            )
        }

        model.result.observe(this) {
            when (it) {
                CheckEmailViewModel.CheckEmailResponseCode.Login.code -> {
                    intentOf<LoginActivity> {
                        putExtra(
                            "email" to
                                    emailTextField.editText?.text.toString() +
                                    emailSuffixTextField.editText?.text.toString()
                        )
                        startActivity(this@CheckEmailActivity)
                    }
                }
                CheckEmailViewModel.CheckEmailResponseCode.Register.code -> {
                    intentOf<RegisterActivity> {
                        putExtra(
                            "email" to
                                    emailTextField.editText?.text.toString() +
                                    emailSuffixTextField.editText?.text.toString()
                        )
                        startActivity(this@CheckEmailActivity)
                    }
                }
                CheckEmailViewModel.CheckEmailResponseCode.NeedRecaptcha.code -> {
                    intentOf<RecaptchaActivity> {
                        putExtra(
                            "email" to
                                    emailTextField.editText?.text.toString() +
                                    emailSuffixTextField.editText?.text.toString()
                        )
                        startActivity(this@CheckEmailActivity)
                    }
                }
            }
        }

        model.errorMsg.observe(this, { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
                Toast.makeText(applicationContext, it, Toast.LENGTH_SHORT).show()
                Log.e("CheckEmailActivity", it)
            }
        })
    }
}