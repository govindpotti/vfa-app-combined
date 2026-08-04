# Step Verifier — the checkpoint AI

The model behind each guided-step **checkpoint**: it looks at a camera frame of the cassette
and decides whether the step was done correctly, returning the app's exact contract —
**Pass** (green check, advance), **Retry** (amber "Almost there" + a spoken reason), or
**FlagForHelp** (escalate). It covers the materials check and the four hands-on steps
(assemble, first wash, add sample, final wash).

There's **no training data yet** — this is the backbone. Everything runs and degrades
gracefully: any step without a trained model returns `unavailable`, and the app falls back to
its simulated checkpoint, so the whole flow keeps working. Drop in images, train, and each
step goes live one at a time.

## How it works
- **Transfer learning** from MobileNetV3-Small (ImageNet) — small, fast, and works with only a
  few hundred images per class. One lightweight classifier **per step**.
- Each step's classes and the guidance for each failure live in **`config.py`** (single source
  of truth). `ok` = pass; every other class carries the reason the user hears.
- `infer.verify()` maps the prediction → `pass`/`retry`/`help`, with a **confidence floor**
  (low confidence ⇒ help, not a false pass) and **repeat-failure escalation** to help.

## Data layout (what you'll add)
```
step_verifier/data/<step_id>/<class_name>/*.jpg
```
Step ids and their classes (from `config.py`):
| step | pass class | failure classes (retry/help) |
|---|---|---|
| `kit` | `ok` | `missing_items`, `obscured` |
| `assemble` | `ok` | `not_assembled`, `misaligned` |
| `first_wash` | `ok` | `not_soaked`, `no_liquid`, `overfilled` |
| `add_sample` | `ok` | `not_soaked`, `no_liquid`, `wrong_well` |
| `final_wash` | `ok` | `not_soaked`, `no_liquid` |

**Capture tips:** shoot the way the app checkpoint sees it (phone held over the cassette),
and vary lighting, background, hands-in-frame, and phone models. A few hundred images per class
to start; augmentation (flips/rotation/color jitter/blur) stretches a small set.

## Quickstart
```bash
cd step_verifier
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt

# 1) Prove the pipeline end-to-end with throwaway data (no real images needed):
python train.py --step assemble --smoke
python serve.py            # then POST a frame to http://localhost:8010/verify

# 2) With real data in data/<step>/<class>/:
python train.py --all              # trains every step that has data
python export.py --step assemble   # optional: ONNX for on-device later
```
Quick check:
```bash
curl -F step=assemble -F image=@/path/to/frame.jpg http://localhost:8010/verify
# -> {"ok":true,"status":"retry","reason":"The two halves aren't joined yet...","confidence":0.87,"label":"not_assembled"}
```

## Connecting the app
The web app already calls the verifier from `runCp` (see
`app/src/main/assets/webapp/index.html`) via the native bridge. To turn it on:
1. Deploy `serve.py` and note its URL.
2. Set `VERIFIER_URL` in `MainActivity.kt` (`VFABridge`).
3. Implement `VFABridge.captureFrameBase64()` to return the checkpoint camera frame (base64 JPEG).

Until a step has a trained model (or no URL/frame is available), the app uses its simulated
checkpoint — no code change needed.

## Files
| file | purpose |
|---|---|
| `config.py` | steps, per-step classes, and their pass/retry/help mapping + reasons |
| `dataset.py` | ImageFolder loading, train/val split, augmentation |
| `model.py` | MobileNetV3 backbone, checkpoint save, `StepVerifier` inference |
| `train.py` | transfer-learn one/all steps (weighted loss, early stop) → `models/<step>/` |
| `infer.py` | `verify(step, image)` → app checkpoint decision; graceful `unavailable` |
| `serve.py` | Flask `/verify` + `/health` |
| `export.py` | ONNX export (path to TFLite on-device) |
| `tools/make_dummy_data.py` | synthetic data to smoke-test the pipeline |

## ⚠️ Before clinical use
Set per-class `min_confidence` and `escalate_after_attempts` in `config.py` from validation
data, review the confusion matrix per step, and make sure the failure classes cover the real
mistakes users make (add classes/reasons as you learn them). The retry copy in `config.py`
is what users hear — keep it kind and specific.
