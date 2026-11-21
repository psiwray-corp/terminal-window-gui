#!/usr/bin/env bash
# One-shot normal release preparation:
#   master snapshot -> release branch -> release commit/tag/snapshot
#   -> master next-minor snapshot -> checkout release tag for building.
#
# Usage:
#   scripts/release.sh <version> [--master-next] [--dry-run]
#
# Run from master with a clean worktree. `--master-next` only performs the
# final master snapshot bump for an already-cut release. Signed commits and the
# signed tag prompt for the GPG passphrase; they are never bypassed.
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
master_next=false

while (($# > 0)); do
    case "$1" in
        --master-next)
            master_next=true
            shift
            ;;
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
next_minor="${major}.$((minor + 1)).0-SNAPSHOT"
release_branch="release/${major}.${minor}"
tag="v$version"

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

git rev-parse --is-inside-work-tree >/dev/null 2>&1 ||
    die "not inside a Git worktree."

[[ "$(git symbolic-ref --quiet --short HEAD || true)" == "master" ]] ||
    die "one-shot release must start on master."

[[ -f gradle.properties ]] || die "gradle.properties not found."
current_version="$(awk -F= '$1 == "version" { print $2; exit }' gradle.properties)"
[[ "$current_version" == "${version}-SNAPSHOT" ]] ||
    die "master version is ${current_version:-missing}, expected ${version}-SNAPSHOT."

[[ -z "$(git status --porcelain)" ]] ||
    die "worktree is dirty; commit release changes before releasing."

if [[ "$master_next" == true ]]; then
    if [[ "$dry_run" == true ]]; then
        echo "sed -i -E 's/^version=.*/version=$next_minor/' gradle.properties"
        echo "git add gradle.properties"
        echo "git commit -S -m \"Next development version $next_minor.\""
    else
        sed -i -E "s/^version=.*/version=${next_minor}/" gradle.properties
        git add gradle.properties
        git commit -S -m "Next development version $next_minor."
        echo "master: $next_minor"
    fi
    exit 0
fi

grep -Eq "^## \\[$version\\]" CHANGELOG.md ||
    die "CHANGELOG.md has no '## [$version]' release heading."

if git show-ref --verify --quiet "refs/heads/$release_branch"; then
    die "local branch $release_branch already exists."
fi
if git show-ref --verify --quiet "refs/remotes/origin/$release_branch"; then
    die "remote branch origin/$release_branch already exists."
fi
if git show-ref --verify --quiet "refs/tags/$tag"; then
    die "tag $tag already exists."
fi

if [[ "$dry_run" == true ]]; then
    echo "scripts/release-branch.sh $version"
    echo "scripts/release-prepare.sh $version"
    echo "sed -i -E 's/^version=.*/version=$next_minor/' gradle.properties"
    echo "git add gradle.properties"
    echo "git commit -S -m \"Next development version $next_minor.\""
    echo "git switch --detach $tag"
    echo "release: $tag"
    echo "release branch: $release_branch at ${major}.${minor}.$(( ${version##*.} + 1 ))-SNAPSHOT"
    echo "master: $next_minor"
    exit 0
fi

"$repo_root/scripts/release-branch.sh" "$version"
"$repo_root/scripts/release-prepare.sh" "$version"

git switch master
sed -i -E "s/^version=.*/version=${next_minor}/" gradle.properties
git add gradle.properties
git commit -S -m "Next development version $next_minor."

git switch --detach "$tag"

echo "release prepared: $tag"
echo "release branch: $release_branch"
echo "master: $next_minor"
echo "build from tag: git switch --detach $tag"