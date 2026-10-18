#!/bin/bash
# R10 capture for one language: the `panels` demo script (workbench with both panels closed -> size
# row + 3 x 2 x 1 preview -> colour rail -> selection drag-move -> colour card), synced through
# walk.sh. Output: docs/screenshots/r10/<lang>/r10-{workbench,size,rail,dragmove,edit}.png + r10-run.log.
# Usage: bash docs/scripts/r10.sh zh|en [out-root=docs/screenshots/r10]   (Git Bash, project root)
set -u
LANG_CODE="${1:?zh|en}"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="${2:-$ROOT/docs/screenshots/r10}/$LANG_CODE"
S="$ROOT/docs/scripts"
mkdir -p "$OUT"
exec > >(tee "$OUT/r10-run.log") 2>&1
echo "== $(date) r10 $LANG_CODE: panels script"
adb shell am force-stop com.kejing.skyboxspatial >/dev/null 2>&1
adb shell am force-stop com.kejing.gridspace
adb shell settings put global gridspace_lang "$LANG_CODE"
bash "$S/walk.sh" "$OUT" r10 panels
rm -f "$OUT/r10-done.png"
adb logcat -d | grep -iE "AndroidRuntime|FATAL|ANR " | grep -v "GridSpaceDemo" | tail -40 > "$OUT/r10-logcat.txt"
adb shell am force-stop com.kejing.gridspace
for s in gridspace_demo gridspace_demo_ack gridspace_lang gridspace_brush gridspace_rail gridspace_selpanel gridspace_popup; do
  adb shell settings delete global "$s" >/dev/null 2>&1
done
adb shell settings list global | grep gridspace || echo "no gridspace settings left"
echo "== $(date) done"
