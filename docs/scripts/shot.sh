#!/bin/bash
# Launches the `sample` demo (editor with the pavilion, no scripted steps) and screenshots it.
# Usage: shot.sh <out.png> [brush placement "ax,ay,az,ox,oy,oz" | -]
OUT="$1"; PLACE="${2:--}"
adb shell am force-stop com.kejing.gridspace
adb shell settings put global gridspace_demo sample
if [ "$PLACE" = "-" ]; then adb shell settings delete global gridspace_brush >/dev/null; else adb shell settings put global gridspace_brush "$PLACE"; fi
pico-cli app launch com.kejing.gridspace >/dev/null 2>&1
sleep 18
adb exec-out screencap -p > "$OUT"
echo "captured $OUT ($PLACE)"
