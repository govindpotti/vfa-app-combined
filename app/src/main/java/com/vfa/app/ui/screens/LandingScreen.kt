package com.vfa.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vfa.app.protocol.Protocol
import com.vfa.app.ui.components.*
import com.vfa.app.ui.theme.*

/**
 * Landing.
 *
 * The VFA_App_Real opening — soft blobs, a hero card, a serif headline — with the real
 * cassette turning inside the card, and the two run settings under it.
 *
 * The two settings earn their place at the top of a clinic workflow: text size means
 * the screen is readable at arm's length across a bench, and language means a test
 * shipped to many clinics reads in the clinician's own. Set once, before the test
 * starts.
 */
@Composable
fun LandingScreen(
    textScale: TextScale,
    onTextScaleChange: (TextScale) -> Unit,
    language: String,
    onLanguageChange: (String) -> Unit,
    languages: List<String>,
    onStart: () -> Unit,
) {
    var langOpen by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        ScreenBlobs()

        ScreenWrapper {
            VfaHeader()

            Box(Modifier.padding(top = 22.dp, start = 24.dp, end = 24.dp)) {
                CassetteHero()
            }

            BigTitle(
                title = "Run the test, ",
                accent = "step by step.",
                sub = "Guided steps with a camera check on each one, and the same read every " +
                    "time. About 25 minutes."
            )

            Column(Modifier.padding(top = 22.dp, start = 24.dp, end = 24.dp)) {

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // — Text size —
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                            Text(
                                "TEXT SIZE",
                                fontFamily = BodyFont, fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold, color = Muted, letterSpacing = 1.2.sp
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                TextScale.entries.forEach { scale ->
                                    TextSizeChip(
                                        scale = scale,
                                        active = scale == textScale,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onTextScaleChange(scale) }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Line)

                        // — Language —
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { langOpen = !langOpen }
                                .padding(horizontal = 16.dp, vertical = 15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "LANGUAGE",
                                    fontFamily = BodyFont, fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold, color = Muted, letterSpacing = 1.2.sp
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    language,
                                    fontFamily = BodyFont, fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold, color = Navy
                                )
                            }
                            Chevron(langOpen)
                        }

                        if (langOpen) {
                            Column(Modifier.padding(bottom = 6.dp)) {
                                languages.forEach { name ->
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onLanguageChange(name)
                                                langOpen = false
                                            }
                                            .background(if (name == language) LavLight else White)
                                            .padding(horizontal = 16.dp, vertical = 13.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(name, fontFamily = BodyFont, fontSize = 15.sp, color = Navy)
                                        if (name == language) {
                                            Text("✓", color = Coral, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(26.dp))
                CTAButton("Start a test", onStart)
                Spacer(Modifier.height(14.dp))
                Text(
                    "For research use. A screening test, not a diagnosis.",
                    modifier = Modifier.fillMaxWidth(),
                    fontFamily = BodyFont,
                    fontSize = 11.sp,
                    color = Muted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

/**
 * The hero: your cassette, before you've touched it.
 *
 * This is the actual print geometry — VFAcomb.stl, the file the cassette is made from —
 * rendered live and turning, so the part can be seen from any angle before it's picked
 * up. Drag to turn it. If the geometry can't be read at all, the assembly clip stands in.
 */
@Composable
private fun CassetteHero() {
    Surface(
        modifier = Modifier.fillMaxWidth().height(228.dp),
        shape = RoundedCornerShape(24.dp),
        color = CreamDeep
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(White, CreamDeep))))

            Cassette3D(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                fallback = { VfaVideo(Protocol.heroClip.res, Modifier.fillMaxSize()) }
            )

            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .clip(CircleShape)
                    .background(CamDarker.copy(alpha = 0.55f))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    "Drag to turn the cassette",
                    fontFamily = BodyFont,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = White.copy(alpha = 0.92f)
                )
            }
        }
    }
}

@Composable
private fun Toggle(on: Boolean) {
    val track by animateColorAsState(if (on) Coral else Ring, label = "track")
    val knobOffset by animateDpAsState(if (on) 22.dp else 3.dp, label = "knob")
    Box(
        Modifier.width(48.dp).height(28.dp).clip(CircleShape).background(track)
    ) {
        Box(
            Modifier
                .padding(start = knobOffset, top = 3.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(White)
        )
    }
}

@Composable
private fun TextSizeChip(
    scale: TextScale,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        color = if (active) LavLight else White,
        border = BorderStroke(1.5.dp, if (active) LavMid else Line)
    ) {
        Column(
            Modifier.padding(vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "A",
                fontFamily = BodyFont,
                fontSize = scale.glyphSp.sp,
                fontWeight = FontWeight.Bold,
                color = if (active) LavDeep else Navy
            )
            Text(
                scale.label,
                fontFamily = BodyFont,
                fontSize = 10.sp,
                color = if (active) LavDeep else Muted
            )
        }
    }
}
