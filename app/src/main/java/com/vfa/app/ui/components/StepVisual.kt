package com.vfa.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vfa.app.protocol.Clip
import com.vfa.app.ui.theme.*

/**
 * The visual for one step.
 *
 * The clips are the animation: Blender renders of the real cassette from the guided
 * app, plus the filmed demonstrations from VFA_App_Real. They loop silently and
 * forever, so the user can watch a step as many times as they need without a control
 * to find. Where a step has both a render and footage, chips switch between them —
 * the render shows the mechanics cleanly, the footage shows a real hand doing it.
 */
@Composable
fun StepVisual(
    clips: List<Clip>,
    modifier: Modifier = Modifier,
    height: Dp = 232.dp,
) {
    if (clips.isEmpty()) return

    var selected by remember(clips) { mutableIntStateOf(0) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = White,
        shadowElevation = 2.dp
    ) {
        Column(Modifier.padding(10.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(height)
                    .clip(RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                VfaVideo(clips[selected.coerceIn(clips.indices)].res, Modifier.fillMaxSize())
            }

            // Only worth a switch when there is something to switch between.
            if (clips.size > 1) {
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    clips.forEachIndexed { i, clip ->
                        VisualChip(clip.label, selected == i, Modifier.weight(1f)) { selected = i }
                    }
                }
            }
        }
    }
}

@Composable
private fun VisualChip(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(if (active) Coral else White, label = "chipBg")
    val fg by animateColorAsState(if (active) White else Navy, label = "chipFg")
    Surface(
        modifier = modifier.height(36.dp).clickable(onClick = onClick),
        shape = CircleShape,
        color = bg,
        border = if (active) null else BorderStroke(1.5.dp, Line)
    ) {
        Row(
            Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (active) {
                Box(Modifier.size(5.dp).clip(CircleShape).background(White.copy(alpha = 0.85f)))
                Spacer(Modifier.width(6.dp))
            }
            Text(
                label,
                fontFamily = BodyFont,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = fg,
                maxLines = 1
            )
        }
    }
}
