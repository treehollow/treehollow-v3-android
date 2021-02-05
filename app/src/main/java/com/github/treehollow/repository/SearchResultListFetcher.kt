package com.github.treehollow.repository

import com.github.treehollow.R
import com.github.treehollow.base.TreeHollowApplication
import com.github.treehollow.data.PostFetcher
import com.github.treehollow.data.PostState
import com.github.treehollow.data.ReplyState
import com.github.treehollow.utils.MarkdownBuilder
import java.io.Serializable

class SearchResultListFetcher(
    private val token: String,
    private val mdBuilder: MarkdownBuilder,
    val keywords: String,
    private val before: Long,
    private val after: Long,
    private val includeComment: Boolean,
    private val order: String
) :
    PostFetcher() {
    override suspend fun fetchList(page: Int): List<PostState> {

        val trendingString = TreeHollowApplication.instance.getString(R.string.hotlist)
        val userFoldable = PreferencesRepository.getBooleanPreference(
            TreeHollowApplication.instance, "fold_hollows", true
        )
        val foldable = userFoldable && (keywords == trendingString)
        val foldTags = TreeHollowApplication.Companion.Config.getConfigItemStringList("fold_tags")!!


        val response = service.searchList(
            token, keywords, page, before,
            after, includeComment, order
        )
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                if (body.code < 0) {
                    throw Exception(body.msg)
                }
                val list = body.data!!.map {
                    PostState(
                        it,
                        mdBuilder.getMarkdown(it.text),
                        environment = PostState.TimelineEnv,
                        foldable = foldable && (it.tag?.let { it3 -> foldTags.contains(it3) } == true)
                    )
                }
                if (body.comments != null) {
                    for ((pid, listOfComments) in body.comments) {
                        list.filter { it.post_data.pid.toString() == pid }
                            .forEach {
                                if (it.comments == null)
                                    it.comments = ArrayList()
                                val i = it.comments!!.size
                                listOfComments.forEach { it1 ->
                                    run {
                                        (it.comments as ArrayList).add(ReplyState(it1, i))
                                    }
                                }
                            }
                        // if includeComment, pid seems to be already in list.
                    }
                }
                return list
            } else {
                throw Exception("Network response body is null")
            }
        } else {
            throw Exception("Network error ${response.code()}: ${response.message()}")
        }
    }
}

data class SearchParameter constructor(
    var valid: Boolean = false,
    var keywords: String = "",
    var before: Long = -1,
    var after: Long = -1,
    var includeComment: Boolean = true,
    var order: String = "id"
) : Serializable {
}