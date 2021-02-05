package com.github.treehollow.data

open class Redirect
data class LinkRedirect(val url: String) : Redirect() {
    override fun toString(): String {
        return url
    }

    fun toHttpUrl(): String {
        return if (url.matches(Regex("^https?://.*"))) url
        else "http://${url}"
    }
}

data class PostRedirect(val pid: Int, val cid: Int) : Redirect() {
    override fun toString(): String {
        return "#${pid}"
    }
}

