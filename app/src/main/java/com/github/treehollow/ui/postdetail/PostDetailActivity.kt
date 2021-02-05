package com.github.treehollow.ui.postdetail

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import com.github.treehollow.R
import com.github.treehollow.base.DataBindingActivity
import com.github.treehollow.databinding.ActivityPostDetailBinding
import com.github.treehollow.network.ApiResponse
import com.github.treehollow.utils.Utils
import com.skydoves.bundler.bundle
import com.skydoves.bundler.bundleNonNull
import com.skydoves.bundler.intentOf

class PostDetailActivity : DataBindingActivity() {

    private val pid: Int by bundleNonNull("pid")
    private val initComments: ApiResponse.ListComments? by bundle("commentData")
    private val initPost: ApiResponse.Post? by bundle("postData")
    private val needRefresh: Boolean? by bundle("needRefresh")
    private val binding: ActivityPostDetailBinding by binding(R.layout.activity_post_detail)
    private val startDetailActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        Utils.setActivityTheme(this)
        super.onCreate(savedInstanceState)
        binding.apply {
            lifecycleOwner = this@PostDetailActivity
        }
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.fragment_container_view, PostDetailFragment.newInstance(
                        pid, initPost, initComments, needRefresh
                    )
                )
                .commitNow()
        }
        binding.toolbar.setNavigationOnClickListener {
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
            } else {
                onFinish()
            }
        }
    }

    override fun onBackPressed() {
//        super.onBackPressed()
        onFinish()
    }

    private fun onFinish() {
        val data = Intent()
        val fragment: PostDetailFragment = supportFragmentManager.fragments[0] as PostDetailFragment
        data.putExtra("postData", fragment.adapter.post.value?.post_data)
        setResult(2, data)
        finish()
    }

    // detail
    fun redirectToPostDetail(pid: Int?) {
        val intent = Intent(this, PostDetailActivity::class.java)
        intent.putExtra("pid", pid ?: -1)
        startDetailActivity.launch(intent)
    }

    fun viewImage(imgUrl: String) {
        intentOf<ImageDetailActivity> {
            putExtra(
                "image_url" to imgUrl
            )
            startActivity(this@PostDetailActivity)
        }
    }
}