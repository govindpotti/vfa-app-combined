package com.vfa.app.protocol

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import com.vfa.app.R

// ─────────────────────────────────────────────────────────────────────────────
// The protocol, as data.
//
// The whole 17-stage assay is the [stages] list below, and every screen is derived
// from it. Adding, removing or re-wording a step means editing this list — no screen
// or navigation code changes. The progress badge, the "STEP · …" kickers and the
// checkpoint ids all come from here.
//
// The copy is written for a clinician at the point of care: someone who has just taken
// a blood sample from the patient in front of them and is running the test then and
// there. It gives the exact volumes and the technique that changes the result, in plain
// words — not laboratory process language. They are qualified; they are not a lab. The
// detail that does matter (why both photos need the same settings, why you blot rather
// than wipe) sits one tap away rather than on the screen.
// ─────────────────────────────────────────────────────────────────────────────

/** What kind of screen a stage renders as. */
enum class StageType {
    /** Instruction + Continue. No camera check. */
    ACTION,

    /** Instruction + demo visuals + "Done — check this step" → camera checkpoint. */
    REAGENT,

    /** Countdown timer. */
    WAIT,

    /** Reader photo through the phone camera (baseline before signal, or the final read). */
    SCAN,
}

enum class ScanKind { BASELINE, FINAL }

/**
 * One visual for a step: a bundled clip. These are the real assets — the Blender 3D
 * renders of the cassette from the guided app, and the filmed demonstrations from
 * VFA_App_Real. Where a step has both, the user can switch between them; the render
 * shows the mechanics cleanly, the footage shows a real hand doing it.
 */
data class Clip(
    val label: String,
    @param:RawRes val res: Int,
    val cropOffsetX: Float = 0f,
    val cropOffsetY: Float = 0f,
)
data class Still(val label: String, @param:DrawableRes val res: Int)

data class Stage(
    val type: StageType,
    val kicker: String,
    val title: String,
    val instruction: String,
    /** Short subtitle cue for the exact physical action. */
    val cue: String = "",
    /** Separate audio script. This is spoken aloud instead of reading the UI text verbatim. */
    val narration: String = "",
    /** Longer "More detail" text, hidden behind the help accordion. */
    val help: String,
    val clips: List<Clip> = emptyList(),
    val playClipsInSequence: Boolean = false,
    val still: Still? = null,
    /** Step id the checkpoint AI grades (see step_verifier/config.py). null = no camera check. */
    val checkpoint: String? = null,
    /** WAIT only. */
    val seconds: Int = 0,
    /** SCAN only. */
    val scan: ScanKind? = null,
)

fun Stage.spokenGuidance(stageNumber: Int, stageTotal: Int): String =
    "Step $stageNumber of $stageTotal. " + narration.ifBlank { cue }

/** The tests this kit runs. Same steps either way; different antibodies, different result. */
enum class TestType(
    val displayName: String,
    val tagline: String,
    /** How the result screen names what was looked for. */
    val antibodies: String,
) {
    LYME("Lyme disease", "Looks for Lyme antibodies", "Lyme antibodies"),
    BABESIOSIS("Babesiosis", "Looks for Babesia antibodies", "Babesia antibodies"),
}

/** One row on the "what you need" screen. */
data class KitItem(
    val name: String,
    val hint: String,
    /** How many of this item the run needs. Shown as stacked copies plus a count. */
    val quantity: Int = 1,
    /** Product photo, when we have one for this item. */
    @param:DrawableRes val photo: Int? = null,
    /** Per-item photo sizing in the materials grid. */
    val photoScale: Float = 1f,
    /** Otherwise a drawn emblem in the same visual language. */
    val emblem: Emblem? = null,
) {
    enum class Emblem { TUBE, GOLD_BOTTLE, WIPES }
}

object Protocol {

    // The Blender renders of the cassette (from the guided app) and the filmed
    // demonstrations (from VFA_App_Real). Every hands-on stage points at one or both.
    private val pipetting = Clip("3D render", R.raw.pipetting_vfa)
    private val pipettingNewTop = Clip("3D render", R.raw.pipetting_new_top)
    private val screwing = Clip("3D render", R.raw.screwing_vfa)
    private val unscrewing = Clip("Unscrew", R.raw.unscrewing_vfa)
    private val attachNewTop = Clip("Attach new top", R.raw.screw_new_top)
    private val pipettingReal = Clip("Real footage", R.raw.pipetting_real)
    private val pipettingRealBuffer = Clip(
        "Real footage",
        R.raw.pipetting_real,
        cropOffsetX = 0.10f,
        cropOffsetY = -0.12f
    )
    private val assembleFootage = Clip("Real footage", R.raw.vfa_assemble_video)
    private val attachPhone = Clip("3D render", R.raw.attaching_phone)
    private val attachPhoneFootage = Clip("Real footage", R.raw.attaching_phone_real)
    private val attachBottomHalf = Clip("3D render", R.raw.attached_bottom_half)
    private val attachBottomHalfFootage = Clip("Real footage", R.raw.attached_bottom_half_real)
    private val attachReaderAndBottomHalf = Clip(
        "Attach reader",
        R.raw.reader_attach_bottom_half
    )

    /** The clip used as the landing-screen fallback if the STL can't be read. */
    val heroClip: Clip get() = screwing

    val kit = listOf(
        KitItem(
            "Smartphone reader", "3D-printed clip that fits over the phone camera",
            photo = R.drawable.vfa_reader
        ),
        KitItem(
            "Bottom case", "Holds the sensing membrane",
            photo = R.drawable.vfa_bottom_case
        ),
        KitItem(
            "Top cases", "Two per test — one gets swapped partway",
            quantity = 2,
            photo = R.drawable.vfa_top_case
        ),
        KitItem(
            "Running buffer", "For the 200 µL steps",
            photo = R.drawable.vfa_buffer
        ),
        KitItem(
            "Blood sample", "From the patient, ready to use",
            emblem = KitItem.Emblem.TUBE
        ),
        KitItem(
            "Gold nanoparticles", "Mix gently before you draw them up",
            photo = R.drawable.gold_nanoparticles,
            photoScale = 1.22f
        ),
        KitItem(
            "Pipette and tips", "For the volumes in this test",
            photo = R.drawable.vfa_pipette
        ),
        KitItem(
            "Lint-free wipes", "To blot the membrane before the last photo",
            photo = R.drawable.kimtech_wipes
        ),
    )

    val stages: List<Stage> = listOf(
        Stage(
            type = StageType.ACTION,
            kicker = "SET UP · READER",
            title = "Attach phone to reader",
            instruction = "Put the phone into the smartphone reader so the reader fits over the " +
                "phone camera. Adjust the shutter speed until the membrane looks evenly lit.",
            cue = "Push the phone in until it sits flat in the reader. The camera should be fully covered.",
            narration = "Place the phone into the reader now. Press it in until it sits flat, with the camera fully covered by the reader opening. When the preview looks evenly lit, continue.",
            help = "The reader blocks outside light so both photos are taken the same way — not " +
                "too bright, not too dark. Use this same phone-and-reader position for the " +
                "first and last photos. The app compares the two photos against each other, " +
                "so they need matching settings.",
            clips = listOf(attachPhone, attachPhoneFootage),
        ),
        Stage(
            type = StageType.ACTION,
            kicker = "SET UP · READER",
            title = "Attach bottom half",
            instruction = "Slide the bottom half into the smartphone reader so the membrane is " +
                "in position for the first photo.",
            cue = "Slide it in until it stops. Do not force it past the stop.",
            narration = "Slide the bottom half into the reader. Move slowly until you feel it reach the stop. Once it stops, do not push harder.",
            help = "This puts the membrane in the same reader position the analyzer will use " +
                "again at the end. Handle the bottom half by the edges and keep the membrane " +
                "face clean.",
            clips = listOf(attachBottomHalf, attachBottomHalfFootage),
        ),
        Stage(
            type = StageType.SCAN,
            scan = ScanKind.BASELINE,
            kicker = "PHOTO · BEFORE",
            title = "First photo",
            instruction = "Put the bottom case in the reader and photograph the membrane before " +
                "anything is added. Line up the top-right corner.",
            cue = "Hold steady. Match the top-right corner and keep the membrane centered.",
            narration = "This is the baseline photo. Keep the phone and reader still. Line up the top right corner in the guide, keep the membrane centered, then take the photo.",
            help = "This is the \u201cbefore\u201d photo. The app compares the final photo " +
                "against it, which is how it tells a real signal from the membrane's own " +
                "background. Take it on the same reader, at the same settings, as the last photo.",
            clips = listOf(attachBottomHalf, attachBottomHalfFootage),
        ),
        Stage(
            type = StageType.ACTION,
            kicker = "SET UP · PHONE",
            title = "Take phone out",
            instruction = "Remove the phone and the bottom half from the reader after the first " +
                "photo. Keep the reader nearby, because both go back in for the final analyzer photo.",
            cue = "Take out both pieces gently. Keep the membrane facing up and clean.",
            narration = "Remove the phone from the reader, then remove the bottom half as well. Keep the membrane facing up and do not touch it. Set the reader nearby for the final photo.",
            help = "The middle steps use the phone camera to check the hands-on work from " +
                "outside the reader. Keep holding the bottom half by the edges, and do not touch " +
                "the membrane. The final photo should match the first photo as closely as possible.",
        ),
        Stage(
            type = StageType.REAGENT,
            checkpoint = "assemble",
            kicker = "STEP · ASSEMBLE",
            title = "Assemble the cassette",
            instruction = "Screw a top case onto the bottom case until it sits flat, with no gap " +
                "around the edge.",
            cue = "Twist until it stops. Go until you cannot turn it anymore, then stop.",
            narration = "Place a top case onto the bottom case. Twist it closed until it stops. Go until you cannot turn it anymore, then stop. Check that the top sits flat with no gap around the edge.",
            help = "Line the well up over the membrane, then twist until it stops. If it isn't " +
                "fully closed it leaks around the edge and the liquid won't flow straight down " +
                "through the membrane.",
            clips = listOf(screwing, assembleFootage),
        ),
        Stage(
            type = StageType.REAGENT,
            checkpoint = "add_buffer",
            kicker = "STEP · RUNNING BUFFER",
            title = "Add running buffer",
            instruction = "Add 200 µL of running buffer to the well. Wait until it has all " +
                "drained through.",
            cue = "Touch the pipette tip to the side of the well. Dispense slowly and wait until the well is empty.",
            narration = "Add the running buffer now. Rest the pipette tip against the inside wall of the well. Dispense slowly down the side, then wait until the well looks empty.",
            help = "Dispense against the side of the well rather than straight onto the " +
                "membrane. Wait until the well looks empty — liquid left behind carries into the " +
                "next step and throws the timing out.",
            clips = listOf(pipetting, pipettingRealBuffer),
        ),
        Stage(
            type = StageType.REAGENT,
            checkpoint = "add_sample",
            kicker = "STEP · BLOOD SAMPLE",
            title = "Add the blood sample",
            instruction = "Add the patient's blood sample to the well. Wait until it has all " +
                "drained through.",
            cue = "Use a fresh tip. Dispense against the side of the well, then wait for the liquid to drain.",
            narration = "Use a fresh tip for the patient sample. Touch the tip to the side of the well and dispense slowly. Wait for the sample to drain through before moving on.",
            help = "Check the label against the patient before you add it, and use a fresh tip.",
            clips = listOf(pipetting, pipettingReal),
        ),
        Stage(
            type = StageType.REAGENT,
            checkpoint = "add_buffer",
            kicker = "STEP · RUNNING BUFFER",
            title = "Add running buffer",
            instruction = "Add another 200 µL of running buffer. Wait until it has all drained " +
                "through.",
            cue = "Add it slowly. Do not move on until the well looks empty.",
            narration = "Add the second running buffer wash. Keep the tip on the side of the well and dispense slowly. Do not continue until the well looks empty.",
            help = "This pushes the sample the rest of the way through the membrane. Let it " +
                "clear completely before the timer starts.",
            clips = listOf(pipetting, pipettingRealBuffer),
        ),
        Stage(
            type = StageType.WAIT,
            seconds = 600,
            kicker = "WAIT · 10 MINUTES",
            title = "Wait 10 minutes",
            instruction = "Leave the cassette flat and don't move it. You'll be told when the " +
                "time is up.",
            cue = "Set it down flat. Do not tilt, lift, or bump the cassette during the wait.",
            narration = "Set the cassette down flat. Leave it alone during the wait. Do not tilt it, lift it, or bump it while the timer runs.",
            help = "Antibodies are sticking to the spots on the membrane as the liquid moves " +
                "down through it. Moving or tilting the cassette now will skew the result.",
        ),
        Stage(
            type = StageType.REAGENT,
            checkpoint = "swap_case",
            kicker = "STEP · TOP CASE",
            title = "Swap the top case",
            instruction = "Unscrew the used top case and put a fresh one on the same bottom case.",
            cue = "Unscrew the used top. Place the fresh top on and twist until it stops.",
            narration = "Unscrew the used top case and remove it from the bottom case. Put a fresh top case on the same bottom half. Twist the fresh top until it stops and sits flat.",
            help = "The used top case has blood in it — put it straight into biohazard waste, " +
                "and never use it on another test. The bottom case, with the membrane, stays.",
            clips = listOf(unscrewing, attachNewTop),
            playClipsInSequence = true,
        ),
        Stage(
            type = StageType.REAGENT,
            checkpoint = "add_gold",
            kicker = "STEP · GOLD SOLUTION",
            title = "Add gold solution",
            instruction = "Add 200 pL of gold solution to the well. Wait until it has all " +
                "drained through.",
            cue = "Mix the gold gently first. Dispense slowly against the side of the well.",
            narration = "Gently mix the gold solution before drawing it up. Add the gold solution against the side of the well. Dispense slowly, and try not to make bubbles.",
            help = "The gold solution is what brings the colour out on the spots. Mix it gently " +
                "before you draw it up — it settles — and try not to introduce bubbles.",
            clips = listOf(pipettingNewTop),
        ),
        Stage(
            type = StageType.REAGENT,
            checkpoint = "add_gold",
            kicker = "STEP · GOLD SOLUTION",
            title = "Add gold solution",
            instruction = "Add another 50 pL of gold solution. Wait until it has all drained " +
                "through.",
            cue = "Use the smaller volume. Wait until the well clears before continuing.",
            narration = "Add the smaller gold solution volume now. Keep the tip against the side of the well. Wait until the liquid has drained before continuing.",
            help = "A smaller amount this time. Same technique — mix, dispense against the side " +
                "of the well, wait for it to clear.",
            clips = listOf(pipettingNewTop),
        ),
        Stage(
            type = StageType.REAGENT,
            checkpoint = "add_buffer",
            kicker = "STEP · LAST WASH",
            title = "Last wash",
            instruction = "Add 200 pL of running buffer to wash out anything left over.",
            cue = "Wash slowly and let it drain completely so the background stays clear.",
            narration = "Add the final wash. Dispense slowly down the side of the well. Let it drain completely so the membrane background stays as clear as possible.",
            help = "Gold solution left on the membrane darkens the background and makes the " +
                "result harder to read. Let this wash clear completely.",
            clips = listOf(pipettingNewTop),
        ),
        Stage(
            type = StageType.WAIT,
            seconds = 600,
            kicker = "WAIT · 10 MINUTES",
            title = "Wait 10 minutes",
            instruction = "Start the timer now everything has been added. Leave the cassette " +
                "flat and don't move it.",
            cue = "Leave the cassette flat. The color is developing now.",
            narration = "Start the final wait. Leave the cassette flat while the color develops. Do not read it early, and do not move it during the timer.",
            help = "The colour is coming up on the spots. Reading it early makes the result look " +
                "weaker than it is; leaving it much longer lets the background darken.",
        ),
        Stage(
            type = StageType.ACTION,
            kicker = "SET UP · READER",
            title = "Put phone back",
            instruction = "Put the phone back into the smartphone reader, then load the bottom " +
                "half before the analyzer photo. Use the same reader position and camera settings " +
                "as the first photo.",
            cue = "Seat the phone first, then slide in the bottom half until it stops.",
            narration = "Put the phone back into the reader first. Seat it the same way as before. Then slide in the bottom half until it reaches the stop.",
            help = "The analyzer depends on the before and after photos matching. Re-seat the " +
                "phone in the reader, then load the bottom case for the final photo.",
            clips = listOf(attachReaderAndBottomHalf),
        ),
        Stage(
            type = StageType.SCAN,
            scan = ScanKind.FINAL,
            kicker = "PHOTO · RESULT",
            title = "Last photo",
            instruction = "Blot the membrane with a lint-free wipe, put the bottom case in the " +
                "reader, and take the analyzer photo. Line up the top-right corner and keep the " +
                "membrane centred.",
            cue = "Blot straight down. Do not wipe. Hold steady for the final read.",
            narration = "Blot the membrane straight down with a lint free wipe. Do not drag or wipe across it. Put the bottom case in the reader, line up the top right corner, and hold steady for the final analyzer photo.",
            help = "Blot, don't wipe — dragging across the membrane smears the colour. Use the " +
                "same reader and the same settings as the first photo, or the comparison won't " +
                "hold. Centre the membrane so the left and right of the frame look even.",
            clips = listOf(attachBottomHalf, attachBottomHalfFootage),
        ),
    )

    val count: Int get() = stages.size
}
