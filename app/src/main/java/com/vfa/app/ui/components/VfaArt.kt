package com.vfa.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vfa.app.protocol.KitItem
import com.vfa.app.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// Kit artwork.
//
// Most items are photographs of the real hardware, carried over from VFA_App_Real —
// a photo of the actual bottom case is worth more to someone hunting through a box
// than any icon. Anything without a photo gets a drawn emblem in the same palette.
//
// Anything needed in multiples is drawn as a stack with a count badge — a run needs
// two top cases, and one picture of one top case does not say that.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun KitArt(item: KitItem, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        val photo = item.photo
        if (photo != null) {
            // Items needed in multiples are drawn as two separate copies rather
            // than one nudged behind another: these photos carry a lot of white
            // margin, so a small offset moves the object barely at all and the
            // second one simply vanishes.
            if (item.quantity > 1) {
                Image(
                    painter = painterResource(photo),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize(0.62f)
                        .align(Alignment.TopStart)
                        .alpha(0.92f),
                    contentScale = ContentScale.Fit
                )
                Image(
                    painter = painterResource(photo),
                    contentDescription = item.name,
                    modifier = Modifier
                        .fillMaxSize(0.62f)
                        .align(Alignment.BottomEnd),
                    contentScale = ContentScale.Fit
                )
            } else {
                Image(
                    painter = painterResource(photo),
                    contentDescription = item.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(item.photoScale),
                    contentScale = ContentScale.Fit
                )
            }
        } else {
            when (item.emblem) {
                KitItem.Emblem.TUBE -> SampleTube(Modifier.fillMaxSize())
                KitItem.Emblem.GOLD_BOTTLE -> GoldBottle(Modifier.fillMaxSize())
                KitItem.Emblem.WIPES -> KimWipes(Modifier.fillMaxSize())
                null -> Unit
            }
        }

        if (item.quantity > 1) {
            Surface(
                modifier = Modifier.align(Alignment.TopEnd),
                shape = CircleShape,
                color = Coral
            ) {
                Text(
                    "×${item.quantity}",
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                    fontFamily = BodyFont,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )
            }
        }
    }
}

@Composable
private fun SampleTube(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val tubeW = w * 0.20f
        val cx = w / 2f
        val top = h * 0.24f
        val bottom = h * 0.80f
        val r = tubeW / 2f
        val body = Path().apply {
            moveTo(cx - r, top)
            lineTo(cx - r, bottom - r)
            cubicTo(cx - r, bottom + r * 0.6f, cx + r, bottom + r * 0.6f, cx + r, bottom - r)
            lineTo(cx + r, top)
            close()
        }
        drawPath(body, CassetteTop)
        // Sample liquid.
        val liquidTop = top + (bottom - top) * 0.42f
        val clip = Path().apply { addPath(body) }
        clipPath(clip) {
            drawRect(
                TintSample.copy(alpha = 0.55f),
                topLeft = Offset(cx - r, liquidTop),
                size = Size(tubeW, bottom - liquidTop + r)
            )
        }
        drawPath(body, CassetteStroke, style = Stroke(2.dp.toPx()))
        // Cap.
        drawRoundRect(
            TintSample,
            topLeft = Offset(cx - r * 1.25f, top - h * 0.10f),
            size = Size(tubeW * 1.25f, h * 0.11f),
            cornerRadius = CornerRadius(r * 0.5f, r * 0.5f)
        )
        drawLine(
            White.copy(alpha = 0.7f),
            Offset(cx - r * 0.45f, top + h * 0.05f),
            Offset(cx - r * 0.45f, bottom - h * 0.08f),
            strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round
        )
    }
}

@Composable
private fun GoldBottle(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val bodyW = w * 0.36f
        val bodyH = h * 0.44f
        val bodyT = h * 0.38f
        // Dropper cap.
        drawRoundRect(
            TintGold,
            topLeft = Offset(cx - bodyW * 0.24f, bodyT - h * 0.16f),
            size = Size(bodyW * 0.48f, h * 0.17f),
            cornerRadius = CornerRadius(w * 0.02f, w * 0.02f)
        )
        // Bottle.
        drawRoundRect(
            TintGold.copy(alpha = 0.30f),
            topLeft = Offset(cx - bodyW / 2f, bodyT),
            size = Size(bodyW, bodyH),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
        )
        drawRoundRect(
            TintGold,
            topLeft = Offset(cx - bodyW / 2f, bodyT + bodyH * 0.38f),
            size = Size(bodyW, bodyH * 0.62f),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
        )
        drawRoundRect(
            CassetteStroke,
            topLeft = Offset(cx - bodyW / 2f, bodyT),
            size = Size(bodyW, bodyH),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f),
            style = Stroke(2.dp.toPx())
        )
        // Label band.
        drawRoundRect(
            White.copy(alpha = 0.85f),
            topLeft = Offset(cx - bodyW * 0.40f, bodyT + bodyH * 0.44f),
            size = Size(bodyW * 0.80f, bodyH * 0.24f),
            cornerRadius = CornerRadius(w * 0.02f, w * 0.02f)
        )
        drawLine(
            White.copy(alpha = 0.6f),
            Offset(cx - bodyW * 0.30f, bodyT + bodyH * 0.10f),
            Offset(cx - bodyW * 0.30f, bodyT + bodyH * 0.32f),
            strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round
        )
    }
}

@Composable
private fun KimWipes(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val boxW = w * 0.52f
        val boxH = h * 0.34f
        val left = (w - boxW) / 2f
        val top = h * 0.44f
        // A tissue pulled up out of the box.
        val tissue = Path().apply {
            moveTo(left + boxW * 0.30f, top + 2f)
            lineTo(left + boxW * 0.18f, top - h * 0.20f)
            lineTo(left + boxW * 0.50f, top - h * 0.27f)
            lineTo(left + boxW * 0.82f, top - h * 0.17f)
            lineTo(left + boxW * 0.70f, top + 2f)
            close()
        }
        drawPath(tissue, White)
        drawPath(tissue, CassetteStroke, style = Stroke(2.dp.toPx()))
        drawRoundRect(
            TintWipe,
            topLeft = Offset(left, top),
            size = Size(boxW, boxH),
            cornerRadius = CornerRadius(w * 0.035f, w * 0.035f)
        )
        drawRoundRect(
            White.copy(alpha = 0.85f),
            topLeft = Offset(left + boxW * 0.10f, top + boxH * 0.42f),
            size = Size(boxW * 0.80f, boxH * 0.26f),
            cornerRadius = CornerRadius(w * 0.015f, w * 0.015f)
        )
        drawRoundRect(
            CassetteStroke,
            topLeft = Offset(left, top),
            size = Size(boxW, boxH),
            cornerRadius = CornerRadius(w * 0.035f, w * 0.035f),
            style = Stroke(2.dp.toPx())
        )
    }
}
