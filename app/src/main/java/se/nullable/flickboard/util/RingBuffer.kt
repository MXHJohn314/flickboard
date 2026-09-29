package se.nullable.flickboard.util

inline fun <reified T> ringBuffer(capacity: Int) =
    RingBuffer.Mutable(
        arrayOfNulls<T>(
            capacity
                    // sentinel
                    + 1,
        ),
    )

/**
 * Ring buffer of values, deleting the oldest value if the capacity is exceeded
 *
 * Construct with [ringBuffer]
 */
sealed class RingBuffer<T>(protected val buf: Array<T?>) {
    // The effective capacity is buf.size-1, because readIndex-1
    // is a sentinel slot
    // (otherwise we'd be unable to distinguish full and empty buffers
    // without a bunch of extra machinery,
    // because both would be expressed as writeIndex==readIndex)
    val slots = buf.size
    var readIndex = 0
        protected set
    var writeIndex = 0
        protected set

    val size
        get() = (writeIndex - readIndex).mod(slots)

    // public for inline funs, you probably don't want to use this directly!
    fun getAtRawIndex(i: Int) = buf[i]

    /**
     * Calls [f] for each value,
     * without removing the iterated values from the buffer
     *
     * (Two successive calls to [forEach] will return the same values)
     */
    // Values are only null (unless T is nullable anyway and the user has supplied a null value manually)
    // if they have never been written to, in which case it should never be returned by forEach.
    // Not using !! since it's perfectly valid to make a RingBuffer<T?> with "legit" nulls.
    @Suppress("UNCHECKED_CAST")
    inline fun forEach(f: (T) -> Unit) {
        if (writeIndex >= readIndex) {
            for (i in readIndex..<writeIndex) {
                f(getAtRawIndex(i) as T)
            }
        } else {
            for (i in readIndex..<slots) {
                f(getAtRawIndex(i) as T)
            }
            for (i in 0..<writeIndex) {
                f(getAtRawIndex(i) as T)
            }
        }
    }

    class Mutable<T>(buf: Array<T?>) : RingBuffer<T>(buf) {
        fun clear() {
            readIndex = writeIndex
        }

        /**
         * Adds [value] to the ring buffer,
         * overwriting the oldest value if the capacity is exceeded
         */
        fun push(value: T) {
            buf[writeIndex] = value
            writeIndex = (writeIndex + 1) % slots
            if (writeIndex == readIndex) {
                readIndex = (writeIndex + 1) % slots
            }
        }
    }
}

