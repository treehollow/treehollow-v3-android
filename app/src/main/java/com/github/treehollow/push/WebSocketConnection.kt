package com.github.treehollow.push

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.github.treehollow.push.Callback.SuccessCallback
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

internal class WebSocketConnection(
    baseUrl: String,
    token: String,
    private val alarmManager: AlarmManager,
    private val context: Context
) {
    private val client: OkHttpClient

    private var reconnectJob: Job? = null
    private var errorCount = 0
    private val baseUrl: String
    private val token: String
    private var webSocket: WebSocket? = null
    private var onMessage: SuccessCallback<String>? = null
    private var onClose: Runnable? = null
    private var onOpen: Runnable? = null
    private var onBadRequest: BadRequestRunnable? = null
    private var onNetworkFailure: OnNetworkFailureRunnable? = null
    private var onReconnected: Runnable? = null
    private var state: State? = null
    private var onDisconnect: Runnable? = null

    @Synchronized
    fun onMessage(onMessage: SuccessCallback<String>?): WebSocketConnection {
        this.onMessage = onMessage
        return this
    }

    @Synchronized
    fun onClose(onClose: Runnable?): WebSocketConnection {
        this.onClose = onClose
        return this
    }

    @Synchronized
    fun onOpen(onOpen: Runnable?): WebSocketConnection {
        this.onOpen = onOpen
        return this
    }

    @Synchronized
    fun onBadRequest(onBadRequest: BadRequestRunnable?): WebSocketConnection {
        this.onBadRequest = onBadRequest
        return this
    }

    @Synchronized
    fun onDisconnect(onDisconnect: Runnable?): WebSocketConnection {
        this.onDisconnect = onDisconnect
        return this
    }

    @Synchronized
    fun onNetworkFailure(onNetworkFailure: OnNetworkFailureRunnable?): WebSocketConnection {
        this.onNetworkFailure = onNetworkFailure
        return this
    }

    @Synchronized
    fun onReconnected(onReconnected: Runnable?): WebSocketConnection {
        this.onReconnected = onReconnected
        return this
    }

    private fun request(): Request {
        val url = baseUrl.toHttpUrlOrNull()
            ?.newBuilder()
            ?.build()
        return Request.Builder().url(url!!).addHeader("TOKEN", token).get().build()
    }

    @Synchronized
    fun start(): WebSocketConnection {
        if (state == State.Connecting || state == State.Connected) {
            return this
        }
        close()
        state = State.Connecting
        val nextId = ID.incrementAndGet()
        Log.i("Websocket", "WebSocket($nextId): starting...")
        webSocket = client.newWebSocket(request(), Listener(nextId))
        return this
    }

    @Synchronized
    fun close() {
        if (webSocket != null) {
            Log.i("Websocket", "WebSocket(" + ID.get() + "): closing existing connection.")
            state = State.Disconnected
            webSocket?.close(1000, "")
            webSocket = null
        }
    }

    @Synchronized
    fun scheduleReconnect(seconds: Long) {
        if (state == State.Connecting || state == State.Connected) {
            return
        }
        state = State.Scheduled
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.i(
                "Websocket",
                "WebSocket: scheduling a restart in "
                        + seconds
                        + " second(s) (via alarm manager)"
            )
            val future = Calendar.getInstance()
            future.add(Calendar.SECOND, seconds.toInt())
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                future.timeInMillis,
                "reconnect-tag", { start() },
                null
            )
        } else {
            Log.i("Websocket", "WebSocket: scheduling a restart in $seconds second(s)")
            reconnectJob?.cancel()

            val ioScope = CoroutineScope(Dispatchers.IO + Job())
            reconnectJob = ioScope.launch {
                delay(TimeUnit.SECONDS.toMillis(seconds))
                start()
            }
        }
    }

    private inner class Listener(private val id: Long) : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            syncExec {
                state = State.Connected
                Log.i("Websocket", "WebSocket($id): opened")
                onOpen?.run()
                if (errorCount > 0) {
                    onReconnected?.run()
                    errorCount = 0
                }
            }
            super.onOpen(webSocket, response)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            syncExec {
                Log.i("Websocket", "WebSocket($id): received message $text")
                onMessage?.onSuccess(text)
            }
            super.onMessage(webSocket, text)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            syncExec {
                if (state == State.Connected) {
                    Log.w("Websocket", "WebSocket($id): closed")
                    onClose?.run()
                }
                state = State.Disconnected
            }
            super.onClosed(webSocket, code, reason)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            val code = if (response != null) "StatusCode: " + response.code else ""
            val message = response?.message ?: ""
            Log.e("Websocket", "WebSocket($id): failure $code Message: $message $t")
            syncExec {
                onNetworkFailure
                state = State.Disconnected
                if (response != null && response.code >= 400 && response.code <= 499) {
                    onBadRequest?.execute(message)
                    close()
                    return@syncExec
                }
                errorCount++
                if (!Utils.isNetworkAvailable(context)) {
                    Log.i("Websocket", "WebSocket($id): Network not connected")
                    onDisconnect?.run()
                    return@syncExec
                }
                val minutes = Math.min(errorCount * 2 - 1, 20)
                onNetworkFailure?.execute(minutes.toLong())
                scheduleReconnect(TimeUnit.MINUTES.toSeconds(minutes.toLong()))
            }
            super.onFailure(webSocket, t, response)
        }

        private fun syncExec(runnable: Runnable) {
            synchronized(this) {
                if (ID.get() == id) {
                    runnable.run()
                }
            }
        }
    }

    internal interface BadRequestRunnable {
        fun execute(message: String)
    }

    internal interface OnNetworkFailureRunnable {
        fun execute(millis: Long)
    }

    internal enum class State {
        Scheduled, Connecting, Connected, Disconnected
    }

    companion object {
        private val ID = AtomicLong(0)
    }

    init {
        val builder: OkHttpClient.Builder = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .pingInterval(90, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
        client = builder.build()
        this.baseUrl = baseUrl
        this.token = token
    }
}