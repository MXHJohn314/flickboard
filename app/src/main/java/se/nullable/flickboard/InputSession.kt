package se.nullable.flickboard

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService
import androidx.core.os.postDelayed
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import se.nullable.flickboard.model.Action
import se.nullable.flickboard.model.CaseChangeDirection
import se.nullable.flickboard.model.EnterActionOverride
import se.nullable.flickboard.model.FastActionType
import se.nullable.flickboard.model.ModifierState
import se.nullable.flickboard.model.SearchDirection
import se.nullable.flickboard.model.TextBoundary
import se.nullable.flickboard.ui.LocalDisplayLimits
import se.nullable.flickboard.ui.LocalZalgoDatabase
import se.nullable.flickboard.ui.OnAction
import se.nullable.flickboard.ui.settings.EnabledLayers
import se.nullable.flickboard.ui.settings.LocalAppSettings
import se.nullable.flickboard.ui.voice.getVoiceInputId
import se.nullable.flickboard.util.AndroidKeycodeMapper
import se.nullable.flickboard.util.LastTypedData
import se.nullable.flickboard.util.asCombiningMarkOrNull
import se.nullable.flickboard.util.times
import java.text.BreakIterator
import kotlin.math.max

/**
 * Represents FlickBoard's state for one [InputConnection].
 */
class InputSession(
    private val inputConnection: InputConnection,
    val editorInfo: EditorInfo?,
) {
    sealed class ActiveView {
        @Serializable
        data object Keyboard : ActiveView()

        @Serializable
        data class EmojiKeyboard(val search: Boolean = false) : ActiveView()

        @Serializable
        data object ClipboardHistory : ActiveView()
    }

    private val keycodeMapper = AndroidKeycodeMapper()

    // Not available in all apps, use selection (or, preferably, the helpers)
    // instead of accessing directly
    private var cursor: CursorAnchorInfo? = null

    private var gestureBeginPosition: Int = 0
    private fun selection(): IntRange? = run {
        val currentCursor = cursor
        when {
            // Some apps (such as Firefox) don't always support requestCursorUpdates,
            // in those cases, fall back to making a synchronous getExtractedText request instead.
            // getExtractedText can still fail for non-standard text editors that don't use a typical
            // "selection" at all (like terminals).
            currentCursor == null ||
                    // Some apps (Google Docs) *do* support requestCursorUpdates, but always return -1
                    // (no selection). Use the fallback for them, too.
                    currentCursor.selectionStart == -1 || currentCursor.selectionEnd == -1 -> {
                inputConnection.getExtractedText(
                    ExtractedTextRequest().also {
                        it.hintMaxChars = 1
                        it.hintMaxLines = 1
                    },
                    0,
                )?.let {
                    val offset = it.startOffset
                    (offset + it.selectionStart)..(offset + it.selectionEnd)
                }
            }

            else -> currentCursor.selectionStart..currentCursor.selectionEnd
        }
    }

    private var activeModifiers = ModifierState()

    enum class SelectionSize {
        Empty,
        NonEmpty,
        BufferUnavailable,
    }

    private fun selectionSize(): SelectionSize =
        selection().let { selection ->
            when {
                selection == null -> SelectionSize.BufferUnavailable
                selection.first == selection.last -> SelectionSize.Empty
                else -> SelectionSize.NonEmpty
            }
        }

    // Returns null if the input buffer is unavailable
    private fun currentCursorPosition(direction: SearchDirection): Int? =
        selection().let { selection ->
            when (direction) {
                SearchDirection.Backwards -> selection?.first
                SearchDirection.Forwards -> selection?.last
            }
        }

    private fun sendKeyPressEvents(
        keyCode: Int,
        extraModifiers: Int = 0,
        ignoreActiveModifiers: Boolean = false
    ) {
        listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP).forEach { keyAction ->
            inputConnection.sendKeyEvent(
                KeyEvent(
                    0,
                    0,
                    keyAction,
                    keyCode,
                    0,
                    (activeModifiers.androidMetaState * !ignoreActiveModifiers)
                            or extraModifiers,
                    KeyCharacterMap.VIRTUAL_KEYBOARD,
                    0,
                    KeyEvent.FLAG_SOFT_KEYBOARD,
                ),
            )
        }
    }

    private var lastTyped: LastTypedData? = null
        // The lastTyped is no longer relevant if the position doesn't match,
        // also cleared in onUpdateCursorAnchorInfo but we still want to double-check
        // as a fallback.
        get() = field?.takeIf { currentCursorPosition(SearchDirection.Backwards) == it.position }

    private fun findBoundary(
        boundary: TextBoundary,
        direction: SearchDirection,
        skipBoundaries: Int = 0,
        coalesce: Boolean = false,
        includeSelection: Boolean = false,
        endOfBufferOffset: Int = 0,
    ): Int {
        val searchBufferSize = when (boundary) {
            TextBoundary.Character -> 40
            else -> 1000
        }
        val selectedText = when {
            includeSelection -> inputConnection.getSelectedText(0) ?: ""
            else -> ""
        }
        val searchBuffer = when (direction) {
            SearchDirection.Backwards -> (inputConnection.getTextBeforeCursor(
                searchBufferSize,
                0,
            ) ?: "").toString() + selectedText

            SearchDirection.Forwards -> selectedText.toString() + (inputConnection.getTextAfterCursor(
                searchBufferSize,
                0,
            ) ?: "")
        }
        val breakIterator = boundary.breakIterator()
        breakIterator.setText(searchBuffer)
        if (direction == SearchDirection.Backwards) {
            breakIterator.last()
        }
        val steps = skipBoundaries + 1
        var pos = breakIterator.next(direction.factor * steps)
        if (pos == BreakIterator.DONE) {
            return searchBuffer.length + endOfBufferOffset
        }
        if (coalesce && boundary != TextBoundary.Character) {
            val next = breakIterator.next(direction.factor)
            if (next != BreakIterator.DONE
                && searchBuffer.getOrNull(max(pos, next))?.isWhitespace() != false
            ) {
                pos = next
            }
        }
        val posInDirection = when (direction) {
            SearchDirection.Backwards -> searchBuffer.length - pos
            SearchDirection.Forwards -> pos
        }
        return posInDirection
    }

    @Composable
    fun actionHandler(
        warningSnackbarHostState: SnackbarHostState,
        switchInputMethod: (String) -> Unit,
        navController: NavController,
    ): OnAction {
        val context = LocalContext.current
        val appSettings = LocalAppSettings.current
        val settingsCtx = appSettings.ctx
        val periodOnDoubleSpace = settingsCtx[appSettings.periodOnDoubleSpace].state
        val disabledDeadkeys = settingsCtx[appSettings.disabledDeadkeys].state
        val displayLimits = LocalDisplayLimits.current
        val zalgoDb = LocalZalgoDatabase.current

        val warningMessageScope = rememberCoroutineScope()

        return OnAction { action, _, _ ->
            warningMessageScope.launch {
                warningSnackbarHostState.currentSnackbarData?.dismiss()
            }
            fun typeText(
                text: String,
                forceRawKeyEvent: Boolean = false,
                forceShift: Boolean = false,
            ) {
                var char = text
                val rawKeyCode = when {
                    forceRawKeyEvent || activeModifiers.useRawKeyEvent -> char.singleOrNull()
                        ?.let(keycodeMapper::keycodeForChar)
                        ?: KeyEvent.KEYCODE_UNKNOWN

                    else -> KeyEvent.KEYCODE_UNKNOWN
                }
                when {
                    rawKeyCode != KeyEvent.KEYCODE_UNKNOWN ->
                        sendKeyPressEvents(
                            rawKeyCode,
                            extraModifiers = when {
                                forceShift -> KeyEvent.normalizeMetaState(
                                    KeyEvent.META_SHIFT_LEFT_ON,
                                )

                                else -> 0
                            },
                        )

                    else -> {
                        val combiner = when {
                            activeModifiers.zalgo ->
                                lastTyped?.tryCombineWith(
                                    char,
                                    periodOnDoubleSpace = false,
                                    zalgoDatabase = zalgoDb.value,
                                ) ?: char.asCombiningMarkOrNull(zalgoMode = true)
                                    ?.let {
                                        LastTypedData.Combiner(
                                            original = char,
                                            combinedReplacement = it,
                                            baseCharLength = 0,
                                        )
                                    }

                            disabledDeadkeys.value.contains(char) -> null

                            else -> lastTyped?.tryCombineWith(
                                char,
                                periodOnDoubleSpace = periodOnDoubleSpace.value,
                            )
                        }
                        if (combiner != null) {
                            char = combiner.combinedReplacement
                        }
                        val positionOfChar =
                            currentCursorPosition(SearchDirection.Backwards)
                                ?.plus(char.length)
                                ?.minus(combiner?.baseCharLength ?: 0)
                        lastTyped = when {
                            positionOfChar != null -> LastTypedData(
                                char = char,
                                position = positionOfChar,
                                combiner = combiner,
                            )

                            else -> null
                        }
                        inputConnection.beginBatchEdit()
                        if (combiner != null) {
                            inputConnection.deleteSurroundingText(
                                combiner.baseCharLength,
                                0,
                            )
                        }
                        inputConnection.commitText(
                            char,
                            1,
                        )
                        inputConnection.endBatchEdit()
                    }
                }
            }

            fun moveSelection(
                direction: SearchDirection,
                boundary: TextBoundary,
            ): Boolean {
                val range = selection() ?: return false
                val movingSide = when {
                    range.first == range.last -> {
                        gestureBeginPosition = range.first
                        direction
                    }

                    range.last == gestureBeginPosition -> SearchDirection.Backwards
                    else -> SearchDirection.Forwards
                }
                val distance = findBoundary(
                    boundary,
                    direction,
                    coalesce = true,
                    includeSelection = movingSide != direction,
                ) * direction.factor
                inputConnection.setSelection(
                    (range.first + distance * (movingSide == SearchDirection.Backwards))
                        .coerceAtMost(range.last),
                    (range.last + distance * (movingSide == SearchDirection.Forwards))
                        .coerceAtLeast(range.first),
                )
                return true
            }

            var actionSuccessful = true
            when (action) {
                is Action.Text -> typeText(
                    action.character,
                    forceRawKeyEvent = action.forceRawKeyEvent,
                )

                is Action.BeginFastAction -> {
                    if (!activeModifiers.select) {
                        gestureBeginPosition =
                            currentCursorPosition(direction = SearchDirection.Backwards)
                                ?: 0
                    }
                }

                is Action.FastActionDone -> when (action.type) {
                    FastActionType.Delete -> {
                        inputConnection.commitText("", 0)
                    }
                }

                is Action.FastDelete -> {
                    // Let user select text, which is then deleted when FastActionDone is fired
                    actionSuccessful =
                        moveSelection(action.direction, action.boundary)
                }

                is Action.Delete -> {
                    when {
                        activeModifiers.useRawKeyEvent -> sendKeyPressEvents(
                            when (action.direction) {
                                SearchDirection.Backwards -> KeyEvent.KEYCODE_DEL
                                SearchDirection.Forwards -> KeyEvent.KEYCODE_FORWARD_DEL
                            },
                        )

                        else -> when (selectionSize()) {
                            SelectionSize.NonEmpty -> {
                                // if selection is non-empty, delete it regardless of the mode requested by the user
                                inputConnection.commitText("", 0)
                            }

                            SelectionSize.Empty -> {
                                val length = findBoundary(
                                    action.boundary,
                                    action.direction,
                                    coalesce = true,
                                )
                                val lastTypedComposed = lastTyped?.combiner
                                when {
                                    // Unmerge composed text if that was the previous action taken.
                                    // For example: normally a¨ would get merged into ä,
                                    // so a¨<del> allows the user to type a literal a¨.
                                    lastTypedComposed != null &&
                                            action.direction == SearchDirection.Backwards -> {
                                        inputConnection.beginBatchEdit()
                                        inputConnection.deleteSurroundingText(
                                            lastTypedComposed.combinedReplacement.length,
                                            0,
                                        )
                                        inputConnection.commitText(
                                            lastTypedComposed.original,
                                            1,
                                        )
                                        inputConnection.endBatchEdit()
                                        // Clear last-typed data even if we're in a context without
                                        // a working requestCursorUpdates
                                        lastTyped = null
                                    }

                                    // If we can't find any text to delete, send a literal key event
                                    // to the app, in case it has anything useful to do with it.
                                    // (such as deleting a row of a list.)
                                    length == 0 -> sendKeyPressEvents(
                                        keyCode = when (action.direction) {
                                            SearchDirection.Backwards -> KeyEvent.KEYCODE_DEL
                                            SearchDirection.Forwards -> KeyEvent.KEYCODE_FORWARD_DEL
                                        },
                                    )

                                    // Otherwise (normally!) delete the text we found.
                                    else -> inputConnection.deleteSurroundingText(
                                        if (action.direction == SearchDirection.Backwards) length else 0,
                                        if (action.direction == SearchDirection.Forwards) length else 0,
                                    )
                                }
                            }

                            SelectionSize.BufferUnavailable -> {
                                sendKeyPressEvents(
                                    keyCode = when (action.direction) {
                                        SearchDirection.Backwards -> KeyEvent.KEYCODE_DEL
                                        SearchDirection.Forwards -> KeyEvent.KEYCODE_FORWARD_DEL
                                    },
                                )
                            }
                        }
                    }
                }

                is Action.Tab -> {
                    sendKeyPressEvents(
                        KeyEvent.KEYCODE_TAB,
                        extraModifiers = when {
                            action.shift -> KeyEvent.normalizeMetaState(KeyEvent.META_SHIFT_LEFT_ON)
                            else -> 0
                        },
                    )
                }

                is Action.Enter -> {
                    val imeOptions = editorInfo?.imeOptions ?: 0
                    val overrideMode = editorInfo?.let(EnterActionOverride.Mode::fromEditorInfo)
                    val isNewline = when {
                        action.secondary -> false
                        else -> activeModifiers.useRawKeyEvent ||
                                activeModifiers.shift.isShift ||
                                action.shift ||
                                overrideMode != EnterActionOverride.Mode.Replace
                    }
                    if (isNewline) {
                        typeText(
                            "\n",
                            forceRawKeyEvent = activeModifiers.shift.isShift || action.shift,
                            forceShift = action.shift,
                        )
                    } else {
                        inputConnection.performEditorAction(
                            when {
                                editorInfo?.actionLabel != null -> editorInfo.actionId
                                else -> imeOptions and EditorInfo.IME_MASK_ACTION
                            },
                        )
                    }
                }

                Action.Escape -> sendKeyPressEvents(KeyEvent.KEYCODE_ESCAPE)

                is Action.Jump -> {
                    when {
                        activeModifiers.select -> actionSuccessful =
                            moveSelection(action.direction, action.boundary)

                        else -> {
                            when (val currentPos = when {
                                activeModifiers.useRawKeyEvent -> null
                                else -> currentCursorPosition(action.direction)
                            }) {
                                null -> sendKeyPressEvents(
                                    keyCode = when (action.direction) {
                                        SearchDirection.Backwards -> KeyEvent.KEYCODE_DPAD_LEFT
                                        SearchDirection.Forwards -> KeyEvent.KEYCODE_DPAD_RIGHT
                                    },
                                    extraModifiers = when (action.boundary) {
                                        TextBoundary.Character -> 0
                                        TextBoundary.Word -> KeyEvent.META_CTRL_LEFT_ON
                                        // No modifier we can apply here, but we handle this case in
                                        // JumpLineKeepPos anyway
                                        TextBoundary.Line -> 0
                                    },
                                )

                                else -> {
                                    val newPos = currentPos + findBoundary(
                                        action.boundary,
                                        action.direction,
                                        coalesce = true,
                                    ) * action.direction.factor
                                    inputConnection.setSelection(
                                        newPos,
                                        newPos,
                                    )
                                }
                            }
                        }
                    }
                }

                is Action.JumpLineKeepPos -> {
                    // Yes, this is a horror beyond comprehension.
                    // Yes, this should really be the editor's responsibility...
                    // How is this different from Action.Jump? Action.Jump jumps to *the boundary*,
                    // for TextBoundary.Line this is equivalent to Home/End.
                    val currentPos = when {
                        action.rawEvent || activeModifiers.useRawKeyEvent -> null
                        else -> currentCursorPosition(action.direction)
                    }
                    when (currentPos) {
                        null -> sendKeyPressEvents(
                            keyCode = when (action.direction) {
                                SearchDirection.Backwards -> KeyEvent.KEYCODE_DPAD_UP
                                SearchDirection.Forwards -> KeyEvent.KEYCODE_DPAD_DOWN
                            },
                        )

                        else -> {
                            // Find our position on the current line
                            val posOnLine = findBoundary(
                                TextBoundary.Line,
                                SearchDirection.Backwards,
                            )
                            val lineSearchSkip = when (action.direction) {
                                // When going backwards, we need to find the linebreak before the current one
                                SearchDirection.Backwards -> 1
                                SearchDirection.Forwards -> 0
                            }
                            // Find the offset to the target line
                            val targetLineOffset = (findBoundary(
                                TextBoundary.Line,
                                action.direction,
                                skipBoundaries = lineSearchSkip,
                            )) * action.direction.factor
                            // To reconstruct the position on the line, we also need to clamp
                            // to the length of the new line, if it is shorter than the current one
                            val targetLineLength = when (action.direction) {
                                // When jumping backwards, we already know the length of the
                                // line since we're jumping into it
                                SearchDirection.Backwards -> -targetLineOffset - posOnLine - 1
                                // When jumping forwards.. search for the next newline after the current one
                                SearchDirection.Forwards -> findBoundary(
                                    TextBoundary.Line,
                                    action.direction,
                                    skipBoundaries = 1,
                                    endOfBufferOffset = 1,
                                ) - targetLineOffset - 1
                            }
                            val newPos = (currentPos + targetLineOffset +
                                    posOnLine.coerceAtMost(targetLineLength))
                                .coerceAtLeast(0)
                            inputConnection.setSelection(
                                newPos,
                                newPos,
                            )
                        }
                    }
                }

                is Action.ToggleWordCase -> {
                    selection()?.let { currentSelection ->
                        inputConnection.beginBatchEdit()

                        var prefixLength = 0
                        var suffixLength = 0
                        val word = when {
                            currentSelection.first == currentSelection.last -> {
                                val posInWord = findBoundary(
                                    TextBoundary.Word,
                                    SearchDirection.Backwards,
                                )
                                val textAfterInWord = findBoundary(
                                    TextBoundary.Word,
                                    SearchDirection.Forwards,
                                )

                                val prefix =
                                    inputConnection.getTextBeforeCursor(
                                        posInWord,
                                        0,
                                    )?.takeUnless { it.isBlank() }
                                        ?.toString()
                                        ?: ""
                                val suffix =
                                    inputConnection.getTextAfterCursor(
                                        textAfterInWord,
                                        0,
                                    )?.takeUnless { it.isBlank() }
                                        ?.toString()
                                        ?: ""
                                prefixLength = prefix.length
                                suffixLength = suffix.length
                                prefix + suffix
                            }

                            else -> inputConnection.getSelectedText(0)
                                .toString()
                        }

                        fun replaceWord(newWord: String) {
                            inputConnection.deleteSurroundingText(
                                prefixLength,
                                suffixLength,
                            )
                            inputConnection.commitText(newWord, 1)
                        }

                        val lowercase = word.lowercase(action.locale)
                        val titlecase =
                            lowercase.replaceFirstChar { it.titlecase(action.locale) }
                        val uppercase = word.uppercase(action.locale)
                        when (action.direction) {
                            CaseChangeDirection.Up -> when (word) {
                                uppercase -> {}
                                lowercase -> replaceWord(titlecase)
                                else -> replaceWord(uppercase)
                            }

                            CaseChangeDirection.Down -> when (word) {
                                lowercase -> {}
                                titlecase -> replaceWord(lowercase)
                                else -> replaceWord(titlecase)
                            }
                        }

                        inputConnection.setSelection(
                            currentSelection.first,
                            currentSelection.last,
                        )
                        inputConnection.endBatchEdit()

                    }
                }

                is Action.ToggleShift, Action.ToggleCtrl, Action.ToggleAlt, Action.ToggleZalgo, Action.TransientShift -> {
                    // handled internally in Keyboard
                }

                Action.ToggleSelect -> gestureBeginPosition =
                    currentCursorPosition(direction = SearchDirection.Backwards)
                        ?: 0

                Action.Copy -> {
                    if (selectionSize() == SelectionSize.Empty) {
                        inputConnection.performContextMenuAction(android.R.id.selectAll)
                        // HACK: In some editors (like Jetpack Compose's TextField), selectAll is
                        // asynchronous and doesn't actually select immediately, so the copy
                        // ends up copying nothing.
                        // Delaying slightly seems to make it more reliable.
                        // HACK²: For some inscrutable reason, holding a Handler reference into the
                        // OnAction lambda makes the `defaultFocus` Modifier act up,
                        // while creating it on demand works fine? Yeah, I don't get it either...
                        Handler(context.mainLooper).postDelayed(delayInMillis = 1) {
                            inputConnection.performContextMenuAction(android.R.id.copy)
                        }
                    } else {
                        inputConnection.performContextMenuAction(android.R.id.copy)
                    }
                }

                Action.Cut ->
                    if (selectionSize() != SelectionSize.Empty) {
                        inputConnection.performContextMenuAction(android.R.id.cut)
                    }

                Action.Paste ->
                    inputConnection.performContextMenuAction(
                        android.R.id.paste,
                    )

                Action.Undo ->
                    sendKeyPressEvents(
                        KeyEvent.KEYCODE_Z,
                        extraModifiers = KeyEvent.normalizeMetaState(
                            KeyEvent.META_CTRL_LEFT_ON,
                        ),
                        ignoreActiveModifiers = true,
                    )

                Action.Redo ->
                    sendKeyPressEvents(
                        KeyEvent.KEYCODE_Z,
                        extraModifiers = KeyEvent.normalizeMetaState(
                            KeyEvent.META_CTRL_LEFT_ON or KeyEvent.META_SHIFT_LEFT_ON,
                        ),
                        ignoreActiveModifiers = true,
                    )

                Action.SelectAll ->
                    inputConnection.performContextMenuAction(
                        android.R.id.selectAll,
                    )

                Action.Settings -> context.startActivity(
                    Intent.makeMainActivity(
                        ComponentName(context, MainActivity::class.java),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )

                is Action.SwitchLetterLayer -> {
                    val activeIndex = settingsCtx[appSettings.activeLetterLayerIndex]
                    activeIndex.currentValue =
                        (activeIndex.currentValue + action.direction.factor)
                            .mod(settingsCtx[appSettings.letterLayers].currentValue.size)
                }

                is Action.SwitchSystemKeyboard -> {
                    context.getSystemService<InputMethodManager>()?.showInputMethodPicker()
                }

                Action.ToggleActiveLayer -> {
                    val enabledLayers =
                        appSettings.enabledLayersProjectionForOrientation(displayLimits)

                    settingsCtx[appSettings.handedness].modify { !it }

                    // Toggling *appears* to do nothing if using doubleletters and both sides are the same,
                    // see https://codeberg.org/natkr/flickboard/issues/318
                    // We can't really *fix* it due to https://codeberg.org/natkr/flickboard/issues/318#issuecomment-2932842,
                    // but we can at least tell the user what went wrong and how to fix it.
                    if (settingsCtx[enabledLayers].currentValue == EnabledLayers.DoubleLetters &&
                        settingsCtx[appSettings.letterLayers].currentValue.getOrNull(settingsCtx[appSettings.activeLetterLayerIndex].currentValue)
                        == settingsCtx[appSettings.secondaryLetterLayer].currentValue
                    ) {
                        warningMessageScope.launch {
                            warningSnackbarHostState.showSnackbar("Toggling when using the double-letters layout flips the sides, which does nothing if both sides are already the same.\nTo switch to a numbers layout, hold the key instead.")
                        }
                    }
                }

                Action.ToggleHandedness -> {
                    settingsCtx[appSettings.handedness].modify { !it }
                    settingsCtx[appSettings.portraitLocation].modify { -it }
                    settingsCtx[appSettings.landscapeLocation].modify { -it }
                }

                Action.ToggleNumbersLayer -> {
                    settingsCtx[appSettings.enabledLayersProjectionForOrientation(displayLimits)]
                        .modify { old -> old?.toggleNumbers }
                }

                is Action.AdjustCellHeight ->
                    settingsCtx[appSettings.keyHeight].currentValue += action.amount

                is Action.OpenEmojiKeyboard ->
                    navController.navigate(ActiveView.EmojiKeyboard(search = action.search))

                Action.OpenClipboardHistory ->
                    navController.navigate(ActiveView.ClipboardHistory)

                Action.ToggleShowSymbols ->
                    settingsCtx[appSettings.showSymbols].modify { !it }

                Action.ToggleShowLetters ->
                    settingsCtx[appSettings.showLetters].modify { !it }

                Action.EnableVoiceMode -> {
                    val inputManager =
                        context.getSystemService<InputMethodManager>()
                    val voiceMethodId = inputManager?.let(::getVoiceInputId)
                    if (voiceMethodId != null) {
                        // Only works on Android 9+ and when a compatible
                        // voice keyboard is installed
                        switchInputMethod(voiceMethodId)
                    } else {
                        warningMessageScope.launch {
                            warningSnackbarHostState
                                .showSnackbar("No voice input method found")
                        }
                    }
                }
            }
            actionSuccessful
        }
    }

    fun updateModifierState(newModifiers: ModifierState) {
        if (newModifiers != activeModifiers) {
            fun newKeyEvent(
                isDown: Boolean,
                code: Int,
            ): KeyEvent =
                KeyEvent(
                    0,
                    0,
                    when {
                        isDown -> KeyEvent.ACTION_DOWN
                        else -> KeyEvent.ACTION_UP
                    },
                    code,
                    0,
                    newModifiers.androidMetaState,
                    KeyCharacterMap.VIRTUAL_KEYBOARD,
                    0,
                    KeyEvent.FLAG_SOFT_KEYBOARD,
                )
            if (newModifiers.shift.isShift != activeModifiers.shift.isShift) {
                inputConnection.sendKeyEvent(
                    newKeyEvent(
                        newModifiers.shift.isShift,
                        KeyEvent.KEYCODE_SHIFT_LEFT,
                    ),
                )
            }
            if (newModifiers.shift.isCapsLock != activeModifiers.shift.isCapsLock) {
                inputConnection.sendKeyEvent(
                    newKeyEvent(
                        newModifiers.shift.isCapsLock,
                        KeyEvent.KEYCODE_CAPS_LOCK,
                    ),
                )
            }
            if (newModifiers.ctrl != activeModifiers.ctrl) {
                inputConnection.sendKeyEvent(
                    newKeyEvent(
                        newModifiers.ctrl,
                        KeyEvent.KEYCODE_CTRL_LEFT,
                    ),
                )
            }
            if (newModifiers.alt != activeModifiers.alt) {
                inputConnection.sendKeyEvent(
                    newKeyEvent(
                        newModifiers.alt,
                        KeyEvent.KEYCODE_ALT_LEFT,
                    ),
                )
            }
            activeModifiers = newModifiers
        }
    }

    fun updateCursorAnchorInfo(cursorAnchorInfo: CursorAnchorInfo?) {
        cursor = cursorAnchorInfo
        // Best-effort: drop lastTyped when the user moves the selection,
        // such that moving the cursor away and then back into position
        // will still clear it.
        if (cursorAnchorInfo?.selectionStart != lastTyped?.position) {
            lastTyped = null
        }
    }

    /**
     * Updates whether the InputSession is currently "shadowed" by another nested input field
     */
    fun updateIsShadowed(isShadowed: Boolean) {
        if (Build.VERSION.SDK_INT >= 31) {
            inputConnection.setImeConsumesInput(isShadowed)
        }
    }

    /**
     * Opaque key used to associate [inlineAutofillOptionViews] with a specific
     * autofill session.
     */
    var inlineAutofillKey: Any = Any()

    /**
     * Views representing inline autofill response options.
     *
     * `null` is used for views that are still being inflated.
     */
    val inlineAutofillOptionViews = mutableStateListOf<View?>()
}