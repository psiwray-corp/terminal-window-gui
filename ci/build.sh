#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

bash ci/check-docs.sh
bash ci/check-jdk.sh

./gradlew --no-daemon --stacktrace \
    spotlessCheck \
    test \
    :examples:build \
    dokkaGenerate \
    publishToMavenLocal

bash ci/check-module.sh
