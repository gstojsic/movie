package org.skunkworks.movie

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.skunkworks.movie.manual.CounterActor
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MovieApplication : CommandLineRunner {
	override fun run(vararg args: String?) {
		runBlocking {
			val counter = CounterActor.create(this)
			counter.increment( 2)
			counter.increment( 5)
			val count = counter.getCount()
			logger.info { "counter: $count" }
		}
	}
}

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
	runApplication<MovieApplication>(*args)
}
