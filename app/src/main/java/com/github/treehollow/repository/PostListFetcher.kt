package com.github.treehollow.repository

import com.github.treehollow.base.TreeHollowApplication
import com.github.treehollow.data.PostFetcher
import com.github.treehollow.data.PostState
import com.github.treehollow.data.ReplyState
import com.github.treehollow.utils.MarkdownBuilder

class PostListFetcher(
    private val token: String,
    private val mdBuilder: MarkdownBuilder
) :
    PostFetcher() {

    override suspend fun fetchList(page: Int): List<PostState> {

        val userFoldable = PreferencesRepository.getBooleanPreference(
            TreeHollowApplication.instance, "fold_hollows", true
        )
        val foldTags = TreeHollowApplication.Companion.Config.getConfigItemStringList("fold_tags")!!


        val response = service.postList(token, page)
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                if (body.code < 0) {
                    throw Exception(body.msg)
                }
                return body.data!!.map {
                    PostState(
                        it,
                        mdBuilder.getMarkdown(it.text),
                        comments = body.comments!![it.pid.toString()]?.map { it2 -> ReplyState(it2) },
                        foldable = userFoldable && (it.tag?.let { it3 -> foldTags.contains(it3) } == true)
                    )
                }
            } else {
                throw Exception("Network response body is null")
            }
        } else {
            throw Exception("Network error ${response.code()}: ${response.message()}")
        }
    }
}