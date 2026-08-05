package com.vfa.app.ui.components

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vfa.app.ui.theme.BodyFont
import com.vfa.app.ui.theme.CamDarker
import com.vfa.app.ui.theme.LavDeep
import com.vfa.app.ui.theme.LavLight
import com.vfa.app.ui.theme.Line
import com.vfa.app.ui.theme.Navy
import com.vfa.app.ui.theme.Scan
import com.vfa.app.ui.theme.White
import java.util.Locale

@Composable
fun SpokenSubtitle(
    text: String,
    spokenText: String,
    modifier: Modifier = Modifier,
    dark: Boolean = false,
) {
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(text) {
        if (text.isNotBlank()) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    SpeakOnChange(spokenText)
    SubtitleCue(text = text, modifier = modifier, dark = dark)
}

@Composable
fun SpeakOnChange(text: String) {
    val context = LocalContext.current
    val currentText by rememberUpdatedState(text)
    var ready by remember { mutableStateOf(false) }
    val tts = remember(context) {
        TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
        }
    }

    DisposableEffect(tts) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    LaunchedEffect(tts, ready, currentText) {
        if (!ready || currentText.isBlank()) return@LaunchedEffect
        tts.language = Locale.US
        tts.setSpeechRate(0.92f)
        tts.setPitch(1.0f)
        tts.speak(
            currentText.toSpeechFriendly(),
            TextToSpeech.QUEUE_FLUSH,
            Bundle.EMPTY,
            "vfa-${currentText.hashCode()}"
        )
    }
}

@Composable
private fun SubtitleCue(
    text: String,
    modifier: Modifier = Modifier,
    dark: Boolean = false,
) {
    if (text.isBlank()) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(16.dp),
        color = if (dark) CamDarker.copy(alpha = 0.76f) else LavLight,
        border = BorderStroke(1.dp, if (dark) Scan.copy(alpha = 0.55f) else Line)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Surface(shape = CircleShape, color = if (dark) Scan.copy(alpha = 0.18f) else White) {
                Text(
                    "SUBTITLE",
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    fontFamily = BodyFont,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (dark) Scan else LavDeep,
                    letterSpacing = 1.1.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text,
                fontFamily = BodyFont,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (dark) White.copy(alpha = 0.9f) else Navy,
                lineHeight = 21.sp
            )
        }
    }
}

private fun String.toSpeechFriendly(): String =
    replace("µL", " microliters")
        .replace("pL", " microliters")
        .replace("—", ", ")
        .replace("·", ", ")
        .replace("…", ".")
        .replace("VFA", "V F A")
