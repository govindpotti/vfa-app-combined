package com.vfa.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vfa.app.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// The shared chrome. Visually this is the VFA_App_Real design — soft blobs on a
// warm ivory canvas, a serif headline with a coral accent, a full-width coral
// pill for the primary action. Structurally it borrows the guided app's habits:
// more detail one tap from every instruction, and amber (never red) for anything
// that has to be redone.
// ─────────────────────────────────────────────────────────────────────────────

/** Soft out-of-focus colour wash. Two or three per screen, behind everything. */
@Composable
fun Blob(modifier: Modifier = Modifier, color: Color, opacity: Float = 0.35f) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = opacity))
    )
}

/** The standard blob arrangement, so every screen feels like the same room. */
@Composable
fun BoxScope.ScreenBlobs() {
    Blob(Modifier.size(180.dp).offset(x = 250.dp, y = (-50).dp), Pink, 0.42f)
    Blob(Modifier.size(140.dp).align(Alignment.BottomStart).offset(x = (-55).dp, y = 70.dp), Lavender, 0.30f)
}

/** Scrolling page body with room for the bottom action. */
@Composable
fun ScreenWrapper(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 40.dp),
        content = content
    )
}

@Composable
fun VfaLogo(size: Dp = 22.dp) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Canvas(modifier = Modifier.size(size * 28f / 22f)) {
            val scale = size.toPx() / 22f
            drawCircle(Coral.copy(alpha = 0.92f), 5.5f * scale, Offset(14f * scale, 6f * scale))
            drawCircle(Color(0xFF6AAEDB).copy(alpha = 0.92f), 5.5f * scale, Offset(21f * scale, 19f * scale))
            drawCircle(Lavender.copy(alpha = 0.92f), 5.5f * scale, Offset(7f * scale, 19f * scale))
        }
        Column {
            Text(
                "VFA",
                fontFamily = BodyFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = (size.value * 0.9f).sp,
                color = Navy,
                letterSpacing = (-0.5).sp,
                lineHeight = (size.value * 1.0f).sp
            )
            Text(
                "DICARLO LAB",
                fontFamily = BodyFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = (size.value * 0.36f).sp,
                color = Muted,
                letterSpacing = 1.6.sp
            )
        }
    }
}

/** Page header: the mark on the left, anything the screen needs on the right. */
@Composable
fun VfaHeader(
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 14.dp, start = 24.dp, end = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        VfaLogo()
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            trailing?.invoke()
        }
    }
}

@Composable
fun PlayGlyph(color: Color, size: Dp = 12.dp) {
    Canvas(Modifier.size(size)) {
        val p = Path().apply {
            moveTo(0f, 0f)
            lineTo(this@Canvas.size.width, this@Canvas.size.height / 2f)
            lineTo(0f, this@Canvas.size.height)
            close()
        }
        drawPath(p, color)
    }
}

/**
 * Where the user is in the 14-stage protocol. The animated segment is the Real
 * design's step indicator; the caption above it is the stage's own kicker.
 */
@Composable
fun StepProgress(
    kicker: String,
    current: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                kicker,
                fontFamily = BodyFont,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Muted,
                letterSpacing = 1.2.sp
            )
            Text(
                "Step $current of $total",
                fontFamily = BodyFont,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Navy
            )
        }
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (i in 0 until total) {
                val active = i == current - 1
                val done = i < current - 1
                val weight by animateDpAsState(if (active) 22.dp else 8.dp, label = "seg")
                Box(
                    Modifier
                        .height(5.dp)
                        .then(if (active) Modifier.width(weight) else Modifier.weight(1f))
                        .clip(CircleShape)
                        .background(
                            when {
                                active -> Coral
                                done -> Coral.copy(alpha = 0.38f)
                                else -> Ring
                            }
                        )
                )
            }
        }
    }
}

/** Serif headline with a coral accent, plus an optional sub-line. */
@Composable
fun BigTitle(
    title: String,
    accent: String? = ".",
    sub: String? = null,
    modifier: Modifier = Modifier,
    size: Int = 34,
) {
    Column(modifier.padding(top = 20.dp, start = 24.dp, end = 24.dp)) {
        Text(
            text = buildAnnotatedString {
                append(title)
                if (accent != null) withStyle(SpanStyle(color = Coral)) { append(accent) }
            },
            fontFamily = TitleFont,
            fontSize = size.sp,
            fontWeight = FontWeight.Bold,
            color = Navy,
            lineHeight = (size * 1.13f).sp
        )
        if (sub != null) {
            Text(
                sub,
                modifier = Modifier.padding(top = 10.dp),
                fontFamily = BodyFont,
                fontSize = 14.sp,
                color = Muted,
                lineHeight = 21.sp
            )
        }
    }
}

/** The primary action. Full-width coral pill with a nudge arrow. */
@Composable
fun CTAButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    disabled: Boolean = false,
    disabledLabel: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    val pressed by interaction.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        if (pressed && !disabled) 0.985f else 1f, label = "press"
    )
    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        enabled = !disabled,
        interactionSource = interaction,
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .scale(scale),
        colors = ButtonDefaults.buttonColors(
            containerColor = Coral,
            contentColor = White,
            disabledContainerColor = Color(0xFFDDD9E2),
            disabledContentColor = Color(0xFF9A94A6)
        ),
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (disabled && disabledLabel != null) disabledLabel else label,
                fontFamily = BodyFont,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.1.sp
            )
            if (!disabled) Text("→", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Recover / try again. Amber, warm, never alarming. */
@Composable
fun AmberButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier.fillMaxWidth().height(54.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = White),
        shape = CircleShape
    ) {
        Text(label, fontFamily = BodyFont, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

/** Quiet secondary action. */
@Composable
fun GhostButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        shape = CircleShape,
        color = White,
        border = BorderStroke(1.5.dp, Line)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, fontFamily = BodyFont, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy)
        }
    }
}

/** Underlined text action for the low-stakes escape hatches ("Skip the wait"). */
@Composable
fun QuietLink(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    Text(
        label,
        modifier = modifier
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        fontFamily = BodyFont,
        fontSize = 13.sp,
        color = Muted,
        textAlign = TextAlign.Center,
        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
    )
}

/** Lavender advice panel. */
@Composable
fun TipBox(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LavLight)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(LavMid),
            contentAlignment = Alignment.Center
        ) { Text("!", color = White, fontWeight = FontWeight.Bold, fontSize = 17.sp) }
        Text(text, fontFamily = BodyFont, fontSize = 13.sp, color = Navy, lineHeight = 20.sp)
    }
}

/**
 * One-tap expansion for the detail behind a step. Kept collapsed so the screen stays a
 * single instruction — someone who has done this before shouldn't have to read past what
 * they already know — but always one tap from the detail that changes the result.
 */
@Composable
fun HelpAccordion(
    text: String,
    modifier: Modifier = Modifier,
    label: String = "More detail",
) {
    var open by remember(text) { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = White,
        border = BorderStroke(1.5.dp, Line)
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        open = !open
                    }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    Box(
                        Modifier.size(26.dp).clip(CircleShape).background(LavLight),
                        contentAlignment = Alignment.Center
                    ) { Text("?", color = LavDeep, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                    Text(label, fontFamily = BodyFont, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Navy)
                }
                Chevron(open)
            }
            if (open) {
                Text(
                    text,
                    modifier = Modifier.padding(start = 51.dp, end = 16.dp, bottom = 16.dp),
                    fontFamily = BodyFont,
                    fontSize = 14.sp,
                    color = Muted,
                    lineHeight = 21.sp
                )
            }
        }
    }
}

@Composable
fun Chevron(open: Boolean) {
    val angle by androidx.compose.animation.core.animateFloatAsState(
        if (open) 225f else 45f, label = "chev"
    )
    Canvas(Modifier.size(10.dp).rotate(angle)) {
        val w = 2.dp.toPx()
        drawLine(Muted, Offset(size.width, 0f), Offset(size.width, size.height), w)
        drawLine(Muted, Offset(0f, size.height), Offset(size.width, size.height), w)
    }
}

/** Small status pill, e.g. "6 of 8 found". */
@Composable
fun CountPill(text: String, complete: Boolean) {
    val bg by animateColorAsState(if (complete) GreenSoft else CreamDeep, label = "pill")
    val fg = if (complete) GreenDeep else Muted
    Surface(shape = CircleShape, color = bg) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp),
            fontFamily = BodyFont,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}
