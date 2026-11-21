# ADR-0012: Release branches, signing, and Maven Central

- **Status:** Accepted
- **Date:** 2026-11-21
- **Deciders:** project owner
- **Supersedes:** none

## Context

`terminal-window-gui` must be published to Maven Central as
`com.psiwray.organization:terminal-window-gui`. Central requires a POM,
sources jar, Javadoc jar, and signatures. Published versions are immutable.
The repository uses `master` as its development branch and a release branch
for each minor line.

The host `terminal-window` uses a local release flow: Git tags and GPG signing
on the maintainer machine, with no CI publishing credentials. The GUI mirrors
that flow so the two projects have one release model.

## Decision

We will release from `master` and a per-minor release branch:

1. `master` carries `1.0.0-SNAPSHOT` until the first release.
2. `release/1.0` is cut from `master` when `1.0.0` is ready.
3. The release commit sets `version=1.0.0` and the tag is `v1.0.0`.
4. After the tag, `master` advances to `1.1.0-SNAPSHOT`; `release/1.0`
   advances to `1.0.1-SNAPSHOT`.
5. `1.0.x` patches are cut from `release/1.0`; `1.1.0` is cut from a future
   `release/1.1`.
6. Tags are signed annotated tags. Commits on the release branch are signed.
7. The tag message matches the version and is not reused.

Publication is local:

- `./gradlew publishToMavenLocal` verifies the POM, sources, and Dokka Javadoc.
- `scripts/release.sh` creates the release branch, sets the version, commits,
  tags, and advances the next development versions.
- Signing uses the project owner's GPG key; the passphrase is entered locally.
- `scripts/verify-signatures.sh` verifies the published artifacts in
  `library/build/libs`.
- `nmcp` uploads to the Central Portal and leaves the deployment in
  `USER_MANAGED` state for manual publication.
- A clean consumer project resolves `1.0.0` from a clean cache.

CI never receives a signing key, a Central token, or a release trigger. GitHub
and Forgejo workflows run build, unit tests, and Xvfb integration tests only.
They may build the publication locally, but they do not upload.

Release artifacts are exact: `terminal-window-gui-1.0.0.jar`,
`-sources.jar`, `-javadoc.jar`, and the POM. The POM contains no native
classifier and declares `terminal-window:2.0.0` as a compile dependency.

## Consequences

### Positive

- One release model shared with `terminal-window`.
- No long-lived publishing secrets in CI.
- Release branches isolate patch maintenance from new minor work.
- Sources and Dokka Javadoc are always present.

### Negative

- Releases require a maintainer machine with JDK 25, GPG, and Central
  credentials.
- A failed upload requires a new version; `1.0.0` cannot be repaired.
- Manual Portal validation adds a step.
- CI cannot publish a security fix without a maintainer.

### Neutral

- Section 02 creates the CI scripts; section 15 creates the release scripts
  and runbook.
- The exact GPG fingerprint and Portal credentials are local, not repository
  facts.
- `:examples` is never published.

## Alternatives

| Alternative | Why rejected |
|---|---|
| CI publishing | Requires a Central token and signing key in CI, a larger secret surface. |
| Publish every merge | No release review and no stable version line. |
| Single `master` version | Cannot patch `1.0.x` while `1.1.0` is in development. |
| Maven Central auto-release | Removes the manual validation step after a failed release. |
| No Javadoc jar | Central rejects the publication. |
| No signing | Central rejects unsigned artifacts. |

## References

- `terminal-window` `docs/release/runbook.md`, `docs/release/publishing.md`,
  `docs/release/signing.md`, and `docs/release/rollback.md`.
- Central Portal: <https://central.sonatype.com/>.
- nmcp: <https://github.com/martinbonnin/nmcp>.
- `docs/guide/15-maven-central-and-release.md`.
