package org.skunkworks.movie

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.skunkworks.movie.generated.Counter
import org.skunkworks.movie.manual.CounterFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MovieApplication : CommandLineRunner {
    //@Autowired
    //private lateinit var counter: Counter
    @Autowired
    private lateinit var counterFactory: CounterFactory

    override fun run(vararg args: String?) {
        runBlocking {
            log.info { "start" }
            val c = counterFactory.create()
            //counter.increment()
            //counter.increment(5)
            //val inc = counter.incrementAndGet(7)
            //log.info { "inc: $inc" }

            //val count = counter.getCount()
            //log.info { "counter: $count" }

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
