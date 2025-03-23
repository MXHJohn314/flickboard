package se.nullable.flickboard.ui

import android.content.Context
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
import se.nullable.flickboard.model.credits.DependencyInfo
import se.nullable.flickboard.model.emoji.EmojiDb
import se.nullable.flickboard.model.emoji.EmojiTrie
import se.nullable.flickboard.ui.settings.AppSettingsProvider
import se.nullable.flickboard.ui.theme.FlickBoardTheme
import se.nullable.flickboard.util.Boxed
import se.nullable.flickboard.util.ZalgoDatabase

val LocalEmojiTrie = staticCompositionLocalOf<Lazy<EmojiTrie>> {
    throw Exception("LocalEmojiTrie used outside of FlickBoardParent")
}
val LocalEmojiDb = staticCompositionLocalOf<Lazy<EmojiDb>> {
    throw Exception("LocalEmojiDb used outside of FlickBoardParent")
}
val LocalDependencyInfo = staticCompositionLocalOf<Lazy<List<DependencyInfo>>> {
    throw Exception("LocalDependencyInfo used outside of FlickBoardParent")
}
val LocalZalgoDatabase = staticCompositionLocalOf<Lazy<ZalgoDatabase>> {
    throw Exception("LocalZalgoDatabase used outside of FlickBoardParent")
}
val LocalAppDatabase = staticCompositionLocalOf<Lazy<AppDatabase>> {
    throw Exception("LocalAppDatabase used outside of FlickBoardParent")
}

/**
 * Do not use in a composable context! Use [LocalAppDatabase] to share the instance.
 */
fun appDatabaseForContext(context: Context): AppDatabase =
    Room.databaseBuilder(context, AppDatabase::class.java, "flickboard-appdb").build()

// HACK: Boxing allows the variable to exist on platforms that do not provide the type
// (like the IDE previews)
val LocalDollar1GestureLibrary = staticCompositionLocalOf<Lazy<Boxed<GestureLibrary>>> {
    throw Exception("LocalDollar1GestureLibrary used outside of FlickBoardParent")
}

@OptIn(ExperimentalSerializationApi::class)
@Composable
fun FlickBoardParent(
    prefs: SharedPreferences? = null,
    appDatabase: Lazy<AppDatabase>? = null,
    content: @Composable () -> Unit
) {
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
    val dependencyInfo = remember(context) {
        lazy {
            context.resources.openRawResource(R.raw.dependencies).use { dependenciesStr ->
                Json.decodeFromStream<List<DependencyInfo>>(dependenciesStr)
            }
        }
    }
    val zalgoDatabase = remember(context) {
        lazy {
            context.resources.openRawResource(R.raw.zalgo_substitutions).use { stream ->
                stream.reader(Charsets.UTF_8).use {
                    ZalgoDatabase.parse(it.readText())
                }
            }
        }
    }
    val providedAppDatabase = appDatabase ?: remember(context) {
        lazy {
            appDatabaseForContext(context)
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
        LocalDependencyInfo provides dependencyInfo,
        LocalZalgoDatabase provides zalgoDatabase,
        LocalAppDatabase provides providedAppDatabase,
        LocalDollar1GestureLibrary provides dollar1GestureLibrary,
    ) {
        AppSettingsProvider(prefs) {
            FlickBoardTheme(content = content)
        }
    }
}