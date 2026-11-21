#!/usr/bin/env bash
# Prepare a release without manual version commits:
#   2.0.0-SNAPSHOT -> commit 2.0.0 + signed v2.0.0 -> commit 2.0.1-SNAPSHOT
#
# Usage:
#   scripts/release-prepare.sh <version> [--dry-run]
#
# Run on release/<major.minor> with a clean worktree, <version>-SNAPSHOT in
# gradle.properties, and a CHANGELOG heading for <version>. Commits are signed;
# GPG passphrase entry is manual.
set -euo pipefail

usage() {
    sed -n '2,10p' "$0" | sed 's/^# \{0,1\}//'
}

die() {
    echo "error: $*" >&2
    exit 1
}

version=""
dry_run=false

while (($# > 0)); do
    case "$1" in
        --dry-run)
            dry_run=true
            shift
            ;;
        -h | --help)
            usage
            exit 0
            ;;
        -*)
            die "unknown option: $1"
            ;;
        *)
            [[ -z "$version" ]] || die "version specified more than once."
            version="$1"
            shift
            ;;
    esac
done

[[ -n "$version" ]] || {
    usage >&2
    exit 2
}
[[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] ||
    die "version must look like MAJOR.MINOR.PATCH, got: $version"

major="${version%%.*}"
minor="${version#*.}"; minor="${minor%%.*}"
patch="${version##*.}"
next_version="${major}.${minor}.$((patch + 1))-SNAPSHOT"
release_branch="release/${major}.${minor}"
tag="v$version"

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

git rev-parse --is-inside-work-tree >/dev/null 2>&1 ||
    die "not inside a Git worktree."

current_branch="$(git symbolic-ref --quiet --short HEAD || true)"
[[ "$current_branch" == "$release_branch" ]] ||
    die "expected branch $release_branch, currently on ${current_branch:-detached HEAD}."

[[ -f gradle.properties ]] || die "gradle.properties not found."
current_version="$(awk -F= '$1 == "version" { print $2; exit }' gradle.properties)"
[[ "$current_version" == "${version}-SNAPSHOT" ]] ||
    die "gradle.properties version is ${current_version:-missing}, expected ${version}-SNAPSHOT."

[[ -z "$(git status --porcelain)" ]] ||
    die "worktree is dirty; commit release changes before preparing."

grep -Eq "^## \\[$version\\]" CHANGELOG.md ||
    die "CHANGELOG.md has no '## [$version]' release heading."

if git show-ref --verify --quiet "refs/tags/$tag"; then
    die "tag $tag already exists."
fi

if [[ "$dry_run" == true ]]; then
    echo "sed -i -E 's/^version=.*/version=$version/' gradle.properties"
    echo "git add gradle.properties"
    echo "git commit -S -m \"Release $version.\""
    echo "scripts/release-tag.sh $version"
    echo "sed -i -E 's/^version=.*/version=$next_version/' gradle.properties"
    echo "git add gradle.properties"
    echo "git commit -S -m \"Next development version $next_version.\""
    echo "prepared $tag; next snapshot $next_version"
    exit 0
fi

sed -i -E "s/^version=.*/version=${version}/" gradle.properties
git add gradle.properties
git commit -S -m "Release $version."

"$repo_root/scripts/release-tag.sh" "$version"

sed -i -E "s/^version=.*/version=${next_version}/" gradle.properties
git add gradle.properties
git commit -S -m "Next development version $next_version."

echo "prepared $tag"
echo "next snapshot: $next_version"
echo "build from tag: git switch --detach $tag"