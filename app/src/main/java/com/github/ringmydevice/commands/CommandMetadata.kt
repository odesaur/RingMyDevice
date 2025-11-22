package com.github.ringmydevice.commands

enum class CommandId {
    RING,
    RINGER_MODE,
    STATS,
    BLUETOOTH,
    CAMERA,
    DELETE
}

data class CommandMetadata(
    val id: CommandId,
    val syntax: String,
    val summary: String,
    val description: String,
    val requiredPermissions: List<String>,
    val optionalPermissions: List<String> = emptyList(),
    val details: List<String> = emptyList(),
    val smsExample: String
)

object CommandRegistry {
    val commands: List<CommandMetadata> = listOf(
        CommandMetadata(
            id = CommandId.RING,
            syntax = "ring [long]",
            summary = "Make the device ring",
            description = "Play a loud alert so you can locate your device. Add \"long\" for a longer alarm.",
            requiredPermissions = listOf("Do Not Disturb access", "Display over other apps"),
            smsExample = "ring long"
        ),
        CommandMetadata(
            id = CommandId.RINGER_MODE,
            syntax = "ringermode [normal | vibrate | silent]",
            summary = "Change the ringer mode",
            description = "Switch the device ringer mode. Silent also enables Do Not Disturb.",
            requiredPermissions = listOf("Do Not Disturb access"),
            details = listOf("Android enables Do Not Disturb automatically when switching to silent."),
            smsExample = "ringermode vibrate"
        ),
        CommandMetadata(
            id = CommandId.STATS,
            syntax = "stats",
            summary = "Get network statistics",
            description = "Retrieve recent network statistics from the device.",
            requiredPermissions = listOf("Location"),
            smsExample = "stats"
        ),
        CommandMetadata(
            id = CommandId.BLUETOOTH,
            syntax = "bluetooth [on | off]",
            summary = "Toggle Bluetooth",
            description = "Enable or disable Bluetooth remotely.",
            requiredPermissions = listOf("Connect Bluetooth"),
            smsExample = "bluetooth on"
        ),
        CommandMetadata(
            id = CommandId.CAMERA,
            syntax = "camera [front | back]",
            summary = "Take a picture",
            description = "Capture a picture using the front or rear camera.",
            requiredPermissions = listOf("Camera"),
            smsExample = "camera front"
        ),
        CommandMetadata(
            id = CommandId.DELETE,
            syntax = "delete <pin> [dryrun]",
            summary = "Factory reset the device",
            description = "Factory reset the device using the configured PIN. Add \"dryrun\" to test without wiping.",
            requiredPermissions = listOf("Device admin"),
            smsExample = "delete 1234"
        )
    )
}
