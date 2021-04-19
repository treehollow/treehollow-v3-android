package com.github.treehollow.ui.postdetail

import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.github.treehollow.R
import com.github.treehollow.base.TreeHollowApplication
import com.github.treehollow.data.*
import com.github.treehollow.databinding.FragmentPostDetailBinding
import com.github.treehollow.network.ApiResponse
import com.github.treehollow.ui.mainscreen.timeline.VoteAdapter
import com.github.treehollow.utils.MarkdownBuilder
import com.github.treehollow.utils.Utils
import com.github.treehollow.utils.Utils.calculateCompressScale
import com.github.treehollow.utils.Utils.compressDisplayImage
import com.github.treehollow.utils.Utils.launchCustomTab
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.microsoft.appcenter.analytics.Analytics
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.util.*
import kotlin.properties.Delegates


class PostDetailFragment : Fragment() {
    companion object {
        fun newInstance(
            pid: Int,
            initPost: ApiResponse.Post?,
            initComments: ApiResponse.ListComments?,
            needRefresh: Boolean?
        ): PostDetailFragment {
            val frag = PostDetailFragment()
            val bundle = Bundle()
            bundle.putInt("pid", pid)
            bundle.putBoolean("needRefresh", needRefresh != false)
            frag.initPost = initPost
            frag.initComments = initComments
            frag.arguments = bundle
            return frag
        }
    }

    var pid by Delegates.notNull<Int>()
    var needRefresh by Delegates.notNull<Boolean>()
    var initPost: ApiResponse.Post? = null
    var initComments: ApiResponse.ListComments? = null
    private var _binding: FragmentPostDetailBinding? = null
    private val binding get() = _binding!!

    private val selectedReply: MutableLiveData<ReplyState?> = MutableLiveData(null)

    //    private var page: Int = 1
    @SuppressLint("SetTextI18n")
    val adapter = ReplyCardAdapter(
        postInit = {
//        Handle image
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
                val imgUrl = imgBaseUrls[0] + this.post!!.post_data.url
                Glide.with(this@PostDetailFragment)
                    .asBitmap()
                    .load(imgUrl)
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
                postImage.setOnClickListener {
                    (this@PostDetailFragment.activity as PostDetailActivity).viewImage(imgUrl)
                }
            } else {
                Glide.with(this@PostDetailFragment).clear(this.postImage)
            }

//        Handle votes
            if (!post!!.hasNoVotes()) {
                val vote = post!!.post_data.vote
                val adapter = VoteAdapter(voteInit = {
                    if (vote.voted == "") {
                        voteLayout.setOnClickListener {
                            model.doVote(this.vote!!.option)
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
                model.star(if (post!!.post_data.attention) 0 else 1)
            }

//        Handle tag
            if (post!!.post_data.tag != null) {
                this.postTag.text = post!!.post_data.tag
            } else {
                this.postTagView.visibility = View.GONE
            }

//        Markdown
            postContent.text = post!!.md

//        Avatar
            postAvatar.setImageBitmap(post!!.avatar)

//        Redirects
            if (post!!.redirects.isNotEmpty()) {
                if (post!!.redirects.size == 1) {
                    val tmp = post!!.redirects[0]
                    redirectsText.text = "跳转到${Utils.trimString(tmp.toString(), 30)}"
                    redirectsCard.setOnClickListener {
                        if (tmp is LinkRedirect)
                            this@PostDetailFragment.activity?.launchCustomTab(Uri.parse(tmp.toHttpUrl()))
                        else if (tmp is PostRedirect) {
                            (this@PostDetailFragment.activity as PostDetailActivity).redirectToPostDetail(
                                tmp.pid
                            )
                        }
                    }
                } else
                    redirectsCard.setOnClickListener {
                        MaterialAlertDialogBuilder(requireContext()).apply {
                            val items =
                                post!!.redirects.map { redirect -> redirect.toString() }
                                    .toTypedArray()
                            setItems(items) { _: DialogInterface, i: Int ->
                                val tmp = post!!.redirects[i]
                                if (tmp is LinkRedirect)
                                    this@PostDetailFragment.activity?.launchCustomTab(Uri.parse(tmp.toHttpUrl()))
                                else if (tmp is PostRedirect) {
                                    (this@PostDetailFragment.activity as PostDetailActivity).redirectToPostDetail(
                                        tmp.pid
                                    )
                                }
                            }
                            show()
                        }
                    }
            }

//            onClick
            expanded.setOnClickListener {
                selectedReply.postValue(null)
            }
//            Handle Long Click
            expanded.setOnLongClickListener {
                showLongClickDialog(
                    post!!.post_data.text,
                    post!!.post_data.permissions,
                    post!!.post_data.pid,
                    true
                )
                return@setOnLongClickListener true
            }
        },
        replyInit = {
            if (this.reply!!.hasImage()) {
//          Add comment image
                val imgBaseUrls =
                    TreeHollowApplication.Companion.Config.getConfigItemStringList("img_base_urls")!!
                val imgUrl = imgBaseUrls[0] + this.reply!!.comment_data.url
                Glide.with(this@PostDetailFragment)
                    .load(imgUrl)
                    .error(ColorDrawable(Color.RED))
                    .placeholder(R.drawable.ic_baseline_image_24)
                    .thumbnail()
                    .override(
                        this.reply!!.comment_data.image_metadata.w!!,
                        this.reply!!.comment_data.image_metadata.h!!
                    )
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(this.commentImage)

                commentImage.setOnClickListener {
                    (this@PostDetailFragment.activity as PostDetailActivity).viewImage(imgUrl)
                }
            } else {
                Glide.with(this@PostDetailFragment).clear(this.commentImage)
            }

//          Avatar
            commentAvatar.setImageBitmap(reply!!.avatar)

//        Handle tag
            if (reply!!.comment_data.tag != null) {
                this.replyTag.text = reply!!.comment_data.tag
            } else {
                this.replyTagView.visibility = View.GONE
            }

//            Handle onclick
            expanded.setOnClickListener {
                selectedReply.postValue(reply)
            }


//        Redirects
            if (reply!!.redirects.isNotEmpty()) {
                if (reply!!.redirects.size == 1) {
                    val tmp = reply!!.redirects[0]
                    redirectsText.text = "跳转到${Utils.trimString(tmp.toString(), 30)}"
                    redirectsCard.setOnClickListener {
                        if (tmp is LinkRedirect)
                            this@PostDetailFragment.activity?.launchCustomTab(Uri.parse(tmp.toHttpUrl()))
                        else if (tmp is PostRedirect) {
                            (this@PostDetailFragment.activity as PostDetailActivity).redirectToPostDetail(
                                tmp.pid
                            )
                        }
                    }
                } else
                    redirectsCard.setOnClickListener {
                        MaterialAlertDialogBuilder(requireContext()).apply {
                            val items =
                                reply!!.redirects.map { redirect -> redirect.toString() }
                                    .toTypedArray()
                            setItems(items) { _: DialogInterface, i: Int ->
                                val tmp = reply!!.redirects[i]
                                if (tmp is LinkRedirect)
                                    this@PostDetailFragment.activity?.launchCustomTab(Uri.parse(tmp.toHttpUrl()))
                                else if (tmp is PostRedirect) {
                                    (this@PostDetailFragment.activity as PostDetailActivity).redirectToPostDetail(
                                        tmp.pid
                                    )
                                }
                            }
                            show()
                        }
                    }
            }


//            Handle Long Click
            expanded.setOnLongClickListener {
                showLongClickDialog(
                    reply!!.comment_data.text,
                    reply!!.comment_data.permissions,
                    reply!!.comment_data.cid,
                    false
                )
                return@setOnLongClickListener true
            }
        })
    val model: PostDetailViewModel by viewModels {
        PostDetailViewModelFactory(pid, adapter, needRefresh)
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        pid = arguments?.getInt("pid")!!
        needRefresh = arguments?.getBoolean("needRefresh")!!


        val properties: MutableMap<String, String> = HashMap()
        properties["pid"] = pid.toString()
        Analytics.trackEvent("PostDetailStart", properties)

        _binding = FragmentPostDetailBinding.inflate(inflater, container, false)
        val view = binding.root
//        model.adapter = adapter

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
        binding.swipeRefresh.setOnRefreshListener {
            model.refresh()
        }
        binding.fabRefresh.setOnClickListener {
            model.refresh()
        }

        // bind adapter to RecyclerView
        binding.detailRecycle.apply {
            this.adapter = model.adapter
            layoutManager = LinearLayoutManager(context)
        }

        binding.replySubmit.setOnClickListener {
            model.reply(
                binding.replyEditText,
                selectedReply.value,
                pid
            )
        }


        val startForPickImageResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val intent = result.data
                    try {
                        Log.d("SendPostActivity", "intent: ${intent!!.data!!}")
                        val imageStream =
                            requireActivity().contentResolver.openInputStream(intent.data!!)
                        val selectedImage = BitmapFactory.decodeStream(imageStream!!)
                        model.addImage(selectedImage)
                        binding.replyImage.setImageResource(R.drawable.ic_baseline_image_cancel_24)
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                    }
                }
            }

        binding.replyImage.setOnClickListener {
            if (model.hasReplyImage.value!!) {
                model.removeImage()
                binding.replyImage.setImageResource(R.drawable.ic_baseline_image_24)
            } else {
                val photoPickerIntent = Intent(Intent.ACTION_PICK)
                photoPickerIntent.type = "image/*"
                startForPickImageResult.launch(photoPickerIntent)
            }
        }

        selectedReply.observe(viewLifecycleOwner) {
            if (binding.replyEditText.text!!.isEmpty() || binding.replyEditText.text.toString() ==
                binding.replyHint.hint!!.replace(Regex("回复"), "Re ") + ": "
            )
                if (it == null || "回复${it.comment_data.name}" == binding.replyHint.hint) {
                    binding.replyHint.hint = "回复原帖"
                    binding.replyEditText.setText("")
                } else {
                    binding.replyHint.hint = "回复${it.comment_data.name}"
                    binding.replyEditText.setText("Re ${it.comment_data.name}: ")
                }
        }

        selectedReply.postValue(null)
        model.sending.observe(viewLifecycleOwner) {
            if (it) {
                binding.replySubmit.visibility = View.GONE
                binding.replyHint.isEnabled = false
                binding.replyProgress.visibility = View.VISIBLE
            } else {
                binding.replySubmit.visibility = View.VISIBLE
                binding.replyHint.isEnabled = true
                binding.replyProgress.visibility = View.GONE
            }
        }

        model.compressor.infoMsg.observe(viewLifecycleOwner, { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
                Snackbar.make(view, it, Snackbar.LENGTH_SHORT).apply {
                    show()
                }
            }
        })

        Utils.onlyRunOnce("long_click_prompt") {
            MaterialAlertDialogBuilder(requireContext()).apply {
                setTitle("提示")
                setMessage("长按树洞可以举报、复制哦")
                setPositiveButton("我知道了") { dialog: DialogInterface, _: Int ->
                    dialog.cancel()
                }
                show()
            }
        }

        lifecycleScope.launch {
            initPost?.let {
                var comments = initComments?.comments?.map { it2 ->
                    ReplyState(
                        it2,
                        environment = PostState.DetailPostEnv
                    )
                }
                if (comments == null)
                    comments = listOf()
                adapter.post.value = PostState(
                    it,
                    MarkdownBuilder(TreeHollowApplication.instance.applicationContext).getMarkdown(
                        it.text
                    ),
                    PostState.DetailPostEnv,
                    comments
                )
                adapter.list.value = comments
                adapter.notifyDataSetChanged()
            }
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showLongClickDialog(
        text: String,
        permissions: List<String>,
        id: Int,
        isPost: Boolean
    ) {

        val reportableTags =
            TreeHollowApplication.Companion.Config.getConfigItemStringList("reportable_tags")!!
        val spannable = SpannableString(text)


        val items: MutableList<Pair<String, () -> Unit>> = mutableListOf()
        items += "复制内容" to { Utils.copy(requireContext(), text, binding.root) }
        items += "自由复制" to { Utils.showSelectDialog(requireContext(), spannable) }
        if (!permissions.contains("set_tag") && permissions.contains("fold")) {
            items += "举报折叠" to {
                showSelectionAlertDialog("举报折叠$id", "", reportableTags.map { str ->
                    str to {
                        model.doReport(
                            "fold",
                            id,
                            str, isPost
                        )
                    }
                })
            }
        }
        if (permissions.contains("set_tag")) {
            items += "管理员打tag" to {
                val items2: MutableList<Pair<String, () -> Unit>> = reportableTags.map { str ->
                    str to {
                        model.doReport(
                            "set_tag",
                            id,
                            str, isPost
                        )
                        Unit
                    }
                }.toMutableList()
                items2 += "空" to {
                    model.doReport(
                        "set_tag",
                        id,
                        "", isPost
                    )
                }
                items2 += "自定义tag" to {
                    showInputAlertDialog(
                        "管理员打tag: $id", "输入tag"
                    ) { str ->
                        model.doReport(
                            "set_tag",
                            id,
                            str, isPost
                        )
                    }
                }
                showSelectionAlertDialog("管理员打tag: $id", "", items2)
            }
        }

        if (!permissions.contains("delete") && !permissions.contains("undelete_unban") &&
            permissions.contains("report")
        ) {
            items += "举报删除" to {
                showInputAlertDialog(
                    "举报删除$id", "这条${
                        if (isPost) {
                            "树洞"
                        } else {
                            "回复"
                        }
                    }违反社区规范，应被删除"
                ) { str ->
                    model.doReport(
                        "report",
                        id,
                        str, isPost
                    )
                }
            }
        }
        if (permissions.contains("delete")) {
            val title = if (permissions.contains("delete_ban")) {
                "管理员删除"
            } else {
                "撤回"
            }
            items += title to {
                showInputAlertDialog(
                    "$title$id", if (permissions.contains("delete_ban")) {
                        "没有禁言惩罚的删除"
                    } else {
                        "树洞发送两分钟内可以撤回"
                    }
                ) { str ->
                    model.doReport(
                        "delete",
                        id,
                        str, isPost
                    )
                }
            }
        }
        if (permissions.contains("undelete_unban")) {
            items += "撤销删除" to {
                showInputAlertDialog(
                    "撤销删除$id", "撤销删除并解除禁言（如果存在禁言的话）"
                ) { str ->
                    model.doReport(
                        "undelete_unban",
                        id,
                        str, isPost
                    )
                }
            }
        }
        if (permissions.contains("delete_ban")) {
            val reasons = listOf(
                "洞规5.2 人身攻击",
                "洞规5.2 不当概化",
                "洞规5.8 禁止约炮",
                "洞规5.8.3",
                "洞规5.9",
                "洞规6.4 政治相关无tag",
                "洞规6.4 性相关无tag",
                "洞规6.5"
            )

            items += "管理员删帖禁言" to {

                val items2: MutableList<Pair<String, () -> Unit>> = reasons.map { str ->
                    str to {
                        model.doReport(
                            "delete_ban",
                            id,
                            str, isPost
                        )
                        Unit
                    }
                }.toMutableList()


                items2 += "其他理由" to {
                    showInputAlertDialog(
                        "管理员删帖禁言: $id", "删除并禁言。删除理由会通知用户。"
                    ) { str ->
                        model.doReport(
                            "delete_ban",
                            id,
                            str, isPost
                        )
                    }
                }
                showSelectionAlertDialog("管理员删帖禁言: $id", "", items2)
            }
        }
        if (permissions.contains("unban")) {
            items += "解封" to {
                showInputAlertDialog(
                    "解封$id", "解封，但不撤销删除"
                ) { str ->
                    model.doReport(
                        "unban",
                        id,
                        str, isPost
                    )
                }
            }
        }

        showSelectionAlertDialog("", "", items)
    }

    private fun showInputAlertDialog(title: String, text: String, callback: (String) -> Unit) {

        context?.let { it1 ->
            MaterialAlertDialogBuilder(it1).apply {
                setTitle(title)
                if (text.isNotEmpty())
                    setMessage(text)
                val input = EditText(it1)
                setView(input)
                setPositiveButton("OK") { dialog: DialogInterface, _: Int ->
                    callback(input.text.toString())
                    dialog.cancel()
                }
                setNegativeButton("Cancel") { dialog: DialogInterface, _: Int ->
                    dialog.cancel()
                }
                show()
            }
        }
    }

    private fun showSelectionAlertDialog(
        title: String,
        text: String,
        options: List<Pair<String, () -> Unit>>,
    ) {
        context?.let { it1 ->
            MaterialAlertDialogBuilder(it1).apply {
                if (title.isNotEmpty())
                    setTitle(title)
                if (text.isNotEmpty())
                    setMessage(text)
                setItems(options.map { x -> x.first }
                    .toTypedArray()) { _: DialogInterface, i: Int ->
                    options[i].second()
                }
                setNegativeButton("取消") { dialog: DialogInterface, _: Int ->
                    dialog.cancel()
                }
                show()
            }
        }
    }
}