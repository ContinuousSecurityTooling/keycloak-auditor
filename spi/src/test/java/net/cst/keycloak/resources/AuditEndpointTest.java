package net.cst.keycloak.resources;

import net.cst.keycloak.audit.model.AuditedClientRepresentation;
import net.cst.keycloak.audit.model.AuditedUserRepresentation;
import net.cst.keycloak.events.logging.util.ClientModelHelper;
import net.cst.keycloak.events.logging.util.UserModelHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.store.ResourceServerStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.common.Profile;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author : mreinhardt
 * @created : 09.08.23
 **/
class AuditEndpointTest {

    static KeycloakSession session;

    @BeforeEach
    void setUp() {
        session = mock(KeycloakSession.class);
        AuthorizationProvider authorization = mock(AuthorizationProvider.class);
        StoreFactory store = mock(StoreFactory.class);
        ResourceServerStore resourceServerStore = mock(ResourceServerStore.class);
        when(authorization.getStoreFactory()).thenReturn(store);
        when(store.getResourceServerStore()).thenReturn(resourceServerStore);
        when(session.getProvider(AuthorizationProvider.class)).thenReturn(authorization);
        Profile.configure();
    }

    @Test
    void shouldConvertToAuditedClientRepresentation() {
        ClientModel source = ClientModelHelper.buildClient();
        AuditedClientRepresentation client = AuditEndpoint.toBriefRepresentation(source, session);
        assertNotNull(client);
        assertEquals(client.getClientId(), source.getClientId());
    }

    @Test
    void shouldConvertToAuditedUserRepresentation() {
        UserModel source = UserModelHelper.buildUser();
        AuditedUserRepresentation user = AuditEndpoint.toBriefRepresentation(source);
        assertNotNull(user);
        assertEquals(user.getId(), source.getId());
    }
}
