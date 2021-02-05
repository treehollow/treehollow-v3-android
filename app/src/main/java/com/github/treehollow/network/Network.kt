package com.github.treehollow.network

import androidx.annotation.WorkerThread
import com.github.treehollow.data.model.TreeHollowConfig
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request


object Network {
    @WorkerThread
    suspend fun getConfig(
        url: String,
    ): TreeHollowConfig? = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).execute().use { response ->
            val str = response.body
                ?.string()
            if (str == null) {
                null
            } else {
                val configStr =
                    str.substringAfter("-----BEGIN TREEHOLLOW CONFIG-----")
                        .substringBefore("-----END TREEHOLLOW CONFIG-----")
                val moshi = Moshi.Builder().build()
                val jsonAdapter: JsonAdapter<TreeHollowConfig> =
                    moshi.adapter(TreeHollowConfig::class.java)
                val rtn: TreeHollowConfig? = jsonAdapter.fromJson(configStr)
                rtn
            }
        }
    }
}