package com.github.ringmydevice

import android.content.Context
import com.github.ringmydevice.data.repo.RmdServerRepository
import com.github.ringmydevice.receiver.PushReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Light startup helper to re-register push endpoint (if present) when the app launches.
 */
object AppStartup {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun onAppStart(context: Context) {
        scope.launch {
            // If Sunup is installed and we have an endpoint, ensure it's registered server-side.
            val repo = RmdServerRepository.getInstance()
            repo.syncStoredPushEndpoint()
        }
        // Do not auto-register push here; leave registration to explicit user action.
    }
}
