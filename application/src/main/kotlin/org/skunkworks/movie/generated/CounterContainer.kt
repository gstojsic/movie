package org.skunkworks.movie.generated

import org.skunkworks.movie.annotation.Actor

//Todo: fix to get messages from parent (Container)
//@Actor
open class CounterContainer : Container<Int, Counter>() {
    override suspend fun computeIfAbsent(key: Int, mappingFunction: (Int) -> Counter): Counter {
        return super.computeIfAbsent(key, mappingFunction)
    }
}