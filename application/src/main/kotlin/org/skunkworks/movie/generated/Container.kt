package org.skunkworks.movie.generated

open class Container<K, V> {
    private val container = hashMapOf<K, V>()

    open suspend fun get(key: K): V? {
        return container[key]
    }

    open suspend fun put(key: K, value: V): V? {
        return container.put(key, value)
    }

    open suspend fun computeIfAbsent(key: K, mappingFunction: (K) -> V): V {
        return container.computeIfAbsent(key, mappingFunction)
    }

    open suspend fun remove(key: K): V? {
        return container.remove(key)
    }
}