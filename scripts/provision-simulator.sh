#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
source "$PROJECT_ROOT/scripts/common.sh"

require_oracle_jdk
require_jcdk_simulator

simulator="$JC_HOME_SIMULATOR/runtime/bin/jcsl"
configurator="$JC_HOME_SIMULATOR/tools/Configurator.jar"
backup="$simulator.unprovisioned"

[[ -f "$configurator" ]] ||
    { echo "Configurator.jar was not found under JC_HOME_SIMULATOR." >&2; exit 1; }
if [[ -e "$backup" ]]; then
    echo "The backup already exists: $backup" >&2
    echo "Use a newly extracted Simulator when provisioning again." >&2
    exit 1
fi

cp -p "$simulator" "$backup"
if ! "$JAVA_HOME/bin/java" -jar "$configurator" \
    -binary "$simulator" \
    -SCP-keyset 10 \
    1111111111111111111111111111111111111111111111111111111111111111 \
    2222222222222222222222222222222222222222222222222222222222222222 \
    3333333333333333333333333333333333333333333333333333333333333333 \
    -global-pin 01020304050f 03; then
    cp -p "$backup" "$simulator"
    rm -f "$backup"
    echo "Provisioning failed; the original Simulator binary was restored." >&2
    exit 1
fi

echo "Oracle Java Card Simulator provisioning succeeded."
echo "Original binary: $backup"
