#!/usr/bin/env bash
# One-time setup of the GitHub secrets the release workflow needs to publish to Maven Central.
# Run it yourself in a terminal: secret values are piped or typed straight into GitHub and never printed.
#
#   scripts/setup-maven-central.sh [GPG_KEY_ID]
#
# Prerequisites: gh (logged in with access to the repo), gpg with your signing key, and a Sonatype
# Central user token (https://central.sonatype.com -> Account -> Generate User Token).
set -euo pipefail

REPO="${REPO:-dockndevai/keycloak-configurable-theme}"
KEY_ID="${1:-531B87F307A8A450}"

command -v gh >/dev/null || { echo "gh CLI not found" >&2; exit 1; }
command -v gpg >/dev/null || { echo "gpg not found (brew install gnupg)" >&2; exit 1; }
gh auth status >/dev/null 2>&1 || { echo "Run 'gh auth login' first" >&2; exit 1; }

if ! gpg --list-secret-keys "$KEY_ID" >/dev/null 2>&1; then
  echo "No secret GPG key $KEY_ID found. Available keys:" >&2
  gpg --list-secret-keys --keyid-format long >&2
  exit 1
fi

echo "Repository: $REPO"
echo "Signing key: $(gpg --list-keys --keyid-format long "$KEY_ID" | awk '/^uid/ {sub(/^uid +(\[[^]]*\] +)?/, ""); print; exit}') ($KEY_ID)"
echo

echo "1/4  Exporting the private key into secret GPG_PRIVATE_KEY (gpg may ask for the passphrase)..."
gpg --armor --export-secret-keys "$KEY_ID" | gh secret set GPG_PRIVATE_KEY --repo "$REPO"

echo "2/4  Secret GPG_PASSPHRASE: enter the passphrase of that key."
gh secret set GPG_PASSPHRASE --repo "$REPO"

echo "3/4  Secret CENTRAL_USERNAME: enter the username part of your Central user token."
gh secret set CENTRAL_USERNAME --repo "$REPO"

echo "4/4  Secret CENTRAL_TOKEN: enter the password part of your Central user token."
gh secret set CENTRAL_TOKEN --repo "$REPO"

echo
echo "Making sure the public key is on the keyservers Central checks..."
gpg --keyserver keyserver.ubuntu.com --send-keys "$KEY_ID" >/dev/null 2>&1 && echo "  keyserver.ubuntu.com: ok" || echo "  keyserver.ubuntu.com: failed (retry later)"

echo
gh secret list --repo "$REPO"
echo
echo "Done. The next release tag (git tag vX.Y.Z && git push origin vX.Y.Z) also publishes to Maven Central."
