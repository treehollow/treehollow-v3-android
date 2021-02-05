package com.github.treehollow.data

import android.annotation.SuppressLint
import com.github.treehollow.network.ApiResponse
import com.github.treehollow.utils.Utils
import java.io.Serializable
import kotlin.time.ExperimentalTime


data class MessageState constructor(
    val data: ApiResponse.PushMessage,
    var index: Int? = null,
    var quoteId: Int? = null,
    var quotePostState: PostState? = null,
) : MessageListElem(), Serializable {

    constructor(
        data: ApiResponse.PushMessage,
    ) : this(data, null, data.pid, null) {
    }

    fun title(): String {
        return data.title!!
    }

    fun textBody(): String {
        return data.body!!
    }

    fun hasQuote(): Boolean {
        return data.pid != null
    }

    @SuppressLint("SimpleDateFormat")
    @ExperimentalTime
    fun timeString(): String {
        return if (data.timestamp == null)
            Utils.displayTimestamp(0)
        else {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd\nHH:mm")
            val date = java.util.Date(data.timestamp * 1000)
            sdf.format(date)
        }
    }
}

sealed class MessageListElem : Serializable
object MessageListBottom : MessageListElem()
