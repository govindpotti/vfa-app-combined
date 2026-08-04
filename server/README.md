# VFA Analyzer Service

A small Flask service that wraps the DiCarlo Lab `VFA_analyzer` pipeline so the phone app can
read a test by uploading the reader-captured cassette image and getting back per-spot
intensities and a verdict. It reuses `../VFA_analyzer/helpers.py` **unchanged**.

## Run locally
```bash
cd server
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
python app.py            # http://localhost:8000
```
Production: `gunicorn -w 2 -b 0.0.0.0:8000 app:app`

## Endpoints
- `GET /health` → `{ ok, spots: 17, analyzer_dir }`
- `POST /analyze` (multipart form)
  - `image`: the **final** (after-signal) membrane photo (`.jpg`/`.png`, or `.dng` if `rawpy`)
  - `baseline` (optional): the **before-signal** reference photo taken at the start. When
    present, the service subtracts it per spot (`final − baseline`) so illumination and
    membrane background cancel out and only the developed signal drives the verdict.
  - `radius` (optional): spot mask radius in px (default 60)
  - Response:
    ```json
    { "ok": true, "verdict": "negative", "background": 41.2, "peak": 55.7,
      "alignment_uncertain": false, "radius": 60,
      "spots": { "1": 42.1, "...": "...", "17": 55.7 },
      "baseline_spots": { "...": "..." }, "signal": { "...": "..." } }
    ```
  The app captures the baseline photo at the first reader step and sends it with the final
  photo automatically (see `capturePhoto`/`runAnalyzer` in the web app).

Quick test:
```bash
curl -F image=@/path/to/cassette.jpg http://localhost:8000/analyze
```

## Connecting the app
The app already POSTs here from the readout screen (see `runAnalyzer` in
`app/src/main/assets/webapp/index.html`). To turn it on:
1. Deploy this service and note its URL.
2. Set `ANALYZER_URL` in `MainActivity.kt` (the `VFABridge`) to that URL.
3. Implement `VFABridge.captureCassetteBase64()` to return the reader-captured JPEG as base64.

Until a captured image is available (reader hardware), the app falls back to the simulated
result — no code change needed.

## ⚠️ Must be set by the lab before clinical use
- **Positivity cutoffs.** `verdict_from()` in `app.py` is a transparent placeholder
  (strongest spot vs. background). Replace it with the validated per-assay thresholds
  (Lyme vs. Babesiosis), including control-spot checks and invalid handling.
- **Spot map / templates.** `pointMapInit`, `template_dictionary`, the crop bounds, and
  `NUM_SPOTS` must match the current VFA design and the reader's capture geometry
  (see `../VFA_analyzer/README.md`). If the reader crops/aligns differently than the desktop
  DNG workflow these constants were tuned for, adjust the crop bounds and marker map.
