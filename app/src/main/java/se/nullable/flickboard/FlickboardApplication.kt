package se.nullable.flickboard

import android.app.Application
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import kotlinx.coroutines.runBlocking
import se.nullable.flickboard.model.AppDatabase
import se.nullable.flickboard.ui.appDatabaseForContext
import se.nullable.flickboard.ui.settings.AppSettings
import se.nullable.flickboard.ui.settings.SettingsContext

class FlickboardApplication : Application() {
    val appDatabase: Lazy<AppDatabase> = lazy { appDatabaseForContext(this) }

    override fun onCreate() {
        super.onCreate()
        runBlocking {
            val appSettings = AppSettings(
                SettingsContext(
                    SettingsContext.appPrefsForContext(this@FlickboardApplication),
                    this@runBlocking,
                    appDatabase,
                ),
            )
            appSettings.ctx.edit {
                appSettings.migrateSettings(it)
            }
        }
        EmojiCompat.init(BundledEmojiCompatConfig(this))
    }
}