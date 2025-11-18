package com.github.ringmydevice.di

import android.content.Context
import com.github.ringmydevice.data.repo.AllowedContactsRepository
import com.github.ringmydevice.data.repo.CommandRepository

/**
 * Super simple service locator for Show & Tell 1.
 * Swap this to proper DI (Hilt/Koin) later.
 */
object AppGraph {
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun requireContext(): Context {
        check(::appContext.isInitialized) { "AppGraph must be initialized before use." }
        return appContext
    }

    // IMPORTANT: single instance so writers/readers see the same logs
    val commandRepo: CommandRepository by lazy { CommandRepository.fake() }
    val allowedRepo: AllowedContactsRepository by lazy { AllowedContactsRepository.getInstance(requireContext()) }
}
