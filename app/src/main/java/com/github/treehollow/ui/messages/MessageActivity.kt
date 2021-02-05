package com.github.treehollow.ui.messages

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import com.github.treehollow.R
import com.github.treehollow.base.DataBindingActivity
import com.github.treehollow.databinding.ActivityMessageBinding
import com.github.treehollow.ui.postdetail.PostDetailActivity
import com.github.treehollow.utils.Utils

class MessageActivity : DataBindingActivity() {

    private val binding: ActivityMessageBinding by binding(R.layout.activity_message)

    override fun onCreate(savedInstanceState: Bundle?) {
        Utils.setActivityTheme(this)
        super.onCreate(savedInstanceState)
        binding.apply {
            lifecycleOwner = this@MessageActivity
        }
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_view, MessageFragment.newInstance())
                .commitNow()
        }
        binding.toolbar.setNavigationOnClickListener {
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
            } else {
                finish()
            }
        }
    }

    // detail
    fun onPostCardClicked(pid: Int?) {
        val intent = Intent(this, PostDetailActivity::class.java)
        intent.putExtra("pid", pid ?: -1)
        startDetailActivity.launch(intent)
    }

    private val startDetailActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            try {
                Log.d("MessageActivity", "Return.")
            } catch (e: NullPointerException) {
            } finally {
            }
        }

}