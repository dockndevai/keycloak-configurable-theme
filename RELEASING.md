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

### Maven Central (optional)

1. Sign in at <https://central.sonatype.com> with the GitHub account `dockndevai`. The namespace `io.github.dockndevai` is verified automatically.
2. Generate a user token: *Account → Generate User Token*.
3. Create a GPG key for signing and publish the public key:
   ```bash
   gpg --quick-gen-key "keycloak-configurable-theme releases" rsa4096 sign 2y
   ```
   ```bash
   gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
   ```
4. Add these repository secrets (*Settings → Secrets and variables → Actions*):

   | Secret | Value |
   |---|---|
   | `CENTRAL_USERNAME` | token username |
   | `CENTRAL_TOKEN` | token password |
   | `GPG_PRIVATE_KEY` | output of `gpg --armor --export-secret-keys <KEY_ID>` |
   | `GPG_PASSPHRASE` | the key's passphrase |

Without these secrets, the `maven-central` job skips itself with a notice and everything else still publishes.

## Upgrading Keycloak

Dependabot opens a PR when `org.keycloak` artifacts get a new version, and CI runs the integration tests against that version. The weekly compatibility workflow also tests the `nightly` image, so breaking changes in the internal SPIs show up early. Bump the version in `docker/docker-compose.yml` and the `Dockerfile` default in the same PR.
