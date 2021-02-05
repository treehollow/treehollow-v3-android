package com.github.treehollow.push

import android.app.*
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.treehollow.R
import com.github.treehollow.base.TreeHollowApplication
import com.github.treehollow.network.ApiResponse
import com.github.treehollow.push.Callback.SuccessCallback
import com.github.treehollow.push.WebSocketConnection.BadRequestRunnable
import com.github.treehollow.push.WebSocketConnection.OnNetworkFailureRunnable
import com.github.treehollow.ui.MainActivity
import com.github.treehollow.ui.messages.MessageActivity
import com.github.treehollow.ui.postdetail.PostDetailActivity
import com.github.treehollow.utils.Utils
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger


class WebSocketService : Service() {
    private var connection: WebSocketConnection? = null
    private lateinit var channelId: String
    private lateinit var foregroundChannelId: String

    private val lastReceivedMessage: AtomicInteger = AtomicInteger(NOT_LOADED)
    private var missingMessageUtil: MissedMessageUtil? = null

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val chan = NotificationChannel(
            channelId,
            channelName, NotificationManager.IMPORTANCE_DEFAULT
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createForegroundNotificationChannel(
        channelId: String,
        channelName: String
    ): String {
        val chan = NotificationChannel(
            channelId,
            channelName, NotificationManager.IMPORTANCE_LOW
        )
        chan.setShowBadge(false)
        val service = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    override fun onCreate() {
        super.onCreate()
        channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel("treehollow_service", "树洞消息")
            } else {
                // If earlier version channel ID is not used
                // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
                ""
            }
        foregroundChannelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createForegroundNotificationChannel("foreground_service", "树洞常驻通知")
            } else {
                // If earlier version channel ID is not used
                // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
                "foreground"
            }

    }

    override fun onDestroy() {
        super.onDestroy()
        if (connection != null) {
            connection!!.close()
        }
        Log.w("WebSocketService", "Destroy " + javaClass.simpleName)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (connection != null) {
            connection!!.close()
        }
        Log.i("WebSocketService", "Starting " + javaClass.simpleName)
        super.onStartCommand(intent, flags, startId)

        val ioScope = CoroutineScope(Dispatchers.IO + Job())
        ioScope.launch {

            val token_ = TreeHollowApplication.token
            val wsUrl = TreeHollowApplication.Companion.Config.getConfigItemString("websocket_url")
            if (token_ != null && wsUrl != null) {
                token = token_
                url = wsUrl + "v3/stream"
                missingMessageUtil =
                    MissedMessageUtil(token!!)
                Thread { startPushService() }.start()
//                when (val result = TreeHollowApplication.Companion.Config.initConfig(
//                    configUrl
//                )) {
//                    is Result.Success<TreeHollowConfig> -> {
//                        url = result.data.websocket_url + "v3/stream"
//                        missingMessageUtil =
//                            MissedMessageUtil(token!!)
//                        Thread { startPushService() }.start()
//                    }
//                    is Result.Error -> {
////                            this.stopSelf()
//                    }
//                }

            } else {
//           TODO
            }
        }
        return START_STICKY
    }

    private fun startPushService() {
//        UncaughtExceptionHandler.registerCurrentThread();
//        foreground(getString(R.string.websocket_init));
//
        if (lastReceivedMessage.get() == NOT_LOADED) {

            val ioScope = CoroutineScope(Dispatchers.IO + Job())
            ioScope.launch {
                lastReceivedMessage.set(missingMessageUtil!!.lastReceivedMessage())
                notifyMissedNotifications()
            }
        }
//        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        connection = WebSocketConnection(
            url!!,
            token!!,
            alarmManager,
            this
        )
            .onOpen { onOpen() }
            .onClose { onClose() }
            .onBadRequest(object : BadRequestRunnable {
                override fun execute(message: String) {
                    onBadRequest(message)
                }
            })
            .onNetworkFailure(
                object : OnNetworkFailureRunnable {
                    override fun execute(millis: Long) {
                        foreground("网络错误，${millis}分钟后重新尝试连接树洞推送服务")
                    }
                })
            .onDisconnect { onDisconnect() }
            .onMessage(object : SuccessCallback<String> {
                override fun onSuccess(data: String) {
                    val moshi = Moshi.Builder().build()
                    val jsonAdapter: JsonAdapter<ApiResponse.PushMessage> =
                        moshi.adapter(ApiResponse.PushMessage::class.java)
                    val msg = jsonAdapter.fromJson(data)
                    if (msg != null) {
                        onMessage(msg)
                    }
                }
            })
            .onReconnected(this::notifyMissedNotifications)
            .start()
//        val connectivityManager =
//            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            connectivityManager.registerDefaultNetworkCallback(
//                object : ConnectivityManager.NetworkCallback() {
//                    override fun onAvailable(network: Network) {
//                        super.onAvailable(network)
//                        doReconnect()
//                    }
//                })
//        } else {
        val intentFilter = IntentFilter()
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        val receiver = ReconnectListener { doReconnect() }
        registerReceiver(receiver, intentFilter)
//        }
    }

    private fun notifyMissedNotifications() {
        val messageId = lastReceivedMessage.get()
        if (messageId == NOT_LOADED) {
            return
        }


        val ioScope = CoroutineScope(Dispatchers.IO + Job())
        ioScope.launch {
            val messages: List<ApiResponse.PushMessage> =
                missingMessageUtil!!.missingMessages(messageId)
            for (message in messages) {
                onMessage(message)
            }
        }
    }

    private fun onClose() {
//        foreground(getString(R.string.websocket_closed_try_reconnect));
//        ClientFactory.userApiWithToken(settings)
//                .currentUser()
//                .enqueue(
//                        call(
//                                (ignored) -> this.doReconnect(),
//                                (exception) -> {
//                                    if (exception.code() == 401) {
//                                        foreground(getString(R.string.websocket_closed_logout));
//                                    } else {
//                                        Log.i(
//                                                "WebSocket closed but the user still authenticated, trying to reconnect");
//                                        this.doReconnect();
//                                    }
//                                }));
    }

    private fun onDisconnect() {
        foreground("无网络连接，消息推送服务连接失败")
    }

    private fun doReconnect() {
        if (connection == null) {
            return
        }
        connection!!.scheduleReconnect(15)
    }

    private fun onBadRequest(message: String) {
        foreground("消息推送服务连接失败: $message")
    }

    private fun onOpen() {
        foreground("已连接到消息推送服务")
    }

//    @JsonClass(generateAdapter = true)
//    data class PushMsg(
//        val body: String,
//        val pid: String?,
//        val cid: String?,
//        val title: String,
//        val type: Int
//    )

    private fun onMessage(msg: ApiResponse.PushMessage) {
        if (lastReceivedMessage.get() < msg.id) {
            lastReceivedMessage.set(msg.id)
        }

        broadcast(msg)

        if (msg.delete != 1) {
            val ioScope = CoroutineScope(Dispatchers.IO + Job())
            ioScope.launch {
                missingMessageUtil!!.saveOneMessage(msg)
            }

            showNotification(msg)
        } else {
            with(NotificationManagerCompat.from(this)) {
                cancel(msg.id)
            }
        }
    }

    private fun broadcast(message: ApiResponse.PushMessage) {
        val intent = Intent()
        intent.action = NEW_MESSAGE_BROADCAST
        intent.putExtra("message", message)
        sendBroadcast(intent)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun foreground(message: String) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
        val notification =
            NotificationCompat.Builder(this, foregroundChannelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setShowWhen(false)
                .setWhen(0)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setContentIntent(pendingIntent)
                .setColor(
                    Utils.getColor(
                        applicationContext, R.attr.color_all_button
                    )
                )
                .build()
        startForeground(-1, notification)
    }

    private fun showNotification(
        msg: ApiResponse.PushMessage
    ) {

//        String intentUrl =
//                Extras.getNestedValue(
//                        String.class, extras, "android::action", "onReceive", "intentUrl");
//
//        if (intentUrl != null) {
//            intent = new Intent(Intent.ACTION_VIEW);
//            intent.setData(Uri.parse(intentUrl));
//            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            startActivity(intent);
//        }

//        String url =
//                Extras.getNestedValue(String.class, extras, "client::notification", "click", "url");
//        if (url != null) {
//            intent = Intent(Intent.ACTION_VIEW)
//            intent.data = Uri.parse(url)
//        } else {

        val intent: Intent = if (msg.type == 0x01) {
            Intent(this, MessageActivity::class.java)
        } else {
            val tmp = Intent(this, PostDetailActivity::class.java)
            tmp.putExtra("pid", msg.pid)
            tmp.putExtra("cid", msg.cid)
            tmp
        }
//        }
        val contentIntent =
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val b = NotificationCompat.Builder(
            this, channelId
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            showNotificationGroup()
        }
        b.setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_ALL)
            .setWhen(System.currentTimeMillis())
            .setSmallIcon(R.mipmap.ic_launcher)
            .setTicker(getString(R.string.app_name) + " - " + msg.title)
            .setGroup(GROUP_MESSAGES)
            .setContentTitle(msg.title)
            .setContentText(msg.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(msg.body))
            .setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_SOUND)
            .setLights(Color.CYAN, 1000, 5000)
            .setColor(Utils.getColor(applicationContext, R.attr.color_all_button))
            .setContentIntent(contentIntent)
        with(NotificationManagerCompat.from(this)) {
            notify(msg.id, b.build())
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun showNotificationGroup() {
        val intent = Intent(this, MainActivity::class.java)
        val contentIntent =
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val b = NotificationCompat.Builder(
            this, channelId
        )
        b.setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_ALL)
            .setWhen(System.currentTimeMillis())
            .setSmallIcon(R.mipmap.ic_launcher)
            .setTicker(getString(R.string.app_name))
            .setGroup(GROUP_MESSAGES)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setContentTitle("TreeHollow Notifications")
            .setGroupSummary(true)
            .setContentText("TreeHollow Notifications")
            .setColor(Utils.getColor(applicationContext, R.attr.color_all_button))
            .setContentIntent(contentIntent)
        val notificationManager =
            this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(-5, b.build())
    }

    companion object {
        val NEW_MESSAGE_BROADCAST = WebSocketService::class.java.name + ".NEW_MESSAGE"
        private var url: String? = null
        private var token: String? = null
        const val NOT_LOADED = -3
        const val GROUP_MESSAGES = "TREEHOLLOW_GROUP_MESSAGES"
    }
}