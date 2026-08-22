#!/bin/sh

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd) || exit 1
JAVACMD="${JAVA_HOME:+$JAVA_HOME/bin/}java"

if ! command -v "$JAVACMD" >/dev/null 2>&1; then
    echo "ERROR: Java was not found. Set JAVA_HOME or add java to PATH." >&2
    exit 1
fi

exec "$JAVACMD" ${JAVA_OPTS:-} ${GRADLE_OPTS:-} \
    -Dorg.gradle.appname=gradlew \
    -jar "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "$@"
