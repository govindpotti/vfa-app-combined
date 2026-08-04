package com.vfa.app.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vfa.app.protocol.Stage
import com.vfa.app.ui.components.*
import com.vfa.app.ui.theme.*
import kotlinx.coroutines.delay

/**
 * The two ten-minute waits.
 *
 * The guided app's breathing ring, in the warm palette. It starts on its own — there is
 * nothing to decide here — and it is deliberately readable across a room, because the
 * clinician will be doing something else and glancing back. The demo skip stays, clearly
 * labelled, for dry runs and training.
 */
@Composable
fun TimerScreen(
    stage: Stage,
    onDone: () -> Unit,
    stageNumber: Int,
    stageTotal: Int,
) {
    var remaining by remember(stage) { mutableIntStateOf(stage.seconds) }

    LaunchedEffect(stage) {
        while (remaining > 0) {
            delay(1000)
            remaining -= 1
        }
        onDone()
    }

    val progress by animateFloatAsState(
        if (stage.seconds > 0) 1f - remaining.toFloat() / stage.seconds else 0f,
        animationSpec = tween(900), label = "ring"
    )

    val t = rememberInfiniteTransition(label = "breathe")
    val breathe by t.animateFloat(
        0.92f, 1.06f,
        infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b"
    )

    Box(Modifier.fillMaxSize()) {
        ScreenBlobs()

        ScreenWrapper {
            VfaHeader()

            StepProgress(
                kicker = stage.kicker,
                current = stageNumber,
                total = stageTotal,
                modifier = Modifier.padding(top = 18.dp, start = 24.dp, end = 24.dp)
            )

            Column(
                Modifier.fillMaxWidth().padding(top = 20.dp, start = 26.dp, end = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stage.title,
                    fontFamily = TitleFont, fontSize = 30.sp, fontWeight = FontWeight.Bold,
                    color = Navy, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stage.instruction,
                    fontFamily = BodyFont, fontSize = 14.sp, color = Muted,
                    textAlign = TextAlign.Center, lineHeight = 21.sp
                )

                Spacer(Modifier.height(24.dp))

                Box(
                    Modifier.size(248.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val stroke = 14.dp.toPx()
                        val inset = stroke / 2f + 6.dp.toPx()
                        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)

                        // Breathing halo.
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(CoralSoft.copy(alpha = 0.75f), Color.Transparent),
                                center = center, radius = size.minDimension * 0.5f * breathe
                            ),
                            radius = size.minDimension * 0.48f * breathe
                        )
                        drawArc(
                            color = Ring,
                            startAngle = 0f, sweepAngle = 360f, useCenter = false,
                            topLeft = Offset(inset, inset), size = arcSize,
                            style = Stroke(stroke)
                        )
                        drawArc(
                            brush = Brush.sweepGradient(listOf(Coral, Lavender, Coral)),
                            startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
                            topLeft = Offset(inset, inset), size = arcSize,
                            style = Stroke(stroke, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "%d:%02d".format(remaining / 60, remaining % 60),
                            fontFamily = TitleFont, fontSize = 46.sp, fontWeight = FontWeight.Bold,
                            color = Navy
                        )
                        Text(
                            "REMAINING",
                            fontFamily = BodyFont, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                            color = Muted, letterSpacing = 1.4.sp
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(LavLight)
                        .padding(16.dp)
                ) {
                    Text(
                        "Leave the cassette flat and don't move it. You'll be told when the " +
                            "time is up.",
                        fontFamily = BodyFont, fontSize = 14.sp, color = LavDeep, lineHeight = 21.sp
                    )
                }

                Spacer(Modifier.height(14.dp))
                HelpAccordion(stage.help, label = "What's happening")

                Spacer(Modifier.height(18.dp))
                QuietLink("Skip the wait (demo only)", onDone)
            }
        }
    }
}
