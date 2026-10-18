#!/bin/bash
# Static + build gate for a PICO Spatial app (run from the project root, Git Bash). PASS/FAIL per
# item, exit 1 on any FAIL. Build/test can be skipped with --no-build when an agent already ran
# them (one Gradle at a time!). Set REF_PROJECT to a known-good project to assert the dependency
# files are unchanged (the --offline rule).
# Usage: gate.sh [--no-build] [--ref /c/Users/Admin/Downloads/GridSpace]
# Source: GridSpace QA_REPORT_R4 §1 / R7 §1 / R8 §1 static-check tables; AGENTS "House rules".
HERE="$(cd "$(dirname "$0")" && pwd)"
. "$HERE/app.env" 2>/dev/null
BUILD=1; REF="${REF_PROJECT:-}"
while [ $# -gt 0 ]; do case "$1" in --no-build) BUILD=0;; --ref) REF="$2"; shift;; esac; shift; done
fail=0
pass() { echo "PASS  $1"; }; failf() { echo "FAIL  $1"; fail=1; }; warn() { echo "WARN  $1"; }
SRC=app/src/main

if [ $BUILD -eq 1 ]; then
  export JAVA_HOME="${JAVA_HOME:-C:/Program Files/Android/Android Studio/jbr}"
  ./gradlew :app:assembleDebug --offline -q && pass "assembleDebug --offline" || failf "assembleDebug"
  ./gradlew :app:testDebugUnitTest --offline -q && pass "testDebugUnitTest --offline" || failf "unit tests"
  tests=$(grep -ho 'tests="[0-9]*"' app/build/test-results/testDebugUnitTest/*.xml 2>/dev/null | grep -o '[0-9]*' | awk '{s+=$1} END {print s+0}')
  fails=$(grep -ho 'failures="[0-9]*"' app/build/test-results/testDebugUnitTest/*.xml 2>/dev/null | grep -o '[0-9]*' | awk '{s+=$1} END {print s+0}')
  echo "tests: $tests run, $fails failed"
fi
bash "$HERE/dexcheck.sh" app/build/outputs/apk/debug/app-debug.apk || fail=1

g() { grep -rnE "$1" "$SRC/java" 2>/dev/null | grep -v '^\s*//' | wc -l; }
[ "$(g 'androidx\.compose\.material')" -eq 0 ] && pass "no Material imports" || failf "Material imports present (SpatialUI only)"
[ "$(g 'FontWeight\.(Bold|SemiBold|W600|W700)')" -eq 0 ] && pass "no Bold/SemiBold/W600/W700 names" || failf "banned FontWeight names (use FontWeight(600) literals for registration only)"
[ "$(g 'combinedClickable|onLongClick')" -eq 0 ] && pass "no long press" || failf "combinedClickable / onLongClick present"
[ "$(g 'backgroundMaterial\(|Modifier\.hoverable\(|googlefonts')" -eq 0 ] && pass "no backgroundMaterial / hoverable / googlefonts" || failf "app-authored material, hoverable or Google Fonts found"
cjk=$(grep -rnP '[\x{4e00}-\x{9fff}]' "$SRC/java" 2>/dev/null | grep -v '^\s*//' | grep -v '\*' | wc -l)
[ "$cjk" -eq 0 ] && pass "no CJK literals in Kotlin" || warn "$cjk Kotlin lines contain CJK (allowed only in comments)"
[ "$(g 'Text\("')" -eq 0 ] && pass "no hard-coded Text(\"...\")" || warn "hard-coded Text literals found"

if [ -f "$SRC/res/values-zh/strings.xml" ]; then
  en=$(grep -o 'name="[^"]*"' "$SRC/res/values/strings.xml" | sort); zh=$(grep -o 'name="[^"]*"' "$SRC/res/values-zh/strings.xml" | sort)
  d=$(diff <(echo "$en") <(echo "$zh") | grep -c '^[<>]')
  [ "$d" -eq 0 ] && pass "strings key sets identical ($(echo "$en" | wc -l) keys)" || failf "strings key sets differ in $d entries (values vs values-zh)"
fi
for f in NotoSans-VF.ttf NotoSansSC-VF.ttf OFL-NotoSans.txt OFL-NotoSansSC.txt; do
  [ -f "$SRC/assets/fonts/$f" ] || warn "font asset missing: $f (system font fallback)"
done
if [ -n "$REF" ]; then
  cmp -s gradle/libs.versions.toml "$REF/gradle/libs.versions.toml" && pass "libs.versions.toml identical to $REF" || failf "libs.versions.toml differs from $REF (no new dependencies: --offline)"
  d=$(diff app/build.gradle.kts "$REF/app/build.gradle.kts" | grep -cE '^[<>].*(implementation|testImplementation|api\()')
  [ "$d" -eq 0 ] && pass "dependency block identical to $REF" || failf "dependency lines differ from $REF"
fi
hover=$(g 'spatialHoverEffect'); custom=$(g 'Modifier\.clickable\(|\.clickable \{')
echo "info: $hover spatialHoverEffect vs $custom custom clickable modifiers (every custom clickable container needs one)"
exit $fail
