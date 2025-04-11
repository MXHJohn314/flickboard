package se.nullable.flickboard.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.shareIn
import se.nullable.flickboard.model.AppDatabase
import se.nullable.flickboard.util.Boxed
import se.nullable.flickboard.util.orNull
import se.nullable.flickboard.util.tryEnumValueOf
import kotlin.math.roundToInt

class SettingsContext(
    val prefs: SharedPreferences,
    val coroutineScope: CoroutineScope,
    val appDatabase: Lazy<AppDatabase>,
) {
    companion object {
        /**
         * Do not use in a composable context! Use [LocalAppSettings] instead!
         */
        fun appPrefsForContext(context: Context): SharedPreferences =
            context.getSharedPreferences(
                "flickboard",
                Context.MODE_PRIVATE,
            )
    }
}

abstract class SettingProjection<T> {
    abstract var currentValue: T

    inline fun modify(f: (T) -> T) {
        currentValue = f(currentValue)
    }

    inline fun tryModify(f: (T) -> Boxed<T>?): Boolean {
        currentValue = (f(currentValue) ?: return false).value
        return true
    }

    fun <U : Any> tryMap(
        get: (T) -> U?,
        set: (T, U) -> T?,
    ): SettingProjection<U?> = let { base ->
        object : SettingProjection<U?>() {
            override var currentValue: U?
                get() = get(base.currentValue)
                set(value) {
                    base.tryModify { oldBase ->
                        set(
                            oldBase,
                            (value ?: return@tryModify null),
                        )?.let(::Boxed)
                    }
                }
        }
    }
}

sealed class Setting<T>(private val ctx: SettingsContext) : SettingProjection<T>() {
    abstract val key: String
    abstract val label: String
    abstract val description: String?

    override var currentValue: T
        get() = readFrom(ctx.prefs)
        set(value) = ctx.prefs.edit { writeTo(this, value) }

    fun resetToDefault() {
        ctx.prefs.edit { resetIn(this) }
    }

    abstract fun readFrom(prefs: SharedPreferences): T
    abstract fun writeTo(prefs: SharedPreferences.Editor, value: T)
    fun resetIn(prefs: SharedPreferences.Editor) {
        prefs.remove(key)
    }

    abstract fun readFromJson(json: JsonReader): T?
    abstract fun writeToJson(json: JsonWriter, value: T)

    fun exportToJson(prefs: SharedPreferences, json: JsonWriter) {
        writeToJson(json, readFrom(prefs))
    }

    fun importFromJson(prefs: SharedPreferences.Editor, json: JsonReader) {
        readFromJson(json)?.let { writeTo(prefs, it) }
    }

    private var lastCachedValue: Boxed<T>? = null
    private val cachedValue: T
        get() {
            var v = lastCachedValue
            if (v == null) {
                v = Boxed(currentValue)
                lastCachedValue = v
            }
            return v.value
        }

    val watch: Flow<T> = callbackFlow {
        // Type MUST Be initialized by name to ensure that the same object is passed to
        // register and unregister. Otherwise no strong reference is held to the listener,
        // meaning that the registration can be "lost" on any garbage collection.
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == this@Setting.key) {
                val v = currentValue
                lastCachedValue = Boxed(v)
                trySendBlocking(v)
            }
        }
        send(cachedValue)
        ctx.prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { ctx.prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
        .conflate()
        .shareIn(
            ctx.coroutineScope,
            // If the Flow is stopped once cachedValue is loaded then updates may be missed
            // Instead, make sure to constrain coroutineScope to the lifetime of the setting to
            // avoid leaks
            SharingStarted.Lazily,
            replay = 1,
        )

    val state: State<T>
        @Composable
        get() = watch.collectAsState(initial = cachedValue)

    class Bool(
        override val key: String,
        override val label: String,
        val defaultValue: Boolean,
        ctx: SettingsContext,
        override val description: String? = null,
        val onChange: (Boolean) -> Unit = {},
    ) : Setting<Boolean>(ctx) {
        override fun readFrom(prefs: SharedPreferences): Boolean =
            prefs.getBoolean(key, defaultValue)

        override fun writeTo(prefs: SharedPreferences.Editor, value: Boolean) {
            onChange(value)
            prefs.putBoolean(key, value)
        }

        override fun readFromJson(json: JsonReader): Boolean = json.nextBoolean()
        override fun writeToJson(json: JsonWriter, value: Boolean) {
            json.value(value)
        }
    }

    class Integer(
        override val key: String,
        override val label: String,
        val defaultValue: Int,
        ctx: SettingsContext,
        override val description: String? = null,
    ) : Setting<Int>(ctx) {
        override fun readFrom(prefs: SharedPreferences): Int = prefs.getInt(key, defaultValue)
        override fun writeTo(prefs: SharedPreferences.Editor, value: Int) {
            prefs.putInt(key, value)
        }

        override fun readFromJson(json: JsonReader): Int = json.nextInt()
        override fun writeToJson(json: JsonWriter, value: Int) {
            json.value(value)
        }
    }

    class Text(
        override val key: String,
        override val label: String,
        val defaultValue: String,
        ctx: SettingsContext,
        override val description: String? = null,
        val placeholder: String? = null,
    ) : Setting<String>(ctx) {
        override fun readFrom(prefs: SharedPreferences): String =
            prefs.getString(key, defaultValue) ?: defaultValue

        override fun writeTo(prefs: SharedPreferences.Editor, value: String) {
            prefs.putString(key, value)
        }

        override fun readFromJson(json: JsonReader): String = json.nextString()
        override fun writeToJson(json: JsonWriter, value: String) {
            json.value(value)
        }
    }

    class FloatSlider(
        override val key: String,
        override val label: String,
        val defaultValue: Float,
        val range: ClosedFloatingPointRange<Float>,
        ctx: SettingsContext,
        override val description: String? = null,
        val render: (Float) -> String = { it.roundToInt().toString() },
    ) : Setting<Float>(ctx) {
        override fun readFrom(prefs: SharedPreferences): Float = prefs.getFloat(key, defaultValue)
        override fun writeTo(prefs: SharedPreferences.Editor, value: Float) {
            prefs.putFloat(key, value.coerceIn(range))
        }

        override fun readFromJson(json: JsonReader): Float = json.nextDouble().toFloat()
        override fun writeToJson(json: JsonWriter, value: Float) {
            json.value(value)
        }

        companion object {
            fun percentage(x: Float): String = "${(x * 100).roundToInt()}%"
            fun angle(x: Float): String = "${Math.toDegrees(x.toDouble()).roundToInt()}°"
        }
    }

    class EnumList<T : Labeled>(
        override val key: String,
        override val label: String,
        val defaultValue: List<T>,
        val options: List<T>,
        /** Should be [tryEnumValueOf] */
        val tryEnumValueOfT: (String) -> T?,
        ctx: SettingsContext,
        override val description: String? = null,
        val writePreviewSettings: (SharedPreferences, SharedPreferences.Editor) -> Unit = { _, _ -> },
    ) : Setting<List<T>>(ctx) {
        override fun readFrom(prefs: SharedPreferences): List<T> =
            prefs.getString(key, null)
                ?.split(',')
                ?.mapNotNull { it.takeIf { it.isNotEmpty() }?.let(tryEnumValueOfT) }
                ?: defaultValue

        override fun writeTo(prefs: SharedPreferences.Editor, value: List<T>) {
            prefs.putString(key, value.joinToString(",") { it.toString() })
        }

        override fun readFromJson(json: JsonReader): List<T> = mutableListOf<T>().also { out ->
            json.beginArray()
            while (json.peek() != JsonToken.END_ARRAY) {
                tryEnumValueOfT(json.nextString())?.let(out::add)
            }
            json.endArray()
        }

        override fun writeToJson(json: JsonWriter, value: List<T>) {
            json.beginArray()
            value.forEach {
                json.value(it.toString())
            }
            json.endArray()
        }
    }

    class Enum<T : Labeled>(
        override val key: String,
        override val label: String,
        val defaultValue: T,
        val options: List<T>,
        /** Should be [tryEnumValueOf]<T> */
        val tryEnumValueOfT: (String) -> T?,
        ctx: SettingsContext,
        override val description: String? = null,
        val writePreviewSettings: (SharedPreferences, SharedPreferences.Editor) -> Unit = { _, _ -> },
        val previewOverride: (@Composable (T) -> Unit)? = null,
        val previewForceLandscape: Boolean = false,
    ) : Setting<T>(ctx) {
        override fun readFrom(prefs: SharedPreferences): T =
            prefs.getString(key, null)?.let(tryEnumValueOfT) ?: defaultValue

        override fun writeTo(prefs: SharedPreferences.Editor, value: T) {
            prefs.putString(key, value.toString())
        }

        override fun readFromJson(json: JsonReader): T? = tryEnumValueOfT(json.nextString())
        override fun writeToJson(json: JsonWriter, value: T) {
            json.value(value.toString())
        }
    }

    class Image(
        override val key: String,
        override val label: String,
        ctx: SettingsContext,
        override val description: String? = null,
    ) : Setting<Uri?>(ctx) {
        override fun readFrom(prefs: SharedPreferences): Uri? =
            prefs.getString(key, null)?.let(Uri::parse)

        override fun writeTo(prefs: SharedPreferences.Editor, value: Uri?) {
            prefs.putString(key, value?.toString())
        }

        override fun readFromJson(json: JsonReader): Uri? =
            json.orNull { nextString() }?.let(Uri::parse)

        override fun writeToJson(json: JsonWriter, value: Uri?) {
            json.value(value?.toString())
        }
    }

    class Colour(
        override val key: String,
        override val label: String,
        ctx: SettingsContext,
        override val description: String? = null,
    ) : Setting<Color?>(ctx) {
        override fun readFrom(prefs: SharedPreferences): Color? =
            prefs.getInt(key, 0).takeUnless { it == 0 }?.let { Color(it) }

        override fun writeTo(prefs: SharedPreferences.Editor, value: Color?) {
            prefs.putInt(key, value?.toArgb() ?: 0)
        }

        override fun readFromJson(json: JsonReader): Color? =
            json.nextInt().takeUnless { it == 0 }?.let { Color(it) }

        override fun writeToJson(json: JsonWriter, value: Color?) {
            json.value(value?.toArgb() ?: 0)
        }
    }
}

interface Labeled {
    val label: String
}
