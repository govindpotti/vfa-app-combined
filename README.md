# VFA Guided Test App — combined build

An Android app that guides a clinician through running a **Vertical Flow Assay** on a patient's
blood sample — for **Lyme disease** or **Babesiosis** — and reading the result. The membrane is
imaged through a **3D-printed reader that clips over the phone camera**.

It is built for point of care: a nurse or clinician takes the blood sample and runs the test
there and then, in a clinic that has a pipette but not a lab. The app carries the protocol so
nobody has to hold the whole timed workflow in their head, checks each hands-on step through the
camera, and standardises the read — which is what makes a result from one clinic mean the same
thing as a result from another.

The copy is written for that reader: qualified, busy, hands full, and possibly running this test
for the first time. Exact volumes and the technique that changes the result, in plain words.

This repo merges two earlier prototypes:

| Source | What was taken |
|---|---|
| [`september2027/VFA_App_Real`](https://github.com/september2027/VFA_App_Real) | The whole visual design — warm ivory canvas, coral accent, navy serif headlines, soft blobs, pill CTAs, the step indicator — plus the **product photographs** behind the materials page and the two **filmed demonstrations**. |
| [`govindpotti/vfa-guided-app`](https://github.com/govindpotti/vfa-guided-app) | The **protocol engine**, the **camera checkpoints**, the accessibility model (text size, language), the **Blender-rendered clips**, the **STL cassette geometry**, and the whole **backend** (`server/`, `VFA_analyzer/`, `step_verifier/`). |

It is a single **native Jetpack Compose** app — no WebView shell. Going native is what lets the
camera actually open at each checkpoint and at both reader photos, which the WebView build could
only mock up.

---

## What the app does

**Before the test**
1. **Landing** — the real cassette turning in 3D, rendered from the print geometry, plus the
   two settings: text size (Standard / Large / XL) and language.
2. **Which test** — Lyme disease or Babesiosis, plus an optional patient name or ID that carries
   through to the result.
3. **What you need** — all eight items checked off before the timed steps start.

**The 17-stage protocol** — every stage is a row in [`Protocol.stages`](app/src/main/java/com/vfa/app/protocol/Protocol.kt):

| # | Stage | Type | Camera |
|---|---|---|---|
| 1 | Attach phone to reader | action | |
| 2 | Take the bottom case | action | |
| 3 | Attach bottom half | action | |
| 4 | **First photo** | scan | reader photo, before anything is added |
| 5 | Take phone out | action | |
| 6 | Assemble the cassette | reagent | checkpoint `assemble` |
| 7 | Add 200 µL running buffer | reagent | checkpoint `add_buffer` |
| 8 | Add the blood sample | reagent | checkpoint `add_sample` |
| 9 | Add 200 µL running buffer | reagent | checkpoint `add_buffer` |
| 10 | Wait 10 minutes | timer | |
| 11 | Swap the top case | reagent | checkpoint `swap_case` |
| 12 | Add 200 pL gold solution | reagent | checkpoint `add_gold` |
| 13 | Add 50 pL gold solution | reagent | checkpoint `add_gold` |
| 14 | Last wash, 200 pL running buffer | reagent | checkpoint `add_buffer` |
| 15 | Wait 10 minutes | timer | |
| 16 | Put phone back | action | |
| 17 | **Last photo** | scan | reader photo → analyze → result |

> **Check the picolitre volumes before deployment.** Stages 12–14 are carried over verbatim from
> the source protocol, which specifies 200 pL, 50 pL and 200 pL. Those are four orders of
> magnitude below what an air-displacement pipette can deliver — almost certainly µL in the
> original. The app states whatever this table says, so correcting it is a one-line edit in
> `Protocol.kt`, but a clinician will try to follow it exactly as written.

Adding, removing or re-wording a step means editing that one list. Screens, the progress
indicator, the kickers and the checkpoint ids all derive from it.

**Result** — positive or negative for the antibodies, with the patient label, what the result
does and doesn't tell you, what to do now, and — when the analyzer actually ran — the measured
peak/background for the notes. When it didn't run, the screen says the result was not measured
and must not be recorded.

---

## The visuals

Every hands-on stage plays a looping, silent clip. There are two kinds and some stages have both,
switchable with a chip:

- **3D render** — Blender renders of the real cassette (`pipetting_vfa`, `screwing_vfa`,
  `unscrewing_vfa`, from the guided app). Clean mechanics: where the liquid goes, which way the
  case twists. The top-case swap plays `unscrewing_vfa` and then `screwing_vfa` as one sequence.
- **Real footage** — filmed lab demonstrations (`attaching_phone`, `attached_bottom_half`,
  `vfa_assemble_video`, from VFA_App_Real). A real gloved hand doing the real thing.

Two short attachment clips cover the reader-specific actions:

| Clip | Shows | Used on |
|---|---|---|
| `attaching_phone` | the phone being attached to the smartphone reader | steps 1 and 16 |
| `attached_bottom_half` | the bottom half being loaded into the smartphone reader | step 3 and the two photo demos |

Both are short, loop silently, and are paced to be followed in real time rather than skimmed.

The landing screen opens on the **actual print geometry** — `VFAcomb.stl`, the file the cassette
is made from — rendered live and turning, draggable. The first thing the user sees is the real
device rather than a picture of one.

It is rendered in software: the STL is parsed into flat arrays and rasterised with a z-buffer
into a Bitmap that Compose draws like any other image
([`Cassette3D.kt`](app/src/main/java/com/vfa/app/ui/components/Cassette3D.kt)). The guided app did
this with Three.js in a WebView, which is far less code — but a WebView inside the Compose tree
never composited on the emulator (not even a plain coloured page) and twice took the emulator's
GPU stack down with it. Drawing through the same path as the rest of the UI removes that
dependency entirely: if the app renders, the cassette renders. 75,710 triangles cost ~10 ms per
frame on a software-GPU emulator, so it holds 30 fps with headroom to spare.

---

## Built for a clinician with their hands full

Qualified, busy, gloved, patient waiting, and possibly running this test for the first time.
Every interaction assumption follows from that.

- **Text and video together.** Each step is one instruction and one looping clip of the action.
- **Three text sizes**, applied by scaling the whole composition's font scale, so the screen
  stays readable at arm's length — every piece of text grows, including anything added later.
- **One decision per screen**, large tap targets, high contrast.
- **A camera check on each hands-on step.** Not hand-holding — consistency. The same check
  applied wherever the test is run.
- **Failures are amber, never red.** A step that doesn't check out names the one thing to
  correct and offers a retry; a technique slip mid-test is fixable, not an emergency. The check
  can be skipped when the clinician judges it wrong.
- **Nothing claims a check that didn't happen.** With no verifier deployed the screen says the
  step *wasn't checked*, rather than showing a green tick.
- **Back always steps backwards through the protocol** — a mis-tap mid-test can't drop the run.

---

## The two analysis services

Both are optional HTTP endpoints, carried over unchanged from the guided app. Both degrade
gracefully: with no URL configured, unreachable, or no camera frame, the app simulates and *says
on screen that it simulated*.

### `server/` — readout analyzer (Flask + OpenCV)
Wraps the lab's `VFA_analyzer/quantify_VFA.py` unchanged. `POST /analyze` with `image` (final
photo) and optional `baseline` (before-signal photo): aligns to the four corner markers,
localizes the 17 spots, measures red-channel intensity, subtracts the baseline per spot, returns
`{ verdict, spots, baseline_spots, signal, background, peak }`.

> **The positivity cutoff in `verdict_from()` is a labelled placeholder.** The lab must set
> validated per-assay thresholds before any real use.

### `step_verifier/` — checkpoint AI (PyTorch)
Per-step MobileNetV3-Small classifiers. `POST /verify` with `image`, `step`, `attempt` returns
`pass` / `retry(reason)` / `help` / `unavailable`. `config.py` is the single source of truth for
each checkpoint's classes and the spoken reason for each failure. A starter `add_buffer` model is
included under `step_verifier/models/add_buffer`; more classes/photos can be added later and
retrained with `step_verifier/train.py`. `tools/make_dummy_data.py` + `train.py --smoke`
exercise the whole pipeline with synthetic images; `step_verifier/README.md` describes the data
layout to fill in (`data/<step_id>/<class_name>/*.jpg`).

### Real-phone workflow from GitHub

On the laptop that will talk to the LG phone:

```bash
git pull
./scripts/start-services.sh
./scripts/build-phone-apk.sh
./scripts/install-phone-apk.sh
```

`start-services.sh` creates local Python virtualenvs, installs the analyzer/verifier
dependencies, starts the Flask services, and prints this laptop's LAN URLs. Before running the
test, open the printed analyzer health URL on the LG browser, for example:

```text
http://192.168.x.x:8001/health
```

If the LG can load that JSON, the app can reach the analyzer. If it cannot, fix Wi-Fi/IP/firewall
first — rebuilding the APK will not help until the phone can reach the service.

`build-phone-apk.sh` detects the laptop IP and bakes these URLs into the APK:

```text
ANALYZER_URL=http://<this-laptop-ip>:8001
VERIFIER_URL=http://<this-laptop-ip>:8010
```

Override the detection when needed:

```bash
HOST_IP=192.168.1.42 ./scripts/build-phone-apk.sh
ANALYZER_URL=https://example.ngrok.app ./scripts/build-phone-apk.sh
```

Stop the local services with:

```bash
./scripts/stop-services.sh
```

Nothing else changes — the app POSTs the baseline/final reader photos to `/analyze`, maps the
response into the result screen, and shows a concrete connection/analyzer error if the readout
service is not usable.

---

## Build & run

**Prereqs:** Android SDK, an emulator or device (developed against `Pixel_8`, API 36).

⚠️ CLI Gradle builds need `JAVA_HOME` pointed at Android Studio's JBR (JDK 21). Building inside
Android Studio works without this.

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:installDebug
adb shell am start -n com.vfa.app/.MainActivity
```

The camera permission is requested on first launch. Decline it and the run still completes — the
checkpoints report that the camera is unavailable and stop claiming to verify anything.

---

## Layout

```
vfa-app-combined/
├─ app/src/main/
│  ├─ java/com/vfa/app/
│  │  ├─ MainActivity.kt          # edge-to-edge single activity
│  │  ├─ VfaApp.kt                # the flow: front matter, then Protocol.stages
│  │  ├─ protocol/Protocol.kt     # THE PROTOCOL — 17 stages + the kit, as data
│  │  ├─ backend/VfaBackend.kt    # /verify + /analyze, with graceful fallback
│  │  ├─ camera/VfaCamera.kt      # one CameraX session: preview + still capture
│  │  ├─ ui/theme/                # palette, type, text-size scaling
│  │  ├─ ui/components/           # chrome, step visual, video player, kit art, STL viewer
│  │  └─ ui/screens/              # landing, test select, materials, step,
│  │                              #   checkpoint, timer, scan, result
│  ├─ res/drawable/               # the five product photographs
│  ├─ res/raw/                    # eight bundled clips for the step visuals
│  └─ assets/VFAcomb.stl          # the print geometry, rendered on the landing screen
├─ server/                        # readout analyzer (Flask + OpenCV)
├─ VFA_analyzer/                  # the lab's spot-quantification pipeline, unchanged
├─ step_verifier/                 # checkpoint AI (PyTorch) + data-collection spec
└─ VFA_GUIDED_APP_DESIGN_BRIEF.md
```

---

## Status & caveats

- **Guided flow:** complete and verified end to end on the emulator, all 17 stages → result.
- **Camera:** live at every checkpoint and both reader photos, and the captured JPEG is what gets
  POSTed. Verified on the emulator's virtual scene.
- **Analysis:** both services are real code but not deployed. Until they are, checkpoints pass
  and the result is simulated — both states are labelled on screen.
- **Must be set by the lab before any real use:** analyzer positivity cutoffs (`server/app.py`),
  the spot map and alignment templates matching the final cassette and reader geometry
  (`VFA_analyzer/`), and verifier confidence thresholds plus real class coverage
  (`step_verifier/config.py`).
- **Not validated, research use.** This guides a research assay. The result screen always frames
  the outcome as a screening result, not a diagnosis, with confirmatory testing per the site's
  own algorithm.
- **Nothing is saved.** The patient label carries to the result screen so it can be written
  down, but no result is stored on the device or exported anywhere. A clinic that needs records
  or a hand-off to its own system will need that built.
- **No voice guidance.** It was removed after the speech kept cutting out mid-instruction. The
  cause was the announcement effect re-firing on recomposition and flushing the utterance
  queue; if spoken steps are wanted back, that's the thing to fix rather than the TTS setup.
