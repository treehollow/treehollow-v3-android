package com.github.treehollow.network

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass
import java.io.Serializable

class ApiResponse {

    @JsonClass(generateAdapter = true)
    data class PushType(
        val push_system_msg: Int,
        val push_reply_me: Int,
        val push_favorited: Int,
    ) : Serializable

    @Entity
    @JsonClass(generateAdapter = true)
    data class GetPushResponse(
        val code: Int,
        val data: PushType,
        val msg: String?
    ) : Serializable

    @Entity
    @JsonClass(generateAdapter = true)
    data class SetPushResponse(val code: Int, val msg: String?)

    @Entity
    @JsonClass(generateAdapter = true)
    data class CheckEmailResponse(val code: Int, val msg: String?)

    @Entity
    @JsonClass(generateAdapter = true)
    data class ChangePasswordResponse(val code: Int, val msg: String?)

    @Entity
    @JsonClass(generateAdapter = true)
    data class RegisterResponse(val code: Int, val msg: String?, val token: String?)

    @Entity
    @JsonClass(generateAdapter = true)
    data class LoginResponse(val code: Int, val msg: String?, val token: String?)

    @Entity
    @JsonClass(generateAdapter = true)
    data class SendPostResponse(val code: Int, val msg: String?)

    @Entity
    @JsonClass(generateAdapter = true)
    data class SendReplyResponse(val code: Int, val msg: String?)

    @Entity
    @JsonClass(generateAdapter = true)
    data class ReportResponse(val code: Int, val msg: String?)

    @JsonClass(generateAdapter = true)
    data class ImageMetadata(
        val w: Int?,
        val h: Int?,
    ) : Serializable

    @JsonClass(generateAdapter = true)
    data class Vote(
        val voted: String?,
        val vote_options: List<String>?,
        val vote_data: Map<String, Int>?,
    ) : Serializable

    @JsonClass(generateAdapter = true)
    data class Post(
        val attention: Boolean,
        val deleted: Boolean,
        val likenum: Int,
        val permissions: List<String>,
        val pid: Int,
        val reply: Int,
        val text: String,
        val tag: String?,
        val timestamp: Long,
        val updated_at: Long,
        val type: String,
        val url: String,
        val image_metadata: ImageMetadata,
        var vote: Vote,
    ) : Serializable

    @JsonClass(generateAdapter = true)
    data class Comment(
        val deleted: Boolean,
        val permissions: List<String>,
        val pid: Int,
        val cid: Int,
        val text: String,
        val timestamp: Long,
        val tag: String?,
        val type: String,
        val url: String,
        val image_metadata: ImageMetadata,
        val reply_to: Int,
        val name: String,
    ) : Serializable

    data class ListComments(
        val comments: List<Comment>
    ) : Serializable

    @Entity
    @JsonClass(generateAdapter = true)
    data class ListPostResponse(
        val code: Int,
        val data: List<Post>?,
        val comments: Map<String, List<Comment>>?,
        val msg: String?
    ) : Serializable

    @Entity
    @JsonClass(generateAdapter = true)
    data class ListPostResponseWithoutComments(
        val code: Int,
        val data: List<Post>?,
        val msg: String?
    ) : Serializable

    @Entity
    @JsonClass(generateAdapter = true)
    data class DetailPostResponse(
        val code: Int,
        var data: List<Comment>?,
        val post: Post?,
        val msg: String?
    ) : Serializable

    @JsonClass(generateAdapter = true)
    data class DeviceEntry(
        val device_uuid: String,
        val login_date: String,
        val device_info: String,
        val device_type: Int,
    )

    @Entity
    @JsonClass(generateAdapter = true)
    data class DevicesListResponse(
        val code: Int,
        val data: List<DeviceEntry>?,
        val this_device: String?,
        val msg: String?
    )

    @Entity
    @JsonClass(generateAdapter = true)
    data class EditAttentionResponse(
        val code: Int,
        val msg: String?,
        val data: Post?
    )

    @Entity
    @JsonClass(generateAdapter = true)
    data class SendVoteResponse(
        val code: Int,
        val msg: String?,
        val vote: Vote?
    )

    @Entity
    @JsonClass(generateAdapter = true)
    data class PushMessage(
        @PrimaryKey val id: Int,
        val title: String?,
        val body: String?,
        val pid: Int?,
        val cid: Int?,
        val type: Int?,
        val timestamp: Long?,
        @Ignore val delete: Int? = null
    ) : Serializable {
        constructor(
            id: Int,
            title: String?,
            body: String?,
            pid: Int?,
            cid: Int?,
            type: Int?,
            timestamp: Long?
        ) : this(id, title, body, pid, cid, type, timestamp, null)
    }

    @Entity
    @JsonClass(generateAdapter = true)
    data class PushMessageResponse(
        val code: Int,
        val msg: String?,
        val data: List<PushMessage>?
    ) : Serializable

    @Entity
    @JsonClass(generateAdapter = true)
    data class TerminateDeviceResponse(
        val code: Int,
        val msg: String?
    )

    @Entity
    @JsonClass(generateAdapter = true)
    data class QuitDeviceResponse(
        val code: Int,
        val msg: String?
    )
}