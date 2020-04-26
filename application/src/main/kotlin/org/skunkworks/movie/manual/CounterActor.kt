package org.skunkworks.movie.manual

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.actor

class CounterActor private constructor(coroutineScope: CoroutineScope, logger: Logger) : Counter(logger) {
    companion object {
        fun create(coroutineScope: CoroutineScope, logger: Logger): Counter {
            return CounterActor(coroutineScope, logger)
        }
    }

    private val actor = coroutineScope.createActor()

    private fun CoroutineScope.createActor() = actor<Messages> {
        for (msg in channel) {
            when (msg) {
                is Messages.Increment -> super.increment(msg.amount)
                is Messages.GetCounter -> msg.response.complete(super.getCount())
            }
        }
    }

    override suspend fun increment(amount: Int) {
        actor.send(Messages.Increment(amount))
    }

    override suspend fun getCount(): Int {
        val response = CompletableDeferred<Int>()
        actor.send(Messages.GetCounter(response))
        return response.await()
    }

    private sealed class Messages {
        class Increment(val amount: Int) : Messages()
        class GetCounter(val response: CompletableDeferred<Int>) : Messages()
    }
}