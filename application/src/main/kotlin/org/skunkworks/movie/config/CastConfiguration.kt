package org.skunkworks.movie.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import org.skunkworks.movie.manual.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Configuration
class CastConfiguration {

   // @Bean
    //fun executor1(): ExecutorService = Executors.newSingleThreadExecutor()

    @Bean
    fun logger1(executor: ExecutorService): Logger {
        return LoggerActor.create(CoroutineScope(executor.asCoroutineDispatcher()))
    }

    @Bean
    fun counter1(executor: ExecutorService, logger: Logger): Counter {
        return CounterActor.create(CoroutineScope(executor.asCoroutineDispatcher()), logger)
    }
    @Bean
    fun counterfactory1(executor: ExecutorService, logger: Logger): CounterFactory {
        return CounterFactory()
    }
}