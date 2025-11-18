package com.github.ringmydevice

import android.app.Application
import com.github.ringmydevice.di.AppGraph

class RmdApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
    }
}
