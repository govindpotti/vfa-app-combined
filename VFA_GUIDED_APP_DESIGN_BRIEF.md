# VFA Guided Test App — Design Brief

A complete rundown of the product for a designer to produce a professional, high‑fidelity
version. It covers what the app is, who it's for, the science, the full screen‑by‑screen
flow, the current design system, and exactly what needs to be designed — including making the
vertical flow assay (VFA) device look like the real hardware (an STL is provided).

> **Working brand name:** *VeriFlow* (placeholder — rename freely).
> **Platform today:** Native Android, Jetpack Compose + Material 3, single portrait phone
> layout. Design mobile‑first, portrait. A web/React mock is fine for exploration, but the
> target is a phone app.

---

## 1. What this app is

A **step‑by‑step guide that walks a completely untrained person through running a medical
self‑test at home** and reading the result — with their phone's voice and camera helping at
every step.

The test is a **Vertical Flow Assay (VFA)**. Unlike a lateral flow test (e.g. a COVID rapid
test) where liquid wicks *sideways* across a strip, in a VFA the liquid flows **straight down
through stacked membrane layers** into an absorbent pad. This makes it faster, cleaner, and
able to test for more targets — but it takes **several timed, hands‑on steps** (pipetting
reagents in a specific order), which is exactly why users need guidance.

VFAs are **not commercially available yet**, so this app is being built ahead of the hardware.
The result is read by a **custom 3D‑printed reader that clips onto the phone**; the reader's
optics + the phone camera quantify the colored signal. That reader's software lands later — for
now the "AI" is stubbed behind a clean interface.

**The core promise:** someone with no lab training, poor eyesight, low literacy, or who
doesn't speak English fluently can still run this test correctly and understand the result.

---

## 2. Who it's for (design for the hardest case)

- **Untrained, first‑time users** — assume they have *never* done a lab procedure.
- **Older adults / low‑vision** — large type, high contrast, generous tap targets.
- **Low literacy** — plain language, short sentences, spoken guidance, visual demonstration.
- **Non‑English speakers** — multilingual, voice‑first, must survive translation and RTL.
- **Anxious users** — this is a health test; tone must be calm and reassuring, never clinical
  or alarming.

**Design principles that follow from this:**
1. **One decision per screen.** Never make the user choose between many things at once.
2. **Voice + text + animation together.** Every instruction is spoken, written, and shown.
3. **The app checks the user's work** — they're never left wondering "did I do that right?"
4. **Mistakes are recoverable and gentle** — guidance is *amber/encouraging*, never red/error.
5. **Warm, human, trustworthy** — feels like a calm guide, not a cold medical device.

---

## 3. The hardware (must look real)

An **STL of the actual VFA cassette is provided** — the on‑screen device must match it.

### 3.1 The VFA cassette
- A flat plastic cassette with a **round sample well on top** and a **result window** where
  the test/control lines appear.
- Liquid pipetted into the well flows **vertically down** through the membrane.
- **Design need:** replace the current stylized illustration with a **faithful 3D render (or
  high‑quality illustration) of the real cassette from the STL** — correct proportions,
  well placement, window placement, materials (plastic sheen), and branding surface.

### 3.2 Reagents & tools (the kit)
The user confirms these before starting (see Kit Check screen). Current kit:
| Item | What it looks like |
|---|---|
| Test cassette | Flat device with a round well on top |
| Sample swab | Long stick with a soft foam tip |
| Sample tube | Small tube of clear liquid |
| Pipette | Small plastic dropper for the wells |
| Wash bottle | Dropper bottle marked "Wash" |
| Phone reader | Small 3D‑printed clip that snaps onto the phone |

> Each kit item should have its own clean, recognizable icon/render that matches the real part.

### 3.3 The 3D‑printed phone reader
- A **clip/hood that attaches to the phone** over the rear camera.
- The **cassette screws into the reader**; the reader shrouds it from ambient light and the
  phone camera reads the membrane lines through it.
- **Design need:** a believable render of the reader clipped to a phone with the cassette
  mounted, plus a "scanning" state. (A second STL/render of the reader may follow.)

---

## 4. The assay protocol (the actual steps)

The test the user selects (Lyme vs Babesiosis) uses the **same mechanical protocol**; only the
cassette's coated antigen and the result interpretation differ.

**Hands‑on steps (each is a pipetting step, each is camera‑verified):**
1. **First wash** — pipette ~3 drops of "Wash" into the well; wait until it soaks all the way
   down (primes the membrane).
2. **Add your sample** — pipette ~4 drops of the prepared sample into the same well; wait until
   it soaks down.
3. **Final wash** — pipette ~3 drops of "Wash" again; wait until it soaks in (clears background).

**Then:**
4. **Develop** — an **8‑minute** timed wait while the signal forms in the membrane.
5. **Read** — screw the cassette into the phone reader and start the scan.
6. **Result** — interpreted for the selected test.

> Sample collection (swab → tube) happens before step 1 and is part of the kit; the app's first
> hands‑on step is the first wash.

---

## 5. Full screen‑by‑screen flow

Portrait phone. Order is linear; the app is a single source of navigation over a `Screen` state.

### 5.1 Onboarding
- **Purpose:** welcome + set accessibility preferences before anything else.
- **Contains:** brand mark (VeriFlow); **hero animation of the kit/cassette**; headline
  "Let's run your test together"; reassurance chip ("About 12 minutes · guided every step");
  **language picker**; **voice on/off toggle** (ON by default); **text‑size selector**
  (A / A / A → Standard / Large / XL); primary **Start** button.
- **States:** language dropdown open; each text size selected; voice on/off.

### 5.2 Test selection
- **Purpose:** "Which test are you running?"
- **Contains:** two large selectable cards — **Lyme disease** (Detects Borrelia antibodies) and
  **Babesiosis** (Detects Babesia antibodies) — each with a distinct accent, a radio/check
  indicator; a **Continue** button; a voice‑replay button.
- **States:** each card selected.

### 5.3 Kit check (materials checklist)
- **Purpose:** make sure the user physically has every item before starting.
- **Contains:** header with the chosen test name; a **"X of N found"** progress badge; a
  **"Watch: unpacking your kit"** demonstration button; a scrollable list of kit items — each a
  tappable row with a color‑coded thumbnail, name, "what it looks like" hint, and a check
  circle; primary button disabled until all are found ("Find all items to continue" →
  "I have everything").
- **States:** item unchecked/checked; count; all‑complete (button enabled); demo dialog open.

### 5.4 Kit scan (camera materials checkpoint) — *AI checkpoint*
- **Purpose:** camera confirms the kit is actually present/complete.
- **Contains:** camera **viewfinder** with corner brackets; phase title; steady‑hand hint.
- **States (shared checkpoint pattern):**
  - **Positioning** — "Point your camera at your kit" / hint "lay your kit inside the box."
  - **Analyzing** — "Hold still — checking" + spinner.
  - **Pass** — green check, "All set!", auto‑advances.
  - **Retry** — amber (never red): a friendly reason (e.g. "I can see most of your kit, but not
    the phone reader…") + a **Try again** button.

### 5.5 Guided step (×3: First wash, Add sample, Final wash)
- **Purpose:** show and narrate one pipetting action.
- **Contains:** a **3‑segment progress bar** (current step emphasized); "STEP n OF 3" chip;
  voice‑replay button; a **large animated illustration of the cassette + pipette + drops +
  downward flow**; the step title (accent) + the plain‑language instruction (large); a
  **"How do I do this?"** expandable help pill with detail text; a **"Watch a real
  demonstration"** video button; primary **"Done — check my work"** button.
- After "Done," the app opens the per‑step camera checkpoint (same pattern as 5.4).

### 5.6 Develop timer
- **Purpose:** the 8‑minute wait while the result forms.
- **Contains:** "Developing your result"; a large **circular countdown ring** (mm:ss);
  reassurance "You can put your phone down. We'll let you know when it's ready."; a
  **"What's happening right now?"** explainer pill (vertical‑flow explanation); a subtle
  breathing animation. (There is a demo "skip wait" affordance for testing.)

### 5.7 Final readout (attach reader → read)
- **Purpose:** mount the cassette in the reader and scan.
- **Contains — attach phase:** "Attach the reader"; instruction "Screw the cassette into the
  reader, then start the scan."; **render/animation of phone + reader + cassette**; a
  **"Watch a real demonstration"** button (the *screwing/mounting* video); a **"Start reading"**
  button.
- **Contains — reading phase:** "Reading your result"; scanning animation (sweeping scan line
  over the membrane window); "Analyzing…" progress.

### 5.8 Result
- **Purpose:** deliver and explain the outcome, calmly.
- **Contains:** a large status badge (✓ negative / ! positive) with soft color; a headline
  interpreted for the test (e.g. **"No Babesiosis detected"** / **"Possible Lyme disease"**);
  a **"What does this mean?"** expandable plain‑language explanation (always framed as *not a
  diagnosis — confirm with a provider*); a **"What should I do next?"** numbered list (talk to a
  doctor / find a clinic / save‑share); **Save / share result (PDF)** button; **Start a new
  test** link.
- **Tone:** positive result uses a **warm terracotta, not alarm red**; negative uses brand green.

---

## 6. Current design system (starting point — please elevate)

The app already has a warm, calm, non‑clinical system. Treat this as the foundation to refine
into something that feels like a real, funded health company shipped it.

### 6.1 Color tokens (current hex)
**Brand (deep emerald‑teal):**
- Primary `#12786A` · Dark `#0C5A50` · Bright `#2AA890` · Soft `#E6F2ED` · Softer `#D8EBE3` ·
  Brand mist `#F1F7F4`

**Warm amber (recoverable guidance — never red):**
- `#D68A2E` · Soft `#FBEEDA` · Border `#EFD3A0`

**Text (warm charcoal‑greens):**
- Ink `#17251F` · Muted `#566862` · Faint `#93A29B`

**Surfaces / lines:**
- Canvas (warm ivory) `#F7F3EC` · Canvas deep `#F1ECE1` · Surface `#FFFFFF` ·
  Surface alt `#FCFAF5` · Line `#E8E1D4` · Card line `#EDE7DB` · Ring track `#D2CCBE`

**Result:** Positive (terracotta) `#D26A4E` / soft `#F7E7E0`; Negative uses brand green.

**Camera / reader:** dark `#16211E` / darker `#0E1614`; scan‑line green `#3DDCB4`;
reader shell `#243530` / `#354A43`.

**Device (cassette) palette:** top `#F6F8F6`, bottom `#E3E9E5`, stroke `#C9D3CE`,
well rim `#BFCAC4`, well inner `#EAEEEC`, membrane `#DCE6E1`, membrane line `#C7D2CC`.

**Reagent drop colors (per phase):** sample amber `#E0A24B`, wash blue `#5FA6C9`,
developer magenta `#BE5B85`.

### 6.2 Typography
- Intended typeface: **Lexend** (rounded, highly legible, low‑literacy friendly). Currently
  substituted with the platform sans as a stand‑in.
- Scale (sp / line‑height, tracking): Display 54/58 (‑1.0) · Headline L 31/37 (‑0.5) ·
  Headline M 26/32 (‑0.3) · Title L 22/28 · Title M 18/24 · Body L 18/27 · Body M 16/24 ·
  Label 19/24. Sizes are already large by default.
- **Text‑size accessibility multiplier:** Standard ×1.00, Large ×1.15, XL ×1.32 — applied
  globally. Layouts must not break or clip at XL.

### 6.3 Components (already exist — restyle, don't reinvent the set)
- **Primary button** — full‑width, 68dp tall, brand gradient, soft shadow, press feedback.
- **Amber button** — "Try again" recovery action.
- **Ghost/outline button** — quiet secondary.
- **Progress bar** — segmented, emphasizes current step.
- **Voice‑replay button** — small brand‑tinted square with a play glyph (re‑reads the screen).
- **Help pill** — expandable "How do I do this?" with a "?" badge.
- **Demo video button** — opens a real‑life demonstration player.
- **Badge / chip** — soft brand pill (counts, status, reassurance).
- **Brand mark** — logo + wordmark lockup.
- **Camera viewfinder** — dark rounded frame, corner brackets, phase animations.
- **Device illustration** — animated cassette/pipette/drops/flow, and the reader scan.

### 6.4 Motion & tone
- Motion is **soft and reassuring**: gentle floats, breathing pulses, drops falling, a
  downward "flow" band through the cassette, a sweeping scan line. Nothing fast or jarring.
- Copy is **short, warm, plain, second‑person** ("Let's…", "Nice work", "Almost there").
- Errors are **encouragement**, not failure ("Almost there" + a specific, kind fix).

---

## 7. Real‑life demonstration videos

Short real‑life clips supplement the animations. The player already:
- Shows a "Watch a real demonstration" button on each step, the kit check, and the reader step.
- Plays the bundled clip if present; otherwise falls back to the animated walkthrough with a
  "coming soon" note.
- Provided footage: **pipetting into the VFA** (used on all three pipetting steps) and
  **screwing the cassette into the reader** (used on the read step).
- **Design need:** a polished full‑screen player (title, video frame matching device
  proportions, caption, close), plus thumbnail/poster treatment for the demo buttons.

---

## 8. The "AI" (hidden from the user)

The user **never sees the word "AI."** Two invisible jobs:
1. **Checkpoints** — after each step (and the kit scan), the camera verifies the work and
   returns Pass / Retry(reason) / needs‑help. UI shows only a viewfinder → check or gentle
   amber guidance.
2. **Final readout** — the reader + camera quantify the membrane signal into a result.

All of this sits behind one clean interface, so the design must fully specify the **checkpoint
states** (positioning / analyzing / pass / retry) and the **reading states** (attach / scanning
/ done) since they carry most of the app's trust.

---

## 9. Accessibility requirements (non‑negotiable)

- Voice guidance ON by default; every screen has a re‑read button.
- Large default type + 3 user‑selectable sizes; nothing clips at XL.
- Tap targets ≥ ~64dp for primary actions.
- High contrast; never rely on color alone (pair with icon/shape/label).
- Full localization incl. **RTL** (Arabic is in the language list) — layouts must mirror.
- Calm, non‑alarming color logic (amber for recover, terracotta not red for positive).

---

## 10. What to deliver (designer scope)

1. **High‑fidelity screens** for every state in §5 (portrait phone).
2. **A refined design system**: color, type (Lexend), spacing, elevation, iconography, the
   component set in §6.3, and motion specs.
3. **Realistic VFA cassette render/illustration from the provided STL** — plus the kit items
   and the **phone‑reader** render (idle + scanning).
4. **All checkpoint & reading states** (positioning/analyzing/pass/retry; attach/scanning).
5. **Empty / loading / error / positive / negative** states throughout.
6. **Light theme required**; dark optional. **RTL** mirrored examples.
7. **Redlines / tokens** exportable for Jetpack Compose (Material 3) implementation.

### Assets provided
- **STL of the VFA cassette** (match the on‑screen device to this).
- **Video:** pipetting into the VFA; screwing the cassette into the reader.
- This brief + the existing app's color/type tokens above.

### Open decisions the designer can shape
- Final **brand name, logo, and identity** (VeriFlow is a placeholder).
- Whether the reader render is a "clip" vs "screw‑in hood" (match final hardware).
- Illustration vs photoreal 3D for the device and reader.

---

## 11. One‑paragraph summary (for the top of a design request)

> VeriFlow is an accessibility‑first mobile app that guides a completely untrained person
> through running a vertical flow assay (VFA) rapid test at home — for Lyme disease or
> Babesiosis — and reading the result with a 3D‑printed reader that clips to the phone. It's
> voice‑first, plain‑language, and warm rather than clinical, for older, low‑literacy, and
> non‑English users. The flow is: onboarding → pick test → check the kit → camera‑verify the
> kit → three camera‑verified pipetting steps (wash, sample, wash) → an 8‑minute develop timer
> → mount the cassette in the reader and scan → a calm, plain‑language result. I need a
> polished, professional visual design and design system, with the on‑screen VFA device made to
> look like the real hardware (STL provided).
