#!/usr/bin/env bash
set -euo pipefail

export JAVA_HOME="${JAVA_HOME:-/root/.local/share/mise/installs/java/21.0.2}"
export PATH="$JAVA_HOME/bin:$PATH"

GRADLE_CMD="./gradlew"
if [[ ! -x "$GRADLE_CMD" ]]; then
  GRADLE_CMD="gradle"
fi

VERSION="${1:-v1_1_0}"

$GRADLE_CMD assembleDebug
mkdir -p APK
cp app/build/outputs/apk/debug/app-debug.apk "APK/Warpath_${VERSION}.apk"

echo "Built APK at APK/Warpath_${VERSION}.apk"
