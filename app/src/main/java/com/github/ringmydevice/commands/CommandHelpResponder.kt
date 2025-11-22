package com.github.ringmydevice.commands

object CommandHelpResponder {
    fun buildHelpMessageFromCommands(): String {
        val builder = StringBuilder("Commands:\n")
        CommandRegistry.commands.forEach { command ->
            builder.append(command.syntax)
                .append(" – ")
                .append(command.summary)
                .append('\n')
        }
        return builder.toString().trim()
    }
}
