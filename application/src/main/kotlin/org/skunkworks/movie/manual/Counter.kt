package org.skunkworks.movie.manual

open class Counter(private val logger: Logger) {
    private var count = 0;
    open suspend fun increment(amount: Int) {
        count += amount
        logger.log("logging increment: $count")
    }

    open suspend fun getCount(): Int {
        logger.log("logging getCount: $count")
        return count
    }
}