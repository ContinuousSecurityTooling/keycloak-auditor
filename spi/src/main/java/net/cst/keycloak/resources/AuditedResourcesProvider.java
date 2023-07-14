package net.cst.keycloak.resources;

import lombok.RequiredArgsConstructor;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

/**
 * @author : mreinhardt
 * @created : 13.07.23
 **/
@RequiredArgsConstructor
public class AuditedResourcesProvider implements RealmResourceProvider {

    private final KeycloakSession session;

    @Override
    public Object getResource() {
        return new AuditEndpoint(session);
    }

    @Override
    public void close() {
        // Nothing to close
    }

}
