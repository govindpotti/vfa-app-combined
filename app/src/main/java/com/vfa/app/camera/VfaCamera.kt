package com.vfa.app.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// ─────────────────────────────────────────────────────────────────────────────
// The live camera behind every checkpoint and both reader photos.
//
// The guided app's checkpoint screens described a viewfinder; this actually opens
// one. A single [VfaCameraState] owns the CameraX controller so a screen can show
// the preview and then grab a still from the very same session.
//
// If the user declines the camera permission — or the device has no camera — the
// state reports [available] = false and the screens keep working on the simulated
// path. The flow is never allowed to dead-end.
// ─────────────────────────────────────────────────────────────────────────────

private const val TAG = "VfaCamera"
private val PREVIEW_TARGET = Size(960, 720)
private val CAPTURE_TARGET = Size(1280, 960)

private fun lowPowerSelector(size: Size) = ResolutionSelector.Builder()
    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
    .setResolutionStrategy(
        ResolutionStrategy(size, ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER)
    )
    .setAllowedResolutionMode(ResolutionSelector.PREFER_CAPTURE_RATE_OVER_HIGHER_RESOLUTION)
    .build()

class VfaCameraState internal constructor(private val context: Context) {

    internal val controller: LifecycleCameraController = LifecycleCameraController(context).apply {
        setEnabledUseCases(LifecycleCameraController.IMAGE_CAPTURE)
        imageCaptureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
        setPreviewResolutionSelector(lowPowerSelector(PREVIEW_TARGET))
        setImageCaptureResolutionSelector(lowPowerSelector(CAPTURE_TARGET))
    }
    private var usingLowPowerTargets = true

    /** True once the permission is granted and the preview can be bound. */
    var granted by mutableStateOf(hasPermission(context))
        internal set

    /** The user was asked and said no — show the "carry on without the camera" path. */
    var denied by mutableStateOf(false)
        internal set

    val available: Boolean get() = granted

    internal fun bindTo(lifecycleOwner: LifecycleOwner) {
        if (!granted) return

        runCatching { controller.bindToLifecycle(lifecycleOwner) }
            .onSuccess { return }
            .onFailure { first ->
                if (!usingLowPowerTargets) {
                    Log.w(TAG, "could not bind camera", first)
                    granted = false
                    return
                }

                Log.w(TAG, "could not bind camera with low-power targets; retrying defaults", first)
                usingLowPowerTargets = false
                runCatching {
                    controller.unbind()
                    controller.setPreviewResolutionSelector(null)
                    controller.setImageCaptureResolutionSelector(null)
                    controller.bindToLifecycle(lifecycleOwner)
                }.onFailure { second ->
                    Log.w(TAG, "could not bind camera with default targets", second)
                    granted = false
                }
            }
    }

    /** Grab a still JPEG from the running preview. Returns null if the camera isn't usable. */
    suspend fun capture(): ByteArray? {
        if (!granted) return null
        return suspendCancellableCoroutine { cont ->
            try {
                controller.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            val bytes = try {
                                val buffer = image.planes[0].buffer
                                ByteArray(buffer.remaining()).also { buffer.get(it) }
                            } catch (e: Exception) {
                                Log.w(TAG, "could not read captured frame", e)
                                null
                            } finally {
                                image.close()
                            }
                            if (cont.isActive) cont.resume(bytes)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.w(TAG, "capture failed", exception)
                            if (cont.isActive) cont.resume(null)
                        }
                    }
                )
            } catch (e: Exception) {
                Log.w(TAG, "capture unavailable", e)
                if (cont.isActive) cont.resume(null)
            }
        }
    }
}

private fun hasPermission(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

/**
 * One camera session for the whole flow, so the preview doesn't have to be torn down
 * and rebuilt between the checkpoint screens and the reader photos.
 */
@Composable
fun rememberVfaCamera(): VfaCameraState {
    val context = LocalContext.current
    val state = remember { VfaCameraState(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { ok ->
        state.granted = ok
        state.denied = !ok
    }

    LaunchedEffect(Unit) {
        if (!state.granted) launcher.launch(Manifest.permission.CAMERA)
    }

    DisposableEffect(lifecycleOwner, state.granted) {
        if (state.granted) {
            state.bindTo(lifecycleOwner)
        }
        onDispose { }
    }

    return state
}

/** The live preview surface. Fills whatever box it is given. */
@Composable
fun CameraViewfinder(state: VfaCameraState, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                controller = state.controller
            }
        },
        update = { it.controller = state.controller }
    )
}
