#!/usr/bin/env sh
set -eu
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
GRADLE_VERSION=8.9
GRADLE_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}/joymart-distributions/gradle-$GRADLE_VERSION"
if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  mkdir -p "$(dirname "$GRADLE_HOME")"
  ZIP="$(dirname "$GRADLE_HOME")/gradle-$GRADLE_VERSION-bin.zip"
  if [ ! -f "$ZIP" ]; then
    command -v curl >/dev/null 2>&1 || { echo "curl is required to download Gradle $GRADLE_VERSION"; exit 1; }
    curl -fL "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$ZIP"
  fi
  command -v unzip >/dev/null 2>&1 || { echo "unzip is required"; exit 1; }
  unzip -q -o "$ZIP" -d "$(dirname "$GRADLE_HOME")"
fi
exec "$GRADLE_HOME/bin/gradle" -p "$APP_HOME" "$@"
