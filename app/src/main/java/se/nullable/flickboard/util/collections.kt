package se.nullable.flickboard.util

fun <T> List<T>.unique(): List<T> = mutableSetOf<T>().let { seenItems ->
    this.filter(seenItems::add)
}