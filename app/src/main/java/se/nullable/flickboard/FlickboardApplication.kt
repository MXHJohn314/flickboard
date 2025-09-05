package se.nullable.flickboard

import android.app.Application
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import se.nullable.flickboard.model.AppDatabase
import se.nullable.flickboard.ui.appDatabaseForContext

class FlickboardApplication : Application() {
    val appDatabase: Lazy<AppDatabase> = lazy { appDatabaseForContext(this) }

    override fun onCreate() {
        super.onCreate()
        EmojiCompat.init(BundledEmojiCompatConfig(this))
    }
}