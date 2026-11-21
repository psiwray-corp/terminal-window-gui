#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

jar_file="$(
    find library/build/libs -maxdepth 1 -name 'terminal-window-gui-*.jar' \
        ! -name '*-sources.jar' ! -name '*-javadoc.jar' -print 2>/dev/null |
        sort | tail -n 1
)"
if [[ -z "$jar_file" || ! -f "$jar_file" ]]; then
    echo "error: library jar not found under library/build/libs" >&2
    exit 1
fi

description="$(jar --describe-module --file "$jar_file" 2>&1)"
if ! grep -q '^com\.psiwray\.organization\.terminal_window_gui@.* automatic$' <<<"$description"; then
    echo "error: automatic module name is missing from $jar_file" >&2
    echo "$description" >&2
    exit 1
fi

if jar --list --file "$jar_file" | grep -q '^module-info\.class$'; then
    echo "error: $jar_file contains module-info.class" >&2
    exit 1
fi

echo "module check passed: $(grep '^com\.psiwray\.organization\.terminal_window_gui@.* automatic$' <<<"$description")"
