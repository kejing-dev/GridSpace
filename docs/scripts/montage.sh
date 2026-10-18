#!/bin/bash
# Builds the "representative frames" sheet an agent looks at instead of every capture:
# up to 6 frames scaled to 1024 px wide, tiled 2 x 3 (or fewer), JPEG. Needs ffmpeg from
# imageio_ffmpeg (no system ffmpeg on this machine).
# Usage: montage.sh <out.jpg> <frame1.png> [frame2.png ...]      (max 6)
# Source: showcase skill §3 (ffmpeg hstack recipe); FACTORY_PLAN P3 (≤ 6 frames at 1024 px).
OUT="$1"; shift
FF=$(python -c "import imageio_ffmpeg,sys;sys.stdout.write(imageio_ffmpeg.get_ffmpeg_exe())" 2>/dev/null)
[ -x "$FF" ] || { echo "ffmpeg missing: pip install --cert <pem> imageio-ffmpeg"; exit 2; }
n=$#; [ $n -gt 6 ] && { echo "max 6 frames"; exit 2; }
inputs=""; f=""; i=0
for p in "$@"; do inputs="$inputs -i \"$p\""; f="$f[$i]scale=1024:-1[s$i];"; i=$((i+1)); done
case $n in
  1) layout="[s0]copy[v]";;
  2) layout="[s0][s1]hstack[v]";;
  3) layout="[s0][s1][s2]hstack=3[v]";;
  4) layout="[s0][s1]hstack[r0];[s2][s3]hstack[r1];[r0][r1]vstack[v]";;
  5) layout="[s0][s1][s2]hstack=3[r0];[s3][s4]hstack[r1];[r1]pad=iw*3/2:ih[r1p];[r0][r1p]vstack[v]";;
  6) layout="[s0][s1][s2]hstack=3[r0];[s3][s4][s5]hstack=3[r1];[r0][r1]vstack[v]";;
esac
eval "\"$FF\" -y -loglevel error $inputs -filter_complex \"$f$layout\" -map \"[v]\" -q:v 3 \"$OUT\""
echo "montage $OUT ($n frames)"
