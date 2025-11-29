package com.github.ringmydevice.commands

object CommandFeedbackBuilder {
    fun buildFeedbackMessage(
        baseCommand: String,
        args: List<String>,
        result: CommandExecutionResult
    ): String? {
        return when (result.commandId) {
            CommandId.RING -> buildRingFeedback(result)
            CommandId.NODISTURB -> result.feedbackMessage ?: "Do Not Disturb mode has been updated."
            CommandId.RINGER_MODE -> result.feedbackMessage ?: "The ringer mode has been changed."
            CommandId.STATS -> result.feedbackMessage ?: "Network statistics have been requested. You will receive them shortly."
            CommandId.LOCATE -> result.feedbackMessage ?: "A location request has been triggered."
            CommandId.CAMERA -> result.feedbackMessage ?: "A photo request has been triggered."
            CommandId.LOCK -> result.feedbackMessage ?: "The device has been locked."
            CommandId.HELP -> result.feedbackMessage ?: CommandHelpResponder.buildHelpMessageFromCommands(baseCommand)
            CommandId.UNKNOWN -> result.feedbackMessage
        }?.let { messageForStatus(result, it, args) }
    }

    private fun messageForStatus(
        result: CommandExecutionResult,
        defaultMessage: String,
        args: List<String>
    ): String {
        if (result.status == CommandStatus.SUCCESS) return defaultMessage
        val fallback = when (result.status) {
            CommandStatus.PERMISSION_MISSING -> "The command could not be executed because required permissions are missing."
            CommandStatus.INVALID_ARGUMENTS -> "The command could not be executed: invalid arguments."
            else -> "The command could not be executed due to an error."
        }
        return result.feedbackMessage?.takeIf { it.isNotBlank() } ?: fallbackWithArgs(fallback, args)
    }

    private fun fallbackWithArgs(fallback: String, args: List<String>): String =
        if (args.isEmpty()) fallback else "$fallback Args: ${args.joinToString(" ")}"

    private fun buildRingFeedback(result: CommandExecutionResult): String {
        return if (result.status == CommandStatus.SUCCESS) {
            "The device should now be ringing."
        } else {
            result.feedbackMessage ?: "The ring command could not be executed because required permissions are missing."
        }
    }
}
