"""
Inference: turn a captured frame + step id into the app's checkpoint decision.

verify(step_id, pil_image) -> {
    "status": "pass" | "retry" | "help" | "unavailable",
    "reason": str,            # what the app shows/speaks when not a pass
    "confidence": float,      # 0..1
    "label": str,             # predicted class name (debug)
}

'unavailable' means no trained model for that step yet — the app should fall back to its
simulated checkpoint, so the whole flow keeps working before any model is trained.
"""
import os
from functools import lru_cache
from typing import Optional

import config as C

ROOT = os.path.dirname(os.path.abspath(__file__))
MODELS_ROOT = os.path.join(ROOT, "models")


@lru_cache(maxsize=None)
def _load(step_id: str):
    model_dir = os.path.join(MODELS_ROOT, step_id)
    if not os.path.exists(os.path.join(model_dir, "model.pt")):
        return None
    from model import StepVerifier  # heavy import (torch) — only when a model exists
    return StepVerifier(model_dir)


def verify(step_id: str, pil_image, attempt: int = 0) -> dict:
    spec = C.get_step(step_id)  # raises on unknown step
    verifier = _load(step_id)
    if verifier is None:
        return {"status": "unavailable", "reason": "", "confidence": 0.0, "label": ""}

    label, conf, _ = verifier.predict(pil_image)
    step_class = spec.by_name(label)

    # Low confidence -> don't trust it; ask for help rather than wave a bad step through.
    if conf < verifier.min_confidence:
        return {"status": C.STATUS_HELP, "confidence": conf, "label": label,
                "reason": "I couldn't get a clear enough look. Let's get some help to be sure."}

    status = step_class.status if step_class else C.STATUS_HELP
    reason = (step_class.reason if step_class else "") if status != C.STATUS_PASS else ""

    # Escalate repeated retries on the same step to a help hand-off.
    if status == C.STATUS_RETRY and attempt + 1 >= spec.escalate_after_attempts:
        status = C.STATUS_HELP

    return {"status": status, "reason": reason, "confidence": conf, "label": label}


def model_status() -> dict:
    """Which steps currently have a trained model (for /health and dashboards)."""
    return {sid: os.path.exists(os.path.join(MODELS_ROOT, sid, "model.pt")) for sid in C.STEPS}
