package com.github.ringmydevice

import android.app.Application
import com.github.ringmydevice.di.AppGraph
import org.osmdroid.config.Configuration

class RmdApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().userAgentValue = packageName
        AppGraph.init(this)
    }
}
