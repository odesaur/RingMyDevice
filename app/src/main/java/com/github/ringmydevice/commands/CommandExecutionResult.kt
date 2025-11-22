package com.github.ringmydevice.commands

data class CommandExecutionResult(
    val commandId: CommandId,
    val status: CommandStatus,
    val feedbackMessage: String? = null,
    val logNotes: String? = null
)

enum class CommandStatus {
    SUCCESS,
    FAILURE,
    PERMISSION_MISSING,
    INVALID_ARGUMENTS
}
