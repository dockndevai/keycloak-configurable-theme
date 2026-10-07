# Contributing

Thanks for helping! Issues and pull requests are welcome.

## Development

You need Java 21, Maven and Docker.

Run all tests (the integration tests start Keycloak in Docker):
```bash
mvn verify
```
Start a local demo at <http://localhost:8080>:
```bash
mvn -DskipITs package && docker compose -f docker/docker-compose.yml up
```

## Adding a branding setting

1. Add a constant to `BrandingField`. Its key, type, default, label and help text drive validation, the admin-console tab, `/branding/schema` and the docs.
2. Use the setting in `CssGenerator` (for CSS variables) or `BrandingBean` (for templates).
3. Add tests and update the settings table in the README.

## Pull requests

- Keep changes focused, and add or adjust tests.
- Add a line under `Unreleased` in `CHANGELOG.md`.
- CI must be green; it runs the full Keycloak integration suite.
