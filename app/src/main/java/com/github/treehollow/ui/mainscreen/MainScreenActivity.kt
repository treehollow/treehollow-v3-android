package com.github.treehollow.ui.mainscreen


import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.github.treehollow.base.Result
import com.github.treehollow.base.TreeHollowApplication
import com.github.treehollow.data.model.TreeHollowConfig
import com.github.treehollow.databinding.ActivityMainScreenBinding
import com.github.treehollow.network.ApiResponse
import com.github.treehollow.push.FcfrtAppBhUtils
import com.github.treehollow.push.WebSocketService
import com.github.treehollow.repository.*
import com.github.treehollow.ui.mainscreen.timeline.TimelineFragment
import com.github.treehollow.ui.messages.MessageActivity
import com.github.treehollow.ui.postdetail.PostDetailActivity
import com.github.treehollow.ui.search.SearchActivity
import com.github.treehollow.ui.sendpost.SendPostActivity
import com.github.treehollow.ui.settings.SettingsActivity
import com.github.treehollow.utils.MarkdownBuilder
import com.github.treehollow.utils.Utils
import com.github.treehollow.utils.Utils.launchCustomTab
import com.github.treehollow.utils.Utils.onlyRunOnce
import com.github.treehollow.utils.Version
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.microsoft.appcenter.analytics.Analytics
import com.skydoves.bundler.intentOf
import kotlinx.coroutines.launch

class MainScreenActivity : AppCompatActivity() {
    private val token: String = TreeHollowApplication.token!!

    private lateinit var viewPager: ViewPager2
    lateinit var binding: ActivityMainScreenBinding
    private var page: MutableLiveData<Int> = MutableLiveData(1)
    private lateinit var pagerAdapter: ScreenSlidePagerAdapter
    private var notWanderPage = TimeLinePage

    private companion object {
        const val TimeLinePage = 1
        const val SearchResultPage = 2
        const val FavoritesPage = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Utils.setActivityTheme(this)
        super.onCreate(savedInstanceState)
        Log.d("MainScreenActivity", "token: $token")

        onlyRunOnce("battery_prompt") {
            MaterialAlertDialogBuilder(this).apply {
                setTitle("提示")
                setMessage("受Android系统限制，树洞消息推送服务必须显示一条通知才可以持续在后台运行。")
                setPositiveButton("我知道了") { dialog: DialogInterface, _: Int ->

                    MaterialAlertDialogBuilder(this@MainScreenActivity).apply {
                        setTitle("提示")
                        setMessage(
                            "为了保持树洞消息推送服务在后台的稳定运行，请允许树洞app忽略Android系统的电池优化。" +
                                    "（树洞消息推送服务在后台运行时只会消耗几乎可忽略不及的电量。）"
                        )
                        setPositiveButton("我知道了") { dialog: DialogInterface, _: Int ->
                            if (!FcfrtAppBhUtils.isIgnoringBatteryOptimizations(this@MainScreenActivity))
                                FcfrtAppBhUtils.requestIgnoreBatteryOptimizations(this@MainScreenActivity)
                            dialog.cancel()
                        }
                        show()
                    }
                    dialog.cancel()
                }
                show()
            }
        }


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, WebSocketService::class.java))
        } else {
            startService(Intent(this, WebSocketService::class.java))
        }

        binding = ActivityMainScreenBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        viewPager = binding.viewPager2
        pagerAdapter = ScreenSlidePagerAdapter(this, token)
        viewPager.adapter = pagerAdapter
        val wanderText = binding.wanderText
        val timelineText = binding.timelineText
        viewPager.setPageTransformer(PageTransformer(wanderText, timelineText))
        val accountIcon = binding.accountIcon

//        Update config
        lifecycleScope.launch {
            val configUrl = TreeHollowApplication.Companion.Config.getConfigItemString("config_url")
            if (configUrl != null) {
                when (val result = TreeHollowApplication.Companion.Config.initConfig(
                    configUrl
                )) {
                    is Result.Success<TreeHollowConfig> -> {
                        (pagerAdapter.fragments[1] as TimelineFragment).updateAnnouncementAndNotify(
                            result.data.announcement
                        )


//                    check updates
                        val pInfo: PackageInfo =
                            TreeHollowApplication.instance.packageManager.getPackageInfo(
                                TreeHollowApplication.instance.packageName, 0
                            )
                        val version = pInfo.versionName
                        if (Version(result.data.android_frontend_version) > Version(version)) {
                            Log.d(
                                "MainScreenActivity",
                                "New version detected: ${result.data.android_frontend_version}, old version = $version"
                            )
                            MaterialAlertDialogBuilder(this@MainScreenActivity).apply {
                                setTitle("发现新版本：${result.data.android_frontend_version}")

                                setPositiveButton("立即更新") { dialog: DialogInterface, _: Int ->
                                    this@MainScreenActivity.launchCustomTab(Uri.parse(result.data.android_apk_download_url))
                                    dialog.cancel()
                                }
                                setNegativeButton("取消") { dialog: DialogInterface, _: Int ->
                                    dialog.cancel()
                                }
                                show()
                            }
                        }
//                        Do something
                    }
                    is Result.Error -> {
//                            this.stopSelf()
                    }
                }
            }

        }

        page.observe(this) {
            setTextViewSizes(wanderText, timelineText, (1 - viewPager.currentItem).toFloat())
            when (it) {
                0 -> {
                    viewPager.currentItem = 0
                }
                1 -> {
                    viewPager.currentItem = 1
                }
            }
        }

//        https://al-e-shevelev.medium.com/how-to-reduce-scroll-sensitivity-of-viewpager2-widget-87797ad02414
        val recyclerViewField = ViewPager2::class.java.getDeclaredField("mRecyclerView")
        recyclerViewField.isAccessible = true
        val recyclerView = recyclerViewField.get(binding.viewPager2) as RecyclerView
        val touchSlopField = RecyclerView::class.java.getDeclaredField("mTouchSlop")
        touchSlopField.isAccessible = true
        val touchSlop = touchSlopField.get(recyclerView) as Int
        touchSlopField.set(recyclerView, touchSlop * 6)

        wanderText.setOnClickListener {
            page.value = 0
        }
        timelineText.setOnClickListener {
            page.value = 1
            if (notWanderPage == SearchResultPage || notWanderPage == FavoritesPage) {
                val timelineFragment = pagerAdapter.fragments[1] as TimelineFragment
                timelineFragment.setFetcher(
                    PostListFetcher(
                        token,
                        MarkdownBuilder(applicationContext)
                    )
                )
            }
            notWanderPage = TimeLinePage
        }
        accountIcon.setOnClickListener {
            intentOf<SettingsActivity> {
                startActivity(this@MainScreenActivity)
            }
        }
        binding.favoritesIcon.setOnClickListener {
            Analytics.trackEvent("ClickFavorites")

            page.value = 1
            notWanderPage = FavoritesPage
            (pagerAdapter.fragments[1] as TimelineFragment).setFetcher(
                AttentionListFetcher(
                    token, MarkdownBuilder(applicationContext)
                )
            )
        }

        binding.trendingIcon.setOnClickListener {
            Analytics.trackEvent("ClickTrending")
            page.value = 1
            notWanderPage = SearchResultPage
            (pagerAdapter.fragments[1] as TimelineFragment).setFetcher(
                SearchResultListFetcher(
                    token, MarkdownBuilder(applicationContext),
                    "热榜", -1, -1,
                    true, "id"
                )
            )
        }

        binding.notificationsIcon.setOnClickListener {
            Analytics.trackEvent("ClickNotification")

            intentOf<MessageActivity> {
                startActivity(this@MainScreenActivity)
            }
        }
    }

    // send post

    fun onSendBarClicked() {
        startSendPostActivity.launch(Intent(this, SendPostActivity::class.java))
    }

    private val startSendPostActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            try {
                notWanderPage = TimeLinePage
                val tmp = it.data?.getSerializableExtra("successfulSend")
                if (tmp != null) {
                    val successfulSend = tmp as Boolean
                    if (successfulSend) {
                        // refresh the page
                        val timelineFragment = pagerAdapter.fragments[1] as TimelineFragment
                        timelineFragment.setFetcher(
                            PostListFetcher(
                                token,
                                MarkdownBuilder(applicationContext)
                            )
                        )
                    }
                }
            } catch (e: NullPointerException) {
            } finally {
            }
        }

    // search

    fun onSearchButtonClicked(str: String) {
        val intent = Intent(this, SearchActivity::class.java)
        intent.putExtra("keywords", str)
        startSearchActivity.launch(intent)
    }

    private val startSearchActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            Log.d("MainScreenActivity", "activityResult -> $it")
            try {
                notWanderPage = SearchResultPage
                val param = it.data?.getSerializableExtra("searchParameter") as SearchParameter
                if (param.valid) {

                    val searchResultListFetcher = SearchResultListFetcher(
                        token, MarkdownBuilder(applicationContext),
                        param.keywords, param.before, param.after,
                        param.includeComment, param.order
                    )
                    (pagerAdapter.fragments[1] as TimelineFragment).setFetcher(
                        searchResultListFetcher
                    )
                } else {
                    Log.d("MainScreenActivity", "parameter invalid.")
                }
            } catch (e: NullPointerException) {
                Log.d("MainScreenActivity", "search returned with no param.")
            } finally {
            }
        }


    // post detail

    fun startPostDetail(
        pid: Int,
        timelineIndex: Int?,
        postData: ApiResponse.Post?,
        commentData: ApiResponse.ListComments?,
        needRefresh: Boolean
    ) {
        val intent = Intent(this, PostDetailActivity::class.java)
        intent.putExtra("pid", pid)
        intent.putExtra("postData", postData)
        intent.putExtra("commentData", commentData)
        intent.putExtra("needRefresh", needRefresh)
        currentClickedTimelineIndex.value = timelineIndex
        startDetailPostActivity.launch(intent)
    }

    private val currentClickedTimelineIndex: MutableLiveData<Int?> = MutableLiveData(null)
    private val startDetailPostActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            Log.d("MainScreenActivity", "activityResult -> $it")
            try {
                val postData = it.data?.getSerializableExtra("postData") as ApiResponse.Post?
                if (postData != null) {
                    val adapter =
                        (pagerAdapter.fragments[binding.viewPager2.currentItem] as TimelineFragment).adapter
                    currentClickedTimelineIndex.value?.let {
                        adapter.list.value?.get(it - adapter.hasTop)?.post_data = postData
                        adapter.notifyItemChanged(it)
                    }
                } else {
                    Log.d("MainScreenActivity", "parameter invalid.")
                }
            } catch (e: NullPointerException) {
                Log.d("MainScreenActivity", "search returned with no param.")
            } finally {
            }
        }


    // detail
    fun simpleDetailActivityRedirect(pid: Int?) {
        val intent = Intent(this, PostDetailActivity::class.java)
        intent.putExtra("pid", pid ?: -1)
        startSimpleDetailActivity.launch(intent)
    }

    private val startSimpleDetailActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            try {
                Log.d("MainScreenActivity", "Return.")
            } catch (e: NullPointerException) {
            } finally {
            }
        }

    private inner class ScreenSlidePagerAdapter(
        fa: AppCompatActivity,
        token: String
    ) : FragmentStateAdapter(fa) {
        val fragments: Array<Fragment> = arrayOf(
            TimelineFragment.newInstance(
                0,
                PostRandomListFetcher(token, MarkdownBuilder(applicationContext))
            ),
            TimelineFragment.newInstance(
                1,
                PostListFetcher(token, MarkdownBuilder(applicationContext))
            )
        )

        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment = fragments[position]
    }
}