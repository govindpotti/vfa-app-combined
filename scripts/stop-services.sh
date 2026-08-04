#!/usr/bin/env bash

set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh"

ensure_dirs
stop_pidfile "$PID_DIR/analyzer.pid"
stop_pidfile "$PID_DIR/verifier.pid"

echo "Stopped VFA analyzer/verifier services."

