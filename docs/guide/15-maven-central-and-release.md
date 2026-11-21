# 15 — Maven Central, signing, release, 1.0.0

**Phase:** release · **Depends on:** 14 · **Blocks:** —

## Goal

Publish `com.psiwray.organization:terminal-window-gui:1.0.0` to Maven Central
with sources, Dokka Javadoc, signatures, and complete POM metadata. Adopt the
same branch/tag/signing procedure as `terminal-window`. Signing and publishing
are local-only; CI never receives the key or Portal token.

## Context budget

Read: `GUIDE.md`, this file, `terminal-window` `docs/guide/22-gpg-signing.md`,
`docs/guide/23-maven-central.md`, `docs/guide/24-release-runbook.md`,
`docs/release/publishing.md`, `docs/release/signing.md`,
`docs/release/runbook.md`, `docs/release/rollback.md`,
`scripts/release*.sh`, `scripts/verify-signatures.sh`, `settings.gradle.kts`,
and `library/build.gradle.kts`. Do not read sibling sections. Expected output:
release docs, final publishing/signing config, dry run, and verified `1.0.0`.

## Deliverables

- `docs/release/publishing.md`, `signing.md`, `runbook.md`, `rollback.md`.
- Final `settings.gradle.kts` nmcp configuration.
- Final `library/build.gradle.kts` publication, POM, sources, Dokka Javadoc,
  and signing blocks.
- Updated `scripts/release-*.sh` with `terminal-window-gui` naming.
- Verified local dry run and public-key signature check.
- `1.0.0` release commit/tag and next snapshot commits, executed by the user.
- `CHANGELOG.md` `1.0.0` heading.

## Steps

1. **Identity.** Group `com.psiwray.organization`, artifact
   `terminal-window-gui`, version `1.0.0`, package root
   `com.psiwray.organization.terminal_window_gui`, module
   `com.psiwray.organization.terminal_window_gui`. Confirm the Portal namespace
   `com.psiwray.organization` is verified.
2. **nmcp.** In `settings.gradle.kts`, use
   `id("com.gradleup.nmcp.settings") version "1.6.2"`, credentials from
   `MAVEN_CENTRAL_USERNAME`/`MAVEN_CENTRAL_PASSWORD`, `USER_MANAGED`, and
   `publicationName = "terminal-window-gui:${version}"`. No legacy OSSRH URLs.
3. **Publication.** In `library/build.gradle.kts`:
   - `maven-publish` publication `mavenJava` from `components["java"]`;
   - `artifactId = "terminal-window-gui"`;
   - `withSourcesJar()` and a `javadocJar` built from
     `dokkaGeneratePublicationJavadoc` (not `withJavadocJar()`);
   - POM: name `Terminal Window GUI`, description, repository URL, MIT license,
     developer `psiwray` / Luca Cavallari / `development@psiwray.com`, SCM
     `scm:git:git@github.com:psiwray-corp/terminal-window-gui.git` and URL;
   - local file repository `build/repo` for dry runs.
   The POM must list `terminal-window` at compile scope, Kotlin stdlib at
   runtime, and no LWJGL native classifier.
4. **Signing.** Add `signing { useGpgCmd(); sign(jar, sourcesJar, javadocJar);
   sign(publication) if -Prelease; isRequired = -Prelease }`. Keep the same
   maintainer key/fingerprint as `terminal-window` unless the user changes it:
   `FAA5A60467F543371A28988501E34446CF304A61`. `gradle.properties` keeps
   `signing.gnupg.executable=gpg` and `signing.gnupg.keyName=...`. The
   passphrase is manual; no key or passphrase in CI.
5. **Release docs.** Copy and adapt `terminal-window`'s publishing/signing/
   runbook/rollback docs. Replace artifact paths, coordinates, tag messages,
   and example versions. Document local-only signing, Portal token setup,
   namespace verification, dry run, validation, publish, snapshot channel, and
   the immutable-version rule.
6. **Release scripts.** Keep `release-branch.sh`, `release-prepare.sh`,
   `release-tag.sh`, `release.sh`, `verify-signatures.sh`. Change the tag
   message to `terminal-window-gui $version`; `verify-signatures.sh` defaults
   to `library/build/libs`; expected fingerprint is the maintainer key.
   `release-branch.sh` requires `${version}-SNAPSHOT` in `gradle.properties`;
   `release-prepare.sh` requires the `CHANGELOG.md` heading and a clean tree.
7. **Version model.**
   - `master`: `1.1.0-SNAPSHOT` after the release.
   - `release/1.0`: `1.0.0-SNAPSHOT` before, `1.0.1-SNAPSHOT` after.
   - Tag `v1.0.0`: the only commit containing `1.0.0`.
   - Release branches are never merged back; hotfixes are cherry-picked.
   - Signed commits (`git commit -S`) and signed tags (`git tag -s`).
8. **Local dry run.**
   ```sh
   ./gradlew --no-daemon clean :library:publishAllPublicationsToLocalRepository \
     -Prelease -Pversion=1.0.0
   ```
   Inspect
   `library/build/repo/com/psiwray/organization/terminal-window-gui/1.0.0/`:
   jar, sources, javadoc, pom, module, one `.asc` per file, checksums.
9. **Verify.** Run `scripts/verify-signatures.sh` with a public-key-only GnuPG
   home. Check `jar tf` for Javadoc HTML, `jar --describe-module` for the
   automatic module name, and `grep` the POM for `terminal-window` compile
   scope, Kotlin stdlib runtime, and no natives.
10. **Portal validation.** Upload a `USER_MANAGED` deployment with
    `publishAggregationToCentralPortal`; inspect and discard until clean. For
    the final release, leave the clean deployment and click **Publish**.
11. **Consumer proof.** Resolve `1.0.0` from `repo1.maven.org` in a tiny Gradle
    consumer on a clean cache; compile a program that constructs `GuiSurface`,
    resolves the bundled font, and paints one matrix. Do not use `mavenLocal`.
12. **Release.** On `master`, commit the `1.0.0` changelog heading; run
    `scripts/release.sh 1.0.0`; build from `v1.0.0`; publish; wait for
    `PUBLISHED`; verify the artifact page. The user performs signed commits,
    tags, and the Portal publish.
13. **Rollback.** A bad release cannot be replaced. Publish `1.0.1`; document
    the rollback procedure and never re-upload.
14. Update the `GUIDE.md` progress entry. Leave the tree uncommitted.

## Acceptance

- [ ] The local dry run contains exactly jar, sources, javadoc, pom, module,
      one signature per artifact, and checksums.
- [ ] The Javadoc jar contains HTML for every exported package.
- [ ] The POM lists `terminal-window` at compile scope and Kotlin stdlib at
      runtime, with no native classifier.
- [ ] `scripts/verify-signatures.sh` verifies every artifact with the public key.
- [ ] `jar --describe-module` shows
      `com.psiwray.organization.terminal_window_gui`.
- [ ] The Portal validates the deployment with no errors.
- [ ] `1.0.0` resolves from `repo1.maven.org` in a clean consumer.
- [ ] The tag is `v1.0.0`, matches `gradle.properties` at the tagged commit, and
      the tree is clean.
- [ ] `master` is at `1.1.0-SNAPSHOT`; `release/1.0` is at `1.0.1-SNAPSHOT`.
- [ ] No CI job has the signing key or Portal credentials.
- [ ] `docs/release/*` matches the scripts and the actual procedure.
- [ ] No v1.1 feature is claimed in the release notes.

## Pitfalls

- Maven Central rejects SNAPSHOT versions on the release repository.
- A published version is immutable; fix with the next patch.
- Missing/empty Javadoc fails validation; verify HTML, not just the manifest.
- A native classifier in the POM means the natives configuration leaked into
  the publication.
- Do not publish from a dirty tree or a version that differs from the tag.
- nmcp uploads in parallel; a partial failure leaves a deployment to discard.
- Do not put the Portal token or GPG passphrase in the repo or CI.
- Do not merge `release/1.0` back into `master`.
- Do not re-run the dry run after signing without re-signing changed files.

## Plan seed

> Plan section 15 only. Finalize nmcp and the `terminal-window-gui` Maven
> publication with sources, Dokka Javadoc, POM metadata, and local GPG signing.
> Copy/adapt the release docs and scripts, run the local dry run and signature
> verification, validate the Portal deployment, publish `1.0.0`, and verify a
> clean consumer. Signing and publishing stay local; CI never holds secrets.
