# Keycloak with the configurable theme pre-installed and pre-built.
#   mvn -DskipITs package && docker build -t keycloak-configurable-theme .
# Run with "start --optimized" plus your usual DB/hostname options; mount branding.json at
# /opt/keycloak/conf/branding.json and shared assets at /opt/keycloak/branding.
ARG KEYCLOAK_VERSION=26.8.0

FROM quay.io/keycloak/keycloak:${KEYCLOAK_VERSION} AS builder
ARG KC_DB=postgres
ENV KC_DB=${KC_DB} \
    KC_HEALTH_ENABLED=true \
    KC_METRICS_ENABLED=true
COPY target/keycloak-configurable-theme.jar /opt/keycloak/providers/
RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:${KEYCLOAK_VERSION}
COPY --from=builder /opt/keycloak/ /opt/keycloak/
LABEL org.opencontainers.image.title="keycloak-configurable-theme" \
      org.opencontainers.image.description="Keycloak with one configurable theme for every realm" \
      org.opencontainers.image.source="https://github.com/dockndevai/keycloak-configurable-theme" \
      org.opencontainers.image.licenses="Apache-2.0"
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", "--optimized"]
