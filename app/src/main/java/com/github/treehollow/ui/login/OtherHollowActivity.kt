package com.github.treehollow.ui.login

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import com.github.treehollow.R
import com.github.treehollow.base.DataBindingActivity
import com.github.treehollow.databinding.ActivityOtherHollowBinding
import com.skydoves.bundler.intentOf

class OtherHollowActivity : DataBindingActivity() {
    @VisibleForTesting
    val model: OtherHollowViewModel by viewModels()
    private val binding: ActivityOtherHollowBinding by binding(R.layout.activity_other_hollow)

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding.apply {
            lifecycleOwner = this@OtherHollowActivity
            vm = model
        }
        binding.buttonContinue.setOnClickListener {
            model.loadConfig(binding.configTextField.editText!!.text.toString())
        }

        binding.configTextField.setOnKeyListener { _, keyCode, event ->
            if ((event.action == KeyEvent.ACTION_DOWN) &&
                (keyCode == KeyEvent.KEYCODE_ENTER)
            ) {
                model.loadConfig(binding.configTextField.editText!!.text.toString())
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }
        model.config.observe(this) {
            if (it != null) {
                intentOf<CheckEmailActivity> {
                    startActivity(this@OtherHollowActivity)
                }
                finish()
            }
        }
        model.networkError.observe(this, { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
                Toast.makeText(applicationContext, it, Toast.LENGTH_SHORT).show()
                Log.e("OtherHollowActivity", it)
            }
        })
    }
}