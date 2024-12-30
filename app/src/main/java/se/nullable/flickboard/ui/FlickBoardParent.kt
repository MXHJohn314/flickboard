package se.nullable.flickboard.ui

import android.content.SharedPreferences
import android.gesture.GestureLibraries
import android.gesture.GestureLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.room.Room
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import se.nullable.flickboard.R
import se.nullable.flickboard.model.AppDatabase
import se.nullable.flickboard.model.emoji.EmojiDb
import se.nullable.flickboard.model.emoji.EmojiTrie
import se.nullable.flickboard.ui.theme.FlickBoardTheme
import se.nullable.flickboard.util.Boxed

val LocalEmojiTrie = staticCompositionLocalOf<Lazy<EmojiTrie>> {
    throw Exception("LocalEmojiTrie used outside of FlickBoardParent")
}
val LocalEmojiDb = staticCompositionLocalOf<Lazy<EmojiDb>> {
    throw Exception("LocalEmojiDb used outside of FlickBoardParent")
}
val LocalAppDatabase = staticCompositionLocalOf<Lazy<AppDatabase>> {
    throw Exception("LocalAppDatabase used outside of FlickBoardParent")
}

// HACK: Boxing allows the variable to exist on platforms that do not provide the type
// (like the IDE previews)
val LocalDollar1GestureLibrary = staticCompositionLocalOf<Lazy<Boxed<GestureLibrary>>> {
    throw Exception("LocalDollar1GestureLibrary used outside of FlickBoardParent")
}

@OptIn(ExperimentalSerializationApi::class)
@Composable
fun FlickBoardParent(prefs: SharedPreferences? = null, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val emojiTrie = remember(context) {
        lazy {
            context.resources.openRawResource(R.raw.emoji_trie).use { emojiJsonStr ->
                EmojiTrie(root = Json.decodeFromStream(emojiJsonStr))
            }
        }
    }
    val emojiDb = remember(context) {
        lazy {
            context.resources.openRawResource(R.raw.emoji_db).use { emojiJsonStr ->
                Json.decodeFromStream<EmojiDb>(emojiJsonStr)
            }
        }
    }
    val appDatabase = remember(context) {
        lazy {
            Room.databaseBuilder(context, AppDatabase::class.java, "flickboard-appdb").build()
        }
    }
    val dollar1GestureLibrary: Lazy<Boxed<GestureLibrary>> = remember(context) {
        lazy {
            GestureLibraries.fromRawResource(context, R.raw.gestures)
                .also {
                    // GestureLibrary.ORIENTATION_STYLE_8
                    // required for recognizing 8 orientations
                    // of otherwise equivalent gestures
                    it.orientationStyle = 8
                    it.load()
                }
                .let(::Boxed)
        }
    }
    CompositionLocalProvider(
        LocalEmojiTrie provides emojiTrie,
        LocalEmojiDb provides emojiDb,
        LocalAppDatabase provides appDatabase,
        LocalDollar1GestureLibrary provides dollar1GestureLibrary,
    ) {
        AppSettingsProvider(prefs) {
            FlickBoardTheme(content = content)
        }
    }
}