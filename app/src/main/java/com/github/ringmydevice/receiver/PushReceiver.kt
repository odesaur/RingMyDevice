package com.github.ringmydevice.receiver

import android.content.Context
import com.github.ringmydevice.commands.CommandProcessor
import com.github.ringmydevice.commands.CommandSource
import com.github.ringmydevice.data.repo.RmdServerRepository
import com.github.ringmydevice.di.AppGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.unifiedpush.android.connector.MessagingReceiver
import org.unifiedpush.android.connector.UnifiedPush
import org.unifiedpush.android.connector.RegistrationDialogContent
import java.util.ArrayList

class PushReceiver : MessagingReceiver() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onMessage(context: Context, message: ByteArray, instance: String) {
        scope.launch {
            val repo = RmdServerRepository.getInstance()
            val command = repo.fetchPendingCommand() ?: return@launch
            val baseCommand = AppGraph.settingsRepo.getRmdCommandKeyword()
            val wrapped = "$baseCommand $command"
            CommandProcessor.handle(context, sender = "push", rawMessage = wrapped, source = CommandSource.IN_APP)
            repo.clearPendingCommand()
        }
    }

    override fun onNewEndpoint(context: Context, endpoint: String, instance: String) {
        scope.launch {
            val repo = RmdServerRepository.getInstance()
            repo.registerPushEndpoint(endpoint)
            AppGraph.settingsRepo.setPushEndpoint(endpoint)
        }
    }

    override fun onUnregistered(context: Context, instance: String) {
        scope.launch {
            val repo = RmdServerRepository.getInstance()
            repo.registerPushEndpoint("")
            AppGraph.settingsRepo.setPushEndpoint("")
        }
    }

    override fun onRegistrationFailed(context: Context, instance: String) {
        // no-op
    }

    companion object {
        private const val DEFAULT_INSTANCE = "default"
        fun register(context: Context) {
            val distributors = UnifiedPush.getDistributors(context, ArrayList())
            if (distributors.isNotEmpty()) {
                UnifiedPush.registerAppWithDialog(context, DEFAULT_INSTANCE, RegistrationDialogContent(), ArrayList(), "")
            }
        }

        fun unregister(context: Context) {
            UnifiedPush.unregisterApp(context, DEFAULT_INSTANCE)
        }

        fun distributorAvailable(context: Context): Boolean {
            return UnifiedPush.getDistributors(context, ArrayList()).isNotEmpty()
        }
    }
}
