#!/usr/bin/env bash
set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
LOG_DIR="$REPO_DIR/engine/build/native-logs"
ABI="${1:-arm64-v8a}"
LIVE_LOG="$LOG_DIR/live-$ABI.log"
PID_FILE="$LOG_DIR/live-$ABI.pid"

mkdir -p "$LOG_DIR"
rm -f "$LIVE_LOG" "$PID_FILE"

nohup bash -c 'cd "$1"; exec bash engine/src/main/jni/build_wsl.sh "$2"' \
  mkxp-wsl-build "$REPO_DIR" "$ABI" > "$LIVE_LOG" 2>&1 &

echo "$!" > "$PID_FILE"
echo "started pid=$(cat "$PID_FILE") log=$LIVE_LOG"
