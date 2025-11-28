package com.github.ringmydevice.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
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
            Log.d("RMD_CAMERA", "enqueueCapture(sender=$sender, facing=$facing)")
            val intent = Intent(context, CameraService::class.java).apply {
                putExtra(EXTRA_SENDER, sender)
                putExtra(EXTRA_FACING, facing)
            }
            queue.add(intent)

            if (!isRunning) {
                Log.d("RMD_CAMERA", "Starting CameraService…")
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
        Log.d("RMD_CAMERA", "CameraService.onCreate")
        isRunning = true
        startAsForeground()
        lifecycleScope.launch {
            waitUntilStarted()
            Log.d("RMD_CAMERA", "Lifecycle STARTED — binding CameraX is allowed now")
            scope.launch { processQueue() }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("RMD_CAMERA", "onStartCommand received")
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    private fun startAsForeground() {
        Log.d("RMD_CAMERA", "startAsForeground() called")

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
            Log.e("RMD_CAMERA", "Failed to startForeground()", result.exceptionOrNull())
            stopSelf()
        } else {
            Log.d("RMD_CAMERA", "startForeground() OK")
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
        Log.d("RMD_CAMERA", "updateNotification: $text")
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private suspend fun waitUntilStarted() {
        Log.d("RMD_CAMERA", "Waiting for lifecycle to reach STARTED…")
        while (lifecycle.currentState < androidx.lifecycle.Lifecycle.State.STARTED) {
            delay(50)
        }
    }

    private suspend fun processQueue() {
        Log.d("RMD_CAMERA", "processQueue() starting")

        while (queue.isNotEmpty()) {
            val work = queue.poll() ?: continue
            val sender = work.getStringExtra(EXTRA_SENDER) ?: continue
            val facing = work.getStringExtra(EXTRA_FACING) ?: continue

            Log.d("RMD_CAMERA", "Processing job: sender=$sender, facing=$facing")

            updateNotification("Capturing $facing camera…")

            val uri = CameraCapture.capture(
                context = this,
                lifecycleOwner = this,
                facing = facing
            )

            Log.d("RMD_CAMERA", "CameraCapture result uri=$uri")

            updateNotification("Sending MMS…")

            if (uri != null) {
                val text = if (facing == FACING_FRONT) "Took front camera image" else "Took back camera image"
                Log.d("RMD_CAMERA", "Sending MMS to $sender")
                MmsSender.sendMms(this, sender, uri, text)
            } else {
                Log.e("RMD_CAMERA", "Capture failed — URI was null")
            }

            updateNotification("Done.")
        }

        Log.d("RMD_CAMERA", "Queue empty — stopping service")
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("RMD_CAMERA", "CameraService.onDestroy")
        isRunning = false
        scope.cancel()
    }

    override fun onBind(intent: Intent): IBinder? = super.onBind(intent)
}