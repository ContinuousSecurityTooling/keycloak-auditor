package net.cst.keycloak.resources;

import net.cst.keycloak.utils.EndpointTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class AuditedResourcesProviderFactoryTest extends EndpointTest {

    @Test
    void shouldCreateProvider() {
        var given = new AuditedResourcesProviderFactory().create(session);
        assertNotNull(given);
    }

}
