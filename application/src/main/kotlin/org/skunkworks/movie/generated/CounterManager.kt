package org.skunkworks.movie.generated

import org.skunkworks.movie.annotation.Actor

@Actor
open class CounterManager(private val counterFactory: CounterFactory) {
    private val counters = HashMap<Int, Counter>()

    open suspend fun increment(key: Int, amount: Int = 0) {
        getCounter(key).increment(amount)
    }

    open suspend fun getCount(key: Int): Int {
        return getCounter(key).getCount()
    }

    open suspend fun getSum(): Int {
        return counters.values.map { it.getCount() }.sum()
    }

    private fun getCounter(key: Int): Counter {
        return counters.computeIfAbsent(key) { counterFactory.create(key) }
    }
}