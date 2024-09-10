FROM quay.io/keycloak/keycloak:25.0.5
COPY spi/target/keycloak-auditor-spi.jar /opt/keycloak/providers/

# Enable health and metrics support
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true
ENV KC_HOSTNAME=localhost

RUN /opt/keycloak/bin/kc.sh build

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]