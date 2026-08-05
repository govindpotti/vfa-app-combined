"""
VFA analyzer service — a thin Flask wrapper around the DiCarlo Lab quantify_VFA pipeline.

The app POSTs the reader-captured cassette image to POST /analyze; this service aligns the
image against the four marker templates, localizes the 17-spot grid, measures the red-channel
mean at each spot, and returns per-spot values plus a verdict.

It reuses VFA_analyzer/helpers.py UNCHANGED (alignImage, localizeWithCentroid, getStats,
generateMask). Only the spot map / constants below are copied from quantify_VFA.py so the
service doesn't import rawpy at startup (DNG support is loaded lazily, only if a .dng is sent).

Run:
    pip install -r requirements.txt
    python app.py            # serves on 0.0.0.0:8000

IMPORTANT: the positivity verdict here is a transparent placeholder. The lab must replace
`verdict_from()` with the validated cutoffs for each assay (and confirm the pointMap /
templates match the current VFA design — see VFA_analyzer/README.md).
"""

import io
import os
import sys

import cv2
import numpy as np
from flask import Flask, jsonify, request
from PIL import Image, ImageOps

# --- Reuse the lab's analyzer helpers unchanged -----------------------------------------
ANALYZER_DIR = os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "VFA_analyzer")
)
sys.path.insert(0, ANALYZER_DIR)
# matchTemplate() reads "alignment templates/..." relative to the working dir:
os.chdir(ANALYZER_DIR)

from helpers import alignImage, localizeWithCentroid, getStats, generateMask  # noqa: E402

# --- Spot map + constants (identical to quantify_VFA.py) --------------------------------
pointMapInit = {
    "A": (1084 + 100, 1561 + 100),
    "B": (2531 + 100, 1579 + 100),
    "C": (1082 + 95, 3019 + 95),
    "D": (2538 + 95, 3035 + 95),
    "1": (1424, 1856), "2": (1752, 1888), "3": (2072, 1880), "4": (2392, 1896),
    "5": (1412, 2232), "6": (1712, 2224), "7": (2068, 2224), "8": (2404, 2224),
    "9": (1420, 2552), "10": (1740, 2552), "11": (2068, 2552), "12": (2396, 2552),
    "13": (1412, 2864), "14": (1732, 2864), "15": (2068, 2864), "16": (2404, 2864),
    "17": (1900, 2384),
}
template_dictionary = {
    "template_A": "alignment_A_CPN.jpg",
    "template_B": "alignment_B_CPN.jpg",
    "template_C": "alignment_C_CPN.jpg",
    "template_D": "alignment_D_CPN.jpg",
}
YMIN, YMAX, XMIN, XMAX = 600, 4200, 200, 3800
MARKER_A = pointMapInit["A"]
DIST_A_TO_B = pointMapInit["B"][0] - pointMapInit["A"][0]
THRESH = [3, 1, 0.5]  # [filter1 (std), upper, lower] — same as quantify_VFA.py
DEFAULT_R = 60
MEAN_ONLY = [0, 1, 0, 0]  # getStats "commands": std, mean, max, min -> mean only
SPOT_KEYS = [k for k in pointMapInit if k not in ("A", "B", "C", "D")]
MIN_ANALYZER_WIDTH = XMAX + 20
MIN_ANALYZER_HEIGHT = YMAX + 20
ANALYZER_SIZE = YMAX - YMIN

app = Flask(__name__)


def normalize_capture(image_bgr):
    """Scale phone JPEGs into the coordinate space expected by the legacy analyzer."""
    h, w = image_bgr.shape[:2]
    scale = max(MIN_ANALYZER_WIDTH / max(1, w), MIN_ANALYZER_HEIGHT / max(1, h), 1.0)
    if scale <= 1.0:
        return image_bgr
    return cv2.resize(
        image_bgr,
        (int(round(w * scale)), int(round(h * scale))),
        interpolation=cv2.INTER_CUBIC,
    )


def legacy_crop(image_bgr):
    """Original desktop/DNG path: upscale, then use the hard-coded analyzer crop."""
    image_bgr = normalize_capture(image_bgr)
    h, w = image_bgr.shape[:2]
    if h < YMAX or w < XMAX:
        raise ValueError(f"image too small after normalization: {w}x{h}")
    return image_bgr[YMIN:YMAX, XMIN:XMAX]


def reader_square_crop(image_bgr):
    """
    Phone-reader path: the reader photo is centered by the live guide, not by the
    legacy desktop crop constants. Crop the sensor frame to the centered square the
    analyzer expects, then resize into the 3600x3600 coordinate system used by the
    spot map and alignment templates.
    """
    h, w = image_bgr.shape[:2]
    side = min(h, w)
    if side <= 0:
        raise ValueError("empty image")
    x0 = max(0, (w - side) // 2)
    y0 = max(0, (h - side) // 2)
    square = image_bgr[y0:y0 + side, x0:x0 + side]
    return cv2.resize(square, (ANALYZER_SIZE, ANALYZER_SIZE), interpolation=cv2.INTER_CUBIC)


def analyze_prepared_bgr(cropped_bgr, r=DEFAULT_R):
    """Return {spot_key: mean_red_intensity} and an alignment error flag for one cropped image."""
    h, w = cropped_bgr.shape[:2]
    if h < ANALYZER_SIZE or w < ANALYZER_SIZE:
        raise ValueError(f"prepared image too small: {w}x{h}")

    aligned, err = alignImage(cropped_bgr, "upload", DIST_A_TO_B, MARKER_A, template_dictionary)
    point_map, err = localizeWithCentroid(aligned, pointMapInit, False, err)
    red = aligned[:, :, 2]
    generateMask(r)
    spots = {}
    for k in SPOT_KEYS:
        stats = getStats(red, r, point_map[k], MEAN_ONLY, 0, THRESH, "", "upload")
        spots[k] = float(stats[0])
    return spots, bool(err)


def analyze_bgr(image_bgr, r=DEFAULT_R):
    """Analyze one BGR image, trying phone framing and common orientations before giving up."""
    candidates = [
        image_bgr,
        cv2.rotate(image_bgr, cv2.ROTATE_90_CLOCKWISE),
        cv2.rotate(image_bgr, cv2.ROTATE_90_COUNTERCLOCKWISE),
        cv2.rotate(image_bgr, cv2.ROTATE_180),
    ]
    last_error = None
    fallback = None
    for candidate in candidates:
        for crop_name, prepare in (
            ("reader_square", reader_square_crop),
            ("legacy_crop", legacy_crop),
        ):
            try:
                prepared = prepare(candidate)
                spots, err = analyze_prepared_bgr(prepared, r)
                if not err:
                    return spots, err
                fallback = fallback or (spots, err)
            except Exception as exc:  # noqa: BLE001
                last_error = f"{crop_name}: {exc}"

    if fallback is not None:
        return fallback
    raise ValueError(f"could not align image: {last_error}")


def verdict_from(spots):
    """PLACEHOLDER positivity call — replace with the lab's validated per-assay cutoffs."""
    vals = np.array(list(spots.values()), dtype=float)
    background = float(np.percentile(vals, 20))
    peak = float(np.max(vals))
    positive = (peak > background + 8) and ((peak - background) > 0.10 * (background + 1e-6))
    return ("positive" if positive else "negative"), background, peak


def decode(file_storage):
    data = file_storage.read()
    name = (file_storage.filename or "").lower()
    if name.endswith(".dng"):
        import rawpy  # lazy — only needed for raw DNG input
        with rawpy.imread(io.BytesIO(data)) as raw:
            return np.flip(raw.postprocess(), axis=2)
    try:
        image = Image.open(io.BytesIO(data))
        image = ImageOps.exif_transpose(image).convert("RGB")
        return cv2.cvtColor(np.array(image), cv2.COLOR_RGB2BGR)
    except Exception:
        arr = np.frombuffer(data, np.uint8)
        return cv2.imdecode(arr, cv2.IMREAD_COLOR)


@app.get("/health")
def health():
    return jsonify(ok=True, spots=len(SPOT_KEYS), analyzer_dir=ANALYZER_DIR)


@app.post("/analyze")
def analyze():
    f = request.files.get("image")
    if f is None:
        return jsonify(ok=False, error="no image file (field 'image')"), 400
    try:
        r = int(request.form.get("radius", DEFAULT_R))
        img = decode(f)
        if img is None:
            return jsonify(ok=False, error="could not decode image"), 400
        spots, err = analyze_bgr(img, r)

        # Optional before-signal baseline: subtract it per spot so illumination / membrane
        # background cancels out and only the developed signal remains.
        baseline_spots = None
        bf = request.files.get("baseline")
        if bf is not None:
            b_img = decode(bf)
            if b_img is not None:
                baseline_spots, b_err = analyze_bgr(b_img, r)
                err = err or b_err

        signal = ({k: spots[k] - baseline_spots.get(k, 0.0) for k in spots}
                  if baseline_spots is not None else spots)

        verdict, background, peak = verdict_from(signal)
        return jsonify(
            ok=True, verdict=verdict, background=background, peak=peak,
            alignment_uncertain=err, radius=r, spots=spots,
            baseline_spots=baseline_spots,
            signal=(signal if baseline_spots is not None else None),
        )
    except Exception as exc:  # noqa: BLE001
        return jsonify(ok=False, error=str(exc)), 500


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=int(os.environ.get("PORT", 8000)))
