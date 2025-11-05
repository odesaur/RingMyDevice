package com.github.ringmydevice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.github.ringmydevice.ui.HomeSetup
import com.github.ringmydevice.ui.theme.RMDTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RMDTheme { HomeSetup() } }
    }
}
