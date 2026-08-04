"""
Train a step-verifier model.

    python train.py --step assemble          # train one step from data/assemble/*
    python train.py --all                     # train every step that has data
    python train.py --step assemble --smoke   # generate dummy data + train (pipeline smoke test)

Outputs models/<step>/model.pt + labels.json, used by infer.py / serve.py.
"""
import argparse
import os
import sys

import torch
import torch.nn as nn

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import config as C
from dataset import build_dataloaders, has_data
from model import build_model, unfreeze_backbone, save_checkpoint

ROOT = os.path.dirname(os.path.abspath(__file__))
DATA_ROOT = os.path.join(ROOT, "data")
MODELS_ROOT = os.path.join(ROOT, "models")


def evaluate(net, loader, device):
    net.eval()
    correct = total = 0
    per_class_correct, per_class_total = {}, {}
    with torch.no_grad():
        for x, y in loader:
            x, y = x.to(device), y.to(device)
            pred = net(x).argmax(1)
            correct += (pred == y).sum().item()
            total += y.numel()
            for t, p in zip(y.tolist(), pred.tolist()):
                per_class_total[t] = per_class_total.get(t, 0) + 1
                per_class_correct[t] = per_class_correct.get(t, 0) + int(t == p)
    acc = correct / max(1, total)
    recalls = {c: per_class_correct.get(c, 0) / per_class_total[c] for c in per_class_total}
    return acc, recalls


def train_step(step_id: str, epochs: int, lr: float, batch_size: int, device: str):
    spec = C.get_step(step_id)
    if not has_data(DATA_ROOT, step_id):
        print(f"[{step_id}] no data under {os.path.join(DATA_ROOT, step_id)} — skipping.")
        return False

    dev = torch.device(device)
    train_loader, val_loader, class_names, counts = build_dataloaders(
        DATA_ROOT, step_id, spec.input_size, batch_size=batch_size
    )
    print(f"[{step_id}] classes={class_names} train_counts={counts}")

    net = build_model(len(class_names), pretrained=True, freeze_backbone=True).to(dev)

    # Weighted loss to counter class imbalance from hand-collected data.
    weights = torch.tensor([len(counts) * (sum(counts) / (len(counts) * c)) if c else 0.0
                            for c in counts], dtype=torch.float32)
    weights = (weights / weights.sum() * len(counts)).to(dev)
    criterion = nn.CrossEntropyLoss(weight=weights)

    def run(optimizer, n_epochs, tag, best):
        best_acc, best_state, patience, bad = best, None, 4, 0
        for ep in range(n_epochs):
            net.train()
            for x, y in train_loader:
                x, y = x.to(dev), y.to(dev)
                optimizer.zero_grad()
                criterion(net(x), y).backward()
                optimizer.step()
            acc, recalls = evaluate(net, val_loader, dev)
            print(f"[{step_id}] {tag} epoch {ep+1}/{n_epochs}  val_acc={acc:.3f}  recalls={ {class_names[c]: round(r,2) for c,r in recalls.items()} }")
            if acc > best_acc:
                best_acc, best_state, bad = acc, {k: v.cpu().clone() for k, v in net.state_dict().items()}, 0
            else:
                bad += 1
                if bad >= patience:
                    print(f"[{step_id}] early stop ({tag})")
                    break
        if best_state:
            net.load_state_dict(best_state)
        return best_acc

    # Phase 1: train the head only (backbone frozen).
    head_params = [p for p in net.parameters() if p.requires_grad]
    best = run(torch.optim.Adam(head_params, lr=lr), epochs, "head", 0.0)
    # Phase 2: fine-tune the whole network at a lower LR.
    unfreeze_backbone(net)
    best = run(torch.optim.Adam(net.parameters(), lr=lr * 0.1), max(2, epochs // 2), "finetune", best)

    out_dir = os.path.join(MODELS_ROOT, step_id)
    save_checkpoint(net, out_dir, class_names, spec.input_size, spec.min_confidence,
                    extra={"val_accuracy": round(best, 4)})
    print(f"[{step_id}] saved -> {out_dir}  (best val_acc={best:.3f})")
    return True


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--step", help="step id (see config.STEPS)")
    ap.add_argument("--all", action="store_true", help="train every step with data")
    ap.add_argument("--smoke", action="store_true", help="generate dummy data first (pipeline test)")
    ap.add_argument("--epochs", type=int, default=15)
    ap.add_argument("--lr", type=float, default=1e-3)
    ap.add_argument("--batch-size", type=int, default=32)
    ap.add_argument("--device", default="cuda" if torch.cuda.is_available() else "cpu")
    args = ap.parse_args()

    if args.smoke:
        from tools.make_dummy_data import make_dummy
        make_dummy(DATA_ROOT, steps=[args.step] if args.step else list(C.STEPS))

    steps = list(C.STEPS) if args.all else ([args.step] if args.step else [])
    if not steps:
        ap.error("pass --step <id> or --all")
    trained = sum(train_step(s, args.epochs, args.lr, args.batch_size, args.device) for s in steps)
    print(f"done — trained {trained}/{len(steps)} step(s).")


if __name__ == "__main__":
    main()
