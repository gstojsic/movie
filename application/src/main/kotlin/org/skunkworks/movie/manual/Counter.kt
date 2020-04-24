package org.skunkworks.movie.manual

class Counter : CounterMessages {
    private var count = 0;
    override suspend fun increment(amount: Int) {
        count += amount
    }

    override suspend fun getCount(): Int {
        return count
    }
}

interface CounterMessages {
    suspend fun increment(amount: Int)
    suspend fun getCount(): Int
}