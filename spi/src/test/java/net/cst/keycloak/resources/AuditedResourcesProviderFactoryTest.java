package net.cst.keycloak.resources;

import net.cst.keycloak.utils.EndpointTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AuditedResourcesProviderFactoryTest extends EndpointTest {

    @Test
    void shouldCreateProvider() {
        var given = new AuditedResourcesProviderFactory().create(session);
        assertNotNull(given);
    }

    @Test
    void shouldExposeContextPathAsId() {
        assertEquals(AuditedResourcesProviderFactory.CONTEXT_PATH, new AuditedResourcesProviderFactory().getId());
    }
}
