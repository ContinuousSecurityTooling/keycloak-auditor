package net.cst.keycloak.resources;

import lombok.extern.slf4j.Slf4j;
import net.cst.keycloak.events.logging.LoginEventListenerProviderFactory;
import net.cst.keycloak.utils.RuntimeHelper;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProviderFactory;

@Slf4j
public class AuditedResourcesProviderFactory implements RealmResourceProviderFactory {
    public static final String CONTEXT_PATH = "auditing";

    @Override
    public AuditedResourcesProvider create(KeycloakSession keycloakSession) {
        return new AuditedResourcesProvider(keycloakSession);
    }

    /**
     * this ID identifies the rest provider and is used as base context path for this module
     */
    @Override
    public String getId() {
        return CONTEXT_PATH;
    }

    @Override
    public void init(Config.Scope scope) {
        log.info("Initializing Keycloak Auditor REST extension (Version {}).",
                RuntimeHelper.getVersion());
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
        // Nothing to do
    }

    @Override
    public void close() {
        // Nothing to do
    }
}
