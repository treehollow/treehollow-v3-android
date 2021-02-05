package com.github.treehollow.repository

import androidx.core.text.toSpanned
import com.github.treehollow.base.TreeHollowApplication
import com.github.treehollow.data.PostState


class SimplePostDetailFetcher(
    private val token: String
) :
    Fetcher() {

    class NoPostException : Exception()

    suspend fun fetchPostDetail(pid: Int): PostState {
        val cache = TreeHollowApplication.dbInstance.commentDao().getCommentCache(pid)
        val oldUpdatedAt = cache?.updatedAt ?: 0
        val response = service.detailPost(token, pid, oldUpdatedAt, 0)

        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                if (body.code < 0) {
                    if (body.code == -101) {
                        throw NoPostException()
                    }
                    throw Exception(body.msg)
                }
                return PostState(
                    body.post!!,
                    body.post.text.toSpanned(),
                    PostState.TimelineEnv,
                    null
                )
            } else {
                throw Exception("Network response body is null")
            }
        } else {
            throw Exception("Network error ${response.code()}: ${response.message()}")
        }
    }
}