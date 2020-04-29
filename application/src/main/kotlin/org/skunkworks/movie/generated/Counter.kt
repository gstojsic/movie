package org.skunkworks.movie.generated

import org.skunkworks.movie.annotation.Actor

@Actor
open class Counter {
    private var count = 0;
    open suspend fun increment(amount: Int) {
        count += amount
    }

    open suspend fun getCount(): Int {
        return count
    }
}