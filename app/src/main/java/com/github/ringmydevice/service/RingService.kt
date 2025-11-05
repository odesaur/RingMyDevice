package com.github.ringmydevice.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

/** No-op stub so you can wire calls without bringing audio/notifications yet. */
class RingService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val seconds = intent?.getIntExtra("seconds", 5) ?: 5
        Log.d("RMD", "RingService start (stub), seconds=$seconds")
        stopSelf()
        return START_NOT_STICKY
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
