package se.nullable.flickboard.ui.util

import androidx.compose.runtime.MutableState

inline fun <T> MutableState<T>.modify(f: T.() -> T) {
    value = value.f()
}