package com.github.treehollow.data

import android.graphics.Bitmap
import android.graphics.Color
import com.github.treehollow.data.PostState.Companion.TimelineEnv
import com.github.treehollow.network.ApiResponse
import com.github.treehollow.utils.Avatar
import com.github.treehollow.utils.Utils
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

open class ReplyListElem : ListElem()
object ReplyListBottom : ReplyListElem()

data class ReplyState(
    val comment_data: ApiResponse.Comment,
    val avatar: Bitmap,
    val redirects: List<Redirect> = listOf(),
    var index: Int? = null,
    var environment: Int = TimelineEnv,
) : ReplyListElem(), Serializable {

    constructor(
        comment_data: ApiResponse.Comment,
        index: Int? = null,
        environment: Int = TimelineEnv
    ) : this(
        comment_data = comment_data, avatar = getCommentAvatar(comment_data),
        index = index, environment = environment, redirects = Utils.urlsFromText(comment_data.text)
    )

    fun id() = "#${comment_data.pid}"
    fun hasImage(): Boolean = comment_data.url.isNotEmpty()
    fun accurateTimeString(): String {
        val timestamp = comment_data.timestamp * 1000
        val postCalendar = Calendar.getInstance()
        postCalendar.timeInMillis = timestamp

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
//        Log.d("PostState", "current year: $currentYear")
        if (currentYear == postCalendar.get(Calendar.YEAR)) {
            return SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
        } else {
            return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                Date(
                    timestamp
                )
            )
        }
    }

}

fun getCommentAvatar(commentData: ApiResponse.Comment): Bitmap {
    return if (commentData.name == "洞主") {
        Avatar.genAvatarBitMap(
            Color.WHITE, Avatar.hash(commentData.pid, ""), 6, 10,
        )
    } else {
        Avatar.genAvatarBitMap(
            Color.WHITE, Avatar.hash(commentData.pid, commentData.name), 4, 15,
        )
    }
}
