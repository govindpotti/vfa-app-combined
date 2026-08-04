"""
Model backbone for the step verifier.

Transfer learning from a MobileNetV3-Small (small, fast, mobile-friendly — good for a cloud
endpoint now and on-device later). One lightweight classifier per step sits on top of the
pretrained ImageNet features; with only a few hundred images per class this trains quickly and
generalizes far better than training from scratch.
"""
import json
import os
from typing import List, Tuple

import torch
import torch.nn as nn
from torchvision import models


def build_model(num_classes: int, pretrained: bool = True, freeze_backbone: bool = True) -> nn.Module:
    weights = models.MobileNet_V3_Small_Weights.IMAGENET1K_V1 if pretrained else None
    net = models.mobilenet_v3_small(weights=weights)
    if freeze_backbone:
        for p in net.features.parameters():
            p.requires_grad = False
    in_features = net.classifier[-1].in_features
    net.classifier[-1] = nn.Linear(in_features, num_classes)
    return net


def unfreeze_backbone(net: nn.Module) -> None:
    for p in net.parameters():
        p.requires_grad = True


def save_checkpoint(net: nn.Module, out_dir: str, class_names: List[str], input_size: int,
                    min_confidence: float, extra: dict = None) -> None:
    os.makedirs(out_dir, exist_ok=True)
    torch.save(net.state_dict(), os.path.join(out_dir, "model.pt"))
    labels = {
        "class_names": class_names,
        "input_size": input_size,
        "min_confidence": min_confidence,
        "arch": "mobilenet_v3_small",
    }
    if extra:
        labels.update(extra)
    with open(os.path.join(out_dir, "labels.json"), "w") as f:
        json.dump(labels, f, indent=2)


class StepVerifier:
    """Loads a trained step model and runs inference. Thread-safe for read-only predict()."""

    def __init__(self, model_dir: str, device: str = "cpu"):
        with open(os.path.join(model_dir, "labels.json")) as f:
            self.meta = json.load(f)
        self.class_names: List[str] = self.meta["class_names"]
        self.input_size: int = self.meta.get("input_size", 224)
        self.min_confidence: float = self.meta.get("min_confidence", 0.55)
        self.device = torch.device(device)
        self.net = build_model(len(self.class_names), pretrained=False, freeze_backbone=False)
        self.net.load_state_dict(torch.load(os.path.join(model_dir, "model.pt"), map_location=self.device))
        self.net.eval().to(self.device)

        from dataset import eval_transforms  # local import to avoid heavy deps at import time
        self._tf = eval_transforms(self.input_size)

    @torch.no_grad()
    def predict(self, pil_image) -> Tuple[str, float, List[float]]:
        """Return (top_class_name, top_confidence, all_probs)."""
        x = self._tf(pil_image.convert("RGB")).unsqueeze(0).to(self.device)
        probs = torch.softmax(self.net(x), dim=1)[0]
        conf, idx = torch.max(probs, dim=0)
        return self.class_names[int(idx)], float(conf), [float(p) for p in probs]
