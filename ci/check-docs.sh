#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

readme_lines="$(awk 'END { print NR }' README.md)"
if ((readme_lines > 150)); then
    echo "error: README.md is $readme_lines lines; the limit is 150." >&2
    exit 1
fi

broken=0
checked=0

while IFS= read -r -d '' file; do
    directory="$(dirname "$file")"
    links="$(grep -oE '\]\([^)]+\)' "$file" || true)"
    while IFS= read -r raw; do
        [[ -z "$raw" ]] && continue
        link="${raw#](}"
        link="${link%)}"
        link="${link%% *}"

        case "$link" in
            http://* | https://* | mailto:* | tel:* | '#') continue ;;
        esac

        target="${link%%#*}"
        [[ -z "$target" ]] && continue

        if [[ "$target" = /* ]]; then
            resolved="$repo_root$target"
        else
            resolved="$directory/$target"
        fi

        checked=$((checked + 1))
        if [[ ! -e "$resolved" ]]; then
            echo "error: broken link in ${file#./}: $link" >&2
            broken=$((broken + 1))
        fi
    done <<<"$links"
done < <(find . -path './.git' -prune -o -path '*/build' -prune -o -name '*.md' -print0)

if ((broken > 0)); then
    echo "error: $broken broken internal link(s)." >&2
    exit 1
fi

echo "docs check passed: README lines=$readme_lines, internal links=$checked."
