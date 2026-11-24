package io.github.smallora.sharereborn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.smallora.sharereborn.ui.AppRoot
import io.github.smallora.sharereborn.ui.theme.ShareRebornTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ShareRebornTheme {
                AppRoot()
            }
        }
    }
}
