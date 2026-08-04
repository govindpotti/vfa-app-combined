#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOCAL_DIR="$ROOT_DIR/.local"
LOG_DIR="$LOCAL_DIR/logs"
PID_DIR="$LOCAL_DIR/pids"

detect_host_ip() {
    if [[ -n "${HOST_IP:-}" ]]; then
        printf '%s\n' "$HOST_IP"
        return
    fi

    local ip=""
    if command -v ipconfig >/dev/null 2>&1; then
        ip="$(ipconfig getifaddr en0 2>/dev/null || true)"
        [[ -n "$ip" ]] || ip="$(ipconfig getifaddr en1 2>/dev/null || true)"
    fi
    if [[ -z "$ip" ]] && command -v hostname >/dev/null 2>&1; then
        ip="$(hostname -I 2>/dev/null | awk '{print $1}' || true)"
    fi
    if [[ -z "$ip" ]]; then
        ip="$(python3 - <<'PY'
import socket
s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
try:
    s.connect(("8.8.8.8", 80))
    print(s.getsockname()[0])
finally:
    s.close()
PY
)"
    fi

    if [[ -z "$ip" ]]; then
        echo "Could not detect this laptop's LAN IP. Set HOST_IP manually." >&2
        exit 1
    fi
    printf '%s\n' "$ip"
}

ensure_dirs() {
    mkdir -p "$LOG_DIR" "$PID_DIR"
}

wait_for_health() {
    local url="$1"
    local name="$2"
    local i
    for i in {1..30}; do
        if curl -fsS "$url" >/dev/null 2>&1; then
            return 0
        fi
        sleep 1
    done
    echo "$name did not answer at $url" >&2
    return 1
}

stop_pidfile() {
    local pidfile="$1"
    if [[ -f "$pidfile" ]]; then
        local pid
        pid="$(cat "$pidfile")"
        if [[ -n "$pid" ]] && kill -0 "$pid" >/dev/null 2>&1; then
            kill "$pid" >/dev/null 2>&1 || true
        fi
        rm -f "$pidfile"
    fi
}

