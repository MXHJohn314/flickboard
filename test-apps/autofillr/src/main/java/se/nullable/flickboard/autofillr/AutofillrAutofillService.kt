package se.nullable.flickboard.autofillr

import android.app.assist.AssistStructure
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.Presentations
import android.service.autofill.SaveCallback
import android.service.autofill.SaveInfo
import android.service.autofill.SaveRequest
import android.view.View
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews

class AutofillrAutofillService : AutofillService() {
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
                        listOf("foo", "bar", "spam", "eggs").forEach { username ->
                            val menuPresentation = RemoteViews(
                                packageName,
                                android.R.layout.simple_list_item_1,
                            )
                            menuPresentation.setTextViewText(android.R.id.text1, username)
                            fill.addDataset(
                                Dataset.Builder(
                                    Presentations.Builder()
                                        .setMenuPresentation(menuPresentation)
                                        .build(),
                                ).also { ds ->
                                    ds.setValue(
                                        usernameField,
                                        AutofillValue.forText(username),
                                    )
                                    if (passwordField != null) {
                                        ds.setValue(
                                            passwordField,
                                            AutofillValue.forText("password for $username"),
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