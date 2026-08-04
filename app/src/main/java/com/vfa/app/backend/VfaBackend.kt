package com.vfa.app.backend

import android.util.Log
import com.vfa.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

// ─────────────────────────────────────────────────────────────────────────────
// The two analysis services, as one small client.
//
//   step_verifier/serve.py   POST /verify   → grades a checkpoint frame
//   server/app.py            POST /analyze  → quantifies the final membrane photo
//
// Both are optional. When a URL is blank, the service is unreachable, or no frame
// was captured, the call returns [CheckDecision.Unavailable] / null and the UI
// falls back to its simulation — the guided flow always completes end to end.
// Configure the endpoints in app/build.gradle.kts (ANALYZER_URL / VERIFIER_URL).
// ─────────────────────────────────────────────────────────────────────────────

private const val TAG = "VfaBackend"

/** Mirrors the verifier's contract in step_verifier/config.py. */
sealed interface CheckDecision {
    data class Pass(val confidence: Float) : CheckDecision

    /** Recoverable. Shown in amber with a "Try again" — never as an error. */
    data class Retry(val reason: String, val confidence: Float) : CheckDecision

    /** Low confidence or repeated failure: hand off to help rather than wave it through. */
    data class Help(val reason: String) : CheckDecision

    /** No endpoint / no model / no frame — the caller simulates. */
    data object Unavailable : CheckDecision
}

enum class Verdict { NEGATIVE, POSITIVE }

data class Readout(
    val verdict: Verdict,
    val background: Double,
    val peak: Double,
    val alignmentUncertain: Boolean,
)

object VfaBackend {

    val verifierUrl: String get() = BuildConfig.VERIFIER_URL.trimEnd('/')
    val analyzerUrl: String get() = BuildConfig.ANALYZER_URL.trimEnd('/')

    val verifierConfigured: Boolean get() = verifierUrl.isNotEmpty()
    val analyzerConfigured: Boolean get() = analyzerUrl.isNotEmpty()

    /**
     * Grade one checkpoint frame. [step] is the checkpoint id from the protocol
     * (assemble / add_buffer / add_sample / swap_case / add_gold).
     */
    suspend fun verify(step: String, attempt: Int, frame: ByteArray?): CheckDecision =
        withContext(Dispatchers.IO) {
            if (verifierUrl.isEmpty() || frame == null || frame.isEmpty() || step.isEmpty()) {
                return@withContext CheckDecision.Unavailable
            }
            try {
                val body = Multipart()
                    .file("image", "frame.jpg", "image/jpeg", frame)
                    .field("step", step)
                    .field("attempt", attempt.toString())
                val json = post("$verifierUrl/verify", body) ?: return@withContext CheckDecision.Unavailable
                if (!json.optBoolean("ok", false)) return@withContext CheckDecision.Unavailable

                val confidence = json.optDouble("confidence", 0.0).toFloat()
                val reason = json.optString("reason", "")
                when (json.optString("status", "unavailable")) {
                    "pass" -> CheckDecision.Pass(confidence)
                    "retry" -> CheckDecision.Retry(reason, confidence)
                    "help" -> CheckDecision.Help(reason)
                    else -> CheckDecision.Unavailable
                }
            } catch (e: Exception) {
                Log.w(TAG, "step verifier unavailable — simulating this checkpoint", e)
                CheckDecision.Unavailable
            }
        }

    /**
     * Quantify the membrane. [baseline] is the before-signal reader photo; when it is
     * present the service subtracts it per spot so illumination and membrane background
     * cancel out. Returns null when the analyzer can't be reached — the caller simulates.
     */
    suspend fun analyze(finalFrame: ByteArray?, baseline: ByteArray?): Readout? =
        withContext(Dispatchers.IO) {
            if (analyzerUrl.isEmpty() || finalFrame == null || finalFrame.isEmpty()) {
                return@withContext null
            }
            try {
                val body = Multipart().file("image", "final.jpg", "image/jpeg", finalFrame)
                if (baseline != null && baseline.isNotEmpty()) {
                    body.file("baseline", "baseline.jpg", "image/jpeg", baseline)
                }
                val json = post("$analyzerUrl/analyze", body) ?: return@withContext null
                if (!json.optBoolean("ok", false)) return@withContext null
                Readout(
                    verdict = if (json.optString("verdict") == "positive") Verdict.POSITIVE
                    else Verdict.NEGATIVE,
                    background = json.optDouble("background", 0.0),
                    peak = json.optDouble("peak", 0.0),
                    alignmentUncertain = json.optBoolean("alignment_uncertain", false),
                )
            } catch (e: Exception) {
                Log.w(TAG, "analyzer unavailable — simulating the result", e)
                null
            }
        }

    private fun post(url: String, body: Multipart): JSONObject? {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 8_000
            readTimeout = 25_000
            setRequestProperty("Content-Type", "multipart/form-data; boundary=${body.boundary}")
        }
        return try {
            DataOutputStream(conn.outputStream).use { it.write(body.build()) }
            val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: return null
            JSONObject(text)
        } finally {
            conn.disconnect()
        }
    }
}

/** Minimal multipart/form-data writer — avoids pulling in an HTTP library for two calls. */
private class Multipart {
    val boundary = "----vfa" + System.currentTimeMillis()
    private val out = java.io.ByteArrayOutputStream()

    fun field(name: String, value: String) = apply {
        write("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\n\r\n$value\r\n")
    }

    fun file(name: String, filename: String, mime: String, bytes: ByteArray) = apply {
        write(
            "--$boundary\r\nContent-Disposition: form-data; name=\"$name\"; " +
                "filename=\"$filename\"\r\nContent-Type: $mime\r\n\r\n"
        )
        out.write(bytes)
        write("\r\n")
    }

    fun build(): ByteArray {
        write("--$boundary--\r\n")
        return out.toByteArray()
    }

    private fun write(s: String) = out.write(s.toByteArray(Charsets.UTF_8))
}
