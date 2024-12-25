package se.nullable.flickboard.util

/**
 * Returns true [charIterator] is a prefix of this, or the other way around.
 *
 * If true: consumes [String.length] characters from [charIterator]
 * If false: the state of [charIterator] is undefined
 */
fun String.startsWithOrIsPrefixOf(charIterator: CharIterator): Boolean {
    val thisIterator = this.iterator()
    while (thisIterator.hasNext() && charIterator.hasNext()) {
        if (thisIterator.nextChar() != charIterator.nextChar()) {
            return false
        }
    }
    return true
}