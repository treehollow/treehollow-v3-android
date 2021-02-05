package com.github.treehollow.base

//https://developer.android.com/kotlin/coroutines
sealed class Result<out R> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
}
