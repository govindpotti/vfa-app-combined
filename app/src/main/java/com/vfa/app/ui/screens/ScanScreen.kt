package com.vfa.app.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vfa.app.camera.CameraViewfinder
import com.vfa.app.camera.VfaCameraState
import com.vfa.app.protocol.ScanKind
import com.vfa.app.protocol.Stage
import com.vfa.app.protocol.spokenGuidance
import com.vfa.app.ui.components.*
import com.vfa.app.ui.theme.*
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
// The two reader photos.
//
// Stage 3 takes the baseline — the membrane before any signal. Stage 14 takes the
// final read. Both go to the analyzer together, which subtracts the baseline so
// illumination and membrane background cancel out.
//
// The alignment guidance is the whole game here: the top-right corner is the anchor
// the spot-localisation aligns against, so it gets the live, pulsing bracket while
// the other three stay quiet.
// ─────────────────────────────────────────────────────────────────────────────

private enum class ScanPhase { ALIGN, CAPTURING }

@Composable
fun ScanScreen(
    stage: Stage,
    stageNumber: Int,
    stageTotal: Int,
    camera: VfaCameraState,
    onCaptured: suspend (ByteArray?) -> Unit,
) {
    var phase by remember(stage) { mutableStateOf(ScanPhase.ALIGN) }
    var showDemo by remember(stage) { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(stage, phase) {
        if (phase == ScanPhase.CAPTURING) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(2600)
            val frame = camera.capture()
            // Keep this screen up long enough for the spoken hold-steady cue to finish.
            delay(2200)
            onCaptured(frame)
        }
    }

    if (showDemo && stage.clips.isNotEmpty()) {
        Dialog(
            onDismissRequest = { showDemo = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(CamDarker.copy(alpha = 0.9f))
                    .clickable { showDemo = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "DEMONSTRATION",
                        fontFamily = BodyFont, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                        color = Scan, letterSpacing = 1.4.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    StepVisual(
                        clips = stage.clips,
                        height = 300.dp,
                        playSequence = stage.playClipsInSequence
                    )
                    Spacer(Modifier.height(16.dp))
                    GhostButton("Close") { showDemo = false }
                }
            }
        }
    }

    when (phase) {
        ScanPhase.ALIGN -> Box(Modifier.fillMaxSize()) {
            ScreenBlobs()
            ScreenWrapper {
                VfaHeader()

                StepProgress(
                    kicker = stage.kicker,
                    current = stageNumber,
                    total = stageTotal,
                    modifier = Modifier.padding(top = 18.dp, start = 24.dp, end = 24.dp)
                )

                Column(Modifier.padding(top = 16.dp, start = 24.dp, end = 24.dp)) {
                    Text(
                        stage.title,
                        fontFamily = TitleFont, fontSize = 30.sp, fontWeight = FontWeight.Bold,
                        color = Navy
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stage.instruction,
                        fontFamily = BodyFont, fontSize = 15.sp, color = Navy, lineHeight = 23.sp
                    )

                    Spacer(Modifier.height(12.dp))
                    SpokenSubtitle(
                        text = stage.cue,
                        spokenText = stage.spokenGuidance(stageNumber, stageTotal)
                    )

                    Spacer(Modifier.height(14.dp))
                    ReaderFrame(camera)

                    if (stage.clips.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        WatchRow { showDemo = true }
                    }

                    Spacer(Modifier.height(12.dp))
                    HelpAccordion(stage.help)

                    Spacer(Modifier.height(20.dp))
                    CTAButton(
                        label = if (stage.scan == ScanKind.FINAL) "Take the final photo" else "Take photo",
                        onClick = { phase = ScanPhase.CAPTURING }
                    )
                }
            }
        }

        ScanPhase.CAPTURING -> CapturingScreen(
            kicker = stage.kicker,
            title = stage.title,
            isFinal = stage.scan == ScanKind.FINAL,
            cue = if (stage.scan == ScanKind.FINAL) {
                "Hold steady while the analyzer photo is saved."
            } else {
                "Hold steady while the baseline photo is saved."
            }
        )
    }
}

@Composable
private fun WatchRow(onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Surface(
        modifier = Modifier.fillMaxWidth().clickable {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        shape = RoundedCornerShape(16.dp),
        color = White,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Line)
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(Coral),
                contentAlignment = Alignment.Center
            ) { PlayGlyph(White, 11.dp) }
            Spacer(Modifier.width(12.dp))
            Text(
                "Watch the reader demonstration",
                fontFamily = BodyFont, fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold, color = Navy, modifier = Modifier.weight(1f)
            )
            Text("›", color = Muted, fontSize = 20.sp)
        }
    }
}

/** Live reader view with the alignment guides drawn over it. */
@Composable
private fun ReaderFrame(camera: VfaCameraState) {
    val t = rememberInfiniteTransition(label = "align")
    val pulse by t.animateFloat(
        0.45f, 1f,
        infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "p"
    )

    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(24.dp))
            .background(CamDark)
            .border(1.dp, Line, RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (camera.granted) {
            CameraViewfinder(camera, Modifier.fillMaxSize())
        } else {
            Column(
                Modifier.padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Camera is off",
                    fontFamily = BodyFont, fontSize = 15.sp,
                    fontWeight = FontWeight.Bold, color = White.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Allow camera access to photograph the membrane. You can still walk " +
                        "through the rest of the test without it.",
                    fontFamily = BodyFont, fontSize = 12.sp,
                    color = White.copy(alpha = 0.5f), textAlign = TextAlign.Center, lineHeight = 18.sp
                )
            }
        }

        // The membrane target: keep it centred, and pin the top-right corner.
        Box(
            Modifier.fillMaxSize().drawBehind {
                val pad = 18.dp.toPx()
                val s = 30.dp.toPx()
                val w = 3.dp.toPx()
                val dim = White.copy(alpha = 0.4f)
                val right = size.width - pad
                val bottom = size.height - pad
                drawLine(dim, Offset(pad, pad), Offset(pad + s, pad), w)
                drawLine(dim, Offset(pad, pad), Offset(pad, pad + s), w)
                drawLine(dim, Offset(pad, bottom), Offset(pad + s, bottom), w)
                drawLine(dim, Offset(pad, bottom), Offset(pad, bottom - s), w)
                drawLine(dim, Offset(right, bottom), Offset(right - s, bottom), w)
                drawLine(dim, Offset(right, bottom), Offset(right, bottom - s), w)

                val hot = Scan.copy(alpha = pulse)
                drawLine(hot, Offset(right, pad), Offset(right - s * 1.3f, pad), w * 1.4f)
                drawLine(hot, Offset(right, pad), Offset(right, pad + s * 1.3f), w * 1.4f)

                // Centre target for the membrane.
                drawCircle(
                    White.copy(alpha = 0.22f),
                    radius = size.minDimension * 0.26f,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx())
                )
            }
        )

        Text(
            "align",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 54.dp, end = 22.dp),
            fontFamily = BodyFont, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Scan
        )

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(14.dp)
                .clip(CircleShape)
                .background(CamDarker.copy(alpha = 0.72f))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                "Line up the top-right corner · keep the membrane centred",
                fontFamily = BodyFont, fontSize = 11.sp, color = White.copy(alpha = 0.85f)
            )
        }
    }
}

/** Full-bleed "reading the membrane" state — the sweep from the guided app. */
@Composable
private fun CapturingScreen(kicker: String, title: String, isFinal: Boolean, cue: String) {
    val t = rememberInfiniteTransition(label = "cap")
    val sweep by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sw"
    )
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "sp")

    Column(
        Modifier
            .fillMaxSize()
            .background(CamDarker)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 26.dp)
            .padding(top = 18.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            kicker,
            fontFamily = BodyFont, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
            color = Scan, letterSpacing = 1.4.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            title,
            fontFamily = TitleFont, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = White
        )

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(190.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(Color(0xFFE9EDEA), Color(0xFFC2CCC6)))
                    )
                    .drawBehind {
                        val y = size.height * (0.06f + 0.88f * sweep)
                        drawRect(
                            brush = Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.5f to Scan.copy(alpha = 0.42f),
                                1f to Color.Transparent,
                                startY = y - size.height * 0.14f, endY = y + size.height * 0.14f
                            ),
                            topLeft = Offset(0f, y - size.height * 0.14f),
                            size = Size(size.width, size.height * 0.28f)
                        )
                        drawLine(Scan, Offset(0f, y), Offset(size.width, y), 3.dp.toPx())
                    }
            )
        }

        SpokenSubtitle(
            text = cue,
            spokenText = "$title. $cue",
            dark = true
        )
        Spacer(Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(18.dp).drawBehind {
                    drawArc(
                        Scan, startAngle = spin, sweepAngle = 90f, useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx())
                    )
                }
            )
            Spacer(Modifier.width(10.dp))
            Text(
                if (isFinal) "Reading the membrane…" else "Saving the baseline…",
                fontFamily = BodyFont, fontSize = 15.sp, color = White.copy(alpha = 0.85f)
            )
        }
    }
}
