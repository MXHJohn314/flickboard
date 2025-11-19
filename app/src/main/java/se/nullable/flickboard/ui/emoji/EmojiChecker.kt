package se.nullable.flickboard.ui.emoji

import android.graphics.Paint
import androidx.emoji2.text.EmojiCompat

class EmojiChecker(
    private val paint: Paint,
    private val emojiCompat: EmojiCompat
) {
    private val cache = mutableMapOf<String, Boolean>()

    /**
     * Returns true if [emoji] can be rendered by the app.
     *
     * If [compatMetadataVersion] is set (to [EmojiCompat.EDITOR_INFO_METAVERSION_KEY])
     * it can be used to limit the list to the emojis supported by another app.
     */
    fun hasEmoji(emoji: String, compatMetadataVersion: Int? = Int.MAX_VALUE): Boolean =
        cache.computeIfAbsent(emoji) {
            paint.hasGlyph(emoji) ||
                    (compatMetadataVersion != null &&
                            emojiCompat.getEmojiMatch(emoji, compatMetadataVersion)
                            == EmojiCompat.EMOJI_SUPPORTED)
        }
}