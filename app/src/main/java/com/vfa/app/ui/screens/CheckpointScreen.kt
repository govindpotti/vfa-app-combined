package com.vfa.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vfa.app.backend.CheckDecision
import com.vfa.app.backend.VfaBackend
import com.vfa.app.camera.CameraViewfinder
import com.vfa.app.camera.VfaCameraState
import com.vfa.app.protocol.Stage
import com.vfa.app.ui.components.AmberButton
import com.vfa.app.ui.components.GhostButton
import com.vfa.app.ui.components.SpeakOnChange
import com.vfa.app.ui.theme.*
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
// The step check.
//
// This is the quality control the whole app exists to provide: the same test, run the
// same way, everywhere it's used. The camera opens, holds for a beat while the cassette
// is framed, grabs a frame, and sends it to the step verifier.
//
// A pass is a green check and the test continues. A failure is amber, never red — it
// names the one thing to correct and offers a retry, because a technique slip mid-test
// is fixable, not an emergency. If the verifier isn't configured or can't be reached
// the step is *not* checked, and the screen says exactly that rather than implying a
// check that never happened.
// ─────────────────────────────────────────────────────────────────────────────

private enum class Phase { POSITIONING, ANALYZING, PASS, RETRY }

@Composable
fun CheckpointScreen(
    stage: Stage,
    camera: VfaCameraState,
    onPass: () -> Unit,
    onSkip: () -> Unit,
) {
    var phase by remember(stage) { mutableStateOf(Phase.POSITIONING) }
    var reason by remember(stage) { mutableStateOf("") }
    var attempt by remember(stage) { mutableIntStateOf(0) }
    var simulated by remember(stage) { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(stage, attempt) {
        phase = Phase.POSITIONING
        delay(1800)

        phase = Phase.ANALYZING
        val frame = camera.capture()
        val decision = VfaBackend.verify(stage.checkpoint.orEmpty(), attempt, frame)
        // Never let the "checking" state flicker past — it's what tells the user to hold still.
        delay(800)

        when (decision) {
            is CheckDecision.Pass -> {
                simulated = false
                phase = Phase.PASS
                delay(1500)
                onPass()
            }

            is CheckDecision.Retry -> {
                simulated = false
                reason = decision.reason.ifBlank { "That doesn't look right yet. Check the step and try again." }
                phase = Phase.RETRY
            }

            is CheckDecision.Help -> {
                simulated = false
                reason = decision.reason.ifBlank { "Couldn't get a clear enough look. Get a second pair of eyes on this one." }
                phase = Phase.RETRY
            }

            CheckDecision.Unavailable -> {
                // No verifier deployed, or no camera frame. Keep the run moving, but never
                // report this as a verification — it wasn't one.
                simulated = true
                phase = Phase.PASS
                delay(1500)
                onPass()
            }
        }
    }

    val bg by animateColorAsState(if (phase == Phase.RETRY) AmberInk.copy(alpha = 0.96f) else CamDarker, label = "bg")
    val statusTitle = when (phase) {
        Phase.POSITIONING -> "Point the camera at the cassette"
        Phase.ANALYZING -> "Hold steady"
        Phase.PASS -> if (simulated) "Moving on" else "Checked"
        Phase.RETRY -> "Not quite right"
    }
    val spokenStatus = when (phase) {
        Phase.POSITIONING -> "Point the camera at the cassette for ${stage.title}."
        Phase.ANALYZING -> "Hold steady while this step is checked."
        Phase.PASS -> if (simulated) {
            "This step was not checked by the verifier. Moving on."
        } else {
            "Checked. Continuing."
        }
        Phase.RETRY -> "Not quite right. ${reason.ifBlank { "Check the step and try again." }}"
    }

    SpeakOnChange(spokenStatus)
    LaunchedEffect(phase) {
        when (phase) {
            Phase.ANALYZING -> haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            Phase.PASS, Phase.RETRY -> haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            else -> Unit
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(bg)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 22.dp)
            .padding(top = 14.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "STEP CHECK",
            fontFamily = BodyFont, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
            color = Scan, letterSpacing = 1.4.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            statusTitle,
            fontFamily = TitleFont, fontSize = 26.sp, fontWeight = FontWeight.Bold,
            color = White, textAlign = TextAlign.Center, lineHeight = 30.sp
        )

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Viewfinder(phase = phase, camera = camera, stageTitle = stage.title)
        }

        Box(Modifier.fillMaxWidth().heightIn(min = 150.dp), contentAlignment = Alignment.TopCenter) {
            when (phase) {
                Phase.POSITIONING, Phase.ANALYZING -> SteadyHint(camera.granted)
                Phase.PASS -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (simulated) "Continuing…" else "Checked — continuing…",
                        fontFamily = BodyFont, fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold, color = Scan
                    )
                    if (simulated) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (!VfaBackend.verifierConfigured)
                                "This step wasn't checked — no checker is set up."
                            else "This step wasn't checked — the checker couldn't be reached.",
                            fontFamily = BodyFont, fontSize = 11.sp,
                            color = White.copy(alpha = 0.5f), textAlign = TextAlign.Center
                        )
                    }
                }

                Phase.RETRY -> Column(Modifier.fillMaxWidth()) {
                    RetryCard(reason)
                    Spacer(Modifier.height(12.dp))
                    AmberButton("Try again") { attempt++ }
                    Spacer(Modifier.height(10.dp))
                    GhostButton("Skip this check") { onSkip() }
                }
            }
        }
    }
}

@Composable
private fun Viewfinder(phase: Phase, camera: VfaCameraState, stageTitle: String) {
    val t = rememberInfiniteTransition(label = "vf")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "spin")
    val ringAlpha by t.animateFloat(0.55f, 0f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "ring")
    val ringScale by t.animateFloat(0.94f, 1.18f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "rs")
    val bracket by t.animateFloat(
        0.45f, 1f,
        infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse), label = "brk"
    )

    val borderColor by animateColorAsState(
        when (phase) {
            Phase.RETRY -> Amber
            Phase.PASS -> Green
            else -> Scan.copy(alpha = 0.55f)
        }, label = "border"
    )

    Box(
        Modifier
            .fillMaxWidth(0.86f)
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(24.dp))
            .background(CamDark)
            .border(3.dp, borderColor, RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
    ) {
        // The live camera sits underneath every overlay, so the user can see what the
        // check is actually looking at.
        if (camera.granted) {
            CameraViewfinder(camera, Modifier.fillMaxSize())
        } else {
            Column(
                Modifier.padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Camera unavailable",
                    fontFamily = BodyFont, fontSize = 15.sp,
                    fontWeight = FontWeight.Bold, color = White.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "The test can continue, but steps won't be checked.",
                    fontFamily = BodyFont, fontSize = 12.sp,
                    color = White.copy(alpha = 0.5f), textAlign = TextAlign.Center, lineHeight = 18.sp
                )
            }
        }

        Box(Modifier.fillMaxSize().drawBehind { drawCornerBrackets(Scan.copy(alpha = bracket)) })

        when (phase) {
            Phase.POSITIONING -> Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(14.dp)
                    .clip(CircleShape)
                    .background(CamDarker.copy(alpha = 0.72f))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    "Frame the cassette and its well",
                    fontFamily = BodyFont, fontSize = 12.sp, color = White.copy(alpha = 0.85f)
                )
            }

            Phase.ANALYZING -> Box(
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(CamDarker.copy(alpha = 0.55f))
                    .drawBehind {
                        drawArc(
                            Scan, startAngle = spin, sweepAngle = 90f, useCenter = false,
                            style = Stroke(width = 5.dp.toPx())
                        )
                    }
            )

            Phase.PASS -> Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(120.dp).scale(ringScale).clip(CircleShape)
                        .background(Green.copy(alpha = ringAlpha))
                )
                Box(
                    Modifier.size(96.dp).clip(CircleShape).background(Green),
                    contentAlignment = Alignment.Center
                ) { Text("✓", color = White, fontSize = 50.sp, fontWeight = FontWeight.Bold) }
            }

            Phase.RETRY -> Box(
                Modifier.size(88.dp).clip(CircleShape).background(Amber),
                contentAlignment = Alignment.Center
            ) { Text("↻", color = White, fontSize = 42.sp, fontWeight = FontWeight.Bold) }
        }

        if (phase == Phase.ANALYZING) {
            Text(
                stageTitle,
                modifier = Modifier.align(Alignment.TopCenter).padding(14.dp),
                fontFamily = BodyFont, fontSize = 11.sp,
                fontWeight = FontWeight.Bold, color = White.copy(alpha = 0.6f)
            )
        }
    }
}

private fun DrawScope.drawCornerBrackets(color: Color) {
    val s = 30.dp.toPx()
    val pad = 16.dp.toPx()
    val w = 3.dp.toPx()
    val right = size.width - pad
    val bottom = size.height - pad
    drawLine(color, Offset(pad, pad), Offset(pad + s, pad), w)
    drawLine(color, Offset(pad, pad), Offset(pad, pad + s), w)
    drawLine(color, Offset(right, pad), Offset(right - s, pad), w)
    drawLine(color, Offset(right, pad), Offset(right, pad + s), w)
    drawLine(color, Offset(pad, bottom), Offset(pad + s, bottom), w)
    drawLine(color, Offset(pad, bottom), Offset(pad, bottom - s), w)
    drawLine(color, Offset(right, bottom), Offset(right - s, bottom), w)
    drawLine(color, Offset(right, bottom), Offset(right, bottom - s), w)
}

@Composable
private fun SteadyHint(cameraOn: Boolean) {
    val t = rememberInfiniteTransition(label = "dots")
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            val a by t.animateFloat(
                0.3f, 1f,
                infiniteRepeatable(tween(1000, delayMillis = i * 200), RepeatMode.Reverse), label = "d$i"
            )
            Box(Modifier.padding(horizontal = 3.dp).size(8.dp).clip(CircleShape).background(Scan.copy(alpha = a)))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            if (cameraOn) "Keep the phone steady" else "Moving on…",
            fontFamily = BodyFont, fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold, color = White.copy(alpha = 0.72f)
        )
    }
}

@Composable
private fun RetryCard(reason: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = AmberSoft
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(Amber),
                contentAlignment = Alignment.Center
            ) { Text("!", color = White, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            Spacer(Modifier.width(12.dp))
            Text(reason, fontFamily = BodyFont, fontSize = 14.sp, color = AmberInk, lineHeight = 21.sp)
        }
    }
}
