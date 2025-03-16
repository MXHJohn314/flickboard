package se.nullable.flickboard.formr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import se.nullable.flickboard.formr.ui.theme.FormrTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FormrTheme {
                val snackbarState = remember { SnackbarHostState() }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarState, Modifier.safeDrawingPadding()) },
                ) { innerPadding ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(innerPadding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Welcome to Formr!")
                        val username = remember { mutableStateOf("") }
                        val password = remember { mutableStateOf("") }
                        val loginScope = rememberCoroutineScope()
                        val failureSnackbarScope = rememberCoroutineScope()
                        val sendIs = remember { MutableInteractionSource() }

                        val isLoggingIn = remember { mutableStateOf(false) }
                        val loginMutex = remember { UiMutex() }
                        suspend fun logIn() {
                            assert(!isLoggingIn.value)
                            val success = loginMutex.withLock {
                                delay(1000)
                                false
                            }
                            if (!success) {
                                failureSnackbarScope.coroutineContext.cancelChildren()
                                failureSnackbarScope.launch {
                                    snackbarState.showSnackbar("Login failed")
                                }
                            }
                        }

                        TextField(
                            username.value,
                            username::value::set,
                            enabled = !loginMutex.isLocked,
                            singleLine = true,
                            label = { Text("Username") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        )
                        TextField(
                            password.value,
                            password::value::set,
                            enabled = !loginMutex.isLocked,
                            singleLine = true,
                            label = { Text("Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Send,
                            ),
                            keyboardActions = KeyboardActions(
                                onSend = { loginScope.launch { logIn() } },
                            ),
                        )
                        Button(
                            onClick = { loginScope.launch { logIn() } },
                            interactionSource = sendIs,
                            enabled = !loginMutex.isLocked,
                        ) { Text("Log in") }
                    }
                }
            }
        }
    }
}

class UiMutex {
    private val innerLock = Mutex()
    var isLocked by mutableStateOf(false)
        private set

    suspend fun <T> withLock(f: suspend () -> T): T =
        innerLock.withLock {
            isLocked = true
            try {
                f()
            } finally {
                isLocked = false
            }
        }
}