package com.github.ringmydevice.service

import android.content.Context
import com.github.ringmydevice.commands.CommandProcessor
import com.github.ringmydevice.commands.CommandSource
import com.github.ringmydevice.data.repo.RmdServerRepository
import com.github.ringmydevice.di.AppGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/**
 * Very small background poller that checks the self-hosted server for pending commands.
 * This runs while the app process is alive; if the OS kills the process, polling stops.
 */
object RmdServerPoller {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var job: Job? = null

    fun start(context: Context) {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                runCatching { pollOnce(context) }
                delay(15 * 60 * 1000L) // 15 minutes
            }
        }
    }

    private suspend fun pollOnce(context: Context) = withContext(Dispatchers.IO) {
        val repo = RmdServerRepository.getInstance()
        val command = repo.fetchPendingCommand() ?: return@withContext
        val baseCommand = AppGraph.settingsRepo.getRmdCommandKeyword()
        val wrappedCommand = "$baseCommand $command"
        CommandProcessor.handle(context, sender = "server", rawMessage = wrappedCommand, source = CommandSource.IN_APP)
        repo.clearPendingCommand()
    }
}
