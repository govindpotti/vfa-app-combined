package com.vfa.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.vfa.app.ui.theme.BodyFont
import com.vfa.app.ui.theme.Muted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// ─────────────────────────────────────────────────────────────────────────────
// The cassette, in 3D, on the landing screen.
//
// This renders the real print geometry — the same VFAcomb.stl the cassette is made
// from — so the user can turn the actual device over before they hold one.
//
// It is drawn in software: the STL is parsed into flat arrays, transformed, and
// rasterised with a z-buffer into a Bitmap that Compose draws like any other image.
// That sounds like the long way round, and it is — the guided app did this with
// Three.js in a WebView, which is less code. But a WebView inside the Compose tree
// never composited on the emulator (not even a plain coloured page), and twice took
// the emulator's GPU stack down with it. Drawing through the same path as the rest
// of the UI has no such dependency: if the app renders at all, the cassette renders.
// ─────────────────────────────────────────────────────────────────────────────

private const val TAG = "VfaCassette3D"
private const val ASSET = "VFAcomb.stl"

/**
 * Longest edge of the render buffer. Capped so fill cost doesn't scale with screen
 * density; the result is scaled up to the card, which at this size is imperceptible.
 *
 * [MIN_EDGE] and [SLOW_FRAME_MS] are the fallback: the target phones are old, and a
 * software rasteriser is the one thing here that scales with CPU. Rather than guess
 * a device tier, the loop times itself and steps down once if it can't keep up.
 */
private const val MAX_EDGE = 460
private const val MIN_EDGE = 260
private const val SLOW_FRAME_MS = 55L

/** ~30 fps when the phone can manage it. */
private const val FRAME_MS = 33L
private const val FRAME_SPIN = 0.0105f

/**
 * Centred triangle soup. [v] is 9 floats per triangle, [n] is 3. [radius] is the
 * bounding-sphere radius — framing off that, rather than the bounding box, is what
 * keeps the model inside the card at every angle it turns through.
 */
private class Mesh(val v: FloatArray, val n: FloatArray, val tris: Int, val radius: Float)

/** Parsed once per process — 75k triangles is not something to re-read on every visit. */
private object MeshCache {
    @Volatile
    var mesh: Mesh? = null

    @Volatile
    var failed = false
}

private suspend fun loadMesh(context: Context): Mesh? = withContext(Dispatchers.IO) {
    MeshCache.mesh?.let { return@withContext it }
    if (MeshCache.failed) return@withContext null
    val started = System.nanoTime()
    try {
        val bytes = context.assets.open(ASSET).use { it.readBytes() }
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val tris = buf.getInt(80)
        // 80-byte header + uint32 count + 50 bytes per facet.
        if (tris <= 0 || bytes.size < 84 + tris.toLong() * 50) {
            Log.w(TAG, "unexpected STL size: $tris facets in ${bytes.size} bytes")
            MeshCache.failed = true
            return@withContext null
        }

        val v = FloatArray(tris * 9)
        val n = FloatArray(tris * 3)
        var lo = Float.MAX_VALUE
        var hi = -Float.MAX_VALUE
        var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        var minZ = Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE

        for (i in 0 until tris) {
            var o = 84 + i * 50
            n[i * 3] = buf.getFloat(o)
            n[i * 3 + 1] = buf.getFloat(o + 4)
            n[i * 3 + 2] = buf.getFloat(o + 8)
            o += 12
            for (k in 0 until 9) {
                val f = buf.getFloat(o + k * 4)
                v[i * 9 + k] = f
            }
            for (k in 0 until 3) {
                val x = v[i * 9 + k * 3]
                val y = v[i * 9 + k * 3 + 1]
                val z = v[i * 9 + k * 3 + 2]
                if (x < minX) minX = x; if (x > maxX) maxX = x
                if (y < minY) minY = y; if (y > maxY) maxY = y
                if (z < minZ) minZ = z; if (z > maxZ) maxZ = z
            }
        }
        lo = min(min(minX, minY), minZ)
        hi = max(max(maxX, maxY), maxZ)
        if (!(hi > lo)) {
            MeshCache.failed = true
            return@withContext null
        }

        // Centre on the origin and scale so the longest axis spans 2 units, so the
        // camera framing below is independent of the model's units.
        val cx = (minX + maxX) / 2f
        val cy = (minY + maxY) / 2f
        val cz = (minZ + maxZ) / 2f
        val span = max(max(maxX - minX, maxY - minY), maxZ - minZ)
        val s = 2f / span
        var r2 = 0f
        for (i in v.indices step 3) {
            val x = (v[i] - cx) * s
            val y = (v[i + 1] - cy) * s
            val z = (v[i + 2] - cz) * s
            v[i] = x; v[i + 1] = y; v[i + 2] = z
            val d = x * x + y * y + z * z
            if (d > r2) r2 = d
        }
        val radius = max(0.001f, sqrt(r2))

        Log.d(TAG, "parsed $tris facets in ${(System.nanoTime() - started) / 1_000_000}ms")
        Mesh(v, n, tris, radius).also { MeshCache.mesh = it }
    } catch (e: Exception) {
        Log.w(TAG, "could not load $ASSET", e)
        MeshCache.failed = true
        null
    }
}

/**
 * Software rasteriser. Reusable buffers — one instance renders every frame, so a
 * spinning model allocates nothing after the first.
 */
private class Rasterizer(val w: Int, val h: Int) {
    val pixels = IntArray(w * h)
    private val depth = FloatArray(w * h)

    // Warm plastic grey-green, lit from the upper left.
    private val baseR = 198
    private val baseG = 208
    private val baseB = 202
    private val lx = -0.34f
    private val ly = 0.56f
    private val lz = 0.76f

    fun render(mesh: Mesh, yaw: Float, pitch: Float) {
        java.util.Arrays.fill(pixels, 0)
        java.util.Arrays.fill(depth, -Float.MAX_VALUE)

        // The STL is Z-up; tilt it a quarter turn so the well stands upright, then
        // apply the user's pitch and yaw.
        val rx = pitch - (Math.PI / 2).toFloat()
        val cx = cos(rx); val sx = sin(rx)
        val cy = cos(yaw); val sy = sin(yaw)

        val halfW = w / 2f
        val halfH = h * 0.46f
        // Fit the bounding sphere with a little breathing room, so no rotation clips.
        val scale = min(w, h) * 0.46f / mesh.radius

        val v = mesh.v
        val n = mesh.n

        for (t in 0 until mesh.tris) {
            // Face normal first — it culls ~half the triangles before any transform.
            val n0 = n[t * 3]; val n1 = n[t * 3 + 1]; val n2 = n[t * 3 + 2]
            val ny = n1 * cx - n2 * sx
            val nz0 = n1 * sx + n2 * cx
            val nz = -n0 * sy + nz0 * cy
            if (nz <= 0f) continue
            val nx = n0 * cy + nz0 * sy

            val o = t * 9
            var ax = 0f; var ay = 0f; var az = 0f
            var bx = 0f; var by = 0f; var bz = 0f
            var ccx = 0f; var ccy = 0f; var cz = 0f

            for (k in 0 until 3) {
                val px = v[o + k * 3]
                val py = v[o + k * 3 + 1]
                val pz = v[o + k * 3 + 2]
                val y1 = py * cx - pz * sx
                val z1 = py * sx + pz * cx
                val x2 = px * cy + z1 * sy
                val z2 = -px * sy + z1 * cy
                val sxp = halfW + x2 * scale
                val syp = halfH - y1 * scale
                when (k) {
                    0 -> { ax = sxp; ay = syp; az = z2 }
                    1 -> { bx = sxp; by = syp; bz = z2 }
                    else -> { ccx = sxp; ccy = syp; cz = z2 }
                }
            }

            // Flat shading from the rotated normal.
            val len = sqrt(nx * nx + ny * ny + nz * nz)
            val d = if (len > 0f) (nx * lx + ny * ly + nz * lz) / len else 0f
            val lit = 0.34f + 0.66f * max(0f, d)
            val col = (0xFF shl 24) or
                (min(255, (baseR * lit).toInt()) shl 16) or
                (min(255, (baseG * lit).toInt()) shl 8) or
                min(255, (baseB * lit).toInt())

            fillTriangle(ax, ay, az, bx, by, bz, ccx, ccy, cz, col)
        }
    }

    private fun fillTriangle(
        ax: Float, ay: Float, az: Float,
        bx0: Float, by0: Float, bz0: Float,
        cx0: Float, cy0: Float, cz0: Float,
        color: Int,
    ) {
        // Screen y runs downwards, so winding flips; normalise it rather than guess.
        var bx = bx0; var by = by0; var bz = bz0
        var cx = cx0; var cy = cy0; var cz = cz0
        var area = (bx - ax) * (cy - ay) - (by - ay) * (cx - ax)
        if (area == 0f || area.isNaN()) return
        if (area < 0f) {
            var tmp = bx; bx = cx; cx = tmp
            tmp = by; by = cy; cy = tmp
            tmp = bz; bz = cz; cz = tmp
            area = -area
        }

        val minX = max(0, min(ax, min(bx, cx)).toInt())
        val maxX = min(w - 1, (max(ax, max(bx, cx)) + 1f).toInt())
        val minY = max(0, min(ay, min(by, cy)).toInt())
        val maxY = min(h - 1, (max(ay, max(by, cy)) + 1f).toInt())
        if (minX > maxX || minY > maxY) return

        val inv = 1f / area
        for (py in minY..maxY) {
            val fy = py + 0.5f
            var idx = py * w + minX
            for (px in minX..maxX) {
                val fx = px + 0.5f
                val w0 = (bx - ax) * (fy - ay) - (by - ay) * (fx - ax)
                val w1 = (cx - bx) * (fy - by) - (cy - by) * (fx - bx)
                val w2 = (ax - cx) * (fy - cy) - (ay - cy) * (fx - cx)
                if (w0 >= 0f && w1 >= 0f && w2 >= 0f) {
                    // Barycentric weights: w1 belongs to a, w2 to b, w0 to c.
                    val z = (w1 * az + w2 * bz + w0 * cz) * inv
                    if (z > depth[idx]) {
                        depth[idx] = z
                        pixels[idx] = color
                    }
                }
                idx++
            }
        }
    }
}

/**
 * The rotating cassette. Auto-turns slowly; drag to spin it yourself.
 *
 * [fallback] is shown if the geometry can't be read at all — so a device that somehow
 * can't load the asset still gets a hero rather than an apology.
 */
@Composable
fun Cassette3D(
    modifier: Modifier = Modifier,
    fallback: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current
    val mesh by produceState<Mesh?>(initialValue = MeshCache.mesh) {
        if (value == null) value = loadMesh(context)
    }

    var yaw by remember { mutableStateOf(0.6f) }
    var pitch by remember { mutableStateOf(0.22f) }
    var dragging by remember { mutableStateOf(false) }
    var frame by remember { mutableStateOf<Bitmap?>(null) }
    // Measured in the layout pass, never written from draw — a snapshot write during
    // draw re-invalidates the frame it is drawing.
    var box by remember { mutableStateOf(IntSize.Zero) }

    val paint = remember { Paint().apply { isFilterBitmap = true; isAntiAlias = true } }

    LaunchedEffect(mesh, box) {
        val m = mesh ?: return@LaunchedEffect
        if (box.width <= 0 || box.height <= 0) return@LaunchedEffect

        // Match the card's aspect ratio — a square buffer stretched to a wide card
        // squashes the model.
        val longest = max(box.width, box.height)
        val k = min(1f, MAX_EDGE.toFloat() / longest)
        val w = max(2, (box.width * k).toInt())
        val h = max(2, (box.height * k).toInt())
        val raster = Rasterizer(w, h)
        // Two bitmaps: one on screen, one being written into.
        val buffers = Array(2) { Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888) }
        var next = 0
        var logged = false

        while (isActive) {
            val start = System.nanoTime()
            val bmp = buffers[next]
            // Everything heavy stays off the main thread — the rasterisation *and* the
            // pixel upload. Only the one-word handoff below happens on it.
            withContext(Dispatchers.Default) {
                raster.render(m, yaw, pitch)
                bmp.setPixels(raster.pixels, 0, w, 0, 0, w, h)
            }
            frame = bmp
            next = 1 - next

            if (!dragging) yaw += FRAME_SPIN
            val elapsedMs = (System.nanoTime() - start) / 1_000_000
            if (!logged) {
                Log.d(TAG, "first frame ${w}x$h, ${m.tris} triangles, ${elapsedMs}ms")
                logged = true
            }
            delay(max(0L, FRAME_MS - elapsedMs))
        }
    }

    Box(
        modifier
            .onSizeChanged { box = it }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                ) { change, drag ->
                    change.consume()
                    yaw += drag.x * 0.008f
                    pitch = (pitch + drag.y * 0.008f).coerceIn(-1.2f, 1.2f)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val bmp = frame ?: return@Canvas
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawBitmap(
                    bmp,
                    null,
                    Rect(0, 0, size.width.toInt(), size.height.toInt()),
                    paint
                )
            }
        }

        if (frame == null) {
            if (MeshCache.failed && fallback != null) {
                fallback()
            } else {
                Text(
                    if (MeshCache.failed) "3D model unavailable" else "Loading the model…",
                    fontFamily = BodyFont,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Muted
                )
            }
        }
    }
}
