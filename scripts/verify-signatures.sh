#!/usr/bin/env bash
# Verify every published artifact in a directory against its detached .asc.
# Usage:
#   scripts/verify-signatures.sh [artifact-directory]
#
# Verification uses a temporary, public-key-only GnuPG home. Set
# SIGNING_PUBLIC_KEY_FILE to an armored public key export; otherwise the key is
# exported from the local keyring. SIGNING_FINGERPRINT overrides the expected key.
set -euo pipefail

expected_fingerprint="${SIGNING_FINGERPRINT:-FAA5A60467F543371A28988501E34446CF304A61}"
artifact_dir="${1:-library/build/libs}"
public_key_file="${SIGNING_PUBLIC_KEY_FILE:-}"

if [[ ! "$expected_fingerprint" =~ ^[0-9A-Fa-f]{40}$ ]]; then
    echo "error: SIGNING_FINGERPRINT must be a 40-character fingerprint." >&2
    exit 1
fi
expected_fingerprint="${expected_fingerprint^^}"

if [[ ! -d "$artifact_dir" ]]; then
    echo "error: artifact directory not found: $artifact_dir" >&2
    exit 1
fi
if ! command -v gpg >/dev/null 2>&1; then
    echo "error: gpg is required." >&2
    exit 1
fi

verify_home="$(mktemp -d)"
trap 'rm -rf "$verify_home"' EXIT
chmod 700 "$verify_home"

if [[ -n "$public_key_file" ]]; then
    if [[ ! -s "$public_key_file" ]]; then
        echo "error: public key file is missing or empty: $public_key_file" >&2
        exit 1
    fi
    gpg --homedir "$verify_home" --batch --import "$public_key_file" >/dev/null 2>&1
else
    if ! gpg --batch --armor --export "$expected_fingerprint" >"$verify_home/public.asc" 2>/dev/null ||
        [[ ! -s "$verify_home/public.asc" ]]; then
        echo "error: expected key is not in the local public keyring." >&2
        exit 1
    fi
    gpg --homedir "$verify_home" --batch --import "$verify_home/public.asc" >/dev/null 2>&1
fi

listed_fingerprint="$(
    gpg --homedir "$verify_home" --batch --with-colons --list-keys |
        awk -F: '$1 == "fpr" { print toupper($10); exit }'
)"
if [[ "$listed_fingerprint" != "$expected_fingerprint" ]]; then
    echo "error: verification key is $listed_fingerprint; expected $expected_fingerprint." >&2
    exit 1
fi

verified=0

while IFS= read -r -d '' artifact; do
    signature="${artifact}.asc"
    if [[ ! -s "$signature" ]]; then
        echo "error: missing or empty signature: $signature" >&2
        exit 1
    fi

    if ! status="$(gpg --homedir "$verify_home" --batch --no-tty --status-fd 1 \
        --verify "$signature" "$artifact" 2>&1)"; then
        printf '%s\n' "$status" >&2
        echo "error: signature verification failed: $artifact" >&2
        exit 1
    fi

    signing_fingerprint="$(
        printf '%s\n' "$status" |
            awk '$1 == "[GNUPG:]" && $2 == "VALIDSIG" { print toupper($3); exit }'
    )"
    primary_fingerprint="$(
        printf '%s\n' "$status" |
            awk '$1 == "[GNUPG:]" && $2 == "VALIDSIG" { print toupper($NF); exit }'
    )"
    if [[ "$signing_fingerprint" != "$expected_fingerprint" &&
        "$primary_fingerprint" != "$expected_fingerprint" ]]; then
        echo "error: $artifact was signed by $signing_fingerprint, not $expected_fingerprint." >&2
        exit 1
    fi

    verified=$((verified + 1))
    echo "verified: $artifact"
done < <(
    find "$artifact_dir" -type f \
        \( -name '*.jar' -o -name '*.pom' -o -name '*.module' \) \
        -print0 | sort -z
)

if [[ "$verified" -eq 0 ]]; then
    echo "error: no artifacts found under $artifact_dir." >&2
    exit 1
fi

echo "verified $verified artifact(s) with $expected_fingerprint."