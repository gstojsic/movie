package org.skunkworks.movie

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.skunkworks.movie.generated.CounterFactory
import org.skunkworks.movie.generated.CounterManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MovieApplication : CommandLineRunner {
    @Autowired
    private lateinit var counterManager: CounterManager

    override fun run(vararg args: String?) {
        runBlocking {
            log.info { "start" }
            (1..10).forEach {
                counterManager.increment(it, it)
            }
            (1..10).forEach {
                val count = counterManager.getCount(it)
                log.info { "knauter $it: $count" }
            }
            val sum = counterManager.getSum()
            log.info { "sum: $sum" }
            //manualActors()
        }
    }

//    private suspend fun CoroutineScope.manualActors() {
//        val logger = LoggerActor.create(this)
//        val counter = CounterActor.create(this, logger)
//        counter.increment(2)
//        counter.increment(5)
//        val count = counter.getCount()
//        log.info { "counter: $count" }
//    }
}

private val log = KotlinLogging.logger {}

fun main(args: Array<String>) {
    runApplication<MovieApplication>(*args)
}
