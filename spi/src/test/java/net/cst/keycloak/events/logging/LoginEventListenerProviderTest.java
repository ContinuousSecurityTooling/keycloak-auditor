package net.cst.keycloak.events.logging;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.events.Event;
import org.keycloak.models.*;

import static net.cst.keycloak.events.logging.util.ClientModelHelper.buildClient;
import static net.cst.keycloak.events.logging.util.EventHelper.buildClientLoginEvent;
import static net.cst.keycloak.events.logging.util.EventHelper.buildUserLoginEvent;
import static net.cst.keycloak.events.logging.util.UserModelHelper.buildUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author : mreinhardt
 * @created : 22.06.23
 **/
class LoginEventListenerProviderTest {
    static KeycloakSession session;
    static RealmModel realmModel;
    static UserProvider userProvider;

    static ClientProvider clientProvider;
    private LoginEventListenerProvider provider;

    @BeforeAll
    static void setup() throws Exception {
        session = mock(KeycloakSession.class);
        realmModel = mock(RealmModel.class);
        userProvider = mock(UserProvider.class);
        clientProvider = mock(ClientProvider.class);
        RealmProvider realmProvider = mock(RealmProvider.class);
        KeycloakContext ctx = mock(KeycloakContext.class);
        when(session.getContext()).thenReturn(ctx);
        when(session.realms()).thenReturn(realmProvider);
        when(realmProvider.getRealm(anyString())).thenReturn(realmModel);
        when(session.users()).thenReturn(userProvider);
        when(session.clients()).thenReturn(clientProvider);
    }

    @BeforeEach
    void prepare() {
        provider = new LoginEventListenerProvider(session);
    }

    @Test
    void shouldNotAddAnythingWithEmptyUser() {
        Event event = buildUserLoginEvent();
        UserModel user = buildUser(event.getUserId());
        when(userProvider.getUserById(realmModel, event.getUserId())).thenReturn(null);
        provider.onEvent(event);
        assertTrue(user.getAttributes().isEmpty());
    }

    @Test
    void shouldAuditUserLogin() {
        Event event = buildUserLoginEvent();
        UserModel user = buildUser(event.getUserId());
        when(userProvider.getUserById(realmModel, event.getUserId())).thenReturn(user);
        provider.onEvent(event);
        assertEquals(user.getAttributes().size(), 2);
        assertNotNull(user.getAttributes().get("aud_usr_last-login"));
        assertNotNull(user.getAttributes().get("aud_usr_last-login_" + event.getClientId()));
    }

    @Test
    void shouldNotAddAnythingWithEmptyClient() {
        Event event = buildClientLoginEvent();
        ClientModel client = buildClient(event.getClientId());
        when(clientProvider.getClientByClientId(realmModel, event.getClientId())).thenReturn(null);
        provider.onEvent(event);
        assertTrue(client.getAttributes().isEmpty());
    }

    @Test
    void shouldAuditClientLogin() {
        Event event = buildClientLoginEvent();
        ClientModel client = buildClient(event.getClientId());
        when(clientProvider.getClientByClientId(realmModel, event.getClientId())).thenReturn(client);
        provider.onEvent(event);
        assertEquals(client.getAttributes().size(), 1);
        assertNotNull(client.getAttributes().get("aud_cls_last-login"));
    }
}
