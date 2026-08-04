"""
Step-verifier service. The app POSTs the checkpoint frame here; it returns the Pass / Retry /
help decision. Runs independently of the readout analyzer (../server) since it needs torch.

    pip install -r requirements.txt
    python serve.py                 # http://0.0.0.0:8010
    gunicorn -w 2 -b 0.0.0.0:8010 serve:app

Endpoints:
    GET  /health           -> { ok, models: {step: trained?} }
    POST /verify           -> multipart: image=<frame>, step=<step_id>, attempt=<int>
                              { ok, status, reason, confidence, label }
"""
import io
import os
import sys

from flask import Flask, jsonify, request

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import config as C
from infer import verify, model_status

app = Flask(__name__)


@app.get("/health")
def health():
    return jsonify(ok=True, steps=list(C.STEPS), models=model_status())


@app.post("/verify")
def verify_route():
    step = request.form.get("step", "")
    if step not in C.STEPS:
        return jsonify(ok=False, error=f"unknown step '{step}'"), 400
    f = request.files.get("image")
    if f is None:
        return jsonify(ok=False, error="no image file (field 'image')"), 400
    try:
        attempt = int(request.form.get("attempt", 0))
        from PIL import Image
        image = Image.open(io.BytesIO(f.read()))
        result = verify(step, image, attempt=attempt)
        return jsonify(ok=True, **result)
    except Exception as exc:  # noqa: BLE001
        return jsonify(ok=False, error=str(exc)), 500


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=int(os.environ.get("PORT", 8010)))
