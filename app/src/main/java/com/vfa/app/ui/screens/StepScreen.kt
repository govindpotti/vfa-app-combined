package com.vfa.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vfa.app.protocol.Stage
import com.vfa.app.protocol.spokenGuidance
import com.vfa.app.ui.components.*
import com.vfa.app.ui.theme.*

/**
 * One instruction. One decision.
 *
 * Every hands-on stage of the protocol renders through here: the clip, the action to
 * take, the detail one tap away, and a single button. Reagent stages end with
 * "Done — check this step", which opens the camera; set-up stages just continue.
 */
@Composable
fun StepScreen(
    stage: Stage,
    stageNumber: Int,
    stageTotal: Int,
    onAction: () -> Unit,
) {
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

            Column(Modifier.padding(top = 16.dp, start = 24.dp, end = 24.dp)) {
                StepVisual(
                    clips = stage.clips,
                    still = stage.still,
                    playSequence = stage.playClipsInSequence
                )

                Spacer(Modifier.height(18.dp))
                Text(
                    stage.title,
                    fontFamily = TitleFont, fontSize = 30.sp, fontWeight = FontWeight.Bold,
                    color = Navy, lineHeight = 34.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stage.instruction,
                    fontFamily = BodyFont, fontSize = 16.sp, color = Navy, lineHeight = 24.sp
                )

                Spacer(Modifier.height(14.dp))
                SpokenSubtitle(
                    text = stage.cue,
                    spokenText = stage.spokenGuidance(stageNumber, stageTotal)
                )

                Spacer(Modifier.height(14.dp))
                HelpAccordion(stage.help)

                Spacer(Modifier.height(24.dp))
                CTAButton(
                    label = if (stage.checkpoint != null) "Done — check this step" else "Continue",
                    onClick = onAction
                )
            }
        }
    }
}
