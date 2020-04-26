package org.skunkworks.movie.generated

import mu.KotlinLogging
import org.skunkworks.movie.annotation.Actor

private val logger = KotlinLogging.logger {}

@Actor
open class Logger {
    open suspend fun log(message: String) {
        logger.info { "message: $message" }
    }
}
