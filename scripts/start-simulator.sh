#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
source "$PROJECT_ROOT/scripts/common.sh"

require_jcdk_simulator

export LD_LIBRARY_PATH="$JC_HOME_SIMULATOR/runtime/bin${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
exec "$JC_HOME_SIMULATOR/runtime/bin/jcsl" "-f=$PROJECT_ROOT/config/simulator.conf"
