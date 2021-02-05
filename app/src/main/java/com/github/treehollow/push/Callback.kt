package com.github.treehollow.push

import android.app.Activity
import android.util.Log
import retrofit2.Call
import retrofit2.Response

class Callback<T> private constructor(
    private val onSuccess: SuccessCallback<T>,
    private val onError: ErrorCallback
) {
    interface SuccessCallback<T> {
        fun onSuccess(data: T)
    }

    interface ErrorCallback {
        fun onError(t: ApiException?)
    }

    private class RetrofitCallback<T>(private val callback: Callback<T>) : retrofit2.Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            if (response.isSuccessful) {
                response.body()?.let { callback.onSuccess.onSuccess(it) }
            } else {
                callback.onError.onError(ApiException(response))
            }
        }

        override fun onFailure(call: Call<T>, t: Throwable) {
            callback.onError.onError(ApiException(t))
        }

    }

    companion object {
        fun <T> callInUI(
            context: Activity, onSuccess: SuccessCallback<T>, onError: ErrorCallback
        ): retrofit2.Callback<T> {
            return call(
                object : SuccessCallback<T> {
                    override fun onSuccess(data: T) {
                        context.runOnUiThread { onSuccess.onSuccess(data) }
                    }
                },
                object : ErrorCallback {
                    override fun onError(t: ApiException?) {
                        context.runOnUiThread { onError.onError(t) }
                    }
                })
        }

        fun <T> call(): retrofit2.Callback<T> {
            return call(object : SuccessCallback<T> {
                override fun onSuccess(data: T) {

                }
            }, object : ErrorCallback {
                override fun onError(t: ApiException?) {

                }
            })
        }

        fun <T> call(
            onSuccess: SuccessCallback<T>, onError: ErrorCallback
        ): retrofit2.Callback<T> {
            return RetrofitCallback(merge(of(onSuccess, onError), errorCallback()))
        }

        private fun <T> of(onSuccess: SuccessCallback<T>, onError: ErrorCallback): Callback<T> {
            return Callback(onSuccess, onError)
        }

        private fun <T> errorCallback(): Callback<T> {
            return Callback(
                object : SuccessCallback<T> {
                    override fun onSuccess(data: T) {

                    }
                },
                object : ErrorCallback {
                    override fun onError(t: ApiException?) {
                        Log.e(
                            "push.Callback",
                            "Error while api call" + t.toString()
                        )
                    }
                })
        }

        private fun <T> merge(left: Callback<T>, right: Callback<T>): Callback<T> {
            return Callback(
                object : SuccessCallback<T> {
                    override fun onSuccess(data: T) {
                        left.onSuccess.onSuccess(data)
                        right.onSuccess.onSuccess(data)
                    }
                },
                object : ErrorCallback {
                    override fun onError(t: ApiException?) {
                        left.onError.onError(t)
                        right.onError.onError(t)
                    }
                })
        }
    }
}