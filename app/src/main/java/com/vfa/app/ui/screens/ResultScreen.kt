package com.vfa.app.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vfa.app.backend.Readout
import com.vfa.app.backend.Verdict
import com.vfa.app.protocol.TestType
import com.vfa.app.ui.components.*
import com.vfa.app.ui.theme.*

/**
 * The result.
 *
 * Worded for the clinician who has to record it and decide what happens next: what the
 * test found, what that does and doesn't tell them, and what to do now. A positive
 * reads terracotta rather than alarm-red — this is a screening test, and a positive
 * means confirm before treating, not diagnose.
 *
 * When the analyzer actually ran, its numbers are shown alongside for the notes. When
 * it didn't run, the screen says so rather than letting a demo result pass for real.
 */
@Composable
fun ResultScreen(
    test: TestType?,
    patientLabel: String,
    verdict: Verdict,
    readout: Readout?,
    onStartNew: () -> Unit,
    onToggleDemo: () -> Unit,
) {
    val negative = verdict == Verdict.NEGATIVE
    val accent = if (negative) Green else Terracotta
    val soft = if (negative) GreenSoft else TerracottaSoft
    val antibodies = test?.antibodies ?: "these antibodies"

    val t = rememberInfiniteTransition(label = "res")
    val halo by t.animateFloat(
        1f, 1.08f,
        infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "halo"
    )

    Box(Modifier.fillMaxSize()) {
        ScreenBlobs()

        ScreenWrapper {
            VfaHeader()

            Column(
                Modifier.fillMaxWidth().padding(top = 22.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.size(112.dp).scale(halo).clip(CircleShape).background(soft)
                    )
                    Box(
                        Modifier.size(84.dp).clip(CircleShape).background(accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (negative) "✓" else "!",
                            color = White, fontSize = if (negative) 38.sp else 42.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    if (negative) "RESULT · NEGATIVE" else "RESULT · POSITIVE",
                    fontFamily = BodyFont, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                    color = accent, letterSpacing = 1.4.sp
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    if (negative) "No $antibodies found"
                    else "$antibodies found",
                    fontFamily = TitleFont, fontSize = 30.sp, fontWeight = FontWeight.Bold,
                    color = Navy, textAlign = TextAlign.Center, lineHeight = 35.sp
                )

                if (patientLabel.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Surface(shape = RoundedCornerShape(10.dp), color = CreamDeep) {
                        Text(
                            "PATIENT  $patientLabel",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontFamily = BodyFont, fontSize = 12.sp,
                            fontWeight = FontWeight.Bold, color = Navy, letterSpacing = 0.6.sp
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))
                HelpAccordion(
                    label = "What this means",
                    text = if (negative)
                        "The test didn't find $antibodies in this blood sample. Antibodies take " +
                            "time to appear, so a negative result doesn't rule out a recent " +
                            "infection.\n\n" +
                            "This is a screening test, not a diagnosis. Confirm before treating."
                    else
                        "The test found $antibodies in this blood sample. That doesn't confirm " +
                            "an active infection on its own — past exposure and cross-reaction " +
                            "with other infections both give a positive here.\n\n" +
                            "This is a screening test, not a diagnosis. Confirm before treating."
                )

                Spacer(Modifier.height(18.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = White,
                    shadowElevation = 3.dp
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text(
                            "WHAT TO DO NOW",
                            fontFamily = BodyFont, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                            color = Muted, letterSpacing = 1.2.sp
                        )
                        Spacer(Modifier.height(14.dp))
                        val steps = if (negative) listOf(
                            "Record the result against the patient.",
                            "If the symptoms point to a recent infection, test again later.",
                            "Put the cassette and used top case in biohazard waste."
                        ) else listOf(
                            "Record the result against the patient.",
                            "Send for a confirmatory test before starting treatment.",
                            "Put the cassette and used top case in biohazard waste."
                        )
                        steps.forEachIndexed { i, text ->
                            Row(verticalAlignment = Alignment.Top) {
                                Box(
                                    Modifier.size(26.dp).clip(CircleShape).background(soft),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${i + 1}",
                                        fontFamily = BodyFont, fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold, color = accent
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text,
                                    fontFamily = BodyFont, fontSize = 14.sp, color = Navy,
                                    lineHeight = 21.sp, modifier = Modifier.padding(top = 3.dp)
                                )
                            }
                            if (i < steps.size - 1) Spacer(Modifier.height(12.dp))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                MeasurementCard(readout)

                Spacer(Modifier.height(22.dp))
                CTAButton("Start a new test", onStartNew)
                Spacer(Modifier.height(6.dp))
                QuietLink(
                    if (negative) "Demo: show a positive result" else "Demo: show a negative result",
                    onToggleDemo
                )
            }
        }
    }
}

/**
 * What the analyzer actually measured, for the notes. Present only when the readout
 * service ran — otherwise the screen says plainly that nothing was measured.
 */
@Composable
private fun MeasurementCard(readout: Readout?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = CreamDeep.copy(alpha = 0.6f)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                if (readout != null) "MEASURED SIGNAL" else "SIMULATED RESULT — NOT MEASURED",
                fontFamily = BodyFont, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                color = Muted, letterSpacing = 1.2.sp
            )
            Spacer(Modifier.height(10.dp))
            if (readout != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Metric("Peak spot", "%.1f".format(readout.peak))
                    Metric("Background", "%.1f".format(readout.background))
                    Metric("Δ", "%.1f".format(readout.peak - readout.background))
                }
                if (readout.alignmentUncertain) {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = Ring)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "The corner markers were hard to find in this photo. Take the last " +
                            "photo again with the top-right corner lined up before recording " +
                            "this result.",
                        fontFamily = BodyFont, fontSize = 12.sp, color = AmberInk, lineHeight = 18.sp
                    )
                }
            } else {
                Text(
                    "No readout service is set up, so this is a demonstration — it was not " +
                        "measured from the membrane and must not be recorded. Deploy /server " +
                        "and set ANALYZER_URL to read the real signal.",
                    fontFamily = BodyFont, fontSize = 12.sp, color = Muted, lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontFamily = TitleFont, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Navy)
        Text(label, fontFamily = BodyFont, fontSize = 11.sp, color = Muted)
    }
}
