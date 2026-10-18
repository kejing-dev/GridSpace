#!/bin/bash
# Placement probe without a rebuild: launches a demo script with one ${APP}_<setting> override and
# screenshots it. The app reads the override once per editor composition (DemoMode.placement):
# "ax,ay,az,ox,oy,oz[,align]" = Augment anchor (normalised Volume coords), offset dp, alignment
# 0 BottomCenter / 1 CenterLeft / 2 CenterRight / 3 TopCenter; a negative ax hides the card.
# Usage: probe.sh <out.png> <setting-suffix|-> <value|-> [script=$WORKBENCH_SCRIPT] [wait=$WORKBENCH_S]
#   e.g. probe.sh docs/screenshots/probe/rail-z0.5.png rail 1,0.5,0.5,32,0,0,1
# Deletes the override afterwards. Source: GridSpace docs/scripts/shot.sh + AGENTS "Placement overrides".
HERE="$(cd "$(dirname "$0")" && pwd)"
. "$HERE/app.env" || exit 2
OUT="$1"; SETTING="${2:--}"; VALUE="${3:--}"; SCRIPT="${4:-${WORKBENCH_SCRIPT:-sample}}"; WAIT="${5:-${WORKBENCH_S:-45}}"
mkdir -p "$(dirname "$OUT")"
for p in $STOP_OTHERS; do adb shell am force-stop "$p" >/dev/null 2>&1; done
adb shell am force-stop "$PKG"
adb shell settings put global "${APP}_demo" "$SCRIPT"
if [ "$SETTING" != "-" ]; then
  if [ "$VALUE" = "-" ]; then adb shell settings delete global "${APP}_$SETTING" >/dev/null
  else adb shell settings put global "${APP}_$SETTING" "$VALUE"; fi
fi
pico-cli app launch "$PKG" >/dev/null 2>&1
if [ "$SCRIPT" = "${WORKBENCH_SCRIPT:-sample}" ]; then
  sleep "$WAIT"
  bash "$HERE/cap.sh" "$OUT" "${CAP_TRIES:-5}"
else
  # a marker-driven script: capture every marker into the same folder with the file's stem as prefix
  bash "$HERE/walk.sh" "$(dirname "$OUT")" "$(basename "$OUT" .png)" "$SCRIPT"
fi
[ "$SETTING" != "-" ] && adb shell settings delete global "${APP}_$SETTING" >/dev/null
adb shell settings delete global "${APP}_demo" >/dev/null
echo "probe $OUT ($SETTING=$VALUE, $SCRIPT)"
