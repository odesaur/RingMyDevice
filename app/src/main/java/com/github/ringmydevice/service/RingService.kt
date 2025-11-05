package com.github.ringmydevice.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import android.app.Service

class RingService : Service() {
    private val chId = "rmd_ring"
    private var tg: ToneGenerator? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val seconds = intent?.getIntExtra("seconds", 5) ?: 5
        startForeground(1, buildNotif())
        startTone(seconds)
        return START_NOT_STICKY
    }

    private fun startTone(seconds: Int) {
        // Boost alarm stream a bit (non-persistent)
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        @Suppress("DEPRECATION")
        am.adjustStreamVolume(AudioManager.STREAM_ALARM, AudioManager.ADJUST_RAISE, 0)

        tg = ToneGenerator(AudioManager.STREAM_ALARM, /* volume */ 100)
        // Long tone; we’ll stop it after <seconds>
        tg?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD)
        handler.postDelayed({ stopSelf() }, seconds * 1000L)
    }

    private fun buildNotif(): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(chId, "Ringing", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        return NotificationCompat.Builder(this, chId)
            .setContentTitle("Ringing device…")
            .setContentText("Audible alert active")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        tg?.stopTone()
        tg?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
