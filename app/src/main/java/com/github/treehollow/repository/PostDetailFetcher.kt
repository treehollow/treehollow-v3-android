package com.github.treehollow.repository

import com.github.treehollow.base.TreeHollowApplication
import com.github.treehollow.data.PostState
import com.github.treehollow.data.PostState.Companion.DetailPostEnv
import com.github.treehollow.data.ReplyState
import com.github.treehollow.database.LocalComment
import com.github.treehollow.utils.MarkdownBuilder

class PostDetailFetcher(
    private val token: String,
    private val mdBuilder: MarkdownBuilder
) :
    Fetcher() {

    class NoPostException : Exception()

    suspend fun fetchPostDetail(pid: Int, includeComment: Int): PostState {
        val cache = TreeHollowApplication.dbInstance.commentDao().getCommentCache(pid)
        val oldUpdatedAt = cache?.updatedAt ?: 0
        val response = service.detailPost(token, pid, oldUpdatedAt, includeComment)

        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                if (body.code < 0) {
                    if (body.code == -101) {
                        throw NoPostException()
                    }
                    throw Exception(body.msg)
                }
                if (body.code == 1 && includeComment == 1) {
                    body.data = cache!!.comments
                }
                if (includeComment == 1) {
                    TreeHollowApplication.dbInstance.commentDao()
                        .saveLocalComment(LocalComment(pid, body.data!!, body.post!!.updated_at))
                }
                val comments = body.data?.map { ReplyState(it, environment = DetailPostEnv) }
                return PostState(
                    body.post!!,
                    mdBuilder.getMarkdown(body.post.text),
                    DetailPostEnv,
                    comments
                )
            } else {
                throw Exception("Network response body is null")
            }
        } else {
            throw Exception("Network error ${response.code()}: ${response.message()}")
        }
    }
}