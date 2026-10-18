#!/bin/bash
# R11 capture for one language: the `panels` demo script (both panels closed -> size row + preview ->
# ...), keeping `workbench` (bare model + axis guides) and `size`. The demo cannot orbit the camera
# (yaw is view-local state with no event), so there is no rotated frame.
# Usage: bash docs/scripts/r11.sh zh|en [out-root=docs/screenshots/r11]   (Git Bash, project root)
set -u
LANG_CODE="${1:?zh|en}"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="${2:-$ROOT/docs/screenshots/r11}/$LANG_CODE"
S="$ROOT/docs/scripts"
mkdir -p "$OUT"
exec > >(tee "$OUT/r11-run.log") 2>&1
echo "== $(date) r11 $LANG_CODE: panels script, keep workbench + size"
adb shell am force-stop com.kejing.skyboxspatial >/dev/null 2>&1
adb shell am force-stop com.kejing.gridspace
adb shell settings put global gridspace_lang "$LANG_CODE"
bash "$S/walk.sh" "$OUT" r11 panels
for f in "$OUT"/r11-*.png; do
  n=$(basename "$f" .png); n=${n#r11-}
  case " workbench size " in *" $n "*) ;; *) rm -f "$f"; echo "dropped $n";; esac
done
adb logcat -d | grep -iE "AndroidRuntime|FATAL|ANR " | grep -v "GridSpaceDemo" | tail -40 > "$OUT/r11-logcat.txt"
adb shell am force-stop com.kejing.gridspace
for s in gridspace_demo gridspace_demo_ack gridspace_lang gridspace_brush gridspace_rail gridspace_selpanel gridspace_popup; do
  adb shell settings delete global "$s" >/dev/null 2>&1
done
adb shell settings list global | grep gridspace || echo "no gridspace settings left"
echo "== $(date) done"
