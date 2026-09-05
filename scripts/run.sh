#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
source "$PROJECT_ROOT/scripts/common.sh"

require_oracle_jdk
require_jcdk_simulator

cap_file=$(find "$PROJECT_ROOT/build/deliverables" -type f -name '*.cap' -print -quit 2>/dev/null || true)
if [[ -z "$cap_file" ]]; then
    echo "CAP file is missing. Run ./scripts/build.sh first." >&2
    exit 1
fi

client_classes="$PROJECT_ROOT/build/client-classes"
main_class="io.github.tubesound.javacardbasic.client.HelloClient"
am_dir="$JC_HOME_SIMULATOR/client/AMService"
com_dir="$JC_HOME_SIMULATOR/client/COMService"
module_path="$am_dir:$com_dir"
class_path="$client_classes:$am_dir/amservice.jar:$com_dir/socketprovider.jar"
connection="${1:-socket:localhost:9025}"

exec "$JAVA_HOME/bin/java" -cp "$class_path" \
    --module-path "$module_path" --add-modules ALL-MODULE-PATH \
    "$main_class" \
    "--cap=$cap_file" \
    "--props=$PROJECT_ROOT/config/client.config.properties" \
    "--host=$connection"
