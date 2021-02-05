package com.github.treehollow.push

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ReconnectListener internal constructor(private val reconnected: Runnable) :
    BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Utils.isNetworkAvailable(context)) {
            Log.i("ReconnectListener", "Network reconnected")
            reconnected.run()
        }
    }
}