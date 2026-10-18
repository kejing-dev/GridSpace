#!/bin/bash
# Log checks after a capture round. Prints PASS/FAIL lines; exit 1 on an app crash / ANR.
# Usage: logs.sh <out-dir> <prefix> [expected-steps="ghost occupied ... done"]
# Reads <out-dir>/<prefix>-logcat-walk.txt, -logcat-export.txt, -demo-steps.txt, -anr-ls.txt,
# -run.log written by round.sh. Source: GridSpace QA_REPORT_R4 §1, QA_REPORT_R7 §1, QA_REPORT_R8 §1.
HERE="$(cd "$(dirname "$0")" && pwd)"
. "$HERE/app.env" 2>/dev/null
OUT="$1"; P="$2"; EXPECT="${3:-}"
fail=0
for f in "$OUT/$P-logcat-walk.txt" "$OUT/$P-logcat-export.txt"; do
  [ -f "$f" ] || continue
  n=$(grep -v SysPerfMonitorService "$f" | grep -ciE "AndroidRuntime|FATAL EXCEPTION|ANR in ${PKG:-.}")
  if [ "$n" -gt 0 ]; then echo "FAIL  $n crash/ANR lines in $(basename "$f")"; fail=1; else echo "PASS  no app crash/ANR in $(basename "$f")"; fi
done
if [ -f "$OUT/$P-demo-steps.txt" ]; then
  steps=$(grep -oE "step [a-z0-9-]+" "$OUT/$P-demo-steps.txt" | awk '{print $2}' | tr '\n' ' ')
  echo "steps: $steps"
  if [ -n "$EXPECT" ]; then
    [ "$(echo $steps)" = "$(echo $EXPECT)" ] && echo "PASS  step order matches" || { echo "FAIL  step order differs from expected: $EXPECT"; fail=1; }
  fi
  echo "$steps" | grep -q "done" && echo "PASS  walk reached done" || { echo "FAIL  walk never logged done"; fail=1; }
fi
if [ -f "$OUT/$P-run.log" ]; then
  bad=$(grep -c "no clean frame" "$OUT/$P-run.log")
  [ "$bad" -gt 0 ] && echo "WARN  $bad frames without a clean capture (emulator black quad): $(grep "no clean frame" "$OUT/$P-run.log" | sed -E 's/.*\/([^ ]+\.png).*/\1/' | tr '\n' ' ')" || echo "PASS  every frame captured clean"
  miss=0
  for png in "$OUT"/$P-*.png; do grep -q "$(basename "$png")" "$OUT/$P-run.log" || miss=$((miss+1)); done
  [ "$miss" -gt 0 ] && echo "WARN  $miss png files have no cap.sh record in run.log (re-captures must be appended)" || echo "PASS  every png has a run.log record"
fi
if [ -f "$OUT/$P-anr-ls.txt" ]; then
  echo "anr:  $(tail -n +2 "$OUT/$P-anr-ls.txt" | awk '{print $6" "$7" "$8}' | tail -1)  (compare with the run date; a fresh trace = ANR during the run)"
fi
exit $fail
