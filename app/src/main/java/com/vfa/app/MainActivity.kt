package com.vfa.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/**
 * Single-activity host. The whole guided run lives in [VfaApp].
 *
 * Edge-to-edge, with the screens taking their own insets — the camera checkpoint and
 * the reader capture run full bleed behind the status bar, everything else clears it.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { VfaApp() }
    }
}
