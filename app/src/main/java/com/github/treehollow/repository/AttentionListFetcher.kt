package com.github.treehollow.repository

import com.github.treehollow.data.PostFetcher
import com.github.treehollow.data.PostState
import com.github.treehollow.utils.MarkdownBuilder

class AttentionListFetcher(
    private val token: String,
    private val mdBuilder: MarkdownBuilder
) :
    PostFetcher() {

    override suspend fun fetchList(page: Int): List<PostState> {
        val response = service.attentionList(token, page)

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
                        foldable = false
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