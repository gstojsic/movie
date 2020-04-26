package org.skunkworks.movie.generated

open class Counter {
    private var count = 0;
    open suspend fun increment(amount: Int) {
        count += amount
    }

    open suspend fun getCount(): Int {
        return count
    }
}