#!/bin/bash
# QA round-1 reproduction: reinstall, entry / sample / walk / export screenshots, file + JSON dumps.
# Usage: bash docs/scripts/qa1.sh   (run from the project root in Git Bash)
set -u
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="$ROOT/docs/screenshots/qa1"
LOG="$OUT/qa1-run.log"
mkdir -p "$OUT" "$ROOT/docs/export"
exec > >(tee "$LOG") 2>&1
echo "== $(date) install"
adb install -r "$ROOT/app/build/outputs/apk/debug/app-debug.apk"

echo "== entry (no demo setting)"
adb shell settings delete global gridspace_demo
adb shell am force-stop com.kejing.gridspace
adb logcat -c
pico-cli app launch com.kejing.gridspace >/dev/null 2>&1
sleep 10
adb exec-out screencap -p > "$OUT/qa1-entry.png"

echo "== sample"
adb shell am force-stop com.kejing.gridspace
adb shell settings put global gridspace_demo sample
pico-cli app launch com.kejing.gridspace >/dev/null 2>&1
sleep 20
adb exec-out screencap -p > "$OUT/qa1-editor.png"

echo "== walk"
bash "$ROOT/docs/scripts/walk.sh" "$OUT" qa1
sleep 3
adb exec-out screencap -p > "$OUT/qa1-after-walk.png"
echo "== logcat after walk"
adb logcat -d | grep -iE "AndroidRuntime|FATAL|ANR |gridspace" | grep -v "GridSpaceDemo" | tail -60 > "$OUT/qa1-logcat-walk.txt"
adb logcat -d -s GridSpaceDemo:I > "$OUT/qa1-demo-steps.txt"

echo "== export script"
adb shell am force-stop com.kejing.gridspace
adb shell settings put global gridspace_demo export
adb logcat -c
pico-cli app launch com.kejing.gridspace >/dev/null 2>&1
sleep 25
adb exec-out screencap -p > "$OUT/qa1-entry-after-export.png"
MSYS_NO_PATHCONV=1 adb shell ls -la /sdcard/Download/GridSpace/ > "$OUT/qa1-download-ls.txt"
cat "$OUT/qa1-download-ls.txt"
NEWEST=$(MSYS_NO_PATHCONV=1 adb shell "ls -t /sdcard/Download/GridSpace/ | head -1" | tr -d '\r')
echo "newest: $NEWEST"
MSYS_NO_PATHCONV=1 adb pull "/sdcard/Download/GridSpace/$NEWEST" "$ROOT/docs/export/qa1-newest.vox"
adb logcat -d | grep -iE "AndroidRuntime|FATAL|ANR |gridspace" | grep -v "GridSpaceDemo" | tail -40 > "$OUT/qa1-logcat-export.txt"

echo "== project json"
adb shell run-as com.kejing.gridspace ls -la files/projects/ > "$OUT/qa1-files-ls.txt"
cat "$OUT/qa1-files-ls.txt"
adb shell run-as com.kejing.gridspace cat files/projects/index.json > "$OUT/qa1-index.json"
for f in $(adb shell run-as com.kejing.gridspace ls files/projects/ | tr -d '\r' | grep -v index.json | grep '\.json$'); do
  adb shell run-as com.kejing.gridspace cat "files/projects/$f" > "$OUT/qa1-project-$f"
done

echo "== cleanup"
adb shell settings delete global gridspace_demo
adb shell settings get global gridspace_demo
MSYS_NO_PATHCONV=1 adb shell ls -la /data/anr/ > "$OUT/qa1-anr-ls.txt"
cat "$OUT/qa1-anr-ls.txt"
echo "== $(date) done"
