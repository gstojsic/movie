package org.skunkworks.movie.manual

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.ExecutorService

class CounterFactory {
    @Autowired
    private lateinit var executorService: ExecutorService
    @Autowired
    private lateinit var logger: Logger

    fun create(): Counter {
        return CounterActor.create(CoroutineScope(executorService.asCoroutineDispatcher()), logger)
    }
}