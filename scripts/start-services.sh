#!/usr/bin/env bash

set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh"

ANALYZER_PORT="${ANALYZER_PORT:-8001}"
VERIFIER_PORT="${VERIFIER_PORT:-8010}"
START_VERIFIER="${START_VERIFIER:-1}"
HOST_IP="$(detect_host_ip)"

ensure_dirs

echo "Setting up analyzer Python environment..."
if [[ ! -x "$ROOT_DIR/server/.venv/bin/python" ]]; then
    python3 -m venv "$ROOT_DIR/server/.venv"
fi
"$ROOT_DIR/server/.venv/bin/python" -m pip install -q --upgrade pip
"$ROOT_DIR/server/.venv/bin/pip" install -q -r "$ROOT_DIR/server/requirements.txt"

stop_pidfile "$PID_DIR/analyzer.pid"
(
    cd "$ROOT_DIR"
    nohup env PORT="$ANALYZER_PORT" "$ROOT_DIR/server/.venv/bin/python" \
        "$ROOT_DIR/server/app.py" > "$LOG_DIR/analyzer.log" 2>&1 &
    echo $! > "$PID_DIR/analyzer.pid"
)

if [[ "$START_VERIFIER" == "1" ]]; then
    echo "Setting up verifier Python environment..."
    if [[ ! -x "$ROOT_DIR/step_verifier/.venv/bin/python" ]]; then
        python3 -m venv "$ROOT_DIR/step_verifier/.venv"
    fi
    "$ROOT_DIR/step_verifier/.venv/bin/python" -m pip install -q --upgrade pip
    "$ROOT_DIR/step_verifier/.venv/bin/pip" install -q -r "$ROOT_DIR/step_verifier/requirements.txt"

    stop_pidfile "$PID_DIR/verifier.pid"
    (
        cd "$ROOT_DIR/step_verifier"
        nohup "$ROOT_DIR/step_verifier/.venv/bin/python" serve.py \
            > "$LOG_DIR/verifier.log" 2>&1 &
        echo $! > "$PID_DIR/verifier.pid"
    )
fi

wait_for_health "http://127.0.0.1:$ANALYZER_PORT/health" "Analyzer"
if [[ "$START_VERIFIER" == "1" ]]; then
    wait_for_health "http://127.0.0.1:$VERIFIER_PORT/health" "Verifier"
fi

echo
echo "Services are running."
echo "Analyzer URL: http://$HOST_IP:$ANALYZER_PORT"
if [[ "$START_VERIFIER" == "1" ]]; then
    echo "Verifier URL: http://$HOST_IP:$VERIFIER_PORT"
fi
echo
echo "On the LG browser, check:"
echo "  http://$HOST_IP:$ANALYZER_PORT/health"
echo
echo "Logs:"
echo "  $LOG_DIR/analyzer.log"
if [[ "$START_VERIFIER" == "1" ]]; then
    echo "  $LOG_DIR/verifier.log"
fi

