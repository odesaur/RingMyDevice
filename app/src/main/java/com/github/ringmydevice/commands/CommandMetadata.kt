package com.github.ringmydevice.commands

enum class CommandId {
    NODISTURB,
    RING,
    RINGER_MODE,
    STATS,
    GPS,
    LOCATE,
    LOCK,
    HELP,
    CAMERA,
    UNKNOWN
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
            id = CommandId.NODISTURB,
            syntax = "nodisturb [on | off]",
            summary = "Toggle Do Not Disturb on and off",
            description = "Turn Do Not Disturb on or off remotely.",
            requiredPermissions = listOf("Do Not Disturb access"),
            smsExample = "nodisturb on"
        ),
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
            summary = "Get device network info",
            description = "List active interface IPs and connected or nearby Wi-Fi networks.",
            requiredPermissions = listOf("Location", "Nearby Wi-Fi devices"),
            details = listOf("Wi-Fi needs to be on to list nearby networks."),
            smsExample = "stats"
        ),
        CommandMetadata(
            id = CommandId.GPS,
            syntax = "gps [on | off]",
            summary = "Toggle GPS on and off",
            description = "Turn GPS on or off remotely.",
            requiredPermissions = listOf("Write to secure settings"),
            smsExample = "gps on"
        ),
        CommandMetadata(
            id = CommandId.LOCATE,
            syntax = "locate [last | all | cell | gps]",
            summary = "Locate the device",
            description = "Locate the device using GPS, cell data, or other providers.",
            requiredPermissions = listOf("Location"),
            optionalPermissions = listOf("Write to secure settings"),
            smsExample = "locate gps"
        ),
        CommandMetadata(
            id = CommandId.LOCK,
            syntax = "lock [msg]",
            summary = "Lock the device",
            description = "Lock the device and optionally show a message on the lock screen.",
            requiredPermissions = listOf("Device admin"),
            smsExample = "lock help"
        ),
        CommandMetadata(
            id = CommandId.HELP,
            syntax = "help",
            summary = "Show the help",
            description = "Send a list of available commands.",
            requiredPermissions = emptyList(),
            smsExample = "help"
        ),
        CommandMetadata(
            id = CommandId.CAMERA,
            syntax = "camera [front | back]",
            summary = "Take a photo",
            description = "Take a photo using the front or back camera and send it via SMS.",
            requiredPermissions = listOf("Camera"),
            smsExample = "camera front"
        )
    )
}
