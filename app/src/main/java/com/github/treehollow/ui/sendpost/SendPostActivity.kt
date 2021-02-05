package com.github.treehollow.ui.sendpost

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.github.treehollow.R
import com.github.treehollow.base.DataBindingActivity
import com.github.treehollow.base.TreeHollowApplication
import com.github.treehollow.databinding.ActivitySendPostBinding
import com.github.treehollow.utils.Utils
import kotlinx.coroutines.launch
import java.io.FileNotFoundException


class SendPostActivity : DataBindingActivity() {

    private val binding: ActivitySendPostBinding by binding(R.layout.activity_send_post)
    private val viewModel: DraftViewModel by viewModels()

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Utils.setActivityTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_post)

        val toolbar: Toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setHomeButtonEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
            } else {
                val data = Intent()
                data.putExtra("successfulSend", false)
                setResult(2, data)
                finish()
            }
        }
        binding.apply {
            lifecycleOwner = this@SendPostActivity
            vm = viewModel
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.draft_body, DraftEditFragment.newInstance())
                .commitNow()
        }

        val startForPickImageResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val intent = result.data
                    try {
                        Log.d("SendPostActivity", "intent: ${intent!!.data!!}")
                        val imageStream = contentResolver.openInputStream(intent.data!!)
                        val selectedImage = BitmapFactory.decodeStream(imageStream!!)
                        viewModel.addImage(selectedImage)
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                    }
                }
            }

        binding.draftAddImage.setOnClickListener {
            val photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            startForPickImageResult.launch(photoPickerIntent)
        }

        binding.draftAddVote.setOnClickListener {
            viewModel.toggleVote()
        }
        lifecycleScope.launch {
            val tmp =
                TreeHollowApplication.Companion.Config.getConfigItemStringList("sendable_tags")!!
            val mutable = ArrayList<String>()
            tmp.forEach { mutable.add(it) }
            mutable.add(0, "（无）")  // temporarily
            viewModel.availableTag.postValue(mutable)
            binding.submitDraft.setOnClickListener { _ ->
                viewModel.errorCode.observe(this@SendPostActivity, Observer {
                    if (it == null) {
                        return@Observer
                    }
                    if (it != viewModel.SUCCESS) {
                        Toast.makeText(
                            applicationContext,
                            "Failed to send post : $it, ${viewModel.responseMsg}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            applicationContext,
                            "Success",
                            Toast.LENGTH_SHORT
                        ).show()
                        val data = Intent()
                        data.putExtra("successfulSend", true)
                        setResult(2, data)
                        finish()
                    }
                })
                viewModel.errorMessage.observe(this@SendPostActivity, Observer { event ->
                    if (event == null) {
                        return@Observer
                    }
                    event.getContentIfNotHandledOrReturnNull()?.let {
                        Toast.makeText(applicationContext, it, Toast.LENGTH_SHORT).show()
                    }
                })
                // 避免重复发送
                if (!viewModel.isSending.value!!) {
                    viewModel.submit()
                }
            }
        }
    }
}