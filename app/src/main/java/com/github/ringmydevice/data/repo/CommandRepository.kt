package com.github.ringmydevice.data.repo

import com.github.ringmydevice.data.model.CommandLog

interface CommandRepository {
    suspend fun log(entry: CommandLog)
    suspend fun latest(limit: Int = 20): List<CommandLog>

    companion object {
        fun fake(): CommandRepository = InMemoryCommandRepository()
    }
}

private class InMemoryCommandRepository : CommandRepository {
    private val storage = ArrayDeque<CommandLog>()

    override suspend fun log(entry: CommandLog) {
        storage.addFirst(entry.copy(id = (storage.firstOrNull()?.id ?: 0L) + 1))
        while (storage.size > 200) storage.removeLast()
    }

    override suspend fun latest(limit: Int): List<CommandLog> =
        storage.take(limit)
}
