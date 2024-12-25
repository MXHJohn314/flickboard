package se.nullable.flickboard.ui

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import se.nullable.flickboard.R
import se.nullable.flickboard.model.emoji.EmojiTrie
import se.nullable.flickboard.ui.theme.FlickBoardTheme
import se.nullable.flickboard.ui.util.Dollar1GestureLibraryProvider

val LocalEmojiTrie = compositionLocalOf<Lazy<EmojiTrie>> {
    throw Exception("LocalEmojiTrie used outside of FlickBoardParent")
}

@OptIn(ExperimentalSerializationApi::class)
@Composable
fun FlickBoardParent(prefs: SharedPreferences? = null, content: @Composable () -> Unit) {
    AppSettingsProvider(prefs) {
        Dollar1GestureLibraryProvider {
            CompositionLocalProvider(
                LocalEmojiTrie provides run {
                    val context = LocalContext.current
                    remember(context) {
                        lazy {
                            context.resources.openRawResource(R.raw.emoji_trie)
                                .use { emojiJsonStr ->
                                    EmojiTrie(root = Json.decodeFromStream(emojiJsonStr))
                                }
                        }
                    }
                },
            ) {
                FlickBoardTheme(content = content)
            }
        }
    }
}