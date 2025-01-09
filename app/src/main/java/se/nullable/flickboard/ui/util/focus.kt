package se.nullable.flickboard.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

/**
 * Requests focus when the modifier is added.
 * The user can still move focus after that point.
 */
@Composable
fun Modifier.defaultFocus(): Modifier = run {
    val focusRequester = FocusRequester()
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    focusRequester(focusRequester)
}