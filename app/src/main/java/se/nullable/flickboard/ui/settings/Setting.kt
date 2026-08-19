package se.nullable.flickboard.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import androidx.compose.runtime.Composable
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
import se.nullable.flickboard.ui.util.ResettableState
import se.nullable.flickboard.util.Boxed
import se.nullable.flickboard.util.orNull
import se.nullable.flickboard.util.tryEnumValueOf
import java.util.WeakHashMap
import kotlin.math.roundToInt

class SettingsContext(
    val prefs: SharedPreferences,
    val coroutineScope: CoroutineScope,
    val appDatabase: Lazy<AppDatabase>,
) {
    private val settingCache = WeakHashMap<SettingProjection<*>, Setting<*>>()
    operator fun <T> get(meta: SettingProjection<T>): Setting<T> = run {
        val setting = settingCache.computeIfAbsent(meta) { Setting(meta, this) }
        @Suppress("UNCHECKED_CAST")
        setting as Setting<T>
    }

    class Editor(val reader: SettingsContext, val prefsEditor: SharedPreferences.Editor)

    /**
     * Applies [f] to this [SettingsContext]'s state.
     */
    inline fun edit(f: (Editor) -> Unit) {
        prefs.edit { f(Editor(this@SettingsContext, this)) }
    }

    /**
     * Returns a new [SettingsContext] where [f] has been applied.
     *
     * [edit]s to the new [SettingsContext] are also not propagated to the parent [SettingsContext]
     * (or otherwise persisted).
     */
    inline fun mock(f: (Editor) -> Unit): SettingsContext =
        SettingsContext(MockedSharedPreferences(prefs), coroutineScope, appDatabase)
            .also { it.edit(f) }

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

class Setting<T>(
    val meta: SettingProjection<T>,
    private val ctx: SettingsContext
) {
    var currentValue: T
        get() = meta.readFrom(ctx)
        set(value) = ctx.edit { meta.writeTo(it, value) }

    inline fun modify(f: (T) -> T) {
        currentValue = f(currentValue)
    }

    fun resetToDefault() {
        ctx.edit(meta::resetIn)
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
            if (meta.observesKey(key)) {
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

    val state: ResettableState<T>
        @Composable
        get() = run {
            val collector = watch.collectAsState(initial = cachedValue)
            object : ResettableState<T> {
                override var value: T
                    get() = collector.value
                    set(value) {
                        currentValue = value
                    }

                override fun resetToDefault() {
                    this@Setting.resetToDefault()
                }

                override fun component1(): T = value
                override fun component2(): (T) -> Unit = { value = it }
            }
        }
}

interface SettingProjection<T> {
    fun observesKey(key: String?): Boolean
    fun readFrom(ctx: SettingsContext): T
    fun writeTo(ctx: SettingsContext.Editor, value: T)
    fun isSet(ctx: SettingsContext): Boolean
    fun resetIn(ctx: SettingsContext.Editor)

    fun <U : Any> tryMap(
        get: (T) -> U?,
        set: (U) -> T?,
    ): SettingProjection<U?> = let { base ->
        object : SettingProjection<U?> {
            override fun observesKey(key: String?): Boolean = base.observesKey(key)

            override fun readFrom(ctx: SettingsContext): U? =
                get(base.readFrom(ctx))

            override fun writeTo(ctx: SettingsContext.Editor, value: U?) {
                base.writeTo(ctx, set(value ?: return) ?: return)
            }

            override fun isSet(ctx: SettingsContext): Boolean = base.isSet(ctx)
            override fun resetIn(ctx: SettingsContext.Editor) {
                base.resetIn(ctx)
            }
        }
    }
}

sealed class SettingMeta<T> : SettingProjection<T> {
    abstract val key: String
    abstract val label: String
    abstract val description: String?

    override fun observesKey(key: String?): Boolean = key == this.key
    override fun isSet(ctx: SettingsContext): Boolean = ctx.prefs.contains(key)
    override fun resetIn(ctx: SettingsContext.Editor) {
        ctx.prefsEditor.remove(key)
    }

    abstract fun readFromJson(json: JsonReader): T?
    abstract fun writeToJson(json: JsonWriter, value: T)

    fun exportToJson(ctx: SettingsContext, json: JsonWriter) {
        writeToJson(json, readFrom(ctx))
    }

    fun importFromJson(ctx: SettingsContext.Editor, json: JsonReader) {
        readFromJson(json)?.let { writeTo(ctx, it) }
    }

    class Bool(
        override val key: String,
        override val label: String,
        val defaultValue: Boolean,
        override val description: String? = null,
        val onChange: (SettingsContext.Editor, Boolean) -> Unit = { _, _ -> },
    ) : SettingMeta<Boolean>() {
        override fun readFrom(ctx: SettingsContext): Boolean =
            ctx.prefs.getBoolean(key, defaultValue)

        override fun writeTo(ctx: SettingsContext.Editor, value: Boolean) {
            onChange(ctx, value)
            ctx.prefsEditor.putBoolean(key, value)
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
        override val description: String? = null,
    ) : SettingMeta<Int>() {
        override fun readFrom(ctx: SettingsContext): Int =
            ctx.prefs.getInt(key, defaultValue)

        override fun writeTo(ctx: SettingsContext.Editor, value: Int) {
            ctx.prefsEditor.putInt(key, value)
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
        override val description: String? = null,
        val placeholder: String? = null,
    ) : SettingMeta<String>() {
        override fun readFrom(ctx: SettingsContext): String =
            ctx.prefs.getString(key, defaultValue) ?: defaultValue

        override fun writeTo(ctx: SettingsContext.Editor, value: String) {
            ctx.prefsEditor.putString(key, value)
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
        override val description: String? = null,
        val render: (Float) -> String = { it.roundToInt().toString() },
    ) : SettingMeta<Float>() {
        override fun readFrom(ctx: SettingsContext): Float =
            ctx.prefs.getFloat(key, defaultValue)

        override fun writeTo(ctx: SettingsContext.Editor, value: Float) {
            ctx.prefsEditor.putFloat(key, value.coerceIn(range))
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
        override val description: String? = null,
        val writePreviewSettings: (SettingsContext.Editor) -> Unit = {},
    ) : SettingMeta<List<T>>() {
        override fun readFrom(ctx: SettingsContext): List<T> =
            ctx.prefs.getString(key, null)
                ?.split(',')
                ?.mapNotNull { it.takeIf { it.isNotEmpty() }?.let(tryEnumValueOfT) }
                ?: defaultValue

        override fun writeTo(ctx: SettingsContext.Editor, value: List<T>) {
            ctx.prefsEditor.putString(key, value.joinToString(",") { it.toString() })
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
        override val description: String? = null,
        val writePreviewSettings: (SettingsContext.Editor) -> Unit = {},
        val previewOverride: (@Composable (T) -> Unit)? = null,
        val previewForceLandscape: Boolean = false,
    ) : SettingMeta<T>() {
        override fun readFrom(ctx: SettingsContext): T =
            ctx.prefs.getString(key, null)?.let(tryEnumValueOfT) ?: defaultValue

        override fun writeTo(ctx: SettingsContext.Editor, value: T) {
            ctx.prefsEditor.putString(key, value.toString())
        }

        override fun readFromJson(json: JsonReader): T? = tryEnumValueOfT(json.nextString())
        override fun writeToJson(json: JsonWriter, value: T) {
            json.value(value.toString())
        }
    }

    class Image(
        override val key: String,
        override val label: String,
        val cacheFileName: String,
        val scalingTarget: ImageScalingTarget,
        override val description: String? = null,
    ) : SettingMeta<Uri?>() {
        override fun readFrom(ctx: SettingsContext): Uri? =
            ctx.prefs.getString(key, null)?.let(Uri::parse)

        override fun writeTo(ctx: SettingsContext.Editor, value: Uri?) {
            ctx.prefsEditor.putString(key, value?.toString())
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
        override val description: String? = null,
    ) : SettingMeta<Color?>() {
        override fun readFrom(ctx: SettingsContext): Color? =
            ctx.prefs.getInt(key, 0).takeUnless { it == 0 }?.let { Color(it) }

        override fun writeTo(ctx: SettingsContext.Editor, value: Color?) {
            ctx.prefsEditor.putInt(key, value?.toArgb() ?: 0)
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

enum class ImageScalingTarget {
    /** Do not pre-scale the image. */
    Original,

    /** Pre-scale the image to fit the phone's display. */
    DeviceDisplay,
}