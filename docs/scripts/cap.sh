#!/bin/bash
# Screenshot helper for the PICO emulator: `adb exec-out screencap` intermittently composites a
# full-size black or white quad over the scene (seen with ElementSpace too, survives an emulator
# restart), so this takes up to N shots and keeps the first one whose near-black / near-white
# coverage is below 3 %. Prints the coverage of the kept frame.
# Usage: cap.sh <out.png> [tries=4]
OUT="$1"; TRIES="${2:-4}"
TMP="$OUT.try.png"
best=""
for i in $(seq 1 "$TRIES"); do
  adb exec-out screencap -p > "$TMP"
  frac=$(python3 - "$TMP" <<'EOF'
import sys
from PIL import Image
im = Image.open(sys.argv[1]).convert('L'); px = im.load(); w, h = im.size
n = tot = 0
for x in range(0, w, 24):
    for y in range(0, h, 24):
        tot += 1
        if px[x, y] < 8 or px[x, y] > 252: n += 1
print("%.3f" % (n / tot))
EOF
)
  if python3 -c "import sys; sys.exit(0 if float('$frac') < 0.03 else 1)"; then
    mv -f "$TMP" "$OUT"; echo "kept $OUT (try $i, bad $frac)"; exit 0
  fi
  best="$frac"
done
mv -f "$TMP" "$OUT"; echo "kept $OUT (no clean frame in $TRIES tries, bad $best)"
