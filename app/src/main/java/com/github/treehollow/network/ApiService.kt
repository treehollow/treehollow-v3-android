package com.github.treehollow.network

import com.github.treehollow.BuildConfig
import retrofit2.Response
import retrofit2.http.*

//Documentation: https://square.github.io/retrofit/
interface ApiService {

    // config

    @GET("v3/config/get_push?device=1&v=" + BuildConfig.VERSION_NAME)
    suspend fun getPush(
        @Header("TOKEN") TOKEN: String,
    ): Response<ApiResponse.GetPushResponse>

    @FormUrlEncoded
    @POST("v3/config/set_push?device=1&v=" + BuildConfig.VERSION_NAME)
    suspend fun setPush(
        @Header("TOKEN") TOKEN: String,
        @Field("push_system_msg") pushSystemMsg: Int,
        @Field("push_reply_me") pushReplyMe: Int,
        @Field("push_favorited") pushFavorited: Int,
    ): Response<ApiResponse.SetPushResponse>

    // security

    @FormUrlEncoded
    @POST("v3/security/login/check_email?device=1&v=" + BuildConfig.VERSION_NAME)
    suspend fun checkEmail(
        @Field("email") email: String?,
        @Field("recaptcha_token") recaptcha_token: String?,
        @Field("recaptcha_version") recaptcha_version: String?
    ): Response<ApiResponse.CheckEmailResponse>

    @FormUrlEncoded
    @POST("v3/security/login/create_account?device=1&v=" + BuildConfig.VERSION_NAME)
    suspend fun register(
        @Field("email") email: String,
        @Field("password_hashed") password_hashed: String,
        @Field("device_type") device_type: Int = 1,
        @Field("device_info") device_info: String,
        @Field("valid_code") valid_code: String,
    ): Response<ApiResponse.RegisterResponse>

    @FormUrlEncoded
    @POST("v3/security/login/change_password?device=1&v=" + BuildConfig.VERSION_NAME)
    suspend fun changePassword(
        @Field("email") email: String,
        @Field("old_password_hashed") old_password_hashed: String,
        @Field("new_password_hashed") new_password_hashed: String,
    ): Response<ApiResponse.ChangePasswordResponse>

    @FormUrlEncoded
    @POST("v3/security/login/login?device=1&v=" + BuildConfig.VERSION_NAME)
    suspend fun login(
        @Field("email") email: String,
        @Field("password_hashed") password_hashed: String,
        @Field("device_type") device_type: Int = 1,
        @Field("device_info") device_info: String,
    ): Response<ApiResponse.LoginResponse>

    @GET("v3/security/devices/list?device=1&v=" + BuildConfig.VERSION_NAME)
    suspend fun devicesList(
        @Header("TOKEN") TOKEN: String,
    ): Response<ApiResponse.DevicesListResponse>

    @FormUrlEncoded
    @POST("v3/security/devices/terminate?device=1&v=" + BuildConfig.VERSION_NAME)
    suspend fun terminateDevice(
        @Header("TOKEN") TOKEN: String,
        @Field("device_uuid") uuid: String,
    ): Response<ApiResponse.TerminateDeviceResponse>

    @POST("v3/security/logout?device=1&v=" + BuildConfig.VERSION_NAME)
    suspend fun logoutCurrentDevice(
        @Header("TOKEN") TOKEN: String,
    ): Response<ApiResponse.QuitDeviceResponse>

    // send

    @FormUrlEncoded
    @POST("v3/send/post?device=1&v=" + BuildConfig.VERSION_NAME)
    suspend fun sendPost(
        @Field("text") text: String,
        @Field("type") type: String,
        @Field("tag") tag: String?,
        @Field("data") data: String?,
        @Field("vote_options[]") vote_options: List<String>?
    ): Response<ApiResponse.SendPostResponse>

    @FormUrlEncoded
    @POST("v3/send/comment?device=1&v=" + BuildConfig.VERSION_NAME)
    suspend fun replyPost(
        @Field("text") text: String,
        @Field("type") type: String,
        @Field("pid") tag: Int,
        @Field("reply_to_cid") replyTo: Int?,
        @Field("data") data: String?,
    ): Response<ApiResponse.SendReplyResponse>

    @FormUrlEncoded
    @POST("v3/send/vote?device=1&v=" + BuildConfig.VERSION_NAME)
    suspend fun sendVote(
        @Header("TOKEN") TOKEN: String,
        @Field("pid") pid: Int,
        @Field("option") option: String,
    ): Response<ApiResponse.SendVoteResponse>

    // contents

    @GET("v3/contents/post/list?device=1&v=" + BuildConfig.VERSION_NAME)
    suspend fun postList(
        @Header("TOKEN") TOKEN: String,
        @Query("page") page: Int,
    ): Response<ApiResponse.ListPostResponse>

    @GET("v3/contents/post/detail?device=1&v=" + BuildConfig.VERSION_NAME)
    suspend fun detailPost(
        @Header("TOKEN") TOKEN: String,
        @Query("pid") pid: Int,
        @Query("old_updated_at") oldUpdatedAt: Long,
        @Query("include_comment") includeComment: Int,
    ): Response<ApiResponse.DetailPostResponse>

    @GET("v3/contents/post/attentions?device=1&v=" + BuildConfig.VERSION_NAME)
    suspend fun attentionList(
        @Header("TOKEN") TOKEN: String,
        @Query("page") page: Int,
    ): Response<ApiResponse.ListPostResponseWithoutComments>

    @GET("v3/contents/post/randomlist?device=1&v=" + BuildConfig.VERSION_NAME)
    suspend fun randomList(
        @Header("TOKEN") TOKEN: String,
    ): Response<ApiResponse.ListPostResponseWithoutComments>

    @GET("v3/contents/search?device=1&v=" + BuildConfig.VERSION_NAME)
    suspend fun searchList(
        @Header("TOKEN") TOKEN: String,
        @Query("keywords") keywords: String,
        @Query("page") page: Int,
        @Query("before") before: Long,
        @Query("after") after: Long,
        @Query("include_comment") includeComment: Boolean,
        @Query("order") order: String,
    ): Response<ApiResponse.ListPostResponse>

    @GET("v3/contents/my_msgs?device=1&v=" + BuildConfig.VERSION_NAME)
    suspend fun messageList(
        @Header("TOKEN") TOKEN: String,
        @Query("page") page: Int,
        @Query("push_only") pushOnly: Boolean,
        @Query("since_id") sinceId: Int,
    ): Response<ApiResponse.PushMessageResponse>

    @GET("v3/contents/my_msgs?device=1&v=" + BuildConfig.VERSION_NAME)
    suspend fun myMessages(
        @Header("TOKEN") TOKEN: String,
        @Query("page") page: Int,
        @Query("push_only") pushOnly: Int,
        @Query("since_id") sinceId: Int,
    ): Response<ApiResponse.PushMessageResponse>

    // edit

    @FormUrlEncoded
    @POST("v3/edit/report/post?device=1&v=" + BuildConfig.VERSION_NAME)
    suspend fun reportPost(
        @Header("TOKEN") TOKEN: String,
        @Field("id") id: Int,
        @Field("type") type: String,
        @Field("reason") reason: String,
    ): Response<ApiResponse.ReportResponse>

    @FormUrlEncoded
    @POST("v3/edit/report/comment?device=1&v=" + BuildConfig.VERSION_NAME)
    suspend fun reportComment(
        @Header("TOKEN") TOKEN: String,
        @Field("id") id: Int,
        @Field("type") type: String,
        @Field("reason") reason: String,
    ): Response<ApiResponse.ReportResponse>

    @FormUrlEncoded
    @POST("v3/edit/attention?device=1&v=" + BuildConfig.VERSION_NAME)
    suspend fun editAttention(
        @Header("TOKEN") TOKEN: String,
        @Field("pid") pid: Int,
        @Field("switch") switch: Int,
    ): Response<ApiResponse.EditAttentionResponse>
}