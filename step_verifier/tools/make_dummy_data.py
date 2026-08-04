"""
Generate throwaway images so the training pipeline can be exercised before real data exists.

Each class gets random-noise images with a faint class-specific tint, so the model can even
reach decent accuracy on the dummy set — proving train -> save -> infer -> serve all work.
DELETE data/ before adding real footage.
"""
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import config as C


def make_dummy(data_root: str, steps=None, per_class: int = 40, size: int = 224):
    from PIL import Image
    import numpy as np

    steps = steps or list(C.STEPS)
    for sid in steps:
        spec = C.get_step(sid)
        for ci, cls in enumerate(spec.class_names):
            out = os.path.join(data_root, sid, cls)
            os.makedirs(out, exist_ok=True)
            # A faint per-class tint gives the classifier a learnable (fake) signal.
            tint = np.array([(ci * 53) % 256, (ci * 97) % 256, (ci * 151) % 256], dtype=np.float32)
            for i in range(per_class):
                noise = np.random.randint(0, 256, (size, size, 3)).astype(np.float32)
                arr = (0.6 * noise + 0.4 * tint).clip(0, 255).astype("uint8")
                Image.fromarray(arr).save(os.path.join(out, f"dummy_{i:03d}.jpg"))
        print(f"[{sid}] dummy data -> {os.path.join(data_root, sid)}")


if __name__ == "__main__":
    root = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "data")
    make_dummy(root)
