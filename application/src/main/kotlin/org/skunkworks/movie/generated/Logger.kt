package org.skunkworks.movie.generated

import mu.KotlinLogging
import org.skunkworks.movie.annotation.Actor
import java.sql.Date

private val logger = KotlinLogging.logger {}

//@Actor
open class Logger {
    open suspend fun blog(message: String, bla: Int, truc: Date) {
        logger.info { "message: $message" }
    }
    open suspend fun log(message: String) {
        logger.info { "message: $message" }
    }
}
