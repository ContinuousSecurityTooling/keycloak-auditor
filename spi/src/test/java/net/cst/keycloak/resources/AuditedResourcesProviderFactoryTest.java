package net.cst.keycloak.resources;

import net.cst.keycloak.utils.EndpointTest;
import org.junit.jupiter.api.Test;
import org.keycloak.Config;
import org.keycloak.authorization.util.Tokens;
import org.keycloak.models.KeycloakContext;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuditedResourcesProviderFactoryTest extends EndpointTest {

    @Test
    void shouldCreateProvider() {
        assertNotNull(new AuditedResourcesProviderFactory().create(session));
    }

    @Test
    void shouldExposeContextPathAsId() {
        assertEquals(AuditedResourcesProviderFactory.CONTEXT_PATH, new AuditedResourcesProviderFactory().getId());
    }

    @Test
    void initShouldNotThrow() {
        assertDoesNotThrow(() -> new AuditedResourcesProviderFactory().init(mock(Config.Scope.class)));
    }

    @Test
    void postInitShouldNotThrow() {
        assertDoesNotThrow(() -> new AuditedResourcesProviderFactory().postInit(null));
    }

    @Test
    void closeShouldNotThrow() {
        assertDoesNotThrow(() -> new AuditedResourcesProviderFactory().close());
    }

    @Test
    void providerGetResourceShouldReturnAuditEndpoint() {
        try (MockedStatic<Tokens> t = mockStatic(Tokens.class)) {
            t.when(() -> Tokens.getAccessToken(session)).thenReturn(null);
            assertNotNull(new AuditedResourcesProvider(session).getResource());
        }
    }

    @Test
    void providerCloseShouldNotThrow() {
        assertDoesNotThrow(() -> new AuditedResourcesProvider(session).close());
    }
}
