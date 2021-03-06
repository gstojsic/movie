package org.skunkworks.movie.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class Actor(val factory: Boolean = false)