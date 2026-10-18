#!/bin/bash
# Verifies that the APK's dex files reference the SpatialApplication class. Concurrent Gradle
# builds corrupt the Kotlin incremental caches and can produce an APK whose dex lacks it
# (ClassNotFoundException at launch). The class moves between classesN.dex from build to build
# (R2: classes10.dex), so EVERY dex is scanned and the counts summed. Expect 2-3, exit 0.
# Usage: dexcheck.sh [apk=app/build/outputs/apk/debug/app-debug.apk]
# Source: GridSpace AGENTS.md "Concurrent Gradle builds"; QA_REPORT_R2 N-03.
APK="${1:-app/build/outputs/apk/debug/app-debug.apk}"
[ -f "$APK" ] || { echo "FAIL  apk missing: $APK"; exit 1; }
for d in $(unzip -Z1 "$APK" 'classes*.dex'); do unzip -p "$APK" "$d" | grep -c platform/SpatialApplication; done \
  | awk '{s+=$1} END {print "SpatialApplication refs: " s; exit (s>0 ? 0 : 1)}'
rc=$?
[ $rc -eq 0 ] && echo "PASS  dex ok ($APK)" || echo "FAIL  dex broken: rm -rf app/build/kotlin app/build/tmp/kotlin-classes && ./gradlew :app:assembleDebug --offline --rerun-tasks"
exit $rc
