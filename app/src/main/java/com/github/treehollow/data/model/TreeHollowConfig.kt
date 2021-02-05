package com.github.treehollow.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass
import java.io.Serializable


@JsonClass(generateAdapter = true)
data class SearchPromptButton(val text: String, val url: String)

@JsonClass(generateAdapter = true)
data class SearchPrompt(
    val keywords: List<String>,
    val description: String,
    val urls: Map<String, String>,
    val buttons: List<SearchPromptButton>
)


@JsonClass(generateAdapter = true)
data class TreeHollowConfig(
    val name: String,
    val recaptcha_url: String,
    val api_root_urls: List<String>,
    val tos_url: String,
    val privacy_url: String,
    val rules_url: String,
    val contact_email: String,
    val email_suffixes: List<String>,
    val announcement: String,
    val fold_tags: List<String>,
    val reportable_tags: List<String>,
    val sendable_tags: List<String>,
    val img_base_urls: List<String>,
    val web_frontend_version: String,
    val android_frontend_version: String,
    val android_apk_download_url: String,
    val ios_frontend_version: String,
    val websocket_url: String,
    val search_prompts: List<SearchPrompt>,
) : Serializable

@Entity
@JsonClass(generateAdapter = true)
data class ConfigWithUrl(
    @PrimaryKey val id: Int = 1,
    val url: String,
    val config: TreeHollowConfig
)

@Entity
@JsonClass(generateAdapter = true)
data class TOKEN(
    @PrimaryKey val id: Int = 1,
    val token: String
)
