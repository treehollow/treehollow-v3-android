package com.github.treehollow.data

import com.github.treehollow.utils.Utils
import java.io.Serializable

data class AnnouncementState constructor(
    val text: String,
    val redirects: List<Redirect> = listOf(),
) : PostListElem(), Serializable {
    constructor(text: String) : this(text, Utils.urlsFromText(text))
}
