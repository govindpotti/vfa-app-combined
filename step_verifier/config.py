"""
Step-verifier configuration — the single source of truth for what the checkpoint AI checks.

Each guided step that opens the camera has a StepSpec here: the classes the model predicts
(one per folder under data/<step_id>/), and how each class maps to the app's checkpoint
contract — Pass / Retry(reason) / FlagForHelp — so the phone shows the right guidance.

To add/adjust a check: edit STEPS below, create matching folders under data/<step_id>/<class>,
add images, and retrain that step (see README).
"""
from dataclasses import dataclass
from typing import List, Dict, Optional

# Status values map 1:1 to the app's CheckpointResult:
#   'pass'  -> Pass(confidence)               (green check, advance)
#   'retry' -> Retry(reason, confidence)      (amber "Almost there" + Try again)
#   'help'  -> FlagForHelp(reason)            (escalate; also used on low confidence)
STATUS_PASS = "pass"
STATUS_RETRY = "retry"
STATUS_HELP = "help"


@dataclass(frozen=True)
class StepClass:
    name: str            # folder name under data/<step_id>/<name>
    status: str          # STATUS_PASS | STATUS_RETRY | STATUS_HELP
    reason: str = ""     # shown + spoken when not a pass
    speech: str = ""     # optional distinct TTS line (defaults to reason)


@dataclass(frozen=True)
class StepSpec:
    id: str
    title: str
    classes: List[StepClass]
    input_size: int = 224
    # If the top class' probability is below this, escalate to 'help' rather than trust it.
    min_confidence: float = 0.55
    # After this many failed attempts on one step, escalate a 'retry' to 'help'.
    escalate_after_attempts: int = 3

    @property
    def class_names(self) -> List[str]:
        return [c.name for c in self.classes]

    def by_name(self, name: str) -> Optional[StepClass]:
        for c in self.classes:
            if c.name == name:
                return c
        return None


# ── The checkpoints, in flow order ──────────────────────────────────────────────
# 'ok' is always the pass class. Failure classes carry the guidance the user hears.
STEPS: Dict[str, StepSpec] = {
    "kit": StepSpec(
        id="kit",
        title="Materials check",
        classes=[
            StepClass("ok", STATUS_PASS),
            StepClass("missing_items", STATUS_RETRY,
                      "I can't see every item — lay all of them flat inside the frame and try again."),
            StepClass("obscured", STATUS_RETRY,
                      "Something's blocking the view. Spread the items out with nothing on top."),
        ],
    ),
    "assemble": StepSpec(
        id="assemble",
        title="Assemble the case",
        classes=[
            StepClass("ok", STATUS_PASS),
            StepClass("not_assembled", STATUS_RETRY,
                      "The top and bottom cases aren't joined yet — press and twist them together until flat."),
            StepClass("misaligned", STATUS_RETRY,
                      "The cases look crooked. Line up the well and twist until there's no gap around the edge."),
        ],
    ),
    "add_buffer": StepSpec(
        id="add_buffer",
        title="Add running buffer",
        classes=[
            StepClass("ok", STATUS_PASS),
            StepClass("not_soaked", STATUS_RETRY,
                      "The buffer hasn't gone all the way down yet — give it a few more seconds, then try again."),
            StepClass("no_liquid", STATUS_RETRY,
                      "I don't see any buffer in the well — add the running buffer."),
            StepClass("overfilled", STATUS_RETRY,
                      "There's liquid spilling outside the well. Wipe the edge and let the well drain."),
        ],
    ),
    "add_sample": StepSpec(
        id="add_sample",
        title="Add your sample",
        classes=[
            StepClass("ok", STATUS_PASS),
            StepClass("not_soaked", STATUS_RETRY,
                      "The sample hasn't gone all the way down yet — wait a moment and try again."),
            StepClass("no_liquid", STATUS_RETRY,
                      "The well looks empty — add your prepared sample."),
            StepClass("wrong_well", STATUS_HELP,
                      "That doesn't look like the sample well. Let's get some help to be sure."),
        ],
    ),
    "swap_case": StepSpec(
        id="swap_case",
        title="Swap the top case",
        classes=[
            StepClass("ok", STATUS_PASS),
            StepClass("old_case", STATUS_RETRY,
                      "This still looks like the used top case — swap it for a new, clean one."),
            StepClass("not_seated", STATUS_RETRY,
                      "The new top case isn't fully seated — twist it down until it's flat with no gap."),
        ],
    ),
    "add_gold": StepSpec(
        id="add_gold",
        title="Add gold solution",
        classes=[
            StepClass("ok", STATUS_PASS),
            StepClass("not_soaked", STATUS_RETRY,
                      "The gold solution hasn't gone all the way down yet — give it a few more seconds."),
            StepClass("no_liquid", STATUS_RETRY,
                      "I don't see the gold solution in the well — add it now."),
        ],
    ),
}


def get_step(step_id: str) -> StepSpec:
    if step_id not in STEPS:
        raise KeyError(f"Unknown step '{step_id}'. Known: {list(STEPS)}")
    return STEPS[step_id]
