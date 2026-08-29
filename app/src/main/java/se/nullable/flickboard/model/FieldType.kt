package se.nullable.flickboard.model

import android.text.InputType
import android.view.inputmethod.EditorInfo

enum class FieldType {
    /** A regular input field that doesn't require special handling */
    Regular,

    /** A password field or some other sensitive target, where we should avoid enabling shoulder snooping */
    Password;

    companion object {
        fun fromEditorInfo(editorInfo: EditorInfo): FieldType = run {
            val inputType = editorInfo.inputType
            when (inputType and InputType.TYPE_MASK_CLASS) {
                // See all variants as the constants on InputType

                InputType.TYPE_CLASS_TEXT -> when (inputType and InputType.TYPE_MASK_VARIATION) {
                    InputType.TYPE_TEXT_VARIATION_PASSWORD,
                    InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD -> Password

                    // Not considering InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD to be a password
                    // for our purposes, since it's intentionally not designed to be protected against
                    // shoulder snooping
                    else -> Regular
                }

                InputType.TYPE_CLASS_NUMBER -> when (inputType and InputType.TYPE_MASK_VARIATION) {
                    InputType.TYPE_NUMBER_VARIATION_PASSWORD -> Password
                    else -> Regular
                }

                // To quote the docs: unknown classes should be treated as
                // *text with no variations*
                else -> Regular
            }
        }
    }
}