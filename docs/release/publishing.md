# Maven Central publishing

`terminal-window-gui` publishes to the Central Portal through
`com.gradleup.nmcp` 1.6.2. The Portal has no official Gradle plugin; nmcp
bridges Gradle's Maven publication to the Portal upload API.

Publication is local-only. **PsiWray, the OSSRH key owner, is the only person
who may sign or publish a release.** CI never receives the signing key,
passphrase, or Portal token. The branch/tag/hotfix sequence is in
[runbook.md](runbook.md).

## Prerequisites

- The `com.psiwray.organization` namespace is verified in the Central Portal.
- A Portal user token exists. The token username and password are separate from
  the account password.
- The release tree is clean, has no `-SNAPSHOT` version, and includes the
  `CHANGELOG.md` entry.
- The OSSRH key from [signing.md](signing.md) is available in the local GnuPG
  keyring.

Do not use the legacy `oss.sonatype.org` staging URLs. Do not put the Portal
password, GPG passphrase, or signing properties in CI.

## Namespace and token

Confirm `com.psiwray.organization` is verified at
<https://central.sonatype.com/>. If it is absent, complete the Portal's DNS TXT
verification for `psiwray.com`; do not publish until it is verified.

Create the token in the Portal account settings and load it into the current
shell only:

```bash
export MAVEN_CENTRAL_USERNAME='<portal-token-username>'
read -rsp 'Central Portal token password: ' MAVEN_CENTRAL_PASSWORD; echo
export MAVEN_CENTRAL_PASSWORD
```

Use `--no-configuration-cache` for Portal commands so the token is not retained
in the Gradle configuration cache.

## Local dry run

The release tree sets `version=1.0.0` in `gradle.properties`. The explicit
override also works from a `-SNAPSHOT` development checkout:

```bash
./gradlew --no-daemon clean \
  :library:publishAllPublicationsToLocalRepository \
  -Prelease -Pversion=1.0.0
```

Gradle invokes `gpg-agent`/pinentry for the main JAR, sources JAR, Dokka
Javadoc JAR, POM, and Gradle module metadata. Enter the passphrase manually.

The local repository is:

```text
library/build/repo/com/psiwray/organization/terminal-window-gui/1.0.0/
```

It must contain:

```text
terminal-window-gui-1.0.0.jar
terminal-window-gui-1.0.0-sources.jar
terminal-window-gui-1.0.0-javadoc.jar
terminal-window-gui-1.0.0.pom
terminal-window-gui-1.0.0.module
*.asc                       # one signature for each file above
*.md5, *.sha1, *.sha256, *.sha512
```

Verify the signatures with only the public key:

```bash
gpg --armor --export FAA5A60467F543371A28988501E34446CF304A61 \
  > /secure/path/terminal-window-gui-signing.asc
SIGNING_PUBLIC_KEY_FILE=/secure/path/terminal-window-gui-signing.asc \
  scripts/verify-signatures.sh \
  library/build/repo/com/psiwray/organization/terminal-window-gui/1.0.0
```

Inspect the publication:

```bash
jar tf library/build/repo/com/psiwray/organization/terminal-window-gui/1.0.0/terminal-window-gui-1.0.0-javadoc.jar | head
jar --describe-module --file \
  library/build/repo/com/psiwray/organization/terminal-window-gui/1.0.0/terminal-window-gui-1.0.0.jar
grep -A4 '<artifactId>terminal-window</artifactId>' \
  library/build/repo/com/psiwray/organization/terminal-window-gui/1.0.0/terminal-window-gui-1.0.0.pom
grep -A4 '<artifactId>kotlin-stdlib</artifactId>' \
  library/build/repo/com/psiwray/organization/terminal-window-gui/1.0.0/terminal-window-gui-1.0.0.pom
```

The Javadoc JAR must contain HTML for every exported package. The POM must list
`terminal-window:2.0.0` at compile scope and Kotlin stdlib at runtime scope.
No LWJGL native classifier may appear.

## Portal validation

Upload a user-managed deployment and let the Portal validate it:

```bash
./gradlew --no-daemon publishAggregationToCentralPortal \
  -Prelease -Pversion=1.0.0 --no-configuration-cache
```

Open <https://central.sonatype.com/> and inspect the deployment. For a
pre-release dry run, **discard** it after validation and repeat until clean.
For the final release, leave the clean deployment in place and click
**Publish**. nmcp uploads in parallel; a failed upload can leave a partial
deployment that must be discarded rather than released.

## Release

1. Set `version=1.0.0` in `gradle.properties`; remove `-SNAPSHOT`.
2. Confirm `git status` is clean and the tree is the tagged release commit.
3. Run the local dry run and public-key verification again.
4. Upload with `USER_MANAGED` using the command above.
5. Review coordinates, files, signatures, and checksums, then click **Publish**.
6. Wait for the deployment to report `PUBLISHED`.

Versions are immutable. A bad `1.0.0` cannot be replaced; fix it by publishing
`1.0.1`. Past releases stay signed by their original key.

## Snapshots

Snapshots are optional and are not on `repo1`:

```bash
./gradlew --no-daemon publishAggregationToCentralSnapshots \
  -Pversion=1.1.0-SNAPSHOT --no-configuration-cache
```

Snapshot coordinates are served from
`https://central.sonatype.com/repository/maven-snapshots/`.

## Validation-error catalogue

| Error | Cause | Fix |
|---|---|---|
| `401`/`403` | Wrong token or namespace | Regenerate the Portal token; confirm namespace ownership. |
| Missing `-sources.jar` | Publication lacks sources | Keep `withSourcesJar()` and `from(components["java"])`. |
| Empty `-javadoc.jar` | `withJavadocJar()` used instead of Dokka | Use the `javadocJar` task backed by `dokkaGeneratePublicationJavadoc`. |
| Missing `.asc` | Signing did not run | Rerun with `-Prelease`; check GnuPG key selection. |
| Invalid POM | Missing name, description, URL, license, developers, or SCM | Keep the `pom { ... }` block in `library/build.gradle.kts`. |
| Native classifier in POM | `natives` configuration leaked into the publication | Keep natives as a separate resolvable-only configuration. |
| `SNAPSHOT` rejected | Release repository given a snapshot version | Publish `1.0.0`, not `1.0.0-SNAPSHOT`. |
| Duplicate version | Version already on Central | Publish the next patch version. |
| Checksum mismatch | Upload interrupted or files changed after signing | Discard the deployment; rebuild and re-sign from a clean tree. |

## Related

[Artifact signing](signing.md) · [Release runbook](runbook.md) ·
[Compatibility](../compatibility.md)
