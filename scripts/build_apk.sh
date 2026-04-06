#!/usr/bin/env bash
set -euo pipefail

export JAVA_HOME="${JAVA_HOME:-/root/.local/share/mise/installs/java/21.0.2}"
export PATH="$JAVA_HOME/bin:$PATH"

GRADLE_CMD="./gradlew"
if [[ ! -x "$GRADLE_CMD" ]]; then
  GRADLE_CMD="gradle"
fi

$GRADLE_CMD assembleDebug
echo "Built APK at app/build/outputs/apk/debug/app-debug.apk"
echo "Release/versioned artifacts are produced by the CI pipeline."
