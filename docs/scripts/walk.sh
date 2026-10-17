#!/bin/bash
# Runs the `walk` demo and screenshots each scripted step, synced on the GridSpaceDemo log tag.
# R4 marks (DemoMode.walk): ghost occupied bar box erase paint selection dragmove dragmove-commit
# rotate duplicate stepx delete vivid edit eyedrop edit-close lang lang-back undo burst redo reset
# export exported projects done (r4.sh copies vivid -> rail and lang -> lang-toggle).
# The app logs `step <name>` only after the named state has been drawn, then holds that state
# until this script acknowledges the capture (`settings put global gridspace_demo_ack <name>`,
# 20 s timeout), so slow emulator screencaps never make the loop drift behind the script. Each
# capture goes through cap.sh, which retries past the emulator's intermittent black/white quad.
# On `burst` (three redos one second apart) it takes shots back to back to check the title
# Augment through rapid autosaves.
# R5: a third argument names the demo script (`walk` by default; `newproject` logs newproject /
# newproject-vivid / done).
# Usage: walk.sh <out-dir> [prefix] [script=walk]
OUT="$1"; PREFIX="${2:-r1}"; SCRIPT="${3:-walk}"
HERE="$(cd "$(dirname "$0")" && pwd)"
mkdir -p "$OUT"
adb shell am force-stop com.kejing.skyboxspatial >/dev/null 2>&1
adb shell am force-stop com.kejing.gridspace
adb shell settings put global gridspace_demo "$SCRIPT"
adb shell settings delete global gridspace_demo_ack >/dev/null
adb logcat -c
pico-cli app launch com.kejing.gridspace >/dev/null 2>&1
seen=""
deadline=$((SECONDS + 600))
while [ $SECONDS -lt $deadline ]; do
  lines=$(adb logcat -d -v time -s GridSpaceDemo:I 2>/dev/null | grep -E "step [a-z0-9-]+")
  while IFS= read -r line; do
    [ -z "$line" ] && continue
    s=$(echo "$line" | grep -oE "step [a-z0-9-]+" | awk '{print $2}')
    t=$(echo "$line" | awk '{print $2}')
    case " $seen " in *" $s "*) continue;; esac
    seen="$seen $s"
    if [ "$s" = "burst" ]; then
      for i in 1 2 3 4; do
        bash "$HERE/cap.sh" "$OUT/$PREFIX-burst-$i.png" 3
        echo "captured burst $i (marker $t, device now $(adb shell date +%H:%M:%S))"
      done
    else
      sleep 1.5
      bash "$HERE/cap.sh" "$OUT/$PREFIX-$s.png" 8
      echo "captured $s (marker $t, device now $(adb shell date +%H:%M:%S))"
    fi
    adb shell settings put global gridspace_demo_ack "$s"
    if [ "$s" = "done" ]; then adb shell settings delete global gridspace_demo_ack >/dev/null; exit 0; fi
  done <<< "$lines"
  sleep 0.3
done
adb shell settings delete global gridspace_demo_ack >/dev/null
echo "timeout; seen:$seen"
