#!/bin/bash
# One full capture round for one language: reinstall, set ${APP}_lang, cold-start entry shot, the
# pre-scripts (ack-synced), the workbench, the full walk with every marker, the export script
# (closes the editor; the entry window reopens), device data pulls, log dumps, cleanup.
# Output: <out-root>/<lang>/<prefix>-*.png + <prefix>-run.log (every cap.sh record; append any
# manual re-capture with `bash cap.sh <png> 5 | tee -a <out>/<prefix>-run.log`).
# Usage: round.sh <prefix> [lang|-] [out-root=docs/screenshots/<prefix>]   (Git Bash, project root)
# Source: GridSpace docs/scripts/r8.sh (R8 full set), generalised with app.env.
set -u
P="${1:?prefix, e.g. r1}"
LANG_CODE="${2:--}"
HERE="$(cd "$(dirname "$0")" && pwd)"
. "$HERE/app.env" || exit 2
ROOT="$(cd "$HERE/../.." && pwd)"
OUT="${3:-$ROOT/docs/screenshots/$P}"; [ "$LANG_CODE" != "-" ] && OUT="$OUT/$LANG_CODE"
mkdir -p "$OUT" "$ROOT/docs/export"
LOG="$OUT/$P-run.log"
exec > >(tee "$LOG") 2>&1
TAGS="$DEMO_TAG:I $APP_TAGS"

cleanup_settings() {
  adb shell settings delete global "${APP}_demo" >/dev/null 2>&1
  adb shell settings delete global "${APP}_demo_ack" >/dev/null 2>&1
  for s in $PLACEMENT_SETTINGS; do adb shell settings delete global "${APP}_$s" >/dev/null 2>&1; done
}
crash_log() {  # $1 = out file; the system SysPerfMonitorService.getSysFatal stack is not ours
  adb logcat -d | grep -iE "AndroidRuntime|FATAL|ANR " | grep -v "$DEMO_TAG" | grep -v SysPerfMonitorService | tail -60 > "$1"
}

echo "== $(date) install ($LANG_CODE)"
for p in $STOP_OTHERS; do adb shell am force-stop "$p" >/dev/null 2>&1; done
adb shell am force-stop "$PKG"
adb install -r "$ROOT/app/build/outputs/apk/debug/app-debug.apk"
cleanup_settings
[ "$LANG_CODE" != "-" ] && adb shell settings put global "${APP}_lang" "$LANG_CODE"

echo "== entry (cold start, no demo setting)"
adb logcat -c
pico-cli app launch "$PKG" >/dev/null 2>&1
sleep "$COLD_START_S"   # window black / half drawn for ~25 s after an install
bash "$HERE/cap.sh" "$OUT/$P-entry.png" "$CAP_TRIES"

for s in $PRE_SCRIPTS; do
  echo "== pre-script $s"
  bash "$HERE/walk.sh" "$OUT" "$P" "$s"
done

if [ -n "${WORKBENCH_SCRIPT:-}" ]; then
  echo "== workbench ($WORKBENCH_SCRIPT)"
  adb shell am force-stop "$PKG"
  adb shell settings put global "${APP}_demo" "$WORKBENCH_SCRIPT"
  pico-cli app launch "$PKG" >/dev/null 2>&1
  sleep "$WORKBENCH_S"   # scaled side cards' glass settles 30-40 s after a cold launch
  bash "$HERE/cap.sh" "$OUT/$P-workbench.png" "$CAP_TRIES"
fi

echo "== walk ($WALK_SCRIPT)"
bash "$HERE/walk.sh" "$OUT" "$P" "$WALK_SCRIPT"
crash_log "$OUT/$P-logcat-walk.txt"
adb logcat -d -v time -s $TAGS > "$OUT/$P-demo-steps.txt"
if [ -n "${EXPORT_DIR:-}" ]; then
  WALKFILE=$(MSYS_NO_PATHCONV=1 adb shell "ls -t $EXPORT_DIR/ | head -1" | tr -d '\r')
  echo "walk export: $WALKFILE"
  [ -n "$WALKFILE" ] && MSYS_NO_PATHCONV=1 adb pull "$EXPORT_DIR/$WALKFILE" "$(cygpath -w "$ROOT/docs/export/$P-$LANG_CODE-walk.$EXPORT_EXT")"
fi

if [ -n "${EXPORT_SCRIPT:-}" ]; then
  echo "== export script (closes the editor; the entry window reopens)"
  adb shell am force-stop "$PKG"
  adb shell settings put global "${APP}_demo" "$EXPORT_SCRIPT"
  adb logcat -c
  pico-cli app launch "$PKG" >/dev/null 2>&1
  sleep "$EXPORT_S"
  bash "$HERE/cap.sh" "$OUT/$P-entry-after.png" "$CAP_TRIES"
  adb logcat -d -v time -s $TAGS FWKWrapper:I > "$OUT/$P-export-log.txt"
  if [ -n "${EXPORT_DIR:-}" ]; then
    MSYS_NO_PATHCONV=1 adb shell ls -la "$EXPORT_DIR/" > "$OUT/$P-download-ls.txt"
    NEWEST=$(MSYS_NO_PATHCONV=1 adb shell "ls -t $EXPORT_DIR/ | head -1" | tr -d '\r')
    echo "newest: $NEWEST"
    [ -n "$NEWEST" ] && MSYS_NO_PATHCONV=1 adb pull "$EXPORT_DIR/$NEWEST" "$(cygpath -w "$ROOT/docs/export/$P-$LANG_CODE-newest.$EXPORT_EXT")"
  fi
  crash_log "$OUT/$P-logcat-export.txt"
fi

if [ -n "${PRIVATE_DIR:-}" ]; then
  echo "== app-private data"
  adb shell run-as "$PKG" ls -la "$PRIVATE_DIR/" > "$OUT/$P-files-ls.txt"
  [ -n "${PRIVATE_INDEX:-}" ] && adb shell run-as "$PKG" cat "$PRIVATE_DIR/$PRIVATE_INDEX" > "$OUT/$P-index.json"
fi

echo "== cleanup"
adb shell am force-stop "$PKG"
cleanup_settings
adb shell settings delete global "${APP}_lang" >/dev/null 2>&1
adb shell settings list global | grep "^${APP}_" || echo "no ${APP} settings left"
MSYS_NO_PATHCONV=1 adb shell ls -la /data/anr/ > "$OUT/$P-anr-ls.txt"
cat "$OUT/$P-anr-ls.txt"
echo "== $(date) done"
