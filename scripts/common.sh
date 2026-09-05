#!/usr/bin/env bash

require_oracle_jdk() {
    : "${JAVA_HOME:?JAVA_HOME must point to Oracle JDK 25}"
    if [[ ! -x "$JAVA_HOME/bin/java" || ! -x "$JAVA_HOME/bin/javac" ]]; then
        echo "JAVA_HOME must point to a 64-bit Oracle JDK 25 installation." >&2
        return 1
    fi

    local settings
    settings=$("$JAVA_HOME/bin/java" -XshowSettings:properties -version 2>&1)
    if [[ "$settings" != *"java.vendor = Oracle Corporation"* ]]; then
        echo "This project requires Oracle JDK. Detected Java settings:" >&2
        echo "$settings" >&2
        return 1
    fi
    if [[ "$settings" != *"java.specification.version = 25"* ]]; then
        echo "This project requires Oracle JDK 25." >&2
        return 1
    fi
    if [[ "$settings" != *"sun.arch.data.model = 64"* ]]; then
        echo "This project requires the 64-bit Oracle JDK." >&2
        return 1
    fi
}

require_jcdk_tools() {
    : "${JC_HOME_TOOLS:?JC_HOME_TOOLS must point to Oracle JCDK Tools 26.0}"
    [[ -f "$JC_HOME_TOOLS/lib/api_classic-3.2.0.jar" ]] ||
        { echo "api_classic-3.2.0.jar was not found under JC_HOME_TOOLS." >&2; return 1; }
    [[ -f "$JC_HOME_TOOLS/bin/converter.sh" ]] ||
        { echo "converter.sh was not found under JC_HOME_TOOLS." >&2; return 1; }
}

require_jcdk_simulator() {
    : "${JC_HOME_SIMULATOR:?JC_HOME_SIMULATOR must point to Oracle JCDK Simulator 26.0}"
    [[ -f "$JC_HOME_SIMULATOR/client/AMService/amservice.jar" ]] ||
        { echo "Oracle AMService was not found under JC_HOME_SIMULATOR." >&2; return 1; }
    [[ -f "$JC_HOME_SIMULATOR/client/COMService/socketprovider.jar" ]] ||
        { echo "Oracle Socket Provider was not found under JC_HOME_SIMULATOR." >&2; return 1; }
    [[ -f "$JC_HOME_SIMULATOR/runtime/bin/jcsl" ]] ||
        { echo "Oracle Java Card Linux Simulator was not found under JC_HOME_SIMULATOR." >&2; return 1; }
}
