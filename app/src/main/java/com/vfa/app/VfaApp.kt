package com.vfa.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.vfa.app.backend.AnalyzerResult
import com.vfa.app.backend.Readout
import com.vfa.app.backend.Verdict
import com.vfa.app.backend.VfaBackend
import com.vfa.app.camera.rememberVfaCamera
import com.vfa.app.protocol.Protocol
import com.vfa.app.protocol.ScanKind
import com.vfa.app.protocol.StageType
import com.vfa.app.protocol.TestType
import com.vfa.app.ui.screens.*
import com.vfa.app.ui.theme.Cream
import com.vfa.app.ui.theme.TextScale
import com.vfa.app.ui.theme.VfaTheme

// ─────────────────────────────────────────────────────────────────────────────
// The flow.
//
// Front matter (landing → pick the test → check what you need), then the protocol
// itself, walked straight out of Protocol.stages: each stage's `type` decides which
// screen renders it, so the whole 14-step sequence is data, not branching code.
// Reagent stages detour through the camera checkpoint before advancing.
// ─────────────────────────────────────────────────────────────────────────────

private enum class Screen { LANDING, TEST, MATERIALS, STEP, CHECKPOINT, TIMER, SCAN, RESULT }

private val LANGUAGES = listOf("English", "Español", "中文", "Tiếng Việt", "العربية", "Русский")

@Composable
fun VfaApp() {
    var textScale by remember { mutableStateOf(TextScale.STANDARD) }
    var screen by remember { mutableStateOf(Screen.LANDING) }

    VfaTheme(textScale = textScale, darkChrome = screen == Screen.CHECKPOINT) {
        var stageIndex by remember { mutableIntStateOf(0) }
        var test by remember { mutableStateOf<TestType?>(null) }
        var patientLabel by remember { mutableStateOf("") }
        var kit by remember { mutableStateOf(List(Protocol.kit.size) { false }) }
        var language by remember { mutableStateOf(LANGUAGES.first()) }

        var baseline by remember { mutableStateOf<ByteArray?>(null) }
        var verdict by remember { mutableStateOf(Verdict.NEGATIVE) }
        var readout by remember { mutableStateOf<Readout?>(null) }
        var analyzerFailure by remember { mutableStateOf<AnalyzerResult.Failed?>(null) }

        val camera = rememberVfaCamera()

        val stage = Protocol.stages[stageIndex.coerceIn(0, Protocol.count - 1)]


        // Route to whichever screen this stage of the protocol renders as.
        fun goStage(i: Int) {
            if (i >= Protocol.count) {
                screen = Screen.RESULT
                return
            }
            stageIndex = i
            screen = when (Protocol.stages[i].type) {
                StageType.WAIT -> Screen.TIMER
                StageType.SCAN -> Screen.SCAN
                else -> Screen.STEP
            }
        }

        fun restart() {
            stageIndex = 0
            test = null
            patientLabel = ""
            kit = List(Protocol.kit.size) { false }
            baseline = null
            readout = null
            analyzerFailure = null
            verdict = Verdict.NEGATIVE
            screen = Screen.LANDING
        }



        // Back is always a step backwards through the protocol, never an exit —
        // a mis-tap in the middle of a timed assay shouldn't drop the run.
        BackHandler(enabled = screen != Screen.LANDING && screen != Screen.RESULT) {
            when (screen) {
                Screen.TEST -> screen = Screen.LANDING
                Screen.MATERIALS -> screen = Screen.TEST
                Screen.CHECKPOINT -> screen = Screen.STEP
                Screen.STEP, Screen.TIMER, Screen.SCAN ->
                    if (stageIndex == 0) screen = Screen.MATERIALS else goStage(stageIndex - 1)
                else -> Unit
            }
        }

        // Screens inset themselves, so the camera views can run full bleed behind the
        // status bar while every ordinary screen still clears it.
        Box(Modifier.fillMaxSize().background(Cream)) {
            when (screen) {
                Screen.LANDING -> LandingScreen(
                    textScale = textScale,
                    onTextScaleChange = { textScale = it },
                    language = language,
                    onLanguageChange = { language = it },
                    languages = LANGUAGES,
                    onStart = { screen = Screen.TEST }
                )

                Screen.TEST -> TestSelectScreen(
                    selected = test,
                    onSelect = { test = it },
                    patientLabel = patientLabel,
                    onPatientLabelChange = { patientLabel = it },
                    onContinue = { screen = Screen.MATERIALS },
                )

                Screen.MATERIALS -> MaterialsScreen(
                    checked = kit,
                    onToggle = { i -> kit = kit.toMutableList().also { it[i] = !it[i] } },
                    onContinue = { goStage(0) },
                    testName = test?.displayName ?: "Test"
                )

                Screen.STEP -> StepScreen(
                    stage = stage,
                    stageNumber = stageIndex + 1,
                    stageTotal = Protocol.count,
                    onAction = {
                        if (stage.checkpoint != null) screen = Screen.CHECKPOINT
                        else goStage(stageIndex + 1)
                    },
                )

                Screen.CHECKPOINT -> CheckpointScreen(
                    stage = stage,
                    camera = camera,
                    onPass = { goStage(stageIndex + 1) },
                    onSkip = { goStage(stageIndex + 1) },
                )

                Screen.TIMER -> TimerScreen(
                    stage = stage,
                    onDone = { goStage(stageIndex + 1) },
                    stageNumber = stageIndex + 1,
                    stageTotal = Protocol.count
                )

                Screen.SCAN -> ScanScreen(
                    stage = stage,
                    stageNumber = stageIndex + 1,
                    stageTotal = Protocol.count,
                    camera = camera,
                    onCaptured = { frame ->
                        if (stage.scan == ScanKind.FINAL) {
                            // Both photos go up together so the analyzer can subtract the
                            // before-signal reference; null means it simulated.
                            when (val result = VfaBackend.analyze(frame, baseline)) {
                                is AnalyzerResult.Measured -> {
                                    readout = result.readout
                                    analyzerFailure = null
                                    verdict = result.readout.verdict
                                }
                                is AnalyzerResult.Failed -> {
                                    readout = null
                                    analyzerFailure = result
                                }
                            }
                            screen = Screen.RESULT
                        } else {
                            baseline = frame
                            goStage(stageIndex + 1)
                        }
                    },
                )

                Screen.RESULT -> ResultScreen(
                    test = test,
                    patientLabel = patientLabel,
                    verdict = verdict,
                    readout = readout,
                    analyzerFailure = analyzerFailure,
                    onStartNew = { restart() },
                    onToggleDemo = {
                        verdict = if (verdict == Verdict.NEGATIVE) Verdict.POSITIVE else Verdict.NEGATIVE
                    },
                )
            }
        }
    }
}
