"""
Export a trained step model to ONNX (portable; also the bridge to on-device TFLite later).

    python export.py --step assemble

Produces models/<step>/model.onnx. For on-device Android inference, convert ONNX -> TFLite
with onnx2tf / onnxruntime, or retrain-export via the LiteRT path (see README).
"""
import argparse
import os
import sys

import torch

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from model import StepVerifier

ROOT = os.path.dirname(os.path.abspath(__file__))


def export_step(step_id: str):
    model_dir = os.path.join(ROOT, "models", step_id)
    v = StepVerifier(model_dir)
    dummy = torch.randn(1, 3, v.input_size, v.input_size)
    out = os.path.join(model_dir, "model.onnx")
    torch.onnx.export(
        v.net, dummy, out,
        input_names=["image"], output_names=["logits"],
        dynamic_axes={"image": {0: "batch"}, "logits": {0: "batch"}},
        opset_version=17,
    )
    print(f"[{step_id}] exported -> {out}  classes={v.class_names}")


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--step", required=True)
    export_step(ap.parse_args().step)
