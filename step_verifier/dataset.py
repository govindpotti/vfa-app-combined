"""
Data loading for the step verifier.

Expected layout (you create this as you collect footage/photos):

    step_verifier/data/<step_id>/<class_name>/*.jpg|png

e.g.
    data/assemble/ok/img001.jpg
    data/assemble/not_assembled/img014.jpg
    data/first_wash/no_liquid/img002.jpg

Capture tips for good models: shoot from the same rough angle/distance the app checkpoint
uses (phone held over the cassette), vary lighting, backgrounds, hands in frame, and phones.
Aim for a few hundred images per class to start; augmentation stretches a small set.
"""
import os
from typing import Tuple, List

import torch
from torch.utils.data import DataLoader, random_split
from torchvision import datasets, transforms

# ImageNet stats — the pretrained backbones expect these.
_MEAN = [0.485, 0.456, 0.406]
_STD = [0.229, 0.224, 0.225]


def train_transforms(size: int) -> transforms.Compose:
    # Augmentation makes the model robust to real-world framing/lighting from many phones.
    return transforms.Compose([
        transforms.RandomResizedCrop(size, scale=(0.7, 1.0)),
        transforms.RandomHorizontalFlip(),
        transforms.RandomRotation(12),
        transforms.ColorJitter(brightness=0.25, contrast=0.25, saturation=0.2, hue=0.03),
        transforms.RandomApply([transforms.GaussianBlur(3)], p=0.2),
        transforms.ToTensor(),
        transforms.Normalize(_MEAN, _STD),
    ])


def eval_transforms(size: int) -> transforms.Compose:
    return transforms.Compose([
        transforms.Resize(int(size * 1.15)),
        transforms.CenterCrop(size),
        transforms.ToTensor(),
        transforms.Normalize(_MEAN, _STD),
    ])


def _data_dir(root: str, step_id: str) -> str:
    return os.path.join(root, step_id)


def has_data(root: str, step_id: str) -> bool:
    d = _data_dir(root, step_id)
    if not os.path.isdir(d):
        return False
    for cls in os.listdir(d):
        cdir = os.path.join(d, cls)
        if os.path.isdir(cdir) and any(
            f.lower().endswith((".jpg", ".jpeg", ".png")) for f in os.listdir(cdir)
        ):
            return True
    return False


def build_dataloaders(
    root: str, step_id: str, size: int, batch_size: int = 32,
    val_split: float = 0.2, num_workers: int = 2, seed: int = 42,
) -> Tuple[DataLoader, DataLoader, List[str], List[int]]:
    """Return (train_loader, val_loader, class_names, train_class_counts)."""
    d = _data_dir(root, step_id)
    full = datasets.ImageFolder(d)  # classes = sorted subfolder names
    class_names = full.classes

    n_val = max(1, int(len(full) * val_split))
    n_train = len(full) - n_val
    g = torch.Generator().manual_seed(seed)
    train_ds, val_ds = random_split(full, [n_train, n_val], generator=g)

    # Apply the right transform per split (ImageFolder holds one transform, so wrap).
    train_ds.dataset_transform = train_transforms(size)
    val_ds.dataset_transform = eval_transforms(size)
    train_ds = _Transformed(train_ds, train_transforms(size))
    val_ds = _Transformed(val_ds, eval_transforms(size))

    # Class counts for weighted loss (imbalance is common with hand-collected data).
    counts = [0] * len(class_names)
    for _, label in [full.samples[i] for i in train_ds.indices]:
        counts[label] += 1

    train_loader = DataLoader(train_ds, batch_size=batch_size, shuffle=True, num_workers=num_workers)
    val_loader = DataLoader(val_ds, batch_size=batch_size, shuffle=False, num_workers=num_workers)
    return train_loader, val_loader, class_names, counts


class _Transformed(torch.utils.data.Dataset):
    """Wrap a random_split Subset so we can apply split-specific transforms."""
    def __init__(self, subset, transform):
        self.subset = subset
        self.transform = transform
        self.indices = subset.indices

    def __len__(self):
        return len(self.subset)

    def __getitem__(self, i):
        img, label = self.subset.dataset.samples[self.subset.indices[i]]
        from PIL import Image
        image = Image.open(img).convert("RGB")
        return self.transform(image), label
