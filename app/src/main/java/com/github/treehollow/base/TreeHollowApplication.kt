package com.github.treehollow.base

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread
import com.github.treehollow.data.model.SearchPrompt
import com.github.treehollow.data.model.TreeHollowConfig
import com.github.treehollow.database.AppDatabase
import com.github.treehollow.database.Db
import com.github.treehollow.network.Network
import com.github.treehollow.utils.Const
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

class TreeHollowApplication : Application() {

    companion object {
        lateinit var instance: TreeHollowApplication
        lateinit var dbInstance: AppDatabase

        // token
        var token: String? = null
            get() {
                if (field != null) {
                    return field
                }
                val sharedPref =
                    instance.applicationContext.getSharedPreferences(
                        Const.TokenKey,
                        Context.MODE_PRIVATE
                    )
                val tokenData = sharedPref.getString(Const.TokenName, "")
                return if (tokenData != null && tokenData != "") {
                    tokenData
                } else {
                    null
                }
            }
            set(value) {
                field = value
                val sharedPref = instance.applicationContext.getSharedPreferences(
                    Const.TokenKey,
                    Context.MODE_PRIVATE
                )
                val edit = sharedPref.edit()
                Log.d("TreeHollowApplication", "edit.putString(${Const.TokenName}, $value)")
                edit.putString(Const.TokenName, value ?: "")
                edit.apply()
            }

        fun deleteToken() {
            token = null
        }

        // config

        object Config {
            private val stringListAdapter = StringListAdapter()
            private val stringConfig = mutableMapOf<String, String?>(
                "config_url" to null,
                "name" to null,
                "recaptcha_url" to null,
                "tos_url" to null,
                "privacy_url" to null,
                "rules_url" to null,
                "contact_email" to null,
                "announcement" to null,
                "web_frontend_version" to null,
                "android_frontend_version" to null,
                "android_apk_download_url" to null,
                "ios_frontend_version" to null,
                "websocket_url" to null,
            )

            private val stringListConfig = mutableMapOf<String, List<String>>(
                "api_root_urls" to listOf(),
                "email_suffixes" to listOf(),
                "fold_tags" to listOf(),
                "reportable_tags" to listOf(),
                "sendable_tags" to listOf(),
                "img_base_urls" to listOf(),
            )


            fun getConfigItemString(itemName: String): String? {
                val cache = stringConfig[itemName]
                if (cache != null) {
                    return cache
                }
                val configSP =
                    instance.applicationContext.getSharedPreferences(
                        Const.ConfigKey,
                        Context.MODE_PRIVATE
                    )
                val dataSP = configSP.getString(itemName, "")
                return if (dataSP != null && dataSP != "") {
                    dataSP
                } else {
                    null
                }
            }

            fun getConfigItemSearchPrompts(): List<SearchPrompt>? {
                val dataSP = if (stringConfig["search_prompts"] != null) {
                    stringConfig["search_prompts"]
                } else {
                    val configSP =
                        instance.applicationContext.getSharedPreferences(
                            Const.ConfigKey,
                            Context.MODE_PRIVATE
                        )
                    configSP.getString("search_prompts", "")
                }
                return if (dataSP != null && dataSP != "") {
                    val moshi = Moshi.Builder().build()
                    val listType =
                        Types.newParameterizedType(List::class.java, SearchPrompt::class.java)
                    val jsonAdapter: JsonAdapter<List<SearchPrompt>> =
                        moshi.adapter(listType)
                    jsonAdapter.fromJson(dataSP)
                } else {
                    null
                }
            }

            fun setConfigItemString(itemName: String, value: String) {
                stringConfig[itemName] = value
                val configSP = instance.applicationContext.getSharedPreferences(
                    Const.ConfigKey,
                    Context.MODE_PRIVATE
                )
                val edit = configSP.edit()
                edit.putString(itemName, value)
                edit.apply()
            }


            fun getConfigItemStringList(itemName: String): List<String>? {
                val cache = stringListConfig[itemName]
                if (cache != null && cache.isNotEmpty()) {
                    return cache
                }
                val configSP =
                    instance.applicationContext.getSharedPreferences(
                        Const.ConfigKey,
                        Context.MODE_PRIVATE
                    )
                val dataSP = configSP.getString(itemName, "[]")
                val listValue = stringListAdapter.fromString(dataSP)
                return if (listValue.isNotEmpty()) {
                    listValue
                } else {
                    null
                }
            }

            fun setConfigItemStringList(itemName: String, value: List<String>) {
                stringListConfig[itemName] = value
                val strValue = stringListAdapter.toString(value)
                val configSP = instance.applicationContext.getSharedPreferences(
                    Const.ConfigKey,
                    Context.MODE_PRIVATE
                )
                val edit = configSP.edit()
                edit.putString(itemName, strValue)
                edit.apply()
            }

            @WorkerThread
            suspend fun initConfig(
                url: String,
            ): Result<TreeHollowConfig> {
                try {
                    val data = Network.getConfig(url) ?: throw Exception("null network response")
                    // string
                    setConfigItemString("config_url", url)
                    setConfigItemString("name", data.name)
                    setConfigItemString("recaptcha_url", data.recaptcha_url)
                    setConfigItemString("tos_url", data.tos_url)
                    setConfigItemString("rules_url", data.rules_url)
                    setConfigItemString("privacy_url", data.privacy_url)
                    setConfigItemString("contact_email", data.contact_email)
                    setConfigItemString("announcement", data.announcement)
                    setConfigItemString("web_frontend_version", data.web_frontend_version)
                    setConfigItemString("android_frontend_version", data.android_frontend_version)
                    setConfigItemString("android_apk_download_url", data.android_apk_download_url)
                    setConfigItemString("ios_frontend_version", data.ios_frontend_version)
                    setConfigItemString("websocket_url", data.websocket_url)

                    // string list
                    setConfigItemStringList("api_root_urls", data.api_root_urls)
                    setConfigItemStringList("email_suffixes", data.email_suffixes)
                    setConfigItemStringList("fold_tags", data.fold_tags)
                    setConfigItemStringList("reportable_tags", data.reportable_tags)
                    setConfigItemStringList("sendable_tags", data.sendable_tags)
                    setConfigItemStringList("img_base_urls", data.img_base_urls)

//                    search prompts
                    val moshi = Moshi.Builder().build()
                    val listType =
                        Types.newParameterizedType(List::class.java, SearchPrompt::class.java)
                    val jsonAdapter: JsonAdapter<List<SearchPrompt>> =
                        moshi.adapter(listType)
                    setConfigItemString("search_prompts", jsonAdapter.toJson(data.search_prompts))

                    Log.d("TreeHollowApplication", "Config save success: $data")
                    return Result.Success(data)
                } catch (e: Exception) {
                    Log.e("TreeHollowApplication", "initConfig: $e")
                    return Result.Error(e)
                }
            }
        }
    }


    override fun onCreate() {
        super.onCreate()
        instance = this
        dbInstance = Db.getDatabase(this)
    }

    class StringListAdapter {
        private val separator = "\t"
        fun fromString(str: String?): List<String> {
            if (str == null)
                return emptyList()
            return str.split(separator)
        }

        fun toString(list: List<String>): String {
            return list.joinToString(separator)
        }
    }


}
