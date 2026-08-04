package com.vfa.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vfa.app.protocol.TestType
import com.vfa.app.ui.components.*
import com.vfa.app.ui.theme.*

/**
 * Which test this cassette is for, and who it's for. The steps are identical either
 * way — the choice only changes which antibodies the result is about.
 */
@Composable
fun TestSelectScreen(
    selected: TestType?,
    onSelect: (TestType) -> Unit,
    patientLabel: String,
    onPatientLabelChange: (String) -> Unit,
    onContinue: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        ScreenBlobs()

        ScreenWrapper {
            VfaHeader()
            BigTitle(
                title = "Which ",
                accent = "test?",
                sub = "Choose the test this cassette is for."
            )

            Column(
                Modifier.padding(top = 22.dp, start = 24.dp, end = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                TestType.entries.forEach { test ->
                    TestCard(
                        test = test,
                        selected = selected == test,
                        accent = if (test == TestType.LYME) Coral else Terracotta,
                        soft = if (test == TestType.LYME) CoralSoft else TerracottaSoft,
                        onClick = { onSelect(test) }
                    )
                }

                Spacer(Modifier.height(6.dp))
                PatientField(patientLabel, onPatientLabelChange)

                Spacer(Modifier.height(14.dp))
                CTAButton(
                    label = "Continue",
                    onClick = onContinue,
                    disabled = selected == null,
                    disabledLabel = "Choose a test to continue"
                )
            }
        }
    }
}

/**
 * Who this test is for.
 *
 * A clinic runs these back to back, so the result screen needs to say whose it is. It
 * stays optional and unstructured — whatever the clinician already writes on the tube
 * is the right thing to type here — and it only travels to the result screen.
 */
@Composable
private fun PatientField(value: String, onValueChange: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = White,
        border = BorderStroke(1.dp, Line)
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Text(
                "PATIENT",
                fontFamily = BodyFont, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                color = Muted, letterSpacing = 1.2.sp
            )
            Spacer(Modifier.height(8.dp))
            Box {
                BasicTextField(
                    value = value,
                    onValueChange = { onValueChange(it.take(32)) },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = BodyFont, fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold, color = Navy
                    ),
                    cursorBrush = SolidColor(Coral),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (value.isEmpty()) {
                    Text(
                        "Optional — name or ID",
                        fontFamily = BodyFont, fontSize = 15.sp, color = Muted
                    )
                }
            }
        }
    }
}

@Composable
private fun TestCard(
    test: TestType,
    selected: Boolean,
    accent: Color,
    soft: Color,
    onClick: () -> Unit,
) {
    val border by animateColorAsState(if (selected) accent else Line, label = "bd")
    val bg by animateColorAsState(if (selected) soft else White, label = "bg")

    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = bg,
        shadowElevation = if (selected) 0.dp else 2.dp,
        border = BorderStroke(if (selected) 2.dp else 1.dp, border)
    ) {
        Row(
            Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                // The tile flips to white once selected, otherwise it disappears into
                // the card's own tint.
                Modifier.size(50.dp).clip(RoundedCornerShape(15.dp))
                    .background(if (selected) White else soft),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .border(3.dp, accent, CircleShape)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    test.displayName,
                    fontFamily = BodyFont, fontSize = 17.sp,
                    fontWeight = FontWeight.Bold, color = Navy
                )
                Text(
                    test.tagline,
                    fontFamily = BodyFont, fontSize = 13.sp, color = Muted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Box(
                Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (selected) accent else Color.Transparent)
                    .border(2.dp, if (selected) accent else Ring, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (selected) Box(Modifier.size(9.dp).clip(CircleShape).background(White))
            }
        }
    }
}
