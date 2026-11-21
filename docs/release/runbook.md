# Release and hotfix runbook

`terminal-window-gui` uses one release branch per major/minor line. `master`
always holds the newest development snapshot. A release branch holds the
current snapshot for its patch line. A published version exists in exactly one
commit, and that commit is tagged.

Signing and publishing are local-only. The key and passphrase never enter CI;
the exact upload mechanics are in [publishing.md](publishing.md).

## Branch and version model

| Ref | Example | Meaning |
|---|---|---|
| `master` | `1.1.0-SNAPSHOT` | Newest development snapshot |
| `release/1.0` | `1.0.1-SNAPSHOT` | Current snapshot of the 1.0 patch line |
| Tag `v1.0.0` | — | The one commit containing `1.0.0` |
| Tag `v1.0.1` | — | The one commit containing hotfix `1.0.1` |

`gradle.properties` is the version source of truth. The tag must match it:
`v${version}`. Never publish a `-SNAPSHOT` to the Central release repository.

The sequence for one release is:

```text
master:       1.0.0-SNAPSHOT
release/1.0:  1.0.0-SNAPSHOT
              |
              +-- commit 1.0.0 + tag v1.0.0
              |
release/1.0:  1.0.1-SNAPSHOT
master:       1.1.0-SNAPSHOT
```

Only the tagged commit contains the non-snapshot version. The next commit
restores the snapshot. The next minor version is `1.1.0-SNAPSHOT`, not
`2.0.0-SNAPSHOT`; `2.0.0` is reserved for a deliberate breaking release.

Release branches are never merged back into `master`. Development continues on
`master`; release branches remain only as maintenance targets for
cherry-picked hotfixes.

## Before starting

Use JDK 25, a clean release branch, and a working GnuPG keyring. The public key
must be available for verification:

```bash
export PUBLIC_KEY_FILE=/secure/path/terminal-window-gui-signing.asc
export VERSION=1.0.0
export MAJOR="${VERSION%%.*}"
export MINOR="${VERSION#*.}"; export MINOR="${MINOR%%.*}"
export RELEASE_BRANCH="release/${MAJOR}.${MINOR}"
export NEXT_MINOR="${MAJOR}.$((MINOR + 1)).0-SNAPSHOT"
export TAG="v$VERSION"
export DATE="$(date -u +%F)"
```

For `1.0.0`, these expand to `release/1.0`, `1.1.0-SNAPSHOT`, `v1.0.0`, and
today's UTC date. `release-prepare.sh` derives the next patch snapshot
(`1.0.1-SNAPSHOT`) itself.

## Git helper scripts

Use the one-shot normal-release script:

```bash
scripts/release.sh "$VERSION"               # all branch/commit/tag/master work
scripts/release.sh "$VERSION" --dry-run
scripts/release.sh "$VERSION" --master-next # master bump for an already-cut release
```

It creates `release/1.0`, makes the release commit, creates the signed tag,
makes the next-patch snapshot commit, moves `master` to the next minor snapshot,
and checks out the release tag for building.

Lower-level helpers remain available:

```bash
scripts/release-branch.sh "$VERSION"             # create or switch release/1.0
scripts/release-branch.sh "$VERSION" --push
scripts/release-branch.sh "$VERSION" --from HEAD
scripts/release-prepare.sh "$VERSION"            # release commit + signed tag + next snapshot
scripts/release-tag.sh "$VERSION"                # tag only; used by release-prepare.sh
scripts/release-tag.sh "$VERSION" --push
```

All accept `--dry-run`; use it to inspect the Git commands without changing the
repository. `release-branch.sh` may be used with uncommitted release-candidate
changes; `release-prepare.sh`, `release-tag.sh`, and `release.sh` require a
clean worktree. Signed commits use `git commit -S`; the GPG passphrase is
entered manually.

## Normal release

### 1. Commit the release changelog

On `master`, move the `[Unreleased]` changelog entries under a heading for this
release, leave a new empty `## [Unreleased]` section, and commit the change:

```bash
git switch master
git pull --ff-only
# Edit CHANGELOG.md.
git add CHANGELOG.md
git commit -S -m "Release notes for $VERSION"
```

The one-shot release script requires this committed changelog heading.

### 2. Run the one-shot preparation

```bash
scripts/release.sh "$VERSION"
```

The script performs, in order:

1. create `release/1.0` from `master`
2. `1.0.0-SNAPSHOT` -> signed commit `Release 1.0.0`
3. signed tag `v1.0.0`
4. `1.0.0` -> signed commit `Next development version 1.0.1-SNAPSHOT.`
5. `master` -> signed commit `Next development version 1.1.0-SNAPSHOT.`
6. check out `v1.0.0`

The release branch is left at `1.0.1-SNAPSHOT`; `master` is at
`1.1.0-SNAPSHOT`; the tag is the only commit containing `1.0.0`. Preview with:

```bash
scripts/release.sh "$VERSION" --dry-run
```

### 3. Build from the tag

The one-shot script already checked out the tag:

```bash
./gradlew --no-daemon clean check
xvfb-run -a ./gradlew --no-daemon integrationTest
./gradlew --no-daemon dokkaGenerate
./gradlew --no-daemon clean \
  :library:publishAllPublicationsToLocalRepository -Prelease
SIGNING_PUBLIC_KEY_FILE="$PUBLIC_KEY_FILE" \
  scripts/verify-signatures.sh \
  "library/build/repo/com/psiwray/organization/terminal-window-gui/$VERSION"
```

The last two commands prompt for the GPG passphrase. Verify that the local
repository contains the JAR, sources, Dokka Javadoc, POM, module metadata, one
`.asc` per file, and checksums. If a gate fails, delete the local tag and
repeat.

### 4. Publish to the Central Portal

Still on the tagged commit, load the Portal token into the current shell only:

```bash
export MAVEN_CENTRAL_USERNAME='<portal-token-username>'
read -rsp 'Central Portal token password: ' MAVEN_CENTRAL_PASSWORD; echo
export MAVEN_CENTRAL_PASSWORD

./gradlew --no-daemon publishAggregationToCentralPortal \
  -Prelease --no-configuration-cache
```

The deployment is `USER_MANAGED`. Review it at
<https://central.sonatype.com/>. If validation fails, discard the deployment
and fix the source; if it passes, click **Publish** and wait for `PUBLISHED`.

Do not discard the final validated deployment; that is the release. Do not move
the tag or re-upload after Central accepts the version.

### 5. Verify and push release refs

```bash
curl -fsSL \
  "https://repo1.maven.org/maven2/com/psiwray/organization/terminal-window-gui/$VERSION/terminal-window-gui-$VERSION.pom" \
  >/dev/null

git switch "$RELEASE_BRANCH"
git push origin "$RELEASE_BRANCH" "$TAG"
git push forgejo "$RELEASE_BRANCH" "$TAG"

git switch master
git push origin master
git push forgejo master
```

Also resolve the version from a clean consumer project and check the Central
artifact page. A failed resolution is not fixed by re-uploading the same
version; publish the next patch version.

The release branch is not merged back. After 1.0.0, `master` is
`1.1.0-SNAPSHOT` and `release/1.0` is `1.0.1-SNAPSHOT`.

## Hotfix

A hotfix bumps only the third version element. The fix lands on `master` first
and is cherry-picked to the release branch; release branches are never merged
back into `master`.

```bash
export VERSION=1.0.1
export RELEASE_BRANCH=release/1.0
export TAG="v$VERSION"
export DATE="$(date -u +%F)"

git switch master
git pull --ff-only
```

### 1. Commit the fix on master

```bash
# Make and test the fix.
git add <fixed-files>
git commit -S -m "Fix <issue>"
export FIX_COMMIT="$(git rev-parse HEAD)"
```

### 2. Cherry-pick the fix to the release branch

```bash
git switch "$RELEASE_BRANCH"
git pull --ff-only
git cherry-pick -S "$FIX_COMMIT"
```

Add the hotfix entry under a new `## [$VERSION] - $DATE` heading and commit it:

```bash
git add CHANGELOG.md
git commit -S -m "Add $VERSION changelog"
```

### 3. Prepare and build the patch release

```bash
scripts/release-prepare.sh "$VERSION"
git switch --detach "$TAG"

./gradlew --no-daemon clean check
xvfb-run -a ./gradlew --no-daemon integrationTest
./gradlew --no-daemon dokkaGenerate
./gradlew --no-daemon clean \
  :library:publishAllPublicationsToLocalRepository -Prelease
SIGNING_PUBLIC_KEY_FILE="$PUBLIC_KEY_FILE" \
  scripts/verify-signatures.sh \
  "library/build/repo/com/psiwray/organization/terminal-window-gui/$VERSION"
```

Upload and publish exactly as in the normal release. Then return to the release
branch and push:

```bash
git switch "$RELEASE_BRANCH"
git push origin "$RELEASE_BRANCH" "$TAG"
```

## Related

[Publishing](publishing.md) · [Signing](signing.md) · [Rollback](rollback.md)
