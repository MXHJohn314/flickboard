package se.nullable.flickboard.ui.util

/**
 * Replace the contents of the collection with a single value
 */
fun <T> MutableCollection<T>.replaceWith(value: T) {
    clear()
    add(value)
}