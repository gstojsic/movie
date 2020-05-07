package org.skunkworks.movie.generated

import org.skunkworks.movie.annotation.Actor
import org.skunkworks.movie.annotation.Provide

@Actor(factory = true)
open class Counter(
        @Provide private val key: Int,
        private val logger: Logger
) {

    private var count = 0;
    open suspend fun increment(amount: Int = 0) {
        count += amount
        logger.log("logging increment: $count")
    }

    open suspend fun incrementAndGet(amount: Int): Int {
        count += amount
        logger.log("logging increment and get: $count")
        return count
    }

    open suspend fun incrementAndGetList(amount: Int): List<Int> {
        count += amount
        logger.log("logging increment and get: $count")
        return listOf(count)
    }

    open suspend fun incrementList(amountList: List<Int>): Int {
        if (amountList.isNotEmpty()) {
            count += amountList.sum()
            logger.log("logging increment list: $count")
        }
        return count
    }

    open suspend fun getCount(): Int {
        return count
    }
}