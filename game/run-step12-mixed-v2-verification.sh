#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 || ( "$1" != "EXACT_TESTS" && "$1" != "FULL_BUILD" && "$1" != "FINAL" ) ]]; then
    echo "usage: $0 EXACT_TESTS|FULL_BUILD|FINAL" >&2
    exit 2
fi

script_directory="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd -- "$script_directory"

if [[ -n "${MAVEN_HOME:-}" && -x "${MAVEN_HOME}/bin/mvn" ]]; then
    maven_executable="${MAVEN_HOME}/bin/mvn"
elif command -v mvn >/dev/null 2>&1; then
    maven_executable="$(command -v mvn)"
elif [[ -x /opt/apache-maven-3.9.12/bin/mvn ]]; then
    maven_executable=/opt/apache-maven-3.9.12/bin/mvn
else
    echo "Maven executable was not found" >&2
    exit 1
fi
export MAVEN_HOME="$(cd -- "$(dirname -- "$maven_executable")/.." && pwd)"
if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]]; then
    java_executable="${JAVA_HOME}/bin/java"
elif command -v java >/dev/null 2>&1; then
    java_executable="$(command -v java)"
elif [[ -x /home/user/.jdks/axiomjdk-25.0.1/bin/java ]]; then
    java_executable=/home/user/.jdks/axiomjdk-25.0.1/bin/java
else
    echo "Java executable was not found" >&2
    exit 1
fi
java_executable="$(readlink -f -- "$java_executable")"
export JAVA_HOME="$(cd -- "$(dirname -- "$java_executable")/.." && pwd)"

"$maven_executable" \
    -Dcheckstyle.skip=false \
    -DskipTests=true \
    -Dmaven.test.skip=false \
    -Dmdep.includeScope=test \
    -Dmdep.outputFile=target/step12-mixed-v2-classpath.txt \
    clean \
    test-compile \
    org.apache.maven.plugins:maven-dependency-plugin:3.7.0:build-classpath

dependency_classpath="$(< target/step12-mixed-v2-classpath.txt)"
mockito_agent=""
IFS=: read -r -a dependencies <<< "$dependency_classpath"
for dependency in "${dependencies[@]}"; do
    case "$dependency" in
        */org/mockito/mockito-core/*/mockito-core-*.jar)
            mockito_agent="$dependency"
            break
            ;;
    esac
done
if [[ -z "$mockito_agent" ]]; then
    echo "mockito-core agent was not found in the Maven test classpath" >&2
    exit 1
fi
export STEP12_MIXED_V2_MOCKITO_AGENT="$mockito_agent"
if [[ " ${JAVA_TOOL_OPTIONS:-} " != *" -javaagent:${mockito_agent} "* ]]; then
    export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:+${JAVA_TOOL_OPTIONS} }-javaagent:${mockito_agent}"
fi

STEP12_MIXED_V2_VERIFICATION_LAUNCHER=1 "$java_executable" \
    -cp "target/test-classes:target/classes:${dependency_classpath}" \
    ru.homyakin.seeker.game.battle.Step12MixedV2VerificationOrchestrator \
    "$1"
