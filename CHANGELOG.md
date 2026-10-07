# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow [Semantic Versioning](https://semver.org/).

## [Unreleased]

## [1.0.0]

### Added
- One `configurable` theme (login, account, admin, email) enforced for every realm through a theme selector.
- 36 branding settings: layout (`centered`, `split-left`, `split-right`, `minimal`), colours, background image and overlay, fonts, corner radius, logo and dark-background logo, favicon, page title, header and hero texts, announcement banner, footer text and links, console header colour, custom CSS.
- Layered configuration: built-in defaults, a hot-reloaded `branding.json` with global and per-realm sections, and per-realm overrides stored in the database.
- Admin REST API at `/admin/realms/{realm}/branding` (config, schema, assets) guarded by the realm's view and manage permissions.
- Admin-console **Realm settings → Branding** tab (requires the `declarative-ui` feature).
- Public endpoints for the generated CSS, console config and assets, with ETags and a locked-down SVG content security policy.
- Branded account and admin consoles, and a branded HTML email layout.
- Docker image on GHCR, release jar on GitHub Releases, Maven artifact `io.github.dockndevai:keycloak-configurable-theme`.

[Unreleased]: https://github.com/dockndevai/keycloak-configurable-theme/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/dockndevai/keycloak-configurable-theme/releases/tag/v1.0.0
