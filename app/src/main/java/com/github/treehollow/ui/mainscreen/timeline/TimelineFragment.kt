package com.github.treehollow.ui.mainscreen.timeline

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.github.treehollow.R
import com.github.treehollow.base.TreeHollowApplication
import com.github.treehollow.data.*
import com.github.treehollow.databinding.FragmentTimelineBinding
import com.github.treehollow.network.ApiResponse
import com.github.treehollow.repository.PostListFetcher
import com.github.treehollow.repository.PostRandomListFetcher
import com.github.treehollow.repository.SearchResultListFetcher
import com.github.treehollow.ui.mainscreen.MainScreenActivity
import com.github.treehollow.utils.MarkdownBuilder
import com.github.treehollow.utils.Utils
import com.github.treehollow.utils.Utils.calculateCompressScale
import com.github.treehollow.utils.Utils.compressDisplayImage
import com.github.treehollow.utils.Utils.launchCustomTab
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.microsoft.appcenter.analytics.Analytics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*


class TimelineFragment : Fragment() {
    companion object {
        fun newInstance(i: Int, fetcher: PostFetcher): TimelineFragment {
            val frag = TimelineFragment()
            frag.i = i
            frag.initFetcher = fetcher
            frag.adapter.hasTop = i
            return frag
        }
    }

    var i: Int = -1
    var initFetcher: PostFetcher? = null

    private var _binding: FragmentTimelineBinding? = null
    private val binding get() = _binding!!
    private val token: String = TreeHollowApplication.token!!

    // Adapter
    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    val adapter = TimelineRecyclerViewAdapter(postInit = {
////        Change margin
//        if (post!!.environment == RandomListEnv) {
//            val p = postCard.layoutParams as StaggeredGridLayoutManager.LayoutParams
//            if (p.spanIndex == 0)
//                p.setMargins(p.leftMargin, p.topMargin, p.rightMargin / 2, p.bottomMargin)
//            else
//                p.setMargins(p.leftMargin / 2, p.topMargin, p.rightMargin, p.bottomMargin)
//            postCard.requestLayout()
//        }
//        Add image
        if (post!!.hasImage()) {
            val scale = calculateCompressScale(
                post!!.post_data.image_metadata.w!!,
                post!!.post_data.image_metadata.h!!,
            )
            if (post!!.post_data.image_metadata.w != null) {
                val placeHolderBitmap = Bitmap.createBitmap(
                    scale.w, scale.h,
                    Bitmap.Config.ARGB_8888
                )
                val can = Canvas(placeHolderBitmap)
                can.drawColor(Color.GRAY)
                postImage.setImageBitmap(placeHolderBitmap)
            }
            val imgBaseUrls =
                TreeHollowApplication.Companion.Config.getConfigItemStringList("img_base_urls")!!
            Glide.with(this@TimelineFragment)
                .asBitmap()
                .load(imgBaseUrls[0] + this.post!!.post_data.url)
                .error(ColorDrawable(Color.RED))
                .placeholder(R.drawable.ic_baseline_image_24)
                .override(scale.w, scale.h)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        val tmp = compressDisplayImage(resource)
                        postImage.setImageBitmap(tmp)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                    }
                })
        } else {
            Glide.with(this@TimelineFragment).clear(this.postImage)
        }

//        Handle comments
        if (!post!!.hasNoComments()) {
            val adapter = SimpleReplyCardAdapter(replyInit = {
                expanded2.setOnTouchListener { _, event -> postCard.onTouchEvent(event) }
            }, bottomInit = {
            })
            adapter.list.value = post!!.comments
            recyclerComments.apply {
                this.adapter = adapter
                layoutManager = LinearLayoutManager(context)
            }
        }

//        Handle votes
        if (!post!!.hasNoVotes()) {
            val vote = post!!.post_data.vote
            val adapter = VoteAdapter(voteInit = {
                if (vote.voted == "") {
                    voteLayout.setOnClickListener {
                        model.doVote(post!!, this.vote!!.option)
                    }
                } else {
                    if (vote.voted == this.vote!!.option) {
                        checked.visibility = View.VISIBLE
                    }
                }
            })
            adapter.list.value = vote.vote_options!!.map {
                VoteState(
                    option = it,
                    count = vote.vote_data!![it]!!,
                    selected = vote.voted == it
                )
            }
            recyclerVotes.apply {
                this.adapter = adapter
                layoutManager = LinearLayoutManager(context)
            }
        }

//        Handle star
        this.starButton.setOnClickListener {
            model.star(post!!, if (post!!.post_data.attention) 0 else 1)
        }

//        Handle tag
        if (post!!.environment == PostState.RandomListEnv) {
            if (post!!.post_data.tag != null) {
                this.postTagRandom.text = post!!.post_data.tag
            }
        } else if (post!!.environment == PostState.TimelineEnv) {
            if (post!!.post_data.tag != null) {
                this.postTagTimeline.text = post!!.post_data.tag
            }
        }
//        Quote
        if (!post!!.hasNoQuote()) {
            quotePostCard.setOnClickListener {
                val tmp = post?.quoteId
                (this@TimelineFragment.activity as MainScreenActivity).simpleDetailActivityRedirect(
                    tmp
                )
            }
        }


//        Markdown
        postContent.text = post!!.md
        postContent.setOnTouchListener { _, event -> postCard.onTouchEvent(event) }

//        onClick
        postCard.setOnClickListener {
            val tmp = post!!.post_data.pid
            (this@TimelineFragment.activity as MainScreenActivity).startPostDetail(
                tmp,
                post?.index,
                post!!.post_data,
                post!!.comments?.let { ApiResponse.ListComments(it.map { c -> c.comment_data }) },
                (post!!.post_data.reply != post!!.comments?.size) && post!!.post_data.reply != 0
            )
        }
//        Avatar
        postAvatar.setImageBitmap(post!!.avatar)

//        Redirects
        if (post!!.haveRedirectOtherThanQuote()) {
            if (post!!.redirects.size == 1) {
                val tmp = post!!.redirects[0]
                redirectsText.text = "跳转到${Utils.trimString(tmp.toString(), 30)}"
                redirectsCard.setOnClickListener {
                    if (tmp is LinkRedirect)
                        this@TimelineFragment.activity?.launchCustomTab(Uri.parse(tmp.toHttpUrl()))
                    else if (tmp is PostRedirect) {
                        (this@TimelineFragment.activity as MainScreenActivity).simpleDetailActivityRedirect(
                            tmp.pid
                        )
                    }
                }
            } else
                redirectsCard.setOnClickListener {
                    MaterialAlertDialogBuilder(requireContext()).apply {
                        val items =
                            post!!.redirects.map { redirect -> redirect.toString() }.toTypedArray()
                        setItems(items) { _: DialogInterface, i: Int ->
                            val tmp = post!!.redirects[i]
                            if (tmp is LinkRedirect)
                                this@TimelineFragment.activity?.launchCustomTab(Uri.parse(tmp.toHttpUrl()))
                            else if (tmp is PostRedirect) {
                                (this@TimelineFragment.activity as MainScreenActivity).simpleDetailActivityRedirect(
                                    tmp.pid
                                )
                            }
                        }
                        show()
                    }
                }
        }
    }, topInit = {
        if (this.announcement!!.redirects.isNotEmpty()) {
            if (this.announcement!!.redirects.size == 1) {
                val tmp = this.announcement!!.redirects[0]
                redirectsText.text = "跳转到${Utils.trimString(tmp.toString(), 30)}"
                redirectsCard.setOnClickListener {
                    if (tmp is LinkRedirect)
                        this@TimelineFragment.activity?.launchCustomTab(Uri.parse(tmp.toHttpUrl()))
                    else if (tmp is PostRedirect) {
                        (this@TimelineFragment.activity as MainScreenActivity).simpleDetailActivityRedirect(
                            tmp.pid
                        )
                    }
                }
            } else
                redirectsCard.setOnClickListener {
                    MaterialAlertDialogBuilder(requireContext()).apply {
                        val items =
                            this@TimelineRecyclerViewAdapter.announcement!!.redirects.map { redirect -> redirect.toString() }
                                .toTypedArray()
                        setItems(items) { _: DialogInterface, i: Int ->
                            val tmp = this@TimelineRecyclerViewAdapter.announcement!!.redirects[i]
                            if (tmp is LinkRedirect)
                                this@TimelineFragment.activity?.launchCustomTab(Uri.parse(tmp.toHttpUrl()))
                            else if (tmp is PostRedirect) {
                                (this@TimelineFragment.activity as MainScreenActivity).simpleDetailActivityRedirect(
                                    tmp.pid
                                )
                            }
                        }
                        show()
                    }
                }
        }

        cancelCard.setOnClickListener {
            TreeHollowApplication.Companion.Config.setConfigItemString(
                "saved_announcement",
                this.announcement!!.text
            )
            setAnnnouncement(AnnouncementState(""))
        }
    }, bottomInit = {
        loadMore.setOnClickListener { model.more() }
        networkError.setOnClickListener { model.more() }
        model.bottom.observe(this@TimelineFragment.viewLifecycleOwner) {
            fun visibility(b: Boolean) = if (b) View.VISIBLE else View.INVISIBLE
            binding.apply {
                loadMore.visibility = visibility(it == Utils.BottomStatus.IDLE)
                noMore.visibility = visibility(it == Utils.BottomStatus.NO_MORE)
                networkError.visibility = visibility(it == Utils.BottomStatus.NETWORK_ERROR)
                bottomLoading.visibility = visibility(it == Utils.BottomStatus.REFRESHING)
            }
        }
    }, i)

    private val model: TimelineViewModel by viewModels {
        TimelineViewModelFactory(initFetcher, adapter)
    }

    fun setFetcher(fetcher: PostFetcher?) {
        if (fetcher is SearchResultListFetcher) {
            val keywords = fetcher.keywords
            binding.searchText.text = keywords
        } else {
            binding.searchText.text = ""
        }
        model.setFetcher(fetcher)
        updateAnnouncement()
        model.refresh()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimelineBinding.inflate(inflater, container, false)
        val view = binding.root
//        if (i == 0) {
//            view.setBackgroundColor(Color.BLUE)
//        }

        updateAnnouncement()

        model.errorMsg.observe(viewLifecycleOwner, { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                Log.e("TimelineFragment", it)
            }
        })

        model.infoMsg.observe(viewLifecycleOwner, { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
                Snackbar.make(view, it, Snackbar.LENGTH_SHORT).apply {
                    show()
                }
            }
        })

        model.isRefreshing.observe(viewLifecycleOwner) {
            binding.swipeRefresh.isRefreshing = it
        }

        adapter.list.observe(viewLifecycleOwner) {
            if (it.isNotEmpty() || model.bottom.value == Utils.BottomStatus.NO_MORE || model.bottom.value == Utils.BottomStatus.NETWORK_ERROR)
                adapter.hasBottom = 1
            else
                adapter.hasBottom = 0
//            adapter.notifyDataSetChanged()
        }

        binding.bottomSend.setOnClickListener {
            Analytics.trackEvent("ClickSend")
            (this@TimelineFragment.activity as MainScreenActivity).onSendBarClicked()
        }

        val shortAnimationDuration = resources.getInteger(android.R.integer.config_shortAnimTime)
        binding.fabRefresh.setOnClickListener {
            val properties: MutableMap<String, String> = HashMap()
            properties["page"] = i.toString()
            Analytics.trackEvent("FabRefreshTimeline", properties)

            context?.let { it1 ->
                if (i == 0) {
                    if (with(binding.timelineRecycle.layoutManager as StaggeredGridLayoutManager) {
                            findFirstVisibleItemPositions(IntArray(2) { it })[0] >= 25
                        } || with(binding.timelineRecycle.layoutManager as StaggeredGridLayoutManager) {
                            findFirstVisibleItemPositions(IntArray(2) { it })[1] >= 25
                        }) {
                        model.setFetcher(PostRandomListFetcher(token, MarkdownBuilder(it1)))
                        model.refresh()
                    } else {
                        binding.timelineRecycle.smoothScrollToPosition(0)
                        model.viewModelScope.launch {
                            delay(500)
                            model.setFetcher(
                                PostRandomListFetcher(
                                    token, MarkdownBuilder(it1)
                                )
                            )
                            model.refresh()
                        }
                    }
                } else {
                    if (with(binding.timelineRecycle.layoutManager as LinearLayoutManager) {
                            findFirstCompletelyVisibleItemPosition() >= 25
                        }) {
                        model.setFetcher(PostListFetcher(token, MarkdownBuilder(it1)))
                        binding.searchText.text = ""
                        updateAnnouncement()
                        model.refresh()
                    } else {
                        binding.timelineRecycle.smoothScrollToPosition(0)
                        model.viewModelScope.launch {
                            delay(500)
                            model.setFetcher(PostListFetcher(token, MarkdownBuilder(it1)))
                            binding.searchText.text = ""
                            updateAnnouncement()
                            model.refresh()
                        }
                    }
                }
            }
        }

        binding.topSearch.setOnClickListener {
            Analytics.trackEvent("ClickSearch")
            (this@TimelineFragment.activity as MainScreenActivity).onSearchButtonClicked(binding.searchText.text.toString())
        }
        if (i == 0)
            binding.topSearch.visibility = View.GONE

        binding.timelineRecycle.apply {
            layoutManager = if (i == 0)
                StaggeredGridLayoutManager(2, 1)
            else
                LinearLayoutManager(context)
            this.adapter = this@TimelineFragment.adapter
            with(itemAnimator as SimpleItemAnimator) { supportsChangeAnimations = false }

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                var isAnimating = false
                var deltaY = 0
                val thresholeY = 200

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE &&
                        model.bottom.value == Utils.BottomStatus.IDLE &&
                        ((i == 1 && (layoutManager as LinearLayoutManager).run {
                            findLastVisibleItemPosition() >= itemCount - 6
                        }) || (i == 0 && (layoutManager as StaggeredGridLayoutManager).run {
                            findLastVisibleItemPositions(IntArray(2) { it })[0] >= itemCount - 6
                        }) || (i == 0 && (layoutManager as StaggeredGridLayoutManager).run {
                            findLastVisibleItemPositions(IntArray(2) { it })[1] >= itemCount - 6
                        }))
                    ) model.more()
                }

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (isAnimating)
                        return
                    deltaY += dy
                    if (deltaY * dy < 0) {
                        deltaY = 0
                    }
                    if (deltaY > thresholeY && binding.bottomSend.visibility == View.VISIBLE) {
                        binding.fabRefresh.hide()
                        isAnimating = true
                        deltaY = 0
                        binding.bottomSend.animate()
                            .alpha(0f)
                            .setDuration(shortAnimationDuration.toLong())
                            .setListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    binding.bottomSend.visibility = View.GONE
                                    isAnimating = false
                                }
                            })
                        if (this@TimelineFragment.adapter.hasTop == 1)
                            binding.topSearch.animate()
                                .alpha(0f)
                                .setDuration(shortAnimationDuration.toLong())
                                .setListener(object : AnimatorListenerAdapter() {
                                    override fun onAnimationEnd(animation: Animator) {
                                        binding.topSearch.visibility = View.GONE
                                        isAnimating = false
                                    }
                                })
                    } else if (deltaY < -thresholeY && binding.bottomSend.visibility != View.VISIBLE) {
                        binding.fabRefresh.show()
                        isAnimating = true
                        deltaY = 0
                        binding.bottomSend.visibility = View.VISIBLE
                        binding.bottomSend.animate()
                            .alpha(1f)
                            .setDuration(shortAnimationDuration.toLong())
                            .setListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    isAnimating = false
                                }
                            })
                        if (this@TimelineFragment.adapter.hasTop == 1) {
                            binding.topSearch.visibility = View.VISIBLE
                            binding.topSearch.animate()
                                .alpha(1f)
                                .setDuration(shortAnimationDuration.toLong())
                                .setListener(object : AnimatorListenerAdapter() {
                                    override fun onAnimationEnd(animation: Animator) {
                                        isAnimating = false
                                    }
                                })
                        }
                    }
                }
            })

            addItemDecoration(GridSpacingItemDecoration(36))
        }

        binding.swipeRefresh.setOnRefreshListener {
            val properties: MutableMap<String, String> = HashMap()
            properties["page"] = i.toString()
            Analytics.trackEvent("SwipeRefreshTimeline", properties)
            context?.let {
                model.setFetcher(
                    if (i == 0) {
                        PostRandomListFetcher(token, MarkdownBuilder(it))
                    } else {
                        PostListFetcher(token, MarkdownBuilder(it))
                    }
                )
                binding.searchText.text = ""
                updateAnnouncement()
                model.refresh()
            }
        }
        return view
    }

    private fun updateAnnouncement() {
        val announcement =
            TreeHollowApplication.Companion.Config.getConfigItemString("announcement")
        val savedAnnouncement =
            TreeHollowApplication.Companion.Config.getConfigItemString("saved_announcement")
        if (announcement != savedAnnouncement && announcement != null)
            adapter.top = AnnouncementState(announcement)
    }

    fun updateAnnouncementAndNotify(announcement: String) {
        val savedAnnouncement =
            TreeHollowApplication.Companion.Config.getConfigItemString("saved_announcement")
        if (announcement != savedAnnouncement)
            adapter.top = AnnouncementState(announcement)
        if (adapter.hasTop == 1)
            adapter.notifyItemChanged(0)
    }

    private fun setAnnnouncement(announcementState: AnnouncementState) {
        adapter.top = announcementState
        adapter.notifyItemChanged(0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}