package se.nullable.flickboard.ui.settings

import android.content.SharedPreferences

/**
 * Limited in-memory variant of SharedPreferences, used to generated previews.
 *
 * Does not support watchers, and changes are not transactional.
 */
class MockedSharedPreferences(private val inner: SharedPreferences) : SharedPreferences by inner {
    private val strings = mutableMapOf<String, String>()

    override fun getString(key: String, defValue: String?): String? =
        strings[key] ?: inner.getString(key, defValue)

    override fun getFloat(key: String, defValue: Float): Float =
        strings[key]?.toFloat() ?: inner.getFloat(key, defValue)

    override fun edit(): SharedPreferences.Editor {
        return object : SharedPreferences.Editor {
            override fun putString(key: String, value: String?): SharedPreferences.Editor {
                if (value != null) {
                    strings[key] = value
                } else {
                    strings.remove(key)
                }
                return this
            }

            override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
                strings[key] = value.toString()
                return this
            }

            override fun putStringSet(
                key: String?,
                values: MutableSet<String>?
            ): SharedPreferences.Editor {
                TODO("Not yet implemented")
            }

            override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
                TODO("Not yet implemented")
            }

            override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
                TODO("Not yet implemented")
            }

            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
                TODO("Not yet implemented")
            }

            override fun remove(key: String): SharedPreferences.Editor {
                strings.remove(key)
                return this
            }

            override fun clear(): SharedPreferences.Editor {
                TODO("Not yet implemented")
            }

            override fun commit(): Boolean {
                return true
            }

            override fun apply() {
            }
        }
    }

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
    }
}
