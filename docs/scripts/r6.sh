#!/bin/bash
# R6 capture for one language: reinstall, set gridspace_lang, sample workbench, the full walk, then
# keep only the steps R6 changed (workbench ghost occupied bar box dragmove dragmove-commit edit
# eyedrop edit-close); the other frames are deleted, the run log keeps every cap.sh record.
# Usage: bash docs/scripts/r6.sh zh|en [out-root=docs/screenshots/r6]   (Git Bash, project root)
set -u
LANG_CODE="${1:?zh|en}"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="${2:-$ROOT/docs/screenshots/r6}/$LANG_CODE"
P="r6"
S="$ROOT/docs/scripts"
KEEP="workbench ghost occupied bar box dragmove dragmove-commit edit eyedrop edit-close"
mkdir -p "$OUT"
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

echo "== sample workbench"
adb shell settings put global gridspace_demo sample
adb logcat -c
pico-cli app launch com.kejing.gridspace >/dev/null 2>&1
sleep 45
bash "$S/cap.sh" "$OUT/$P-workbench.png" 5

echo "== walk"
bash "$S/walk.sh" "$OUT" "$P"
adb logcat -d | grep -iE "AndroidRuntime|FATAL|ANR " | grep -v "GridSpaceDemo" | tail -60 > "$OUT/$P-logcat-walk.txt"
adb logcat -d -v time -s GridSpaceDemo:I GridSpaceEditor:I GridSpaceEntry:W > "$OUT/$P-demo-steps.txt"

echo "== keep only the R6 steps"
for f in "$OUT"/$P-*.png; do
  n=$(basename "$f" .png); n=${n#$P-}
  case " $KEEP " in *" $n "*) ;; *) rm -f "$f"; echo "dropped $n";; esac
done

echo "== cleanup"
adb shell am force-stop com.kejing.gridspace
cleanup_settings
adb shell settings delete global gridspace_lang >/dev/null 2>&1
adb shell settings list global | grep gridspace || echo "no gridspace settings left"
echo "== $(date) done"
