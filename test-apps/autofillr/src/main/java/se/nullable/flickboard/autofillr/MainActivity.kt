package se.nullable.flickboard.autofillr

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import se.nullable.flickboard.autofillr.ui.theme.FlickBoardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlickBoardTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        val ctx = LocalContext.current
                        Button(
                            {
                                ctx.startActivity(
                                    Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE)
                                        .setData("package:${ctx.packageName}".toUri()),
                                )
                            },
                        ) { Text("Open Autofill Settings") }
                    }
                }
            }
        }
    }
}
