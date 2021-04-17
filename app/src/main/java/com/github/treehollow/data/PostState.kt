package com.github.treehollow.data

import android.graphics.Bitmap
import android.graphics.Color
import android.text.Spanned
import com.github.treehollow.network.ApiResponse
import com.github.treehollow.repository.Fetcher
import com.github.treehollow.utils.Avatar
import com.github.treehollow.utils.Utils
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.ExperimentalTime

data class PostState constructor(
    var post_data: ApiResponse.Post,
    val avatar: Bitmap,
    val md: Spanned,
    var index: Int? = null,
    var environment: Int = TimelineEnv,
    var comments: List<ReplyState>? = null,
    var quoteId: Int? = null,
    val redirects: List<Redirect> = listOf(),
    var quotePostState: PostState? = null,
    var foldable: Boolean = true
) : PostListElem(), Serializable {
    companion object {
        const val RandomListEnv = 0
        const val TimelineEnv = 1
        const val DetailPostEnv = 2
    }

    constructor(
        data: ApiResponse.Post,
        md: Spanned,
        environment: Int = TimelineEnv,
        comments: List<ReplyState>? = null,
        foldable: Boolean = true
    ) : this(
        post_data = data,
        md = md,
        avatar = Avatar.genAvatarBitMap(
            Color.WHITE, Avatar.hash(data.pid, ""), 6, 10
        ),
        environment = environment,
        comments = comments,
        foldable = foldable,
        redirects = Utils.urlsFromText(data.text)
    ) {
        val reg = "#\\d{1,7}".toRegex()
        val match = reg.find(data.text)
        if (match != null) {
            quoteId = match.value.substring(1).toInt()
            if (match.next() != null)
                quoteId = null
        }
    }

    // basic
    fun id() = "#${post_data.pid}"
    fun likeNum() = "${post_data.likenum}"
    fun replyNum() = "${post_data.reply}"
    fun haveRedirectOtherThanQuote(): Boolean {
        if (quoteId == null)
            return redirects.isNotEmpty()
        return redirects.size != 1
    }

    // time
    @ExperimentalTime
    fun timeString() = Utils.displayTimestamp(post_data.timestamp * 1000)
    fun accurateTimeString(): String {
        val timestamp = post_data.timestamp * 1000
        val postCalendar = Calendar.getInstance()
        postCalendar.timeInMillis = timestamp

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
//        Log.d("PostState", "current year: $currentYear")
        return if (currentYear == postCalendar.get(Calendar.YEAR)) {
            SimpleDateFormat("MM-dd hh:mm:ss", Locale.getDefault()).format(Date(timestamp))
        } else {
            SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault()).format(Date(timestamp))
        }
    }

    // environment tag fold
    fun isInRandomMode(): Boolean = environment == RandomListEnv
    fun tag(): String? = post_data.tag
    fun isFold() = foldable && !post_data.attention
    fun isShowRandomTag(): Boolean = (environment == RandomListEnv) && (post_data.tag != null)
    fun isShowTimelineTag(): Boolean = (environment == TimelineEnv) && (post_data.tag != null)
    fun isRandomBottomShow(): Boolean = (environment == RandomListEnv) && (!isFold())

    // quote
    fun hasNoQuote() = quoteId == null
    fun quoteId(): String = quoteId?.let { "#$it" } ?: ""
    fun quoteText(): String = quotePostState?.post_data?.text ?: "Loading..."

    // image comment vote
    fun hasImage(): Boolean = post_data.url.isNotEmpty()
    fun hasNoComments(): Boolean = comments.isNullOrEmpty()
    fun hasMoreComments(): Boolean {
        if ((comments != null && post_data.reply > comments!!.size) || (comments == null && post_data.reply > 0)) {
            return true
        }
        return false
    }

    fun getMoreCommentsText() = if (comments != null) {
        "还有${post_data.reply - comments!!.size}条评论"
    } else {
        "还有${post_data.reply}条评论"
    }

    fun getTotalCommentText() = "全部回复 ${post_data.reply}条"
    fun hasNoVotes(): Boolean = post_data.vote.vote_options.isNullOrEmpty()
    fun getMaxLines() = if (environment == DetailPostEnv) {
        Int.MAX_VALUE
    } else {
        10
    }

}


open class ListElem : Serializable
open class PostListElem : ListElem()
object PostListBottom : PostListElem()
object PostListTop : PostListElem()

abstract class PostFetcher : Fetcher() {
    abstract suspend fun fetchList(page: Int): List<PostState>
}