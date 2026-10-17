#!/bin/bash
# R5 capture for one language (spec K4.34 + S): reinstall, set gridspace_lang, entry shot, the
# new-project script (Soft page, then Vivid page for the #FFFFFF matching ring, QA N4-06), sample
# workbench, the full walk with every mark, the export script (closes the editor; branch A reopens
# the entry window), cleanup. Output: docs/screenshots/r5/<lang>/r5-*.png. Everything, including any
# later re-capture (`bash docs/scripts/cap.sh <png> 5 | tee -a <out>/r5-run.log`, QA N4-09), goes
# through r5-run.log.
# Usage: bash docs/scripts/r5.sh zh|en [out-root=docs/screenshots/r5]   (Git Bash, project root)
set -u
LANG_CODE="${1:?zh|en}"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="${2:-$ROOT/docs/screenshots/r5}/$LANG_CODE"
P="r5"
S="$ROOT/docs/scripts"
mkdir -p "$OUT" "$ROOT/docs/export"
LOG="$OUT/$P-run.log"
exec > >(tee "$LOG") 2>&1

cleanup_settings() {
  for s in gridspace_demo gridspace_demo_ack gridspace_brush gridspace_rail gridspace_selpanel gridspace_popup gridspace_probe; do
    adb shell settings delete global "$s" >/dev/null 2>&1
  done
}

echo "== $(date) install ($LANG_CODE)"
adb shell am force-stop com.kejing.skyboxspatial >/dev/null 2>&1
adb shell am force-stop com.kejing.gridspace
adb install -r "$ROOT/app/build/outputs/apk/debug/app-debug.apk"
cleanup_settings
adb shell settings put global gridspace_lang "$LANG_CODE"

echo "== entry"
adb logcat -c
pico-cli app launch com.kejing.gridspace >/dev/null 2>&1
sleep 40   # cold start right after the install shows the window black / half drawn for ~25 s (AGENTS, R5)
bash "$S/cap.sh" "$OUT/$P-entry.png" 5

echo "== newproject (white default slot, K4.42; Soft page then Vivid page, N4-06)"
bash "$S/walk.sh" "$OUT" "$P" newproject

echo "== sample workbench"
adb shell am force-stop com.kejing.gridspace
adb shell settings put global gridspace_demo sample
pico-cli app launch com.kejing.gridspace >/dev/null 2>&1
sleep 45   # R5: the scaled cards' glass settles 30-40 s after a cold launch (docs/screenshots/r5/probe/settle2-*.png, crop-brush.png)
bash "$S/cap.sh" "$OUT/$P-workbench.png" 5

echo "== walk"
bash "$S/walk.sh" "$OUT" "$P"
cp -f "$OUT/$P-vivid.png" "$OUT/$P-rail.png" 2>/dev/null
if [ "$LANG_CODE" = "zh" ]; then cp -f "$OUT/$P-lang.png" "$OUT/$P-lang-toggle.png" 2>/dev/null; fi
adb logcat -d | grep -iE "AndroidRuntime|FATAL|ANR " | grep -v "GridSpaceDemo" | tail -60 > "$OUT/$P-logcat-walk.txt"
adb logcat -d -v time -s GridSpaceDemo:I GridSpaceEditor:I GridSpaceEntry:W > "$OUT/$P-demo-steps.txt"

echo "== export script (closes the editor; the entry window reopens)"
adb shell am force-stop com.kejing.gridspace
adb shell settings put global gridspace_demo export
adb logcat -c
pico-cli app launch com.kejing.gridspace >/dev/null 2>&1
sleep 28
bash "$S/cap.sh" "$OUT/$P-entry-after.png" 5
adb logcat -d -v time -s GridSpaceDemo:I GridSpaceEditor:I GridSpaceEntry:W FWKWrapper:I > "$OUT/$P-export-log.txt"
MSYS_NO_PATHCONV=1 adb shell ls -la /sdcard/Download/GridSpace/ > "$OUT/$P-download-ls.txt"
NEWEST=$(MSYS_NO_PATHCONV=1 adb shell "ls -t /sdcard/Download/GridSpace/ | head -1" | tr -d '\r')
echo "newest: $NEWEST"
MSYS_NO_PATHCONV=1 adb pull "/sdcard/Download/GridSpace/$NEWEST" "$(cygpath -w "$ROOT/docs/export/$P-$LANG_CODE-newest.vox")"
adb logcat -d | grep -iE "AndroidRuntime|FATAL|ANR " | grep -v "GridSpaceDemo" | tail -40 > "$OUT/$P-logcat-export.txt"
adb shell run-as com.kejing.gridspace cat files/projects/index.json > "$OUT/$P-index.json"

echo "== cleanup"
adb shell am force-stop com.kejing.gridspace
cleanup_settings
adb shell settings delete global gridspace_lang >/dev/null 2>&1
adb shell settings list global | grep gridspace || echo "no gridspace settings left"
MSYS_NO_PATHCONV=1 adb shell ls -la /data/anr/ > "$OUT/$P-anr-ls.txt"
cat "$OUT/$P-anr-ls.txt"
echo "== $(date) done"
