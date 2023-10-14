package net.cst.keycloak.resources;

import lombok.extern.slf4j.Slf4j;
import net.cst.keycloak.events.logging.LoginEventListenerProviderFactory;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProviderFactory;

/**
 * @author : mreinhardt
 * @created : 13.07.23
 **/
@Slf4j
public class AuditedResourcesProviderFactory implements RealmResourceProviderFactory {
    public static final String CONTEXT_PATH = "auditing";

    @Override
    public AuditedResourcesProvider create(KeycloakSession keycloakSession) {
        return new AuditedResourcesProvider(keycloakSession);
    }

    @Override
    public String getId() {
        /**
         * this ID identifies the rest provider and is used as base context path for this module
         */
        return CONTEXT_PATH;
    }

    @Override
    public void init(Config.Scope scope) {
        log.info("Initializing Keycloak Auditor REST extension (Version {}).",
                LoginEventListenerProviderFactory.class.getPackage().getImplementationVersion());
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
