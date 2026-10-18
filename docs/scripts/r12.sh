#!/bin/bash
# R12 capture for one language via the `panels` demo script, keeping `workbench` (bare model +
# axis lines) and, for zh, `size` (size row open + preview). Usage (Git Bash, project root):
#   bash docs/scripts/r12.sh zh|en [out-root=docs/screenshots/r12]
set -u
LANG_CODE="${1:?zh|en}"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="${2:-$ROOT/docs/screenshots/r12}/$LANG_CODE"
S="$ROOT/docs/scripts"
KEEP=" workbench size "
[ "$LANG_CODE" = en ] && KEEP=" workbench "
mkdir -p "$OUT"
exec > >(tee "$OUT/r12-run.log") 2>&1
echo "== $(date) r12 $LANG_CODE: panels script, keep$KEEP"
adb shell am force-stop com.kejing.skyboxspatial >/dev/null 2>&1
adb shell am force-stop com.kejing.gridspace
adb shell settings put global gridspace_lang "$LANG_CODE"
bash "$S/walk.sh" "$OUT" r12 panels
for f in "$OUT"/r12-*.png; do
  n=$(basename "$f" .png); n=${n#r12-}
  case "$KEEP" in *" $n "*) ;; *) rm -f "$f"; echo "dropped $n";; esac
done
adb logcat -d | grep -iE "AndroidRuntime|FATAL|ANR " | grep -v "GridSpaceDemo" | tail -40 > "$OUT/r12-logcat.txt"
adb shell am force-stop com.kejing.gridspace
for s in gridspace_demo gridspace_demo_ack gridspace_lang gridspace_brush gridspace_rail gridspace_selpanel gridspace_popup; do
  adb shell settings delete global "$s" >/dev/null 2>&1
done
adb shell settings list global | grep gridspace || echo "no gridspace settings left"
echo "== $(date) done"
