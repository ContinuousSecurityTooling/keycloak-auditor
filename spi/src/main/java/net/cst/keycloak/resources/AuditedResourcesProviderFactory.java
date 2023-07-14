package net.cst.keycloak.resources;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProviderFactory;

/**
 * @author : mreinhardt
 * @created : 13.07.23
 **/
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
        // Nothing to do
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
