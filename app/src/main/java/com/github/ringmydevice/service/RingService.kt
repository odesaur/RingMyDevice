package com.github.ringmydevice.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.pm.ServiceInfo
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.github.ringmydevice.ui.ringing.RingingActivity

class RingService : Service() {
    private val chId = "rmd_ring"
    private var tg: ToneGenerator? = null
    private var ringtone: Ringtone? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        val seconds = intent?.getIntExtra(EXTRA_SECONDS, DEFAULT_SECONDS) ?: DEFAULT_SECONDS
        val ringtoneUri = intent?.getStringExtra(EXTRA_RINGTONE_URI)

        val notification = buildNotif()
        kotlin.runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        }.onFailure {
            Log.e("RMD", "Unable to start foreground service", it)
            stopSelf()
            return START_NOT_STICKY
        }

        startTone(seconds, ringtoneUri)
        showRingingActivity()
        return START_STICKY
    }

    private fun startTone(seconds: Int, uriString: String?) {
        val uri = uriString?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        if (!playRingtone(uri, seconds)) {
            playFallbackTone(seconds)
        }
    }

    private fun playRingtone(uri: Uri?, seconds: Int): Boolean {
        if (uri == null) return false
        val ring = runCatching { RingtoneManager.getRingtone(this, uri) }.getOrNull() ?: return false
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.setStreamVolume(
            AudioManager.STREAM_ALARM,
            am.getStreamMaxVolume(AudioManager.STREAM_ALARM),
            0
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ring.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        } else {
            @Suppress("DEPRECATION")
            ring.streamType = AudioManager.STREAM_ALARM
        }
        ringtone = ring
        ring.play()
        handler.postDelayed({ stopSelf() }, seconds * 1000L)
        return true
    }

    private fun playFallbackTone(seconds: Int) {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        @Suppress("DEPRECATION")
        am.adjustStreamVolume(AudioManager.STREAM_ALARM, AudioManager.ADJUST_RAISE, 0)

        tg = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        tg?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD)
        handler.postDelayed({ stopSelf() }, seconds * 1000L)
    }

    private fun showRingingActivity() {
        val intent = Intent(this, RingingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
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
        ringtone?.stop()
        tg?.stopTone()
        tg?.release()
        ringtone = null
        sendBroadcast(Intent(ACTION_RING_ENDED))
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val DEFAULT_SECONDS = 5
        private const val NOTIFICATION_ID = 1
        const val EXTRA_SECONDS = "seconds"
        const val EXTRA_RINGTONE_URI = "ringtone_uri"
        const val ACTION_STOP = "com.github.ringmydevice.RING_STOP"
        const val ACTION_RING_ENDED = "com.github.ringmydevice.RING_ENDED"
    }
}
