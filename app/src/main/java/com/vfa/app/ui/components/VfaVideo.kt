package com.vfa.app.ui.components

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.annotation.RawRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.vfa.app.ui.theme.CamDark

/**
 * Inline demonstration clip. Loops silently — the voice guide carries the audio, and a
 * looping clip means the user can watch it as many times as they need without a control
 * to find or a decision to make.
 *
 * The frame is centre-cropped so a portrait clip still fills a landscape card.
 */
@Composable
fun VfaVideo(
    @RawRes res: Int,
    modifier: Modifier = Modifier,
    cropOffsetX: Float = 0f,
    cropOffsetY: Float = 0f,
) {
    val player = remember(res) { MediaPlayer() }
    val lifecycleOwner = LocalLifecycleOwner.current
    var prepared by remember(res) { mutableStateOf(false) }

    DisposableEffect(res) {
        onDispose {
            prepared = false
            runCatching { player.release() }
        }
    }

    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (prepared) runCatching { player.start() }
                Lifecycle.Event.ON_STOP -> runCatching {
                    if (player.isPlaying) player.pause()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier.clip(RoundedCornerShape(18.dp)).background(CamDark)) {
        // Keyed on the clip: the surface listener binds one player to one raw resource
        // when it is first created, so switching clips has to build a new TextureView
        // rather than recycle the one still wired to the previous video.
        key(res) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    TextureView(ctx).apply {
                        var surface: Surface? = null
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                                prepared = false
                                runCatching {
                                    player.reset()
                                    surface?.release()
                                    surface = Surface(st)
                                    player.setSurface(surface)
                                    ctx.resources.openRawResourceFd(res).use { fd ->
                                        player.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                                    }
                                    player.setOnPreparedListener { mp ->
                                        prepared = true
                                        mp.isLooping = true
                                        mp.setVolume(0f, 0f)
                                        applyCenterCrop(
                                            mp.videoWidth,
                                            mp.videoHeight,
                                            w,
                                            h,
                                            cropOffsetX,
                                            cropOffsetY
                                        )
                                        mp.start()
                                    }
                                    player.prepareAsync()
                                }.onFailure {
                                    prepared = false
                                    runCatching { player.reset() }
                                    surface?.release()
                                    surface = null
                                }
                            }

                            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {
                                runCatching {
                                    applyCenterCrop(
                                        player.videoWidth,
                                        player.videoHeight,
                                        w,
                                        h,
                                        cropOffsetX,
                                        cropOffsetY
                                    )
                                }
                            }

                            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                                prepared = false
                                runCatching {
                                    player.setOnPreparedListener(null)
                                    player.reset()
                                    player.setSurface(null)
                                }
                                surface?.release()
                                surface = null
                                return true
                            }

                            override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
                        }
                    }
                }
            )
        }
    }
}

private fun TextureView.applyCenterCrop(
    videoW: Int,
    videoH: Int,
    viewW: Int,
    viewH: Int,
    cropOffsetX: Float,
    cropOffsetY: Float,
) {
    if (videoW <= 0 || videoH <= 0 || viewW <= 0 || viewH <= 0) return
    val scaleX = viewW.toFloat() / videoW
    val scaleY = viewH.toFloat() / videoH
    val max = maxOf(scaleX, scaleY)
    setTransform(
        Matrix().apply {
            setScale(max / scaleX, max / scaleY, viewW / 2f, viewH / 2f)
            postTranslate(viewW * cropOffsetX, viewH * cropOffsetY)
        }
    )
}
