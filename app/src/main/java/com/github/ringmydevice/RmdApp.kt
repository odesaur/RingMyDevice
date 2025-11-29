package com.github.ringmydevice

import android.app.Application
import com.github.ringmydevice.di.AppGraph
import com.github.ringmydevice.service.RmdServerPoller
import org.osmdroid.config.Configuration

class RmdApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().userAgentValue = packageName
        AppGraph.init(this)
        // Start lightweight poller to fetch remote commands from the self-hosted server.
        RmdServerPoller.start(this)
    }
}
