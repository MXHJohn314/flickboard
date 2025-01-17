package se.nullable.flickboard.util

import android.icu.lang.UCharacter
import android.icu.text.Normalizer2

data class LastTypedData(val char: String?, val position: Int, val combiner: Combiner?) {
    data class Combiner(
        val original: String,
        val combinedReplacement: String,
        val baseCharLength: Int
    )

    fun tryCombineWith(
        nextChar: String,
        periodOnDoubleSpace: Boolean,
        // zalgo mode
        zalgoDatabase: ZalgoDatabase? = null,
    ): Combiner? {
        return when {
            periodOnDoubleSpace && char == " " && nextChar == " " -> Combiner(
                original = "  ",
                combinedReplacement = ". ",
                baseCharLength = char.length,
            )

            else -> zalgoDatabase?.findCombiner(char, nextChar) ?: run {
                val normalizer = Normalizer2.getNFKDInstance()
                val charCodePoint = char?.singleCodePointOrNull() ?: return null
                val combiningMark =
                    nextChar.asCombiningMarkOrNull(zalgoMode = zalgoDatabase != null) ?: return null
                val combiningMarkCodePoint = combiningMark.singleCodePointOrNull() ?: return null
                val composed =
                    normalizer.composePair(charCodePoint, combiningMarkCodePoint)
                if (composed >= 0) {
                    Combiner(
                        original = char + nextChar,
                        combinedReplacement = UCharacter.toString(composed),
                        baseCharLength = char.length,
                    )
                } else {
                    null
                }
            }
        }
    }
}

