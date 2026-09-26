#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 || ( "$1" != "EXACT_TESTS" && "$1" != "FULL_BUILD" ) ]]; then
    echo "usage: $0 EXACT_TESTS|FULL_BUILD" >&2
    exit 2
fi

script_directory="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd -- "$script_directory"

mvn \
    -Dcheckstyle.skip=false \
    -DskipTests=true \
    -Dmaven.test.skip=false \
    test-compile

STEP12_V4_VERIFICATION_LAUNCHER=1 java \
    -cp target/test-classes \
    ru.homyakin.seeker.game.battle.simulation.Step12V4VerificationOrchestrator \
    "$1"
