package com.github.ringmydevice.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.github.ringmydevice.R
import com.github.ringmydevice.util.CameraCapture
import com.github.ringmydevice.util.MmsSender
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedQueue

class CameraService : LifecycleService() {

    companion object {
        private const val CHANNEL_ID = "rmd_camera_channel"
        private const val NOTIFICATION_ID = 9982

        const val EXTRA_SENDER = "sender"
        const val EXTRA_FACING = "facing"

        const val FACING_FRONT = "front"
        const val FACING_BACK = "back"

        private val queue = ConcurrentLinkedQueue<Intent>()
        private var isRunning = false

        fun enqueueCapture(context: Context, sender: String, facing: String) {
            val intent = Intent(context, CameraService::class.java).apply {
                putExtra(EXTRA_SENDER, sender)
                putExtra(EXTRA_FACING, facing)
            }
            queue.add(intent)

            if (!isRunning) {
                if (Build.VERSION.SDK_INT >= 26) {
                    context.startForegroundService(Intent(context, CameraService::class.java))
                } else {
                    context.startService(Intent(context, CameraService::class.java))
                }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        startAsForeground()
        lifecycleScope.launch {
            waitUntilStarted()
            scope.launch { processQueue() }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    private fun startAsForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Camera Capture",
                NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(channel)
        }

        val notification = buildNotification("Preparing camera…")

        val result = kotlin.runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        }

        if (result.isFailure) {
            stopSelf()
        }
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Ring My Device")
            .setContentText(text)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private suspend fun waitUntilStarted() {
        while (lifecycle.currentState < androidx.lifecycle.Lifecycle.State.STARTED) {
            delay(50)
        }
    }

    private suspend fun processQueue() {
        while (queue.isNotEmpty()) {
            val work = queue.poll() ?: continue
            val sender = work.getStringExtra(EXTRA_SENDER) ?: continue
            val facing = work.getStringExtra(EXTRA_FACING) ?: continue

            updateNotification("Capturing $facing camera…")

            val uri = CameraCapture.capture(
                context = this,
                lifecycleOwner = this,
                facing = facing
            )

            updateNotification("Sending MMS…")

            if (uri != null) {
                val text = if (facing == FACING_FRONT) "Took front camera image" else "Took back camera image"
                MmsSender.sendMms(this, sender, uri, text)
            }

            updateNotification("Done.")
        }

        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        scope.cancel()
    }

    override fun onBind(intent: Intent): IBinder? = super.onBind(intent)
}