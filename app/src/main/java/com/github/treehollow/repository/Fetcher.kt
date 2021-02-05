package com.github.treehollow.repository

import com.github.treehollow.base.TreeHollowApplication
import com.github.treehollow.network.ApiService
import com.skydoves.sandwich.coroutines.CoroutinesResponseCallAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

abstract class Fetcher {

    protected val service: ApiService

    init {
        val apiRootUrls =
            TreeHollowApplication.Companion.Config.getConfigItemStringList("api_root_urls")!!
        val retrofit = Retrofit.Builder()
            .baseUrl(apiRootUrls[0])
            .addConverterFactory(MoshiConverterFactory.create())
            .addCallAdapterFactory(CoroutinesResponseCallAdapterFactory())
            .build()

        service = retrofit.create(ApiService::class.java)
    }
}