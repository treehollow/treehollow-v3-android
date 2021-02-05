package com.github.treehollow.push

import retrofit2.Response
import java.io.IOException
import java.util.*

class ApiException : Exception {
    private var body: String? = null
    private val code: Int

    internal constructor(response: Response<*>) : super("Api Error", null) {
        try {
            body = if (response.errorBody() != null) response.errorBody()!!.string() else ""
        } catch (e: IOException) {
            body = "Error while getting error body :("
        }
        this.code = response.code()
    }

    internal constructor(cause: Throwable?) : super("Request failed.", cause) {
        body = ""
        this.code = 0
    }

    fun body(): String? {
        return body
    }

    fun code(): Int {
        return code
    }

    override fun toString(): String {
        return String.format(
            Locale.ENGLISH,
            "Code(%d) Response: %s",
            code(),
            body()!!.substring(0, body()!!.length.coerceAtMost(200))
        )
    }
}