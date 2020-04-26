package org.skunkworks.movie.manual

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.actor

class LoggerActor private constructor(coroutineScope: CoroutineScope) : Logger() {
    companion object {
        fun create(coroutineScope: CoroutineScope): Logger {
            return LoggerActor(coroutineScope)
        }
    }

    private val actor = coroutineScope.createActor()

    private fun CoroutineScope.createActor() = actor<Messages> {
        for (msg in channel) {
            when (msg) {
                is Messages.Log -> super.log(msg.message)
            }
        }
    }

    override suspend fun log(message: String) {
        actor.send(Messages.Log(message))
    }

    private sealed class Messages {
        class Log(val message: String) : Messages()
    }
}