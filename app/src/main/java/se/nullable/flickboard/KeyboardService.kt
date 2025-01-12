package se.nullable.flickboard

import android.inputmethodservice.InputMethodService
import android.os.Build
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.setViewTreeOnBackPressedDispatcherOwner
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.WindowInfo
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import se.nullable.flickboard.model.ActionVisual
import se.nullable.flickboard.ui.AppSettings
import se.nullable.flickboard.ui.ConfiguredKeyboard
import se.nullable.flickboard.ui.EnabledLayers
import se.nullable.flickboard.ui.FlickBoardParent
import se.nullable.flickboard.ui.ProvideDisplayLimits
import se.nullable.flickboard.ui.SettingsContext
import se.nullable.flickboard.ui.appDatabaseForContext
import se.nullable.flickboard.ui.clipboard.ClipboardHistoryViewer
import se.nullable.flickboard.ui.emoji.EmojiKeyboard
import se.nullable.flickboard.ui.emoji.EmojiTab
import se.nullable.flickboard.ui.layout.BottomUpColumn
import se.nullable.flickboard.ui.theme.LocalKeyboardTheme
import se.nullable.flickboard.ui.util.sharePointerInput
import se.nullable.flickboard.util.onBackPressedDispatcherWrapperCallback

class KeyboardService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner,
    SavedStateRegistryOwner, OnBackPressedDispatcherOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle = lifecycleRegistry

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry =
        savedStateRegistryController.savedStateRegistry

    override val viewModelStore: ViewModelStore = ViewModelStore()

    private val appDatabase = lazy { appDatabaseForContext(this@KeyboardService) }

    private var viewHeight: Int = 0 // Set once view is measured
    private var keyboardHeight: Int = 0 // Set once view is measured
    private var baseKeyboardHeight: Int = 0 // Set once view is measured
    override fun onComputeInsets(outInsets: Insets) {
        super.onComputeInsets(outInsets)
        // Some apps dislike it when there isn't enough room to render the text field.
        // See for example: https://github.com/facebook/react-native/issues/48618
        // To work around this, we only request that apps resize to make room for the bottommost keyboard,
        // and request that the system pan the app to accomodate the others (if required).
        outInsets.contentTopInsets = viewHeight - baseKeyboardHeight
        // We fill the whole screen with an invisible background layer, to allow gestures in progress
        // to leave the IME window.
        outInsets.visibleTopInsets = viewHeight - keyboardHeight
        outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_VISIBLE
    }

    // The inset hack breaks fullscreen, and trying to adjust based on it causes glitches
    // in some apps depending on circumstances (flickering in YouTube comments,
    // Discord force-hiding the keyboard, etc).
    // For now, we just accept the tradeoff and disable fullscreen entirely.
    override fun onEvaluateFullscreenMode(): Boolean = false

    // currentInputSessions[0] is always the "base"/"OS" input session
    // Any sessions on top are nested
    private val currentInputSessions = mutableStateListOf<InputSession>()
    private val baseInputSession: InputSession?
        get() = currentInputSessions.getOrNull(0)

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        val newInputConnection = currentInputConnection

        currentInputSessions.clear()
        if (newInputConnection == null) {
            return
        }

        currentInputSessions.add(
            InputSession(
                newInputConnection,
                attribute,
            ),
        )

        var cursorUpdatesRequested = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                newInputConnection.requestCursorUpdates(
                    InputConnection.CURSOR_UPDATE_MONITOR,
                    InputConnection.CURSOR_UPDATE_FILTER_EDITOR_BOUNDS,
                )
        // Even on modern android, some apps only support unfiltered requestCursorUpdates,
        // so fall back to trying that.
        cursorUpdatesRequested = cursorUpdatesRequested ||
                newInputConnection.requestCursorUpdates(
                    InputConnection.CURSOR_UPDATE_MONITOR,
                )

        if (!cursorUpdatesRequested) {
            println("no cursor data :(")
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreateInputView(): View {
        window.window?.let { window ->
            val decorView = window.decorView
            decorView.setViewTreeLifecycleOwner(this)
            decorView.setViewTreeSavedStateRegistryOwner(this)
            decorView.setViewTreeViewModelStoreOwner(this)
            decorView.setViewTreeOnBackPressedDispatcherOwner(this)
        }
        return ComposeView(this).also { view ->
            view.setContent {
                BoxWithConstraints(
                    Modifier.onSizeChanged { size ->
                        viewHeight = size.height
                    },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    // Android clips ongoing touch events at the border between the keyboard and app.
                    // As a workaround, we claim a transparent whole-screen box, and then
                    // use an inset to limit the content region (for touch input and app resizing)
                    // to the region we actually occupy.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(maxHeight),
                    )
                    Box(
                        Modifier.onSizeChanged { size ->
                            keyboardHeight = size.height
                        },
                    ) {
                        FlickBoardParent(appDatabase = appDatabase) {
                            ProvideDisplayLimits {
                                val snackbarHostState = remember { SnackbarHostState() }
                                val keyboardTheme = LocalKeyboardTheme.current

                                SideEffect {
                                    window.window?.let { window ->
                                        val insets = WindowCompat.getInsetsController(
                                            window,
                                            window.decorView,
                                        )
                                        // This is deprecated in API35 because it's the app's
                                        // responsibility to handle there, but we also need to
                                        // handle older versions.
                                        @Suppress("DEPRECATION")
                                        window.navigationBarColor =
                                            keyboardTheme.surfaceColour.toArgb()
                                        insets.isAppearanceLightNavigationBars =
                                            !keyboardTheme.isDark
                                    }
                                }

                                BottomUpColumn {
                                    // Input methods aren't allowed to request input themselves, so
                                    // instead we capture all nested input requests, and stack them.
                                    InterceptPlatformTextInput(
                                        { request, /*nextHandler*/ _ ->
                                            val editorInfo = EditorInfo()
                                            val newInputConnection =
                                                request.createInputConnection(editorInfo)
                                            val newInputSession =
                                                InputSession(newInputConnection, editorInfo)
                                            currentInputSessions.add(newInputSession)
                                            try {
                                                // Not calling into the nextHandler, since we
                                                // effectively take over keyboard rendering
                                                awaitCancellation()
                                            } finally {
                                                newInputConnection.closeConnection()
                                                currentInputSessions.remove(newInputSession)
                                            }
                                        },
                                    ) {
                                        LaunchedEffect(currentInputSessions.size) {
                                            window.window?.setFlags(
                                                when {
                                                    // When there are nested inputs:
                                                    // - We need to clear FLAG_NOT_FOCUSABLE so we can grab raw (keypress) input
                                                    //     (this is required for hardware keyboard input, but also raw keycodes sent by
                                                    //     IME itself, as well as some editor actions like copy/pasting).
                                                    // - We need to set FLAG_ALT_FOCUSABLE_IM or we get stuck in an infinite
                                                    //     loop of Android setting our IME as the current IME target, and immediately
                                                    //     closing the old IME because it no longer has focus.
                                                    currentInputSessions.size > 1 ->
                                                        WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM

                                                    // When there are no nested inputs:
                                                    // - We need to set FLAG_NOT_FOCUSABLE so the backing app regains focus.
                                                    // - We need to clear FLAG_ALT_FOCUSABLE_IM since we don't want to invert
                                                    //     the behaviour of FLAG_NOT_FOCUSABLE.
                                                    else -> WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                                },
                                                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                                                        or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                                            )
                                        }
                                        currentInputSessions.forEachIndexed { inputSessionIdx, inputSession ->
                                            // A lot of state (including forwarded closures) depends on
                                            // the inputSession - safer to just throw it all away if it changes
                                            // than to try to track it precisely.
                                            key(inputSession) {
                                                SideEffect {
                                                    inputSession.updateIsShadowed(
                                                        inputSessionIdx != currentInputSessions.lastIndex,
                                                    )
                                                }
                                                CompositionLocalProvider(
                                                    // Override LocalSoftwareKeyboardController to affect
                                                    // the nested input instead.
                                                    LocalSoftwareKeyboardController provides
                                                            remember(inputSessionIdx) {
                                                                val nestedInputSessionIdx =
                                                                    inputSessionIdx + 1
                                                                object :
                                                                    SoftwareKeyboardController {
                                                                    var hiddenSession: InputSession? =
                                                                        null

                                                                    override fun hide() {
                                                                        hiddenSession =
                                                                            currentInputSessions.removeAt(
                                                                                nestedInputSessionIdx,
                                                                            )
                                                                    }

                                                                    override fun show() {
                                                                        hiddenSession?.let { session ->
                                                                            currentInputSessions.add(
                                                                                nestedInputSessionIdx,
                                                                                session,
                                                                            )
                                                                        }
                                                                        hiddenSession = null
                                                                    }
                                                                }
                                                            },
                                                    // Some UI components (like text selection handles)
                                                    // only render for the focused window.
                                                    // We're weird in that as an IME we're never considered
                                                    // focused by the OS, but are always the user's
                                                    // primary interaction target.
                                                    // Hence, we lie and pretend that we're always focused.
                                                    LocalWindowInfo provides FocusedWindowInfo,
                                                ) {
                                                    val navController = rememberNavController()
                                                    val onAction = inputSession.actionHandler(
                                                        warningSnackbarHostState = snackbarHostState,
                                                        switchInputMethod = ::switchInputMethod,
                                                        navController = navController,
                                                    )
                                                    val baseKeyboardHeightUpdaterModifier = when {
                                                        inputSessionIdx == currentInputSessions.lastIndex ->
                                                            Modifier.onSizeChanged { size ->
                                                                baseKeyboardHeight = size.height
                                                            }

                                                        else -> Modifier
                                                    }
                                                    Box(baseKeyboardHeightUpdaterModifier) {
                                                        NavHost(
                                                            navController,
                                                            startDestination = InputSession.ActiveView.Keyboard,
                                                            contentAlignment = Alignment.BottomCenter,
                                                            // The input connection stays open while the exit animation is running, so
                                                            // disable animations for now to avoid having two visible "regular" keyboards
                                                            // at the same time.
                                                            enterTransition = { EnterTransition.None },
                                                            exitTransition = { ExitTransition.None },
                                                            popEnterTransition = { EnterTransition.None },
                                                            popExitTransition = { ExitTransition.None },
//                                                            enterTransition = { fadeIn() + slideInVertically { it } + expandVertically() },
//                                                            exitTransition = { fadeOut() + slideOutVertically { it } + shrinkVertically() },
//                                                            popEnterTransition = { fadeIn() + slideInVertically { it } + expandVertically() },
//                                                            popExitTransition = { fadeOut() + slideOutVertically { it } + shrinkVertically() },
                                                        ) {
                                                            composable<InputSession.ActiveView.ClipboardHistory> {
                                                                ClipboardHistoryViewer(
                                                                    onAction = onAction,
                                                                    snackbarHostState = snackbarHostState,
                                                                    navigateUp = navController::navigateUp,
                                                                )
                                                            }

                                                            composable<InputSession.ActiveView.EmojiKeyboard> { backStackEntry ->
                                                                val route =
                                                                    backStackEntry.toRoute<InputSession.ActiveView.EmojiKeyboard>()
                                                                EmojiKeyboard(
                                                                    onAction = onAction,
                                                                    navigateUp = navController::navigateUp,
                                                                    defaultTab = when {
                                                                        route.search -> EmojiTab.Search
                                                                        else -> null
                                                                    },
                                                                )
                                                            }

                                                            composable<InputSession.ActiveView.Keyboard> {
                                                                ConfiguredKeyboard(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    onAction = onAction,
                                                                    onModifierStateUpdated = inputSession::updateModifierState,
                                                                    enterVisualOverride = inputSession.editorInfo?.let { editorInfo ->
                                                                        editorInfo.actionLabel?.toString()
                                                                            ?.let(ActionVisual::Label)
                                                                            ?: when (editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION) {
                                                                                EditorInfo.IME_ACTION_GO -> ActionVisual.Label(
                                                                                    "GO",
                                                                                )

                                                                                EditorInfo.IME_ACTION_SEARCH -> ActionVisual.Icon(
                                                                                    R.drawable.baseline_search_24,
                                                                                )

                                                                                EditorInfo.IME_ACTION_SEND -> ActionVisual.Icon(
                                                                                    R.drawable.baseline_send_24,
                                                                                )

                                                                                EditorInfo.IME_ACTION_DONE -> ActionVisual.Icon(
                                                                                    R.drawable.baseline_done_24,
                                                                                )
                                                                                /* Not using the next/prev icons since they can be confused with the jump left/right icons */
                                                                                EditorInfo.IME_ACTION_NEXT -> ActionVisual.Label(
                                                                                    "NEXT",
                                                                                )

                                                                                EditorInfo.IME_ACTION_PREVIOUS -> ActionVisual.Label(
                                                                                    "PREV",
                                                                                )

                                                                                else -> null
                                                                            }
                                                                    },
                                                                    overrideEnabledLayers = when (inputSession.editorInfo?.let { it.inputType and InputType.TYPE_MASK_CLASS }) {
                                                                        InputType.TYPE_CLASS_NUMBER, InputType.TYPE_CLASS_PHONE -> EnabledLayers.Numbers
                                                                        else -> null
                                                                    },
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                SnackbarHost(
                                    snackbarHostState,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .sharePointerInput()
                                        .safeContentPadding(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override val onBackPressedDispatcher: OnBackPressedDispatcher = OnBackPressedDispatcher()
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean = when {
        keyCode == KeyEvent.KEYCODE_BACK && onBackPressedDispatcher.hasEnabledCallbacks() -> {
            onBackPressedDispatcher.onBackPressed()
            true
        }

        else -> super.onKeyDown(keyCode, event)
    }

    override fun onUpdateCursorAnchorInfo(cursorAnchorInfo: CursorAnchorInfo?) {
        super.onUpdateCursorAnchorInfo(cursorAnchorInfo)
        baseInputSession?.updateCursorAnchorInfo(cursorAnchorInfo)
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)

        // Register predictive back
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // onBackInvoked: System API
            // onBackPressed: Jetpack compatibility wrapper API
            // This is a confusing mess, but essentially:
            // - onBackInvokedDispatcher.registerOnBackInvokedCallback is *only* called for events
            //   while the input window is focused
            // - onBackPressedDispatcher.setOnBackInvokedDispatcher is *only* called for events
            //   forwarded from the app being input into (so while the input window is unfocused).
            // Not sure about why.. the implementations seem basically equivalent as far as I can tell.
            val onBackInvokedDispatcher = window.onBackInvokedDispatcher
            onBackPressedDispatcher.setOnBackInvokedDispatcher(onBackInvokedDispatcher)
            // This means we can't unregister the onBackInvokedCallback, but there when focused
            // there are always things to back to.
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                onBackPressedDispatcherWrapperCallback(onBackPressedDispatcher),
            )
        }

        // Normally, InputMethodService.onKeyDown (and co.) callbacks are only called for key events
        // *forwarded from the application's input field*.
        // This makes sense, since IMEs normally never have focus.
        // However, we *sometimes do*, and in those cases we still want to intercept them as usual.
        // --- QUIRKS
        // On API 33+ (with OnBackInvokedDispatcher): setOnKeyListener does *not* get invoked
        //   for back events, even if there is no backinvoked listener.
        // However, on older (tested on API 30), backing when focused and not having an onKeyListener
        //   invokes a system default back of some kind, that: 1) leaves an empty window, 2) seems
        //   to clear window insets permanently.
        val dialogDispatcherState = KeyEvent.DispatcherState()
        window.setOnKeyListener { _, _, event ->
            event.dispatch(this, dialogDispatcherState, this)
        }

        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        lifecycleScope.launch {
            // Background jobs (which should be active even while composables are not) cannot
            // access the composable context.
            val backgroundAppSettings = AppSettings(
                SettingsContext(
                    prefs = SettingsContext.appPrefsForContext(
                        context = this@KeyboardService,
                    ),
                    coroutineScope = this,
                    appDatabase = appDatabase,
                ),
            )
            clipboardTracker(
                appSettings = backgroundAppSettings,
                context = this@KeyboardService,
            )
        }
    }

    override fun onStartInputView(editorInfo: EditorInfo?, restarting: Boolean) {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        super.onStartInputView(editorInfo, restarting)
    }

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
        super.onDestroy()
    }
}

private object FocusedWindowInfo : WindowInfo {
    override val isWindowFocused: Boolean = true
}