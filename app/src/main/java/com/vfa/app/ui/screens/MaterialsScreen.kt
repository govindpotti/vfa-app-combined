package com.vfa.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vfa.app.protocol.KitItem
import com.vfa.app.protocol.Protocol
import com.vfa.app.ui.components.*
import com.vfa.app.ui.theme.*

/**
 * What you need.
 *
 * The photo grid from VFA_App_Real, extended to the full eight-item kit the test needs.
 * The photographs earn their place on the printed parts — someone running this for the
 * first time can tell the top case from the bottom case at a glance, which no amount of
 * naming achieves. Checking each one off is deliberate: the steps are timed, so finding
 * a missing reagent halfway through costs the cassette.
 */
@Composable
fun MaterialsScreen(
    checked: List<Boolean>,
    onToggle: (Int) -> Unit,
    onContinue: () -> Unit,
    testName: String,
) {
    val items = Protocol.kit
    val found = checked.count { it }
    val allFound = found == items.size

    Box(Modifier.fillMaxSize()) {
        ScreenBlobs()

        ScreenWrapper {
            VfaHeader(
                trailing = {
                    Surface(shape = CircleShape, color = CoralSoft) {
                        Text(
                            testName,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontFamily = BodyFont, fontSize = 11.sp,
                            fontWeight = FontWeight.Bold, color = CoralDeep
                        )
                    }
                }
            )

            Row(
                Modifier.fillMaxWidth().padding(top = 20.dp, start = 24.dp, end = 24.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "What you need",
                    fontFamily = TitleFont, fontSize = 30.sp, fontWeight = FontWeight.Bold,
                    color = Navy, lineHeight = 34.sp, modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                CountPill("$found of ${items.size}", allFound)
            }

            Text(
                "Check each item is ready before you start.",
                modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp),
                fontFamily = BodyFont, fontSize = 14.sp, color = Muted, lineHeight = 21.sp
            )

            Column(
                Modifier.padding(top = 18.dp, start = 24.dp, end = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TipBox("Work on a clean, flat surface with good light. Keep the bottom case face-up — the membrane inside it is what gets read.")

                // The reader leads: it's the one piece that isn't standard clinic kit.
                MaterialCard(items[0], checked[0], artHeight = 200.dp) { onToggle(0) }

                // The rest in pairs, then the wipes across the bottom.
                for (row in 0 until 3) {
                    val a = 1 + row * 2
                    val b = a + 1
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MaterialCard(items[a], checked[a], Modifier.weight(1f)) { onToggle(a) }
                        MaterialCard(items[b], checked[b], Modifier.weight(1f)) { onToggle(b) }
                    }
                }
                MaterialCard(items[7], checked[7], artHeight = 130.dp) { onToggle(7) }

                Spacer(Modifier.height(14.dp))
                CTAButton(
                    label = "I have everything",
                    onClick = onContinue,
                    disabled = !allFound,
                    disabledLabel = "Check all ${items.size} items to continue"
                )
            }
        }
    }
}

@Composable
private fun MaterialCard(
    item: KitItem,
    checked: Boolean,
    modifier: Modifier = Modifier,
    artHeight: Dp = 148.dp,
    onClick: () -> Unit,
) {
    val border by animateColorAsState(if (checked) Green else Line, label = "bd")
    val tick by animateColorAsState(if (checked) Green else Ring, label = "tick")

    Surface(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = White,
        shadowElevation = 2.dp,
        border = BorderStroke(if (checked) 2.dp else 1.dp, border)
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(artHeight)
                    // The product photos have white backgrounds — sitting them on white
                    // keeps the cut-out invisible; the drawn emblems get the warm tile.
                    .background(
                        when {
                            checked -> GreenSoft.copy(alpha = 0.35f)
                            item.photo != null -> White
                            else -> CreamDeep.copy(alpha = 0.45f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                KitArt(item, Modifier.fillMaxSize().padding(12.dp))
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Box(
                    Modifier.size(22.dp).clip(CircleShape).background(tick),
                    contentAlignment = Alignment.Center
                ) {
                    if (checked) Text("✓", color = White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        item.name,
                        fontFamily = BodyFont, fontSize = 13.sp,
                        fontWeight = FontWeight.Bold, color = Navy, maxLines = 2
                    )
                    Text(
                        item.hint,
                        fontFamily = BodyFont, fontSize = 11.sp,
                        color = Muted, lineHeight = 15.sp, maxLines = 3
                    )
                }
            }
        }
    }
}
