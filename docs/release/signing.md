# Artifact signing

**Only PsiWray, the OSSRH key owner, may sign or publish a release.** Signing
is local-only; no CI job has the key or passphrase.

Every Maven Central artifact is signed with the dedicated OSSRH key. Signatures
are detached ASCII-armored files (`.asc`) next to the artifact.

## Key to trust

| Field | Value |
|---|---|
| Fingerprint | `FAA5A60467F543371A28988501E34446CF304A61` |
| Long key ID | `01E34446CF304A61` |
| User ID | `PsiWray (OSSRH) <development@psiwray.com>` |
| Algorithm | RSA 4096, signing-capable primary key |
| Created | 2023-07-29 |
| Expires | **2027-06-13** |

The key is published on `keys.openpgp.org` and `keyserver.ubuntu.com`. Central
only needs a discoverable public key. **The secret key stays on the maintainer's
device.** It is not stored in GitHub, Forgejo, or any other CI system, and no
pipeline signs artifacts.

## Local signing

The build selects the key from `gradle.properties`:

```properties
signing.gnupg.executable=gpg
signing.gnupg.keyName=FAA5A60467F543371A28988501E34446CF304A61
```

Generate the standalone JAR signatures:

```bash
./gradlew sign -Prelease
```

Gradle invokes `gpg`, which asks `gpg-agent`/pinentry for the passphrase. Enter
it in the pinentry window. Do not put the passphrase in a Gradle property, a
shell command, or an environment file for local builds.

The signed files are:

```text
library/build/libs/terminal-window-gui-1.0.0.jar
library/build/libs/terminal-window-gui-1.0.0-sources.jar
library/build/libs/terminal-window-gui-1.0.0-javadoc.jar
library/build/libs/*.jar.asc
```

`-javadoc.jar` is the Dokka-generated Javadoc archive and is signed like the
other two JARs. The `mavenJava` publication also creates
`signMavenJavaPublication`, which signs the POM and Gradle module metadata
alongside the JARs. The local upload procedure is in
[publishing.md](publishing.md).

A normal build without `-Prelease` does not create or require signatures.

## Verify locally with the public key only

Export the public key once:

```bash
gpg --armor --export FAA5A60467F543371A28988501E34446CF304A61 \
  > /secure/path/terminal-window-gui-signing.asc
```

Then verify all artifacts with only that public key:

```bash
SIGNING_PUBLIC_KEY_FILE=/secure/path/terminal-window-gui-signing.asc \
  scripts/verify-signatures.sh library/build/libs
```

The script creates a temporary GnuPG home, imports only the public key, checks
every `*.jar`, `*.pom`, and `*.module` in the directory, requires a matching
non-empty `.asc`, and rejects a signature whose primary key is not
`FAA5A60467F543371A28988501E34446CF304A61`.

For a single file:

```bash
gpg --verify library/build/libs/terminal-window-gui-1.0.0.jar.asc \
  library/build/libs/terminal-window-gui-1.0.0.jar
```

## Release policy

**Release authority: PsiWray only.** Releases are performed locally by the key
owner; no other person or automation may sign or publish a release. There is
deliberately no release-signing workflow in `.github/workflows/` or
`.forgejo/workflows/`. CI builds and tests unsigned snapshot artifacts.

Do not add `SIGNING_KEY` or `SIGNING_PASSWORD` secrets to either forge. A
release is cut from the maintainer's device with:

```bash
./gradlew --no-daemon sign -Prelease
scripts/verify-signatures.sh library/build/libs
```

Only the maintainer may run those commands. The branch/tag/hotfix sequence is
in [runbook.md](runbook.md); the Central upload mechanics are in
[publishing.md](publishing.md).

## Rotation and revocation

- Rotate before **2027-06-13**. An expired key fails Central validation even
  when the old signatures remain cryptographically valid.
- Keep the revocation certificate offline. If the key is compromised, publish
  the revocation, generate a new dedicated key, publish it on both keyservers,
  update `gradle.properties`, and update the fingerprint in this document and
  `scripts/verify-signatures.sh`.
- Past releases stay signed by their original key; never re-sign and re-upload
  an already published Central version.

Generate a replacement dedicated signing key locally:

```bash
gpg --quick-gen-key 'PsiWray (OSSRH) <development@psiwray.com>' rsa4096 sign 2y
```

Create the offline revocation certificate immediately:

```bash
gpg --output terminal-window-gui-revocation.asc \
  --gen-revoke FAA5A60467F543371A28988501E34446CF304A61
```

Move the revocation certificate to offline media, then publish the new public
key to both keyservers and update the three fingerprint references:

1. `gradle.properties` (`signing.gnupg.keyName`)
2. this document's key table and verification commands
3. `scripts/verify-signatures.sh` (`expected_fingerprint` default)
