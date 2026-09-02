package se.nullable.flickboard.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

sealed interface Translated {
    @Composable
    fun get(): String

    /**
     * A string value that should be rendered as translated at runtime
     *
     * Compared to [stringResource], constructing it doesn't require being in a [Composable].
     * Compared to a @[StringRes] [Int], it allows passing formatting arguments, and many strings
     * can be combined using [Concat].
     */
    data class Part(@param:StringRes val id: Int, val args: Array<out Any> = emptyArray()) :
        Translated {
        @Composable
        override fun get(): String = stringResource(id, formatArgs = args)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Part

            if (id != other.id) return false
            if (!args.contentEquals(other.args)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id
            result = 31 * result + args.contentHashCode()
            return result
        }
    }

    /**
     * Combines many [Translated]s
     */
    data class Concat(val parts: List<Translated>, val separator: String) : Translated {
        @Composable
        override fun get(): String =
        // List.joinToString is not inlined over transform,
            // so it doesn't preserve the @Composable context
            @Suppress("SimplifiableCallChain")
            parts.map { it.get() }.joinToString(separator = separator)
    }
}

fun translated(@StringRes id: Int, vararg args: Any): Translated =
    Translated.Part(id = id, args = args)
