#!/usr/bin/env bash
set -euo pipefail

if ! command -v java >/dev/null 2>&1; then
    echo "error: java is not on PATH; JDK 25 is required." >&2
    exit 1
fi

version_line="$(java -version 2>&1 | head -n 1)"
if [[ ! "$version_line" =~ \"([0-9]+)([.\"]|$) ]]; then
    echo "error: could not parse the Java version from: $version_line" >&2
    exit 1
fi

major="${BASH_REMATCH[1]}"
if [[ "$major" != "25" ]]; then
    echo "error: JDK 25 is required, found: $version_line" >&2
    exit 1
fi

echo "JDK 25 detected: $version_line"
