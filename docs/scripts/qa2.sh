#!/bin/bash
# QA round-2 reproduction: reinstall, entry / sample / walk / export screenshots, file + JSON dumps.
# Usage: bash docs/scripts/qa2.sh [out-dir=docs/screenshots/qa2] [prefix=qa2]   (Git Bash, project root)
set -u
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="${1:-$ROOT/docs/screenshots/qa2}"
P="${2:-qa2}"
LOG="$OUT/$P-run.log"
mkdir -p "$OUT" "$ROOT/docs/export"
exec > >(tee "$LOG") 2>&1
echo "== $(date) install"
adb install -r "$ROOT/app/build/outputs/apk/debug/app-debug.apk"

echo "== entry (no demo setting)"
adb shell settings delete global gridspace_demo
adb shell settings delete global gridspace_demo_ack
adb shell settings delete global gridspace_brush
adb shell am force-stop com.kejing.gridspace
adb logcat -c
pico-cli app launch com.kejing.gridspace >/dev/null 2>&1
sleep 12
bash "$ROOT/docs/scripts/cap.sh" "$OUT/$P-entry.png" 5

echo "== sample"
adb shell am force-stop com.kejing.gridspace
adb shell settings put global gridspace_demo sample
pico-cli app launch com.kejing.gridspace >/dev/null 2>&1
sleep 20
bash "$ROOT/docs/scripts/cap.sh" "$OUT/$P-editor.png" 5

echo "== walk"
bash "$ROOT/docs/scripts/walk.sh" "$OUT" "$P"
echo "== logcat after walk"
adb logcat -d | grep -iE "AndroidRuntime|FATAL|ANR " | grep -v "GridSpaceDemo" | tail -60 > "$OUT/$P-logcat-walk.txt"
adb logcat -d -v time -s GridSpaceDemo:I > "$OUT/$P-demo-steps.txt"

echo "== export script (closes the editor; entry list shows the project row)"
adb shell am force-stop com.kejing.gridspace
adb shell settings put global gridspace_demo export
adb logcat -c
pico-cli app launch com.kejing.gridspace >/dev/null 2>&1
sleep 25
bash "$ROOT/docs/scripts/cap.sh" "$OUT/$P-entry-after-export.png" 5
MSYS_NO_PATHCONV=1 adb shell ls -la /sdcard/Download/GridSpace/ > "$OUT/$P-download-ls.txt"
cat "$OUT/$P-download-ls.txt"
NEWEST=$(MSYS_NO_PATHCONV=1 adb shell "ls -t /sdcard/Download/GridSpace/ | head -1" | tr -d '\r')
echo "newest: $NEWEST"
MSYS_NO_PATHCONV=1 adb pull "/sdcard/Download/GridSpace/$NEWEST" "$(cygpath -w "$ROOT/docs/export/$P-newest.vox")"
adb logcat -d | grep -iE "AndroidRuntime|FATAL|ANR " | grep -v "GridSpaceDemo" | tail -40 > "$OUT/$P-logcat-export.txt"

echo "== project json"
adb shell run-as com.kejing.gridspace ls -la files/projects/ > "$OUT/$P-files-ls.txt"
adb shell run-as com.kejing.gridspace cat files/projects/index.json > "$OUT/$P-index.json"
for f in $(adb shell run-as com.kejing.gridspace ls files/projects/ | tr -d '\r' | grep -v index.json | grep '\.json$'); do
  adb shell run-as com.kejing.gridspace cat "files/projects/$f" > "$OUT/$P-project-$f"
done

echo "== cleanup"
adb shell settings delete global gridspace_demo
adb shell settings delete global gridspace_demo_ack
adb shell settings delete global gridspace_brush
adb shell settings get global gridspace_demo
MSYS_NO_PATHCONV=1 adb shell ls -la /data/anr/ > "$OUT/$P-anr-ls.txt"
cat "$OUT/$P-anr-ls.txt"
echo "== $(date) done"
