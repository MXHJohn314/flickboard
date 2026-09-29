package se.nullable.flickboard.util

import org.junit.Assert
import org.junit.Test

class FloatRingIteratorTest {
    fun <T> RingBuffer<T>.toList() = mutableListOf<T>().also {
        forEach(it::add)
    }

    @Test
    fun pushAndIterate() {
        (1..10).forEach { capacity ->
            (0..20).forEach { valueCount ->
                val buf = ringBuffer<Int>(capacity)
                val values = (1..valueCount).toList()
                values.forEach(buf::push)
                Assert.assertEquals(
                    valueCount.coerceAtMost(capacity),
                    buf.size,
                )
                Assert.assertEquals(values.takeLast(capacity), buf.toList())
            }
        }
    }
}