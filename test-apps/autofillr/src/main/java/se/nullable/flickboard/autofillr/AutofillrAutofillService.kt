package se.nullable.flickboard.autofillr

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.assist.AssistStructure
import android.content.Intent
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.Field
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.InlinePresentation
import android.service.autofill.Presentations
import android.service.autofill.SaveCallback
import android.service.autofill.SaveInfo
import android.service.autofill.SaveRequest
import android.view.View
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.view.inputmethod.InlineSuggestionsRequest
import android.widget.RemoteViews
import androidx.autofill.inline.UiVersions
import androidx.autofill.inline.v1.InlineSuggestionUi

class AutofillrAutofillService : AutofillService() {
    val attributionIntent: PendingIntent by lazy {
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun inlinePresentationFor(
        request: InlineSuggestionsRequest?,
        index: Int,
        title: String
    ): InlinePresentation? = run {
        request ?: return@run null
        if (index >= request.maxSuggestionCount) {
            return@run null
        }
        val specs = request.inlinePresentationSpecs
        val spec = (specs.getOrNull(index)
            ?: specs.lastOrNull()
            ?: return@run null)
        if (!UiVersions.getVersions(spec.style)
                .contains(UiVersions.INLINE_UI_VERSION_1)
        ) {
            return@run null
        }
        val content = InlineSuggestionUi
            .newContentBuilder(attributionIntent)
            .setTitle(title)
            .build()
        InlinePresentation(
            // The RestrictedApi seems to be a mistake..
            // It's explicitly documented as the API to use,
            // and I can't find a better option...
            @SuppressLint("RestrictedApi")
            content.slice,
            spec,
            false,
        )
    }

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        val fillCtx = request.fillContexts.last()

        var usernameField: AutofillId? = null
        var passwordField: AutofillId? = null
        fillCtx.structure.autofillItems.forEach { item ->
            if (item.autofillHints?.contains(View.AUTOFILL_HINT_USERNAME) == true) {
                usernameField = item.autofillId
            }
            if (item.autofillHints?.contains(View.AUTOFILL_HINT_PASSWORD) == true) {
                passwordField = item.autofillId
            }
        }

        if (usernameField != null) {
            callback.onSuccess(
                FillResponse.Builder()
                    .setSaveInfo(
                        SaveInfo.Builder(
                            SaveInfo.SAVE_DATA_TYPE_USERNAME or SaveInfo.SAVE_DATA_TYPE_PASSWORD,
                            arrayOf(usernameField, passwordField),
                        ).build(),
                    )
                    .also { fill ->
                        listOf("foo", "bar", "spam", "eggs").forEachIndexed { i, username ->
                            val menuPresentation = RemoteViews(
                                packageName,
                                android.R.layout.simple_list_item_1,
                            )
                            menuPresentation.setTextViewText(android.R.id.text1, username)
                            fill.addDataset(
                                Dataset.Builder(
                                    Presentations.Builder()
                                        .setMenuPresentation(menuPresentation)
                                        .also {
                                            inlinePresentationFor(
                                                request.inlineSuggestionsRequest,
                                                index = i,
                                                title = username,
                                            )?.let(it::setInlinePresentation)
                                        }
                                        .build(),
                                ).also { ds ->
                                    ds.setField(
                                        usernameField,
                                        Field.Builder()
                                            .setValue(AutofillValue.forText(username))
                                            .build(),
                                    )
                                    if (passwordField != null) {
                                        ds.setField(
                                            passwordField,
                                            Field.Builder()
                                                .setValue(AutofillValue.forText("password for $username"))
                                                .build(),
                                        )
                                    }
                                }.build(),
                            )
                        }
                    }.build(),
            )
        } else {
            callback.onSuccess(null)
        }
    }

    override fun onSaveRequest(
        request: SaveRequest,
        callback: SaveCallback
    ) {
        println("Totally saved I promise")
        callback.onSuccess()
    }
}

val AssistStructure.windowNodes
    get() = (0..<windowNodeCount).map(::getWindowNodeAt)

val AssistStructure.autofillItems
    get() = windowNodes.flatMap { it.rootViewNode.autofillItemsRecursive }

val AssistStructure.ViewNode.children
    get() = (0..<childCount).map(::getChildAt)

val AssistStructure.ViewNode.autofillItemsRecursive: List<AssistStructure.ViewNode>
    get() = run {
        val self = when (importantForAutofill) {
            View.IMPORTANT_FOR_AUTOFILL_NO,
            View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS ->
                emptyList()

            else -> listOf(this)
        }

        val children = when (importantForAutofill) {
            View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS,
            View.IMPORTANT_FOR_AUTOFILL_YES_EXCLUDE_DESCENDANTS ->
                emptyList()

            else -> children.flatMap { it.autofillItemsRecursive }
        }

        self + children
    }