package se.nullable.flickboard.model.emoji

import android.icu.lang.UCharacter
import android.icu.lang.UProperty
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.stream.Collectors

/**
 * Must match [se.nullable.flickboard.build.EmojiDb]
 */
@Serializable
data class EmojiDb(val entries: Map<String, Metadata>) {
    @Serializable
    data class Metadata(
        @SerialName("n") val name: String,
        val skinTones: Boolean = false,
    )

    fun groupForBaseEmoji(baseEmoji: String): EmojiGroup = entries[baseEmoji].let { meta ->
        EmojiGroup(
            names = listOfNotNull(meta?.name),
            baseVariant = baseEmoji,
            variants = when {
                meta?.skinTones == true -> lazy {
                    EMOJI_SKIN_TONES.map { tone ->
                        var prev = 0
                        baseEmoji.codePoints().mapToObj { cp ->
                            when {
                                // Handshake should not have a skintone applied when used
                                // as a modifier rather than a standalone emoji
                                cp == 0x1F91D && prev == 0x200D -> UCharacter.toString(cp)

                                UCharacter.hasBinaryProperty(cp, UProperty.EMOJI_MODIFIER_BASE) ->
                                    UCharacter.toString(cp) + tone

                                else -> UCharacter.toString(cp)
                            }.also { prev = cp }
                        }.collect(Collectors.joining())
                    }
                }

                else -> null
            },
        )
    }
}

private val EMOJI_SKIN_TONES = listOf(
    "",
    UCharacter.toString(0x1F3FB),
    UCharacter.toString(0x1F3FC),
    UCharacter.toString(0x1F3FD),
    UCharacter.toString(0x1F3FE),
    UCharacter.toString(0x1F3FF),
)
