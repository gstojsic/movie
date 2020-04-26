package org.skunkworks.movie.manual

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

open class Logger {
    open suspend fun log(message: String) {
        logger.info { "message: $message" }
    }
}
