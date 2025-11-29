package com.github.ringmydevice.domain

import com.github.ringmydevice.data.model.CommandType

object CommandParser {
    /** Extremely simple parser for now: looks for keywords. */
    fun parse(body: String): CommandType = when {
        body.contains("ring", ignoreCase = true)   -> CommandType.RING
        body.contains("locate", ignoreCase = true) -> CommandType.LOCATE
        body.contains("photo", ignoreCase = true)  -> CommandType.PHOTO
        body.contains("camera", ignoreCase = true) -> CommandType.PHOTO
        body.contains("wipe", ignoreCase = true)   -> CommandType.WIPE
        else -> CommandType.UNKNOWN
    }
}
