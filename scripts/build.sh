#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
source "$PROJECT_ROOT/scripts/common.sh"

require_oracle_jdk
require_jcdk_tools
require_jcdk_simulator

BUILD_DIR="$PROJECT_ROOT/build"
APPLET_CLASSES="$BUILD_DIR/applet-classes"
CLIENT_CLASSES="$BUILD_DIR/client-classes"
DELIVERABLES="$BUILD_DIR/deliverables"
CARD_API="$JC_HOME_TOOLS/lib/api_classic-3.2.0.jar"
AM_DIR="$JC_HOME_SIMULATOR/client/AMService"
COM_DIR="$JC_HOME_SIMULATOR/client/COMService"
AM_JAR="$AM_DIR/amservice.jar"
SOCKET_JAR="$COM_DIR/socketprovider.jar"

if [[ -d "$BUILD_DIR" ]]; then
    find "$BUILD_DIR" -depth -mindepth 1 -delete
fi
mkdir -p "$APPLET_CLASSES" "$CLIENT_CLASSES" "$DELIVERABLES"

mapfile -d '' -t applet_sources < <(
    find "$PROJECT_ROOT/src/applet/java" -type f -name '*.java' -print0 | sort -z
)
mapfile -d '' -t client_sources < <(
    find "$PROJECT_ROOT/src/client/java" -type f -name '*.java' -print0 | sort -z
)

echo "Compiling Java Card applet with Oracle JDK 25..."
# Oracle Converter input requires Java 8 class files (major version 52).
"$JAVA_HOME/bin/javac" -g -d "$APPLET_CLASSES" -cp "$CARD_API" \
    --release 8 "${applet_sources[@]}"

echo "Converting and verifying CAP with Oracle JCDK Tools 26.0..."
"$JC_HOME_TOOLS/bin/converter.sh" \
    -classdir "$APPLET_CLASSES" \
    -d "$DELIVERABLES" \
    -target 3.2.0 \
    -applet 0xf0:0x54:0x55:0x42:0x45:0x01:0x01 \
    io.github.tubesound.javacardbasic.card.HelloApplet \
    io.github.tubesound.javacardbasic.card \
    0xf0:0x54:0x55:0x42:0x45:0x01 \
    1.0

cap_file=$(find "$DELIVERABLES" -type f -name '*.cap' -print -quit)
if [[ -z "$cap_file" ]]; then
    echo "Converter completed without producing a CAP file." >&2
    exit 1
fi

echo "Compiling Oracle Simulator client..."
module_path="$AM_DIR:$COM_DIR"
class_path="$AM_JAR:$SOCKET_JAR"
"$JAVA_HOME/bin/javac" -g -d "$CLIENT_CLASSES" -cp "$class_path" \
    --module-path "$module_path" --add-modules ALL-MODULE-PATH \
    "${client_sources[@]}"

echo "Build succeeded."
echo "CAP: $cap_file"
