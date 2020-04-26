package org.skunkworks.movie

import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.skunkworks.movie.generated.LoggerGeneratedActor
import org.skunkworks.movie.manual.CounterActor
import org.skunkworks.movie.manual.LoggerActor
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MovieApplication : CommandLineRunner {
    override fun run(vararg args: String?) {
        runBlocking {
            val t = LoggerGeneratedActor()
            val logger = LoggerActor.create(this)
            val counter = CounterActor.create(this, logger)
            counter.increment(2)
            counter.increment(5)
            val count = counter.getCount()
            log.info { "counter: $count" }
        }
    }
}

private val log = KotlinLogging.logger {}

fun main(args: Array<String>) {
    runApplication<MovieApplication>(*args)
}
