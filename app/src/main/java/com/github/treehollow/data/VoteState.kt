package com.github.treehollow.data

import java.io.Serializable

data class VoteState constructor(
    var option: String,
    var count: Int,
    var selected: Boolean,
) : Serializable {
    fun num() = "$count"
    fun voted(): Boolean = (count >= 0)
}