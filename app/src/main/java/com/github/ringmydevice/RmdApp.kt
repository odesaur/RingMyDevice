package com.github.ringmydevice

import android.app.Application
import com.github.ringmydevice.di.AppGraph
import com.github.ringmydevice.service.RmdServerPoller
import com.github.ringmydevice.AppStartup
import org.osmdroid.config.Configuration

class RmdApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().userAgentValue = packageName
        AppGraph.init(this)
        // Attempt to restore push registration and sync endpoint on startup.
        AppStartup.onAppStart(this)
    }
}
