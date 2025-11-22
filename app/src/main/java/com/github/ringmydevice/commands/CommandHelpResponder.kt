package com.github.ringmydevice.commands

object CommandHelpResponder {
    fun buildHelpMessageFromCommands(baseCommand: String): String {
        val trimmedBase = baseCommand.trim()
        return buildString {
            append("Commands:\n")
            CommandRegistry.commands.forEach { command ->
                append("- ")
                append(trimmedBase)
                append(' ')
                append(command.syntax)
                append(" - ")
                append(command.summary)
                append('\n')
            }
        }.trimEnd()
    }
}
