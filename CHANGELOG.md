# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added
- Publishing to Maven Central as `io.github.dockndevai:keycloak-configurable-theme`, with a one-time `scripts/setup-maven-central.sh` for the signing and token secrets.

### Changed
- Test and build tooling updated (JUnit 6.1, Maven surefire/failsafe 3.6, source plugin 3.4).

## [1.1.0]

### Added
- **Pluggable layouts, no rebuild:** a layout is a CSS file in `<assets-dir>/layouts/`, `<assets-dir>/<realm>/layouts/`, or uploaded via `PUT /admin/realms/{realm}/branding/layouts/{name}`. A leading `/* extends: <built-in> */` comment reuses a built-in layout's markup and CSS.
- `GET /admin/realms/{realm}/branding/layouts` lists available layouts; the public `/realms/{realm}/branding/layouts/{name}.css` serves them.
- Example layouts `card-left` and `banner-top` in the Docker demo.

### Changed
- `layout` accepts any layout name. The admin API and Branding tab reject unknown layouts, and a removed layout falls back to `centered`.

## [1.0.1]

### Changed
- Release pipeline: the Docker image and Maven Central publishing run as independent jobs, so one registry failing no longer blocks the others.
- The Maven artifact is no longer published to GitHub Packages (it needs a login even for public packages). Use the release jar, the Docker image or Maven Central.

### Added
- README screenshots of the admin-console Branding tab and the branded account console.

## [1.0.0]

### Added
- One `configurable` theme (login, account, admin, email) enforced for every realm through a theme selector.
- 36 branding settings: layout (`centered`, `split-left`, `split-right`, `minimal`), colours, background image and overlay, fonts, corner radius, logo and dark-background logo, favicon, page title, header and hero texts, announcement banner, footer text and links, console header colour, custom CSS.
- Layered configuration: built-in defaults, a hot-reloaded `branding.json` with global and per-realm sections, and per-realm overrides stored in the database.
- Admin REST API at `/admin/realms/{realm}/branding` (config, schema, assets) guarded by the realm's view and manage permissions.
- Admin-console **Realm settings → Branding** tab (requires the `declarative-ui` feature).
- Public endpoints for the generated CSS, console config and assets, with ETags and a locked-down SVG content security policy.
- Branded account and admin consoles, and a branded HTML email layout.
- Release jar on GitHub Releases.

[Unreleased]: https://github.com/dockndevai/keycloak-configurable-theme/compare/v1.1.0...HEAD
[1.1.0]: https://github.com/dockndevai/keycloak-configurable-theme/compare/v1.0.1...v1.1.0
[1.0.1]: https://github.com/dockndevai/keycloak-configurable-theme/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/dockndevai/keycloak-configurable-theme/releases/tag/v1.0.0
