# Releasing

Releases are fully automated from git tags. The version in `pom.xml` stays a `-SNAPSHOT`; the workflow sets the real version from the tag.

## Cut a release

1. Make sure `main` is green in CI.
2. Move the `Unreleased` notes in `CHANGELOG.md` under a new version heading and commit.
3. Tag and push:
   ```bash
   git tag v1.0.0
   ```
   ```bash
   git push origin v1.0.0
   ```
4. The **Release** workflow runs the full test suite and then publishes (the image and Maven Central jobs run independently):
   - a GitHub Release with `keycloak-configurable-theme-1.0.0.jar` and `SHA256SUMS`
   - Docker: `ghcr.io/dockndevai/keycloak-configurable-theme:1.0.0`, `:1.0`, `:1.0.0-kc26.8.0` and `:latest` (amd64 and arm64)
   - Maven Central, if its secrets are configured (see below)

A tag with a suffix such as `v1.1.0-rc.1` is published as a pre-release and does not move `latest`.

## One-time setup

### GHCR image visibility

The first image push creates the package as **private**. To let anyone pull it, open the package under *Packages → keycloak-configurable-theme → Package settings*, set its visibility to public, and link it to this repository.

### Maven Central

The `io.github.dockndevai` namespace is already verified on <https://central.sonatype.com>, and the `dockndevai` signing key (`531B87F307A8A450`) is published on the keyservers. Only the repository secrets need setting once:

1. In Central, create a user token: *Account → Generate User Token*. Keep both parts (username and password) at hand.
2. Run the setup script in your own terminal. It pipes the private key straight into GitHub and prompts for the passphrase and token, so no secret is printed or stored on disk:
   ```bash
   scripts/setup-maven-central.sh
   ```
   To use a different key, pass its ID as the first argument.

It sets these repository secrets:

| Secret | Value |
|---|---|
| `GPG_PRIVATE_KEY` | ASCII-armoured private signing key |
| `GPG_PASSPHRASE` | the key's passphrase |
| `CENTRAL_USERNAME` | Central user token username |
| `CENTRAL_TOKEN` | Central user token password |

Without these secrets, the `maven-central` job skips itself with a notice and everything else still publishes. Versions on Central are permanent: a published version can never be changed or deleted, only superseded.

## Upgrading Keycloak

Dependabot opens a PR when `org.keycloak` artifacts get a new version, and CI runs the integration tests against that version. The weekly compatibility workflow also tests the `nightly` image, so breaking changes in the internal SPIs show up early. Bump the version in `docker/docker-compose.yml` and the `Dockerfile` default in the same PR.
